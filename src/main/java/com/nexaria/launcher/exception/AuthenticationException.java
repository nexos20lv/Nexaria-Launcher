package com.nexaria.launcher.exception;

/**
 * Exception levée lors d'erreurs d'authentification Azuriom.
 */
public class AuthenticationException extends LauncherException {
    private final AuthErrorType errorType;
    private final String username;

    public AuthenticationException(String message) {
        this(message, null, AuthErrorType.UNKNOWN);
    }

    public AuthenticationException(String message, AuthErrorType errorType) {
        this(message, null, errorType, null);
    }

    public AuthenticationException(String message, Throwable cause, AuthErrorType errorType) {
        this(message, cause, errorType, null);
    }

    public AuthenticationException(String message, Throwable cause, AuthErrorType errorType, String username) {
        super(message, cause, ErrorSeverity.ERROR, "AUTH_ERROR");
        this.errorType = errorType;
        this.username = username;
    }

    public AuthErrorType getErrorType() {
        return errorType;
    }

    public String getUsername() {
        return username;
    }

    public enum AuthErrorType {
        INVALID_CREDENTIALS,
        TWO_FACTOR_REQUIRED,
        ACCOUNT_SUSPENDED,
        SERVER_UNREACHABLE,
        INVALID_TOKEN,
        TOKEN_EXPIRED,
        UNKNOWN
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" | Type: ").append(errorType);
        if (username != null) {
            sb.append(" | User: ").append(username);
        }
        return sb.toString();
    }
}
