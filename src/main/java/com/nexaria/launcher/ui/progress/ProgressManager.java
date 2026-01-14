package com.nexaria.launcher.ui.progress;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestionnaire de progression amélioré avec calcul de vitesse, ETA, et fichier en cours.
 * Thread-safe et optimisé pour les mises à jour fréquentes.
 */
public class ProgressManager {
    private final String operationName;
    private final long totalItems;
    private final long totalBytes;
    
    private volatile long currentItem = 0;
    private volatile long currentBytes = 0;
    private volatile String currentFileName = "";
    
    private final Instant startTime;
    private final List<ProgressSnapshot> snapshots = new ArrayList<>();
    private final List<Consumer<ProgressUpdate>> listeners = new ArrayList<>();
    
    // Configuration
    private static final int SPEED_CALCULATION_WINDOW_MS = 5000; // 5 secondes
    private static final int UPDATE_THROTTLE_MS = 100; // 100ms entre les updates UI
    
    private long lastUpdateTime = 0;

    public ProgressManager(String operationName, long totalItems, long totalBytes) {
        this.operationName = operationName;
        this.totalItems = totalItems;
        this.totalBytes = totalBytes;
        this.startTime = Instant.now();
    }

    /**
     * Met à jour la progression.
     * 
     * @param currentItem Nombre d'éléments traités
     * @param currentBytes Nombre d'octets traités
     * @param currentFileName Nom du fichier en cours
     */
    public synchronized void updateProgress(long currentItem, long currentBytes, String currentFileName) {
        this.currentItem = currentItem;
        this.currentBytes = currentBytes;
        this.currentFileName = currentFileName;
        
        // Enregistrer un snapshot pour le calcul de vitesse
        snapshots.add(new ProgressSnapshot(Instant.now(), currentBytes));
        cleanupOldSnapshots();
        
        // Throttle les updates UI pour éviter la surcharge
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime >= UPDATE_THROTTLE_MS) {
            lastUpdateTime = now;
            notifyListeners();
        }
    }

    /**
     * Met à jour uniquement les octets téléchargés (pour les téléchargements).
     */
    public void updateBytes(long bytes, String fileName) {
        updateProgress(currentItem, bytes, fileName);
    }

    /**
     * Incrémente le compteur d'éléments.
     */
    public void incrementItem(String fileName) {
        updateProgress(currentItem + 1, currentBytes, fileName);
    }

    /**
     * Marque l'opération comme terminée.
     */
    public synchronized void complete() {
        this.currentItem = totalItems;
        this.currentBytes = totalBytes;
        notifyListeners();
    }

    /**
     * Ajoute un listener pour les updates de progression.
     */
    public synchronized void addListener(Consumer<ProgressUpdate> listener) {
        listeners.add(listener);
    }

    /**
     * Retire un listener.
     */
    public synchronized void removeListener(Consumer<ProgressUpdate> listener) {
        listeners.remove(listener);
    }

    /**
     * Notifie tous les listeners d'une mise à jour.
     */
    private void notifyListeners() {
        ProgressUpdate update = buildProgressUpdate();
        for (Consumer<ProgressUpdate> listener : listeners) {
            try {
                listener.accept(update);
            } catch (Exception e) {
                // Ignorer les erreurs des listeners
            }
        }
    }

    /**
     * Construit un objet ProgressUpdate avec toutes les métriques.
     */
    private ProgressUpdate buildProgressUpdate() {
        long itemsCompleted = currentItem;
        long bytesCompleted = currentBytes;
        
        double itemsPercent = totalItems > 0 ? (itemsCompleted * 100.0 / totalItems) : 0.0;
        double bytesPercent = totalBytes > 0 ? (bytesCompleted * 100.0 / totalBytes) : 0.0;
        
        double speedBytesPerSecond = calculateSpeed();
        Duration eta = calculateETA(speedBytesPerSecond);
        Duration elapsed = Duration.between(startTime, Instant.now());
        
        return new ProgressUpdate(
                operationName,
                itemsCompleted,
                totalItems,
                itemsPercent,
                bytesCompleted,
                totalBytes,
                bytesPercent,
                currentFileName,
                speedBytesPerSecond,
                eta,
                elapsed
        );
    }

    /**
     * Calcule la vitesse moyenne sur les dernières secondes.
     */
    private double calculateSpeed() {
        if (snapshots.size() < 2) {
            return 0.0;
        }
        
        // Prendre le premier et le dernier snapshot de la fenêtre
        ProgressSnapshot first = snapshots.get(0);
        ProgressSnapshot last = snapshots.get(snapshots.size() - 1);
        
        long bytesDiff = last.bytes - first.bytes;
        long timeDiffMs = Duration.between(first.timestamp, last.timestamp).toMillis();
        
        if (timeDiffMs <= 0) {
            return 0.0;
        }
        
        // Octets par seconde
        return (bytesDiff * 1000.0) / timeDiffMs;
    }

    /**
     * Calcule l'ETA (Estimated Time of Arrival).
     */
    private Duration calculateETA(double speedBytesPerSecond) {
        if (speedBytesPerSecond <= 0 || totalBytes <= 0) {
            return Duration.ZERO;
        }
        
        long remainingBytes = totalBytes - currentBytes;
        long secondsRemaining = (long) (remainingBytes / speedBytesPerSecond);
        
        return Duration.ofSeconds(secondsRemaining);
    }

    /**
     * Nettoie les snapshots trop anciens.
     */
    private void cleanupOldSnapshots() {
        Instant cutoff = Instant.now().minusMillis(SPEED_CALCULATION_WINDOW_MS);
        snapshots.removeIf(snapshot -> snapshot.timestamp.isBefore(cutoff));
    }

    /**
     * Snapshot de progression pour le calcul de vitesse.
     */
    private static class ProgressSnapshot {
        final Instant timestamp;
        final long bytes;

        ProgressSnapshot(Instant timestamp, long bytes) {
            this.timestamp = timestamp;
            this.bytes = bytes;
        }
    }

    /**
     * Mise à jour de progression complète avec toutes les métriques.
     */
    public static class ProgressUpdate {
        private final String operationName;
        private final long itemsCompleted;
        private final long totalItems;
        private final double itemsPercent;
        private final long bytesCompleted;
        private final long totalBytes;
        private final double bytesPercent;
        private final String currentFileName;
        private final double speedBytesPerSecond;
        private final Duration eta;
        private final Duration elapsed;

        public ProgressUpdate(String operationName, long itemsCompleted, long totalItems, 
                             double itemsPercent, long bytesCompleted, long totalBytes, 
                             double bytesPercent, String currentFileName, 
                             double speedBytesPerSecond, Duration eta, Duration elapsed) {
            this.operationName = operationName;
            this.itemsCompleted = itemsCompleted;
            this.totalItems = totalItems;
            this.itemsPercent = itemsPercent;
            this.bytesCompleted = bytesCompleted;
            this.totalBytes = totalBytes;
            this.bytesPercent = bytesPercent;
            this.currentFileName = currentFileName;
            this.speedBytesPerSecond = speedBytesPerSecond;
            this.eta = eta;
            this.elapsed = elapsed;
        }

        // Getters
        public String getOperationName() { return operationName; }
        public long getItemsCompleted() { return itemsCompleted; }
        public long getTotalItems() { return totalItems; }
        public double getItemsPercent() { return itemsPercent; }
        public long getBytesCompleted() { return bytesCompleted; }
        public long getTotalBytes() { return totalBytes; }
        public double getBytesPercent() { return bytesPercent; }
        public String getCurrentFileName() { return currentFileName; }
        public double getSpeedBytesPerSecond() { return speedBytesPerSecond; }
        public Duration getEta() { return eta; }
        public Duration getElapsed() { return elapsed; }

        // Méthodes utilitaires
        public double getSpeedMBps() {
            return speedBytesPerSecond / 1024.0 / 1024.0;
        }

        public double getSpeedKBps() {
            return speedBytesPerSecond / 1024.0;
        }

        public String getFormattedSpeed() {
            double mbps = getSpeedMBps();
            if (mbps >= 1.0) {
                return String.format("%.2f MB/s", mbps);
            } else {
                return String.format("%.1f KB/s", getSpeedKBps());
            }
        }

        public String getFormattedETA() {
            long seconds = eta.getSeconds();
            if (seconds <= 0) {
                return "Calcul...";
            } else if (seconds < 60) {
                return seconds + "s";
            } else if (seconds < 3600) {
                return (seconds / 60) + "m " + (seconds % 60) + "s";
            } else {
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                return hours + "h " + minutes + "m";
            }
        }

        public String getFormattedElapsed() {
            long seconds = elapsed.getSeconds();
            if (seconds < 60) {
                return seconds + "s";
            } else if (seconds < 3600) {
                return (seconds / 60) + "m " + (seconds % 60) + "s";
            } else {
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                return hours + "h " + minutes + "m";
            }
        }

        public String getFormattedBytes() {
            return formatBytes(bytesCompleted) + " / " + formatBytes(totalBytes);
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.1f KB", bytes / 1024.0);
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
            } else {
                return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
            }
        }

        @Override
        public String toString() {
            return String.format("%s: %.1f%% (%d/%d items) - %s - %s - ETA: %s",
                    operationName, itemsPercent, itemsCompleted, totalItems,
                    currentFileName, getFormattedSpeed(), getFormattedETA());
        }
    }
}
