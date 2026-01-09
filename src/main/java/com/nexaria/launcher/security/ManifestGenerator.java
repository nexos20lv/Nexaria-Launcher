package com.nexaria.launcher.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Génère des manifests (mods/configs) depuis le dossier data/ avec SHA-256.
 * Produit un JSON minimal compatible avec les vérificateurs.
 */
public class ManifestGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ManifestGenerator.class);

    public static void generateModsManifest(Path dataModsDir, Path outputFile, boolean withHash) throws IOException {
        Files.createDirectories(outputFile.getParent());
        List<String> entries = new ArrayList<>();
        if (Files.isDirectory(dataModsDir)) {
            try (var stream = Files.list(dataModsDir)) {
                for (Path p : stream.collect(java.util.stream.Collectors.toList())) {
                    if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                        String name = escape(p.getFileName().toString());
                        String hash = withHash ? sha256Hex(p) : null;
                        if (hash != null) entries.add("{\"name\":\"" + name + "\",\"sha256\":\"" + hash + "\"}");
                        else entries.add("{\"name\":\"" + name + "\"}");
                    }
                }
            }
        }
        String json = "{\n  \"mods\": [\n    " + String.join(",\n    ", entries) + "\n  ]\n}";
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        logger.info("Généré manifest mods: {} ({} éléments)", outputFile, entries.size());
    }

    public static void generateConfigsManifest(Path dataConfigsDir, Path outputFile, boolean withHash) throws IOException {
        Files.createDirectories(outputFile.getParent());
        List<String> entries = new ArrayList<>();
        if (Files.isDirectory(dataConfigsDir)) {
            try (var stream = Files.list(dataConfigsDir)) {
                for (Path p : stream.collect(java.util.stream.Collectors.toList())) {
                    if (Files.isRegularFile(p)) {
                        String fileName = p.getFileName().toString();
                        // Exclure les fichiers manifests du launcher
                        if (!fileName.endsWith("-manifest.json")) {
                            String name = escape(fileName);
                            String hash = withHash ? sha256Hex(p) : null;
                            if (hash != null) entries.add("{\"name\":\"" + name + "\",\"sha256\":\"" + hash + "\"}");
                            else entries.add("{\"name\":\"" + name + "\"}");
                        }
                    }
                }
            }
        }
        String json = "{\n  \"configs\": [\n    " + String.join(",\n    ", entries) + "\n  ]\n}";
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        logger.info("Généré manifest configs: {} ({} éléments)", outputFile, entries.size());
    }

    private static String sha256Hex(Path file) throws IOException {
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
