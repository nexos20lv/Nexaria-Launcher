package com.nexora.launcher.auth;

// Stub minimal pour supprimer la dépendance Azure inutilisée
public class AzureAuthManager {
    public static class UserProfile { }
    public String getAccessToken() { return null; }
    public UserProfile authenticate(String username, String password) { return null; }
    public boolean isAuthenticated() { return false; }
    public void logout() { }
    public UserProfile getUserProfile() { return null; }
}
