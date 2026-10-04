package functional;

import enums.GradeLevel;
import models.Admin;
import models.Course;
import models.Department;
import models.Enrollment;
import models.Professor;
import models.Student;
import models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static functional.Predicates.*;
import static org.assertj.core.api.Assertions.assertThat;

class PredicatesTest {

    private static Student student(double gpa, Student.AcademicYear year) {
        Student s = new Student("U1", "Ada", "Lovelace", "ada@uni.edu", "555-0100", "S1", "Computer Science", year);
        s.setGpa(gpa);
        return s;
    }

    private static Course course(String code) {
        return new Course("C-" + code, code, "Intro to Things", "Covers Java and more", 3, "D-CS");
    }

    @Test
    void userTypeAndStatus() {
        Student s = student(3.0, Student.AcademicYear.JUNIOR);
        Professor p = new Professor("U2", "Alan", "Turing", "alan@uni.edu", null, "P1", "D-CS",
                Professor.AcademicRank.FULL, "AI");
        Admin a = new Admin("U3", "Grace", "Hopper", "grace@uni.edu", null, "A1", Admin.AdminLevel.SYSTEM_ADMIN, null, "Ops");

        assertThat(IS_STUDENT.test(s)).isTrue();
        assertThat(IS_PROFESSOR.test(s)).isFalse();
        assertThat(IS_PROFESSOR.test(p)).isTrue();
        assertThat(IS_ADMIN.test(a)).isTrue();
        assertThat(IS_ACTIVE.test(s)).isTrue();
        assertThat(IS_INACTIVE.test(s)).isFalse();
        s.setActive(false);
        assertThat(IS_ACTIVE.test(s)).isFalse();
        assertThat(IS_INACTIVE.test(s)).isTrue();
        assertThat(HAS_PHONE.test(s)).isTrue();
        assertThat(HAS_PHONE.test(p)).isFalse();
        assertThat(HAS_EMAIL.test(p)).isTrue();
    }

    @Test
    void negatedPredicatesDoNotMatchNull() {
        // Regression: predicates built with negate() turned the null guard of the original into a match,
        // e.g. a null course "had availability".
        assertThat(IS_INACTIVE.test(null)).isFalse();
        assertThat(HAS_AVAILABILITY.test(null)).isFalse();
        assertThat(NO_PREREQUISITES.test(null)).isFalse();
        assertThat(IS_ACTIVE.test(null)).isFalse();
        assertThat(IS_FULL.test(null)).isFalse();
    }

    @Test
    void userAttributeMatchers() {
        Student s = student(3.0, Student.AcademicYear.JUNIOR);

        assertThat(hasRole(" student ").test(s)).isTrue();
        assertThat(hasRole("PROFESSOR").test(s)).isFalse();
        assertThat(hasRole(null).test(s)).isFalse();
        assertThat(nameContains("LOVE").test(s)).isTrue();
        assertThat(nameContains("ada ").test(s)).isTrue();
        assertThat(nameContains("bob").test(s)).isFalse();
        assertThat(emailDomain("UNI.edu").test(s)).isTrue();
        assertThat(emailDomain("ni.edu").test(s)).isFalse();
        LocalDate today = s.getCreatedAt().toLocalDate();
        assertThat(createdAfter(today.minusDays(1)).test(s)).isTrue();
        assertThat(createdAfter(today).test(s)).isFalse();
        assertThat(createdBefore(today.plusDays(1)).test(s)).isTrue();
    }

