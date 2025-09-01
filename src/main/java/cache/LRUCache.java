// File location: src/main/java/cache/LRUCache.java
package cache;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Generic LRU (Least Recently Used) Cache implementation
 * Thread-safe implementation with configurable capacity and TTL support
 */
public class LRUCache<K, V> {
    
    private final int capacity;
    private final long ttlMillis;
    private final Map<K, Node> map;
    private final Node head;
    private final Node tail;
    private final ReentrantReadWriteLock lock;
    
    // Statistics
    private long hits;
    private long misses;
    private long evictions;
    private final LocalDateTime createdAt;
    
    /**
     * Node class for doubly linked list
     */
    private class Node {
        K key;
        V value;
        long timestamp;
        Node prev;
        Node next;
        
        Node() {
            this.timestamp = System.currentTimeMillis();
        }
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return ttlMillis > 0 && (System.currentTimeMillis() - timestamp) > ttlMillis;
        }
        
        void updateTimestamp() {
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates LRU cache with specified capacity (no TTL)
     */
    public LRUCache(int capacity) {
        this(capacity, 0);
    }
    
    /**
     * Creates LRU cache with capacity and TTL
     * @param capacity Maximum number of entries
     * @param ttlMillis Time-to-live in milliseconds (0 for no expiration)
     */
    public LRUCache(int capacity, long ttlMillis) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        
        this.capacity = capacity;
        this.ttlMillis = ttlMillis;
        this.map = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.createdAt = LocalDateTime.now();
        
        // Initialize dummy head and tail nodes
        this.head = new Node();
        this.tail = new Node();
        head.next = tail;
        tail.prev = head;
    }
    
    // ==================== MAIN OPERATIONS ====================
    
