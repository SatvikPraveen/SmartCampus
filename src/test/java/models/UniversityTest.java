package models;

import models.University.AccreditationStatus;
import models.University.UniversityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniversityTest {

    private static University usf() {
        return new University("U1", "University of South Florida", "USF", UniversityType.PUBLIC);
    }

    @Test
    void defaults() {
        University u = usf();
        assertThat(u.isActive()).isTrue();
        assertThat(u.isAcceptingApplications()).isTrue();
        assertThat(u.getMaxStudentsPerCourse()).isEqualTo(30);
        assertThat(u.getMaxCoursesPerStudent()).isEqualTo(6);
        assertThat(u.getMinimumGpaRequirement()).isEqualTo(2.0);
        assertThat(u.getSemesterSchedule()).containsExactly("Fall", "Spring", "Summer");
    }

    @Test
    void researchUniversityFactory() {
        University u = University.createResearchUniversity("Big State", "big state", "FL");
        assertThat(u.getUniversityId()).startsWith("UNI_BIG_STATE_");
        assertThat(u.getType()).isEqualTo(UniversityType.RESEARCH_UNIVERSITY);
        assertThat(u.getAccreditationStatus()).isEqualTo(AccreditationStatus.FULLY_ACCREDITED);
        assertThat(u.getState()).isEqualTo("FL");
    }

    @Test
    void membershipIsDeduplicatedAndCounted() {
        University u = usf();
        assertThat(u.addStudent("S1")).isTrue();
        assertThat(u.addStudent("S1")).isFalse();
        assertThat(u.addStudent(" ")).isFalse();
        assertThat(u.addCourse("C1")).isTrue();
        assertThat(u.addAdmin("A1")).isTrue();
        assertThat(u.getTotalStudents()).isEqualTo(1);
        assertThat(u.getTotalCourses()).isEqualTo(1);
        assertThat(u.getTotalAdmins()).isEqualTo(1);
        assertThat(u.removeStudent("S1")).isTrue();
        assertThat(u.removeStudent("S1")).isFalse();
        assertThat(u.removeCourse("C1")).isTrue();
        assertThat(u.removeAdmin("A1")).isTrue();
        assertThat(u.getTotalStudents()).isZero();
    }

    @Test
    @DisplayName("student/faculty ratio follows student changes too (regression)")
    void ratioStaysCurrent() {
        University u = usf();
        u.addProfessor("P1");
        u.addProfessor("P2");
        for (int i = 0; i < 10; i++) {
            u.addStudent("S" + i);
        }
        assertThat(u.getFacultyToStudentRatio()).isEqualTo(5.0);
        u.removeStudent("S0");
        assertThat(u.getFacultyToStudentRatio()).isEqualTo(4.5);
        u.removeProfessor("P1");
        assertThat(u.getFacultyToStudentRatio()).isEqualTo(9.0);
        u.removeProfessor("P2");
        assertThat(u.getFacultyToStudentRatio()).isZero();
        assertThat(u.getEnrollmentStatistics()).containsEntry("facultyToStudentRatio", 0.0);
    }

    @Test
    void departmentEnrollment() {
        University u = usf();
        u.addDepartment("CS");
        u.addDepartment("MATH");
        u.updateDepartmentEnrollment("CS", 300);
        u.updateDepartmentEnrollment("MATH", 120);
        u.updateDepartmentEnrollment("GHOST", 999);
        assertThat(u.getTotalEnrollment()).isEqualTo(420);
        assertThat(u.getMostPopularDepartment()).isEqualTo("CS");
        assertThat(u.getEnrollmentByDepartment()).doesNotContainKey("GHOST");

        assertThat(u.removeDepartment("CS")).isTrue();
        assertThat(u.getTotalEnrollment()).isEqualTo(120);
        assertThat(u.getTotalDepartments()).isEqualTo(1);
        assertThat(u.addDepartment("MATH")).isFalse();
    }

    @Test
    void mostPopularDepartmentOfNothingIsNull() {
        assertThat(usf().getMostPopularDepartment()).isNull();
        assertThat(usf().getTotalEnrollment()).isZero();
    }

    @Test
    void boundedSettersIgnoreInvalidValues() {
        University u = usf();
        u.setAverageGPA(3.2);
        u.setAverageGPA(4.1);
        u.setGraduationRate(80);
        u.setGraduationRate(100.1);
        u.setRetentionRate(-1);
        u.setTuitionInState(-100);
        u.setMaxStudentsPerCourse(0);
        u.setMinimumGpaRequirement(5);
        assertThat(u.getAverageGPA()).isEqualTo(3.2);
        assertThat(u.getGraduationRate()).isEqualTo(80);
        assertThat(u.getRetentionRate()).isZero();
        assertThat(u.getTuitionInState()).isZero();
        assertThat(u.getMaxStudentsPerCourse()).isEqualTo(30);
        assertThat(u.getMinimumGpaRequirement()).isEqualTo(2.0);
    }

    @Test
    void inactiveUniversitiesDoNotAcceptApplications() {
        University u = usf();
        u.setActive(false);
        assertThat(u.isAcceptingApplications()).isFalse();
        u.setActive(true);
        u.setAcceptingApplications(false);
        assertThat(u.isAcceptingApplications()).isFalse();
    }

    @Test
    void fullAddressSkipsMissingParts() {
        University u = usf();
        assertThat(u.getFullAddress()).isEmpty();
        u.setCity("Tampa");
        u.setCountry("USA");
        assertThat(u.getFullAddress()).isEqualTo("Tampa, USA");
        u.setAddress("4202 E Fowler Ave");
        u.setState("FL");
        u.setZipCode("33620");
        assertThat(u.getFullAddress()).isEqualTo("4202 E Fowler Ave, Tampa, FL 33620, USA");
    }

    @Test
    void collectionsAreReadOnlyViews() {
        University u = usf();
        u.addStudent("S1");
        assertThatThrownBy(() -> u.getStudentIds().add("S2")).isInstanceOf(UnsupportedOperationException.class);
        Map<String, Integer> byDept = u.getEnrollmentByDepartment();
        assertThatThrownBy(() -> byDept.put("X", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("toString works on a default-constructed university (regression)")
    void toStringIsNullSafe() {
        assertThat(new University().toString()).startsWith("University{");
        assertThat(usf().toString()).contains("Public University");
    }

    @Test
    void equalityIsByUniversityId() {
        University a = usf();
        University b = new University("U1", "Other", "O", UniversityType.ONLINE);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
