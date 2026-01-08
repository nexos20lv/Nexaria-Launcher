package com.nexaria.launcher.auth;

public class TwoFactorRequiredException extends AuthenticationException {
    public TwoFactorRequiredException(String message) {
        super(message);
    }
}
