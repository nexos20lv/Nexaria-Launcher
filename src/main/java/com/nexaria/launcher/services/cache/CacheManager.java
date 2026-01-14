package com.nexaria.launcher.services.cache;

import com.nexaria.launcher.logging.LoggingService;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Gestionnaire de cache intelligent avec TTL (Time To Live) et nettoyage
 * automatique.
 * Thread-safe et optimisé pour les accès concurrents.
 */
public class CacheManager<K, V> {
    private static final LoggingService logger = LoggingService.getLogger(CacheManager.class);

    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final String cacheName;
    private final Duration defaultTtl;
    private final int maxSize;
    private final ScheduledExecutorService cleanupExecutor;

    // Statistiques
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    public CacheManager(String cacheName, Duration defaultTtl, int maxSize) {
        this.cacheName = cacheName;
        this.defaultTtl = defaultTtl;
        this.maxSize = maxSize;

        // Démarrer un nettoyage périodique toutes les 5 minutes
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CacheCleanup-" + cacheName);
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);

        logger.info("Cache '{}' initialized with TTL={}, maxSize={}", cacheName, defaultTtl, maxSize);
    }

    /**
     * Récupère une valeur du cache ou la calcule si absente/expirée.
     * 
     * @param key      La clé de cache
     * @param supplier Fournisseur de la valeur si absente
     * @return La valeur (depuis le cache ou fraîchement calculée)
     */
    public V get(K key, Supplier<V> supplier) {
        return get(key, supplier, defaultTtl);
    }

    /**
     * Récupère une valeur avec un TTL personnalisé.
     */
    public V get(K key, Supplier<V> supplier, Duration ttl) {
        CacheEntry<V> entry = cache.get(key);

        // Cache hit: vérifier si encore valide
        if (entry != null) {
            if (!entry.isExpired()) {
                hits++;
                logger.trace("Cache hit for key: {} in cache '{}'", key, cacheName);
                return entry.getValue();
            } else {
                // Entrée expirée, la supprimer
                cache.remove(key);
                evictions++;
                logger.debug("Cache entry expired for key: {} in cache '{}'", key, cacheName);
            }
        }

        // Cache miss: calculer la valeur
        misses++;
        logger.debug("Cache miss for key: {} in cache '{}', computing value", key, cacheName);

        V value = supplier.get();
        put(key, value, ttl);
        return value;
    }

    /**
     * Récupère une valeur uniquement si elle existe dans le cache.
     * 
     * @return La valeur ou null si absente/expirée
     */
    public V getIfPresent(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            hits++;
            return entry.getValue();
        }
        misses++;
        return null;
    }

    /**
     * Insère une valeur dans le cache avec le TTL par défaut.
     */
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    /**
     * Insère une valeur dans le cache avec un TTL personnalisé.
     */
    public void put(K key, V value, Duration ttl) {
        // Vérifier si on dépasse la taille max
        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            evictOldest();
        }

        CacheEntry<V> entry = new CacheEntry<>(value, ttl);
        cache.put(key, entry);
        logger.trace("Cached value for key: {} in cache '{}' with TTL={}", key, cacheName, ttl);
    }

    /**
     * Supprime une entrée du cache.
     */
    public void invalidate(K key) {
        CacheEntry<V> removed = cache.remove(key);
        if (removed != null) {
            evictions++;
            logger.debug("Invalidated cache entry for key: {} in cache '{}'", key, cacheName);
        }
    }

    /**
     * Vide tout le cache.
     */
    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        evictions += size;
        logger.info("Cleared all {} entries from cache '{}'", size, cacheName);
    }

    /**
     * Nettoie les entrées expirées.
     */
    public void cleanup() {
        logger.debug("Running cleanup for cache '{}'", cacheName);
        int removed = 0;

        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            evictions += removed;
            logger.info("Cleaned up {} expired entries from cache '{}'", removed, cacheName);
        }
    }

    /**
     * Évicte l'entrée la plus ancienne quand le cache est plein.
     */
    private void evictOldest() {
        K oldestKey = null;
        Instant oldestTime = Instant.now();

        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (entry.getValue().getCreatedAt().isBefore(oldestTime)) {
                oldestTime = entry.getValue().getCreatedAt();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            evictions++;
            logger.debug("Evicted oldest entry with key: {} from cache '{}'", oldestKey, cacheName);
        }
    }

    /**
     * Retourne les statistiques du cache.
     */
    public CacheStats getStats() {
        return new CacheStats(
                cacheName,
                cache.size(),
                maxSize,
                hits,
                misses,
                evictions,
                calculateHitRate());
    }

    private double calculateHitRate() {
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * Arrête le nettoyage automatique (à appeler lors de l'arrêt de l'application).
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        logger.info("Cache '{}' shutdown", cacheName);
    }

    /**
     * Entrée de cache avec métadonnées.
     */
    private static class CacheEntry<V> {
        private final V value;
        private final Instant createdAt;
        private final Instant expiresAt;

        public CacheEntry(V value, Duration ttl) {
            this.value = value;
            this.createdAt = Instant.now();
            this.expiresAt = createdAt.plus(ttl);
        }

        public V getValue() {
            return value;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Statistiques du cache.
     */
    public static class CacheStats {
        private final String name;
        private final int size;
        private final int maxSize;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;

        public CacheStats(String name, int size, int maxSize, long hits, long misses,
                long evictions, double hitRate) {
            this.name = name;
            this.size = size;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }

        public String getName() {
            return name;
        }

        public int getSize() {
            return size;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public long getEvictions() {
            return evictions;
        }

        public double getHitRate() {
            return hitRate;
        }

        @Override
        public String toString() {
            return String.format("Cache[%s] size=%d/%d, hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d",
                    name, size, maxSize, hits, misses, hitRate * 100, evictions);
        }
    }
}
