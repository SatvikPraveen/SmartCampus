// File location: src/main/java/cache/CacheManager.java
package cache;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central cache management system for the campus management application
 * Provides unified caching interface with configurable strategies and automatic cleanup
 */
public class CacheManager {
    
    private static final CacheManager INSTANCE = new CacheManager();
    private final Map<String, CacheContainer<?>> caches;
    private final ScheduledExecutorService cleanupExecutor;
    private final ReentrantReadWriteLock lock;
    private volatile boolean isRunning;
    
    // Cache statistics
    private long totalHits;
    private long totalMisses;
    private long totalEvictions;
    
    // Default configuration
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_TTL_MINUTES = 60;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    private CacheManager() {
        this.caches = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CacheManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        this.lock = new ReentrantReadWriteLock();
        this.isRunning = true;
        
        // Start cleanup task
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 
            CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    public static CacheManager getInstance() {
        return INSTANCE;
    }
    
    // ==================== CACHE CONFIGURATION ====================
    
    /**
     * Configuration for cache instances
     */
    public static class CacheConfig {
        private int maxSize = DEFAULT_MAX_SIZE;
        private long ttlMinutes = DEFAULT_TTL_MINUTES;
        private CacheStrategy strategy = CacheStrategy.LRU;
        private boolean enableStatistics = true;
        private boolean autoCleanup = true;
        
        public static CacheConfig defaultConfig() {
            return new CacheConfig();
        }
        
        public CacheConfig maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public CacheConfig ttl(long minutes) {
            this.ttlMinutes = minutes;
            return this;
        }
        
        public CacheConfig strategy(CacheStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public CacheConfig enableStatistics(boolean enable) {
            this.enableStatistics = enable;
            return this;
        }
        
        public CacheConfig autoCleanup(boolean enable) {
            this.autoCleanup = enable;
            return this;
        }
        
        // Getters
        public int getMaxSize() { return maxSize; }
        public long getTtlMinutes() { return ttlMinutes; }
        public CacheStrategy getStrategy() { return strategy; }
        public boolean isStatisticsEnabled() { return enableStatistics; }
        public boolean isAutoCleanupEnabled() { return autoCleanup; }
    }
    
    // ==================== CACHE CONTAINER ====================
    
    /**
     * Internal container for cache instances
     */
    private static class CacheContainer<T> {
        private final String name;
        private final CacheConfig config;
        private final Map<String, CacheEntry<T>> data;
        private final Queue<String> accessOrder;
        private final LocalDateTime createdAt;
        private long hits;
        private long misses;
        private long evictions;
        
        public CacheContainer(String name, CacheConfig config) {
            this.name = name;
            this.config = config;
            this.data = new ConcurrentHashMap<>();
            this.accessOrder = switch (config.getStrategy()) {
                case LRU -> new LinkedList<>();
                case FIFO -> new LinkedList<>();
                case LIFO -> Collections.asLifoQueue(new LinkedList<>());
                default -> new LinkedList<>();
            };
            this.createdAt = LocalDateTime.now();
        }
        
        public synchronized T get(String key) {
            CacheEntry<T> entry = data.get(key);
            if (entry == null) {
                misses++;
                return null;
            }
            
            if (entry.isExpired()) {
                data.remove(key);
                accessOrder.remove(key);
                misses++;
                evictions++;
                return null;
            }
            
            // Update access information
            entry.updateAccess();
            if (config.getStrategy() == CacheStrategy.LRU) {
                accessOrder.remove(key);
                accessOrder.offer(key);
            }
            
            hits++;
            return entry.getValue();
        }
        
        public synchronized void put(String key, T value) {
            // Remove existing entry if present
            if (data.containsKey(key)) {
                accessOrder.remove(key);
            } else if (data.size() >= config.getMaxSize()) {
                // Evict based on strategy
                evict();
            }
            
            CacheEntry<T> entry = new CacheEntry<>(value, config.getTtlMinutes());
            data.put(key, entry);
            accessOrder.offer(key);
        }
        
        public synchronized void remove(String key) {
            if (data.remove(key) != null) {
                accessOrder.remove(key);
            }
        }
        
        public synchronized void clear() {
            data.clear();
            accessOrder.clear();
        }
        
        public synchronized int size() {
            return data.size();
        }
        
        public synchronized boolean isEmpty() {
            return data.isEmpty();
        }
        
        public synchronized boolean containsKey(String key) {
            CacheEntry<T> entry = data.get(key);
            if (entry == null) return false;
            if (entry.isExpired()) {
                data.remove(key);
                accessOrder.remove(key);
                evictions++;
                return false;
            }
            return true;
        }
        
        public synchronized Set<String> keySet() {
            // Clean expired entries first
            cleanExpired();
            return new HashSet<>(data.keySet());
        }
        
        public synchronized void cleanExpired() {
            Iterator<Map.Entry<String, CacheEntry<T>>> it = data.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CacheEntry<T>> entry = it.next();
                if (entry.getValue().isExpired()) {
                    it.remove();
                    accessOrder.remove(entry.getKey());
                    evictions++;
                }
            }
        }
        
        private void evict() {
            if (!accessOrder.isEmpty()) {
                String keyToEvict = accessOrder.poll();
                data.remove(keyToEvict);
                evictions++;
            }
        }
        
        public CacheStats getStats() {
            return new CacheStats(name, hits, misses, evictions, data.size(), 
                                config.getMaxSize(), createdAt);
        }
    }
    
    // ==================== CACHE ENTRY ====================
    
    /**
     * Individual cache entry with TTL support
     */
    private static class CacheEntry<T> {
        private final T value;
        private final LocalDateTime createdAt;
        private final long ttlMinutes;
        private LocalDateTime lastAccessed;
        private long accessCount;
        
        public CacheEntry(T value, long ttlMinutes) {
            this.value = value;
            this.createdAt = LocalDateTime.now();
            this.lastAccessed = this.createdAt;
            this.ttlMinutes = ttlMinutes;
            this.accessCount = 0;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            if (ttlMinutes <= 0) return false;
            return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now()) >= ttlMinutes;
        }
        
        public void updateAccess() {
            this.lastAccessed = LocalDateTime.now();
            this.accessCount++;
        }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public long getAccessCount() { return accessCount; }
    }
    
