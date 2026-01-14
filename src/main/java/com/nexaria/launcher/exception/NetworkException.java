package com.nexaria.launcher.exception;

/**
 * Exception levée lors d'erreurs réseau (timeout, connexion, etc.)
 */
public class NetworkException extends LauncherException {
    private final String endpoint;
    private final int statusCode;

    public NetworkException(String message) {
        this(message, null, null, -1);
    }

    public NetworkException(String message, Throwable cause) {
        this(message, cause, -1);
    }

    public NetworkException(String message, String endpoint, int statusCode) {
        this(message, null, endpoint, statusCode);
    }

    public NetworkException(String message, Throwable cause, int statusCode) {
        this(message, cause, null, statusCode);
    }

    public NetworkException(String message, Throwable cause, String endpoint, int statusCode) {
        super(message, cause, ErrorSeverity.ERROR, "NET_ERROR");
        this.endpoint = endpoint;
        this.statusCode = statusCode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (endpoint != null) {
            sb.append(" | Endpoint: ").append(endpoint);
        }
        if (statusCode > 0) {
            sb.append(" | HTTP ").append(statusCode);
        }
        return sb.toString();
    }
}
