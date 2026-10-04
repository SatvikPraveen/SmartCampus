package functional;

import models.Admin;
import models.Course;
import models.Department;
import models.Enrollment;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static functional.Functions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FunctionsTest {

    private static Student student(Student.AcademicYear year, double gpa) {
        Student s = new Student("U1", "ada", "Lovelace", "Ada@Uni.edu", null, "S1", "Math", year);
        s.setGpa(gpa);
        return s;
    }

    @Test
    void userFunctions() {
        Student s = student(Student.AcademicYear.JUNIOR, 3.0);

        assertThat(GET_FULL_NAME.apply(s)).isEqualTo("ada Lovelace");
        assertThat(GET_FORMATTED_NAME.apply(s)).isEqualTo("Lovelace, ada");
        assertThat(GET_INITIALS.apply(s)).isEqualTo("AL");
        assertThat(GET_EMAIL.apply(s)).isEqualTo("ada@uni.edu");
        assertThat(GET_EMAIL_DOMAIN.apply(s)).isEqualTo("uni.edu");
        assertThat(GET_USER_ROLE.apply(s)).isEqualTo("STUDENT");
        assertThat(GET_USER_TYPE.apply(s)).isEqualTo("Student");
        assertThat(GET_USER_TYPE.apply(new Admin())).isEqualTo("Admin");
        assertThat(GET_USER_TYPE.apply(null)).isEqualTo("Unknown");
        assertThat(GET_CREATED_DATE.apply(s)).isEqualTo(s.getCreatedAt().toLocalDate());
        assertThat(GET_DAYS_SINCE_CREATION.apply(s)).isZero();
        assertThat(GET_DISPLAY_NAME.apply(s)).isEqualTo("ada Lovelace");
    }

    @Test
    void userFunctionsAreNullSafe() {
        assertThat(GET_FULL_NAME.apply(null)).isEmpty();
        assertThat(GET_INITIALS.apply(null)).isEmpty();
        assertThat(GET_EMAIL_DOMAIN.apply(null)).isEmpty();
        assertThat(GET_DISPLAY_NAME.apply(null)).isEqualTo("Unknown User");
        assertThat(GET_DAYS_SINCE_CREATION.apply(null)).isZero();
        assertThat(GET_GPA.apply(null)).isZero();
        assertThat(GET_CLASSIFICATION.apply(null)).isEqualTo("Unknown");
        assertThat(GET_COURSE_IDS.apply(null)).isEmpty();
        assertThat(GET_TEACHING_LOAD.apply(null)).isZero();
        assertThat(GET_COURSE_LEVEL.apply(null)).isZero();
        assertThat(GET_BUDGET.apply(null)).isZero();
        assertThat(GET_GRADE_LEVEL.apply(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({"FRESHMAN, Freshman", "SOPHOMORE, Sophomore", "JUNIOR, Junior", "SENIOR, Senior", "GRADUATE, Graduate"})
    void classification(Student.AcademicYear year, String expected) {
        assertThat(GET_CLASSIFICATION.apply(student(year, 3.0))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"3.5, Good Standing", "2.0, Satisfactory", "3.49, Satisfactory", "1.99, Academic Probation"})
    void academicStatus(double gpa, String expected) {
        assertThat(GET_ACADEMIC_STATUS.apply(student(Student.AcademicYear.SENIOR, gpa))).isEqualTo(expected);
    }

    @Test
    void studentFunctions() {
        Student s = student(Student.AcademicYear.SENIOR, 3.7);
        s.enrollInCourse("C1");

        assertThat(GET_GPA.apply(s)).isEqualTo(3.7);
        assertThat(GET_MAJOR.apply(s)).isEqualTo("Math");
        assertThat(GET_ACADEMIC_YEAR.apply(s)).isEqualTo(4);
        List<String> ids = GET_COURSE_IDS.apply(s);
        assertThat(ids).containsExactly("C1");
        ids.clear();
        assertThat(s.getEnrolledCourseIds()).containsExactly("C1");
    }

    @ParameterizedTest
    @CsvSource({"ASSISTANT, false, Tenure Track", "ASSOCIATE, false, Tenure Track", "FULL, false, Other",
            "ADJUNCT, false, Adjunct", "EMERITUS, false, Emeritus", "FULL, true, Tenured"})
    void employmentStatus(Professor.AcademicRank rank, boolean tenured, String expected) {
        Professor p = new Professor("U2", "Alan", "Turing", "a@u.edu", null, "P1", "D1", rank, "AI");
        p.setTenured(tenured);

        assertThat(GET_EMPLOYMENT_STATUS.apply(p)).isEqualTo(expected);
    }

    @Test
    void professorFunctions() {
        Professor p = new Professor("U2", "Alan", "Turing", "a@u.edu", null, "P1", "D1",
                Professor.AcademicRank.FULL, "AI");
        p.setHireDate(LocalDate.now().minusYears(7).minusDays(1));
        p.setOfficeLocation("Hall 1");

        assertThat(GET_RANK.apply(p)).isEqualTo(Professor.AcademicRank.FULL.getTitle());
        assertThat(GET_PROFESSOR_DEPARTMENT_ID.apply(p)).isEqualTo("D1");
        assertThat(GET_OFFICE_LOCATION.apply(p)).isEqualTo("Hall 1");
        assertThat(GET_YEARS_OF_SERVICE.apply(p)).isEqualTo(7);
        assertThat(GET_TEACHING_LOAD.apply(p)).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"CS101, 101, 1", "CS 450, 450, 4", "MATH-5010, 5010, 5", "ABC, '', 0"})
    void courseNumberAndLevel(String code, String number, int level) {
        Course c = new Course("C1", code, "Name", "", 3, "D1");

        assertThat(GET_COURSE_NUMBER.apply(c)).isEqualTo(number);
        assertThat(GET_COURSE_LEVEL.apply(c)).isEqualTo(level);
    }

    @Test
    void courseFunctions() {
        Course c = new Course("C1", "CS101", "Intro", "Desc", 4, "D1");
        c.setMaxEnrollment(4);
        c.setStatus(Course.CourseStatus.OPEN);
        c.enrollStudent("S1");
        c.setSchedule(List.of("Mon", "Wed"), LocalTime.of(9, 0), LocalTime.of(10, 15));
        c.setBuilding("Hall");
        c.setClassroom("101");

        assertThat(GET_COURSE_CODE.apply(c)).isEqualTo("CS101");
        assertThat(GET_CREDITS.apply(c)).isEqualTo(4);
        assertThat(GET_CURRENT_ENROLLMENT.apply(c)).isEqualTo(1);
        assertThat(GET_AVAILABLE_SPOTS.apply(c)).isEqualTo(3);
        assertThat(GET_ENROLLMENT_PERCENTAGE.apply(c)).isEqualTo(25.0);
        assertThat(GET_SCHEDULE.apply(c)).isEqualTo("Mon, Wed 09:00-10:15");
        assertThat(GET_COURSE_LOCATION.apply(c)).isEqualTo("Hall 101");
        assertThat(GET_STATUS.apply(c)).isEqualTo(Course.CourseStatus.OPEN);
        assertThat(GET_SCHEDULE.apply(new Course())).isEmpty();
        assertThat(GET_COURSE_LOCATION.apply(new Course())).isEmpty();
    }

    @Test
    void enrollmentFunctions() {
        Enrollment e = new Enrollment("E1", "S1", "C1", "Fall", 2024);
        e.setEnrollmentDate(LocalDateTime.of(2024, 8, 20, 9, 0));
        e.setGrade(Enrollment.Grade.B_PLUS);

        assertThat(GET_STUDENT_ID.apply(e)).isEqualTo("S1");
        assertThat(GET_ENROLLMENT_DATE.apply(e)).isEqualTo(LocalDate.of(2024, 8, 20));
        assertThat(GET_GRADE_POINTS.apply(e)).isEqualTo(3.3);
        assertThat(GET_GRADE_POINTS.apply(null)).isZero();
        assertThat(GET_FINAL_GRADE.apply(e)).isEqualTo(Enrollment.Grade.B_PLUS);
        assertThat(GET_ENROLLMENT_DURATION_DAYS.apply(null)).isZero();
        assertThat(GET_ENROLLMENT_DURATION_DAYS.apply(e)).isPositive();
    }

    @Test
    void departmentFunctions() {
        Department d = new Department("D1", "cs", "Computer Science", "desc", "Hall", "P1", new BigDecimal("1234.5"));
        d.addProfessor("P1");

        assertThat(GET_DEPARTMENT_CODE.apply(d)).isEqualTo("CS");
        assertThat(GET_BUDGET.apply(d)).isEqualTo(1234.5);
        assertThat(GET_FACULTY_COUNT.apply(d)).isEqualTo(1);
        assertThat(GET_DEPARTMENT_HEAD_ID.apply(d)).isEqualTo("P1");
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(value = {"hello world|Hello World", "hELLO|Hello", "'  leading and trailing  '|Leading And Trailing",
            "multiple   spaces|Multiple Spaces", "'   '|''", "''|''", "a|A"}, delimiter = '|')
    void titleCase(String input, String expected) {
        // Regression: leading whitespace produced an empty first "word" and threw StringIndexOutOfBoundsException.
        assertThat(TO_TITLE_CASE.apply(input)).isEqualTo(expected);
    }

    @Test
    void stringFunctions() {
        assertThat(TO_TITLE_CASE.apply(null)).isEmpty();
        assertThat(TO_UPPER_CASE.apply(null)).isEmpty();
        assertThat(TO_LOWER_CASE.apply("AbC")).isEqualTo("abc");
        assertThat(TRIM.apply("  x ")).isEqualTo("x");
        assertThat(STRING_LENGTH.apply(null)).isZero();
        assertThat(REVERSE_STRING.apply("abc")).isEqualTo("cba");
        assertThat(truncate(3).apply("abcdef")).isEqualTo("abc...");
        assertThat(truncate(6).apply("abcdef")).isEqualTo("abcdef");
        assertThat(padLeft(5, '0').apply("42")).isEqualTo("00042");
        assertThat(padLeft(2, '0').apply("123")).isEqualTo("123");
        assertThat(padRight(4, '.').apply(null)).isEqualTo("....");
    }

    @Test
    void dateAndNumberFunctions() {
        LocalDate d = LocalDate.of(2024, 2, 29);

        assertThat(FORMAT_DATE.apply(d)).isEqualTo("2024-02-29");
        assertThat(FORMAT_DATE.apply(null)).isEmpty();
        assertThat(formatDate("dd/MM/yyyy").apply(d)).isEqualTo("29/02/2024");
        assertThat(FORMAT_DATETIME.apply(d.atTime(13, 5))).isEqualTo("2024-02-29T13:05:00");
        assertThat(formatDateTime("HH:mm").apply(d.atTime(13, 5))).isEqualTo("13:05");
        assertThat(GET_YEAR.apply(d)).isEqualTo(2024);
        assertThat(GET_MONTH.apply(d)).isEqualTo(2);
        assertThat(GET_DAY.apply(d)).isEqualTo(29);
        assertThat(GET_DAY_OF_WEEK.apply(d)).isEqualTo("THURSDAY");
        assertThat(TO_DOUBLE.apply(3)).isEqualTo(3.0);
        assertThat(TO_INTEGER.apply(3.9)).isEqualTo(3);
        assertThat(TO_LONG.apply(null)).isZero();
        assertThat(TO_STRING.apply(null)).isEqualTo("0");
        assertThat(formatDecimal(2).apply(3.14159)).isEqualTo(String.format("%.2f", 3.14159));
        assertThat(TO_PERCENTAGE.apply(12.34)).isEqualTo(String.format("%.1f%%", 12.34));
        assertThat(multiply(2.5).apply(4).doubleValue()).isEqualTo(10.0);
        assertThat(add(1.5).apply(null).doubleValue()).isEqualTo(1.5);
    }

    @Test
    void collectionFunctions() {
        List<Integer> nums = List.of(3, 1, 2);

        assertThat(COLLECTION_SIZE.apply(nums)).isEqualTo(3);
        assertThat(IS_EMPTY.apply(null)).isTrue();
        assertThat(Functions.<Integer>toList().apply(Set.of(1))).containsExactly(1);
        assertThat(Functions.<Integer>toSet().apply(List.of(1, 1))).containsExactly(1);
        assertThat(Functions.<Integer>getFirst().apply(nums)).isEqualTo(3);
        assertThat(Functions.<Integer>getLast().apply(nums)).isEqualTo(2);
        assertThat(Functions.<Integer>getLast().apply(List.of())).isNull();
        assertThat(Functions.<Integer>reverse().apply(nums)).containsExactly(2, 1, 3);
        assertThat(nums).containsExactly(3, 1, 2);
    }

    @Test
    void compositionHelpers() {
        Function<String, Integer> length = String::length;

        assertThat(compose(length, i -> i * 2).apply("abc")).isEqualTo(6);
        assertThat(Functions.<String>identity().apply("x")).isEqualTo("x");
        assertThat(Functions.<String, Integer>constant(7).apply("anything")).isEqualTo(7);
        assertThat(Functions.<String, Integer>safe(Integer::parseInt).apply("12")).contains(12);
        assertThat(Functions.<String, Integer>safe(Integer::parseInt).apply("x")).isEqualTo(Optional.empty());
        assertThat(map(List.of("a", "bb"), length)).containsExactly(1, 2);
        assertThat(mapToSet(List.of("a", "b"), length)).containsExactly(1);
        assertThat(groupBy(List.of("a", "bb", "c"), length)).isEqualTo(Map.of(1, List.of("a", "c"), 2, List.of("bb")));
        assertThat(toMap(List.of("a", "bb"), s -> s, length)).isEqualTo(Map.of("a", 1, "bb", 2));
        assertThatThrownBy(() -> toMap(List.of("a", "a"), s -> s, length)).isInstanceOf(IllegalStateException.class);
    }
}
