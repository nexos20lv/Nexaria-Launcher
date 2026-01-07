package com.nexora.launcher.auth;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AzureAuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AzureAuthManager.class);
    
    // À configurer avec vos identifiants Azure
    private static final String TENANT_ID = "YOUR_TENANT_ID";
    private static final String CLIENT_ID = "YOUR_CLIENT_ID";
    private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET";
    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;
    
    private String accessToken;
    private long tokenExpirationTime;
    private UserProfile userProfile;
    
    public AzureAuthManager() {
        this.accessToken = null;
        this.tokenExpirationTime = 0;
    }

    /**
     * Obtient un token d'accès Azure AD
     */
    public String getAccessToken() throws AuthenticationException {
        // Vérifier si le token est toujours valide
        if (accessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            return accessToken;
        }

        try {
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(TENANT_ID)
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .build();

            // Obtenir le token
            var tokenResponse = credential.getToken(
                    com.azure.core.credential.TokenRequestContext.Builder
                            .createRequestContext()
                            .build()
            );

            this.accessToken = tokenResponse.getToken();
            // Token valable 1 heure par défaut, on retire 5 minutes pour la sécurité
            this.tokenExpirationTime = System.currentTimeMillis() + (55 * 60 * 1000);
            
            logger.info("Token d'accès obtenu avec succès");
            return accessToken;
        } catch (Exception e) {
            logger.error("Erreur lors de l'obtention du token Azure", e);
            throw new AuthenticationException("Impossible d'obtenir le token d'accès Azure", e);
        }
    }

    /**
     * Authentifier l'utilisateur et récupérer son profil
     */
    public UserProfile authenticate(String username, String password) throws AuthenticationException {
        try {
            // Obtenir le token
            String token = getAccessToken();
            
            // Récupérer les informations utilisateur
            userProfile = fetchUserProfile(token);
            
            logger.info("Utilisateur authentifié: {}", userProfile.getDisplayName());
            return userProfile;
        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification", e);
            throw new AuthenticationException("Erreur d'authentification", e);
        }
    }

    /**
     * Récupérer le profil utilisateur depuis Microsoft Graph
     */
    private UserProfile fetchUserProfile(String token) throws Exception {
        String url = "https://graph.microsoft.com/v1.0/me";
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("Content-Type", "application/json");

        var response = httpClient.execute(request, httpResponse -> {
            int statusCode = httpResponse.getCode();
            if (statusCode == 200) {
                String content = new String(httpResponse.getEntity().getContent().readAllBytes());
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(content, JsonObject.class);
                
                return new UserProfile(
                        json.get("id").getAsString(),
                        json.get("userPrincipalName").getAsString(),
                        json.get("displayName").getAsString(),
                        json.get("mail") != null ? json.get("mail").getAsString() : ""
                );
            } else {
                throw new AuthenticationException("Erreur serveur: " + statusCode);
            }
        });

        return response;
    }

    /**
     * Vérifier si l'utilisateur est authentifié
     */
    public boolean isAuthenticated() {
        return accessToken != null && System.currentTimeMillis() < tokenExpirationTime;
    }

    /**
     * Déconnecter l'utilisateur
     */
    public void logout() {
        this.accessToken = null;
        this.tokenExpirationTime = 0;
        this.userProfile = null;
        logger.info("Utilisateur déconnecté");
    }

    /**
     * Récupérer le profil utilisateur actuel
     */
    public UserProfile getUserProfile() {
        return userProfile;
    }

    /**
     * Classe représentant le profil utilisateur
     */
    public static class UserProfile {
        private String id;
        private String email;
        private String displayName;
        private String mailAddress;

        public UserProfile(String id, String email, String displayName, String mailAddress) {
            this.id = id;
            this.email = email;
            this.displayName = displayName;
            this.mailAddress = mailAddress;
        }

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getMailAddress() { return mailAddress; }
    }
}
