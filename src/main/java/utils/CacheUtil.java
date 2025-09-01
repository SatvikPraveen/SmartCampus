// File location: src/main/java/utils/CacheUtil.java
package utils;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for caching operations and cache management
 * Provides various caching strategies and cache manipulation utilities
 */
public final class CacheUtil {
    
    private CacheUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== CACHE ENTRY CLASS ====================
    
    /**
     * Represents a cached entry with expiration and metadata
     */
    public static class CacheEntry<T> {
        private final T value;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private volatile LocalDateTime lastAccessed;
        private volatile long accessCount;
        private final Map<String, Object> metadata;
        
        public CacheEntry(T value, Duration ttl) {
            this.value = value;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = ttl != null ? createdAt.plus(ttl) : null;
            this.lastAccessed = createdAt;
            this.accessCount = 0;
            this.metadata = new ConcurrentHashMap<>();
        }
        
        public T getValue() {
            this.lastAccessed = LocalDateTime.now();
            this.accessCount++;
            return value;
        }
        
        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }
        
        public boolean isExpired(LocalDateTime now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }
        
        public Duration getAge() {
            return Duration.between(createdAt, LocalDateTime.now());
        }
        
        public Duration getTimeSinceLastAccess() {
            return Duration.between(lastAccessed, LocalDateTime.now());
        }
        
        // Getters
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public long getAccessCount() { return accessCount; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
    }
    
    // ==================== SIMPLE CACHE CLASS ====================
    
    /**
     * Simple in-memory cache with TTL support
     */
    public static class SimpleCache<K, V> {
        private final Map<K, CacheEntry<V>> cache;
        private final Duration defaultTtl;
        private final int maxSize;
        private final ReentrantReadWriteLock lock;
        
        public SimpleCache() {
            this(1000, Duration.ofHours(1));
        }
        
        public SimpleCache(int maxSize, Duration defaultTtl) {
            this.cache = new ConcurrentHashMap<>();
            this.defaultTtl = defaultTtl;
            this.maxSize = maxSize;
            this.lock = new ReentrantReadWriteLock();
        }
        
