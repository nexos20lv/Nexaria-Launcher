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
 * Génère le manifest complet du dossier data/ avec SHA-256 de tous les fichiers.
 */
public class ManifestGeneratorMojo {

    public static void generateDataManifest(Path dataDir, Path outputFile) throws IOException {
        if (dataDir == null || outputFile == null) {
            throw new IOException("dataDir et outputFile ne peuvent pas être null");
        }
        Files.createDirectories(outputFile.getParent());
        List<String> entries = new ArrayList<>();
        
        if (Files.isDirectory(dataDir)) {
            try (Stream<Path> walk = Files.walk(dataDir)) {
                for (Path p : walk.filter(Files::isRegularFile).toList()) {
                    String fileName = p.getFileName().toString();
                    if (!fileName.endsWith("-manifest.json") && !fileName.startsWith(".")) {
                        String relativePath = dataDir.relativize(p).toString().replace("\\", "/");
                        entries.add("{\"path\":\"" + escape(relativePath) + "\",\"sha256\":\"" + sha256Hex(p) + "\"}");
                    }
                }
            }
        }
        
        String json = "{\n  \"files\": [\n    " + String.join(",\n    ", entries) + "\n  ]\n}";
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        System.out.println("✓ Manifeste data généré: " + outputFile + " (" + entries.size() + " fichiers)");
    }

    private static String sha256Hex(Path file) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Erreur calcul SHA-256: " + file, e);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: java ManifestGeneratorMojo <dataDir> <outputFile>");
                System.exit(1);
            }
            generateDataManifest(new File(args[0]).toPath(), new File(args[1]).toPath());
        } catch (IOException e) {
            System.err.println("Erreur génération manifest: " + e.getMessage());
            System.exit(1);
        }
    }
}
