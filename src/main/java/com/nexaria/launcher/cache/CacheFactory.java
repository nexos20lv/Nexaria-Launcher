package com.nexaria.launcher.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory pour créer des caches préconfigurés selon l'usage.
 */
public class CacheFactory {
    private static final Map<String, CacheManager<?, ?>> caches = new HashMap<>();

    /**
     * Cache pour les requêtes GitHub API (TTL 5 minutes, évite rate limiting).
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CacheManager<K, V> getGitHubApiCache() {
        return (CacheManager<K, V>) caches.computeIfAbsent("github-api", 
                k -> new CacheManager<>(k, Duration.ofMinutes(5), 100));
    }

    /**
     * Cache pour les vérifications d'intégrité (TTL 1 heure, basé sur hash).
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CacheManager<K, V> getIntegrityCache() {
        return (CacheManager<K, V>) caches.computeIfAbsent("integrity", 
                k -> new CacheManager<>(k, Duration.ofHours(1), 500));
    }

    /**
     * Cache pour les métadonnées de fichiers (TTL 30 minutes).
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CacheManager<K, V> getFileMetadataCache() {
        return (CacheManager<K, V>) caches.computeIfAbsent("file-metadata", 
                k -> new CacheManager<>(k, Duration.ofMinutes(30), 200));
    }

    /**
     * Cache pour les images téléchargées (TTL 24 heures).
     */
    @SuppressWarnings("unchecked")
    public static <K, V> CacheManager<K, V> getImageCache() {
        return (CacheManager<K, V>) caches.computeIfAbsent("images", 
                k -> new CacheManager<>(k, Duration.ofHours(24), 50));
    }

    /**
     * Cache générique avec paramètres personnalisés.
     */
    public static <K, V> CacheManager<K, V> createCache(String name, Duration ttl, int maxSize) {
        return new CacheManager<>(name, ttl, maxSize);
    }

    /**
     * Arrête tous les caches (à appeler lors de l'arrêt de l'application).
     */
    public static void shutdownAll() {
        caches.values().forEach(cache -> cache.shutdown());
        caches.clear();
    }

    /**
     * Retourne les statistiques de tous les caches.
     */
    public static Map<String, CacheManager.CacheStats> getAllStats() {
        Map<String, CacheManager.CacheStats> stats = new HashMap<>();
        caches.forEach((name, cache) -> stats.put(name, cache.getStats()));
        return stats;
    }
}