        public V get(K key) {
            lock.readLock().lock();
            try {
                CacheEntry<V> entry = cache.get(key);
                if (entry == null || entry.isExpired()) {
                    return null;
                }
                return entry.getValue();
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public V get(K key, Function<K, V> loader) {
            V value = get(key);
            if (value != null) {
                return value;
            }
            
            lock.writeLock().lock();
            try {
                // Double-check after acquiring write lock
                CacheEntry<V> entry = cache.get(key);
                if (entry != null && !entry.isExpired()) {
                    return entry.getValue();
                }
                
                // Load and cache the value
                V loadedValue = loader.apply(key);
                if (loadedValue != null) {
                    put(key, loadedValue);
                }
                return loadedValue;
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public void put(K key, V value) {
            put(key, value, defaultTtl);
        }
        
        public void put(K key, V value, Duration ttl) {
            if (key == null || value == null) return;
            
            lock.writeLock().lock();
            try {
                // Evict if at capacity
                if (cache.size() >= maxSize) {
                    evictOldest();
                }
                
                cache.put(key, new CacheEntry<>(value, ttl));
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public V remove(K key) {
            lock.writeLock().lock();
            try {
                CacheEntry<V> entry = cache.remove(key);
                return entry != null ? entry.value : null;
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public boolean containsKey(K key) {
            lock.readLock().lock();
            try {
                CacheEntry<V> entry = cache.get(key);
                return entry != null && !entry.isExpired();
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public void clear() {
            lock.writeLock().lock();
            try {
                cache.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public int size() {
            lock.readLock().lock();
            try {
                cleanExpired();
                return cache.size();
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public boolean isEmpty() {
            return size() == 0;
        }
        
        public Set<K> keySet() {
            lock.readLock().lock();
            try {
                cleanExpired();
                return new HashSet<>(cache.keySet());
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public CacheStats getStats() {
            lock.readLock().lock();
            try {
                cleanExpired();
                long totalAccesses = cache.values().stream()
                    .mapToLong(CacheEntry::getAccessCount)
                    .sum();
                
                return new CacheStats(
                    cache.size(),
                    maxSize,
                    totalAccesses,
                    getHitRate(),
                    getExpiredCount()
                );
            } finally {
                lock.readLock().unlock();
            }
        }
        
        private void cleanExpired() {
            LocalDateTime now = LocalDateTime.now();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        }
        
        private void evictOldest() {
            if (cache.isEmpty()) return;
            
            K oldestKey = cache.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().getCreatedAt()))
                .map(Map.Entry::getKey)
                .orElse(null);
                
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
        
        private double getHitRate() {
            long totalAccesses = cache.values().stream()
                .mapToLong(CacheEntry::getAccessCount)
                .sum();
            return totalAccesses > 0 ? (double) cache.size() / totalAccesses : 0.0;
        }
        
        private long getExpiredCount() {
            LocalDateTime now = LocalDateTime.now();
            return cache.values().stream()
                .mapToLong(entry -> entry.isExpired(now) ? 1 : 0)
                .sum();
        }
    }
    
    // ==================== LRU CACHE CLASS ====================
    
    /**
     * LRU (Least Recently Used) cache implementation
     */
    public static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;
        private final Duration defaultTtl;
        private final Map<K, LocalDateTime> expirationTimes;
        
        public LRUCache(int maxSize) {
            this(maxSize, null);
        }
        
        public LRUCache(int maxSize, Duration defaultTtl) {
            super(16, 0.75f, true); // Access-order LinkedHashMap
            this.maxSize = maxSize;
            this.defaultTtl = defaultTtl;
            this.expirationTimes = new ConcurrentHashMap<>();
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            boolean shouldRemove = size() > maxSize;
            if (shouldRemove) {
                expirationTimes.remove(eldest.getKey());
            }
            return shouldRemove;
        }
        
        @Override
        public V get(Object key) {
            cleanExpired();
            @SuppressWarnings("unchecked")
            K k = (K) key;
            
            if (isExpired(k)) {
                remove(key);
                return null;
            }
            
            return super.get(key);
        }
        
        @Override
        public V put(K key, V value) {
            cleanExpired();
            if (defaultTtl != null) {
                expirationTimes.put(key, LocalDateTime.now().plus(defaultTtl));
            }
            return super.put(key, value);
        }
        
        public V put(K key, V value, Duration ttl) {
            cleanExpired();
            if (ttl != null) {
                expirationTimes.put(key, LocalDateTime.now().plus(ttl));
            }
            V result = super.put(key, value);
            return result;
        }
        
        @Override
        public V remove(Object key) {
            expirationTimes.remove(key);
            return super.remove(key);
        }
        
        @Override
        public void clear() {
            expirationTimes.clear();
            super.clear();
        }
        
        private boolean isExpired(K key) {
            LocalDateTime expiration = expirationTimes.get(key);
            return expiration != null && LocalDateTime.now().isAfter(expiration);
        }
        
        private void cleanExpired() {
            LocalDateTime now = LocalDateTime.now();
            List<K> expiredKeys = new ArrayList<>();
            
            for (Map.Entry<K, LocalDateTime> entry : expirationTimes.entrySet()) {
                if (entry.getValue() != null && now.isAfter(entry.getValue())) {
                    expiredKeys.add(entry.getKey());
                }
            }
            
            for (K key : expiredKeys) {
                remove(key);
            }
        }
    }
    
    // ==================== CACHE STATISTICS ====================
    
    /**
     * Cache statistics holder
     */
    public static class CacheStats {
        private final int size;
        private final int maxSize;
        private final long totalAccesses;
        private final double hitRate;
        private final long expiredCount;
        
        public CacheStats(int size, int maxSize, long totalAccesses, double hitRate, long expiredCount) {
            this.size = size;
            this.maxSize = maxSize;
            this.totalAccesses = totalAccesses;
            this.hitRate = hitRate;
            this.expiredCount = expiredCount;
        }
        
        public int getSize() { return size; }
        public int getMaxSize() { return maxSize; }
        public long getTotalAccesses() { return totalAccesses; }
        public double getHitRate() { return hitRate; }
        public long getExpiredCount() { return expiredCount; }
        public double getUtilization() { return maxSize > 0 ? (double) size / maxSize : 0.0; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{size=%d, maxSize=%d, utilization=%.2f%%, hitRate=%.2f%%, totalAccesses=%d, expired=%d}",
                size, maxSize, getUtilization() * 100, hitRate * 100, totalAccesses, expiredCount
            );
        }
    }
    
    // ==================== MEMOIZATION UTILITIES ====================
    
    /**
     * Memoizes a function with default cache
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        return memoize(function, 1000, Duration.ofHours(1));
    }
    
    /**
     * Memoizes a function with custom cache settings
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function, int maxSize, Duration ttl) {
        SimpleCache<T, R> cache = new SimpleCache<>(maxSize, ttl);
        return input -> cache.get(input, function);
    }
    
    /**
     * Memoizes a supplier
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return memoize(supplier, Duration.ofHours(1));
    }
    
    /**
     * Memoizes a supplier with TTL
     */
    public static <T> Supplier<T> memoize(Supplier<T> supplier, Duration ttl) {
        SimpleCache<String, T> cache = new SimpleCache<>(1, ttl);
        String key = "singleton";
        return () -> cache.get(key, k -> supplier.get());
    }
    
    // ==================== CACHE KEY UTILITIES ====================
    
    /**
     * Generates cache key from multiple objects
     */
    public static String generateKey(Object... parts) {
        if (parts == null || parts.length == 0) {
            return "empty";
        }
        
        StringBuilder keyBuilder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                keyBuilder.append(":");
            }
            keyBuilder.append(parts[i] != null ? parts[i].toString() : "null");
        }
        return keyBuilder.toString();
    }
    
    /**
     * Generates cache key with prefix
     */
    public static String generateKey(String prefix, Object... parts) {
        String key = generateKey(parts);
        return prefix != null ? prefix + ":" + key : key;
    }
    
    /**
     * Normalizes cache key (lowercase, trim, replace spaces)
     */
    public static String normalizeKey(String key) {
        if (key == null) return "null";
        return key.toLowerCase().trim().replaceAll("\\s+", "_");
    }
    
    /**
     * Generates hash-based key for complex objects
     */
    public static String generateHashKey(Object... objects) {
        int hash = Objects.hash(objects);
        return "hash_" + Math.abs(hash);
    }
    
    // ==================== CACHE OPERATIONS ====================
    
    /**
     * Bulk put operations
     */
    public static <K, V> void putAll(SimpleCache<K, V> cache, Map<K, V> entries) {
        if (cache == null || entries == null) return;
        entries.forEach(cache::put);
    }
    
    /**
     * Bulk put with custom TTL
     */
    public static <K, V> void putAll(SimpleCache<K, V> cache, Map<K, V> entries, Duration ttl) {
        if (cache == null || entries == null) return;
        entries.forEach((key, value) -> cache.put(key, value, ttl));
    }
    
    /**
     * Gets multiple values from cache
     */
    public static <K, V> Map<K, V> getAll(SimpleCache<K, V> cache, Collection<K> keys) {
        if (cache == null || keys == null) return new HashMap<>();
        
        Map<K, V> result = new HashMap<>();
        for (K key : keys) {
            V value = cache.get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
    
    /**
     * Removes multiple keys from cache
     */
    public static <K, V> void removeAll(SimpleCache<K, V> cache, Collection<K> keys) {
        if (cache == null || keys == null) return;
        keys.forEach(cache::remove);
    }
    
    /**
     * Refreshes cache entry (re-loads value)
     */
    public static <K, V> V refresh(SimpleCache<K, V> cache, K key, Function<K, V> loader) {
        if (cache == null || key == null || loader == null) return null;
        
        cache.remove(key);
        return cache.get(key, loader);
    }
    
    // ==================== CACHE WARMING ====================
    
    /**
     * Warms up cache with pre-computed values
     */
    public static <K, V> void warmUp(SimpleCache<K, V> cache, Function<K, V> loader, Collection<K> keys) {
        if (cache == null || loader == null || keys == null) return;
        
        keys.parallelStream().forEach(key -> {
            try {
                V value = loader.apply(key);
                if (value != null) {
                    cache.put(key, value);
                }
            } catch (Exception e) {
                // Log error but continue warming other keys
                System.err.println("Failed to warm cache for key: " + key + ", error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Async cache warming
     */
    public static <K, V> void warmUpAsync(SimpleCache<K, V> cache, Function<K, V> loader, Collection<K> keys) {
        if (cache == null || loader == null || keys == null) return;
        
        CompletableFutureUtil.runAsync(() -> warmUp(cache, loader, keys));
    }
    
    // ==================== CACHE INSPECTION ====================
    
    /**
     * Gets all cache entries with metadata
     */
    public static <K, V> Map<K, CacheEntry<V>> getAllEntries(SimpleCache<K, V> cache) {
        if (cache == null) return new HashMap<>();
        
        Map<K, CacheEntry<V>> entries = new HashMap<>();
        for (K key : cache.keySet()) {
            CacheEntry<V> entry = cache.cache.get(key);
            if (entry != null && !entry.isExpired()) {
                entries.put(key, entry);
            }
        }
        return entries;
    }
    
    /**
     * Gets cache keys by age
     */
    public static <K, V> List<K> getKeysByAge(SimpleCache<K, V> cache, Duration olderThan) {
        if (cache == null) return new ArrayList<>();
        
        LocalDateTime threshold = LocalDateTime.now().minus(olderThan);
        return cache.cache.entrySet().stream()
            .filter(entry -> entry.getValue().getCreatedAt().isBefore(threshold))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets cache keys by access pattern
     */
    public static <K, V> List<K> getKeysByAccessCount(SimpleCache<K, V> cache, long minAccesses) {
        if (cache == null) return new ArrayList<>();
        
        return cache.cache.entrySet().stream()
            .filter(entry -> entry.getValue().getAccessCount() >= minAccesses)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    // ==================== CACHE MAINTENANCE ====================
    
    /**
     * Cleans expired entries from cache
     */
    public static <K, V> int cleanExpired(SimpleCache<K, V> cache) {
        if (cache == null) return 0;
        
        LocalDateTime now = LocalDateTime.now();
        List<K> expiredKeys = cache.cache.entrySet().stream()
            .filter(entry -> entry.getValue().isExpired(now))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        
        expiredKeys.forEach(cache::remove);
        return expiredKeys.size();
    }
    
    /**
     * Evicts least recently accessed entries
     */
    public static <K, V> int evictLRU(SimpleCache<K, V> cache, int count) {
        if (cache == null || count <= 0) return 0;
        
        List<K> lruKeys = cache.cache.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getValue().getLastAccessed()))
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        
        lruKeys.forEach(cache::remove);
        return lruKeys.size();
    }
    
    /**
     * Evicts entries older than specified duration
     */
    public static <K, V> int evictOlderThan(SimpleCache<K, V> cache, Duration age) {
        if (cache == null) return 0;
        
        List<K> oldKeys = getKeysByAge(cache, age);
        oldKeys.forEach(cache::remove);
        return oldKeys.size();
    }
    
    // ==================== UTILITY CLASSES ====================
    
    /**
     * Utility for CompletableFuture operations
     */
    private static class CompletableFutureUtil {
        public static void runAsync(Runnable task) {
            java.util.concurrent.CompletableFuture.runAsync(task);
        }
    }
    
    // ==================== CACHE BUILDER ====================
    
    /**
     * Builder for SimpleCache
     */
    public static class CacheBuilder<K, V> {
        private int maxSize = 1000;
        private Duration defaultTtl = Duration.ofHours(1);
        
        public CacheBuilder<K, V> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public CacheBuilder<K, V> defaultTtl(Duration ttl) {
            this.defaultTtl = ttl;
            return this;
        }
        
        public SimpleCache<K, V> build() {
            return new SimpleCache<>(maxSize, defaultTtl);
        }
    }
    
    /**
     * Creates a new cache builder
     */
    public static <K, V> CacheBuilder<K, V> newBuilder() {
        return new CacheBuilder<>();
    }
}