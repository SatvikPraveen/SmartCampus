// File location: src/main/java/functional/StreamUtils.java
package functional;

import models.*;
import enums.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Utility class providing enhanced stream operations and custom stream methods
 * for campus entities and common data processing tasks
 */
public final class StreamUtils {
    
    private StreamUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== STREAM CREATION UTILITIES ====================
    
    /**
     * Creates a stream from nullable collection
     */
    public static <T> Stream<T> streamOf(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }
    
    /**
     * Creates a stream from nullable array
     */
    @SafeVarargs
    public static <T> Stream<T> streamOf(T... elements) {
        return elements == null ? Stream.empty() : Arrays.stream(elements);
    }
    
    /**
     * Creates a stream from optional
     */
    public static <T> Stream<T> streamOf(Optional<T> optional) {
        return optional.map(Stream::of).orElse(Stream.empty());
    }
    
    /**
     * Creates an infinite stream with a supplier
     */
    public static <T> Stream<T> generate(Supplier<T> supplier) {
        return Stream.generate(supplier);
    }
    
    /**
     * Creates a stream of numbers in range
     */
    public static IntStream range(int start, int end) {
        return IntStream.range(start, end);
    }
    
    /**
     * Creates a stream of numbers in range (inclusive)
     */
    public static IntStream rangeClosed(int start, int end) {
        return IntStream.rangeClosed(start, end);
    }
    
    // ==================== FILTERING UTILITIES ====================
    
    /**
     * Filters out null values
     */
    public static <T> Stream<T> nonNull(Stream<T> stream) {
        return stream.filter(Objects::nonNull);
    }
    
    /**
     * Filters distinct elements by a key extractor
     */
    public static <T, K> Predicate<T> distinctBy(Function<T, K> keyExtractor) {
        Set<K> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }
    
    /**
     * Filters elements that match any of the predicates
     */
    @SafeVarargs
    public static <T> Predicate<T> anyMatch(Predicate<T>... predicates) {
        return t -> Arrays.stream(predicates).anyMatch(p -> p.test(t));
    }
    
    /**
     * Filters elements that match all predicates
     */
    @SafeVarargs
    public static <T> Predicate<T> allMatch(Predicate<T>... predicates) {
        return t -> Arrays.stream(predicates).allMatch(p -> p.test(t));
    }
    
    /**
     * Filters elements within a specific index range
     */
    public static <T> Stream<T> slice(Stream<T> stream, int start, int end) {
        return stream.skip(start).limit(end - start);
    }
    
    /**
     * Takes first n elements
     */
    public static <T> Stream<T> take(Stream<T> stream, int n) {
        return stream.limit(n);
    }
    
    /**
     * Skips first n elements
     */
    public static <T> Stream<T> drop(Stream<T> stream, int n) {
        return stream.skip(n);
    }
    
