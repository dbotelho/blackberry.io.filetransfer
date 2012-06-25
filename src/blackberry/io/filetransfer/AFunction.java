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

import java.util.Vector;

import net.rim.device.api.script.Scriptable;
import net.rim.device.api.script.ScriptableFunction;
import blackberry.io.filetransfer.lib.FileDownloadListener;
import blackberry.io.filetransfer.lib.FileDownloader;

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

	// private String tempp = "onde foi?";

	public Object invoke(Object thiz, Object[] args) throws Exception {

		ScriptableFunction _error = null;
		ScriptableFunction _progress = null;
		ScriptableFunction _success = null;
		if (getFunctionType().equals(ANamespace.FIELD_DOWNLOAD)) {


			try {
				Scriptable config = (Scriptable) args[0];

				final String remoteUrl = (String) config.getField("url");
				final String localUrl = (String) config.getField("dest");

				// Javascript Methods
				_success = (config.getField("success") != UNDEFINED) ? (ScriptableFunction) config
						.getField("success")
						: null;
				_error = (config.getField("error") != UNDEFINED) ? (ScriptableFunction) config
						.getField("error")
						: null;
				if (config.getField("progress") != UNDEFINED) {
					_progress = (ScriptableFunction) config
							.getField("progress");
				}

				final FileDownloader downloader = new FileDownloader(remoteUrl,
						localUrl);
				final ScriptableFunction _errorCallback = _error;
				final ScriptableFunction _progressCallback = _progress;
				final ScriptableFunction _successCallback = _success;
				downloader.setFileDownloadListener(new FileDownloadListener() {

					public void notifyDownloadProgress(final int totalsize,
							final int chunck) {
						if (_progressCallback != null) {
							new Thread(new Runnable() {

								public void run() {
									try {
										Object[] threadedResult = new Object[2];
										threadedResult[0] = totalsize + "";
										threadedResult[1] = chunck + "";
										try {
											_progressCallback.invoke(
													_progressCallback,
													threadedResult);
										} catch (Exception e) {
											throw new RuntimeException(e
													.getMessage());
										}
									} catch (Exception e) {
										throw new RuntimeException(e
												.getMessage());
									}
								}
							}).start();
						}
					}

					public void notifyDownloadFinnish() {
						if (_successCallback != null) {
							new Thread(new Runnable() {

								public void run() {
									final Object[] threadedResult = new Object[1];
									threadedResult[0] = localUrl;
									try {
										_successCallback.invoke(
												_successCallback,
												threadedResult);
									} catch (Exception e) {
										throw new RuntimeException(e
												.getMessage());
									}
								}
							}).start();

						}
					}

					public void notifyDownloadError(final String error) {

						if (_errorCallback != null) {
							new Thread(new Runnable() {
								public void run() {
									Object[] threadedResult = new Object[1];
									threadedResult[0] = error;

									try {
										_errorCallback.invoke(_errorCallback,
												threadedResult);
									} catch (Exception e) {
										throw new RuntimeException(e
												.getMessage());
									}
								}
							}).start();
						}
					}
				});
				// tempp += "3";

				if (config.getField("type") != UNDEFINED) {
					try {
						downloader.setMethodType((String) config
								.getField("type"));
					} catch (Exception e) {
					}
				}
				// tempp += "4";
				if (config.getField("headers") != UNDEFINED) {
					try {

						Scriptable headers = (Scriptable) config
								.getField("headers");
						Vector names = new Vector();
						headers.enumerateFields(names);
						for (int i = names.size() - 1; i >= 0; --i) {
							String fieldName = (String) names.elementAt(i);
							downloader.addHeaderProperty(fieldName,
									(String) headers.getField(fieldName));
						}

						/*
						 * if (headers
						 * .getField(HttpProtocolConstants.HEADER_AUTHORIZATION)
						 * != null) { downloader .addHeaderProperty(
						 * HttpProtocolConstants.HEADER_AUTHORIZATION, (String)
						 * headers
						 * .getField(HttpProtocolConstants.HEADER_AUTHORIZATION
						 * )); } if (headers
						 * .getField(HttpProtocolConstants.HEADER_CONTENT_TYPE)
						 * != null) { downloader .addHeaderProperty(
						 * HttpProtocolConstants.HEADER_CONTENT_TYPE, (String)
						 * headers
						 * .getField(HttpProtocolConstants.HEADER_CONTENT_TYPE
						 * )); }
						 */
					} catch (Exception e) {
						// tempp = "Couldn't parse headers"
						// + config.getField("headers").getClass();
					}
				}
				// tempp += "5";

				// downloader.addHeaderProperty(HttpProtocolConstants.HEADER_C,
				// authorization);

				// final AJAXRunnable downloader = new AJAXRunnable(url,
				// dest,type, _callback, _error);
				// downloader.setAuthorization(authorization);
				// downloader.setContentType(contentType);
				new Thread(downloader).start();

			} catch (Exception e) {
				if (_error != null) {
					final Object[] threadedResult = new Object[1];
					threadedResult[0] = e.toString();
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

}
