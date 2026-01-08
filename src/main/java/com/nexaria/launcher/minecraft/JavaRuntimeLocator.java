package com.nexaria.launcher.minecraft;

import com.nexaria.launcher.config.LauncherConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

/**
 * Localise ou télécharge automatiquement une runtime Java requise (Temurin/Adoptium)
 * pour macOS, Windows, Linux et retourne le chemin de l'exécutable java.
 */
public class JavaRuntimeLocator {
    private static final Logger logger = LoggerFactory.getLogger(JavaRuntimeLocator.class);

    public interface ProgressListener {
        void onStatus(String status);
        void onProgress(long downloaded, long total);
    }

    /**
     * Retourne le chemin vers l'exécutable Java pour la version requise.
     * Si non disponible localement, tente de télécharger et d'extraire automatiquement.
     */
    public static String getJavaExecutable(int requiredMajor) {
        return getJavaExecutable(requiredMajor, false, null);
    }

    /**
     * Variante qui peut forcer une version exacte (préférer le JDK requis même si une version supérieure est présente).
     */
    public static String getJavaExecutable(int requiredMajor, boolean preferExact) {
        return getJavaExecutable(requiredMajor, preferExact, null);
    }

    public static String getJavaExecutable(int requiredMajor, boolean preferExact, ProgressListener listener) {
        // 1) Si on n'exige pas l'exactitude et que la JVM courante suffit, utiliser "java" du PATH
        int current = parseMajor(System.getProperty("java.version"));
        if (!preferExact && current >= requiredMajor) {
            return "java";
        }

        // 2) Essayer détection spécifique macOS via java_home
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            try {
                Process p = new ProcessBuilder("/usr/libexec/java_home", "-v", String.valueOf(requiredMajor)).start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String home = br.readLine();
                    p.waitFor();
                    if (home != null && !home.isBlank()) {
                        String java = home + File.separator + "bin" + File.separator + "java";
                        logger.info("Java {} détecté via java_home: {}", requiredMajor, java);
                        return java;
                    }
                }
            } catch (Exception e) {
                logger.warn("Impossible de trouver Java {} via java_home", requiredMajor);
            }
        }

        // 3) Télécharger automatiquement une runtime Java adaptée
        try {
            if (listener != null) listener.onStatus("Téléchargement du JDK " + requiredMajor + "...");
            String javaPath = ensureDownloadedJava(requiredMajor, listener);
            if (javaPath != null) {
                return javaPath;
            }
        } catch (Exception e) {
            logger.error("Téléchargement Java {} échoué", requiredMajor, e);
        }

        // Échec de localisation
        return null;
    }

    /**
     * Télécharge et extrait le JDK Temurin pour l'OS/architecture courants.
     * Retourne le chemin complet vers bin/java.
     */
    private static String ensureDownloadedJava(int major, ProgressListener listener) throws Exception {
        String os = detectOs();
        String arch = detectArch();
        String baseDir = LauncherConfig.getDataFolder() + File.separator + "runtime" + File.separator + ("jdk" + major) + File.separator + os + "-" + arch;
        File destDir = new File(baseDir);
        destDir.mkdirs();

        // Si déjà extrait (java présent), réutiliser
        File javaExe = new File(destDir, exe("bin/java"));
        if (javaExe.exists()) {
            logger.info("Java {} déjà installé: {}", major, javaExe.getAbsolutePath());
            return javaExe.getAbsolutePath();
        }

        // Appel API Adoptium v3 pour récupérer l'URL du binaire
        String apiUrl = String.format(
                "https://api.adoptium.net/v3/assets/latest/%d/hotspot?architecture=%s&os=%s&image_type=jdk&heap_size=normal",
                major, arch, os);

        logger.info("Recherche JDK {} via {}", major, apiUrl);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(new HttpGet(apiUrl));
            int code = resp.getCode();
            if (code != 200) {
                throw new IOException("Adoptium API HTTP " + code);
            }
            String json = EntityUtils.toString(resp.getEntity());
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            if (arr.size() == 0) throw new IOException("Aucun asset pour JDK " + major);

            JsonObject first = arr.get(0).getAsJsonObject();
            JsonObject binary = first.getAsJsonObject("binary");
            JsonObject pkg = binary.getAsJsonObject("package");
            String link = pkg.get("link").getAsString();
            String name = pkg.get("name").getAsString();

            // Télécharger archive
            File archive = new File(destDir, name);
            if (!archive.exists()) {
                logger.info("Téléchargement: {}", link);
                if (listener != null) listener.onStatus("Téléchargement: " + name);
                ClassicHttpResponse dl = (ClassicHttpResponse) client.execute(new HttpGet(link));
                if (dl.getCode() != 200) throw new IOException("Download HTTP " + dl.getCode());
                long total = dl.getEntity().getContentLength();
                try (InputStream is = dl.getEntity().getContent();
                     OutputStream out = Files.newOutputStream(archive.toPath())) {
                    byte[] buf = new byte[8192];
                    long done = 0;
                    int r;
                    while ((r = is.read(buf)) != -1) {
                        out.write(buf, 0, r);
                        done += r;
                        if (listener != null && total > 0) listener.onProgress(done, total);
                    }
                }
            } else {
                logger.info("Archive déjà présente: {}", archive.getName());
            }

            // Extraire
            if (listener != null) listener.onStatus("Extraction...");
            extractArchive(archive, destDir);

            // Trouver dossier JDK extrait (contient bin/java)
            File[] children = destDir.listFiles((f) -> f.isDirectory());
            if (children != null) {
                for (File child : children) {
                    File j = new File(child, exe("bin/java"));
                    if (j.exists()) {
                        logger.info("Java {} installé: {}", major, j.getAbsolutePath());
                        if (listener != null) listener.onStatus("JDK installé");
                        return j.getAbsolutePath();
                    }
                }
            }
        }

        throw new IOException("Extraction JDK " + major + " introuvable");
    }

    private static void extractArchive(File archive, File destDir) throws Exception {
        String name = archive.getName().toLowerCase();
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            // macOS/Linux: utiliser tar
            Process p = new ProcessBuilder("tar", "-xzf", archive.getAbsolutePath(), "-C", destDir.getAbsolutePath()).inheritIO().start();
            int exit = p.waitFor();
            if (exit != 0) throw new IOException("tar exit code " + exit);
        } else if (name.endsWith(".zip")) {
            // Windows: PowerShell Expand-Archive
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                    String.format("Expand-Archive -Path \"%s\" -DestinationPath \"%s\" -Force", archive.getAbsolutePath(), destDir.getAbsolutePath()))
                    .inheritIO().start();
            int exit = p.waitFor();
            if (exit != 0) throw new IOException("Expand-Archive exit code " + exit);
        } else {
            throw new IOException("Format d'archive non supporté: " + name);
        }
    }

    private static String detectOs() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "mac";
        return "linux";
    }

    private static String detectArch() {
        String a = System.getProperty("os.arch").toLowerCase();
        if (a.contains("aarch64") || a.contains("arm64")) return "aarch64";
        return "x64"; // défaut courant
    }

    private static String exe(String relPath) {
        String os = System.getProperty("os.name").toLowerCase();
        String p = relPath + (os.contains("win") ? ".exe" : "");
        return p.replace("/", File.separator);
    }

    private static int parseMajor(String version) {
        try {
            if (version == null) return 0;
            String[] parts = version.split("\\.");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 0;
        }
    }
}