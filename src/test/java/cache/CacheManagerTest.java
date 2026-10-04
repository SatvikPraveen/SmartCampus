package cache;

import cache.CacheManager.CacheConfig;
import cache.CacheManager.CacheStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CacheManager is a process-wide singleton, so every test works on uniquely named caches,
 * removes them afterwards and asserts on deltas of the global counters.
 */
class CacheManagerTest {

    private final CacheManager manager = CacheManager.getInstance();
    private final List<String> created = new ArrayList<>();

    private String newCache(CacheConfig config) {
        String name = "test-" + UUID.randomUUID();
        manager.createCache(name, config);
        created.add(name);
        return name;
    }

    @AfterEach
    void removeCreatedCaches() {
        created.forEach(manager::removeCache);
    }

    private long globalLong(String key) {
        return ((Number) manager.getGlobalStats().get(key)).longValue();
    }

    @Test
    void isASingleton() {
        assertThat(CacheManager.getInstance()).isSameAs(manager);
    }

    @Test
    void configDefaultsAndFluentSetters() {
        CacheConfig d = CacheConfig.defaultConfig();
        assertThat(d.getMaxSize()).isEqualTo(1000);
        assertThat(d.getTtlMinutes()).isEqualTo(60);
        assertThat(d.getStrategy()).isEqualTo(CacheStrategy.LRU);
        assertThat(d.isStatisticsEnabled()).isTrue();
        assertThat(d.isAutoCleanupEnabled()).isTrue();
        CacheConfig c = CacheConfig.defaultConfig().maxSize(5).ttl(0).strategy(CacheStrategy.FIFO)
            .enableStatistics(false).autoCleanup(false);
        assertThat(c.getMaxSize()).isEqualTo(5);
        assertThat(c.getTtlMinutes()).isZero();
        assertThat(c.getStrategy()).isEqualTo(CacheStrategy.FIFO);
        assertThat(c.isStatisticsEnabled()).isFalse();
        assertThat(c.isAutoCleanupEnabled()).isFalse();
    }

