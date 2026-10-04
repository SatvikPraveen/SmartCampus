package utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import utils.CacheUtil.CacheEntry;
import utils.CacheUtil.CacheStats;
import utils.CacheUtil.LRUCache;
import utils.CacheUtil.SimpleCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Expiry is exercised with negative TTLs (already expired at insertion) and long TTLs
 * (never expire during a test run), so no test depends on wall-clock timing.
 */
class CacheUtilTest {

    private static final Duration EXPIRED = Duration.ofSeconds(-1);
    private static final Duration LONG = Duration.ofHours(1);

    @Nested
    class Entry {
        @Test
        void tracksAccessAndExpiry() {
            CacheEntry<String> e = new CacheEntry<>("v", LONG);
            assertThat(e.getAccessCount()).isZero();
            assertThat(e.getValue()).isEqualTo("v");
            assertThat(e.getValue()).isEqualTo("v");
            assertThat(e.getAccessCount()).isEqualTo(2);
            assertThat(e.isExpired()).isFalse();
            assertThat(e.isExpired(e.getExpiresAt().plusNanos(1))).isTrue();
            assertThat(e.isExpired(e.getExpiresAt())).isFalse();
            assertThat(e.getAge().isNegative()).isFalse();
            assertThat(e.getTimeSinceLastAccess().isNegative()).isFalse();
        }

        @Test
        void nullTtlNeverExpires() {
            CacheEntry<String> e = new CacheEntry<>("v", null);
            assertThat(e.getExpiresAt()).isNull();
            assertThat(e.isExpired(LocalDateTime.MAX)).isFalse();
        }

        @Test
        void metadataIsCopiedOnRead() {
            CacheEntry<String> e = new CacheEntry<>("v", LONG);
            e.setMetadata("source", "db");
            assertThat(e.getMetadata("source")).isEqualTo("db");
            e.getMetadata().clear();
            assertThat(e.getMetadata()).containsEntry("source", "db");
        }
    }

    @Nested
    class Simple {
        @Test
        void putGetRemove() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(10, LONG);
            cache.put("a", 1);
            assertThat(cache.get("a")).isEqualTo(1);
            assertThat(cache.containsKey("a")).isTrue();
            assertThat(cache.get("missing")).isNull();
            assertThat(cache.remove("a")).isEqualTo(1);
            assertThat(cache.remove("a")).isNull();
            assertThat(cache.isEmpty()).isTrue();
        }

        @Test
        void nullKeysAndValuesAreIgnored() {
            SimpleCache<String, Integer> cache = new SimpleCache<>();
            cache.put(null, 1);
            cache.put("a", null);
            assertThat(cache.size()).isZero();
        }

