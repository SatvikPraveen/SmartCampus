package utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class MathUtilTest {

    private static final double TOL = 1e-9;

    @Nested
    class BasicArithmetic {
        @Test
        void nullSafeOperations() {
            assertThat(MathUtil.add(null, 2.0)).isEqualTo(2.0);
            assertThat(MathUtil.subtract(5.0, null)).isEqualTo(5.0);
            assertThat(MathUtil.multiply(null, 3.0)).isEqualTo(0.0);
            assertThat(MathUtil.multiply(2.0, 3.0)).isEqualTo(6.0);
            assertThat(MathUtil.divide(6.0, 3.0)).isEqualTo(2.0);
            assertThat(MathUtil.divide(6.0, 0.0)).isEqualTo(0.0);
            assertThat(MathUtil.divide(6.0, (Double) null)).isEqualTo(0.0);
            assertThat(MathUtil.divide(1, 0, -1)).isEqualTo(-1.0);
        }

        @Test
        void percentages() {
            assertThat(MathUtil.percentage(25, 200)).isEqualTo(12.5);
            assertThat(MathUtil.percentage(1, 0)).isEqualTo(0.0);
            assertThat(MathUtil.percentageChange(50, 75)).isEqualTo(50.0);
            assertThat(MathUtil.percentageChange(80, 60)).isEqualTo(-25.0);
            assertThat(MathUtil.percentageChange(0, 60)).isEqualTo(0.0);
        }
    }

    @Nested
    class Rounding {
        @ParameterizedTest
        @CsvSource({"2.345, 2, 2.35", "2.344, 2, 2.34", "-2.345, 2, -2.35", "1.005, 2, 1.01", "7.5, 0, 8.0"})
        void roundHalfUp(double value, int places, double expected) {
            assertThat(MathUtil.round(value, places)).isEqualTo(expected);
        }

        @Test
        void roundUpAndDown() {
            assertThat(MathUtil.roundUp(2.341, 2)).isEqualTo(2.35);
            assertThat(MathUtil.roundUp(-2.349, 2)).isEqualTo(-2.34);
            assertThat(MathUtil.roundDown(2.349, 2)).isEqualTo(2.34);
            assertThat(MathUtil.roundDown(-2.341, 2)).isEqualTo(-2.35);
            assertThat(MathUtil.round(2.345, -1)).isEqualTo(2.345);
        }

        @Test
        void roundToNearest() {
            assertThat(MathUtil.roundToNearest(87.3, 5)).isEqualTo(85.0);
            assertThat(MathUtil.roundToNearest(87.5, 5)).isEqualTo(90.0);
            assertThat(MathUtil.roundToNearest(3.3, 0)).isEqualTo(3.3);
        }

        @ParameterizedTest
        @CsvSource({"2.349, 2, 2.34", "0.29, 2, 0.29", "1.1, 1, 1.1", "4.35, 2, 4.35", "-1.55, 1, -1.5", "9.999, 0, 9.0"})
        void truncateDropsDigitsTowardZero(double value, int places, double expected) {
            assertThat(MathUtil.truncate(value, places)).isEqualTo(expected);
        }

        @Test
        void epsilonEquality() {
            assertThat(MathUtil.equals(0.1 + 0.2, 0.3)).isTrue();
            assertThat(MathUtil.equals(1.0, 1.001)).isFalse();
            assertThat(MathUtil.equals(1.0, 1.001, 0.01)).isTrue();
        }
    }

    @Nested
    class Ranges {
        @Test
        void clamp() {
            assertThat(MathUtil.clamp(5.0, 0.0, 4.0)).isEqualTo(4.0);
            assertThat(MathUtil.clamp(-1.0, 0.0, 4.0)).isEqualTo(0.0);
            assertThat(MathUtil.clamp(7, 1, 6)).isEqualTo(6);
            assertThat(MathUtil.clamp(3, 1, 6)).isEqualTo(3);
        }

        @Test
        void inRange() {
            assertThat(MathUtil.isInRange(4.0, 0, 4)).isTrue();
            assertThat(MathUtil.isInRangeExclusive(4.0, 0, 4)).isFalse();
            assertThat(MathUtil.isInRangeExclusive(2.0, 0, 4)).isTrue();
        }

        @Test
        void normalizeScaleAndMap() {
            assertThat(MathUtil.normalize(75, 50, 100)).isEqualTo(0.5);
            assertThat(MathUtil.normalize(150, 50, 100)).isEqualTo(1.0);
            assertThat(MathUtil.normalize(5, 5, 5)).isEqualTo(0.0);
            assertThat(MathUtil.scale(75, 0, 100, 0, 4)).isEqualTo(3.0);
            assertThat(MathUtil.map(5, 0, 10, 100, 200)).isEqualTo(150.0);
            assertThat(MathUtil.lerp(10, 20, 0.25)).isEqualTo(12.5);
            assertThat(MathUtil.lerp(10, 20, 2)).isEqualTo(20.0);
        }
    }

    @Nested
    class Statistics {
        private final List<Double> scores = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);

        @Test
        void meanMedianMode() {
            assertThat(MathUtil.mean(scores)).isEqualTo(5.0);
            assertThat(MathUtil.mean(1, 2, 3, 4)).isEqualTo(2.5);
            assertThat(MathUtil.mean(List.<Double>of())).isEqualTo(0.0);
            assertThat(MathUtil.median(scores)).isEqualTo(4.5);
            assertThat(MathUtil.median(3, 1, 2)).isEqualTo(2.0);
            assertThat(MathUtil.mode(scores)).containsExactly(4.0);
            assertThat(MathUtil.mode(List.of(1, 1, 2, 2, 3))).containsExactlyInAnyOrder(1.0, 2.0);
        }

        @Test
        void nullElementsAreIgnoredByMeanAndVariance() {
            List<Double> withNull = Arrays.asList(1.0, null, 3.0);
            assertThat(MathUtil.mean(withNull)).isEqualTo(2.0);
            assertThat(MathUtil.median(withNull)).isEqualTo(2.0);
            assertThat(MathUtil.variance(withNull)).isEqualTo(MathUtil.variance(List.of(1.0, 3.0)));
            assertThat(MathUtil.populationVariance(withNull)).isEqualTo(1.0);
        }

        @Test
        void varianceAndStandardDeviation() {
            assertThat(MathUtil.populationVariance(scores)).isCloseTo(4.0, within(TOL));
            assertThat(MathUtil.populationStandardDeviation(scores)).isCloseTo(2.0, within(TOL));
            assertThat(MathUtil.variance(scores)).isCloseTo(32.0 / 7, within(TOL));
            assertThat(MathUtil.standardDeviation(scores)).isCloseTo(Math.sqrt(32.0 / 7), within(TOL));
            assertThat(MathUtil.variance(List.of(3.0))).isEqualTo(0.0);
            assertThat(MathUtil.coefficientOfVariation(scores))
                .isCloseTo(Math.sqrt(32.0 / 7) / 5.0 * 100, within(TOL));
            assertThat(MathUtil.coefficientOfVariation(List.of(0.0, 0.0))).isEqualTo(0.0);
        }

        @Test
        void rangeQuartilesAndPercentiles() {
            assertThat(MathUtil.range(scores)).isEqualTo(7.0);
            assertThat(MathUtil.range(Collections.<Double>emptyList())).isEqualTo(0.0);
            List<Integer> seven = List.of(1, 2, 3, 4, 5, 6, 7);
            assertThat(MathUtil.quartile(seven, 1)).isEqualTo(2.0);
            assertThat(MathUtil.quartile(seven, 2)).isEqualTo(4.0);
            assertThat(MathUtil.quartile(seven, 3)).isEqualTo(6.0);
            assertThat(MathUtil.quartile(seven, 4)).isEqualTo(0.0);
            assertThat(MathUtil.interquartileRange(seven)).isEqualTo(4.0);
            assertThat(MathUtil.interquartileRange(List.of(1, 2, 3))).isEqualTo(0.0);

            List<Integer> five = List.of(10, 20, 30, 40, 50);
            assertThat(MathUtil.percentile(five, 0)).isEqualTo(10.0);
            assertThat(MathUtil.percentile(five, 50)).isEqualTo(30.0);
            assertThat(MathUtil.percentile(five, 100)).isEqualTo(50.0);
            assertThat(MathUtil.percentile(five, 10)).isCloseTo(14.0, within(TOL));
            assertThat(MathUtil.percentile(five, 101)).isEqualTo(0.0);
            assertThat(MathUtil.percentile(List.of(7), 90)).isEqualTo(7.0);
        }

        @Test
        void weightedMean() {
            assertThat(MathUtil.weightedMean(new double[]{90, 80}, new double[]{3, 1})).isEqualTo(87.5);
            assertThat(MathUtil.weightedMean(new double[]{90}, new double[]{0})).isEqualTo(0.0);
            assertThatThrownBy(() -> MathUtil.weightedMean(new double[]{1}, new double[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Gpa {
        @Test
        void calculateGpaWeightsByCredits() {
            assertThat(MathUtil.calculateGPA(new double[]{4.0, 3.0}, new int[]{3, 1})).isEqualTo(3.75);
            assertThat(MathUtil.calculateGPA(new double[]{}, new int[]{})).isEqualTo(0.0);
            assertThatThrownBy(() -> MathUtil.calculateGPA(new double[]{4.0}, null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void cumulativeAndRequiredGpaAreConsistent() {
            double cumulative = MathUtil.calculateCumulativeGPA(3.0, 30, 4.0, 15);
            assertThat(cumulative).isCloseTo(10.0 / 3, within(TOL));
            double required = MathUtil.requiredGPA(3.0, 30, cumulative, 15);
            assertThat(required).isCloseTo(4.0, within(TOL));
            assertThat(MathUtil.calculateCumulativeGPA(0, 0, 0, 0)).isEqualTo(0.0);
            assertThat(MathUtil.requiredGPA(3.0, 30, 3.5, 0)).isEqualTo(0.0);
        }

        @ParameterizedTest
        @CsvSource({"100, 4.0", "97, 4.0", "93, 3.8", "90, 3.7", "87, 3.3", "83, 3.0", "80, 2.7",
                    "77, 2.3", "73, 2.0", "70, 1.7", "67, 1.3", "63, 1.0", "60, 0.7", "59.9, 0.0", "150, 4.0", "-5, 0.0"})
        void percentageToGpa(double pct, double gpa) {
            assertThat(MathUtil.percentageToGPA(pct)).isCloseTo(gpa, within(TOL));
        }

        @Test
        void percentageToGpaIsMonotonic() {
            double previous = -1;
            for (int p = 0; p <= 100; p++) {
                double g = MathUtil.percentageToGPA(p);
                assertThat(g).isGreaterThanOrEqualTo(previous);
                previous = g;
            }
            assertThat(MathUtil.percentageToGPA(95, MathUtil.GPA_SCALE_5_0)).isCloseTo(4.75, within(TOL));
        }

        @Test
        void gpaToPercentage() {
            assertThat(MathUtil.gpaToPercentage(4.0)).isEqualTo(100.0);
            assertThat(MathUtil.gpaToPercentage(5.0)).isEqualTo(100.0);
            assertThat(MathUtil.gpaToPercentage(3.0)).isEqualTo(84.5);
            assertThat(MathUtil.gpaToPercentage(0.0)).isEqualTo(0.0);
            assertThat(MathUtil.gpaToPercentage(0.4)).isCloseTo(6.0, within(TOL));
        }
    }

    @Nested
    class Combinatorics {
        @ParameterizedTest
        @CsvSource({"0, 1", "1, 1", "5, 120", "20, 2432902008176640000"})
        void factorial(int n, long expected) {
            assertThat(MathUtil.factorial(n)).isEqualTo(expected);
            assertThat(MathUtil.factorialBig(n)).isEqualTo(BigInteger.valueOf(expected));
        }

        @Test
        void factorialBounds() {
            assertThatThrownBy(() -> MathUtil.factorial(-1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> MathUtil.factorial(21)).isInstanceOf(IllegalArgumentException.class);
            assertThat(MathUtil.factorialBig(25)).isEqualTo(new BigInteger("15511210043330985984000000"));
            assertThatThrownBy(() -> MathUtil.factorialBig(-1)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"5, 2, 20, 10", "5, 0, 1, 1", "5, 5, 120, 1", "5, 6, 0, 0", "-1, 0, 0, 0", "52, 5, 311875200, 2598960"})
        void permutationsAndCombinations(int n, int r, long perms, long combos) {
            assertThat(MathUtil.permutations(n, r)).isEqualTo(perms);
            assertThat(MathUtil.combinations(n, r)).isEqualTo(combos);
        }

        @Test
        void combinationsAreSymmetric() {
            for (int r = 0; r <= 30; r++) {
                assertThat(MathUtil.combinations(30, r)).isEqualTo(MathUtil.combinations(30, 30 - r));
            }
        }
    }

    @Nested
    class PowersRootsLogs {
        @Test
        void power() {
            assertThat(MathUtil.power(2.0, 10)).isEqualTo(1024.0);
            assertThat(MathUtil.power(2.0, -2)).isEqualTo(0.25);
            assertThat(MathUtil.power(5.0, 0)).isEqualTo(1.0);
            assertThat(MathUtil.power(3.0, 7)).isEqualTo(2187.0);
            assertThat(MathUtil.power(2.0, 0.5)).isCloseTo(Math.sqrt(2), within(TOL));
            assertThat(MathUtil.power(0.0, -1.0)).isEqualTo(Double.POSITIVE_INFINITY);
            assertThat(MathUtil.power(Double.NaN, 2.0)).isNaN();
        }

        @Test
        void roots() {
            assertThat(MathUtil.sqrt(-1)).isNaN();
            assertThat(MathUtil.sqrt(16)).isEqualTo(4.0);
            assertThat(MathUtil.cbrt(-27)).isEqualTo(-3.0);
            assertThat(MathUtil.nthRoot(81, 4)).isCloseTo(3.0, within(TOL));
            assertThat(MathUtil.nthRoot(-32, 5)).isCloseTo(-2.0, within(TOL));
            assertThat(MathUtil.nthRoot(-16, 4)).isNaN();
            assertThat(MathUtil.nthRoot(8, 0)).isNaN();
            assertThat(MathUtil.nthRoot(8, 1)).isEqualTo(8.0);
        }

        @Test
        void logarithms() {
            assertThat(MathUtil.ln(Math.E)).isCloseTo(1.0, within(TOL));
            assertThat(MathUtil.ln(0)).isNaN();
            assertThat(MathUtil.log10(1000)).isCloseTo(3.0, within(TOL));
            assertThat(MathUtil.log10(-1)).isNaN();
            assertThat(MathUtil.log(8, 2)).isCloseTo(3.0, within(TOL));
            assertThat(MathUtil.log(8, 1)).isNaN();
            assertThat(MathUtil.log(8, -2)).isNaN();
        }

        @Test
        void trigonometry() {
            assertThat(MathUtil.sinDegrees(30)).isCloseTo(0.5, within(TOL));
            assertThat(MathUtil.cosDegrees(60)).isCloseTo(0.5, within(TOL));
            assertThat(MathUtil.tanDegrees(45)).isCloseTo(1.0, within(TOL));
            assertThat(MathUtil.toDegrees(MathUtil.toRadians(123.0))).isCloseTo(123.0, within(TOL));
        }
    }

    @Nested
    class NumberTheory {
        @ParameterizedTest
        @CsvSource({"12, 18, 6, 36", "-4, 6, 2, 12", "7, 13, 1, 91", "0, 5, 5, 0", "65536, 65536, 65536, 65536",
                    "100000, 30000, 10000, 300000"})
        void gcdAndLcm(int a, int b, int gcd, int lcm) {
            assertThat(MathUtil.gcd(a, b)).isEqualTo(gcd);
            assertThat(MathUtil.lcm(a, b)).isEqualTo(lcm);
        }

        @ParameterizedTest
        @ValueSource(ints = {2, 3, 5, 7, 97, 7919})
        void primes(int n) {
            assertThat(MathUtil.isPrime(n)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {-7, 0, 1, 4, 9, 91, 7917})
        void nonPrimes(int n) {
            assertThat(MathUtil.isPrime(n)).isFalse();
        }

        @Test
        void primeFactorsMultiplyBackToInput() {
            for (int n = 2; n <= 500; n++) {
                List<Integer> factors = MathUtil.primeFactors(n);
                assertThat(factors).allMatch(MathUtil::isPrime).isSorted();
                assertThat(factors.stream().reduce(1, (a, b) -> a * b)).isEqualTo(n);
            }
            assertThat(MathUtil.primeFactors(360)).containsExactly(2, 2, 2, 3, 3, 5);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, -1, -12})
        void primeFactorsOfNumbersBelowTwoIsEmptyAndTerminates(int n) {
            List<Integer> factors = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> MathUtil.primeFactors(n));
            assertThat(factors).isEmpty();
        }
    }

    @Nested
    class FinanceAndSeries {
        @Test
        void interest() {
            assertThat(MathUtil.compoundInterest(1000, 0.12, 12, 1))
                .isCloseTo(1000 * Math.pow(1.01, 12), within(1e-6));
            assertThat(MathUtil.simpleInterest(1000, 0.05, 2)).isCloseTo(1100.0, within(1e-9));
            assertThat(MathUtil.presentValue(1210, 0.1, 2)).isCloseTo(1000.0, within(1e-9));
            assertThat(MathUtil.annuityPayment(1200, 0, 12)).isEqualTo(100.0);
            assertThat(MathUtil.annuityPayment(1000, 0.1, 2)).isCloseTo(576.1904761904, within(1e-6));
        }

        @ParameterizedTest
        @CsvSource({"0, 0", "1, 1", "2, 1", "10, 55", "50, 12586269025", "92, 7540113804746346429"})
        void fibonacci(int n, long expected) {
            assertThat(MathUtil.fibonacci(n)).isEqualTo(expected);
        }

        @Test
        void fibonacciNegative() {
            assertThatThrownBy(() -> MathUtil.fibonacci(-1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void seriesSums() {
            assertThat(MathUtil.arithmeticSum(1, 100, 100)).isEqualTo(5050.0);
            assertThat(MathUtil.geometricSum(1, 2, 10)).isEqualTo(1023.0);
            assertThat(MathUtil.geometricSum(3, 1, 4)).isEqualTo(12.0);
            assertThat(MathUtil.infiniteGeometricSum(1, 0.5)).isEqualTo(2.0);
            assertThat(MathUtil.infiniteGeometricSum(1, 1)).isNaN();
        }
    }

    @Nested
    class Misc {
        @Test
        void parityAndSign() {
            assertThat(MathUtil.isEven(-4)).isTrue();
            assertThat(MathUtil.isOdd(-3)).isTrue();
            assertThat(MathUtil.isOdd(0)).isFalse();
            assertThat(MathUtil.sign(-0.5)).isEqualTo(-1);
            assertThat(MathUtil.sign(0)).isZero();
            assertThat(MathUtil.sign(3)).isEqualTo(1);
            assertThat(MathUtil.abs(null)).isEqualTo(0.0);
            assertThat(MathUtil.abs(-2.5)).isEqualTo(2.5);
        }

        @Test
        void distance() {
            assertThat(MathUtil.distance(0, 0, 3, 4)).isEqualTo(5.0);
        }

        @Test
        void randomValuesStayInRange() {
            for (int i = 0; i < 1000; i++) {
                assertThat(MathUtil.random(2.0, 3.0)).isBetween(2.0, 3.0);
                assertThat(MathUtil.randomInt(1, 6)).isBetween(1, 6);
            }
        }

        @Test
        void validity() {
            assertThat(MathUtil.isValidNumber(1.0)).isTrue();
            assertThat(MathUtil.isValidNumber(Double.NaN)).isFalse();
            assertThat(MathUtil.isValidNumber(Double.NEGATIVE_INFINITY)).isFalse();
            assertThat(MathUtil.isInfinite(Double.POSITIVE_INFINITY)).isTrue();
            assertThat(MathUtil.isNaN(0.0 / 0.0)).isTrue();
            assertThat(MathUtil.safeValue(Double.NaN, -1)).isEqualTo(-1.0);
            assertThat(MathUtil.safeValue(2.0, -1)).isEqualTo(2.0);
        }
    }
}