    /**
     * Takes elements while predicate is true
     */
    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<T> predicate) {
        return stream.takeWhile(predicate);
    }
    
    /**
     * Drops elements while predicate is true
     */
    public static <T> Stream<T> dropWhile(Stream<T> stream, Predicate<T> predicate) {
        return stream.dropWhile(predicate);
    }
    
    // ==================== TRANSFORMATION UTILITIES ====================
    
    /**
     * Maps with index
     */
    public static <T, R> Stream<R> mapWithIndex(Stream<T> stream, BiFunction<T, Integer, R> mapper) {
        return stream.collect(
            () -> new Object() {
                int index = 0;
                Stream.Builder<R> builder = Stream.builder();
            },
            (acc, item) -> acc.builder.add(mapper.apply(item, acc.index++)),
            (acc1, acc2) -> { /* not supported in parallel */ }
        ).builder.build();
    }
    
    /**
     * Flat maps a collection-valued function
     */
    public static <T, R> Stream<R> flatMapCollection(Stream<T> stream, Function<T, Collection<R>> mapper) {
        return stream.flatMap(t -> streamOf(mapper.apply(t)));
    }
    
    /**
     * Maps and filters in one operation
     */
    public static <T, R> Stream<R> mapAndFilter(Stream<T> stream, Function<T, Optional<R>> mapper) {
        return stream.map(mapper).filter(Optional::isPresent).map(Optional::get);
    }
    
    /**
     * Transforms stream with error handling
     */
    public static <T, R> Stream<R> mapSafely(Stream<T> stream, Function<T, R> mapper) {
        return stream.map(t -> {
            try {
                return Optional.ofNullable(mapper.apply(t));
            } catch (Exception e) {
                return Optional.<R>empty();
            }
        }).filter(Optional::isPresent).map(Optional::get);
    }
    
    /**
     * Zips two streams together
     */
    public static <T, U, R> Stream<R> zip(Stream<T> first, Stream<U> second, BiFunction<T, U, R> zipper) {
        Iterator<T> firstIter = first.iterator();
        Iterator<U> secondIter = second.iterator();
        
        Stream.Builder<R> builder = Stream.builder();
        while (firstIter.hasNext() && secondIter.hasNext()) {
            builder.add(zipper.apply(firstIter.next(), secondIter.next()));
        }
        return builder.build();
    }
    
    /**
     * Interleaves elements from two streams
     */
    public static <T> Stream<T> interleave(Stream<T> first, Stream<T> second) {
        Iterator<T> firstIter = first.iterator();
        Iterator<T> secondIter = second.iterator();
        
        Stream.Builder<T> builder = Stream.builder();
        while (firstIter.hasNext() || secondIter.hasNext()) {
            if (firstIter.hasNext()) builder.add(firstIter.next());
            if (secondIter.hasNext()) builder.add(secondIter.next());
        }
        return builder.build();
    }
    
    // ==================== GROUPING UTILITIES ====================
    
    /**
     * Groups consecutive elements by a classifier
     */
    public static <T, K> Stream<List<T>> groupConsecutive(Stream<T> stream, Function<T, K> classifier) {
        return stream.collect(
            () -> new ArrayList<List<T>>(),
            (lists, item) -> {
                K key = classifier.apply(item);
                if (lists.isEmpty() || !key.equals(classifier.apply(lists.get(lists.size() - 1).get(0)))) {
                    lists.add(new ArrayList<>());
                }
                lists.get(lists.size() - 1).add(item);
            },
            (list1, list2) -> { list1.addAll(list2); return list1; }
        ).stream();
    }
    
    /**
     * Partitions stream into chunks of specified size
     */
    public static <T> Stream<List<T>> chunk(Stream<T> stream, int size) {
        if (size <= 0) throw new IllegalArgumentException("Chunk size must be positive");
        
        Iterator<T> iterator = stream.iterator();
        Stream.Builder<List<T>> builder = Stream.builder();
        
        while (iterator.hasNext()) {
            List<T> chunk = new ArrayList<>();
            for (int i = 0; i < size && iterator.hasNext(); i++) {
                chunk.add(iterator.next());
            }
            if (!chunk.isEmpty()) {
                builder.add(chunk);
            }
        }
        return builder.build();
    }
    
    /**
     * Splits stream at elements matching predicate
     */
    public static <T> Stream<List<T>> split(Stream<T> stream, Predicate<T> splitter) {
        return stream.collect(
            () -> new ArrayList<List<T>>(),
            (lists, item) -> {
                if (splitter.test(item)) {
                    if (!lists.isEmpty() && !lists.get(lists.size() - 1).isEmpty()) {
                        lists.add(new ArrayList<>());
                    }
                } else {
                    if (lists.isEmpty()) {
                        lists.add(new ArrayList<>());
                    }
                    lists.get(lists.size() - 1).add(item);
                }
            },
            (list1, list2) -> { list1.addAll(list2); return list1; }
        ).stream().filter(list -> !list.isEmpty());
    }
    
    // ==================== AGGREGATION UTILITIES ====================
    
    /**
     * Finds elements with minimum value by comparator
     */
    public static <T> Stream<T> minElements(Stream<T> stream, Comparator<T> comparator) {
        return stream.collect(
            () -> new ArrayList<T>(),
            (list, item) -> {
                if (list.isEmpty()) {
                    list.add(item);
                } else {
                    int comparison = comparator.compare(item, list.get(0));
                    if (comparison < 0) {
                        list.clear();
                        list.add(item);
                    } else if (comparison == 0) {
                        list.add(item);
                    }
                }
            },
            (list1, list2) -> {
                if (list1.isEmpty()) return list2;
                if (list2.isEmpty()) return list1;
                
                int comparison = comparator.compare(list1.get(0), list2.get(0));
                if (comparison < 0) return list1;
                if (comparison > 0) return list2;
                
                list1.addAll(list2);
                return list1;
            }
        ).stream();
    }
    
    /**
     * Finds elements with maximum value by comparator
     */
    public static <T> Stream<T> maxElements(Stream<T> stream, Comparator<T> comparator) {
        return minElements(stream, comparator.reversed());
    }
    
    /**
     * Calculates running sum for numeric stream
     */
    public static Stream<Double> runningSum(Stream<? extends Number> stream) {
        return stream.collect(
            () -> new Object() {
                double sum = 0.0;
                List<Double> sums = new ArrayList<>();
            },
            (acc, num) -> {
                acc.sum += num.doubleValue();
                acc.sums.add(acc.sum);
            },
            (acc1, acc2) -> { /* not supported in parallel */ }
        ).sums.stream();
    }
    
    /**
     * Calculates running average for numeric stream
     */
    public static Stream<Double> runningAverage(Stream<? extends Number> stream) {
        return stream.collect(
            () -> new Object() {
                double sum = 0.0;
                int count = 0;
                List<Double> averages = new ArrayList<>();
            },
            (acc, num) -> {
                acc.sum += num.doubleValue();
                acc.count++;
                acc.averages.add(acc.sum / acc.count);
            },
            (acc1, acc2) -> { /* not supported in parallel */ }
        ).averages.stream();
    }
    
    // ==================== ACADEMIC-SPECIFIC UTILITIES ====================
    
    /**
     * Filters students by GPA range
     */
    public static Stream<Student> studentsByGpaRange(Stream<Student> students, double minGpa, double maxGpa) {
        return students.filter(s -> s.getGpa() >= minGpa && s.getGpa() <= maxGpa);
    }
    
    /**
     * Gets top N students by GPA
     */
    public static Stream<Student> topStudentsByGpa(Stream<Student> students, int n) {
        return students.sorted(Comparator.comparing(Student::getGpa).reversed()).limit(n);
    }
    
    /**
     * Groups students by academic year with counts
     */
    public static Map<Integer, Long> studentCountByYear(Stream<Student> students) {
        return students.collect(java.util.stream.Collectors.groupingBy(
            Student::getAcademicYear,
            java.util.stream.Collectors.counting()
        ));
    }
    
    /**
     * Filters available courses
     */
    public static Stream<Course> availableCourses(Stream<Course> courses) {
        return courses.filter(c -> c.getCurrentEnrollment() < c.getMaxEnrollment());
    }
    
    /**
     * Gets courses by department code
     */
    public static Stream<Course> coursesByDepartment(Stream<Course> courses, String departmentCode) {
        return courses.filter(c -> departmentCode.equals(c.getDepartmentCode()));
    }
    
    /**
     * Filters courses by credit range
     */
    public static Stream<Course> coursesByCredits(Stream<Course> courses, int minCredits, int maxCredits) {
        return courses.filter(c -> c.getCredits() >= minCredits && c.getCredits() <= maxCredits);
    }
    
    /**
     * Gets enrollments for a specific semester
     */
    public static Stream<Enrollment> enrollmentsForSemester(Stream<Enrollment> enrollments, Semester semester) {
        return enrollments.filter(e -> semester.equals(e.getSemester()));
    }
    
    /**
     * Filters passing enrollments
     */
    public static Stream<Enrollment> passingEnrollments(Stream<Enrollment> enrollments) {
        return enrollments.filter(e -> e.getFinalGrade() != null && e.getFinalGrade().isPassingGrade());
    }
    
    /**
     * Calculates semester GPA from enrollments
     */
    public static OptionalDouble calculateGpa(Stream<Enrollment> enrollments) {
        List<Enrollment> enrollmentList = enrollments.collect(java.util.stream.Collectors.toList());
        
        double totalPoints = 0.0;
        int totalCredits = 0;
        
        for (Enrollment enrollment : enrollmentList) {
            if (enrollment.getFinalGrade() != null && enrollment.getFinalGrade().affectsGpa()) {
                Course course = enrollment.getCourse();
                if (course != null) {
                    Double points = enrollment.getFinalGrade().getGpaPoints();
                    if (points != null) {
                        totalPoints += points * course.getCredits();
                        totalCredits += course.getCredits();
                    }
                }
            }
        }
        
        return totalCredits > 0 ? OptionalDouble.of(totalPoints / totalCredits) : OptionalDouble.empty();
    }
    
    // ==================== UTILITY OPERATIONS ====================
    
    /**
     * Executes an action for each element and returns the original stream
     */
    public static <T> Stream<T> peek(Stream<T> stream, Consumer<T> action) {
        return stream.peek(action);
    }
    
    /**
     * Applies side effect and returns stream unchanged
     */
    public static <T> Stream<T> sideEffect(Stream<T> stream, Runnable action) {
        return stream.peek(t -> action.run());
    }
    
    /**
     * Executes action on stream termination
     */
    public static <T> Stream<T> onClose(Stream<T> stream, Runnable action) {
        return stream.onClose(action);
    }
    
    /**
     * Converts stream to parallel if size exceeds threshold
     */
    public static <T> Stream<T> parallelIfLarge(Stream<T> stream, int threshold) {
        long count = stream.count();
        stream = stream.skip(0); // Reset stream
        return count > threshold ? stream.parallel() : stream;
    }
    
    /**
     * Ensures stream is sequential
     */
    public static <T> Stream<T> sequential(Stream<T> stream) {
        return stream.sequential();
    }
    
    /**
     * Ensures stream is parallel
     */
    public static <T> Stream<T> parallel(Stream<T> stream) {
        return stream.parallel();
    }
    
    /**
     * Converts to list safely
     */
    public static <T> List<T> toList(Stream<T> stream) {
        return stream.collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Converts to set safely
     */
    public static <T> Set<T> toSet(Stream<T> stream) {
        return stream.collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Converts to array safely
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Stream<T> stream, Class<T> clazz) {
        return stream.toArray(size -> (T[]) java.lang.reflect.Array.newInstance(clazz, size));
    }
    
    /**
     * Checks if stream is empty
     */
    public static <T> boolean isEmpty(Stream<T> stream) {
        return !stream.findAny().isPresent();
    }
    
    /**
     * Checks if stream is not empty
     */
    public static <T> boolean isNotEmpty(Stream<T> stream) {
        return stream.findAny().isPresent();
    }
    
    /**
     * Gets size of stream
     */
    public static <T> long size(Stream<T> stream) {
        return stream.count();
    }
    
    /**
     * Gets first element or default
     */
    public static <T> T firstOrDefault(Stream<T> stream, T defaultValue) {
        return stream.findFirst().orElse(defaultValue);
    }
    
    /**
     * Gets last element or default
     */
    public static <T> T lastOrDefault(Stream<T> stream, T defaultValue) {
        return stream.reduce((first, second) -> second).orElse(defaultValue);
    }
    
    /**
     * Gets single element or throws exception
     */
    public static <T> T single(Stream<T> stream) {
        List<T> list = stream.limit(2).collect(java.util.stream.Collectors.toList());
        if (list.isEmpty()) {
            throw new NoSuchElementException("Stream is empty");
        }
        if (list.size() > 1) {
            throw new IllegalStateException("Stream contains more than one element");
        }
        return list.get(0);
    }
    
    /**
     * Gets single element or default
     */
    public static <T> T singleOrDefault(Stream<T> stream, T defaultValue) {
        try {
            return single(stream);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}