package blackberry.io.filetransfer.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.http.HttpProtocolConstants;
import net.rim.device.api.system.Branding;
import net.rim.device.api.system.DeviceInfo;

public class FileDownloader implements Runnable {
	public static int BLOCK_SIZE = 2048;
	private String remoteName;
	private String localName;
	private FileDownloadListener fileDownloadListener;
	private String method_type;
	private Hashtable headers;

	public FileDownloader(String remoteName, String localName) {
		this.localName = localName;
		this.remoteName = remoteName;
		this.fileDownloadListener = new FileDownloadListener() {

			public void notifyDownloadProgress(int totalsize, int chunck) {
			}

			public void notifyDownloadFinnish() {
			}

			public void notifyDownloadError(String error) {
			}
		};
		headers = new Hashtable();
		//headers.put(HttpProtocolConstants.HEADER_AUTHORIZATION, null);
		headers.put(HttpProtocolConstants.HEADER_CONTENT_TYPE,
				"application/x-www-form-urlencoded");
		headers.put(HttpProtocolConstants.HEADER_USER_AGENT,
				getDefaultUserAgent());
		headers.put(HttpProtocolConstants.HEADER_ACCEPT, "*/*");
		method_type = HttpConnection.GET;
	}
	public String getDefaultUserAgent(){
		return "BlackBerry" + DeviceInfo.getDeviceName() + "/" +
        DeviceInfo.getSoftwareVersion() +
        " Profile/" + System.getProperty("microedition.profiles") +
        " Configuration/" + System.getProperty(
        "microedition.configuration") + " VendorID/" + Branding.
        getVendorId();
	}

	public void setMethodType(String method_type) {
		this.method_type = ((method_type != null) && (method_type.toUpperCase() == HttpConnection.POST)) ? HttpConnection.POST
				: HttpConnection.GET;
	}

	public void addHeaderProperty(String key, String value) {
		headers.put(key, value);
	}

	public void run() {
		OutputStream out = null;
		FileConnection file = null;
		HttpConnection conn =null;
		InputStream in = null;
		try {
			int totalSize = 0;
			file = (FileConnection) Connector.open(localName);
			if (!file.exists()) {
				file.create();
			}
			file.setWritable(true);
			out = file.openOutputStream();

			/*
			 * HTTP Connections
			 */
			String currentFile = remoteName
					+ ConnectionCreator.getConnectionString();
			conn = (HttpConnection) Connector.open(currentFile,
					Connector.READ_WRITE, true);
			Enumeration keys = headers.keys();
			String key, value;
			while (keys.hasMoreElements()) {
				key = (String) keys.nextElement();
				// Less efficient b/c I have to do a lookup every time.
				value = (String) headers.get(key);
				if (value != null) {
					conn.setRequestProperty(key, value);
				}
			}
			conn.setRequestMethod(method_type);
			int responseCode = conn.getResponseCode();
			if (responseCode != 200 && responseCode != 206) {
				// log("Response Code = " + conn.getResponseCode());
				throw new Exception("Error " + responseCode);
			}
			// log("Retreived Range: " + conn.getHeaderField("Content-Range"));
			in = conn.openInputStream();
			totalSize = (int) conn.getLength();
			int length = -1;
			byte[] readBlock = new byte[BLOCK_SIZE];
			int fileSize = 0;
			while ((length = in.read(readBlock)) != -1) {
				out.write(readBlock, 0, length);
				fileSize += length;
				fileDownloadListener
						.notifyDownloadProgress(totalSize, fileSize);
				Thread.sleep(1000); // Try not to get cut off
			}
			fileDownloadListener.notifyDownloadFinnish();
			/*
			 * Pause to allow connections to close and other Threads to run.
			 */
			Thread.sleep(1000);
		} catch (Exception e) {
			fileDownloadListener.notifyDownloadError("aqui"+e.getMessage());
		} finally {
			if(in!= null){
				try {
					in.close();
				} catch (IOException e) {	}
				in = null;
			}
			if(conn != null){
				try {
					conn.close();
				} catch (IOException e1) {	}
				conn = null;
			}
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {//ignore
				}
				out = null;
			}
			if(file != null){
				try {
					file.close();
				} catch (IOException e) {//ignore
				}
				file = null;
			}
		}
	}

	public void setFileDownloadListener(
			FileDownloadListener fileDownloadListener) {
		this.fileDownloadListener = fileDownloadListener;
	}

}
