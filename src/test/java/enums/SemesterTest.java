package enums;

import enums.Semester.AcademicYear;
import enums.Semester.SemesterType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SemesterTest {

    @ParameterizedTest(name = "{0} contains {1}: {2}")
    @CsvSource({
            "FALL, AUGUST, true", "FALL, DECEMBER, true", "FALL, JULY, false",
            "SPRING, JANUARY, true", "SPRING, JUNE, false",
            "WINTER_TRIMESTER, DECEMBER, true", "WINTER_TRIMESTER, JANUARY, true",
            "WINTER_TRIMESTER, MARCH, true", "WINTER_TRIMESTER, APRIL, false", "WINTER_TRIMESTER, NOVEMBER, false",
            "YEAR_LONG, OCTOBER, true", "YEAR_LONG, JUNE, false",
            "MAY_TERM, MAY, true", "MAY_TERM, JUNE, false",
            "THESIS, MARCH, false"
    })
    void containsMonthHandlesYearWrap(Semester semester, Month month, boolean expected) {
        assertThat(semester.containsMonth(month)).isEqualTo(expected);
        assertThat(semester.containsDate(LocalDate.of(2025, month, 15))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} {1}: {2} .. {3}")
    @CsvSource({
            "FALL, 2024, 2024-08-01, 2024-12-31",
            "SPRING, 2024, 2024-01-01, 2024-05-31",
            "WINTER_TRIMESTER, 2024, 2024-12-01, 2025-03-31",
            "WINTER_INTERSESSION, 2024, 2024-12-01, 2025-01-31",
            "YEAR_LONG, 2023, 2023-08-01, 2024-05-31"
    })
    void startAndEndDates(Semester semester, int year, LocalDate start, LocalDate end) {
        assertThat(semester.getStartDate(year)).isEqualTo(start);
        assertThat(semester.getEndDate(year)).isEqualTo(end);
        assertThat(semester.getEndDate(year)).isAfter(semester.getStartDate(year));
    }

    @Test
    void specialTermsHaveNoDates() {
        assertThat(Semester.THESIS.getStartDate(2024)).isNull();
        assertThat(Semester.THESIS.getEndDate(2024)).isNull();
    }

    @Test
    void fromAbbreviationIsCaseInsensitive() {
        assertThat(Semester.fromAbbreviation("fall")).isEqualTo(Semester.FALL);
        assertThat(Semester.fromAbbreviation("ss1")).isEqualTo(Semester.SUMMER_SESSION_I);
        assertThat(Semester.fromAbbreviation("XYZ")).isNull();
    }

    @Test
    void nextSemesterCyclesWithinType() {
        assertThat(Semester.getNextSemester(Semester.SPRING)).isEqualTo(Semester.SUMMER);
        assertThat(Semester.getNextSemester(Semester.SUMMER)).isEqualTo(Semester.FALL);
        assertThat(Semester.getNextSemester(Semester.FALL)).isEqualTo(Semester.SPRING);
        assertThat(Semester.getNextSemester(Semester.FALL_QUARTER)).isEqualTo(Semester.WINTER_QUARTER);
        assertThat(Semester.getPreviousSemester(Semester.WINTER_QUARTER)).isEqualTo(Semester.FALL_QUARTER);
    }

    @ParameterizedTest
    @EnumSource(value = Semester.class, mode = EnumSource.Mode.MATCH_ANY,
            names = {"SPRING", "SUMMER", "FALL", ".*_QUARTER", ".*_TRIMESTER", "MICHAELMAS", "HILARY", "TRINITY"})
    void previousIsInverseOfNext(Semester semester) {
        Semester next = Semester.getNextSemester(semester);
        assertThat(next.getType()).isEqualTo(semester.getType());
        assertThat(Semester.getPreviousSemester(next)).isEqualTo(semester);
    }

    @Test
    void getByTypeReturnsAnIndependentCopy() {
        List<Semester> quarters = Semester.getQuarters();
        assertThat(quarters).hasSize(4);
        quarters.clear();
        assertThat(Semester.getQuarters()).hasSize(4);
        assertThat(Semester.getNextSemester(Semester.WINTER_QUARTER)).isEqualTo(Semester.SPRING_QUARTER);
    }

    @Test
    void getByTypeGroupsEverySemester() {
        int total = 0;
        for (SemesterType type : SemesterType.values()) {
            List<Semester> ofType = Semester.getByType(type);
            assertThat(ofType).allMatch(s -> s.getType() == type);
            total += ofType.size();
        }
        assertThat(total).isEqualTo(Semester.values().length);
        assertThat(Semester.getTraditionalSemesters()).containsExactly(Semester.SPRING, Semester.SUMMER, Semester.FALL);
    }

    @ParameterizedTest(name = "{0} multiplier {1}")
    @CsvSource({"FALL, 1.0", "FALL_QUARTER, 0.67", "SPRING_TRIMESTER, 0.75", "MAY_TERM, 0.2",
            "SUMMER_SESSION_I, 0.4", "THESIS, 1.0", "HILARY, 1.0"})
    void creditMultiplier(Semester semester, double expected) {
        assertThat(semester.getCreditMultiplier()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void creditConversionRoundsToNearestCredit() {
        assertThat(Semester.FALL_QUARTER.convertCreditsToSemester(15)).isEqualTo(10);
        assertThat(Semester.FALL_QUARTER.convertCreditsFromSemester(10)).isEqualTo(15);
        assertThat(Semester.FALL.convertCreditsToSemester(12)).isEqualTo(12);
        assertThat(Semester.FALL_TRIMESTER.convertCreditsToSemester(4)).isEqualTo(3);
    }

    @Test
    void summerFlag() {
        assertThat(Semester.SUMMER.isSummer()).isTrue();
        assertThat(Semester.SUMMER_SESSION_II.isSummer()).isTrue();
        assertThat(Semester.FALL.isSummer()).isFalse();
        assertThat(Semester.FALL.isTraditional()).isTrue();
        assertThat(Semester.MAY_TERM.isIntensive()).isTrue();
        assertThat(Semester.THESIS.isSpecial()).isTrue();
    }

    @Test
    void academicYear() {
        AcademicYear year = new AcademicYear(2024, SemesterType.QUARTER);
        assertThat(year.getYearString()).isEqualTo("2024-2025");
        assertThat(year.getEndYear()).isEqualTo(2025);
        assertThat(year.getFirstSemester()).isEqualTo(Semester.WINTER_QUARTER);
        assertThat(year.getLastSemester()).isEqualTo(Semester.FALL_QUARTER);
        assertThat(year.contains(Semester.FALL)).isFalse();
        assertThat(year.getSemesters()).isSortedAccordingTo((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
    }

    @Test
    void academicYearRange() {
        assertThat(Semester.getAcademicYearRange(2020, 2022, SemesterType.TRADITIONAL))
                .extracting(AcademicYear::getStartYear).containsExactly(2020, 2021, 2022);
        assertThat(Semester.getAcademicYearRange(2022, 2020, SemesterType.TRADITIONAL)).isEmpty();
    }

    @Test
    void sequenceValidationAndSorting() {
        assertThat(Semester.isValidSemesterSequence(List.of())).isTrue();
        assertThat(Semester.isValidSemesterSequence(List.of(Semester.FALL, Semester.SPRING))).isTrue();
        assertThat(Semester.isValidSemesterSequence(List.of(Semester.FALL, Semester.FALL_QUARTER))).isFalse();
        assertThat(Semester.sortSemesters(List.of(Semester.FALL, Semester.SPRING, Semester.SUMMER)))
                .containsExactly(Semester.SPRING, Semester.SUMMER, Semester.FALL);
    }

    @Test
    void toStringFormat() {
        assertThat(Semester.FALL.toString()).isEqualTo("Fall (FALL) - 15 weeks");
        assertThat(Semester.THESIS.toString()).isEqualTo("Thesis (THES)");
    }
}
