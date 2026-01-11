package com.nexaria.launcher.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RememberStore {
    private static final Gson gson = new Gson();

    private static File getStoreFile() {
        // Utilise le cache runtime (AppData), pas le workspace
        String base = LauncherConfig.getCacheDir();
        return new File(base + File.separator + "remember.json");
    }

    public static void saveUsername(String username) {
        try {
            ensureParent();
            Map<String, Object> m = new HashMap<>();
            m.put("username", username);
            String json = gson.toJson(m);
            String encoded = java.util.Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            try (Writer w = new OutputStreamWriter(new FileOutputStream(getStoreFile()), StandardCharsets.UTF_8)) {
                w.write(encoded);
            }
        } catch (Exception ignored) {
        }
    }

    public static String loadUsername() {
        try {
            File storeFile = getStoreFile();
            if (!storeFile.exists())
                return null;
            Type t = new TypeToken<Map<String, Object>>() {
            }.getType();
            // Lire tout le fichier, décoder Base64, puis parser JSON
            byte[] fileBytes = Files.readAllBytes(storeFile.toPath());
            String encoded = new String(fileBytes, StandardCharsets.UTF_8);

            // Tenter de décoder (si c'est l'ancien format JSON clair, ça va échouer ou
            // donner du garbage, on gère l'exception)
            String json;
            try {
                json = new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // Fallback: peut-être l'ancien format non encodé ?
                json = encoded;
            }

            Map<String, Object> m = gson.fromJson(json, t);
            if (m != null) {
                Object v = m.get("username");
                return v != null ? String.valueOf(v) : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // Nouvelle méthode: Sauvegarder potentiellement plusieurs sessions
    // On garde l'ancienne méthode pour compatibilité, mais elle écrasera avec une
    // liste de 1 élément
    public static void saveSession(String id, String username, String accessToken) {
        addOrUpdateSession(new RememberSession(id, username, accessToken));
    }

    public static void addOrUpdateSession(RememberSession session) {
        try {
            ensureParent();
            java.util.List<RememberSession> sessions = loadSessions();
            if (sessions == null)
                sessions = new java.util.ArrayList<>();

            // Remove existing session with same ID or Username to update it
            sessions.removeIf(s -> (s.id != null && s.id.equals(session.id))
                    || (s.username != null && s.username.equals(session.username)));

            // Add as first (most recent)
            sessions.add(0, session);

            saveSessions(sessions);
        } catch (Exception ignored) {
        }
    }

    public static void removeSession(String username) {
        try {
            java.util.List<RememberSession> sessions = loadSessions();
            if (sessions != null) {
                sessions.removeIf(s -> s.username != null && s.username.equals(username));
                saveSessions(sessions);
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveSessions(java.util.List<RememberSession> sessions) {
        try {
            ensureParent();
            // Wrap in a map for future extensibility
            Map<String, Object> root = new HashMap<>();
            root.put("sessions", sessions); // Nouvelle structure
            // Backwards compat: store "primary" fields at root so old launchers might still
            // read something (though risk of conflict)
            // Better behavior: Just switch to new format. Old launcher reading this will
            // likely fail or see nulls.
            // Since we are upgrading the launcher, we define the format.

            String json = gson.toJson(root);
            String encoded = java.util.Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            try (Writer w = new OutputStreamWriter(new FileOutputStream(getStoreFile()), StandardCharsets.UTF_8)) {
                w.write(encoded);
            }
        } catch (Exception ignored) {
        }
    }

    public static java.util.List<RememberSession> loadSessions() {
        try {
            File storeFile = getStoreFile();
            if (!storeFile.exists())
                return new java.util.ArrayList<>();

            Type t = new TypeToken<Map<String, Object>>() {
            }.getType();
            byte[] fileBytes = Files.readAllBytes(storeFile.toPath());
            String encoded = new String(fileBytes, StandardCharsets.UTF_8);
            String json;
            try {
                json = new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                json = encoded;
            }

            Map<String, Object> root = gson.fromJson(json, t);
            if (root != null) {
                // Check if it's the new format
                if (root.containsKey("sessions")) {
                    String sessionsJson = gson.toJson(root.get("sessions"));
                    Type listType = new TypeToken<java.util.List<RememberSession>>() {
                    }.getType();
                    return gson.fromJson(sessionsJson, listType);
                } else {
                    // Old format (single session at root)
                    RememberSession s = new RememberSession();
                    Object id = root.get("id");
                    Object un = root.get("username");
                    Object at = root.get("access_token");
                    if (at != null) {
                        s.id = id != null ? String.valueOf(id) : null;
                        s.username = un != null ? String.valueOf(un) : null;
                        s.accessToken = at != null ? String.valueOf(at) : null;
                        java.util.List<RememberSession> list = new java.util.ArrayList<>();
                        list.add(s);
                        return list;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new java.util.ArrayList<>();
    }

    // Garder loadSession() pour compatibilité: retourne le premier (le plus récent)
    public static RememberSession loadSession() {
        java.util.List<RememberSession> list = loadSessions();
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public static void clear() {
        try {
            File storeFile = getStoreFile();
            if (storeFile.exists())
                storeFile.delete();
        } catch (Exception ignored) {
        }
    }

    private static void ensureParent() {
        File parent = getStoreFile().getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();
    }

    public static class RememberSession {
        @com.google.gson.annotations.SerializedName("id")
        public String id;
        @com.google.gson.annotations.SerializedName("username")
        public String username;
        @com.google.gson.annotations.SerializedName("access_token")
        public String accessToken;

        public RememberSession() {
        }

        public RememberSession(String id, String username, String accessToken) {
            this.id = id;
            this.username = username;
            this.accessToken = accessToken;
        }
    }
}
