// File location: src/main/java/utils/MathUtil.java
package utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Utility class for mathematical operations and statistical calculations
 * Provides academic-focused mathematical functions for grade calculations and analysis
 */
public final class MathUtil {
    
    private MathUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== CONSTANTS ====================
    
    public static final double EPSILON = 1e-10;
    public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
    public static final double EULER_NUMBER = Math.E;
    public static final double PI = Math.PI;
    
    // GPA scale constants
    public static final double GPA_SCALE_4_0 = 4.0;
    public static final double GPA_SCALE_5_0 = 5.0;
    public static final double PERCENTAGE_SCALE = 100.0;
    
    // ==================== BASIC OPERATIONS ====================
    
    /**
     * Safely adds two numbers, handling null values
     */
    public static double add(Double a, Double b) {
        return (a != null ? a : 0.0) + (b != null ? b : 0.0);
    }
    
    /**
     * Safely subtracts two numbers, handling null values
     */
    public static double subtract(Double a, Double b) {
        return (a != null ? a : 0.0) - (b != null ? b : 0.0);
    }
    
    /**
     * Safely multiplies two numbers, handling null values
     */
    public static double multiply(Double a, Double b) {
        if (a == null || b == null) return 0.0;
        return a * b;
    }
    
    /**
     * Safely divides two numbers, handling null values and division by zero
     */
    public static double divide(Double a, Double b) {
        if (a == null || b == null || Math.abs(b) < EPSILON) return 0.0;
        return a / b;
    }
    
    /**
     * Safe division with default value for division by zero
     */
    public static double divide(double a, double b, double defaultValue) {
        return Math.abs(b) < EPSILON ? defaultValue : a / b;
    }
    
    /**
     * Calculates percentage
     */
    public static double percentage(double value, double total) {
        return divide(value * 100.0, total, 0.0);
    }
    
    /**
     * Calculates percentage of change
     */
    public static double percentageChange(double oldValue, double newValue) {
        if (Math.abs(oldValue) < EPSILON) return 0.0;
        return ((newValue - oldValue) / oldValue) * 100.0;
    }
    
    // ==================== ROUNDING AND PRECISION ====================
    
    /**
     * Rounds to specified decimal places
     */
    public static double round(double value, int decimalPlaces) {
        if (decimalPlaces < 0) return value;
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    /**
     * Rounds up to specified decimal places
     */
    public static double roundUp(double value, int decimalPlaces) {
        if (decimalPlaces < 0) return value;
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(decimalPlaces, RoundingMode.CEILING);
        return bd.doubleValue();
    }
    
    /**
     * Rounds down to specified decimal places
     */
    public static double roundDown(double value, int decimalPlaces) {
        if (decimalPlaces < 0) return value;
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(decimalPlaces, RoundingMode.FLOOR);
        return bd.doubleValue();
    }
    
    /**
     * Rounds to nearest multiple
     */
    public static double roundToNearest(double value, double multiple) {
        if (Math.abs(multiple) < EPSILON) return value;
        return Math.round(value / multiple) * multiple;
    }
    
    /**
     * Truncates to specified decimal places
     */
    public static double truncate(double value, int decimalPlaces) {
        if (decimalPlaces < 0) return value;
        
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.floor(value * multiplier) / multiplier;
    }
    
    /**
     * Checks if two doubles are equal within epsilon
     */
    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }
    
    /**
     * Checks if two doubles are equal within specified tolerance
     */
    public static boolean equals(double a, double b, double tolerance) {
        return Math.abs(a - b) < tolerance;
    }
    
    // ==================== RANGE AND BOUNDS ====================
    
    /**
     * Clamps value within range
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamps integer value within range
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Checks if value is within range (inclusive)
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
    
    /**
     * Checks if value is within range (exclusive)
     */
    public static boolean isInRangeExclusive(double value, double min, double max) {
        return value > min && value < max;
    }
    
    /**
     * Normalizes value to 0-1 range
     */
    public static double normalize(double value, double min, double max) {
        if (Math.abs(max - min) < EPSILON) return 0.0;
        return clamp((value - min) / (max - min), 0.0, 1.0);
    }
    
    /**
     * Scales value from one range to another
     */
    public static double scale(double value, double fromMin, double fromMax, double toMin, double toMax) {
        double normalized = normalize(value, fromMin, fromMax);
        return toMin + normalized * (toMax - toMin);
    }
    
    // ==================== STATISTICAL FUNCTIONS ====================
    
