package cache;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class LRUCacheTest {

    private static final long TTL = 60_000;

    /**
     * Moves an entry's internal timestamp into the past so that TTL behaviour can be
     * tested without waiting on the wall clock.
     */
    private static void backdate(LRUCache<?, ?> cache, Object key, long millis) throws Exception {
        Field mapField = LRUCache.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object node = ((Map<?, ?>) mapField.get(cache)).get(key);
        Field ts = node.getClass().getDeclaredField("timestamp");
        ts.setAccessible(true);
        ts.setLong(node, ts.getLong(node) - millis);
    }

    private static LRUCache<String, Integer> abc(int capacity) {
        LRUCache<String, Integer> cache = new LRUCache<>(capacity);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        return cache;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveCapacity(int capacity) {
        assertThatThrownBy(() -> new LRUCache<String, String>(capacity)).isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    class Basics {
        @Test
        void putGetAndOverwrite() {
            LRUCache<String, Integer> cache = new LRUCache<>(3);
            assertThat(cache.put("a", 1)).isNull();
            assertThat(cache.put("a", 2)).isEqualTo(1);
            assertThat(cache.get("a")).isEqualTo(2);
            assertThat(cache.size()).isEqualTo(1);
            assertThat(cache.get("zzz")).isNull();
        }

        @Test
        void nullKeysAreIgnored() {
            LRUCache<String, Integer> cache = new LRUCache<>(3);
            assertThat(cache.put(null, 1)).isNull();
            assertThat(cache.get(null)).isNull();
            assertThat(cache.containsKey(null)).isFalse();
            assertThat(cache.remove(null)).isNull();
            assertThat(cache.isEmpty()).isTrue();
        }

        @Test
        void removeAndClear() {
            LRUCache<String, Integer> cache = abc(5);
            assertThat(cache.remove("b")).isEqualTo(2);
            assertThat(cache.remove("b")).isNull();
            assertThat(cache.getKeysInAccessOrder()).containsExactly("c", "a");
            cache.clear();
            assertThat(cache.isEmpty()).isTrue();
            assertThat(cache.getKeysInAccessOrder()).isEmpty();
            cache.put("x", 1);
            assertThat(cache.getKeysInAccessOrder()).containsExactly("x");
        }

        @Test
        void configurationAccessors() {
            LRUCache<String, Integer> cache = new LRUCache<>(7, TTL);
            assertThat(cache.getCapacity()).isEqualTo(7);
            assertThat(cache.getTTL()).isEqualTo(TTL);
            assertThat(cache.isTTLEnabled()).isTrue();
            assertThat(new LRUCache<>(1).isTTLEnabled()).isFalse();
            assertThat(cache.toString()).contains("capacity=7", "size=0");
        }
    }

    @Nested
    class EvictionOrder {
        @Test
        void evictsLeastRecentlyUsedWhenFull() {
            LRUCache<String, Integer> cache = abc(3);
            cache.put("d", 4);
            assertThat(cache.keySet()).containsExactlyInAnyOrder("b", "c", "d");
        }

        @Test
        void getRefreshesRecency() {
            LRUCache<String, Integer> cache = abc(3);
            cache.get("a");
            cache.put("d", 4);
            assertThat(cache.containsKey("a")).isTrue();
            assertThat(cache.containsKey("b")).isFalse();
            assertThat(cache.getKeysInAccessOrder()).containsExactly("d", "a", "c");
        }

        @Test
        void putOnExistingKeyRefreshesRecencyWithoutEviction() {
            LRUCache<String, Integer> cache = abc(3);
            cache.put("a", 10);
            assertThat(cache.size()).isEqualTo(3);
            assertThat(cache.getStats().getEvictions()).isZero();
            cache.put("d", 4);
            assertThat(cache.keySet()).containsExactlyInAnyOrder("a", "c", "d");
        }

        @Test
        void containsKeyDoesNotChangeRecency() {
            LRUCache<String, Integer> cache = abc(3);
            assertThat(cache.containsKey("a")).isTrue();
            cache.put("d", 4);
            assertThat(cache.containsKey("a")).isFalse();
        }

        @Test
        void sizeNeverExceedsCapacity() {
            LRUCache<Integer, Integer> cache = new LRUCache<>(5);
            for (int i = 0; i < 100; i++) {
                cache.put(i, i);
                if (i % 3 == 0) {
                    cache.get(i / 2);
                }
                assertThat(cache.size()).isLessThanOrEqualTo(5);
            }
            assertThat(cache.getKeysInAccessOrder()).hasSize(5).first().isEqualTo(99);
        }
    }

    @Nested
    class Views {
        @Test
        void viewsReturnSnapshotsWithoutDeadlocking() {
            LRUCache<String, Integer> cache = abc(5);
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                assertThat(cache.keySet()).containsExactlyInAnyOrder("a", "b", "c");
                assertThat(cache.values()).containsExactlyInAnyOrder(1, 2, 3);
                assertThat(cache.entrySet()).containsExactlyInAnyOrder(entry("a", 1), entry("b", 2), entry("c", 3));
            });
        }

        @Test
        void viewsAreDetachedCopies() {
            LRUCache<String, Integer> cache = abc(5);
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                cache.keySet().clear();
                cache.values().clear();
            });
            assertThat(cache.size()).isEqualTo(3);
        }

        @Test
        void viewsDropExpiredEntries() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5, TTL);
            cache.put("old", 1);
            cache.put("new", 2);
            backdate(cache, "old", 2 * TTL);
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                assertThat(cache.keySet()).containsExactly("new");
                assertThat(cache.values()).containsExactly(2);
            });
            assertThat(cache.size()).isEqualTo(1);
        }
    }

    @Nested
    class ConditionalOperations {
        @Test
        void putIfAbsent() {
            LRUCache<String, Integer> cache = new LRUCache<>(3);
            assertThat(cache.putIfAbsent("a", 1)).isNull();
            assertThat(cache.putIfAbsent("a", 2)).isEqualTo(1);
            assertThat(cache.get("a")).isEqualTo(1);
            assertThat(cache.putIfAbsent(null, 1)).isNull();
        }

        @Test
        void replaceOnlyExistingKeys() {
            LRUCache<String, Integer> cache = new LRUCache<>(3);
            assertThat(cache.replace("a", 1)).isNull();
            assertThat(cache.containsKey("a")).isFalse();
            cache.put("a", 1);
            assertThat(cache.replace("a", 5)).isEqualTo(1);
            assertThat(cache.get("a")).isEqualTo(5);
        }

        @Test
        void compareAndReplace() {
            LRUCache<String, Integer> cache = new LRUCache<>(3);
            cache.put("a", 1);
            assertThat(cache.replace("a", 2, 3)).isFalse();
            assertThat(cache.replace("a", 1, 3)).isTrue();
            assertThat(cache.get("a")).isEqualTo(3);
            assertThat(cache.replace("missing", null, 3)).isFalse();
        }
    }

    @Nested
    class Expiration {
        @Test
        void expiredEntryIsAMissAndIsRemoved() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5, TTL);
            cache.put("a", 1);
            backdate(cache, "a", 2 * TTL);
            assertThat(cache.get("a")).isNull();
            assertThat(cache.size()).isZero();
            LRUCache.CacheStats stats = cache.getStats();
            assertThat(stats.getMisses()).isEqualTo(1);
            assertThat(stats.getEvictions()).isEqualTo(1);
        }

        @Test
        void containsKeyRemovesExpiredEntry() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5, TTL);
            cache.put("a", 1);
            backdate(cache, "a", 2 * TTL);
            assertThat(cache.containsKey("a")).isFalse();
            assertThat(cache.size()).isZero();
            assertThat(cache.getKeysInAccessOrder()).isEmpty();
        }

        @Test
        void cleanExpiredRemovesOnlyExpiredEntries() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5, TTL);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            backdate(cache, "a", 2 * TTL);
            backdate(cache, "c", 2 * TTL);
            assertThat(cache.getKeysInAccessOrder()).containsExactly("b");
            assertThat(cache.cleanExpired()).isEqualTo(2);
            assertThat(cache.keySet()).containsExactly("b");
        }

        @Test
        void expiredEntriesCanBeReplacedByPutIfAbsent() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5, TTL);
            cache.put("a", 1);
            backdate(cache, "a", 2 * TTL);
            assertThat(cache.replace("a", 9)).isNull();
            assertThat(cache.putIfAbsent("a", 2)).isNull();
            assertThat(cache.get("a")).isEqualTo(2);
        }

        @Test
        void remainingTtlAndRefresh() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5, TTL);
            cache.put("a", 1);
            backdate(cache, "a", TTL / 2);
            assertThat(cache.getRemainingTTL("a")).isBetween(0L, TTL / 2);
            assertThat(cache.refreshTTL("a")).isTrue();
            assertThat(cache.getRemainingTTL("a")).isGreaterThan(TTL / 2);
            backdate(cache, "a", 2 * TTL);
            assertThat(cache.getRemainingTTL("a")).isZero();
            assertThat(cache.refreshTTL("a")).isFalse();
            assertThat(cache.getRemainingTTL("missing")).isEqualTo(-1);
            assertThat(new LRUCache<String, Integer>(1).getRemainingTTL("a")).isEqualTo(-1);
        }

        @Test
        void entriesNeverExpireWithoutTtl() throws Exception {
            LRUCache<String, Integer> cache = new LRUCache<>(5);
            cache.put("a", 1);
            backdate(cache, "a", 365L * 24 * 3600 * 1000);
            assertThat(cache.get("a")).isEqualTo(1);
            assertThat(cache.cleanExpired()).isZero();
        }
    }

    @Nested
    class Statistics {
        @Test
        void countsHitsMissesAndEvictions() {
            LRUCache<String, Integer> cache = abc(2); // "a" evicted
            cache.get("b");
            cache.get("c");
            cache.get("a");
            LRUCache.CacheStats stats = cache.getStats();
            assertThat(stats.getHits()).isEqualTo(2);
            assertThat(stats.getMisses()).isEqualTo(1);
            assertThat(stats.getEvictions()).isEqualTo(1);
            assertThat(stats.getHitRatio()).isCloseTo(2.0 / 3, within(1e-9));
            assertThat(stats.getCurrentSize()).isEqualTo(2);
            assertThat(stats.getMaxSize()).isEqualTo(2);
            assertThat(stats.getCreatedAt()).isNotNull();
            assertThat(stats.toString()).contains("hits=2", "misses=1", "evictions=1");
        }

        @Test
        void statsAreAConsistentSnapshot() {
            LRUCache<String, Integer> cache = new LRUCache<>(2);
            cache.put("a", 1);
            cache.get("a");
            LRUCache.CacheStats stats = cache.getStats();
            cache.get("a");
            cache.get("zzz");
            cache.put("b", 2);
            assertThat(stats.getHits()).isEqualTo(1);
            assertThat(stats.getMisses()).isZero();
            assertThat(stats.getCurrentSize()).isEqualTo(1);
            assertThat(stats.getHitRatio()).isEqualTo(1.0);
        }

        @Test
        void resetStats() {
            LRUCache<String, Integer> cache = abc(2);
            cache.get("a");
            cache.resetStats();
            LRUCache.CacheStats stats = cache.getStats();
            assertThat(stats.getHits()).isZero();
            assertThat(stats.getMisses()).isZero();
            assertThat(stats.getEvictions()).isZero();
            assertThat(stats.getHitRatio()).isZero();
            assertThat(stats.getCurrentSize()).isEqualTo(2);
        }
    }

    @Test
    void concurrentAccessKeepsInvariants() throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<>(50);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                final int offset = t * 1000;
                tasks.add(() -> {
                    for (int i = 0; i < 2000; i++) {
                        int key = offset + (i % 200);
                        cache.put(key, key);
                        Integer v = cache.get(key);
                        if (v != null && v != key) {
                            throw new AssertionError("wrong value for " + key);
                        }
                        if (i % 100 == 0) {
                            cache.keySet();
                        }
                    }
                    return null;
                });
            }
            List<Future<Void>> futures = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> pool.invokeAll(tasks));
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(cache.size()).isEqualTo(50);
        assertThat(cache.getKeysInAccessOrder()).hasSize(50).doesNotHaveDuplicates();
        assertThat(cache.keySet()).hasSize(50);
    }
}
