package com.nexora.launcher.auth;

import com.azuriom.auth.AuthClient;
import com.azuriom.auth.User;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzAuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AzAuthManager.class);
    
    private String azuriomUrl;
    private AuthClient authClient;
    private User currentUser;
    private String accessToken;

    public AzAuthManager(String azuriomUrl) {
        this.azuriomUrl = azuriomUrl;
        this.authClient = new AuthClient(azuriomUrl);
        this.currentUser = null;
        this.accessToken = null;
    }

    /**
     * Authentifier l'utilisateur avec ses identifiants Azuriom
     */
    public User authenticate(String username, String password, String twoFACode) throws AuthenticationException {
        try {
            logger.info("Authentification de l'utilisateur: {}", username);
            
            // Authentifier avec AzAuth
            User user = authClient.login(username, password, () -> {
                if (twoFACode != null && !twoFACode.isEmpty()) {
                    return twoFACode;
                }
                return null;
            });

            this.currentUser = user;
            this.accessToken = user.getAccessToken();
            
            logger.info("Utilisateur authentifié: {}", user.getUsername());
            return user;
        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification", e);
            throw new AuthenticationException("Erreur d'authentification: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifier si le token est toujours valide
     */
    public boolean verifyToken(String token) throws AuthenticationException {
        try {
            logger.info("Vérification du token d'accès");
            
            // Vérifier via le serveur Azuriom
            User user = authClient.verify(token);
            this.currentUser = user;
            this.accessToken = token;
            
            logger.info("Token vérifié pour l'utilisateur: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            logger.warn("Token invalide ou expiré", e);
            return false;
        }
    }

    /**
     * Vérifier si l'utilisateur est authentifié
     */
    public boolean isAuthenticated() {
        return currentUser != null && accessToken != null;
    }

    /**
     * Déconnecter l'utilisateur
     */
    public void logout() throws AuthenticationException {
        try {
            if (accessToken != null) {
                logger.info("Déconnexion de l'utilisateur");
                authClient.logout(accessToken);
                this.currentUser = null;
                this.accessToken = null;
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la déconnexion", e);
            throw new AuthenticationException("Erreur lors de la déconnexion", e);
        }
    }

    /**
     * Récupérer l'utilisateur actuel
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Récupérer le token d'accès
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Définir l'URL d'Azuriom
     */
    public void setAzuriomUrl(String url) {
        this.azuriomUrl = url;
        this.authClient = new AuthClient(url);
    }

    /**
     * Récupérer l'URL d'Azuriom
     */
    public String getAzuriomUrl() {
        return azuriomUrl;
    }
}
