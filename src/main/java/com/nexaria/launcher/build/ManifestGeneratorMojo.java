package com.nexaria.launcher.build;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Utilitaire pour générer le manifest complet du dossier data/.
 * Scanne récursivement tous les fichiers et calcule leur SHA-256.
 */
public class ManifestGeneratorMojo {

    /**
     * Génère un manifest complet de tous les fichiers dans le dossier data/
     */
    public static void generateDataManifest(Path dataDir, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        List<String> entries = new ArrayList<>();
        
        if (Files.isDirectory(dataDir)) {
            // Scanner récursivement tous les fichiers
            try (Stream<Path> walk = Files.walk(dataDir)) {
                for (Path p : walk.filter(Files::isRegularFile).toList()) {
                    String fileName = p.getFileName().toString();
                    
                    // Exclure les manifests eux-mêmes et les fichiers cachés
                    if (!fileName.endsWith("-manifest.json") && 
                        !fileName.equals("data-manifest.json") &&
                        !fileName.startsWith(".")) {
                        
                        // Chemin relatif depuis data/
                        String relativePath = dataDir.relativize(p).toString().replace("\\", "/");
                        String name = escape(relativePath);
                        String hash = sha256Hex(p);
                        entries.add("{\"path\":\"" + name + "\",\"sha256\":\"" + hash + "\"}");
                    }
                }
            }
        }
        
        String json = "{\n  \"files\": [\n    " + String.join(",\n    ", entries) + "\n  ]\n}";
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        System.out.println("✓ Manifeste data généré: " + outputFile + " (" + entries.size() + " fichiers)");
    }

    public static void generateModsManifest(Path dataModsDir, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        List<String> entries = new ArrayList<>();
        
        if (Files.isDirectory(dataModsDir)) {
            try (var stream = Files.list(dataModsDir)) {
                for (Path p : stream.toList()) {
                    if (Files.isRegularFile(p)) {
                        String fileName = p.getFileName().toString();
                        // Inclure les fichiers .jar et exclure le manifest lui-même
                        if (fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                            String name = escape(fileName);
                            String hash = sha256Hex(p);
                            entries.add("{\"name\":\"" + name + "\",\"sha256\":\"" + hash + "\"}");
                        }
                    }
                }
            }
        }
        
        String json = "{\n  \"mods\": [\n    " + String.join(",\n    ", entries) + "\n  ]\n}";
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        System.out.println("✓ Manifeste des mods généré: " + outputFile + " (" + entries.size() + " mods)");
    }

    public static void generateConfigsManifest(Path dataConfigsDir, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        List<String> entries = new ArrayList<>();
        
        if (Files.isDirectory(dataConfigsDir)) {
            try (var stream = Files.list(dataConfigsDir)) {
                for (Path p : stream.toList()) {
                    if (Files.isRegularFile(p)) {
                        String fileName = p.getFileName().toString();
                        // Exclure les manifests
                        if (!fileName.endsWith("-manifest.json") && !fileName.equals("configs-manifest.json") && !fileName.equals("mods-manifest.json")) {
                            String name = escape(fileName);
                            String hash = sha256Hex(p);
                            entries.add("{\"name\":\"" + name + "\",\"sha256\":\"" + hash + "\"}");
                        }
                    }
                }
            }
        }
        
        String json = "{\n  \"configs\": [\n    " + String.join(",\n    ", entries) + "\n  ]\n}";
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        System.out.println("✓ Manifeste des configs généré: " + outputFile + " (" + entries.size() + " fichiers)");
    }

    private static String sha256Hex(Path file) throws IOException {
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void main(String[] args) {
        try {
            if (args.length >= 2 && "data".equalsIgnoreCase(args[0])) {
                Path dataDir = new File(args[1]).toPath();
                Path outputFile = new File(args.length > 2 ? args[2] : "data/data-manifest.json").toPath();
                generateDataManifest(dataDir, outputFile);
            } else if (args.length >= 2 && "configs".equalsIgnoreCase(args[0])) {
                Path dataConfigsDir = new File(args[1]).toPath();
                Path outputFile = new File(args.length > 2 ? args[2] : "data/configs/configs-manifest.json").toPath();
                generateConfigsManifest(dataConfigsDir, outputFile);
            } else {
                Path dataModsDir = new File(args.length > 0 ? args[0] : "data/mods").toPath();
                Path outputFile = new File(args.length > 1 ? args[1] : "data/mods/mods-manifest.json").toPath();
                generateModsManifest(dataModsDir, outputFile);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la génération du manifest: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

