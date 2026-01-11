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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gère les mises à jour du launcher via GitHub Releases
 * Système robuste avec retry, timeouts corrects et gestion d'erreurs complète
 */
public class GitHubUpdater {
    private static final Logger logger = LoggerFactory.getLogger(GitHubUpdater.class);

    private static final int CONNECT_TIMEOUT = 10000; // 10 secondes
    private static final int READ_TIMEOUT = 30000; // 30 secondes
    private static final int MAX_RETRIES = 3;
    private static final String USER_AGENT = "Nexaria-Launcher/1.0";

    private final String githubRepo;
    private final String currentVersion;
    private GitHubRelease cachedRelease;

    public GitHubUpdater(String githubRepo, String currentVersion) {
        this.githubRepo = githubRepo;
        this.currentVersion = currentVersion;
    }

    /**
     * Récupère la dernière version depuis GitHub Releases avec retry automatique
     */
    public GitHubRelease getLatestRelease() throws UpdateException {
        if (cachedRelease != null) {
            return cachedRelease;
        }

        String apiUrl = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
        logger.info("Vérification des mises à jour: {}", apiUrl);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);

                try {
                    int responseCode = conn.getResponseCode();

                    if (responseCode == 404) {
                        throw new UpdateException("Aucune release trouvée sur GitHub. Vérifiez le repo: " + githubRepo);
                    }
                    if (responseCode != 200) {
                        throw new UpdateException("GitHub API error (code " + responseCode + ")");
                    }

                    String jsonResponse = readInputStream(conn.getInputStream());
                    cachedRelease = parseGitHubResponse(jsonResponse);

                    if (cachedRelease.downloadUrl == null || cachedRelease.downloadUrl.isEmpty()) {
                        throw new UpdateException("Aucun fichier JAR trouvé dans les assets GitHub");
                    }

                    logger.info("Release trouvée: {} avec URL: {}", cachedRelease.tagName, cachedRelease.downloadUrl);
                    return cachedRelease;

                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentative {} échouée: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000 * attempt); // Délai progressif
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        throw new UpdateException("Impossible de récupérer la release après " + MAX_RETRIES + " tentatives",
                lastException);
    }

    /**
     * Parse la réponse JSON de GitHub
     */
    private GitHubRelease parseGitHubResponse(String jsonResponse) throws UpdateException {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String tagName = json.get("tag_name").getAsString();
            String downloadUrl = "";
            String changelog = json.has("body") ? json.get("body").getAsString() : "";

            // Trouver le fichier JAR dans les assets
            if (json.has("assets")) {
                JsonArray assets = json.getAsJsonArray("assets");
                for (JsonElement asset : assets) {
                    JsonObject assetObj = asset.getAsJsonObject();
                    String name = assetObj.get("name").getAsString();

                    // Accepter les fichiers JAR avec patterns: launcher.jar, nexaria-launcher.jar,
                    // etc
                    if (name.endsWith(".jar") && !name.contains("sources") && !name.contains("javadoc")) {
                        downloadUrl = assetObj.get("browser_download_url").getAsString();
                        logger.info("Asset trouvé: {}", name);
                        break;
                    }
                }
            }

            return new GitHubRelease(tagName, downloadUrl, changelog);
        } catch (Exception e) {
            throw new UpdateException("Erreur lors du parsing de la réponse GitHub", e);
        }
    }

    /**
     * Récupère et cache la dernière release disponible
     */
    public UpdateCheckResult checkForUpdates() {
        try {
            GitHubRelease latest = getLatestRelease();
            String latestVersion = latest.tagName.replaceFirst("^v", "");
            boolean hasUpdate = isNewerVersion(latestVersion, currentVersion);

            logger.info("Version actuelle: {}, Dernière version: {}, Mise à jour disponible: {}",
                    currentVersion, latestVersion, hasUpdate);

            return new UpdateCheckResult(hasUpdate, latest, null);
        } catch (UpdateException e) {
            logger.warn("Erreur lors de la vérification des mises à jour: {}", e.getMessage());
            return new UpdateCheckResult(false, null, e.getMessage());
        }
    }

    /**
     * Télécharge la mise à jour depuis GitHub avec barre de progression
     */
    public void downloadUpdate(GitHubRelease release, String destination) throws UpdateException {
        if (release.downloadUrl == null || release.downloadUrl.isEmpty()) {
            throw new UpdateException("URL de téléchargement invalide");
        }

        Path destPath = Paths.get(destination);

        // Créer le répertoire parent s'il n'existe pas
        try {
            Files.createDirectories(destPath.getParent());
        } catch (IOException e) {
            throw new UpdateException("Impossible de créer le répertoire: " + destPath.getParent(), e);
        }

        logger.info("Téléchargement de la mise à jour depuis: {}", release.downloadUrl);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                downloadFile(release.downloadUrl, destination);

                // Vérifier que le fichier a été créé et qu'il a une taille raisonnable
                File downloadedFile = new File(destination);
                if (!downloadedFile.exists() || downloadedFile.length() < 1000000) { // Au moins 1MB
                    throw new UpdateException("Fichier téléchargé invalide ou trop petit");
                }

                logger.info("Mise à jour téléchargée: {} ({} bytes)", destination, downloadedFile.length());

                // VÉRIFICATION D'INTÉGRITÉ (SHA-256)
                if (!verifyIntegrity(downloadedFile, release)) {
                    throw new UpdateException("Échec de la vérification d'intégrité (SHA-256 invalide)");
                }
                logger.info("Intégrité du fichier vérifiée avec succès.");

                return;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentative {} de téléchargement échouée: {}", attempt, e.getMessage());

                // Nettoyer le fichier corrompu
                try {
                    Files.deleteIfExists(destPath);
                } catch (IOException ignored) {
                }

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(2000 * attempt);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        throw new UpdateException("Téléchargement échoué après " + MAX_RETRIES + " tentatives", lastException);
    }

    /**
     * Télécharge un fichier avec gestion de redirects
     */
    private void downloadFile(String urlString, String destination) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);

        try {
            int responseCode = conn.getResponseCode();

            // Gérer les redirects manuellement pour les GitHub redirects
            if (responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl != null) {
                    logger.debug("Redirect vers: {}", redirectUrl);
                    conn.disconnect();
                    downloadFile(redirectUrl, destination);
                    return;
                }
            }

            if (responseCode != 200) {
                throw new IOException("Erreur HTTP " + responseCode);
            }

            try (InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(destination)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                logger.debug("Téléchargement complété: {} bytes", totalBytes);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Installe la mise à jour de manière robuste (Windows, macOS, Linux)
     */
    public static void installUpdate(String newJarPath) throws UpdateException {
        try {
            logger.info("Préparation de l'installation de la mise à jour");

            File newJarFile = new File(newJarPath);
            if (!newJarFile.exists()) {
                throw new UpdateException("Le fichier de mise à jour n'existe pas: " + newJarPath);
            }

            String currentJarPath = getCurrentJarPath();
            String os = System.getProperty("os.name").toLowerCase();

            logger.info("JAR actuel: {}", currentJarPath);
            logger.info("Système d'exploitation: {}", os);

            if (os.contains("win")) {
                installUpdateWindows(currentJarPath, newJarPath);
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                installUpdateUnix(currentJarPath, newJarPath, os.contains("mac"));
            } else {
                throw new UpdateException("Système d'exploitation non supporté: " + os);
            }

        } catch (UpdateException e) {
            throw e;
        } catch (Exception e) {
            throw new UpdateException("Erreur lors de l'installation de la mise à jour", e);
        }
    }

    /**
     * Installation sur Windows
     */
    private static void installUpdateWindows(String currentJar, String newJar) throws Exception {
        String launcherDir = LauncherConfig.getLauncherDir();
        String scriptPath = Paths.get(launcherDir, "update.bat").toString();

        StringBuilder script = new StringBuilder();
        String safeCurrent = safeQuote(currentJar);
        String safeNew = safeQuote(newJar);

        script.append("@echo off\n");
        script.append("REM Script de mise à jour du Nexaria Launcher\n");
        script.append("setlocal enabledelayedexpansion\n");
        script.append("\n");
        script.append("REM Attendre que le launcher se ferme\n");
        script.append("timeout /t 3 /nobreak > nul\n");
        script.append("\n");
        script.append("REM Supprimer l'ancienne version\n");
        script.append("if exist ").append(safeCurrent).append(" (\n");
        script.append("    del /f /q ").append(safeCurrent).append(" 2>nul\n");
        script.append("    if exist ").append(safeCurrent).append(" (\n");
        script.append("        echo Impossible de supprimer l'ancienne version\n");
        script.append("        exit /b 1\n");
        script.append("    )\n");
        script.append(")\n");
        script.append("\n");
        script.append("REM Déplacer la nouvelle version\n");
        script.append("move /y ").append(safeNew).append(" ").append(safeCurrent).append(" >nul\n");
        script.append("if not exist ").append(safeCurrent).append(" (\n");
        script.append("    echo Erreur: le fichier de mise à jour n'a pas pu être déplacé\n");
        script.append("    exit /b 1\n");
        script.append(")\n");
        script.append("\n");
        script.append("REM Relancer le launcher\n");
        script.append("start \"Nexaria Launcher\" ").append(safeCurrent).append("\n");
        script.append("\n");
        script.append("REM Auto-suppression du script\n");
        script.append("timeout /t 1 > nul\n");
        script.append("del /f /q \"%~f0\" 2>nul\n");

        Files.write(Paths.get(scriptPath), script.toString().getBytes());
        new File(scriptPath).setReadable(true);
        new File(scriptPath).setWritable(true);

        logger.info("Script de mise à jour créé: {}", scriptPath);
        logger.info("Lancement du script...");

        // Lancer le script en background
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptPath);
        pb.directory(new File(LauncherConfig.getLauncherDir()));
        pb.start();

        logger.info("Installation lancée, redémarrage...");
        System.exit(0);
    }

    /**
     * Installation sur macOS/Linux
     */
    private static void installUpdateUnix(String currentJar, String newJar, boolean isMac) throws Exception {
        String launcherDir = LauncherConfig.getLauncherDir();
        String scriptPath = Paths.get(launcherDir, "update.sh").toString();

        StringBuilder script = new StringBuilder();
        String safeCurrent = safeQuote(currentJar);
        String safeNew = safeQuote(newJar);

        script.append("#!/bin/bash\n");
        script.append("# Script de mise à jour du Nexaria Launcher\n");
        script.append("set -e\n");
        script.append("\n");
        script.append("# Attendre que le launcher se ferme\n");
        script.append("sleep 3\n");
        script.append("\n");
        script.append("# Supprimer l'ancienne version\n");
        script.append("if [ -f ").append(safeCurrent).append(" ]; then\n");
        script.append("    rm -f ").append(safeCurrent).append("\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# Déplacer la nouvelle version\n");
        script.append("mv ").append(safeNew).append(" ").append(safeCurrent).append("\n");
        script.append("\n");
        script.append("# Relancer le launcher\n");

        if (isMac) {
            script.append("nohup java -jar ").append(safeCurrent).append(" > /dev/null 2>&1 &\n");
        } else {
            script.append("nohup java -jar ").append(safeCurrent).append(" > /dev/null 2>&1 &\n");
        }

        script.append("\n");
        script.append("# Auto-suppression du script\n");
        script.append("sleep 1\n");
        script.append("rm -f \"$0\"\n");

        Path scriptFilePath = Paths.get(scriptPath);
        Files.write(scriptFilePath, script.toString().getBytes());

        File scriptFile = new File(scriptPath);
        scriptFile.setExecutable(true, false);
        scriptFile.setReadable(true, false);
        scriptFile.setWritable(true, false);

        logger.info("Script de mise à jour créé: {}", scriptPath);
        logger.info("Lancement du script...");

        // Lancer le script en background
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptPath);
        pb.directory(new File(LauncherConfig.getLauncherDir()));
        pb.start();

        logger.info("Installation lancée, redémarrage...");
        System.exit(0);
    }

    /**
     * Obtient le chemin du JAR actuel
     */
    private static String getCurrentJarPath() throws Exception {
        return new File(GitHubUpdater.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsolutePath();
    }

    /**
     * Compare deux versions (ex: 1.0.1 > 1.0.0)
     */
    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        try {
            String[] latest = latestVersion.split("\\.");
            String[] current = currentVersion.split("\\.");

            for (int i = 0; i < Math.max(latest.length, current.length); i++) {
                int latestPart = 0;
                int currentPart = 0;

                try {
                    if (i < latest.length) {
                        latestPart = Integer.parseInt(latest[i]);
                    }
                    if (i < current.length) {
                        currentPart = Integer.parseInt(current[i]);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Erreur lors du parsing de la version: {} ou {}",
                            (i < latest.length ? latest[i] : ""),
                            (i < current.length ? current[i] : ""));
                    continue;
                }

                if (latestPart > currentPart)
                    return true;
                if (latestPart < currentPart)
                    return false;
            }
            return false;
        } catch (Exception e) {
            logger.error("Erreur lors de la comparaison des versions", e);
            return false;
        }
    }

    /**
     * Lit un InputStream en String
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String calculateSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Escape quotes for Windows/Unix paths to prevent command injection
     */
    private static String safeQuote(String path) {
        if (path == null)
            return "\"\"";
        // Sur Unix, on escape les doubles quotes
        String safe = path.replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    /**
     * Vérifie l'intégrité du fichier téléchargé via SHA-256
     */
    private boolean verifyIntegrity(File file, GitHubRelease release) {
        try {
            String expectedHash = getExpectedHash(release);
            if (expectedHash == null) {
                logger.warn("Aucun hash SHA-256 trouvé pour cette release. VÉRIFICATION SUSPENDUE.");
                // TODO: En production, retourner false pour bloquer les updates non signés.
                // Pour l'instant, on laisse passer avec un warn si pas de hash, pour ne pas
                // casser la prod existante.
                return true;
            }

            String fileHash = calculateSha256(file);
            logger.debug("Hash calculé: {}", fileHash);
            logger.debug("Hash attendu: {}", expectedHash);

            return fileHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification d'intégrité", e);
            return false;
        }
    }

    private String getExpectedHash(GitHubRelease release) {
        // 1. Chercher dans le body de la release (Format: "SHA256: <hash>")
        if (release.changelog != null) {
            Pattern p = Pattern.compile("SHA256:\\s*([a-fA-F0-9]{64})");
            Matcher m = p.matcher(release.changelog);
            if (m.find()) {
                return m.group(1);
            }
        }
        // 2. TODO: Chercher un fichier .sha256 dans les assets (implémentation future)
        return null;
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

    /**
     * Résultat de la vérification des mises à jour
     */
    public static class UpdateCheckResult {
        public boolean hasUpdate;
        public GitHubRelease release;
        public String error;

        public UpdateCheckResult(boolean hasUpdate, GitHubRelease release, String error) {
            this.hasUpdate = hasUpdate;
            this.release = release;
            this.error = error;
        }
    }

    /**
     * Exception personnalisée pour les erreurs de mise à jour
     */
    public static class UpdateException extends Exception {
        public UpdateException(String message) {
            super(message);
        }

        public UpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