    // ==================== CACHE STATISTICS ====================
    
    /**
     * Cache statistics information
     */
    public static class CacheStats {
        private final String name;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final int currentSize;
        private final int maxSize;
        private final LocalDateTime createdAt;
        private final double hitRatio;
        
        public CacheStats(String name, long hits, long misses, long evictions, 
                         int currentSize, int maxSize, LocalDateTime createdAt) {
            this.name = name;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.createdAt = createdAt;
            this.hitRatio = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
        }
        
        // Getters
        public String getName() { return name; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public double getHitRatio() { return hitRatio; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{name='%s', hits=%d, misses=%d, evictions=%d, " +
                               "currentSize=%d, maxSize=%d, hitRatio=%.2f%%}", 
                               name, hits, misses, evictions, currentSize, maxSize, hitRatio * 100);
        }
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Creates a new cache with default configuration
     */
    public <T> void createCache(String name) {
        createCache(name, CacheConfig.defaultConfig());
    }
    
    /**
     * Creates a new cache with custom configuration
     */
    public <T> void createCache(String name, CacheConfig config) {
        lock.writeLock().lock();
        try {
            if (caches.containsKey(name)) {
                throw new IllegalArgumentException("Cache with name '" + name + "' already exists");
            }
            caches.put(name, new CacheContainer<T>(name, config));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets value from cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key) {
        lock.readLock().lock();
        try {
            CacheContainer<T> cache = (CacheContainer<T>) caches.get(cacheName);
            if (cache == null) {
                totalMisses++;
                return null;
            }
            
            T value = cache.get(key);
            if (value != null) {
                totalHits++;
            } else {
                totalMisses++;
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Puts value into cache
     */
    @SuppressWarnings("unchecked")
    public <T> void put(String cacheName, String key, T value) {
        lock.readLock().lock();
        try {
            CacheContainer<T> cache = (CacheContainer<T>) caches.get(cacheName);
            if (cache == null) {
                throw new IllegalArgumentException("Cache '" + cacheName + "' does not exist");
            }
            cache.put(key, value);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Removes value from cache
     */
    public void remove(String cacheName, String key) {
        lock.readLock().lock();
        try {
            CacheContainer<?> cache = caches.get(cacheName);
            if (cache != null) {
                cache.remove(key);
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Clears all entries from cache
     */
    public void clear(String cacheName) {
        lock.readLock().lock();
        try {
            CacheContainer<?> cache = caches.get(cacheName);
            if (cache != null) {
                cache.clear();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Removes cache entirely
     */
    public void removeCache(String cacheName) {
        lock.writeLock().lock();
        try {
            CacheContainer<?> cache = caches.remove(cacheName);
            if (cache != null) {
                cache.clear();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if cache exists
     */
    public boolean cacheExists(String cacheName) {
        lock.readLock().lock();
        try {
            return caches.containsKey(cacheName);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if key exists in cache
     */
    public boolean containsKey(String cacheName, String key) {
        lock.readLock().lock();
        try {
            CacheContainer<?> cache = caches.get(cacheName);
            return cache != null && cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets cache size
     */
    public int size(String cacheName) {
        lock.readLock().lock();
        try {
            CacheContainer<?> cache = caches.get(cacheName);
            return cache != null ? cache.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all cache names
     */
    public Set<String> getCacheNames() {
        lock.readLock().lock();
        try {
            return new HashSet<>(caches.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all keys from cache
     */
    public Set<String> getKeys(String cacheName) {
        lock.readLock().lock();
        try {
            CacheContainer<?> cache = caches.get(cacheName);
            return cache != null ? cache.keySet() : new HashSet<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Gets statistics for specific cache
     */
    public CacheStats getCacheStats(String cacheName) {
        lock.readLock().lock();
        try {
            CacheContainer<?> cache = caches.get(cacheName);
            return cache != null ? cache.getStats() : null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets global cache statistics
     */
    public Map<String, Object> getGlobalStats() {
        lock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCaches", caches.size());
            stats.put("totalHits", totalHits);
            stats.put("totalMisses", totalMisses);
            stats.put("totalEvictions", totalEvictions);
            stats.put("globalHitRatio", (totalHits + totalMisses) > 0 ? 
                     (double) totalHits / (totalHits + totalMisses) : 0.0);
            
            int totalEntries = caches.values().stream().mapToInt(CacheContainer::size).sum();
            stats.put("totalEntries", totalEntries);
            
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets statistics for all caches
     */
    public List<CacheStats> getAllCacheStats() {
        lock.readLock().lock();
        try {
            return caches.values().stream()
                        .map(CacheContainer::getStats)
                        .collect(java.util.stream.Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ==================== MAINTENANCE ====================
    
    /**
     * Performs cleanup of expired entries across all caches
     */
    public void performCleanup() {
        if (!isRunning) return;
        
        lock.readLock().lock();
        try {
            for (CacheContainer<?> cache : caches.values()) {
                cache.cleanExpired();
                totalEvictions += cache.evictions;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Clears all caches
     */
    public void clearAll() {
        lock.readLock().lock();
        try {
            caches.values().forEach(CacheContainer::clear);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets memory usage estimation
     */
    public long getEstimatedMemoryUsage() {
        lock.readLock().lock();
        try {
            // Rough estimation based on number of entries
            // This is a simplified calculation
            long totalEntries = caches.values().stream().mapToLong(CacheContainer::size).sum();
            return totalEntries * 100; // Assume ~100 bytes per entry on average
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Shuts down the cache manager
     */
    public void shutdown() {
        isRunning = false;
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clearAll();
    }
    
    // ==================== CACHE PRESETS ====================
    
    /**
     * Creates cache for student data
     */
    public void createStudentCache() {
        createCache("students", CacheConfig.defaultConfig()
            .maxSize(2000)
            .ttl(30)
            .strategy(CacheStrategy.LRU));
    }
    
    /**
     * Creates cache for course data
     */
    public void createCourseCache() {
        createCache("courses", CacheConfig.defaultConfig()
            .maxSize(1000)
            .ttl(60)
            .strategy(CacheStrategy.LRU));
    }
    
    /**
     * Creates cache for professor data
     */
    public void createProfessorCache() {
        createCache("professors", CacheConfig.defaultConfig()
            .maxSize(500)
            .ttl(45)
            .strategy(CacheStrategy.LRU));
    }
    
    /**
     * Creates cache for enrollment data
     */
    public void createEnrollmentCache() {
        createCache("enrollments", CacheConfig.defaultConfig()
            .maxSize(5000)
            .ttl(15)
            .strategy(CacheStrategy.FIFO));
    }
    
    /**
     * Creates cache for session data
     */
    public void createSessionCache() {
        createCache("sessions", CacheConfig.defaultConfig()
            .maxSize(1000)
            .ttl(120)
            .strategy(CacheStrategy.LRU));
    }
}