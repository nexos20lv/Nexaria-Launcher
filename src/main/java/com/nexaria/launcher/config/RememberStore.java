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

    public static void saveSession(String id, String username, String accessToken) {
        try {
            ensureParent();
            Map<String, Object> m = new HashMap<>();
            if (id != null)
                m.put("id", id);
            if (username != null)
                m.put("username", username);
            if (accessToken != null)
                m.put("access_token", accessToken);

            String json = gson.toJson(m);
            String encoded = java.util.Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            try (Writer w = new OutputStreamWriter(new FileOutputStream(getStoreFile()), StandardCharsets.UTF_8)) {
                w.write(encoded);
            }
        } catch (Exception ignored) {
        }
    }

    public static RememberSession loadSession() {
        try {
            File storeFile = getStoreFile();
            if (!storeFile.exists())
                return null;
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

            Map<String, Object> m = gson.fromJson(json, t);
            if (m != null) {
                RememberSession s = new RememberSession();
                Object id = m.get("id");
                Object un = m.get("username");
                Object at = m.get("access_token");
                s.id = id != null ? String.valueOf(id) : null;
                s.username = un != null ? String.valueOf(un) : null;
                s.accessToken = at != null ? String.valueOf(at) : null;
                return s;
            }
        } catch (Exception ignored) {
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
        public String id;
        public String username;
        public String accessToken;
    }
}
