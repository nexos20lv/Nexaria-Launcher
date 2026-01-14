package com.nexaria.launcher.services.auth;

public class TwoFactorRequiredException extends AuthenticationException {
    public TwoFactorRequiredException(String message) {
        super(message);
    }
}
