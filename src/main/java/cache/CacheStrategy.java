// File location: src/main/java/cache/CacheStrategy.java
package cache;

/**
 * Enumeration defining various cache eviction strategies
 * Used to determine which entries to remove when cache reaches capacity
 */
public enum CacheStrategy {
    
    /**
     * Least Recently Used (LRU) - Evicts the entry that has been used least recently
     * Good for temporal locality, where recently accessed items are more likely to be accessed again
     */
    LRU("Least Recently Used") {
        @Override
        public String getDescription() {
            return "Evicts entries that have been accessed least recently. " +
                   "Assumes recently used items are more likely to be used again.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return true;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return true;
        }
        
        @Override
        public int getComplexity() {
            return 2; // O(1) with proper implementation
        }
    },
    
    /**
     * First In, First Out (FIFO) - Evicts entries in the order they were added
     * Simple strategy that doesn't consider access patterns
     */
    FIFO("First In, First Out") {
        @Override
        public String getDescription() {
            return "Evicts entries in the order they were added (oldest first). " +
                   "Simple strategy that ignores access patterns.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return true;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return false;
        }
        
        @Override
        public int getComplexity() {
            return 1; // O(1)
        }
    },
    
    /**
     * Last In, First Out (LIFO) - Evicts the most recently added entry
     * Acts like a stack, removing the newest entries first
     */
    LIFO("Last In, First Out") {
        @Override
        public String getDescription() {
            return "Evicts the most recently added entries first (newest first). " +
                   "Acts like a stack structure.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return true;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return false;
        }
        
        @Override
        public int getComplexity() {
            return 1; // O(1)
        }
    },
    
    /**
     * Most Recently Used (MRU) - Evicts the entry that was accessed most recently
     * Assumes recently accessed items are less likely to be accessed again
     */
    MRU("Most Recently Used") {
        @Override
        public String getDescription() {
            return "Evicts entries that have been accessed most recently. " +
                   "Assumes recently used items are less likely to be used again.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return true;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return true;
        }
        
        @Override
        public int getComplexity() {
            return 2; // O(1) with proper implementation
        }
    },
    
    /**
     * Least Frequently Used (LFU) - Evicts the entry with the lowest access count
     * Good when some items are accessed much more frequently than others
     */
    LFU("Least Frequently Used") {
        @Override
        public String getDescription() {
            return "Evicts entries with the lowest access frequency. " +
                   "Good when access patterns are predictable and some items are much more popular.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return false;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return true;
        }
        
        @Override
        public int getComplexity() {
            return 3; // O(log n) typically
        }
    },
    
    /**
     * Most Frequently Used (MFU) - Evicts the entry with the highest access count
     * Assumes frequently accessed items will continue to be accessed
     */
    MFU("Most Frequently Used") {
        @Override
        public String getDescription() {
            return "Evicts entries with the highest access frequency. " +
                   "Assumes frequently used items will remain popular.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return false;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return true;
        }
        
        @Override
        public int getComplexity() {
            return 3; // O(log n) typically
        }
    },
    
    /**
     * Random Replacement (RR) - Evicts a randomly selected entry
     * Simple strategy with no bias, useful when access patterns are unpredictable
     */
    RANDOM("Random Replacement") {
        @Override
        public String getDescription() {
            return "Evicts randomly selected entries. " +
                   "Simple strategy with no assumptions about access patterns.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return false;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return false;
        }
        
        @Override
        public int getComplexity() {
            return 1; // O(1)
        }
    },
    
    /**
     * Time-To-Live (TTL) - Evicts entries based on expiration time
     * Removes expired entries first, then falls back to another strategy
     */
    TTL_BASED("Time-To-Live Based") {
        @Override
        public String getDescription() {
            return "Evicts entries based on their expiration time. " +
                   "Removes expired entries first, then uses fallback strategy.";
        }
        
        @Override
        public boolean isTimeBasedEviction() {
            return true;
        }
        
        @Override
        public boolean requiresAccessTracking() {
            return false;
        }
        
        @Override
        public int getComplexity() {
            return 2; // O(1) to O(n) depending on implementation
        }
    };
    
    private final String displayName;
    
