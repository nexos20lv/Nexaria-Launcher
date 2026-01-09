package com.nexaria.launcher.security;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vérifie l'intégrité des fichiers de configuration avant lancement.
 * Basé sur un manifest (URL, game/configs ou data/configs) listant les fichiers attendus
 * et leurs SHA-256 optionnels.
 */
public class ConfigVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigVerificationService.class);

    public static class Entry { public String name; public String sha256; }
    public static class Manifest { public List<Entry> configs = new ArrayList<>(); }

    public static class Result {
        public final List<String> missing = new ArrayList<>();
        public final List<String> unexpected = new ArrayList<>();
        public final List<String> badHash = new ArrayList<>();
        public boolean manifestLoaded;
        public boolean isClean() { return missing.isEmpty() && unexpected.isEmpty() && badHash.isEmpty(); }
    }

    private final Path configsDir;
    private final LauncherConfig cfg;

    public ConfigVerificationService(Path configsDir) {
        this.configsDir = configsDir;
        this.cfg = LauncherConfig.getInstance();
    }

    public Result verify() throws IOException {
        Result r = new Result();
        Manifest m = loadManifest();
        if (m == null) { m = localManifestFromData(); r.manifestLoaded = false; }
        else r.manifestLoaded = true;

        Set<String> allowed = m.configs.stream().map(e -> e.name).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, String> hashes = new HashMap<>();
        for (Entry e : m.configs) if (e.name != null && e.sha256 != null && !e.sha256.isBlank()) hashes.put(e.name, e.sha256.toLowerCase(Locale.ROOT));

        if (!Files.exists(configsDir)) Files.createDirectories(configsDir);
        Set<String> present = new HashSet<>();
        try (var stream = Files.list(configsDir)) {
            for (Path p : stream.collect(Collectors.toList())) {
                if (Files.isRegularFile(p)) {
                    String fileName = p.getFileName().toString();
                    // Exclure les fichiers manifests du launcher lui-même
                    if (!fileName.endsWith("-manifest.json")) {
                        present.add(fileName);
                    }
                }
            }
        }

        for (String req : allowed) if (!present.contains(req)) r.missing.add(req);
        for (String p : present) if (!allowed.contains(p)) r.unexpected.add(p);
        for (String n : present) if (hashes.containsKey(n)) {
            String exp = hashes.get(n);
            String got = sha256Hex(configsDir.resolve(n));
            if (!exp.equalsIgnoreCase(got)) r.badHash.add(n);
        }

        log(r);
        return r;
    }

    private void log(Result r) {
        if (r.isClean()) logger.info("Vérification des configs: OK");
        else {
            if (!r.missing.isEmpty()) logger.warn("Configs manquantes: {}", r.missing);
            if (!r.unexpected.isEmpty()) logger.warn("Configs non attendues: {}", r.unexpected);
            if (!r.badHash.isEmpty()) logger.warn("Configs avec hash invalide: {}", r.badHash);
        }
    }

    private Manifest loadManifest() {
        try {
            if (cfg.configManifestUrl != null && !cfg.configManifestUrl.isBlank()) {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.configManifestUrl)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                    return parse(resp.body());
                }
            }
        } catch (Exception e) { logger.warn("Lecture manifest configs URL échouée", e); }
        Path inGame = Paths.get(LauncherConfig.getConfigsDir(), "configs-manifest.json");
        if (Files.exists(inGame)) { try { return parse(Files.readString(inGame)); } catch (Exception e) { logger.warn("Manifest invalide: {}", inGame, e); } }
        Path local = Paths.get("data", "configs", "configs-manifest.json");
        if (Files.exists(local)) { try { return parse(Files.readString(local)); } catch (Exception e) { logger.warn("Manifest invalide: {}", local, e); } }
        return null;
    }

    private Manifest localManifestFromData() throws IOException {
        Manifest m = new Manifest();
        Path base = Paths.get("data", "configs");
        if (Files.isDirectory(base)) try (var stream = Files.list(base)) {
            for (Path p : stream.collect(Collectors.toList())) {
                if (Files.isRegularFile(p)) {
                    String fileName = p.getFileName().toString();
                    // Exclure les fichiers manifests du launcher
                    if (!fileName.endsWith("-manifest.json")) {
                        Entry e = new Entry();
                        e.name = fileName;
                        m.configs.add(e);
                    }
                }
            }
        }
        return m;
    }

    private Manifest parse(String json) {
        // Parsing minimal sans dépendances externes (très simple):
        // attend des objets {"configs":[{"name":"...","sha256":"..."}, ...]}
        Manifest m = new Manifest();
        String body = json.replace("\r", "").replace("\n", "");
        int arrIdx = body.indexOf("\"configs\"");
        if (arrIdx < 0) return m;
        int start = body.indexOf('[', arrIdx);
        int end = body.indexOf(']', start);
        if (start < 0 || end < 0) return m;
        String arr = body.substring(start + 1, end);
        String[] items = arr.split("\\},\\s*\\{");
        for (String it : items) {
            String s = it.replace("{", "").replace("}", "");
            Entry e = new Entry();
            for (String kv : s.split(",")) {
                String[] parts = kv.split(":", 2);
                if (parts.length != 2) continue;
                String k = parts[0].trim().replace("\"", "");
                String v = parts[1].trim().replace("\"", "");
                if ("name".equals(k)) e.name = v;
                else if ("sha256".equals(k)) e.sha256 = v;
            }
            if (e.name != null && !e.name.isBlank()) m.configs.add(e);
        }
        return m;
    }

    private String sha256Hex(Path file) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r; while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IOException("Echec calcul SHA-256 pour " + file, e); }
    }
}
