package com.nexaria.launcher.services.cache;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service de nettoyage des fichiers temporaires et cache
 */
public class CacheCleanupService {

    public static class CleanupResult {
        public long filesDeleted;
        public long bytesFreed;
        public List<String> errors;

        public CleanupResult() {
            this.filesDeleted = 0;
            this.bytesFreed = 0;
            this.errors = new ArrayList<>();
        }

        public String getSummary() {
            double mb = bytesFreed / (1024.0 * 1024.0);
            return String.format("%d fichiers supprimés, %.2f MB libérés", filesDeleted, mb);
        }
    }

    /**
     * Analyse les fichiers qui peuvent être nettoyés
     */
    public static CleanupResult analyzeCleanableFiles() {
        CleanupResult result = new CleanupResult();

        String userHome = System.getProperty("user.home");
        String nexariaDir = userHome + "/.nexaria";

        // Analyser les logs
        analyzePath(nexariaDir + "/logs", result, false);

        // Analyser les crash reports
        analyzePath(nexariaDir + "/crash-reports", result, false);

        // Analyser le cache
        analyzePath(nexariaDir + "/cache", result, false);

        // Analyser les fichiers temporaires du launcher
        analyzePath(nexariaDir + "/.temp", result, false);

        return result;
    }

    /**
     * Nettoie les fichiers temporaires
     */
    public static CleanupResult cleanupFiles(boolean includeLogs, boolean includeCrashReports, boolean includeCache) {
        CleanupResult result = new CleanupResult();

        if (includeLogs) {
            analyzePath("logs", result, true);
        }

        if (includeCrashReports) {
            analyzePath("crash-reports", result, true);
        }

        if (includeCache) {
            analyzePath("cache", result, true);
            // Nettoyer aussi le cache d'images
            com.nexaria.launcher.services.cache.ImageCache.getInstance().clear();
        }

        return result;
    }

    /**
     * Analyse ou supprime les fichiers d'un répertoire
     */
    private static void analyzePath(String pathStr, CleanupResult result, boolean delete) {
        Path path = Paths.get(pathStr);

        if (!Files.exists(path)) {
            return;
        }

        AtomicLong size = new AtomicLong(0);
        AtomicLong count = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        long fileSize = Files.size(file);
                        size.addAndGet(fileSize);
                        count.incrementAndGet();

                        if (delete) {
                            Files.delete(file);
                        }
                    } catch (IOException e) {
                        result.errors.add("Erreur avec " + file.getFileName() + ": " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (delete && !dir.equals(path)) {
                        try {
                            // Supprimer le répertoire s'il est vide
                            if (Files.list(dir).count() == 0) {
                                Files.delete(dir);
                            }
                        } catch (IOException e) {
                            // Ignorer les erreurs de suppression de répertoire
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            result.filesDeleted += count.get();
            result.bytesFreed += size.get();

        } catch (IOException e) {
            result.errors.add("Erreur parcours " + pathStr + ": " + e.getMessage());
        }
    }

    /**
     * Nettoie les anciens logs (plus de 7 jours)
     */
    public static CleanupResult cleanupOldLogs(int daysOld) {
        CleanupResult result = new CleanupResult();
        Path logsPath = Paths.get("logs");

        if (!Files.exists(logsPath)) {
            return result;
        }

        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);

        try {
            Files.walk(logsPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(file).toMillis();
                            if (lastModified < cutoffTime) {
                                long size = Files.size(file);
                                Files.delete(file);
                                result.filesDeleted++;
                                result.bytesFreed += size;
                            }
                        } catch (IOException e) {
                            result.errors.add("Erreur suppression " + file.getFileName() + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            result.errors.add("Erreur parcours logs: " + e.getMessage());
        }

        return result;
    }
}
