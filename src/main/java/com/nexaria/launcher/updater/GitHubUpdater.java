package com.nexaria.launcher.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Gère les mises à jour du launcher via GitHub Releases
 */
public class GitHubUpdater {
    private static final Logger logger = LoggerFactory.getLogger(GitHubUpdater.class);
    
    private final String githubRepo;
    private final String currentVersion;

    public GitHubUpdater(String githubRepo, String currentVersion) {
        this.githubRepo = githubRepo;
        this.currentVersion = currentVersion;
    }

    /**
     * Récupère la dernière version depuis GitHub Releases
     */
    public GitHubRelease getLatestRelease() throws Exception {
        String apiUrl = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
        logger.info("Vérification des mises à jour: {}", apiUrl);

        HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setConnectTimeout(5000);

        try {
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("GitHub API error: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            String tagName = json.get("tag_name").getAsString();
            String downloadUrl = "";
            
            // Trouver le fichier JAR dans les assets
            JsonArray assets = json.getAsJsonArray("assets");
            for (JsonElement asset : assets) {
                JsonObject assetObj = asset.getAsJsonObject();
                String name = assetObj.get("name").getAsString();
                if (name.endsWith(".jar")) {
                    downloadUrl = assetObj.get("browser_download_url").getAsString();
                    break;
                }
            }

            return new GitHubRelease(tagName, downloadUrl, json.get("body").getAsString());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Vérifie s'il y a une mise à jour disponible
     */
    public boolean hasUpdate() throws Exception {
        try {
            GitHubRelease latest = getLatestRelease();
            String latestVersion = latest.tagName.replaceFirst("^v", "");
            
            return isNewerVersion(latestVersion, currentVersion);
        } catch (Exception e) {
            logger.warn("Erreur lors de la vérification des mises à jour", e);
            return false;
        }
    }

    /**
     * Télécharge la mise à jour depuis GitHub
     */
    public void downloadUpdate(GitHubRelease release, String destination) throws Exception {
        logger.info("Téléchargement de la mise à jour: {}", release.downloadUrl);
        
        HttpURLConnection conn = (HttpURLConnection) URI.create(release.downloadUrl).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setInstanceFollowRedirects(true);

        try {
            if (conn.getResponseCode() != 200) {
                throw new IOException("Erreur lors du téléchargement: " + conn.getResponseCode());
            }

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(destination)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("Mise à jour téléchargée: {}", destination);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Installe la mise à jour (compatible Windows, macOS, Linux)
     */
    public static void installUpdate(String newJarPath) throws Exception {
        logger.info("Installation de la mise à jour");
        
        String currentJar = new File(GitHubUpdater.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsolutePath();
        
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows : Script BAT
            String scriptPath = LauncherConfig.getLauncherDir() + "/update.bat";
            
            StringBuilder script = new StringBuilder();
            script.append("@echo off\n");
            script.append("timeout /t 2 /nobreak > nul\n");
            script.append("del /f \"").append(currentJar).append("\"\n");
            script.append("move /y \"").append(newJarPath).append("\" \"").append(currentJar).append("\"\n");
            script.append("start \"\" javaw -jar \"").append(currentJar).append("\"\n");
            script.append("del \"%~f0\"\n"); // Supprime le script lui-même
            
            Files.write(Paths.get(scriptPath), script.toString().getBytes());
            Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", scriptPath});
        } else {
            // macOS / Linux : Script Shell
            String scriptPath = LauncherConfig.getLauncherDir() + "/update.sh";
            
            StringBuilder script = new StringBuilder();
            script.append("#!/bin/bash\n");
            script.append("sleep 2\n");
            script.append("rm -f \"").append(currentJar).append("\"\n");
            script.append("mv -f \"").append(newJarPath).append("\" \"").append(currentJar).append("\"\n");
            
            // Détecter le chemin Java
            if (os.contains("mac")) {
                script.append("# macOS\n");
                script.append("java -jar \"").append(currentJar).append("\" &\n");
            } else {
                script.append("# Linux\n");
                script.append("java -jar \"").append(currentJar).append("\" &\n");
            }
            
            script.append("rm -f \"$0\"\n"); // Supprime le script lui-même
            
            File scriptFile = new File(scriptPath);
            Files.write(scriptFile.toPath(), script.toString().getBytes());
            scriptFile.setExecutable(true);
            
            Runtime.getRuntime().exec(new String[]{"/bin/sh", scriptPath});
        }
        
        logger.info("Mise à jour lancée, redémarrage...");
        System.exit(0);
    }

    /**
     * Compare deux versions (ex: 1.0.1 > 1.0.0)
     */
    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latest = latestVersion.split("\\.");
        String[] current = currentVersion.split("\\.");
        
        for (int i = 0; i < Math.max(latest.length, current.length); i++) {
            int latestPart = i < latest.length ? Integer.parseInt(latest[i]) : 0;
            int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
            
            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false;
    }

    /**
     * Classe pour représenter une release GitHub
     */
    public static class GitHubRelease {
        public String tagName;
        public String downloadUrl;
        public String changelog;

        public GitHubRelease(String tagName, String downloadUrl, String changelog) {
            this.tagName = tagName;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
        }
    }
}