    @Test
    void studentPredicates() {
        Student junior = student(3.6, Student.AcademicYear.JUNIOR);
        Student grad = student(1.5, Student.AcademicYear.GRADUATE);
        grad.setDepartmentId("D-CS");

        assertThat(IS_UNDERGRADUATE.test(junior)).isTrue();
        assertThat(IS_GRADUATE.test(grad)).isTrue();
        assertThat(IS_UNDERGRADUATE.test(grad)).isFalse();
        assertThat(hasGpaAbove(3.6).test(junior)).isTrue();
        assertThat(hasGpaBelow(1.5).test(grad)).isTrue();
        assertThat(hasGpaBetween(1.0, 3.0).test(junior)).isFalse();
        assertThat(inMajor(" computer science").test(junior)).isTrue();
        assertThat(inYear(3).test(junior)).isTrue();
        assertThat(inDepartment("d-cs").test(grad)).isTrue();
        assertThat(inDepartment("d-cs").test(junior)).isFalse();
        assertThat(onAcademicProbation().test(grad)).isTrue();
        assertThat(onDeansListEligible().test(junior)).isTrue();
        assertThat(IS_ENROLLED.test(junior)).isFalse();
        junior.enrollInCourse("C1");
        assertThat(IS_ENROLLED.test(junior)).isTrue();
        assertThat(enrolledInCourse("C1").test(junior)).isTrue();
        assertThat(enrolledInCourse("C2").test(junior)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"ADJUNCT, false, true", "ASSISTANT, true, false", "ASSOCIATE, true, false",
            "FULL, true, false", "EMERITUS, false, false"})
    void professorRankPredicates(Professor.AcademicRank rank, boolean tenureTrack, boolean adjunct) {
        Professor p = new Professor("U2", "Alan", "Turing", "alan@uni.edu", null, "P1", "D-CS", rank, "AI");

        assertThat(IS_TENURE_TRACK.test(p)).isEqualTo(tenureTrack);
        assertThat(IS_ADJUNCT.test(p)).isEqualTo(adjunct);
        assertThat(IS_EMERITUS.test(p)).isEqualTo(rank == Professor.AcademicRank.EMERITUS);
        assertThat(hasRank(rank.name().toLowerCase()).test(p)).isTrue();
        assertThat(hasRank(rank.getTitle()).test(p)).isTrue();
    }

    @Test
    void professorAttributePredicates() {
        Professor p = new Professor("U2", "Alan", "Turing", "alan@uni.edu", null, "P1", "D-CS",
                Professor.AcademicRank.FULL, "AI");
        p.setResearchArea("Machine Learning");
        p.setHireDate(LocalDate.of(2010, 1, 1));
        Department cs = new Department("D-CS", "CS", "Computer Science", "", "A");

        assertThat(Predicates.inDepartment(cs).test(p)).isTrue();
        assertThat(Predicates.inDepartment((Department) null).test(p)).isFalse();
        assertThat(hasResearchInterest("LEARN").test(p)).isTrue();
        assertThat(hiredAfter(LocalDate.of(2009, 12, 31)).test(p)).isTrue();
        assertThat(hiredAfter(LocalDate.of(2010, 1, 1)).test(p)).isFalse();
        assertThat(teachingCourse("C1").test(p)).isFalse();
    }

    @Test
    void coursePredicates() {
        Course c = course("CS101");
        c.setMaxEnrollment(1);
        c.setSemester("Fall 2024");
        c.setStartTime(LocalTime.of(9, 0));
        c.setEndTime(LocalTime.of(10, 15));

        assertThat(IS_FULL.test(c)).isFalse();
        assertThat(HAS_AVAILABILITY.test(c)).isTrue();
        assertThat(NO_PREREQUISITES.test(c)).isTrue();
        c.addPrerequisite("C0");
        assertThat(HAS_PREREQUISITES.test(c)).isTrue();
        c.setStatus(Course.CourseStatus.OPEN);
        c.enrollStudent("S1");
        assertThat(IS_FULL.test(c)).isTrue();
        assertThat(HAS_AVAILABILITY.test(c)).isFalse();
        assertThat(hasEnrollmentAbove(1).test(c)).isTrue();
        assertThat(hasEnrollmentBelow(0).test(c)).isFalse();
        assertThat(enrollmentBetween(1, 1).test(c)).isTrue();
        assertThat(courseInDepartment(" d-cs ").test(c)).isTrue();
        assertThat(hasCredits(3).test(c)).isTrue();
        assertThat(creditsInRange(4, 6).test(c)).isFalse();
        assertThat(taughtBy("P1").test(c)).isFalse();
        assertThat(scheduledFor("fall 2024").test(c)).isTrue();
        assertThat(titleContains("THINGS").test(c)).isTrue();
        assertThat(titleContains(null).test(c)).isTrue();
        assertThat(descriptionContains("java").test(c)).isTrue();
        c.cancelCourse();
        assertThat(IS_CANCELLED.test(c)).isTrue();
    }

    @ParameterizedTest(name = "{0} at {1} -> {2}")
    @CsvSource({"09:00, true", "10:14, true", "10:15, false", "08:59, false"})
    void meetsDuringTimeIsHalfOpen(LocalTime time, boolean expected) {
        Course c = course("CS101");
        c.setStartTime(LocalTime.of(9, 0));
        c.setEndTime(LocalTime.of(10, 15));

        assertThat(meetsDuringTime(time).test(c)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"CS101, 1, true, false", "CS 450, 4, true, false", "MATH-501, 5, false, true",
            "EE9000, 9, false, true", "BIO99, 9, false, false"})
    void courseLevels(String code, int level, boolean undergraduate, boolean graduate) {
        Course c = course(code);

        assertThat(courseLevel(level).test(c)).isTrue();
        assertThat(isUndergraduate().test(c)).isEqualTo(undergraduate);
        assertThat(isGraduate().test(c)).isEqualTo(graduate);
    }

