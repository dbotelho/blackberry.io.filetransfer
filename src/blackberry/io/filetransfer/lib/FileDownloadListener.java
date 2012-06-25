package blackberry.io.filetransfer.lib;

public abstract class FileDownloadListener {
	
	public abstract void notifyDownloadProgress(final int totalsize,final int chunck);
	public abstract void notifyDownloadFinnish();
	public abstract void notifyDownloadError(final String error);

}
