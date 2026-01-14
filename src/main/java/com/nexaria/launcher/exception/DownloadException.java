package com.nexaria.launcher.exception;

/**
 * Exception levée lors d'erreurs de téléchargement de fichiers.
 */
public class DownloadException extends LauncherException {
    private final String url;
    private final String destination;
    private final long bytesDownloaded;
    private final long totalBytes;

    public DownloadException(String message) {
        this(message, null, null, null, 0, 0);
    }

    public DownloadException(String message, Throwable cause) {
        this(message, cause, null, null, 0, 0);
    }

    public DownloadException(String message, String url, String destination) {
        this(message, null, url, destination, 0, 0);
    }

    public DownloadException(String message, Throwable cause, String url, String destination, 
                            long bytesDownloaded, long totalBytes) {
        super(message, cause, ErrorSeverity.ERROR, "DOWNLOAD_ERROR");
        this.url = url;
        this.destination = destination;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytes = totalBytes;
    }

    public String getUrl() {
        return url;
    }

    public String getDestination() {
        return destination;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public double getProgressPercentage() {
        return totalBytes > 0 ? (bytesDownloaded * 100.0 / totalBytes) : 0.0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (url != null) {
            sb.append(" | URL: ").append(url);
        }
        if (totalBytes > 0) {
            sb.append(String.format(" | Progress: %.1f%% (%d/%d bytes)", 
                    getProgressPercentage(), bytesDownloaded, totalBytes));
        }
        return sb.toString();
    }
}
