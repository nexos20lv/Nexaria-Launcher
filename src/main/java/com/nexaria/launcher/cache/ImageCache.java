package com.nexaria.launcher.cache;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de cache pour les images (avatars, icônes, etc.)
 * avec expiration automatique et nettoyage
 */
public class ImageCache {
    private static final String CACHE_DIR = System.getProperty("user.home") + "/.nexaria/cache/images";
    private static final long DEFAULT_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 heures
    private static final long MAX_CACHE_SIZE_MB = 50; // 50 MB max

    private final Path cacheDirectory;
    private final Map<String, CacheEntry> memoryCache;
    private final long expiryTime;

    private static class CacheEntry {
        BufferedImage image;
        long timestamp;

        CacheEntry(BufferedImage image) {
            this.image = image;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long expiryMs) {
            return (System.currentTimeMillis() - timestamp) > expiryMs;
        }
    }

    public ImageCache() {
        this(DEFAULT_EXPIRY_MS);
    }

    public ImageCache(long expiryTimeMs) {
        this.expiryTime = expiryTimeMs;
        this.memoryCache = new ConcurrentHashMap<>();
        this.cacheDirectory = Paths.get(CACHE_DIR);

        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            System.err.println("[ImageCache] Erreur création répertoire cache: " + e.getMessage());
        }
    }

    /**
     * Récupère une image depuis le cache ou la télécharge
     */
    public BufferedImage get(String url) {
        String cacheKey = generateCacheKey(url);

        // 1. Vérifier le cache mémoire
        CacheEntry entry = memoryCache.get(cacheKey);
        if (entry != null && !entry.isExpired(expiryTime)) {
            System.out.println("[ImageCache] HIT mémoire: " + url);
            return entry.image;
        }

        // 2. Vérifier le cache disque
        Path cachedFile = cacheDirectory.resolve(cacheKey + ".png");
        if (Files.exists(cachedFile)) {
            try {
                long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(cachedFile).toMillis();
                if (fileAge < expiryTime) {
                    BufferedImage image = ImageIO.read(cachedFile.toFile());
                    memoryCache.put(cacheKey, new CacheEntry(image));
                    System.out.println("[ImageCache] HIT disque: " + url);
                    return image;
                }
            } catch (IOException e) {
                System.err.println("[ImageCache] Erreur lecture cache: " + e.getMessage());
            }
        }

        // 3. Télécharger et mettre en cache
        System.out.println("[ImageCache] MISS: téléchargement " + url);
        return downloadAndCache(url, cacheKey, cachedFile);
    }

    /**
     * Télécharge une image et la met en cache
     */
    private BufferedImage downloadAndCache(String url, String cacheKey, Path cachedFile) {
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            if (image != null) {
                // Sauvegarder sur disque
                ImageIO.write(image, "png", cachedFile.toFile());
                // Sauvegarder en mémoire
                memoryCache.put(cacheKey, new CacheEntry(image));
                System.out.println("[ImageCache] Mise en cache: " + url);
            }
            return image;
        } catch (IOException e) {
            System.err.println("[ImageCache] Erreur téléchargement: " + url + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère une clé de cache unique pour une URL
     */
    private String generateCacheKey(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(url.hashCode());
        }
    }

    /**
     * Nettoie les entrées expirées du cache mémoire
     */
    public void cleanupMemory() {
        memoryCache.entrySet().removeIf(entry -> entry.getValue().isExpired(expiryTime));
        System.out.println("[ImageCache] Nettoyage mémoire: " + memoryCache.size() + " entrées restantes");
    }

    /**
     * Nettoie les fichiers expirés du cache disque
     */
    public long cleanupDisk() {
        long deletedSize = 0;
        try {
            Files.walk(cacheDirectory)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis();
                            if (fileAge > expiryTime) {
                                long size = Files.size(path);
                                Files.delete(path);
                                System.out.println("[ImageCache] Supprimé: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            System.err.println("[ImageCache] Erreur suppression: " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("[ImageCache] Erreur nettoyage disque: " + e.getMessage());
        }
        return deletedSize;
    }

    /**
     * Obtient la taille totale du cache disque en MB
     */
    public long getCacheSizeMB() {
        try {
            return Files.walk(cacheDirectory)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum() / (1024 * 1024);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Vide complètement le cache
     */
    public void clear() {
        memoryCache.clear();
        try {
            Files.walk(cacheDirectory)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("[ImageCache] Erreur suppression: " + e.getMessage());
                        }
                    });
            System.out.println("[ImageCache] Cache vidé");
        } catch (IOException e) {
            System.err.println("[ImageCache] Erreur vidage cache: " + e.getMessage());
        }
    }

    /**
     * Instance singleton
     */
    private static ImageCache instance;

    public static ImageCache getInstance() {
        if (instance == null) {
            instance = new ImageCache();
        }
        return instance;
    }
}
