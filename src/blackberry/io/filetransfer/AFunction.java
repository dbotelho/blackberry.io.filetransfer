/*
 * Copyright 2012 Daniel Botelho
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blackberry.io.filetransfer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.io.http.HttpProtocolConstants;
import net.rim.device.api.script.Scriptable;
import net.rim.device.api.script.ScriptableFunction;
import net.rim.device.api.ui.UiApplication;
import blackberry.io.filetransfer.lib.ConnectionCreator;

/**
 * 
 * @author Daniel Botelho (www.dbotelho.com) botelho.daniel@gmail.com
 * 
 */
public class AFunction extends ScriptableFunction {

	public static final String DOWNLOAD_FUNC = "download";
	private String functionType;

	public void setFunctionType(String functionType) {
		this.functionType = functionType;
	}

	public String getFunctionType() {
		return functionType;
	}

	public Object invoke(Object thiz, Object[] args) throws Exception {

		ScriptableFunction _callback = null;
		ScriptableFunction _error = null;
		if (getFunctionType().equals(ANamespace.FIELD_DOWNLOAD)) {
			int idx = 0;
			String url = null;
			String tempp = "";
			try {
				final UiApplication uiApp = UiApplication.getUiApplication();

				Scriptable config = (Scriptable) args[0];
				_callback = (ScriptableFunction) config.getField("success");
				url = (String) config.getField("url");
				String dest = null;
				String type = null;
				String authorization = null;
				String contentType = null;

				if (config.getField("error") != null) {
					_error = (ScriptableFunction) config.getField("error");
				}
				if (config.getField("type") != null) {
					try {
						type = (String) config.getField("type");
					} catch (Exception e) {

						tempp = " type não +e null"
								+ config.getField("c_type").getClass();
					}
				}
				if (config.getField("headers") != null) {
					try {
						Scriptable headers = (Scriptable) config
								.getField("headers");
						if (headers
								.getField(HttpProtocolConstants.HEADER_AUTHORIZATION) != null) {
							authorization = (String) headers
									.getField(HttpProtocolConstants.HEADER_AUTHORIZATION);
						}
						if (headers
								.getField(HttpProtocolConstants.HEADER_CONTENT_TYPE) != null) {
							contentType = (String) headers
									.getField(HttpProtocolConstants.HEADER_CONTENT_TYPE);
						}
					} catch (Exception e) {
						tempp = " Couldn't parse headers"
								+ config.getField("headers").getClass();
					}
				}

				if (config.getField("dest") != null) {
					dest = (String) config.getField("dest");
				}
				AJAXRunnable downloader = new AJAXRunnable(url, dest, type,
						_callback, _error);
				downloader.setAuthorization(authorization);
				downloader.setContentType(contentType);
				uiApp.invokeLater(downloader);
			} catch (Exception e) {
				if (_error != null) {
					final Object[] threadedResult = new Object[1];
					threadedResult[0] = e.getMessage();
					final ScriptableFunction threadedError = _error;
					new Thread() {
						public void run() {
							try {
								threadedError.invoke(threadedError,
										threadedResult);
							} catch (Exception e) {
								throw new RuntimeException(e.getMessage());
							}
						}
					}.start();
				}
			}

		}

		return UNDEFINED;
	}

	private class AJAXRunnable implements Runnable {
		private ScriptableFunction _callback = null;
		private ScriptableFunction _error = null;
		private String _url = null;
		private String _dest = null;
		private String _type = null;
		private String _authorization = null;
		private String _contentType = null;

		public AJAXRunnable(String url, String dest, String type,
				ScriptableFunction callback, ScriptableFunction error) {
			// this.screen = screen;
			_callback = callback;
			_type = ((type != null) && (type.toUpperCase() == HttpConnection.POST)) ? HttpConnection.POST
					: HttpConnection.GET;
			_url = url;
			_dest = dest;
			_error = error;
			_contentType = "application/x-www-form-urlencoded";

		}

		public void setAuthorization(String authorization) {
			if (authorization != null) {
				_authorization = authorization;
			}
		}

		public void setContentType(String contentType) {
			if (contentType != null) {
				_contentType = contentType;
			}
		}

