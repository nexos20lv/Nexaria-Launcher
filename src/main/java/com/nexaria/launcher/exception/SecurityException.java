package com.nexaria.launcher.exception;

/**
 * Exception levée lors de violations de sécurité (anti-cheat, intégrité, etc.)
 */
public class SecurityException extends LauncherException {
    private final SecurityViolationType violationType;
    private final String details;

    public SecurityException(String message) {
        this(message, null, SecurityViolationType.UNKNOWN, null);
    }

    public SecurityException(String message, SecurityViolationType violationType) {
        this(message, null, violationType, null);
    }

    public SecurityException(String message, Throwable cause, SecurityViolationType violationType, String details) {
        super(message, cause, ErrorSeverity.CRITICAL, "SECURITY_VIOLATION");
        this.violationType = violationType;
        this.details = details;
    }

    public SecurityViolationType getViolationType() {
        return violationType;
    }

    public String getDetails() {
        return details;
    }

    public enum SecurityViolationType {
        INTEGRITY_VIOLATION,
        BANNED_PROCESS_DETECTED,
        INVALID_SIGNATURE,
        TAMPERED_FILES,
        SYMLINK_BLOCKED,
        UNKNOWN
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" | Type: ").append(violationType);
        if (details != null) {
            sb.append(" | Details: ").append(details);
        }
        return sb.toString();
    }
}
