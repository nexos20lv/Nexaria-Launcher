package com.nexaria.launcher.services.update;

import com.nexaria.launcher.exception.DownloadException;
import com.nexaria.launcher.logging.LoggingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Gestionnaire de téléchargements parallèles avec suivi de progression
 * détaillé.
 * Utilise CompletableFuture pour télécharger plusieurs fichiers simultanément.
 */
public class ParallelDownloadManager {
    private static final LoggingService logger = LoggingService.getLogger(ParallelDownloadManager.class);

    private final int maxConcurrentDownloads;
    private final ExecutorService executorService;
    private Consumer<DownloadProgress> progressCallback;

    public ParallelDownloadManager(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.executorService = Executors.newFixedThreadPool(
                maxConcurrentDownloads,
                r -> {
                    Thread t = new Thread(r, "DownloadWorker");
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * Télécharge plusieurs fichiers en parallèle.
     * 
     * @param tasks Liste des tâches de téléchargement
     * @return Résultat des téléchargements
     */
    public DownloadResult downloadAll(List<DownloadTask> tasks) {
        String correlationId = logger.startOperation("parallel_download");

        logger.eventBuilder()
                .level(LoggingService.LogLevel.INFO)
                .message("Starting parallel downloads")
                .addContext("totalFiles", tasks.size())
                .addContext("maxConcurrent", maxConcurrentDownloads)
                .log();

        long startTime = System.currentTimeMillis();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        List<DownloadException> errors = new ArrayList<>();

        try {
            // Créer les CompletableFuture pour chaque téléchargement
            List<CompletableFuture<DownloadTaskResult>> futures = tasks.stream()
                    .map(task -> CompletableFuture.supplyAsync(() -> {
                        try {
                            logger.debug("Starting download: {}", task.getUrl());

                            // Simuler le téléchargement (à remplacer par l'implémentation réelle)
                            DownloadTaskResult result = downloadFile(task);

                            completed.incrementAndGet();
                            totalBytes.addAndGet(result.getBytesDownloaded());

                            // Notifier la progression
                            if (progressCallback != null) {
                                progressCallback.accept(new DownloadProgress(
                                        completed.get(),
                                        tasks.size(),
                                        totalBytes.get(),
                                        task.getFileName()));
                            }

                            logger.eventBuilder()
                                    .level(LoggingService.LogLevel.DEBUG)
                                    .message("Download completed")
                                    .addContext("file", task.getFileName())
                                    .addContext("bytes", result.getBytesDownloaded())
                                    .log();

                            return result;

                        } catch (Exception e) {
                            failed.incrementAndGet();
                            logger.error("Download failed for: " + task.getUrl(), e);

                            DownloadException downloadEx = new DownloadException(
                                    "Failed to download " + task.getFileName(),
                                    e,
                                    task.getUrl(),
                                    task.getDestination().getAbsolutePath(),
                                    0,
                                    0);
                            synchronized (errors) {
                                errors.add(downloadEx);
                            }
                            throw new RuntimeException(downloadEx);
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // Attendre que tous les téléchargements se terminent
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            try {
                allOf.join(); // Bloque jusqu'à ce que tous soient terminés
            } catch (Exception e) {
                // Des téléchargements ont échoué, mais on continue
                logger.warn("Some downloads failed", e);
            }

            long duration = System.currentTimeMillis() - startTime;

            logger.eventBuilder()
                    .level(LoggingService.LogLevel.INFO)
                    .message("Parallel downloads completed")
                    .addContext("completed", completed.get())
                    .addContext("failed", failed.get())
                    .addContext("totalBytes", totalBytes.get())
                    .addContext("durationMs", duration)
                    .log();

            return new DownloadResult(
                    tasks.size(),
                    completed.get(),
                    failed.get(),
                    totalBytes.get(),
                    duration,
                    errors);

        } finally {
            logger.endOperation("parallel_download");
        }
    }

    /**
     * Définit un callback pour suivre la progression.
     */
    public void setProgressCallback(Consumer<DownloadProgress> callback) {
        this.progressCallback = callback;
    }

    /**
     * Télécharge un seul fichier (implémentation de base, à enrichir).
     */
    private DownloadTaskResult downloadFile(DownloadTask task) throws Exception {
        // TODO: Implémenter le téléchargement réel avec:
        // - Gestion des timeouts
        // - Reprise sur erreur
        // - Calcul de hash SHA-256
        // - Progression détaillée

        // Pour l'instant, c'est un placeholder
        File destination = task.getDestination();

        // Simuler un téléchargement
        // Dans la vraie implémentation, utiliser HttpClient5
        long bytesDownloaded = 0; // À calculer

        return new DownloadTaskResult(
                task.getFileName(),
                task.getUrl(),
                destination,
                bytesDownloaded,
                true,
                null);
    }

    /**
     * Arrête le gestionnaire de téléchargements.
     */
    public void shutdown() {
        executorService.shutdown();
        logger.info("ParallelDownloadManager shutdown");
    }

    /**
     * Représente une tâche de téléchargement.
     */
    public static class DownloadTask {
        private final String url;
        private final File destination;
        private final String fileName;
        private final String expectedHash; // SHA-256 optionnel

        public DownloadTask(String url, File destination, String fileName) {
            this(url, destination, fileName, null);
        }

        public DownloadTask(String url, File destination, String fileName, String expectedHash) {
            this.url = url;
            this.destination = destination;
            this.fileName = fileName;
            this.expectedHash = expectedHash;
        }

        public String getUrl() {
            return url;
        }

        public File getDestination() {
            return destination;
        }

        public String getFileName() {
            return fileName;
        }

        public String getExpectedHash() {
            return expectedHash;
        }
    }

    /**
     * Résultat d'une tâche de téléchargement.
     */
    public static class DownloadTaskResult {
        private final String fileName;
        private final String url;
        private final File destination;
        private final long bytesDownloaded;
        private final boolean success;
        private final String error;

        public DownloadTaskResult(String fileName, String url, File destination,
                long bytesDownloaded, boolean success, String error) {
            this.fileName = fileName;
            this.url = url;
            this.destination = destination;
            this.bytesDownloaded = bytesDownloaded;
            this.success = success;
            this.error = error;
        }

        public String getFileName() {
            return fileName;
        }

        public String getUrl() {
            return url;
        }

        public File getDestination() {
            return destination;
        }

        public long getBytesDownloaded() {
            return bytesDownloaded;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Progression des téléchargements.
     */
    public static class DownloadProgress {
        private final int completedCount;
        private final int totalCount;
        private final long totalBytes;
        private final String currentFile;

        public DownloadProgress(int completedCount, int totalCount, long totalBytes, String currentFile) {
            this.completedCount = completedCount;
            this.totalCount = totalCount;
            this.totalBytes = totalBytes;
            this.currentFile = currentFile;
        }

        public int getCompletedCount() {
            return completedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public String getCurrentFile() {
            return currentFile;
        }

        public double getProgressPercentage() {
            return totalCount > 0 ? (completedCount * 100.0 / totalCount) : 0.0;
        }

        @Override
        public String toString() {
            return String.format("Progress: %d/%d (%.1f%%) - %s - %.2f MB",
                    completedCount, totalCount, getProgressPercentage(),
                    currentFile, totalBytes / 1024.0 / 1024.0);
        }
    }

    /**
     * Résultat global des téléchargements.
     */
    public static class DownloadResult {
        private final int totalFiles;
        private final int successCount;
        private final int failureCount;
        private final long totalBytes;
        private final long durationMs;
        private final List<DownloadException> errors;

        public DownloadResult(int totalFiles, int successCount, int failureCount,
                long totalBytes, long durationMs, List<DownloadException> errors) {
            this.totalFiles = totalFiles;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.totalBytes = totalBytes;
            this.durationMs = durationMs;
            this.errors = errors;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public List<DownloadException> getErrors() {
            return errors;
        }

        public boolean isSuccessful() {
            return failureCount == 0;
        }

        public double getAverageSpeedMBps() {
            double seconds = durationMs / 1000.0;
            double megabytes = totalBytes / 1024.0 / 1024.0;
            return seconds > 0 ? megabytes / seconds : 0.0;
        }

        @Override
        public String toString() {
            return String.format("Download completed: %d/%d files, %.2f MB in %.1fs (%.2f MB/s)",
                    successCount, totalFiles, totalBytes / 1024.0 / 1024.0,
                    durationMs / 1000.0, getAverageSpeedMBps());
        }
    }
}