    @Test
    void lifecycleOfACache() {
        String name = newCache(CacheConfig.defaultConfig());
        assertThat(manager.cacheExists(name)).isTrue();
        assertThat(manager.getCacheNames()).contains(name);
        assertThatThrownBy(() -> manager.createCache(name)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(name);

        manager.put(name, "k", "v");
        assertThat(manager.<String>get(name, "k")).isEqualTo("v");
        assertThat(manager.containsKey(name, "k")).isTrue();
        assertThat(manager.size(name)).isEqualTo(1);
        assertThat(manager.getKeys(name)).containsExactly("k");

        manager.remove(name, "k");
        assertThat(manager.containsKey(name, "k")).isFalse();
        manager.put(name, "a", 1);
        manager.clear(name);
        assertThat(manager.size(name)).isZero();

        manager.removeCache(name);
        assertThat(manager.cacheExists(name)).isFalse();
        assertThat(manager.size(name)).isZero();
        assertThat(manager.getKeys(name)).isEmpty();
        assertThat(manager.getCacheStats(name)).isNull();
    }

    @Test
    void unknownCachesAreHandled() {
        String missing = "missing-" + UUID.randomUUID();
        assertThat(manager.<Object>get(missing, "k")).isNull();
        assertThat(manager.containsKey(missing, "k")).isFalse();
        assertThatThrownBy(() -> manager.put(missing, "k", 1)).isInstanceOf(IllegalArgumentException.class);
        manager.remove(missing, "k");
        manager.clear(missing);
        manager.removeCache(missing);
    }

    @Test
    void overwritingAKeyDoesNotEvict() {
        String name = newCache(CacheConfig.defaultConfig().maxSize(2));
        manager.put(name, "a", 1);
        manager.put(name, "b", 2);
        manager.put(name, "a", 3);
        assertThat(manager.getKeys(name)).containsExactlyInAnyOrder("a", "b");
        assertThat(manager.<Integer>get(name, "a")).isEqualTo(3);
        assertThat(manager.getCacheStats(name).getEvictions()).isZero();
    }

    @Test
    void lruEvictsLeastRecentlyAccessed() {
        String name = newCache(CacheConfig.defaultConfig().maxSize(3).strategy(CacheStrategy.LRU));
        manager.put(name, "a", 1);
        manager.put(name, "b", 2);
        manager.put(name, "c", 3);
        manager.get(name, "a");
        manager.put(name, "d", 4);
        assertThat(manager.getKeys(name)).containsExactlyInAnyOrder("a", "c", "d");
    }

    @Test
    void fifoIgnoresAccessWhenEvicting() {
        String name = newCache(CacheConfig.defaultConfig().maxSize(3).strategy(CacheStrategy.FIFO));
        manager.put(name, "a", 1);
        manager.put(name, "b", 2);
        manager.put(name, "c", 3);
        manager.get(name, "a");
        manager.put(name, "d", 4);
        assertThat(manager.getKeys(name)).containsExactlyInAnyOrder("b", "c", "d");
    }

    @Test
    void lifoEvictsNewestEntry() {
        String name = newCache(CacheConfig.defaultConfig().maxSize(3).strategy(CacheStrategy.LIFO));
        manager.put(name, "a", 1);
        manager.put(name, "b", 2);
        manager.put(name, "c", 3);
        manager.put(name, "d", 4);
        assertThat(manager.getKeys(name)).containsExactlyInAnyOrder("a", "b", "d");
    }

    @Test
    void sizeNeverExceedsMaxSize() {
        String name = newCache(CacheConfig.defaultConfig().maxSize(10));
        for (int i = 0; i < 100; i++) {
            manager.put(name, "k" + i, i);
            assertThat(manager.size(name)).isLessThanOrEqualTo(10);
        }
        assertThat(manager.getCacheStats(name).getEvictions()).isEqualTo(90);
    }

    @Test
    void perCacheStatistics() {
        String name = newCache(CacheConfig.defaultConfig().maxSize(1));
        manager.put(name, "a", 1);
        manager.get(name, "a");
        manager.get(name, "a");
        manager.get(name, "nope");
        manager.put(name, "b", 2);
        CacheStats stats = manager.getCacheStats(name);
        assertThat(stats.getName()).isEqualTo(name);
        assertThat(stats.getHits()).isEqualTo(2);
        assertThat(stats.getMisses()).isEqualTo(1);
        assertThat(stats.getEvictions()).isEqualTo(1);
        assertThat(stats.getCurrentSize()).isEqualTo(1);
        assertThat(stats.getMaxSize()).isEqualTo(1);
        assertThat(stats.getHitRatio()).isEqualTo(2.0 / 3);
        assertThat(stats.getCreatedAt()).isNotNull();
        assertThat(stats.toString()).contains("hits=2", "misses=1", "evictions=1");
        assertThat(manager.getAllCacheStats()).extracting(CacheStats::getName).contains(name);
    }

    @Test
    void globalHitAndMissCountersTrackLookups() {
        String name = newCache(CacheConfig.defaultConfig());
        long hits = globalLong("totalHits");
        long misses = globalLong("totalMisses");
        long entries = globalLong("totalEntries");
        manager.put(name, "a", 1);
        manager.get(name, "a");
        manager.get(name, "b");
        manager.get("missing-" + UUID.randomUUID(), "a");
        Map<String, Object> global = manager.getGlobalStats();
        assertThat(((Number) global.get("totalHits")).longValue()).isEqualTo(hits + 1);
        assertThat(((Number) global.get("totalMisses")).longValue()).isEqualTo(misses + 2);
        assertThat(((Number) global.get("totalEntries")).longValue()).isEqualTo(entries + 1);
        assertThat((double) global.get("globalHitRatio")).isBetween(0.0, 1.0);
        assertThat((int) global.get("totalCaches")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void globalEvictionsCountEachEvictionExactlyOnce() {
        long before = globalLong("totalEvictions");
        String name = newCache(CacheConfig.defaultConfig().maxSize(1));
        manager.put(name, "a", 1);
        manager.put(name, "b", 2);
        manager.performCleanup();
        manager.performCleanup();
        manager.performCleanup();
        assertThat(globalLong("totalEvictions")).isEqualTo(before + 1);
    }

    @Test
    void cleanupKeepsUnexpiredEntries() {
        String name = newCache(CacheConfig.defaultConfig().ttl(60));
        String eternal = newCache(CacheConfig.defaultConfig().ttl(0));
        manager.put(name, "a", 1);
        manager.put(eternal, "a", 1);
        manager.performCleanup();
        assertThat(manager.containsKey(name, "a")).isTrue();
        assertThat(manager.containsKey(eternal, "a")).isTrue();
    }

    @Test
    void memoryEstimateGrowsWithEntries() {
        String name = newCache(CacheConfig.defaultConfig());
        long before = manager.getEstimatedMemoryUsage();
        manager.put(name, "a", 1);
        manager.put(name, "b", 2);
        assertThat(manager.getEstimatedMemoryUsage()).isEqualTo(before + 200);
    }

    @Test
    void presetsCreateConfiguredCaches() {
        List<String> presets = List.of("students", "courses", "professors", "enrollments", "sessions");
        assumeTrue(presets.stream().noneMatch(manager::cacheExists), "preset caches already created elsewhere");
        try {
            manager.createStudentCache();
            manager.createCourseCache();
            manager.createProfessorCache();
            manager.createEnrollmentCache();
            manager.createSessionCache();
            assertThat(manager.getCacheStats("students").getMaxSize()).isEqualTo(2000);
            assertThat(manager.getCacheStats("courses").getMaxSize()).isEqualTo(1000);
            assertThat(manager.getCacheStats("professors").getMaxSize()).isEqualTo(500);
            assertThat(manager.getCacheStats("enrollments").getMaxSize()).isEqualTo(5000);
            assertThat(manager.getCacheStats("sessions").getMaxSize()).isEqualTo(1000);
            assertThatThrownBy(manager::createStudentCache).isInstanceOf(IllegalArgumentException.class);
        } finally {
            presets.forEach(manager::removeCache);
        }
    }
}