    @ParameterizedTest
    @CsvSource({"A, true, false, true", "C, true, false, false", "D_PLUS, true, false, false", "D_MINUS, false, true, false",
            "F, false, true, false", "PASS, true, false, false", "NO_PASS, false, true, false",
            "WITHDRAW, false, false, false"})
    void enrollmentGradePredicates(Enrollment.Grade grade, boolean passing, boolean failing, boolean aboveThree) {
        Enrollment e = new Enrollment("E1", "S1", "C1", "Fall", 2024);
        e.setGrade(grade);

        assertThat(HAS_GRADE.test(e)).isTrue();
        assertThat(IS_PASSING.test(e)).isEqualTo(passing);
        assertThat(IS_FAILING.test(e)).isEqualTo(failing);
        assertThat(hasGradePointsAbove(3.0).test(e)).isEqualTo(aboveThree);
    }

    @Test
    void enrollmentStatusAndDatePredicates() {
        Enrollment e = new Enrollment("E1", "S1", "C1", "Fall", 2024);
        e.setEnrollmentDate(LocalDate.of(2024, 8, 20).atStartOfDay());

        assertThat(IS_ACTIVE_ENROLLMENT.test(e)).isTrue();
        assertThat(HAS_GRADE.test(e)).isFalse();
        assertThat(IS_PASSING.test(e)).isFalse();
        assertThat(enrolledInSemester(" FALL").test(e)).isTrue();
        assertThat(enrolledAfter(LocalDate.of(2024, 8, 19)).test(e)).isTrue();
        assertThat(enrolledAfter(LocalDate.of(2024, 8, 20)).test(e)).isFalse();
        e.setGrade(Enrollment.Grade.B);
        assertThat(hasGradeLevel(GradeLevel.B).test(e)).isTrue();
        assertThat(hasGradeLevel(null).test(e)).isFalse();
        e.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        assertThat(IS_DROPPED.test(e)).isTrue();
        assertThat(IS_ACTIVE_ENROLLMENT.test(e)).isFalse();
    }

    @Test
    void departmentPredicates() {
        Department d = new Department("D1", "cs", "Computer Science", "", "A", "P1", new BigDecimal("2500000"));
        d.addProfessor("P1");
        d.addStudent("S1", "2024");

        assertThat(IS_ACTIVE_DEPARTMENT.test(d)).isTrue();
        assertThat(HAS_HEAD.test(d)).isTrue();
        assertThat(HAS_BUDGET.test(d)).isTrue();
        assertThat(HAS_BUDGET.test(new Department("D2", "X", "X", "", "A"))).isFalse();
        assertThat(codeEquals(" CS ").test(d)).isTrue();
        assertThat(departmentNameContains("SCI").test(d)).isTrue();
        assertThat(budgetAbove(2_500_000).test(d)).isTrue();
        assertThat(budgetAbove(2_500_000.01).test(d)).isFalse();
        assertThat(facultyCountAbove(1).test(d)).isTrue();
        assertThat(studentCountAbove(2).test(d)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "first.last+tag@sub.uni.edu", "X_Y-z@domain.org"})
    void validEmails(String email) {
        assertThat(IS_VALID_EMAIL.test(email)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"plain", "a@b", "a@b.c", "@b.com", "a b@c.com", "a@b.com "})
    void invalidEmails(String email) {
        assertThat(IS_VALID_EMAIL.test(email)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"'+1 (555) 010-0100', true", "'5550100', true", "'0123', false", "'+', false",
            "'12345678901234567', false", "'555-CALL', false"})
    void phoneValidation(String phone, boolean valid) {
        assertThat(IS_VALID_PHONE.test(phone)).isEqualTo(valid);
    }

