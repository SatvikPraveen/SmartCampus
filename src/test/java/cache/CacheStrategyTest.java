package cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheStrategyTest {

    @ParameterizedTest
    @EnumSource(CacheStrategy.class)
    void everyStrategyIsFullyDescribed(CacheStrategy s) {
        assertThat(s.getDisplayName()).isNotBlank();
        assertThat(s.getDescription()).isNotBlank();
        assertThat(s.getRecommendedUseCases()).isNotBlank();
        assertThat(s.getPerformanceCharacteristics()).isNotBlank();
        assertThat(s.getComplexity()).isBetween(1, 3);
        assertThat(s.getMemoryOverhead()).isBetween(1, 3);
        assertThat(s.toString()).isEqualTo(s.name() + " (" + s.getDisplayName() + ")");
    }

    @ParameterizedTest
    @EnumSource(CacheStrategy.class)
    void fromStringRoundTripsEnumNamesInAnyCase(CacheStrategy s) {
        assertThat(CacheStrategy.fromString(s.name())).isEqualTo(s);
        assertThat(CacheStrategy.fromString(" " + s.name().toLowerCase() + " ")).isEqualTo(s);
        assertThat(CacheStrategy.fromString(s.name().replace('_', '-'))).isEqualTo(s);
    }

    @ParameterizedTest
    @CsvSource({
        "least-recently-used, LRU", "first_in_first_out, FIFO", "Last-In-First-Out, LIFO",
        "most_recently_used, MRU", "LEAST_FREQUENTLY_USED, LFU", "most-frequently-used, MFU",
        "rr, RANDOM", "random-replacement, RANDOM", "ttl, TTL_BASED", "time-to-live, TTL_BASED"
    })
    void fromStringAcceptsAliases(String alias, CacheStrategy expected) {
        assertThat(CacheStrategy.fromString(alias)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void fromStringDefaultsToLru(String blank) {
        assertThat(CacheStrategy.fromString(blank)).isEqualTo(CacheStrategy.LRU);
    }

    @Test
    void fromStringRejectsUnknownNames() {
        assertThatThrownBy(() -> CacheStrategy.fromString("ARC"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ARC");
    }

    @Test
    void compatibilityIsSymmetric() {
        for (CacheStrategy a : CacheStrategy.values()) {
            for (CacheStrategy b : CacheStrategy.values()) {
                assertThat(a.isCompatibleWith(b)).as("%s vs %s", a, b).isEqualTo(b.isCompatibleWith(a));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CacheStrategy.class)
    void ttlCombinesWithEverything(CacheStrategy s) {
        assertThat(CacheStrategy.TTL_BASED.isCompatibleWith(s)).isTrue();
    }

    @Test
    void compatibilityExamples() {
        assertThat(CacheStrategy.LFU.isCompatibleWith(CacheStrategy.LRU)).isTrue();
        assertThat(CacheStrategy.LRU.isCompatibleWith(CacheStrategy.FIFO)).isFalse();
        assertThat(CacheStrategy.LFU.isCompatibleWith(CacheStrategy.RANDOM)).isFalse();
        assertThat(CacheStrategy.RANDOM.isCompatibleWith(CacheStrategy.MRU)).isTrue();
    }

    @Test
    void trackingAndTimeBasedClassification() {
        assertThat(CacheStrategy.LRU.requiresAccessTracking()).isTrue();
        assertThat(CacheStrategy.LFU.requiresAccessTracking()).isTrue();
        assertThat(CacheStrategy.FIFO.requiresAccessTracking()).isFalse();
        assertThat(CacheStrategy.RANDOM.requiresAccessTracking()).isFalse();
        assertThat(CacheStrategy.LFU.isTimeBasedEviction()).isFalse();
        assertThat(CacheStrategy.TTL_BASED.isTimeBasedEviction()).isTrue();
        assertThat(CacheStrategy.LRU.isSuitableForPredictableAccess()).isTrue();
        assertThat(CacheStrategy.RANDOM.isSuitableForPredictableAccess()).isFalse();
        assertThat(CacheStrategy.LFU.isSuitableForHighFrequency()).isFalse();
        assertThat(CacheStrategy.FIFO.isSuitableForHighFrequency()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, true, TTL_BASED", "false, true, false, TTL_BASED",
        "true, false, true, LRU", "true, false, false, RANDOM",
        "false, false, true, LFU", "false, false, false, LRU"
    })
    void recommend(boolean perf, boolean time, boolean predictable, CacheStrategy expected) {
        assertThat(CacheStrategy.recommend(perf, time, predictable)).isEqualTo(expected);
    }

    @Test
    void allStrategiesDescriptionListsEachStrategyOnce() {
        String text = CacheStrategy.getAllStrategiesDescription();
        assertThat(text).startsWith("Available Cache Strategies:");
        assertThat(text.lines().count()).isEqualTo(CacheStrategy.values().length + 1);
        for (CacheStrategy s : CacheStrategy.values()) {
            assertThat(text).contains("- " + s.name() + " (" + s.getDisplayName() + ")");
        }
    }
}