    CacheStrategy(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the display name of the strategy
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets detailed description of the strategy
     */
    public abstract String getDescription();
    
    /**
     * Indicates if this strategy considers time-based factors
     */
    public abstract boolean isTimeBasedEviction();
    
    /**
     * Indicates if this strategy requires tracking access patterns
     */
    public abstract boolean requiresAccessTracking();
    
    /**
     * Gets complexity rating (1=simple, 2=moderate, 3=complex)
     */
    public abstract int getComplexity();
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if strategy is suitable for high-frequency access patterns
     */
    public boolean isSuitableForHighFrequency() {
        return switch (this) {
            case LRU, FIFO, RANDOM -> true;
            case LFU, MFU, TTL_BASED -> false;
            case LIFO, MRU -> true; // Depends on use case
        };
    }
    
    /**
     * Checks if strategy is suitable for predictable access patterns
     */
    public boolean isSuitableForPredictableAccess() {
        return switch (this) {
            case LRU, LFU -> true;
            case MRU, MFU -> false; // Depends on specific pattern
            case FIFO, LIFO, RANDOM, TTL_BASED -> false;
        };
    }
    
    /**
     * Gets recommended use cases for this strategy
     */
    public String getRecommendedUseCases() {
        return switch (this) {
            case LRU -> "General purpose caching, web browsers, virtual memory systems, " +
                       "database buffer pools where recently accessed data is likely to be accessed again.";
                       
            case FIFO -> "Simple scenarios where order of insertion matters, streaming data, " +
                        "log processing, scenarios where fairness is important.";
                        
            case LIFO -> "Stack-like operations, undo operations, recursive algorithms, " +
                        "temporary data that becomes obsolete quickly.";
                        
            case MRU -> "Scenarios where recently accessed items become less likely to be accessed, " +
                       "cyclic patterns, some database applications.";
                       
            case LFU -> "Scenarios with stable access patterns, web caching with consistent popularity, " +
                       "database systems with known query patterns.";
                       
            case MFU -> "Rare use cases where popular items should be evicted, " +
                       "load balancing scenarios, some specialized algorithms.";
                       
            case RANDOM -> "When access patterns are unpredictable, testing environments, " +
                          "scenarios requiring fairness without bias.";
                          
            case TTL_BASED -> "Time-sensitive data, session management, API response caching, " +
                             "data with natural expiration times.";
        };
    }
    
    /**
     * Gets performance characteristics
     */
    public String getPerformanceCharacteristics() {
        return switch (this) {
            case LRU -> "O(1) access and update with proper data structures (HashMap + DoublyLinkedList). " +
                       "Good cache hit rates for temporal locality.";
                       
            case FIFO -> "O(1) operations, minimal memory overhead. " +
                        "Predictable behavior but may not reflect access patterns.";
                        
            case LIFO -> "O(1) operations, stack-like behavior. " +
                        "May have poor hit rates for typical caching scenarios.";
                        
            case MRU -> "O(1) access and update. " +
                       "Opposite behavior to LRU, useful in specific scenarios.";
                       
            case LFU -> "O(log n) operations typically, requires frequency tracking. " +
                       "Good hit rates when access frequencies are stable.";
                       
            case MFU -> "O(log n) operations, complex frequency management. " +
                       "Rarely used in practice due to counterintuitive behavior.";
                       
            case RANDOM -> "O(1) operations, no metadata required. " +
                          "Unpredictable hit rates, good for testing assumptions.";
                          
            case TTL_BASED -> "O(1) to O(n) depending on cleanup strategy. " +
                             "Excellent for time-sensitive data, requires time tracking.";
        };
    }
    
    /**
     * Gets memory overhead characteristics
     */
    public int getMemoryOverhead() {
        return switch (this) {
            case FIFO, LIFO, RANDOM -> 1; // Minimal overhead
            case LRU, MRU, TTL_BASED -> 2; // Moderate overhead
            case LFU, MFU -> 3; // Higher overhead due to frequency tracking
        };
    }
    
    /**
     * Checks if two strategies are compatible for combination
     */
    public boolean isCompatibleWith(CacheStrategy other) {
        // TTL can be combined with most strategies
        if (this == TTL_BASED || other == TTL_BASED) {
            return true;
        }
        
        // Frequency-based strategies can be combined with time-based
        if ((this == LFU || this == MFU) && (other.isTimeBasedEviction())) {
            return true;
        }
        
        if ((other == LFU || other == MFU) && (this.isTimeBasedEviction())) {
            return true;
        }
        
        // Generally, strategies with different approaches can be combined
        return this.isTimeBasedEviction() != other.isTimeBasedEviction();
    }
    
    /**
     * Gets strategy by name (case-insensitive)
     */
    public static CacheStrategy fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return LRU; // Default strategy
        }
        
        String normalized = name.trim().toUpperCase().replace("-", "_");
        
        return switch (normalized) {
            case "LRU", "LEAST_RECENTLY_USED" -> LRU;
            case "FIFO", "FIRST_IN_FIRST_OUT" -> FIFO;
            case "LIFO", "LAST_IN_FIRST_OUT" -> LIFO;
            case "MRU", "MOST_RECENTLY_USED" -> MRU;
            case "LFU", "LEAST_FREQUENTLY_USED" -> LFU;
            case "MFU", "MOST_FREQUENTLY_USED" -> MFU;
            case "RANDOM", "RR", "RANDOM_REPLACEMENT" -> RANDOM;
            case "TTL", "TTL_BASED", "TIME_TO_LIVE" -> TTL_BASED;
            default -> throw new IllegalArgumentException("Unknown cache strategy: " + name);
        };
    }
    
    /**
     * Gets all available strategies with descriptions
     */
    public static String getAllStrategiesDescription() {
        StringBuilder sb = new StringBuilder("Available Cache Strategies:\n");
        for (CacheStrategy strategy : values()) {
            sb.append(String.format("- %s (%s): %s\n", 
                     strategy.name(), strategy.displayName, strategy.getDescription()));
        }
        return sb.toString();
    }
    
    /**
     * Gets recommended strategy for given requirements
     */
    public static CacheStrategy recommend(boolean needsHighPerformance, 
                                        boolean hasTimeConstraints,
                                        boolean hasPredictableAccess) {
        if (hasTimeConstraints) {
            return TTL_BASED;
        }
        
        if (needsHighPerformance) {
            if (hasPredictableAccess) {
                return LRU;
            } else {
                return RANDOM;
            }
        }
        
        if (hasPredictableAccess) {
            return LFU;
        }
        
        return LRU; // Default recommendation
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", name(), displayName);
    }
}