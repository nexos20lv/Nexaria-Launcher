package com.nexaria.launcher.exception;

/**
 * Exception de base pour toutes les exceptions métier du launcher.
 * Permet une gestion centralisée et un traçage unifié.
 */
public class LauncherException extends Exception {
    private final String errorCode;
    private final ErrorSeverity severity;

    public LauncherException(String message) {
        this(message, null, ErrorSeverity.ERROR);
    }

    public LauncherException(String message, Throwable cause) {
        this(message, cause, ErrorSeverity.ERROR);
    }

    public LauncherException(String message, ErrorSeverity severity) {
        this(message, null, severity);
    }

    public LauncherException(String message, Throwable cause, ErrorSeverity severity) {
        this(message, cause, severity, null);
    }

    public LauncherException(String message, Throwable cause, ErrorSeverity severity, String errorCode) {
        super(message, cause);
        this.severity = severity;
        this.errorCode = errorCode;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public enum ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("]");
        if (errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }
        sb.append(" ").append(getMessage());
        return sb.toString();
    }
}
