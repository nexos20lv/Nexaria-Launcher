package com.nexora.launcher.auth;

import com.nexora.launcher.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AzAuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AzAuthManager.class);

    private String azuriomUrl;
    private User currentUser;
    private String accessToken;

    public AzAuthManager(String azuriomUrl) {
        this.azuriomUrl = azuriomUrl;
        this.currentUser = null;
        this.accessToken = null;
    }

    // Simulation d'authentification Azuriom (placeholder sans dépendance externe)
    public User authenticate(String username, String password, String twoFACode) throws AuthenticationException {
        try {
            logger.info("Authentification de l'utilisateur: {}", username);
            if (username.isEmpty() || password.isEmpty()) {
                throw new AuthenticationException("Identifiants manquants");
            }
            // Générer un token simple et un id fictif
            this.accessToken = UUID.randomUUID().toString();
            this.currentUser = new User(UUID.randomUUID().toString(), username, accessToken);
            logger.info("Utilisateur authentifié: {}", currentUser.getUsername());
            return currentUser;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification", e);
            throw new AuthenticationException("Erreur d'authentification: " + e.getMessage(), e);
        }
    }

    public boolean verifyToken(String token) {
        return token != null && token.equals(this.accessToken);
    }

    public boolean isAuthenticated() {
        return currentUser != null && accessToken != null;
    }

    public void logout() {
        this.currentUser = null;
        this.accessToken = null;
        logger.info("Utilisateur déconnecté");
    }

    public User getCurrentUser() { return currentUser; }
    public String getAccessToken() { return accessToken; }
    public void setAzuriomUrl(String url) { this.azuriomUrl = url; }
    public String getAzuriomUrl() { return azuriomUrl; }
}