    @Test
    void stringPredicates() {
        assertThat(IS_NOT_EMPTY.test("  ")).isFalse();
        assertThat(IS_NUMERIC.test("-1.5e3")).isTrue();
        assertThat(IS_NUMERIC.test("12a")).isFalse();
        assertThat(IS_NUMERIC.test(null)).isFalse();
        assertThat(hasMinLength(3).test("abc")).isTrue();
        assertThat(hasMaxLength(2).test("abc")).isFalse();
        assertThat(lengthBetween(1, 3).test("")).isFalse();
        assertThat(matchesPattern("[A-Z]{2}\\d{3}").test("CS101")).isTrue();
        assertThat(matchesPattern("[A-Z]{2}\\d{3}").test("CS1010")).isFalse();
        assertThat(containsOnly("01").test("1010")).isTrue();
        assertThat(containsOnly("01").test("102")).isFalse();
        assertThat(containsOnly("01").test("")).isTrue();
    }

    @Test
    void dateRangePredicatesAreInclusive() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        assertThat(isBetween(start, end).test(start)).isTrue();
        assertThat(isBetween(start, end).test(end)).isTrue();
        assertThat(isBetween(start, end).test(end.plusDays(1))).isFalse();
        assertThat(isBetween(start, end).test(null)).isFalse();
        assertThat(Predicates.isAfter(start).test(start)).isFalse();
        assertThat(Predicates.isBefore(start).test(start.minusDays(1))).isTrue();
        assertThat(isToday().test(LocalDate.now())).isTrue();
        assertThat(isThisWeek().test(LocalDate.now())).isTrue();
        assertThat(isThisMonth().test(LocalDate.now())).isTrue();
        assertThat(isThisMonth().test(LocalDate.now().minusMonths(1))).isFalse();
    }

    @Test
    void collectionAndNumericPredicates() {
        assertThat(Predicates.<Integer>hasSize(2).test(List.of(1, 2))).isTrue();
        assertThat(Predicates.<Integer>hasSizeAbove(2).test(List.of(1, 2))).isFalse();
        assertThat(Predicates.<Integer>hasSizeBelow(3).test(List.of(1, 2))).isTrue();
        assertThat(Predicates.<Integer>isEmpty().test(null)).isTrue();
        assertThat(Predicates.<Integer>isNotEmpty().test(Set.of(1))).isTrue();
        assertThat(contains(2).test(List.of(1, 2))).isTrue();
        assertThat(containsAll(List.of(1, 3)).test(List.of(1, 2))).isFalse();
        assertThat(Predicates.<Integer>isPositive().test(0)).isFalse();
        assertThat(Predicates.<Double>isNegative().test(-0.1)).isTrue();
        assertThat(Predicates.<Long>isZero().test(0L)).isTrue();
        assertThat(Predicates.<Integer>isGreaterThan(5).test(5)).isFalse();
        assertThat(Predicates.<Integer>isLessThan(5).test(4)).isTrue();
        assertThat(Predicates.<Integer>isBetween(1.0, 5.0).test(5)).isTrue();
        assertThat(Predicates.<Integer>isPositive().test(null)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void compositePredicates() {
        Predicate<Integer> even = i -> i % 2 == 0;
        Predicate<Integer> big = i -> i > 10;

        assertThat(allOf(even, big).test(12)).isTrue();
        assertThat(allOf(even, big).test(8)).isFalse();
        assertThat(anyOf(even, big).test(11)).isTrue();
        assertThat(noneOf(even, big).test(3)).isTrue();
        assertThat(Predicates.<Integer>allOf().test(1)).isTrue();
        assertThat(Predicates.<Integer>anyOf().test(1)).isFalse();
        assertThat(not(even).test(3)).isTrue();
    }

    @Test
    void collectionUtilities() {
        List<Integer> nums = List.of(1, 2, 3, 4);

        assertThat(filter(nums, i -> i > 2)).containsExactly(3, 4);
        assertThat(Predicates.anyMatch(nums, i -> i == 4)).isTrue();
        assertThat(Predicates.allMatch(nums, i -> i > 0)).isTrue();
        assertThat(noneMatch(nums, i -> i > 4)).isTrue();
        assertThat(count(nums, i -> i % 2 == 0)).isEqualTo(2);
        assertThat(findFirst(nums, i -> i > 1)).contains(2);
        assertThat(findAny(nums, i -> i > 9)).isEmpty();
    }

    @Test
    void nullUsersNeverMatchAttributePredicates() {
        User nobody = null;
        assertThat(HAS_EMAIL.test(nobody)).isFalse();
        assertThat(hasRole("STUDENT").test(nobody)).isFalse();
        assertThat(nameContains("").test(nobody)).isFalse();
        assertThat(IS_STUDENT.test(nobody)).isFalse();
    }
}