        @Test
        void expiredEntriesAreInvisible() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(10, LONG);
            cache.put("old", 1, EXPIRED);
            cache.put("fresh", 2);
            assertThat(cache.get("old")).isNull();
            assertThat(cache.containsKey("old")).isFalse();
            assertThat(cache.keySet()).containsExactly("fresh");
            assertThat(cache.size()).isEqualTo(1);
        }

        @Test
        void sizeNeverExceedsMaxSize() {
            SimpleCache<Integer, Integer> cache = new SimpleCache<>(3, LONG);
            for (int i = 0; i < 10; i++) {
                cache.put(i, i);
                assertThat(cache.size()).isLessThanOrEqualTo(3);
            }
            assertThat(cache.size()).isEqualTo(3);
            assertThat(cache.containsKey(9)).isTrue();
        }

        @Test
        void updatingExistingKeyAtCapacityDoesNotEvictOtherEntries() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(2, LONG);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("b", 3);
            assertThat(cache.keySet()).containsExactlyInAnyOrder("a", "b");
            cache.put("a", 4);
            assertThat(cache.keySet()).containsExactlyInAnyOrder("a", "b");
            assertThat(cache.get("a")).isEqualTo(4);
            assertThat(cache.get("b")).isEqualTo(3);
        }

        @Test
        void loaderIsOnlyInvokedOnMiss() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(10, LONG);
            AtomicInteger calls = new AtomicInteger();
            Function<String, Integer> loader = k -> { calls.incrementAndGet(); return k.length(); };
            assertThat(cache.get("abc", loader)).isEqualTo(3);
            assertThat(cache.get("abc", loader)).isEqualTo(3);
            assertThat(calls).hasValue(1);
            assertThat(cache.get("none", k -> null)).isNull();
            assertThat(cache.containsKey("none")).isFalse();
        }

        @Test
        void statsReportHitRateAsFractionOfLookups() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(4, LONG);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.get("a");
            cache.get("a");
            cache.get("a");
            cache.get("zzz");
            CacheStats stats = cache.getStats();
            assertThat(stats.getSize()).isEqualTo(2);
            assertThat(stats.getMaxSize()).isEqualTo(4);
            assertThat(stats.getTotalAccesses()).isEqualTo(3);
            assertThat(stats.getHitRate()).isCloseTo(0.75, within(1e-9));
            assertThat(stats.getUtilization()).isCloseTo(0.5, within(1e-9));
            assertThat(stats.getExpiredCount()).isZero();
            assertThat(stats.toString()).contains("hitRate=75.00%", "utilization=50.00%");
        }

        @Test
        void hitRateNeverExceedsOne() {
            SimpleCache<Integer, Integer> cache = new SimpleCache<>(100, LONG);
            for (int i = 0; i < 10; i++) {
                cache.put(i, i);
            }
            cache.get(0);
            assertThat(cache.getStats().getHitRate()).isBetween(0.0, 1.0);
            assertThat(new SimpleCache<>().getStats().getHitRate()).isZero();
        }

        @Test
        void builder() {
            SimpleCache<String, String> cache = CacheUtil.<String, String>newBuilder()
                .maxSize(1).defaultTtl(LONG).build();
            cache.put("a", "1");
            cache.put("b", "2");
            assertThat(cache.size()).isEqualTo(1);
            assertThat(cache.getStats().getMaxSize()).isEqualTo(1);
        }
    }

    @Nested
    class Lru {
        @Test
        void evictsLeastRecentlyAccessed() {
            LRUCache<String, Integer> cache = new LRUCache<>(2);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.get("a");
            cache.put("c", 3);
            assertThat(cache.keySet()).containsExactlyInAnyOrder("a", "c");
        }

        @Test
        void expiredEntriesAreRemovedOnAccess() {
            LRUCache<String, Integer> cache = new LRUCache<>(5, LONG);
            cache.put("x", 1, EXPIRED);
            cache.put("y", 2);
            assertThat(cache.get("x")).isNull();
            assertThat(cache.get("y")).isEqualTo(2);
            assertThat(cache).doesNotContainKey("x");
        }

        @Test
        void overwritingWithoutTtlClearsPreviousExpiry() {
            LRUCache<String, Integer> cache = new LRUCache<>(5);
            cache.put("k", 1, EXPIRED);
            cache.put("k", 2);
            assertThat(cache.get("k")).isEqualTo(2);
            cache.put("j", 1, EXPIRED);
            cache.put("j", 3, null);
            assertThat(cache.get("j")).isEqualTo(3);
        }

        @Test
        void removeAndClear() {
            LRUCache<String, Integer> cache = new LRUCache<>(5, LONG);
            cache.put("a", 1);
            cache.put("b", 2);
            assertThat(cache.remove("a")).isEqualTo(1);
            cache.clear();
            assertThat(cache).isEmpty();
        }
    }

    @Nested
    class Memoization {
        @Test
        void memoizedFunctionComputesOncePerInput() {
            AtomicInteger calls = new AtomicInteger();
            Function<Integer, Integer> square = CacheUtil.memoize((Integer x) -> { calls.incrementAndGet(); return x * x; });
            assertThat(square.apply(4)).isEqualTo(16);
            assertThat(square.apply(4)).isEqualTo(16);
            assertThat(square.apply(5)).isEqualTo(25);
            assertThat(calls).hasValue(2);
        }

        @Test
        void memoizedSupplierComputesOnce() {
            AtomicInteger calls = new AtomicInteger();
            Supplier<String> s = CacheUtil.memoize(() -> "v" + calls.incrementAndGet());
            assertThat(s.get()).isEqualTo("v1");
            assertThat(s.get()).isEqualTo("v1");
        }

        @Test
        void expiredSupplierRecomputes() {
            AtomicInteger calls = new AtomicInteger();
            Supplier<Integer> s = CacheUtil.memoize(calls::incrementAndGet, EXPIRED);
            assertThat(s.get()).isEqualTo(1);
            assertThat(s.get()).isEqualTo(2);
        }
    }

    @Nested
    class Keys {
        @Test
        void generateKey() {
            assertThat(CacheUtil.generateKey("student", 42, null)).isEqualTo("student:42:null");
            assertThat(CacheUtil.generateKey()).isEqualTo("empty");
            assertThat(CacheUtil.generateKey((Object[]) null)).isEqualTo("empty");
        }

        @Test
        void normalizeKey() {
            assertThat(CacheUtil.normalizeKey("  Student  Record ")).isEqualTo("student_record");
            assertThat(CacheUtil.normalizeKey(null)).isEqualTo("null");
        }

        @Test
        void hashKeyIsDeterministicAndNonNegative() {
            assertThat(CacheUtil.generateHashKey("a", 1)).isEqualTo(CacheUtil.generateHashKey("a", 1))
                .startsWith("hash_").doesNotContain("-");
            assertThat(CacheUtil.generateHashKey("a", 1)).isNotEqualTo(CacheUtil.generateHashKey("a", 2));
        }
    }

    @Nested
    class BulkOperations {
        @Test
        void putAllGetAllRemoveAll() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(10, LONG);
            CacheUtil.putAll(cache, Map.of("a", 1, "b", 2, "c", 3));
            assertThat(CacheUtil.getAll(cache, List.of("a", "c", "zz"))).containsOnly(Map.entry("a", 1), Map.entry("c", 3));
            CacheUtil.removeAll(cache, List.of("a", "b"));
            assertThat(cache.keySet()).containsExactly("c");
            CacheUtil.putAll(cache, Map.of("d", 4), EXPIRED);
            assertThat(cache.get("d")).isNull();
            CacheUtil.putAll(null, Map.of());
            assertThat(CacheUtil.getAll(null, List.of("a"))).isEmpty();
        }

        @Test
        void refreshReloadsValue() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(10, LONG);
            cache.put("a", 1);
            assertThat(CacheUtil.refresh(cache, "a", k -> 99)).isEqualTo(99);
            assertThat(cache.get("a")).isEqualTo(99);
            assertThat(CacheUtil.refresh(cache, null, k -> 1)).isNull();
        }

        @Test
        void warmUpLoadsAllKeysAndSurvivesLoaderFailures() {
            SimpleCache<Integer, Integer> cache = new SimpleCache<>(100, LONG);
            CacheUtil.warmUp(cache, k -> {
                if (k == 3) throw new IllegalStateException("boom");
                return k * 10;
            }, List.of(1, 2, 3, 4));
            assertThat(cache.keySet()).containsExactlyInAnyOrder(1, 2, 4);
            assertThat(cache.get(4)).isEqualTo(40);
        }
    }

    @Nested
    class Inspection {
        @Test
        void inspectionAndMaintenance() {
            SimpleCache<String, Integer> cache = new SimpleCache<>(10, LONG);
            cache.put("a", 1);
            cache.put("b", 2);
            cache.get("a");
            cache.get("a");
            assertThat(CacheUtil.getAllEntries(cache)).containsOnlyKeys("a", "b");
            assertThat(CacheUtil.getKeysByAccessCount(cache, 2)).containsExactly("a");
            assertThat(CacheUtil.getKeysByAge(cache, LONG)).isEmpty();
            assertThat(CacheUtil.getKeysByAge(cache, Duration.ofHours(-1))).containsExactlyInAnyOrder("a", "b");

            cache.put("x", 0, EXPIRED);
            assertThat(CacheUtil.cleanExpired(cache)).isEqualTo(1);
            assertThat(CacheUtil.evictOlderThan(cache, LONG)).isZero();
            assertThat(CacheUtil.evictLRU(cache, 1)).isEqualTo(1);
            assertThat(cache.size()).isEqualTo(1);
            assertThat(CacheUtil.evictLRU(cache, 0)).isZero();
            assertThat(CacheUtil.evictOlderThan(cache, Duration.ofHours(-1))).isEqualTo(1);
            assertThat(cache.isEmpty()).isTrue();
        }

        @Test
        void nullCacheIsHandled() {
            assertThat(CacheUtil.getAllEntries(null)).isEmpty();
            assertThat(CacheUtil.cleanExpired(null)).isZero();
            assertThat(CacheUtil.evictLRU(null, 3)).isZero();
            assertThat(CacheUtil.getKeysByAccessCount(null, 0)).isEmpty();
        }
    }
}