    /**
     * Gets value by key
     */
    public V get(K key) {
        if (key == null) return null;
        
        lock.writeLock().lock();
        try {
            Node node = map.get(key);
            
            if (node == null) {
                misses++;
                return null;
            }
            
            if (node.isExpired()) {
                removeNode(node);
                map.remove(key);
                misses++;
                evictions++;
                return null;
            }
            
            // Move to front (most recently used)
            moveToHead(node);
            node.updateTimestamp();
            hits++;
            return node.value;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Puts key-value pair
     */
    public V put(K key, V value) {
        if (key == null) return null;
        
        lock.writeLock().lock();
        try {
            Node existing = map.get(key);
            
            if (existing != null) {
                // Update existing node
                V oldValue = existing.value;
                existing.value = value;
                existing.updateTimestamp();
                moveToHead(existing);
                return oldValue;
            }
            
            // Create new node
            Node newNode = new Node(key, value);
            
            if (map.size() >= capacity) {
                // Remove least recently used
                Node tail = removeTail();
                map.remove(tail.key);
                evictions++;
            }
            
            map.put(key, newNode);
            addToHead(newNode);
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Removes key from cache
     */
    public V remove(K key) {
        if (key == null) return null;
        
        lock.writeLock().lock();
        try {
            Node node = map.remove(key);
            if (node != null) {
                removeNode(node);
                return node.value;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if key exists (without updating access order)
     */
    public boolean containsKey(K key) {
        if (key == null) return false;
        
        lock.readLock().lock();
        try {
            Node node = map.get(key);
            if (node == null) return false;
            
            if (node.isExpired()) {
                // Remove expired entry
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Double-check after acquiring write lock
                    node = map.get(key);
                    if (node != null && node.isExpired()) {
                        map.remove(key);
                        removeNode(node);
                        evictions++;
                        return false;
                    }
                    return node != null;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets current size
     */
    public int size() {
        lock.readLock().lock();
        try {
            return map.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if cache is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Clears all entries
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
            head.next = tail;
            tail.prev = head;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ==================== DOUBLY LINKED LIST OPERATIONS ====================
    
    private void addToHead(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }
    
    private Node removeTail() {
        Node last = tail.prev;
        removeNode(last);
        return last;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets all keys in access order (most recent first)
     */
    public List<K> getKeysInAccessOrder() {
        lock.readLock().lock();
        try {
            List<K> keys = new ArrayList<>();
            Node current = head.next;
            while (current != tail) {
                if (!current.isExpired()) {
                    keys.add(current.key);
                }
                current = current.next;
            }
            return keys;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all keys
     */
    public Set<K> keySet() {
        lock.readLock().lock();
        try {
            cleanExpired();
            return new HashSet<>(map.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all values
     */
    public Collection<V> values() {
        lock.readLock().lock();
        try {
            cleanExpired();
            return map.values().stream()
                     .map(node -> node.value)
                     .collect(java.util.stream.Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all entries
     */
    public Set<Map.Entry<K, V>> entrySet() {
        lock.readLock().lock();
        try {
            cleanExpired();
            return map.entrySet().stream()
                     .collect(java.util.stream.Collectors.toMap(
                         Map.Entry::getKey,
                         entry -> entry.getValue().value,
                         (e1, e2) -> e1,
                         LinkedHashMap::new
                     )).entrySet();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Puts value if key is absent
     */
    public V putIfAbsent(K key, V value) {
        if (key == null) return null;
        
        lock.writeLock().lock();
        try {
            Node existing = map.get(key);
            if (existing != null && !existing.isExpired()) {
                moveToHead(existing);
                existing.updateTimestamp();
                return existing.value;
            }
            
            put(key, value);
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Replaces value if key exists
     */
    public V replace(K key, V value) {
        if (key == null) return null;
        
        lock.writeLock().lock();
        try {
            Node existing = map.get(key);
            if (existing != null && !existing.isExpired()) {
                V oldValue = existing.value;
                existing.value = value;
                existing.updateTimestamp();
                moveToHead(existing);
                return oldValue;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Replaces value if current value matches expected
     */
    public boolean replace(K key, V expectedValue, V newValue) {
        if (key == null) return false;
        
        lock.writeLock().lock();
        try {
            Node existing = map.get(key);
            if (existing != null && !existing.isExpired()) {
                if (Objects.equals(existing.value, expectedValue)) {
                    existing.value = newValue;
                    existing.updateTimestamp();
                    moveToHead(existing);
                    return true;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ==================== EXPIRATION MANAGEMENT ====================
    
    /**
     * Removes all expired entries
     */
    public int cleanExpired() {
        lock.writeLock().lock();
        try {
            int removed = 0;
            Iterator<Map.Entry<K, Node>> it = map.entrySet().iterator();
            
            while (it.hasNext()) {
                Map.Entry<K, Node> entry = it.next();
                Node node = entry.getValue();
                
                if (node.isExpired()) {
                    it.remove();
                    removeNode(node);
                    removed++;
                    evictions++;
                }
            }
            
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets remaining TTL for key (in milliseconds)
     */
    public long getRemainingTTL(K key) {
        if (key == null || ttlMillis <= 0) return -1;
        
        lock.readLock().lock();
        try {
            Node node = map.get(key);
            if (node == null) return -1;
            
            long elapsed = System.currentTimeMillis() - node.timestamp;
            return Math.max(0, ttlMillis - elapsed);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Refreshes TTL for key
     */
    public boolean refreshTTL(K key) {
        if (key == null) return false;
        
        lock.writeLock().lock();
        try {
            Node node = map.get(key);
            if (node != null && !node.isExpired()) {
                node.updateTimestamp();
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Gets cache statistics
     */
    public CacheStats getStats() {
        lock.readLock().lock();
        try {
            double hitRatio = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
            
            return new CacheStats() {
                @Override
                public long getHits() { return hits; }
                
                @Override
                public long getMisses() { return misses; }
                
                @Override
                public long getEvictions() { return evictions; }
                
                @Override
                public double getHitRatio() { return hitRatio; }
                
                @Override
                public int getCurrentSize() { return map.size(); }
                
                @Override
                public int getMaxSize() { return capacity; }
                
                @Override
                public LocalDateTime getCreatedAt() { return createdAt; }
                
                @Override
                public String toString() {
                    return String.format("LRUCacheStats{hits=%d, misses=%d, evictions=%d, " +
                                       "currentSize=%d, maxSize=%d, hitRatio=%.2f%%}",
                                       hits, misses, evictions, getCurrentSize(), capacity, hitRatio * 100);
                }
            };
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Interface for cache statistics
     */
    public interface CacheStats {
        long getHits();
        long getMisses();
        long getEvictions();
        double getHitRatio();
        int getCurrentSize();
        int getMaxSize();
        LocalDateTime getCreatedAt();
    }
    
    /**
     * Resets statistics
     */
    public void resetStats() {
        lock.writeLock().lock();
        try {
            hits = 0;
            misses = 0;
            evictions = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ==================== CONFIGURATION ====================
    
    /**
     * Gets maximum capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Gets TTL in milliseconds
     */
    public long getTTL() {
        return ttlMillis;
    }
    
    /**
     * Checks if TTL is enabled
     */
    public boolean isTTLEnabled() {
        return ttlMillis > 0;
    }
    
    @Override
    public String toString() {
        return String.format("LRUCache{capacity=%d, size=%d, ttl=%d, hits=%d, misses=%d}",
                           capacity, size(), ttlMillis, hits, misses);
    }
}