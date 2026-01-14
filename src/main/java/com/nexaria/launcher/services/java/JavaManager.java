package com.nexaria.launcher.services.java;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavaManager {
    private static final Logger logger = LoggerFactory.getLogger(JavaManager.class);
    private static final String JAVA_VERSION = "17";
    private static final String RUNTIME_DIR = "java-runtime";

    public interface ProgressCallback {
        void onProgress(String status, int percent);
    }

    public static File getJavaHome() {
        File runtimeDir = new File(LauncherConfig.getDataFolder(), RUNTIME_DIR);
        File binDir = new File(runtimeDir, getBinPath());
        if (binDir.exists() && new File(binDir, getExecutableName()).exists()) {
            return runtimeDir;
        }
        return null; // Pas de runtime local
    }

    public static File getJavaExecutable() {
        File home = getJavaHome();
        if (home != null) {
            return new File(home, getBinPath() + File.separator + getExecutableName());
        }
        // Fallback: Java système
        return new File(System.getProperty("java.home"), "bin" + File.separator + getExecutableName());
    }

    public static void downloadJava(ProgressCallback callback) throws Exception {
        File runtimeDir = new File(LauncherConfig.getDataFolder(), RUNTIME_DIR);
        if (runtimeDir.exists())
            return; // Déjà là

        String os = getOS();
        String arch = getArch();
        String ext = os.equals("windows") ? "zip" : "tar.gz";
        String apiUrl = String.format(
                "https://api.adoptium.net/v3/assets/feature_releases/17/ga?architecture=%s&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=%s&vendor=eclipse",
                arch, os);

        logger.info("[JAVA] Récupération métadonnées depuis: {}", apiUrl);
        callback.onProgress("Vérification version Java...", 0);

        // 1. Récupérer le JSON pour avoir l'URL directe et le checksum
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Impossible de récupérer les infos Java (HTTP " + conn.getResponseCode() + ")");
        }

        String jsonResponse;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            jsonResponse = sb.toString();
        }

        // Parsing JSON basique (sans dépendance externe lourde si possible, mais ici on
        // a Gson via AzAuthManager)
        // On suppose que Gson est dispo car utilisé ailleurs
        com.google.gson.JsonArray releases = com.google.gson.JsonParser.parseString(jsonResponse).getAsJsonArray();
        if (releases.size() == 0)
            throw new IOException("Aucune release Java trouvée pour cet OS/Arch");

        com.google.gson.JsonObject release = releases.get(0).getAsJsonObject();
        com.google.gson.JsonObject binary = release.get("binaries").getAsJsonArray().get(0).getAsJsonObject();
        com.google.gson.JsonObject packageObj = binary.get("package").getAsJsonObject();

        String downloadUrl = packageObj.get("link").getAsString();
        String expectedSha256 = packageObj.get("checksum").getAsString();

        logger.info("[JAVA] URL: {}", downloadUrl);
        logger.info("[JAVA] Checksum attendu: {}", expectedSha256);

        File archive = new File(LauncherConfig.getDataFolder(), "java." + ext);

        // 2. Télécharger
        downloadFile(downloadUrl, archive, callback);

        // 3. Vérifier SHA-256
        callback.onProgress("Vérification intégrité...", 80);
        String actualSha256 = computeSha256(archive);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            archive.delete();
            throw new SecurityException("Checksum invalide ! Attendu: " + expectedSha256 + ", Reçu: " + actualSha256);
        }
        logger.info("[JAVA] Intégrité validée.");

        // 4. Extraire
        callback.onProgress("Extraction de Java...", 90);

        if (ext.equals("zip")) {
            unzip(archive, runtimeDir);
        } else {
            untargz(archive, runtimeDir);
        }

        archive.delete();
        logger.info("[JAVA] Installation terminée.");
    }

    private static String computeSha256(File file) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    private static void downloadFile(String url, File target, ProgressCallback callback) throws Exception {
        URL u = new URL(url);
        try (InputStream in = new BufferedInputStream(u.openStream());
                FileOutputStream out = new FileOutputStream(target)) {
            byte[] data = new byte[1024];
            int count;
            long total = 0;
            while ((count = in.read(data, 0, 1024)) != -1) {
                out.write(data, 0, count);
                total += count;
                if (total % 1024000 == 0)
                    callback.onProgress("Téléchargement (" + (total / 1024 / 1024) + " MB)...", -1);
            }
        }
    }

    private static String getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win"))
            return "windows";
        if (os.contains("mac"))
            return "mac";
        return "linux";
    }

    private static String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64"))
            return "aarch64";
        return "x64";
    }

    private static String getBinPath() {
        if (getOS().equals("mac"))
            return "Contents/Home/bin";
        return "bin";
    }

    private static String getExecutableName() {
        return getOS().equals("windows") ? "java.exe" : "java";
    }

    public static boolean isSystemJavaValid() {
        String v = System.getProperty("java.version");
        return v.startsWith("17") || v.startsWith("21");
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    if (getOS().equals("mac") || getOS().equals("linux")) {
                        if (newFile.getName().equals("java"))
                            newFile.setExecutable(true);
                    }
                }
            }
        }
    }

    private static void untargz(File tarFile, File destDir) throws IOException {
        destDir.mkdirs();
        try (InputStream fi = new FileInputStream(tarFile);
                InputStream bi = new BufferedInputStream(fi);
                org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream gzi = new org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(
                        bi);
                org.apache.commons.compress.archivers.tar.TarArchiveInputStream ti = new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                        gzi)) {

            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = ti.getNextTarEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = ti.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    // Restaurer les permissions d'exécution pour le binaire java
                    if (entry.getMode() != 0) { // simple check, plus précis serait mieux
                        if (newFile.getName().equals("java") || newFile.getName().endsWith(".sh")) {
                            newFile.setExecutable(true);
                        }
                    }
                }
            }
        }
    }
}
