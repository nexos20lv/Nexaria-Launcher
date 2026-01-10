package com.nexaria.launcher.security;

import com.google.gson.Gson;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

/**
 * Service de vérification de l'intégrité de tous les fichiers dans data/
 * (mods et configs). Permet à Minecraft d'ajouter ses propres fichiers.
 */
public class DataVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(DataVerificationService.class);

    public static class Entry {
        public String path;
        public String sha256;
    }

    public static class Manifest {
        public List<Entry> files = new ArrayList<>();
    }

    public static class Result {
        public boolean ok = true;
        public boolean manifestLoaded;
        public List<String> missing = new ArrayList<>();
        public List<String> modified = new ArrayList<>();
        public List<String> unexpected = new ArrayList<>();
    }

    private final Path gameDataDir; // game/data ou équivalent
    private final String policy; // warn, quarantine, strict

    public DataVerificationService(Path gameDataDir, String policy) {
        this.gameDataDir = gameDataDir;
        this.policy = policy != null ? policy : "warn";
    }

    /**
     * Vérifie l'intégrité de tous les fichiers du dossier data/
     * Les fichiers créés par Minecraft (nouveaux dossiers/fichiers) sont ignorés
     */
    public Result verify() throws IOException {
        long t0 = System.currentTimeMillis();
        Result r = new Result();

        Manifest m = loadManifest();
        if (m == null) {
            logger.warn("Aucun manifest data trouvé, vérification ignorée");
            r.manifestLoaded = false;
            return r;
        }
        r.manifestLoaded = true;

        // Créer un set de tous les chemins attendus
        Set<String> expectedPaths = new HashSet<>();
        for (Entry e : m.files) {
            expectedPaths.add(e.path);
        }

        // Vérifier chaque fichier attendu
        for (Entry e : m.files) {
            Path file = gameDataDir.resolve(e.path);
            
            if (!Files.exists(file)) {
                r.missing.add(e.path);
                r.ok = false;
                logger.warn("Fichier manquant: {}", e.path);
            } else if (e.sha256 != null && !e.sha256.isEmpty()) {
                String actualHash = sha256Hex(file);
                if (!actualHash.equalsIgnoreCase(e.sha256)) {
                    r.modified.add(e.path);
                    r.ok = false;
                    logger.warn("Fichier modifié: {} (attendu: {}, réel: {})", 
                        e.path, e.sha256.substring(0, 8), actualHash.substring(0, 8));
                }
            }
        }

        // Scanner les fichiers présents et identifier les inattendus
        // MAIS ignorer les fichiers que Minecraft peut créer (logs, saves, etc.)
        if (Files.isDirectory(gameDataDir)) {
            scanForUnexpected(gameDataDir, gameDataDir, expectedPaths, r);
        }

        long dur = System.currentTimeMillis() - t0;
        if (r.ok) {
            logger.info("Vérification data: OK ({}ms)", dur);
        } else {
            logger.warn("Vérification data: ÉCARTS DÉTECTÉS ({}ms)", dur);
            if (!r.missing.isEmpty()) logger.warn("Fichiers manquants: {}", r.missing);
            if (!r.modified.isEmpty()) logger.warn("Fichiers modifiés: {}", r.modified);
            if (!r.unexpected.isEmpty()) logger.warn("Fichiers inattendus: {}", r.unexpected);
        }

        return r;
    }

    private void scanForUnexpected(Path baseDir, Path current, Set<String> expectedPaths, Result r) throws IOException {
        if (!Files.isDirectory(current)) return;

        try (var stream = Files.list(current)) {
            for (Path p : stream.toList()) {
                String relativePath = baseDir.relativize(p).toString().replace("\\", "/");
                
                if (Files.isDirectory(p)) {
                    // Ignorer les dossiers créés par Minecraft
                    String dirName = p.getFileName().toString();
                    if (isMinecraftGeneratedFolder(dirName)) {
                        continue; // Ne pas scanner ces dossiers
                    }
                    scanForUnexpected(baseDir, p, expectedPaths, r);
                } else {
                    String fileName = p.getFileName().toString();
                    
                    // Ignorer les fichiers manifests et les fichiers Minecraft
                    if (fileName.endsWith("-manifest.json") || 
                        fileName.equals("data-manifest.json") ||
                        fileName.startsWith(".") ||
                        isMinecraftGeneratedFile(fileName)) {
                        continue;
                    }

                    // Si le fichier n'est pas dans la liste attendue, c'est inattendu
                    if (!expectedPaths.contains(relativePath)) {
                        r.unexpected.add(relativePath);
                    }
                }
            }
        }
    }

    /**
     * Vérifie si un dossier est généré par Minecraft
     */
    private boolean isMinecraftGeneratedFolder(String name) {
        // Dossiers que Minecraft crée et qui ne doivent pas être vérifiés
        return name.equals("saves") || 
               name.equals("logs") || 
               name.equals("crash-reports") ||
               name.equals("screenshots") ||
               name.equals("resourcepacks") ||
               name.equals("shaderpacks") ||
               name.equals("backups");
    }

    /**
     * Vérifie si un fichier est généré par Minecraft
     */
    private boolean isMinecraftGeneratedFile(String name) {
        // Fichiers que Minecraft peut créer/modifier
        return name.equals("options.txt") ||
               name.equals("servers.dat") ||
               name.equals("servers.dat_old") ||
               name.equals("usercache.json") ||
               name.equals("usernamecache.json") ||
               name.startsWith("replay_") ||
               name.endsWith(".log") ||
               name.endsWith(".log.gz");
    }

    private Manifest loadManifest() {
        // 1. Essayer depuis l'URL si configurée
        try {
            LauncherConfig cfg = LauncherConfig.getInstance();
            if (cfg.dataManifestUrl != null && !cfg.dataManifestUrl.isBlank()) {
                logger.debug("Téléchargement manifest data depuis: {}", cfg.dataManifestUrl);
                HttpClient cli = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.dataManifestUrl)).GET().build();
                HttpResponse<String> res = cli.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) return parse(res.body());
            }
        } catch (Exception e) {
            logger.warn("Lecture manifest data URL échouée", e);
        }

        // 2. Essayer depuis game/data/
        Path inGame = gameDataDir.resolve("data-manifest.json");
        if (Files.exists(inGame)) {
            try {
                return parse(Files.readString(inGame));
            } catch (Exception e) {
                logger.warn("Manifest invalide: {}", inGame, e);
            }
        }

        // 3. Essayer depuis data/ local
        Path local = Paths.get("data", "data-manifest.json");
        if (Files.exists(local)) {
            try {
                return parse(Files.readString(local));
            } catch (Exception e) {
                logger.warn("Manifest invalide: {}", local, e);
            }
        }

        return null;
    }

    private Manifest parse(String json) {
        return new Gson().fromJson(json, Manifest.class);
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
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Echec calcul SHA-256 pour " + file, e);
        }
    }

    /**
     * Applique la politique de sécurité sur les écarts détectés
     */
    public void applyPolicy(Result r) throws IOException {
        if (r.ok) return;

        if ("strict".equalsIgnoreCase(policy)) {
            throw new SecurityException("Vérification data: des écarts ont été détectés.\n" +
                    "Manquants: " + r.missing + "\n" +
                    "Modifiés: " + r.modified + "\n" +
                    "Non attendus: " + r.unexpected);
        }

        if ("quarantine".equalsIgnoreCase(policy)) {
            Path qDir = gameDataDir.resolve("quarantine");
            Files.createDirectories(qDir);

            // Déplacer les fichiers modifiés et inattendus
            for (String path : r.modified) {
                Path src = gameDataDir.resolve(path);
                Path dst = qDir.resolve(path);
                Files.createDirectories(dst.getParent());
                Files.move(src, dst);
                logger.info("Fichier modifié déplacé: {} -> {}", path, dst);
            }

            for (String path : r.unexpected) {
                Path src = gameDataDir.resolve(path);
                Path dst = qDir.resolve(path);
                Files.createDirectories(dst.getParent());
                Files.move(src, dst);
                logger.info("Fichier inattendu déplacé: {} -> {}", path, dst);
            }

            logger.info("Application de la politique: éléments non conformes déplacés vers {}", qDir);
        } else {
            logger.warn("Politique '{}' : écarts détectés mais aucune action prise", policy);
        }
    }
}