		public void run() {
			HttpConnection c = null;
			StreamConnection s = null;
			InputStream is = null;
			OutputStream output = null;
			String value = "";
			try {
				String suffix = ConnectionCreator.getConnectionString();

				s = (StreamConnection) Connector.open(_url + suffix,
						Connector.WRITE, true);
				c = (HttpConnection) s;

				c.setRequestProperty("User-Agent", "BlackBerry Client");
				c.setRequestProperty("Accept", "*/*");
				if (_contentType != null) {
					c.setRequestProperty(
							HttpProtocolConstants.HEADER_CONTENT_TYPE,
							_contentType);
				}

				c.setRequestMethod(_type);
				// Add the authorized header.
				if (_authorization != null) {
					c.setRequestProperty(
							HttpProtocolConstants.HEADER_AUTHORIZATION,
							_authorization);
				}

				/*
				 * if ((_data != null) && (_data.length() > 0)) {
				 * 
				 * byte[] postdata = _data.getBytes("UTF-8");
				 * c.setRequestProperty("Content-Length", Integer
				 * .toString(postdata.length)); output = c.openOutputStream();
				 * output.write(postdata); }
				 */
				int status = c.getResponseCode();
				if (status == HttpConnection.HTTP_UNAUTHORIZED) {
					s.close();
					s = (StreamConnection) Connector.open(_url + suffix,
							Connector.WRITE, true);
					c = (HttpConnection) s;

					c.setRequestProperty("User-Agent", "BlackBerry Client");
					c.setRequestProperty("Accept", "*/*");

					if (_contentType != null) {
						c.setRequestProperty(
								HttpProtocolConstants.HEADER_CONTENT_TYPE,
								_contentType);
					}

					c.setRequestMethod(_type);
					// Add the authorized header.
					if (_authorization != null) {
						c.setRequestProperty(
								HttpProtocolConstants.HEADER_AUTHORIZATION,
								_authorization);
					}
					// httpConn.setRequestProperty("TAG", "ggg=");

					/*
					 * if ((_data != null) && (_data.length() > 0)) {
					 * 
					 * byte[] postdata = _data.getBytes("UTF-8");
					 * c.setRequestProperty("Content-Length", Integer
					 * .toString(postdata.length)); output =
					 * c.openOutputStream(); output.write(postdata);
					 * 
					 * }
					 */
				}
				status = c.getResponseCode();
				value += status;

				is = s.openInputStream();

				// Get the length and process the data
				int len = (int) c.getLength();
				if (len > 0) {
					int actual = 0;
					int bytesread = 0;
					byte[] data = new byte[len];
					while ((bytesread != len) && (actual != -1)) {
						actual = is.read(data, bytesread, len - bytesread);
						bytesread += actual;
					}

					try {
						FileConnection fconn = (FileConnection) Connector
								.open(_dest);
						if (!fconn.exists()) {
							fconn.create(); // create the file if it doesn't
							// exist
							DataOutputStream outstream;
							outstream = fconn.openDataOutputStream();
							outstream.write(data);
							outstream.flush();
							outstream.close();
						}
						fconn.close();
					} catch (IOException ioe) {
					}
				}
				if (is != null) {
					try {
						is.close();
					} catch (Exception ignored) {
					}
				}
				if (output != null) {
					try {
						output.close();
					} catch (Exception ignored) {
					}
				}
				if (c != null) {
					try {
						c.close();
					} catch (Exception ignored) {
					}
				}
				s.close();
				final Object[] threadedResult = new Object[1];
				threadedResult[0] = _dest;
				new Thread() {
					public void run() {
						try {
							_callback.invoke(_callback, threadedResult);
						} catch (Exception e) {
							throw new RuntimeException(e.getMessage());
						}
					}
				}.start();
			} catch (IOException e) {
				if (_error != null) {
					final Object[] threadedResult = new Object[1];
					threadedResult[0] = value;
					new Thread() {
						public void run() {
							try {
								// Pass the result of the spinner back to the
								// handle
								// of the JavaScript callback
								_error.invoke(_error, threadedResult);
							} catch (Exception e) {
								throw new RuntimeException(e.getMessage());
							}
						}
					}.start();
				}
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (Exception ignored) {
					}
				}
				if (output != null) {
					try {
						output.close();
					} catch (Exception ignored) {
					}
				}
				if (c != null) {
					try {
						c.close();
					} catch (Exception ignored) {
					}
				}
			}
		}

	}

}
