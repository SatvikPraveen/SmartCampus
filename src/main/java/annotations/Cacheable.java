// File location: src/main/java/annotations/Cacheable.java
package annotations;

import java.lang.annotation.*;

/**
 * Annotation to mark methods or classes for caching
 * Enables automatic caching of method results or class instances
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    
    /**
     * Cache strategy enumeration
     */
    enum Strategy {
        LRU,        // Least Recently Used
        FIFO,       // First In First Out
        LIFO,       // Last In First Out
        TTL_BASED,  // Time To Live based
        CUSTOM      // Custom strategy
    }
    
    /**
     * Cache eviction policy
     */
    enum EvictionPolicy {
        SIZE_BASED,     // Evict when size limit reached
        TIME_BASED,     // Evict when TTL expires
        MEMORY_BASED,   // Evict when memory threshold reached
        HYBRID          // Combination of policies
    }
    
    /**
     * Name of the cache to use
     * @return cache name, defaults to method or class name
     */
    String cacheName() default "";
    
    /**
     * Cache key expression
     * @return SpEL expression for cache key generation
     */
    String key() default "";
    
    /**
     * Condition for caching
     * @return SpEL expression that must evaluate to true for caching
     */
    String condition() default "";
    
    /**
     * Unless condition - cache unless this condition is true
     * @return SpEL expression that prevents caching if true
     */
    String unless() default "";
    
    /**
     * Time to live in minutes
     * @return TTL in minutes, 0 means no expiration
     */
    int ttl() default 60;
    
    /**
     * Maximum number of entries in cache
     * @return max entries, 0 means unlimited
     */
    int maxEntries() default 1000;
    
    /**
     * Cache strategy to use
     * @return caching strategy
     */
    Strategy strategy() default Strategy.LRU;
    
    /**
     * Eviction policy
     * @return eviction policy
     */
    EvictionPolicy evictionPolicy() default EvictionPolicy.SIZE_BASED;
    
    /**
     * Whether to cache null values
     * @return true to cache null values
     */
    boolean cacheNullValues() default false;
    
    /**
     * Whether to enable cache statistics
     * @return true to enable statistics
     */
    boolean enableStats() default true;
    
    /**
     * Cache refresh threshold as percentage (0-100)
     * @return refresh threshold percentage
     */
    int refreshThreshold() default 80;
    
    /**
     * Whether to refresh cache asynchronously
     * @return true for async refresh
     */
    boolean asyncRefresh() default false;
    
    /**
     * Tags for cache management and bulk operations
     * @return cache tags
     */
    String[] tags() default {};
    
    /**
     * Cache namespace for logical grouping
     * @return namespace name
     */
    String namespace() default "default";
    
    /**
     * Whether the cache is distributed across multiple nodes
     * @return true for distributed cache
     */
    boolean distributed() default false;
    
    /**
     * Serialization format for distributed caches
     * @return serialization format
     */
    String serialization() default "JSON";
    
    /**
     * Cache warmup strategy
     * @return warmup strategy name
     */
    String warmupStrategy() default "";
    
    /**
     * Custom cache key generator class
     * @return key generator class
     */
    Class<?> keyGenerator() default Void.class;
    
    /**
     * Custom cache resolver class
     * @return cache resolver class
     */
    Class<?> cacheResolver() default Void.class;
    
    /**
     * Priority for cache operations (higher number = higher priority)
     * @return priority level
     */
    int priority() default 0;
    
    /**
     * Whether to compress cached values
     * @return true to enable compression
     */
    boolean compress() default false;
    
    /**
     * Whether to encrypt cached values
     * @return true to enable encryption
     */
    boolean encrypt() default false;
}