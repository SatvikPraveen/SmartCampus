package models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepartmentTest {

    private static Department cs() {
        return new Department("D1", " cs ", " Computer Science ", "desc", "Bldg A",
                "P1", new BigDecimal("1000.00"));
    }

    private static void addStudents(Department d, int count) {
        for (int i = 0; i < count; i++) {
            d.addStudent("S" + i, "Freshman");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void constructorNormalises() {
            Department d = cs();
            assertThat(d.getDepartmentCode()).isEqualTo("CS");
            assertThat(d.getDepartmentName()).isEqualTo("Computer Science");
            assertThat(d.isValidDepartment()).isTrue();
        }

        @Test
        void rejectsBlankRequiredFieldsAndBadEmail() {
            Department d = cs();
            assertThatThrownBy(() -> d.setDepartmentId(" ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> d.setDepartmentCode(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> d.setDepartmentName("")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> d.setEmail("cs.example.edu")).isInstanceOf(IllegalArgumentException.class);
            d.setEmail(null);
            assertThat(d.getEmail()).isNull();
        }

        @Test
        void validationErrorsListWhatIsMissing() {
            Department d = cs();
            assertThat(d.getValidationErrors()).containsExactlyInAnyOrder(
                    "Department must have at least one professor",
                    "Department must offer at least one major program");
            d.addProfessor("P1");
            d.addMajorProgram("Computer Science");
            assertThat(d.getValidationErrors()).isEmpty();
            d.setActive(false);
            assertThat(d.isValidDepartment()).isFalse();
        }
    }

    @Nested
    @DisplayName("membership")
    class Membership {

        @Test
        void professorsAreDeduplicatedAndRemovingTheHeadClearsIt() {
            Department d = cs();
            d.addProfessor("P1");
            d.addProfessor("P1");
            d.addProfessor(" ");
            d.addProfessor("P2");
            assertThat(d.getProfessorCount()).isEqualTo(2);
            d.removeProfessor("P2");
            assertThat(d.getHeadOfDepartmentId()).isEqualTo("P1");
            d.removeProfessor("P1");
            assertThat(d.getHeadOfDepartmentId()).isNull();
            assertThat(d.hasProfessor("P1")).isFalse();
        }

        @Test
        void studentCountsByYearNeverGoNegative() {
            Department d = cs();
            d.addStudent("S1", "Junior");
            d.addStudent("S1", "Junior");
            d.addStudent("S2", "Alien");
            assertThat(d.getStudentCount()).isEqualTo(2);
            assertThat(d.getStudentCountByYear("Junior")).isEqualTo(1);
            assertThat(d.getStudentCountByYear("Alien")).isZero();

            d.removeStudent("S1", "Junior");
            d.removeStudent("S2", "Junior");
            assertThat(d.getStudentCountByYear("Junior")).isZero();
            d.removeStudent("ghost", "Senior");
            assertThat(d.getStudentCount()).isZero();
            assertThat(d.getStudentsByYear()).allSatisfy((year, count) -> assertThat(count).isNotNegative());
        }

        @Test
        void programsAreCountedAndFilteredByType() {
            Department d = cs();
            d.addMajorProgram("CS");
            d.addMajorProgram("CS");
            d.addMinorProgram("Data Science");
            d.addGraduateProgram("MS CS");
            assertThat(d.getNumberOfPrograms()).isEqualTo(3);
            assertThat(d.getProgramsByType("MAJOR")).containsExactly("CS");
            assertThat(d.getProgramsByType("graduate")).containsExactly("MS CS");
            assertThat(d.getProgramsByType("anything")).containsExactly("CS", "Data Science", "MS CS");
        }

        @Test
        void equipmentSearch() {
            Department d = cs();
            d.addEquipment("3D Printer", "Lab 1");
            d.addEquipment("Oscilloscope", "Lab 2");
            d.addEquipment("Laser printer", "Lab 1");
            assertThat(d.searchEquipment("PRINTER")).containsExactlyInAnyOrder("3D Printer", "Laser printer");
            assertThat(d.getEquipmentAtLocation("Lab 1")).containsExactlyInAnyOrder("3D Printer", "Laser printer");
            d.removeEquipment("3D Printer");
            assertThat(d.getEquipment()).doesNotContainKey("3D Printer");
        }
    }

    @Nested
    @DisplayName("statistics")
    class Statistics {

        @Test
        void ratiosTrackMembership() {
            Department d = cs();
            d.addProfessor("P1");
            d.addProfessor("P2");
            d.addCourse("C1");
            d.addCourse("C2");
            d.addCourse("C3");
            d.addCourse("C4");
            addStudents(d, 40);
            assertThat(d.getTotalEnrollment()).isEqualTo(40);
            assertThat(d.getStudentToFacultyRatio()).isEqualTo(20.0);
            assertThat(d.getAverageClassSize()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("ratios reset when the last course or professor is removed (regression)")
        void ratiosDoNotGoStale() {
            Department d = cs();
            d.addProfessor("P1");
            d.addCourse("C1");
            addStudents(d, 30);
            d.removeCourse("C1");
            assertThat(d.getAverageClassSize()).isZero();
            d.removeProfessor("P1");
            assertThat(d.getStudentToFacultyRatio()).isZero();
        }

        @Test
        void wellStaffedRequiresThreeProfessorsAndRatioAtMostTwenty() {
            Department d = cs();
            d.addProfessor("P1");
            d.addProfessor("P2");
            d.addProfessor("P3");
            addStudents(d, 60);
            assertThat(d.isWellStaffed()).isTrue();
            d.addStudent("S60", "Senior");
            assertThat(d.isWellStaffed()).isFalse();
        }

        @Test
        void healthStatus() {
            Department d = cs();
            assertThat(d.getDepartmentHealthStatus()).isEqualTo("Needs Improvement");
            addStudents(d, 21);
            assertThat(d.getDepartmentHealthStatus()).isEqualTo("Fair");
            d.addProfessor("P1");
            d.addProfessor("P2");
            d.addProfessor("P3");
            d.addProfessor("P4");
            d.addProfessor("P5");
            assertThat(d.getDepartmentHealthStatus()).isEqualTo("Good");
        }
    }

    @Nested
    @DisplayName("budget")
    class Budget {

        @Test
        void spendingIsCappedByTheAllocation() {
            Department d = cs();
            assertThat(d.allocateBudget(new BigDecimal("400"), "lab")).isTrue();
            assertThat(d.allocateBudget(new BigDecimal("700"), "too much")).isFalse();
            assertThat(d.allocateBudget(new BigDecimal("600"), "exactly the rest")).isTrue();
            assertThat(d.getRemainingBudget()).isEqualByComparingTo("0");
            assertThat(d.getBudgetUtilizationPercentage()).isEqualTo(100.0);
            assertThat(d.isOverBudget()).isFalse();
        }

        @Test
        void nonPositiveAllocationsAreRejected() {
            Department d = cs();
            assertThat(d.allocateBudget(null, "x")).isFalse();
            assertThat(d.allocateBudget(BigDecimal.ZERO, "x")).isFalse();
            assertThat(d.allocateBudget(new BigDecimal("-5"), "x")).isFalse();
            assertThat(d.getSpentBudget()).isEqualByComparingTo("0");
        }

        @Test
        void utilisationAndOverBudget() {
            Department d = cs();
            d.allocateBudget(new BigDecimal("400"), "lab");
            assertThat(d.getBudgetUtilizationPercentage()).isEqualTo(40.0);
            d.setAllocatedBudget(new BigDecimal("2000"));
            assertThat(d.getAllocatedBudget()).isEqualByComparingTo("1000");
            d.setAllocatedBudget(new BigDecimal("300"));
            assertThat(d.isOverBudget()).isTrue();
            assertThat(d.getRemainingBudget()).isEqualByComparingTo("-100");
        }

        @Test
        void zeroAllocationMeansZeroUtilisation() {
            Department d = new Department("D2", "MATH", "Mathematics", null, null);
            assertThat(d.getBudgetUtilizationPercentage()).isZero();
            d.setAnnualBudget(new BigDecimal("-1"));
            assertThat(d.getAnnualBudget()).isEqualByComparingTo("0");
        }
    }

    @Test
    void equalityIsByDepartmentId() {
        Department a = cs();
        Department b = new Department("D1", "X", "Y", null, null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
