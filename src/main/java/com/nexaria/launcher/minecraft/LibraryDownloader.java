package com.nexaria.launcher.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LibraryDownloader {

    public static void ensureLibrariesAndClient(String minecraftDir, String versionId) throws Exception {
        String versionsDir = minecraftDir + "/versions";
        String jsonPath = versionsDir + "/" + versionId + "/" + versionId + ".json";
        try (FileReader reader = new FileReader(jsonPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            String os = System.getProperty("os.name", "").toLowerCase();
            String osKey = os.contains("win") ? "windows" : (os.contains("mac") ? "osx" : "linux");

            // Libraries
            String libsDir = minecraftDir + "/libraries";
            JsonArray libraries = root.getAsJsonArray("libraries");
            for (JsonElement el : libraries) {
                JsonObject lib = el.getAsJsonObject();
                if (!isAllowedForOs(lib, osKey)) continue;

                if (lib.has("downloads")) {
                    JsonObject dl = lib.getAsJsonObject("downloads");
                    if (dl.has("artifact")) {
                        JsonObject art = dl.getAsJsonObject("artifact");
                        if (art.has("path") && art.has("url")) {
                            String path = art.get("path").getAsString();
                            String url = art.get("url").getAsString();
                            Path dest = Paths.get(libsDir, path);
                            ensureDownloaded(url, dest);
                        }
                    }
                }
            }

            // Client jar
            if (root.has("downloads")) {
                JsonObject dlRoot = root.getAsJsonObject("downloads");
                if (dlRoot.has("client")) {
                    JsonObject client = dlRoot.getAsJsonObject("client");
                    if (client.has("url")) {
                        String url = client.get("url").getAsString();
                        Path dest = Paths.get(versionsDir, versionId, versionId + ".jar");
                        ensureDownloaded(url, dest);
                    }
                }
            }
        }
    }

    private static boolean isAllowedForOs(JsonObject lib, String osKey) {
        if (!lib.has("rules")) return true;
        boolean allowed = false; // follow Mojang semantics: last matching rule wins
        JsonArray rules = lib.getAsJsonArray("rules");
        for (JsonElement rEl : rules) {
            JsonObject rule = rEl.getAsJsonObject();
            String action = rule.has("action") ? rule.get("action").getAsString() : "allow";
            boolean matches = true;
            if (rule.has("os")) {
                JsonObject osObj = rule.getAsJsonObject("os");
                if (osObj.has("name")) {
                    String name = osObj.get("name").getAsString();
                    matches = osKey.equalsIgnoreCase(name);
                }
            }
            if (matches) {
                allowed = "allow".equalsIgnoreCase(action);
            }
        }
        return allowed;
    }

    private static void ensureDownloaded(String url, Path dest) throws Exception {
        File f = dest.toFile();
        if (f.exists()) return;
        File parent = f.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        String effectiveUrl = url;
        try {
            downloadWithThrottle(effectiveUrl, f);
        } catch (Exception ex) {
            // Tentative miroir si configuré
            String mirror = com.nexaria.launcher.config.LauncherConfig.getInstance().downloadMirrorBase;
            if (mirror != null && !mirror.isEmpty()) {
                try {
                    String alt = remapToMirror(url, mirror);
                    downloadWithThrottle(alt, f);
                    return;
                } catch (Exception ignore) { }
            }
            throw ex;
        }
    }

    private static void downloadWithThrottle(String url, File dest) throws Exception {
        int kbps = com.nexaria.launcher.config.LauncherConfig.getInstance().downloadRateLimitKBps;
        try (InputStream in = URI.create(url).toURL().openStream();
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            long windowStart = System.nanoTime();
            int bytesThisWindow = 0;
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                if (kbps > 0) {
                    bytesThisWindow += r;
                    long elapsedMs = (System.nanoTime() - windowStart) / 1_000_000L;
                    int limitPerMs = (kbps * 1024) / 1000; // approx
                    if (bytesThisWindow > limitPerMs * Math.max(1, elapsedMs)) {
                        Thread.sleep(10);
                        // reset window periodically
                        if (elapsedMs > 1000) { windowStart = System.nanoTime(); bytesThisWindow = 0; }
                    }
                }
            }
        }
    }

    private static String remapToMirror(String original, String mirrorBase) {
        // Simple remap: if original starts with a known base, replace with mirror base
        if (original.startsWith("https://libraries.minecraft.net/")) {
            return mirrorBase + original.substring("https://libraries.minecraft.net".length());
        }
        return original;
    }

    /**
     * S'assurer que les JARs log4j-api et log4j-core sont présents dans le dossier libraries.
     * Si absents, les télécharger depuis Maven Central dans la hiérarchie Maven habituelle.
     */
    public static void ensureLog4jLibraries(String librariesDir) throws Exception {
        String apiVer = "2.17.2";
        String coreVer = "2.17.2";

        Path apiPath = Paths.get(librariesDir, "org/apache/logging/log4j/log4j-api/" + apiVer, "log4j-api-" + apiVer + ".jar");
        Path corePath = Paths.get(librariesDir, "org/apache/logging/log4j/log4j-core/" + coreVer, "log4j-core-" + coreVer + ".jar");

        if (!apiPath.toFile().exists()) {
            String url = "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/" + apiVer + "/log4j-api-" + apiVer + ".jar";
            ensureDownloaded(url, apiPath);
        }
        if (!corePath.toFile().exists()) {
            String url = "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/" + coreVer + "/log4j-core-" + coreVer + ".jar";
            ensureDownloaded(url, corePath);
        }
    }

    /**
     * S'assurer que jopt-simple est présent dans le dossier libraries, sinon le télécharger.
     */
    public static void ensureJoptSimpleLibrary(String librariesDir) throws Exception {
        // Si un jopt-simple quelconque existe déjà, ne pas télécharger pour éviter les doublons de modules
        try (java.util.stream.Stream<Path> s = Files.walk(Paths.get(librariesDir))) {
            boolean exists = s.filter(p -> p.toString().endsWith(".jar"))
                    .anyMatch(p -> {
                        String t = p.toString().replace('\\','/');
                        String name = p.getFileName().toString();
                        return t.contains("/net/sf/jopt-simple/") || name.startsWith("jopt-simple-");
                    });
            if (exists) return;
        }

        String ver = "5.0.4";
        Path path = Paths.get(librariesDir, "net/sf/jopt-simple/jopt-simple/" + ver, "jopt-simple-" + ver + ".jar");
        if (!path.toFile().exists()) {
            String url = "https://repo1.maven.org/maven2/net/sf/jopt-simple/jopt-simple/" + ver + "/jopt-simple-" + ver + ".jar";
            ensureDownloaded(url, path);
        }
    }

    /**
     * S'assurer que la suite ASM (asm, asm-commons, asm-tree, asm-util, asm-analysis) est présente.
     * Si absente, la télécharger depuis Maven Central.
     */
    public static void ensureAsmLibraries(String librariesDir) throws Exception {
        // Si un asm-commons quelconque existe déjà, présumer que la suite est fournie par le pack
        try (java.util.stream.Stream<Path> s = Files.walk(Paths.get(librariesDir))) {
            boolean hasAsmCommons = s.filter(p -> p.toString().endsWith(".jar"))
                    .anyMatch(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("asm-commons-");
                    });
            if (hasAsmCommons) return;
        }

        String ver = "9.4"; // compatible avec Nashorn 15.x
        String[] arts = new String[] {"asm", "asm-commons", "asm-tree", "asm-util", "asm-analysis"};
        for (String art : arts) {
            Path path = Paths.get(librariesDir, "org/ow2/asm/" + art + "/" + ver, art + "-" + ver + ".jar");
            if (!path.toFile().exists()) {
                String url = "https://repo1.maven.org/maven2/org/ow2/asm/" + art + "/" + ver + "/" + art + "-" + ver + ".jar";
                ensureDownloaded(url, path);
            }
        }
    }
}
