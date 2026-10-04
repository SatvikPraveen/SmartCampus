package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import scheduling.experiment.Statistics;

class StatisticsTest {

    @Test
    void descriptiveStatistics() {
        double[] x = {2, 4, 4, 4, 5, 5, 7, 9};
        assertEquals(5.0, Statistics.mean(x), 1e-12);
        assertEquals(Math.sqrt(32.0 / 7), Statistics.standardDeviation(x), 1e-12);
        assertEquals(4.5, Statistics.median(x), 1e-12);
        assertEquals(0.0, Statistics.standardDeviation(new double[] {3}), 0);
        assertThrows(IllegalArgumentException.class, () -> Statistics.mean(new double[0]));
    }

    @Test
    void bootstrapIntervalBracketsMeanAndIsReproducible() {
        double[] x = {10, 12, 9, 11, 13, 8, 10, 12, 11, 9};
        var a = Statistics.bootstrapMean(x, 0.95, 5_000, 1);
        var b = Statistics.bootstrapMean(x, 0.95, 5_000, 1);
        assertEquals(a, b);
        assertTrue(a.lower() <= a.estimate() && a.estimate() <= a.upper());
        assertTrue(a.lower() > 9 && a.upper() < 12);
    }

    @Test
    void constantSampleGivesDegenerateInterval() {
        var i = Statistics.bootstrapMean(new double[] {3, 3, 3}, 0.95, 1_000, 1);
        assertEquals(3.0, i.lower(), 0);
        assertEquals(3.0, i.upper(), 0);
    }

    @Test
    void pairedDifferenceDetectsConsistentShift() {
        double[] a = {5, 6, 7, 8, 9, 10};
        double[] b = {4, 5, 6, 7, 8, 9};
        var d = Statistics.pairedDifference(a, b, 0.95, 2_000, 3);
        assertEquals(1.0, d.estimate(), 1e-12);
        assertTrue(d.excludesZero());
    }

    @Test
    void signTestMatchesBinomialTail() {
        // 6 of 6 positive: p = 2 * (1/2)^6 = 0.03125
        assertEquals(0.03125, Statistics.signTestPValue(new double[] {1, 1, 1, 1, 1, 1}, new double[6]), 1e-12);
        // Ties only: no evidence.
        assertEquals(1.0, Statistics.signTestPValue(new double[] {1, 2}, new double[] {1, 2}), 0);
        // Balanced signs: p = 1.
        assertFalse(Statistics.signTestPValue(new double[] {1, 0}, new double[] {0, 1}) < 1.0);
    }
}
