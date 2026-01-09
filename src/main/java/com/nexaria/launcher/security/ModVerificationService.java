package com.nexaria.launcher.security;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
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
 * Vérification des mods avant lancement: conformité au manifest, intégrité SHA-256
 * et suppression/isolement des mods non conformes selon la politique.
 */
public class ModVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(ModVerificationService.class);

    public static class VerificationResult {
        public final List<String> missingRequired = new ArrayList<>();
        public final List<String> unexpected = new ArrayList<>();
        public final List<String> hashMismatch = new ArrayList<>();
        public boolean manifestLoaded;

        public boolean isClean() {
            return missingRequired.isEmpty() && unexpected.isEmpty() && hashMismatch.isEmpty();
        }
    }

    public static class ManifestEntry {
        public String name;
        public String sha256; // optionnel
    }

    public static class ModManifest {
        public List<ManifestEntry> mods = new ArrayList<>();

        public Set<String> names() {
            return mods.stream().map(m -> m.name).filter(Objects::nonNull).collect(Collectors.toSet());
        }

        public Optional<String> expectedHash(String name) {
            return mods.stream().filter(m -> name.equals(m.name) && m.sha256 != null && !m.sha256.isBlank()).map(m -> m.sha256.toLowerCase(Locale.ROOT)).findFirst();
        }
    }

    private final Path modsDir;
    private final LauncherConfig cfg;

    public ModVerificationService(Path modsDir) {
        this.modsDir = modsDir;
        this.cfg = LauncherConfig.getInstance();
    }

    public VerificationResult verifyAndEnforce() throws IOException {
        VerificationResult res = verify();
        if (!res.isClean() && cfg.enforceModPolicy) {
            quarantineOrDelete(res);
        }
        return res;
    }

    public VerificationResult verify() throws IOException {
        VerificationResult result = new VerificationResult();
        long start = System.currentTimeMillis();

        ModManifest manifest = loadManifest();
        if (manifest != null) {
            result.manifestLoaded = true;
            logger.info("Manifest des mods chargé avec {} entrées", manifest.mods.size());
        } else {
            result.manifestLoaded = false;
            logger.info("Aucun manifest explicite trouvé; fallback sur data/mods comme référence");
            manifest = manifestFromLocalDataMods();
        }

        Set<String> allowedNames = manifest.names();
        Map<String, String> expectedHashes = new HashMap<>();
        for (ManifestEntry e : manifest.mods) {
            if (e.name != null && e.sha256 != null && !e.sha256.isBlank()) {
                expectedHashes.put(e.name, e.sha256.toLowerCase(Locale.ROOT));
            }
        }
        logger.debug("Mods autorisés: {}, avec {} hashes définis", allowedNames.size(), expectedHashes.size());

        // Lister mods présents dans le dossier cible
        File dir = modsDir.toFile();
        if (!dir.exists()) dir.mkdirs();
        String[] present = dir.list((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (present == null) present = new String[0];
        Set<String> presentSet = Arrays.stream(present)
                .filter(name -> !name.endsWith("-manifest.json"))
                .collect(Collectors.toSet());
        logger.debug("Mods présents: {}", presentSet.size());

        // Manquants (exigés par manifest)
        for (String req : allowedNames) {
            if (!presentSet.contains(req)) {
                result.missingRequired.add(req);
                logger.warn("Mod manquant: {}", req);
            }
        }

        // Non attendus
        for (String p : presentSet) {
            if (!allowedNames.contains(p)) {
                result.unexpected.add(p);
                logger.warn("Mod non attendu: {}", p);
            }
        }

        // Mismatch hash
        int hashChecks = 0;
        for (String name : presentSet) {
            if (expectedHashes.containsKey(name)) {
                hashChecks++;
                String exp = expectedHashes.get(name);
                String got = sha256Hex(modsDir.resolve(name));
                if (!exp.equalsIgnoreCase(got)) {
                    result.hashMismatch.add(name);
                    logger.warn("Mod hash invalide: {} (attendu: {}, obtenu: {})", name, exp, got);
                }
            }
        }
        logger.debug("Vérification hash: {} mods vérifiés", hashChecks);

        long elapsed = System.currentTimeMillis() - start;
        logResult(result, elapsed);
        return result;
    }

    private void logResult(VerificationResult r, long elapsed) {
        if (r.isClean()) {
            logger.info("Vérification des mods: OK ({}ms)", elapsed);
        } else {
            if (!r.missingRequired.isEmpty())
                logger.warn("Mods manquants: {}", r.missingRequired);
            if (!r.unexpected.isEmpty())
                logger.warn("Mods non attendus: {}", r.unexpected);
            if (!r.hashMismatch.isEmpty())
                logger.warn("Mods avec hash invalide: {}", r.hashMismatch);
            logger.warn("Vérification mods: ÉCARTS DÉTECTÉS ({}ms)", elapsed);
        }
    }

    private void quarantineOrDelete(VerificationResult r) throws IOException {
        Path quarantine = modsDir.resolve("quarantine");
        Files.createDirectories(quarantine);
        for (String name : r.unexpected) {
            moveIfExists(modsDir.resolve(name), quarantine.resolve(name));
        }
        for (String name : r.hashMismatch) {
            moveIfExists(modsDir.resolve(name), quarantine.resolve(name + ".badhash"));
        }
        logger.info("Application de la politique: éléments non conformes déplacés vers {}", quarantine);
    }

    private void moveIfExists(Path src, Path dst) throws IOException {
        if (Files.exists(src)) {
            Files.createDirectories(dst.getParent());
            Files.move(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ModManifest loadManifest() {
        // Priorité: URL config > fichier dans game/configs/mods-manifest.json > data/configs/mods-manifest.json
        // 1) URL config
        try {
            if (cfg.modManifestUrl != null && !cfg.modManifestUrl.isBlank()) {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.modManifestUrl)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                    return parseManifest(resp.body());
                }
            }
        } catch (Exception e) {
            logger.warn("Lecture manifest URL échouée", e);
        }
        // 2) game/configs
        Path inGame = Paths.get(LauncherConfig.getConfigsDir(), "mods-manifest.json");
        if (Files.exists(inGame)) {
            try { return parseManifest(Files.readString(inGame)); } catch (Exception e) { logger.warn("Manifest invalide: {}", inGame, e); }
        }
        // 3) data/configs
        Path local = Paths.get("data", "configs", "mods-manifest.json");
        if (Files.exists(local)) {
            try { return parseManifest(Files.readString(local)); } catch (Exception e) { logger.warn("Manifest invalide: {}", local, e); }
        }
        return null;
    }

    private ModManifest manifestFromLocalDataMods() throws IOException {
        ModManifest m = new ModManifest();
        Path localMods = Paths.get("data", "mods");
        if (Files.isDirectory(localMods)) {
            try (var stream = Files.list(localMods)) {
                for (Path p : stream.collect(Collectors.toList())) {
                    if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                        ManifestEntry e = new ManifestEntry();
                        e.name = p.getFileName().toString();
                        // Facultatif: calculer un hash de référence pour renforcer l'intégrité locale
                        // e.sha256 = sha256Hex(p);
                        m.mods.add(e);
                    }
                }
            }
        }
        return m;
    }

    private ModManifest parseManifest(String jsonContent) {
        // Simple parsing JSON pour extraire la liste des mods
        // Format attendu: { "mods": [{ "name": "mod.jar", "sha256": "..." }, ...] }
        ModManifest m = new ModManifest();
        try {
            if (jsonContent == null || jsonContent.isEmpty()) return m;
            
            // Parsing simple: chercher les "name" et "sha256"
            int start = 0;
            while ((start = jsonContent.indexOf("\"name\"", start)) != -1) {
                start += 6;
                int quoteStart = jsonContent.indexOf("\"", start) + 1;
                int quoteEnd = jsonContent.indexOf("\"", quoteStart);
                String name = jsonContent.substring(quoteStart, quoteEnd);
                
                ManifestEntry e = new ManifestEntry();
                e.name = name;
                
                // Chercher le sha256 correspondant
                int sha256Start = jsonContent.indexOf("\"sha256\"", start);
                if (sha256Start > 0 && sha256Start < jsonContent.indexOf(",", start) + 100) {
                    sha256Start += 8;
                    int shaQuoteStart = jsonContent.indexOf("\"", sha256Start) + 1;
                    int shaQuoteEnd = jsonContent.indexOf("\"", shaQuoteStart);
                    if (shaQuoteEnd > shaQuoteStart) {
                        e.sha256 = jsonContent.substring(shaQuoteStart, shaQuoteEnd);
                    }
                }
                
                m.mods.add(e);
            }
        } catch (Exception ex) {
            logger.warn("Erreur parsing manifest JSON", ex);
        }
        return m;
    }

    private String sha256Hex(Path file) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                md.update(buf, 0, r);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Echec calcul SHA-256 pour " + file, e);
        }
    }
}
