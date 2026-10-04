package models;

import models.Professor.AcademicRank;
import models.Professor.EmploymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ProfessorTest {

    private static Professor assistant() {
        return new Professor("U9", "Alan", "Turing", "alan@example.edu", null,
                "P1", "D1", AcademicRank.ASSISTANT, "Computability");
    }

    @ParameterizedTest(name = "{0} may teach {1} courses")
    @CsvSource({"FULL_TIME, 4", "PART_TIME, 2", "CONTRACT, 2", "SABBATICAL, 2"})
    void teachingLoadDependsOnEmploymentStatus(EmploymentStatus status, int max) {
        Professor p = assistant();
        p.setEmploymentStatus(status);
        for (int i = 0; i < max + 2; i++) {
            p.assignCourse("C" + i);
        }
        assertThat(p.getTeachingCourseIds()).hasSize(max);
        assertThat(p.canTeachMoreCourses()).isFalse();
        p.removeCourse("C0");
        assertThat(p.canTeachMoreCourses()).isTrue();
    }

    @Test
    void assignCourseIgnoresBlankAndDuplicateIds() {
        Professor p = assistant();
        p.assignCourse("C1");
        p.assignCourse("C1");
        p.assignCourse(" ");
        p.assignCourse(null);
        assertThat(p.getTeachingCourseIds()).containsExactly("C1");
        p.getTeachingCourseIds().clear();
        assertThat(p.getTeachingCourseIds()).containsExactly("C1");
    }

    @ParameterizedTest(name = "{0} -> tenured {1}")
    @CsvSource({"ASSOCIATE, true", "FULL, true", "EMERITUS, false"})
    void promotionToAssociateOrFullGrantsTenure(AcademicRank rank, boolean tenured) {
        Professor p = assistant();
        p.promote(rank);
        assertThat(p.getAcademicRank()).isEqualTo(rank);
        assertThat(p.isTenured()).isEqualTo(tenured);
    }

    @ParameterizedTest
    @EnumSource(value = AcademicRank.class, names = {"ADJUNCT", "ASSISTANT"})
    void promoteNeverDemotesOrStaysLevel(AcademicRank rank) {
        Professor p = assistant();
        p.promote(rank);
        assertThat(p.getAcademicRank()).isEqualTo(AcademicRank.ASSISTANT);
        assertThat(p.isTenured()).isFalse();
    }

    @Test
    void tenureRequiresSeniorRank() {
        Professor p = assistant();
        p.grantTenure();
        assertThat(p.isTenured()).isFalse();
        p.setAcademicRank(AcademicRank.ASSOCIATE);
        p.grantTenure();
        assertThat(p.isTenured()).isTrue();
    }

    @Test
    void sabbaticalRequiresTenure() {
        Professor p = assistant();
        p.goOnSabbatical();
        assertThat(p.getEmploymentStatus()).isEqualTo(EmploymentStatus.FULL_TIME);
        p.promote(AcademicRank.ASSOCIATE);
        p.goOnSabbatical();
        assertThat(p.getEmploymentStatus()).isEqualTo(EmploymentStatus.SABBATICAL);
        p.returnFromSabbatical();
        assertThat(p.getEmploymentStatus()).isEqualTo(EmploymentStatus.FULL_TIME);

        p.setEmploymentStatus(EmploymentStatus.PART_TIME);
        p.returnFromSabbatical();
        assertThat(p.getEmploymentStatus()).isEqualTo(EmploymentStatus.PART_TIME);
    }

    @Test
    void ratingIsBoundedZeroToFive() {
        Professor p = assistant();
        p.updateTeachingRating(4.5);
        p.updateTeachingRating(5.1);
        p.updateTeachingRating(-0.1);
        p.setTeachingRating(7);
        assertThat(p.getTeachingRating()).isEqualTo(4.5);
    }

    @Test
    void promotionEligibility() {
        Professor p = assistant();
        p.setYearsOfExperience(6);
        p.updateTeachingRating(3.5);
        assertThat(p.isEligibleForPromotion()).isFalse();
        p.setTenured(true);
        assertThat(p.isEligibleForPromotion()).isTrue();
        p.setYearsOfExperience(5);
        assertThat(p.isEligibleForPromotion()).isFalse();
    }

    @Test
    void workloadScore() {
        Professor p = assistant();
        p.setAcademicRank(AcademicRank.ASSOCIATE);
        p.assignCourse("C1");
        p.assignCourse("C2");
        p.setYearsOfExperience(10);
        assertThat(p.calculateWorkloadScore()).isCloseTo(2 + 1.0 + 0.6, within(1e-9));
        p.setYearsOfExperience(40);
        assertThat(p.calculateWorkloadScore()).isCloseTo(2 + 2.0 + 0.6, within(1e-9));
    }

    @Test
    void invalidValuesAreRejectedOrIgnored() {
        Professor p = assistant();
        assertThatThrownBy(() -> p.setProfessorId(" ")).isInstanceOf(IllegalArgumentException.class);
        p.setSalary(new BigDecimal("90000"));
        p.setSalary(new BigDecimal("-1"));
        p.setSalary(null);
        assertThat(p.getSalary()).isEqualByComparingTo("90000");
        p.setYearsOfExperience(-3);
        assertThat(p.getYearsOfExperience()).isZero();
        p.addQualification(" PhD ");
        p.addQualification("");
        assertThat(p.getQualifications()).containsExactly("PhD");
    }

    @Test
    void roleAndEquality() {
        Professor p = assistant();
        assertThat(p.getRole()).isEqualTo("PROFESSOR");
        Professor same = assistant();
        assertThat(p).isEqualTo(same).hasSameHashCodeAs(same);
        same.setProfessorId("P2");
        assertThat(p).isNotEqualTo(same);
    }
}
