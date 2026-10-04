package functional;

import models.Course;
import models.Enrollment;
import models.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static functional.StreamUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class StreamUtilsTest {

    @Test
    void streamOfHandlesNulls() {
        assertThat(StreamUtils.streamOf((Collection<String>) null)).isEmpty();
        assertThat(StreamUtils.streamOf((String[]) null)).isEmpty();
        assertThat(StreamUtils.streamOf(Optional.empty())).isEmpty();
        assertThat(StreamUtils.streamOf(Optional.of("x"))).containsExactly("x");
        assertThat(StreamUtils.streamOf(List.of(1, 2))).containsExactly(1, 2);
        assertThat(StreamUtils.streamOf("a", "b")).containsExactly("a", "b");
    }

    @Test
    void filteringHelpers() {
        assertThat(nonNull(Stream.of("a", null, "b"))).containsExactly("a", "b");
        assertThat(Stream.of("apple", "avocado", "banana", "blueberry", "cherry").filter(distinctBy(s -> s.charAt(0))))
                .containsExactly("apple", "banana", "cherry");
        assertThat(Stream.of(1, 2, 3, 4, 5, 6).filter(anyMatch(i -> i == 1, i -> i > 5))).containsExactly(1, 6);
        assertThat(Stream.of(1, 2, 3, 4, 5, 6).filter(allMatch(i -> i % 2 == 0, i -> i > 2))).containsExactly(4, 6);
        assertThat(Stream.of(1, 2, 3).filter(StreamUtils.<Integer>anyMatch())).isEmpty();
        assertThat(Stream.of(1, 2, 3).filter(StreamUtils.<Integer>allMatch())).containsExactly(1, 2, 3);
    }

    @ParameterizedTest(name = "slice({0},{1})")
    @CsvSource({"0, 3, 'a,b,c'", "2, 4, 'c,d'", "3, 10, 'd,e'", "5, 6, ''", "1, 1, ''"})
    void slice(int start, int end, String expected) {
        List<String> result = toList(StreamUtils.slice(Stream.of("a", "b", "c", "d", "e"), start, end));
        assertThat(String.join(",", result)).isEqualTo(expected);
    }

    @Test
    void takeDropAndWhile() {
        assertThat(take(Stream.of(1, 2, 3), 2)).containsExactly(1, 2);
        assertThat(drop(Stream.of(1, 2, 3), 2)).containsExactly(3);
        assertThat(StreamUtils.takeWhile(Stream.of(1, 2, 5, 1), i -> i < 3)).containsExactly(1, 2);
        assertThat(StreamUtils.dropWhile(Stream.of(1, 2, 5, 1), i -> i < 3)).containsExactly(5, 1);
    }

    @Test
    void mappingHelpers() {
        assertThat(mapWithIndex(Stream.of("a", "b", "c"), (s, i) -> i + s)).containsExactly("0a", "1b", "2c");
        assertThat(flatMapCollection(Stream.of(List.of(1, 2), List.<Integer>of(), List.of(3)), l -> l))
                .containsExactly(1, 2, 3);
        assertThat(mapAndFilter(Stream.of("1", "x", "3"), s -> s.matches("\\d") ? Optional.of(Integer.parseInt(s)) : Optional.empty()))
                .containsExactly(1, 3);
        assertThat(mapSafely(Stream.of("1", "x", "3"), Integer::parseInt)).containsExactly(1, 3);
        assertThat(mapSafely(Stream.of("a", "b"), s -> s.equals("a") ? null : s)).containsExactly("b");
    }

    @Test
    void zipStopsAtShorterStream() {
        assertThat(zip(Stream.of(1, 2, 3), Stream.of("a", "b"), (i, s) -> i + s)).containsExactly("1a", "2b");
        assertThat(zip(Stream.<Integer>empty(), Stream.of("a"), (i, s) -> i + s)).isEmpty();
    }

    @Test
    void interleaveAlternatesThenDrainsLonger() {
        assertThat(interleave(Stream.of(1, 3, 5, 7), Stream.of(2, 4))).containsExactly(1, 2, 3, 4, 5, 7);
        assertThat(interleave(Stream.of(1), Stream.of(2, 4, 6))).containsExactly(1, 2, 4, 6);
    }

    @Test
    void groupConsecutive() {
        assertThat(StreamUtils.groupConsecutive(Stream.of(1, 1, 2, 2, 2, 1, 3), i -> i))
                .containsExactly(List.of(1, 1), List.of(2, 2, 2), List.of(1), List.of(3));
        assertThat(StreamUtils.groupConsecutive(Stream.of("ant", "ape", "bee", "cat", "cow"), s -> s.charAt(0)))
                .containsExactly(List.of("ant", "ape"), List.of("bee"), List.of("cat", "cow"));
        assertThat(StreamUtils.groupConsecutive(Stream.<Integer>empty(), i -> i)).isEmpty();
    }

    @Test
    void chunk() {
        assertThat(StreamUtils.chunk(Stream.of(1, 2, 3, 4, 5), 2)).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
        assertThat(StreamUtils.chunk(Stream.of(1, 2), 5)).containsExactly(List.of(1, 2));
        assertThat(StreamUtils.chunk(Stream.empty(), 3)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void chunkRejectsNonPositiveSize(int size) {
        assertThatThrownBy(() -> StreamUtils.chunk(Stream.of(1), size)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitDropsSeparatorsAndEmptyGroups() {
        assertThat(split(Stream.of(0, 1, 2, 0, 0, 3, 0), i -> i == 0)).containsExactly(List.of(1, 2), List.of(3));
        assertThat(split(Stream.of(1, 2), i -> i == 0)).containsExactly(List.of(1, 2));
        assertThat(split(Stream.of(0, 0), i -> i == 0)).isEmpty();
    }

    @Test
    void minAndMaxElementsKeepAllTies() {
        assertThat(minElements(Stream.of("bb", "a", "cc", "d"), Comparator.comparingInt(String::length)))
                .containsExactly("a", "d");
        assertThat(maxElements(Stream.of("bb", "a", "cc", "d"), Comparator.comparingInt(String::length)))
                .containsExactly("bb", "cc");
        assertThat(minElements(Stream.<String>empty(), Comparator.naturalOrder())).isEmpty();
    }

    @Test
    void runningAggregates() {
        assertThat(runningSum(Stream.of(1, 2, 3.5))).containsExactly(1.0, 3.0, 6.5);
        assertThat(runningAverage(Stream.of(2, 4, 9))).containsExactly(2.0, 3.0, 5.0);
        assertThat(runningSum(Stream.<Integer>empty())).isEmpty();
    }

    @Test
    void terminalHelpers() {
        assertThat(StreamUtils.isEmpty(Stream.empty())).isTrue();
        assertThat(isNotEmpty(Stream.of(1))).isTrue();
        assertThat(size(Stream.of(1, 2, 3))).isEqualTo(3);
        assertThat(firstOrDefault(Stream.<Integer>empty(), 9)).isEqualTo(9);
        assertThat(lastOrDefault(Stream.of(1, 2, 3), 9)).isEqualTo(3);
        assertThat(lastOrDefault(Stream.<Integer>empty(), 9)).isEqualTo(9);
        assertThat(toSet(Stream.of(1, 1, 2))).containsExactlyInAnyOrder(1, 2);
        assertThat(toArray(Stream.of("a", "b"), String.class)).isInstanceOf(String[].class).containsExactly("a", "b");
    }

    @Test
    void single() {
        assertThat(StreamUtils.single(Stream.of("only"))).isEqualTo("only");
        assertThatThrownBy(() -> StreamUtils.single(Stream.empty())).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> StreamUtils.single(Stream.of(1, 2))).isInstanceOf(IllegalStateException.class);
        assertThat(singleOrDefault(Stream.of(1, 2), 0)).isZero();
        assertThat(singleOrDefault(Stream.of(7), 0)).isEqualTo(7);
    }

    @Test
    void sideEffectHelpers() {
        AtomicInteger seen = new AtomicInteger();
        AtomicInteger ticks = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();

        List<Integer> result = toList(sideEffect(StreamUtils.peek(Stream.of(1, 2, 3), seen::addAndGet), ticks::incrementAndGet));
        try (Stream<Integer> s = onClose(Stream.of(1), closed::incrementAndGet)) {
            s.count();
        }

        assertThat(result).containsExactly(1, 2, 3);
        assertThat(seen).hasValue(6);
        assertThat(ticks).hasValue(3);
        assertThat(closed).hasValue(1);
        assertThat(sequential(Stream.of(1).parallel()).isParallel()).isFalse();
        assertThat(StreamUtils.parallel(Stream.of(1)).isParallel()).isTrue();
    }

    @Test
    void parallelIfLargeKeepsAllElements() {
        // Regression: the input stream was consumed by count() and then reused, throwing IllegalStateException.
        Stream<Integer> small = parallelIfLarge(Stream.of(1, 2, 3), 10);
        assertThat(small.isParallel()).isFalse();
        assertThat(small).containsExactly(1, 2, 3);

        Stream<Integer> large = parallelIfLarge(Stream.iterate(0, i -> i + 1).limit(100), 10);
        assertThat(large.isParallel()).isTrue();
        assertThat(large.mapToInt(Integer::intValue).sum()).isEqualTo(4950);
    }

    @Test
    void ranges() {
        assertThat(range(1, 4).boxed()).containsExactly(1, 2, 3);
        assertThat(rangeClosed(1, 4).boxed()).containsExactly(1, 2, 3, 4);
        assertThat(generate(() -> "x").limit(2)).containsExactly("x", "x");
    }

    // ==================== academic helpers ====================

    private static Student student(String id, double gpa, Student.AcademicYear year) {
        Student s = new Student("U" + id, "F" + id, "L" + id, id + "@u.edu", null, id, "CS", year);
        s.setGpa(gpa);
        return s;
    }

    @Test
    void studentHelpers() {
        Student a = student("a", 3.9, Student.AcademicYear.SENIOR);
        Student b = student("b", 2.5, Student.AcademicYear.FRESHMAN);
        Student c = student("c", 3.2, Student.AcademicYear.FRESHMAN);

        assertThat(studentsByGpaRange(Stream.of(a, b, c), 2.5, 3.2)).containsExactly(b, c);
        assertThat(topStudentsByGpa(Stream.of(b, a, c), 2)).containsExactly(a, c);
        assertThat(studentCountByYear(Stream.of(a, b, c))).isEqualTo(Map.of(4, 1L, 1, 2L));
    }

    @Test
    void courseHelpers() {
        Course open = new Course("C1", "CS101", "Intro", "", 3, "D-CS");
        Course full = new Course("C2", "CS102", "Next", "", 4, "D-CS");
        Course other = new Course("C3", "MA101", "Calc", "", 1, "D-MA");
        full.setMaxEnrollment(1);
        full.setStatus(Course.CourseStatus.OPEN);
        full.enrollStudent("S1");

        assertThat(availableCourses(Stream.of(open, full, other))).containsExactly(open, other);
        assertThat(coursesByDepartment(Stream.of(open, full, other), "D-CS")).containsExactly(open, full);
        assertThat(coursesByCredits(Stream.of(open, full, other), 3, 4)).containsExactly(open, full);
    }

    private static Enrollment enrollment(String semester, Enrollment.Grade grade, int credits) {
        Enrollment e = new Enrollment("E", "S", "C", semester, 2024);
        e.setGrade(grade);
        e.setCreditHours(credits);
        return e;
    }

    @Test
    void enrollmentHelpers() {
        Enrollment a = enrollment("Fall", Enrollment.Grade.A, 4);
        Enrollment c = enrollment("fall", Enrollment.Grade.C, 2);
        Enrollment f = enrollment("Spring", Enrollment.Grade.F, 3);
        Enrollment pass = enrollment("Spring", Enrollment.Grade.PASS, 3);
        Enrollment ungraded = enrollment("Spring", null, 3);

        assertThat(enrollmentsForSemester(Stream.of(a, c, f), "FALL")).containsExactly(a, c);
        assertThat(passingEnrollments(Stream.of(a, c, f, pass, ungraded))).containsExactly(a, c, pass);
        // (4*4.0 + 2*2.0 + 3*0.0) / 9; pass/fail and ungraded enrollments are excluded
        assertThat(calculateGpa(Stream.of(a, c, f, pass, ungraded)).getAsDouble()).isCloseTo(20.0 / 9, within(1e-9));
        assertThat(calculateGpa(Stream.of(pass, ungraded))).isEmpty();
    }
}
