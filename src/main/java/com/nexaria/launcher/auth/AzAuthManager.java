package com.nexaria.launcher.auth;

import com.nexaria.launcher.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    // Authentification via AzAuth HTTP API
    public User authenticate(String usernameOrEmail, String password, String twoFACode) throws AuthenticationException {
        try {
            logger.info("Authentification de l'utilisateur: {}", usernameOrEmail);
            if (usernameOrEmail.isEmpty() || password.isEmpty()) {
                throw new AuthenticationException("Identifiants manquants");
            }
            String base = azuriomUrl != null ? azuriomUrl.replaceAll("/+$", "") : "";
            HttpURLConnection conn = (HttpURLConnection) URI.create(base + "/api/auth/authenticate").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Gestion 2FA basique
            String code = twoFACode != null ? twoFACode : "";
            JsonObject payload = new JsonObject();
            payload.addProperty("email", usernameOrEmail);
            payload.addProperty("password", password);
            if (!code.isEmpty()) payload.addProperty("code", code);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(data);
            }

            int status = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            if (status == 200) {
                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                logger.debug("Réponse API Azuriom: {}", json.toString());
                
                int id = json.has("id") ? json.get("id").getAsInt() : -1;
                String siteUsername = json.has("username") ? json.get("username").getAsString() : usernameOrEmail;
                this.accessToken = json.has("access_token") ? json.get("access_token").getAsString() : UUID.randomUUID().toString();
                String uid = (id >= 0) ? String.valueOf(id) : UUID.randomUUID().toString();
                
                // Extraire les données supplémentaires de l'API Azuriom
                String uuid = json.has("uuid") ? json.get("uuid").getAsString() : null;
                boolean emailVerified = json.has("email_verified") && json.get("email_verified").getAsBoolean();
                double money = json.has("money") ? json.get("money").getAsDouble() : 0.0;
                
                String roleName = null;
                String roleColor = null;
                if (json.has("role") && json.get("role").isJsonObject()) {
                    JsonObject role = json.getAsJsonObject("role");
                    roleName = role.has("name") ? role.get("name").getAsString() : null;
                    roleColor = role.has("color") ? role.get("color").getAsString() : null;
                }
                
                boolean banned = json.has("banned") && json.get("banned").getAsBoolean();
                String createdAt = json.has("created_at") ? json.get("created_at").getAsString() : null;
                
                logger.info("Données utilisateur - UUID: {}, Role: {}, Money: {}, EmailVerified: {}", 
                           uuid, roleName, money, emailVerified);
                
                this.currentUser = new User(uid, siteUsername, accessToken, uuid, emailVerified, 
                                           money, roleName, roleColor, banned, createdAt);
                logger.info("Utilisateur authentifié: {}", currentUser.getUsername());
                return currentUser;
            } else {
                JsonObject err = null;
                try { err = JsonParser.parseString(sb.toString()).getAsJsonObject(); } catch (Exception ignore) {}
                String reason = err != null && err.has("reason") ? err.get("reason").getAsString() : "error";
                if ("pending".equalsIgnoreCase(reason) || (err != null && err.has("message") && err.get("message").getAsString().toLowerCase().contains("2fa"))) {
                    throw new TwoFactorRequiredException("Deux facteurs requis");
                }
                throw new AuthenticationException("Échec authentification: " + sb);
            }
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification", e);
            throw new AuthenticationException("Erreur d'authentification: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private String resolveUsername(String login) {
        try {
            // Si c'est déjà un pseudo (pas d'@), le retourner tel quel
            if (!login.contains("@")) {
                return login;
            }

            String encoded = URLEncoder.encode(login, StandardCharsets.UTF_8.name());
            String base = azuriomUrl != null ? azuriomUrl.replaceAll("/+$", "") : "";

            // Essais d'endpoints potentiels (dépend de la config Azuriom)
            String[] candidates = new String[] {
                base + "/api/users/by-email?email=" + encoded,
                base + "/api/user/by-email?email=" + encoded,
                base + "/api/users/resolve?email=" + encoded
            };

            for (String urlStr : candidates) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        String body = sb.toString();
                        // Extraction naïve du champ username sans lib JSON
                        String uname = extractField(body, "username");
                        if (uname != null && !uname.isBlank()) {
                            return uname;
                        }
                    }
                } catch (Exception inner) {
                    // Essayer le prochain endpoint
                }
            }
        } catch (Exception e) {
            logger.warn("Impossible de résoudre le nom d'utilisateur via l'API", e);
        }
        return null;
    }

    private String extractField(String json, String field) {
        try {
            // Très simple: recherche "field":"value" ou 'field':'value'
            java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\"(.*?)\"",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*'(.*?)'",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m1 = p1.matcher(json);
            if (m1.find()) return m1.group(1);
            java.util.regex.Matcher m2 = p2.matcher(json);
            if (m2.find()) return m2.group(1);
        } catch (Exception ignored) {}
        return null;
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

    // Upload Azuriom skin via multipart/form-data
    public void uploadSkin(String accessToken, java.io.File skinPng) throws Exception {
        String base = azuriomUrl != null ? azuriomUrl.replaceAll("/+$", "") : "";
        java.net.URL url = java.net.URI.create(base + "/api/skin-api/skins").toURL();
        String boundary = "----NexariaBoundary" + System.currentTimeMillis();
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
            // access_token field
            String part1 = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"access_token\"\r\n\r\n" +
                    accessToken + "\r\n";
            os.write(part1.getBytes(utf8));
            // skin file
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"skin\"; filename=\"skin.png\"\r\n" +
                    "Content-Type: image/png\r\n\r\n";
            os.write(header.getBytes(utf8));
            try (java.io.InputStream fis = new java.io.FileInputStream(skinPng)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) os.write(buf, 0, r);
            }
            os.write("\r\n".getBytes(utf8));
            // end
            String end = "--" + boundary + "--\r\n";
            os.write(end.getBytes(utf8));
        }
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new Exception("Échec upload skin: HTTP " + code);
        }
    }

    // Remote verification of access token
    public boolean verifyAccessTokenRemote(String token) {
        try {
            String base = azuriomUrl != null ? azuriomUrl.replaceAll("/+$", "") : "";
            HttpURLConnection conn = (HttpURLConnection) URI.create(base + "/api/auth/verify").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            String payload = "{\"access_token\":\"" + token + "\"}";
            try (OutputStream os = conn.getOutputStream()) { os.write(payload.getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            if (code == 200) {
                this.accessToken = token;
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
