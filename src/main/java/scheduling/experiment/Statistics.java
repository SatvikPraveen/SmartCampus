package scheduling.experiment;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Small, dependency-free statistics toolkit for experiment reporting.
 */
public final class Statistics {

    /**
     * A point estimate with a two-sided percentile-bootstrap confidence interval.
     *
     * @param estimate point estimate
     * @param lower    lower bound
     * @param upper    upper bound
     */
    public record Interval(double estimate, double lower, double upper) {

        /** True when the interval excludes zero. */
        public boolean excludesZero() {
            return lower > 0 || upper < 0;
        }
    }

    private Statistics() {
    }

    public static double mean(double[] x) {
        requireNonEmpty(x);
        double s = 0;
        for (double v : x) {
            s += v;
        }
        return s / x.length;
    }

    /** Unbiased sample standard deviation (0 for a single observation). */
    public static double standardDeviation(double[] x) {
        requireNonEmpty(x);
        if (x.length < 2) {
            return 0.0;
        }
        double m = mean(x);
        double ss = 0;
        for (double v : x) {
            ss += (v - m) * (v - m);
        }
        return Math.sqrt(ss / (x.length - 1));
    }

    public static double median(double[] x) {
        requireNonEmpty(x);
        double[] s = x.clone();
        Arrays.sort(s);
        int n = s.length;
        return n % 2 == 1 ? s[n / 2] : 0.5 * (s[n / 2 - 1] + s[n / 2]);
    }

    /**
     * Percentile bootstrap confidence interval for the mean.
     *
     * @param x          observations
     * @param confidence e.g. 0.95
     * @param resamples  number of bootstrap resamples, e.g. 10&nbsp;000
     * @param seed       RNG seed, making the interval reproducible
     */
    public static Interval bootstrapMean(double[] x, double confidence, int resamples, long seed) {
        requireNonEmpty(x);
        SplittableRandom rng = new SplittableRandom(seed);
        double[] means = new double[resamples];
        int n = x.length;
        for (int b = 0; b < resamples; b++) {
            double s = 0;
            for (int i = 0; i < n; i++) {
                s += x[rng.nextInt(n)];
            }
            means[b] = s / n;
        }
        Arrays.sort(means);
        double alpha = (1 - confidence) / 2;
        return new Interval(mean(x), quantile(means, alpha), quantile(means, 1 - alpha));
    }

    /** Bootstrap CI of the mean paired difference {@code a[i] - b[i]}. */
    public static Interval pairedDifference(double[] a, double[] b, double confidence, int resamples, long seed) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Paired samples must have equal length");
        }
        double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            d[i] = a[i] - b[i];
        }
        return bootstrapMean(d, confidence, resamples, seed);
    }

    /**
     * Exact two-sided sign test p-value for paired samples, ignoring ties.
     * Small samples make the bootstrap unreliable; the sign test needs no distributional assumption.
     */
    public static double signTestPValue(double[] a, double[] b) {
        int pos = 0;
        int neg = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > b[i]) {
                pos++;
            } else if (a[i] < b[i]) {
                neg++;
            }
        }
        int n = pos + neg;
        if (n == 0) {
            return 1.0;
        }
        int k = Math.min(pos, neg);
        double p = 0;
        for (int i = 0; i <= k; i++) {
            p += Math.exp(logChoose(n, i) - n * Math.log(2));
        }
        return Math.min(1.0, 2 * p);
    }

    /** Linear-interpolated quantile of a sorted array. */
    static double quantile(double[] sorted, double q) {
        double pos = q * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        return sorted[lo] + (pos - lo) * (sorted[hi] - sorted[lo]);
    }

    private static double logChoose(int n, int k) {
        double r = 0;
        for (int i = 1; i <= k; i++) {
            r += Math.log(n - k + i) - Math.log(i);
        }
        return r;
    }

    private static void requireNonEmpty(double[] x) {
        if (x == null || x.length == 0) {
            throw new IllegalArgumentException("At least one observation is required");
        }
    }
}