    /**
     * Calculates mean (average) of numbers
     */
    public static double mean(double... values) {
        return mean(Arrays.stream(values).boxed().collect(Collectors.toList()));
    }
    
    /**
     * Calculates mean of collection
     */
    public static double mean(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) return 0.0;
        
        double sum = values.stream()
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .sum();
            
        return sum / values.size();
    }
    
    /**
     * Calculates weighted mean
     */
    public static double weightedMean(double[] values, double[] weights) {
        if (values == null || weights == null || values.length != weights.length) {
            throw new IllegalArgumentException("Values and weights arrays must be non-null and same length");
        }
        
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        for (int i = 0; i < values.length; i++) {
            weightedSum += values[i] * weights[i];
            totalWeight += weights[i];
        }
        
        return divide(weightedSum, totalWeight, 0.0);
    }
    
    /**
     * Calculates median of numbers
     */
    public static double median(double... values) {
        return median(Arrays.stream(values).boxed().collect(Collectors.toList()));
    }
    
    /**
     * Calculates median of collection
     */
    public static double median(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) return 0.0;
        
        List<Double> sorted = values.stream()
            .filter(Objects::nonNull)
            .map(Number::doubleValue)
            .sorted()
            .collect(Collectors.toList());
            
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    /**
     * Calculates mode (most frequent value)
     */
    public static List<Double> mode(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) return new ArrayList<>();
        
        Map<Double, Long> frequency = values.stream()
            .filter(Objects::nonNull)
            .map(Number::doubleValue)
            .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
            
        long maxFreq = frequency.values().stream().mapToLong(Long::longValue).max().orElse(0);
        
        return frequency.entrySet().stream()
            .filter(entry -> entry.getValue() == maxFreq)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates variance
     */
    public static double variance(Collection<? extends Number> values) {
        if (values == null || values.size() < 2) return 0.0;
        
        double mean = mean(values);
        double sumOfSquares = values.stream()
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .map(v -> Math.pow(v - mean, 2))
            .sum();
            
        return sumOfSquares / (values.size() - 1); // Sample variance
    }
    
    /**
     * Calculates population variance
     */
    public static double populationVariance(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) return 0.0;
        
        double mean = mean(values);
        double sumOfSquares = values.stream()
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .map(v -> Math.pow(v - mean, 2))
            .sum();
            
        return sumOfSquares / values.size();
    }
    
    /**
     * Calculates standard deviation
     */
    public static double standardDeviation(Collection<? extends Number> values) {
        return Math.sqrt(variance(values));
    }
    
    /**
     * Calculates population standard deviation
     */
    public static double populationStandardDeviation(Collection<? extends Number> values) {
        return Math.sqrt(populationVariance(values));
    }
    
    /**
     * Calculates coefficient of variation
     */
    public static double coefficientOfVariation(Collection<? extends Number> values) {
        double mean = mean(values);
        if (Math.abs(mean) < EPSILON) return 0.0;
        
        double stdDev = standardDeviation(values);
        return (stdDev / mean) * 100.0;
    }
    
    /**
     * Calculates range (max - min)
     */
    public static double range(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) return 0.0;
        
        DoubleSummaryStatistics stats = values.stream()
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .summaryStatistics();
            
        return stats.getMax() - stats.getMin();
    }
    
    /**
     * Calculates interquartile range (Q3 - Q1)
     */
    public static double interquartileRange(Collection<? extends Number> values) {
        if (values == null || values.size() < 4) return 0.0;
        
        List<Double> sorted = values.stream()
            .filter(Objects::nonNull)
            .map(Number::doubleValue)
            .sorted()
            .collect(Collectors.toList());
            
        return quartile(sorted, 3) - quartile(sorted, 1);
    }
    
    /**
     * Calculates specified quartile (1, 2, or 3)
     */
    public static double quartile(Collection<? extends Number> values, int quartile) {
        if (values == null || values.isEmpty() || quartile < 1 || quartile > 3) {
            return 0.0;
        }
        
        List<Double> sorted = values.stream()
            .filter(Objects::nonNull)
            .map(Number::doubleValue)
            .sorted()
            .collect(Collectors.toList());
            
        return quartile(sorted, quartile);
    }
    
    private static double quartile(List<Double> sortedValues, int quartile) {
        int n = sortedValues.size();
        double index = quartile * (n + 1) / 4.0;
        
        if (index == Math.floor(index)) {
            return sortedValues.get((int) index - 1);
        } else {
            int lower = (int) Math.floor(index) - 1;
            int upper = (int) Math.ceil(index) - 1;
            
            if (upper >= n) upper = n - 1;
            if (lower < 0) lower = 0;
            
            double weight = index - Math.floor(index);
            return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
        }
    }
    
    /**
     * Calculates percentile
     */
    public static double percentile(Collection<? extends Number> values, double percentile) {
        if (values == null || values.isEmpty() || percentile < 0 || percentile > 100) {
            return 0.0;
        }
        
        List<Double> sorted = values.stream()
            .filter(Objects::nonNull)
            .map(Number::doubleValue)
            .sorted()
            .collect(Collectors.toList());
            
        if (sorted.size() == 1) return sorted.get(0);
        
        double index = (percentile / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        
        if (lower == upper) {
            return sorted.get(lower);
        }
        
        double weight = index - lower;
        return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
    }
    
    // ==================== GPA CALCULATIONS ====================
    
    /**
     * Calculates GPA from grades and credit hours
     */
    public static double calculateGPA(double[] gradePoints, int[] creditHours) {
        if (gradePoints == null || creditHours == null || gradePoints.length != creditHours.length) {
            throw new IllegalArgumentException("Grade points and credit hours arrays must be non-null and same length");
        }
        
        double totalPoints = 0.0;
        int totalCredits = 0;
        
        for (int i = 0; i < gradePoints.length; i++) {
            totalPoints += gradePoints[i] * creditHours[i];
            totalCredits += creditHours[i];
        }
        
        return divide(totalPoints, totalCredits, 0.0);
    }
    
    /**
     * Calculates cumulative GPA
     */
    public static double calculateCumulativeGPA(double currentGPA, int currentCredits, 
                                               double newGPA, int newCredits) {
        double currentPoints = currentGPA * currentCredits;
        double newPoints = newGPA * newCredits;
        int totalCredits = currentCredits + newCredits;
        
        return divide(currentPoints + newPoints, totalCredits, 0.0);
    }
    
    /**
     * Converts percentage to GPA (4.0 scale)
     */
    public static double percentageToGPA(double percentage) {
        return percentageToGPA(percentage, GPA_SCALE_4_0);
    }
    
    /**
     * Converts percentage to GPA with custom scale
     */
    public static double percentageToGPA(double percentage, double scale) {
        percentage = clamp(percentage, 0.0, 100.0);
        
        if (percentage >= 97) return scale;
        if (percentage >= 93) return scale * 0.95;
        if (percentage >= 90) return scale * 0.925;
        if (percentage >= 87) return scale * 0.825;
        if (percentage >= 83) return scale * 0.75;
        if (percentage >= 80) return scale * 0.675;
        if (percentage >= 77) return scale * 0.575;
        if (percentage >= 73) return scale * 0.5;
        if (percentage >= 70) return scale * 0.425;
        if (percentage >= 67) return scale * 0.325;
        if (percentage >= 63) return scale * 0.25;
        if (percentage >= 60) return scale * 0.175;
        
        return 0.0;
    }
    
    /**
     * Converts GPA to percentage
     */
    public static double gpaToPercentage(double gpa) {
        return gpaToPercentage(gpa, GPA_SCALE_4_0);
    }
    
    /**
     * Converts GPA to percentage with custom scale
     */
    public static double gpaToPercentage(double gpa, double scale) {
        gpa = clamp(gpa, 0.0, scale);
        double ratio = gpa / scale;
        
        if (ratio >= 1.0) return 100.0;
        if (ratio >= 0.95) return 95.0;
        if (ratio >= 0.925) return 91.5;
        if (ratio >= 0.825) return 88.5;
        if (ratio >= 0.75) return 84.5;
        if (ratio >= 0.675) return 81.5;
        if (ratio >= 0.575) return 78.5;
        if (ratio >= 0.5) return 74.5;
        if (ratio >= 0.425) return 71.5;
        if (ratio >= 0.325) return 68.5;
        if (ratio >= 0.25) return 64.5;
        if (ratio >= 0.175) return 61.5;
        
        return ratio * 60.0;
    }
    
    /**
     * Calculates required GPA for target cumulative GPA
     */
    public static double requiredGPA(double currentGPA, int currentCredits, 
                                   double targetGPA, int newCredits) {
        double currentPoints = currentGPA * currentCredits;
        double totalCredits = currentCredits + newCredits;
        double requiredTotalPoints = targetGPA * totalCredits;
        double requiredNewPoints = requiredTotalPoints - currentPoints;
        
        return divide(requiredNewPoints, newCredits, 0.0);
    }
    
    // ==================== COMBINATORICS ====================
    
    /**
     * Calculates factorial
     */
    public static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        if (n > 20) throw new IllegalArgumentException("Factorial too large for long type");
        
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
    
    /**
     * Calculates factorial using BigInteger for large numbers
     */
    public static BigInteger factorialBig(int n) {
        if (n < 0) throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }
    
    /**
     * Calculates permutations P(n, r) = n! / (n-r)!
     */
    public static long permutations(int n, int r) {
        if (n < 0 || r < 0 || r > n) return 0;
        if (r == 0) return 1;
        
        long result = 1;
        for (int i = n; i > n - r; i--) {
            result *= i;
        }
        return result;
    }
    
    /**
     * Calculates combinations C(n, r) = n! / (r! * (n-r)!)
     */
    public static long combinations(int n, int r) {
        if (n < 0 || r < 0 || r > n) return 0;
        if (r == 0 || r == n) return 1;
        if (r > n - r) r = n - r; // Take advantage of symmetry
        
        long result = 1;
        for (int i = 0; i < r; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }
    
    // ==================== POWER AND ROOTS ====================
    
    /**
     * Safe power function handling edge cases
     */
    public static double power(double base, double exponent) {
        if (Double.isNaN(base) || Double.isNaN(exponent)) return Double.NaN;
        if (equals(base, 0.0) && exponent < 0) return Double.POSITIVE_INFINITY;
        
        return Math.pow(base, exponent);
    }
    
    /**
     * Integer power function (more efficient for integer exponents)
     */
    public static double power(double base, int exponent) {
        if (exponent == 0) return 1.0;
        if (exponent == 1) return base;
        if (exponent < 0) return 1.0 / power(base, -exponent);
        
        double result = 1.0;
        double currentPower = base;
        
        while (exponent > 0) {
            if (exponent % 2 == 1) {
                result *= currentPower;
            }
            currentPower *= currentPower;
            exponent /= 2;
        }
        
        return result;
    }
    
    /**
     * Square root with bounds checking
     */
    public static double sqrt(double value) {
        if (value < 0) return Double.NaN;
        return Math.sqrt(value);
    }
    
    /**
     * Cube root
     */
    public static double cbrt(double value) {
        return Math.cbrt(value);
    }
    
    /**
     * Nth root
     */
    public static double nthRoot(double value, int n) {
        if (n == 0) return Double.NaN;
        if (n == 1) return value;
        if (n == 2) return sqrt(value);
        if (n == 3) return cbrt(value);
        
        if (n % 2 == 0 && value < 0) return Double.NaN;
        
        double sign = value < 0 ? -1 : 1;
        return sign * Math.pow(Math.abs(value), 1.0 / n);
    }
    
    // ==================== LOGARITHMS ====================
    
    /**
     * Natural logarithm with bounds checking
     */
    public static double ln(double value) {
        if (value <= 0) return Double.NaN;
        return Math.log(value);
    }
    
    /**
     * Base-10 logarithm
     */
    public static double log10(double value) {
        if (value <= 0) return Double.NaN;
        return Math.log10(value);
    }
    
    /**
     * Logarithm with arbitrary base
     */
    public static double log(double value, double base) {
        if (value <= 0 || base <= 0 || equals(base, 1.0)) return Double.NaN;
        return Math.log(value) / Math.log(base);
    }
    
    // ==================== TRIGONOMETRIC FUNCTIONS ====================
    
    /**
     * Converts degrees to radians
     */
    public static double toRadians(double degrees) {
        return Math.toRadians(degrees);
    }
    
    /**
     * Converts radians to degrees
     */
    public static double toDegrees(double radians) {
        return Math.toDegrees(radians);
    }
    
    /**
     * Sine function with degree input
     */
    public static double sinDegrees(double degrees) {
        return Math.sin(toRadians(degrees));
    }
    
    /**
     * Cosine function with degree input
     */
    public static double cosDegrees(double degrees) {
        return Math.cos(toRadians(degrees));
    }
    
    /**
     * Tangent function with degree input
     */
    public static double tanDegrees(double degrees) {
        return Math.tan(toRadians(degrees));
    }
    
    // ==================== NUMBER THEORY ====================
    
    /**
     * Greatest Common Divisor
     */
    public static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    
    /**
     * Least Common Multiple
     */
    public static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
    }
    
    /**
     * Checks if number is prime
     */
    public static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    /**
     * Finds all prime factors
     */
    public static List<Integer> primeFactors(int n) {
        List<Integer> factors = new ArrayList<>();
        
        // Handle factor 2
        while (n % 2 == 0) {
            factors.add(2);
            n /= 2;
        }
        
        // Handle odd factors
        for (int i = 3; i * i <= n; i += 2) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }
        
        // If n is still > 2, it's a prime
        if (n > 2) {
            factors.add(n);
        }
        
        return factors;
    }
    
    // ==================== FINANCIAL MATHEMATICS ====================
    
    /**
     * Calculates compound interest
     */
    public static double compoundInterest(double principal, double rate, int periods, double time) {
        return principal * Math.pow(1 + rate / periods, periods * time);
    }
    
    /**
     * Calculates simple interest
     */
    public static double simpleInterest(double principal, double rate, double time) {
        return principal * (1 + rate * time);
    }
    
    /**
     * Calculates present value
     */
    public static double presentValue(double futureValue, double rate, double time) {
        return futureValue / Math.pow(1 + rate, time);
    }
    
    /**
     * Calculates annuity payment
     */
    public static double annuityPayment(double principal, double rate, int periods) {
        if (equals(rate, 0.0)) return principal / periods;
        
        double factor = Math.pow(1 + rate, periods);
        return principal * (rate * factor) / (factor - 1);
    }
    
    // ==================== SEQUENCE AND SERIES ====================
    
    /**
     * Calculates nth Fibonacci number
     */
    public static long fibonacci(int n) {
        if (n < 0) throw new IllegalArgumentException("Fibonacci not defined for negative numbers");
        if (n <= 1) return n;
        
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }
    
    /**
     * Calculates sum of arithmetic sequence
     */
    public static double arithmeticSum(double first, double last, int terms) {
        return terms * (first + last) / 2.0;
    }
    
    /**
     * Calculates sum of geometric series
     */
    public static double geometricSum(double first, double ratio, int terms) {
        if (equals(ratio, 1.0)) return first * terms;
        return first * (1 - Math.pow(ratio, terms)) / (1 - ratio);
    }
    
    /**
     * Calculates sum of infinite geometric series
     */
    public static double infiniteGeometricSum(double first, double ratio) {
        if (Math.abs(ratio) >= 1) return Double.NaN;
        return first / (1 - ratio);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if number is even
     */
    public static boolean isEven(int n) {
        return n % 2 == 0;
    }
    
    /**
     * Checks if number is odd
     */
    public static boolean isOdd(int n) {
        return n % 2 != 0;
    }
    
    /**
     * Gets absolute value safely
     */
    public static double abs(Double value) {
        return value == null ? 0.0 : Math.abs(value);
    }
    
    /**
     * Gets sign of number (-1, 0, or 1)
     */
    public static int sign(double value) {
        if (value > 0) return 1;
        if (value < 0) return -1;
        return 0;
    }
    
    /**
     * Linear interpolation between two values
     */
    public static double lerp(double start, double end, double t) {
        t = clamp(t, 0.0, 1.0);
        return start + t * (end - start);
    }
    
    /**
     * Maps value from one range to another (linear mapping)
     */
    public static double map(double value, double fromMin, double fromMax, double toMin, double toMax) {
        return scale(value, fromMin, fromMax, toMin, toMax);
    }
    
    /**
     * Calculates distance between two points
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Generates random number in range
     */
    public static double random(double min, double max) {
        return min + Math.random() * (max - min);
    }
    
    /**
     * Generates random integer in range (inclusive)
     */
    public static int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }
    
    // ==================== VALIDATION METHODS ====================
    
    /**
     * Checks if value is finite
     */
    public static boolean isFinite(double value) {
        return Double.isFinite(value);
    }
    
    /**
     * Checks if value is infinite
     */
    public static boolean isInfinite(double value) {
        return Double.isInfinite(value);
    }
    
    /**
     * Checks if value is NaN
     */
    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }
    
    /**
     * Checks if value is a valid number (finite and not NaN)
     */
    public static boolean isValidNumber(double value) {
        return isFinite(value) && !isNaN(value);
    }
    
    /**
     * Returns safe value (replaces NaN and infinity with default)
     */
    public static double safeValue(double value, double defaultValue) {
        return isValidNumber(value) ? value : defaultValue;
    }
}