package com.nexaria.launcher.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
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
            try (Writer w = new OutputStreamWriter(new FileOutputStream(getStoreFile()), StandardCharsets.UTF_8)) {
                gson.toJson(m, w);
            }
        } catch (Exception ignored) {}
    }

    public static String loadUsername() {
        try {
            File storeFile = getStoreFile();
            if (!storeFile.exists()) return null;
            Type t = new TypeToken<Map<String, Object>>(){}.getType();
            try (Reader r = new InputStreamReader(new FileInputStream(storeFile), StandardCharsets.UTF_8)) {
                Map<String, Object> m = gson.fromJson(r, t);
                Object v = m.get("username");
                return v != null ? String.valueOf(v) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void saveSession(String id, String username, String accessToken) {
        try {
            ensureParent();
            Map<String, Object> m = new HashMap<>();
            if (id != null) m.put("id", id);
            if (username != null) m.put("username", username);
            if (accessToken != null) m.put("access_token", accessToken);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(getStoreFile()), StandardCharsets.UTF_8)) {
                gson.toJson(m, w);
            }
        } catch (Exception ignored) {}
    }

    public static RememberSession loadSession() {
        try {
            File storeFile = getStoreFile();
            if (!storeFile.exists()) return null;
            Type t = new TypeToken<Map<String, Object>>(){}.getType();
            try (Reader r = new InputStreamReader(new FileInputStream(storeFile), StandardCharsets.UTF_8)) {
                Map<String, Object> m = gson.fromJson(r, t);
                RememberSession s = new RememberSession();
                Object id = m.get("id");
                Object un = m.get("username");
                Object at = m.get("access_token");
                s.id = id != null ? String.valueOf(id) : null;
                s.username = un != null ? String.valueOf(un) : null;
                s.accessToken = at != null ? String.valueOf(at) : null;
                return s;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void clear() {
        try { 
            File storeFile = getStoreFile();
            if (storeFile.exists()) storeFile.delete(); 
        } catch (Exception ignored) {}
    }

    private static void ensureParent() {
        File parent = getStoreFile().getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    public static class RememberSession {
        public String id;
        public String username;
        public String accessToken;
    }
}
