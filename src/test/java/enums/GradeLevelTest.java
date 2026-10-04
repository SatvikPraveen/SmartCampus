package enums;

import enums.GradeLevel.GradeCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GradeLevelTest {

    private static final Set<GradeLevel> LETTER_SCALE = EnumSet.range(GradeLevel.A_PLUS, GradeLevel.F);
    private static final Set<GradeCategory> PERFORMANCE_CATEGORIES = EnumSet.of(
            GradeCategory.EXCELLENT, GradeCategory.GOOD, GradeCategory.SATISFACTORY,
            GradeCategory.BELOW_AVERAGE, GradeCategory.FAILING);

    @Nested
    @DisplayName("fromPercentage")
    class FromPercentage {

        @ParameterizedTest(name = "{0}% -> {1}")
        @CsvSource({
                "100.0, A_PLUS", "97.0, A_PLUS", "96.9, A", "93.0, A", "92.9, A_MINUS", "90.0, A_MINUS",
                "89.9, B_PLUS", "87.0, B_PLUS", "85.0, B", "83.0, B", "82.9, B_MINUS", "80.0, B_MINUS",
                "77.0, C_PLUS", "75.0, C", "73.0, C", "70.0, C_MINUS",
                "67.0, D_PLUS", "65.0, D", "60.0, D_MINUS", "59.9, F", "0.0, F"
        })
        void mapsPercentagesToTheUndergraduateLetterScale(double percentage, GradeLevel expected) {
            assertThat(GradeLevel.fromPercentage(percentage)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}% -> {1}")
        @CsvSource({"96.95, A", "92.95, A_MINUS", "89.95, B_PLUS", "69.95, D_PLUS", "59.95, F"})
        @DisplayName("values between the one-decimal band edges fall into the lower band")
        void handlesGapsBetweenBands(double percentage, GradeLevel expected) {
            assertThat(GradeLevel.fromPercentage(percentage)).isEqualTo(expected);
        }

        @Test
        @DisplayName("85% is a B, not the international DISTINCTION grade (regression)")
        void doesNotLeakIntoInternationalOrGraduateScales() {
            assertThat(GradeLevel.fromPercentage(85.0)).isEqualTo(GradeLevel.B);
            assertThat(GradeLevel.fromPercentage(95.0)).isEqualTo(GradeLevel.A);
        }

        @Test
        void clampsOutOfRangeScores() {
            assertThat(GradeLevel.fromPercentage(105.0)).isEqualTo(GradeLevel.A_PLUS);
            assertThat(GradeLevel.fromPercentage(-3.0)).isEqualTo(GradeLevel.F);
        }

        @Test
        @DisplayName("is monotonic and always returns a standard letter grade")
        void isMonotonicOverTheWholeRange() {
            double previousPoints = -1;
            for (int i = 0; i <= 2000; i++) {
                double pct = i * 0.05;
                GradeLevel grade = GradeLevel.fromPercentage(pct);
                assertThat(LETTER_SCALE).as("grade for %s%%", pct).contains(grade);
                assertThat(grade.getGpaPoints()).as("points for %s%%", pct).isGreaterThanOrEqualTo(previousPoints);
                previousPoints = grade.getGpaPoints();
            }
        }

        @ParameterizedTest
        @EnumSource(value = GradeLevel.class, names = {
                "A_PLUS", "A", "A_MINUS", "B_PLUS", "B", "B_MINUS", "C_PLUS", "C", "C_MINUS",
                "D_PLUS", "D", "D_MINUS", "F"})
        void bandEdgesRoundTrip(GradeLevel grade) {
            assertThat(GradeLevel.fromPercentage(grade.getMinPercentage())).isEqualTo(grade);
            assertThat(GradeLevel.fromPercentage(grade.getMaxPercentage())).isEqualTo(grade);
            assertThat(grade.isInRange(grade.getMinPercentage())).isTrue();
        }
    }

    @Nested
    @DisplayName("fromGpaPoints")
    class FromGpaPoints {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"3.8, A_MINUS", "3.7, A_MINUS", "3.4, B_PLUS", "3.0, B", "2.0, C", "1.1, D", "0.7, D_MINUS", "0.0, F"})
        void returnsTheNearestLetterGrade(double points, GradeLevel expected) {
            assertThat(GradeLevel.fromGpaPoints(points)).isEqualTo(expected);
        }

        @Test
        @DisplayName("never returns honors / international grades (regression: 3.8 used to be MAGNA_CUM_LAUDE)")
        void staysOnTheLetterScale() {
            for (int i = 0; i <= 50; i++) {
                assertThat(LETTER_SCALE).contains(GradeLevel.fromGpaPoints(i * 0.1));
            }
        }

        @Test
        void clampsAboveFour() {
            assertThat(GradeLevel.fromGpaPoints(5.0).getGpaPoints()).isEqualTo(4.0);
        }
    }

    @Nested
    @DisplayName("getGpaCategory")
    class GpaCategory {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "4.0, EXCELLENT", "3.7, EXCELLENT", "3.65, GOOD", "3.6, GOOD", "2.7, GOOD",
                "2.65, SATISFACTORY", "1.7, SATISFACTORY", "1.65, BELOW_AVERAGE", "0.7, BELOW_AVERAGE",
                "0.69, FAILING", "0.0, FAILING", "4.3, EXCELLENT"
        })
        void classifiesByPerformanceBand(double gpa, GradeCategory expected) {
            assertThat(GradeLevel.getGpaCategory(gpa)).isEqualTo(expected);
        }

        @Test
        @DisplayName("only ever returns a performance category (regression: gaps used to map to GRADUATE/INTERNATIONAL)")
        void neverReturnsNonPerformanceCategories() {
            for (int i = 0; i <= 400; i++) {
                assertThat(PERFORMANCE_CATEGORIES).contains(GradeLevel.getGpaCategory(i * 0.01));
            }
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"3.7, Excellent", "3.69, Good", "3.0, Good", "2.0, Satisfactory", "1.0, Below Average", "0.99, Poor"})
        void describesGpa(double gpa, String expected) {
            assertThat(GradeLevel.getGpaDescription(gpa)).isEqualTo(expected);
        }

        @Test
        void categoriesWithoutRangeIncludeNothing() {
            assertThat(GradeCategory.SPECIAL.includesGpa(2.0)).isFalse();
            assertThat(GradeCategory.EXCELLENT.includesGpa(null)).isFalse();
            assertThat(GradeCategory.EXCELLENT.includesGpa(3.7)).isTrue();
        }
    }

    @Nested
    @DisplayName("grade predicates")
    class Predicates {

        @Test
        void passingAndFailing() {
            assertThat(GradeLevel.B.isPassingGrade()).isTrue();
            assertThat(GradeLevel.D.isPassingGrade()).isTrue();
            assertThat(GradeLevel.F.isFailingGrade()).isTrue();
            assertThat(GradeLevel.PASS.isPassingGrade()).isTrue();
            assertThat(GradeLevel.NO_PASS.isFailingGrade()).isTrue();
            assertThat(GradeLevel.WITHDRAW_FAILING.isFailingGrade()).isTrue();
            assertThat(GradeLevel.INCOMPLETE.isPassingGrade()).isFalse();
            assertThat(GradeLevel.INCOMPLETE.isFailingGrade()).isFalse();
            assertThat(GradeLevel.WITHDRAW.isPassingGrade()).isFalse();
            assertThat(GradeLevel.WITHDRAW.isFailingGrade()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(GradeLevel.class)
        void noGradeIsBothPassingAndFailing(GradeLevel grade) {
            assertThat(grade.isPassingGrade() && grade.isFailingGrade()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(GradeLevel.class)
        void gradesWithoutPointsNeverAffectGpa(GradeLevel grade) {
            if (grade.getGpaPoints() == null || grade.getCategory() == GradeCategory.SPECIAL) {
                assertThat(grade.affectsGpa()).isFalse();
            }
        }

        @Test
        void withdrawalAndIncompleteFlags() {
            assertThat(GradeLevel.WITHDRAW_PASSING.isWithdrawal()).isTrue();
            assertThat(GradeLevel.IN_PROGRESS.isIncomplete()).isTrue();
            assertThat(GradeLevel.A.isWithdrawal()).isFalse();
            assertThat(GradeLevel.WITHDRAW_FAILING.affectsGpa()).isFalse();
        }

        @Test
        void listsArePartitionedConsistently() {
            assertThat(GradeLevel.getPassingGrades()).allMatch(GradeLevel::isPassingGrade);
            assertThat(GradeLevel.getFailingGrades()).allMatch(GradeLevel::isFailingGrade);
            assertThat(GradeLevel.getPassingGrades()).doesNotContainAnyElementsOf(GradeLevel.getFailingGrades());
            assertThat(GradeLevel.getGpaAffectingGrades()).allMatch(GradeLevel::affectsGpa);
        }
    }

    @Nested
    @DisplayName("lookups")
    class Lookups {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({"a+, A_PLUS", "b-, B_MINUS", "WF, WITHDRAW_FAILING", "cr, CREDIT", "GA, GRADUATE_A"})
        void fromLetterGradeIsCaseInsensitive(String letter, GradeLevel expected) {
            assertThat(GradeLevel.fromLetterGrade(letter)).isEqualTo(expected);
        }

        @Test
        void fromLetterGradeReturnsNullForUnknownLetters() {
            assertThat(GradeLevel.fromLetterGrade("Z")).isNull();
        }

        @Test
        void getGradesByCategoryReturnsAnIndependentCopy() {
            List<GradeLevel> excellent = GradeLevel.getGradesByCategory(GradeCategory.EXCELLENT);
            assertThat(excellent).containsExactly(GradeLevel.A_PLUS, GradeLevel.A, GradeLevel.A_MINUS);
            excellent.clear();
            assertThat(GradeLevel.getGradesByCategory(GradeCategory.EXCELLENT)).hasSize(3);
        }

        @Test
        void standardGradesAreSortedByPointsDescendingAndExcludeSpecialGrades() {
            List<GradeLevel> standard = GradeLevel.getStandardGrades();
            assertThat(standard).isSortedAccordingTo((a, b) -> Double.compare(b.getGpaPoints(), a.getGpaPoints()));
            assertThat(standard).noneMatch(g -> g.getCategory() == GradeCategory.SPECIAL);
            assertThat(standard).containsAll(LETTER_SCALE);
        }
    }

    @Nested
    @DisplayName("GPA arithmetic")
    class GpaArithmetic {

        @Test
        void calculateGpaWeightsByCredits() {
            Map<GradeLevel, Integer> credits = new LinkedHashMap<>();
            credits.put(GradeLevel.A, 4);
            credits.put(GradeLevel.F, 2);
            assertThat(GradeLevel.calculateGpa(credits)).isCloseTo(16.0 / 6, within(1e-9));
        }

        @Test
        void calculateGpaIgnoresNonGpaGrades() {
            Map<GradeLevel, Integer> credits = new LinkedHashMap<>();
            credits.put(GradeLevel.A, 3);
            credits.put(GradeLevel.B, 3);
            credits.put(GradeLevel.PASS, 3);
            credits.put(GradeLevel.WITHDRAW, 3);
            credits.put(GradeLevel.WITHDRAW_FAILING, 3);
            assertThat(GradeLevel.calculateGpa(credits)).isCloseTo(3.5, within(1e-9));
        }

        @Test
        void calculateGpaOfNothingIsZero() {
            assertThat(GradeLevel.calculateGpa(Map.of())).isZero();
            assertThat(GradeLevel.calculateGpa(Map.of(GradeLevel.INCOMPLETE, 3))).isZero();
        }

        @Test
        void averageGpaSkipsNonGpaGrades() {
            OptionalDouble avg = GradeLevel.getAverageGpa(List.of(GradeLevel.A, GradeLevel.C, GradeLevel.WITHDRAW));
            assertThat(avg).hasValueCloseTo(3.0, within(1e-9));
            assertThat(GradeLevel.getAverageGpa(List.of(GradeLevel.INCOMPLETE))).isEmpty();
        }

        @Test
        void distributionCountsByCategory() {
            Map<GradeCategory, Long> dist = GradeLevel.getGradeDistribution(
                    List.of(GradeLevel.A, GradeLevel.A_MINUS, GradeLevel.B, GradeLevel.F));
            assertThat(dist).containsEntry(GradeCategory.EXCELLENT, 2L)
                    .containsEntry(GradeCategory.GOOD, 1L)
                    .containsEntry(GradeCategory.FAILING, 1L)
                    .hasSize(3);
        }
    }

    @Nested
    @DisplayName("grade changes")
    class GradeChanges {

        @Test
        void progressionRules() {
            assertThat(GradeLevel.isValidGradeProgression(null, GradeLevel.A)).isTrue();
            assertThat(GradeLevel.isValidGradeProgression(GradeLevel.INCOMPLETE, GradeLevel.B)).isTrue();
            assertThat(GradeLevel.isValidGradeProgression(GradeLevel.INCOMPLETE, GradeLevel.IN_PROGRESS)).isFalse();
            assertThat(GradeLevel.isValidGradeProgression(GradeLevel.WITHDRAW, GradeLevel.A)).isFalse();
            assertThat(GradeLevel.isValidGradeProgression(GradeLevel.B, GradeLevel.A)).isTrue();
        }

        @Test
        void validOptions() {
            assertThat(GradeLevel.getValidGradeOptions(GradeLevel.WITHDRAW)).containsExactly(GradeLevel.WITHDRAW);
            assertThat(GradeLevel.getValidGradeOptions(GradeLevel.INCOMPLETE))
                    .containsExactlyElementsOf(GradeLevel.getStandardGrades());
            assertThat(GradeLevel.getValidGradeOptions(null)).containsExactly(GradeLevel.values());
        }
    }

    @Test
    void toStringIncludesPointsAndRangeWhenPresent() {
        assertThat(GradeLevel.A.toString()).isEqualTo("A (4.0 pts) [93.0-96.9%] - Excellent");
        assertThat(GradeLevel.WITHDRAW.toString()).isEqualTo("W - Withdrawal");
    }
}
