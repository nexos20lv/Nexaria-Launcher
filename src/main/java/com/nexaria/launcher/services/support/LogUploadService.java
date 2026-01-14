package com.nexaria.launcher.services.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Service pour envoyer des logs vers mclo.gs
 */
public class LogUploadService {
    private static final Logger logger = LoggerFactory.getLogger(LogUploadService.class);
    private static final String API_URL = "https://api.mclo.gs/1/log";
    private final Gson gson = new Gson();

    public String uploadLog(File logFile) throws IOException {
        if (logFile == null || !logFile.exists()) {
            throw new IOException("Fichier de log introuvable");
        }

        String content = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
        return uploadContent(content);
    }

    public String uploadContent(String content) throws IOException {
        logger.info("Envoi des logs vers mclo.gs...");

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        String data = "content=" + URLEncoder.encode(content, StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Erreur upload mclo.gs (Code " + code + ")");
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String response = br.lines().collect(Collectors.joining("\n"));
            logger.debug("Réponse mclo.gs: {}", response);

            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    String logUrl = json.get("url").getAsString();
                    logger.info("Log uploadé avec succès: {}", logUrl);
                    return logUrl;
                } else {
                    throw new IOException("Erreur de l'API mclo.gs: " + response);
                }
            } catch (Exception e) {
                logger.error("Erreur parsing JSON", e);
                throw new IOException("Réponse invalide de mclo.gs: " + response, e);
            }
        }
    }
}
