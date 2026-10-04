package utils;

import enums.Semester;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilTest {

    @Nested
    class Parsing {
        @Test
        void parseIsoDateAndDateTime() {
            assertThat(DateUtil.parseDate("2024-03-15")).contains(LocalDate.of(2024, 3, 15));
            assertThat(DateUtil.parseDate("2024-13-01")).isEmpty();
            assertThat(DateUtil.parseDate("15/03/2024", DateUtil.US_DATE)).isEmpty();
            assertThat(DateUtil.parseDate("03/15/2024", DateUtil.US_DATE)).contains(LocalDate.of(2024, 3, 15));
            assertThat(DateUtil.parseDateTime("2024-03-15T10:30:00")).contains(LocalDateTime.of(2024, 3, 15, 10, 30));
            assertThat(DateUtil.parseDateTime("2024-03-15 10:30")).isEmpty();
        }

        @Test
        void nullInputYieldsEmptyOptional() {
            assertThat(DateUtil.parseDate(null)).isEmpty();
            assertThat(DateUtil.parseDateTime(null)).isEmpty();
            assertThat(DateUtil.parseFlexibleDate(null)).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "2024-03-15, 2024-03-15",
            "03/15/2024, 2024-03-15",
            "25/12/2024, 2024-12-25",
            "25-12-2024, 2024-12-25",
            "12-25-2024, 2024-12-25",
            "2024/12/25, 2024-12-25"
        })
        void flexibleParsingAcceptsCommonLayouts(String input, LocalDate expected) {
            assertThat(DateUtil.parseFlexibleDate(input)).contains(expected);
        }

        @Test
        void flexibleParsingPrefersUsLayoutForAmbiguousInput() {
            assertThat(DateUtil.parseFlexibleDate("03/04/2024")).contains(LocalDate.of(2024, 3, 4));
            assertThat(DateUtil.parseFlexibleDate("not a date")).isEmpty();
        }
    }

    @Nested
    class Formatting {
        private final LocalDate date = LocalDate.of(2024, 3, 5);

        @Test
        void formats() {
            assertThat(DateUtil.format(date)).isEqualTo("2024-03-05");
            assertThat(DateUtil.formatUS(date)).isEqualTo("03/05/2024");
            assertThat(DateUtil.format(LocalDateTime.of(2024, 3, 5, 14, 7, 9))).isEqualTo("2024-03-05T14:07:09");
            assertThat(DateUtil.format24Hour(LocalTime.of(14, 7))).isEqualTo("14:07");
            assertThat(DateUtil.format12Hour(LocalTime.of(14, 7))).matches("2:07 \\S+");
            assertThat(DateUtil.formatAcademic(date)).matches("\\S+ 05, 2024");
        }

        @Test
        void nullsFormatAsEmpty() {
            assertThat(DateUtil.format((LocalDate) null)).isEmpty();
            assertThat(DateUtil.format((LocalDateTime) null)).isEmpty();
            assertThat(DateUtil.formatAcademic((LocalDate) null)).isEmpty();
            assertThat(DateUtil.format12Hour(null)).isEmpty();
            assertThat(DateUtil.format24Hour(null)).isEmpty();
        }

        @Test
        void formatThenParseRoundTrips() {
            assertThat(DateUtil.parseDate(DateUtil.formatUS(date), DateUtil.US_DATE)).contains(date);
            assertThat(DateUtil.parseDate(DateUtil.format(date))).contains(date);
        }
    }

    @Nested
    class Arithmetic {
        private final LocalDate jan31 = LocalDate.of(2024, 1, 31);

        @Test
        void addAndSubtract() {
            assertThat(DateUtil.addDays(jan31, 1)).isEqualTo(LocalDate.of(2024, 2, 1));
            assertThat(DateUtil.addWeeks(jan31, 1)).isEqualTo(LocalDate.of(2024, 2, 7));
            assertThat(DateUtil.addMonths(jan31, 1)).isEqualTo(LocalDate.of(2024, 2, 29));
            assertThat(DateUtil.addYears(LocalDate.of(2024, 2, 29), 1)).isEqualTo(LocalDate.of(2025, 2, 28));
            assertThat(DateUtil.subtractDays(jan31, 31)).isEqualTo(LocalDate.of(2023, 12, 31));
            assertThat(DateUtil.subtractWeeks(jan31, 2)).isEqualTo(LocalDate.of(2024, 1, 17));
            assertThat(DateUtil.subtractMonths(jan31, 2)).isEqualTo(LocalDate.of(2023, 11, 30));
            assertThat(DateUtil.subtractYears(jan31, 4)).isEqualTo(LocalDate.of(2020, 1, 31));
            assertThat(DateUtil.addDays(null, 1)).isNull();
            assertThat(DateUtil.subtractYears(null, 1)).isNull();
        }

        @Test
        void addThenSubtractIsIdentityForDaysAndWeeks() {
            for (int i = -400; i <= 400; i += 37) {
                assertThat(DateUtil.subtractDays(DateUtil.addDays(jan31, i), i)).isEqualTo(jan31);
                assertThat(DateUtil.subtractWeeks(DateUtil.addWeeks(jan31, i), i)).isEqualTo(jan31);
            }
        }

        @Test
        void betweenCalculations() {
            LocalDate a = LocalDate.of(2020, 2, 29);
            LocalDate b = LocalDate.of(2024, 3, 1);
            assertThat(DateUtil.daysBetween(a, b)).isEqualTo(1462);
            assertThat(DateUtil.daysBetween(b, a)).isEqualTo(-1462);
            assertThat(DateUtil.weeksBetween(a, b)).isEqualTo(208);
            assertThat(DateUtil.monthsBetween(a, b)).isEqualTo(48);
            assertThat(DateUtil.yearsBetween(a, b)).isEqualTo(4);
            assertThat(DateUtil.daysBetween(null, b)).isZero();
        }

        @ParameterizedTest
        @CsvSource({"2000-06-15, 2024-06-14, 23", "2000-06-15, 2024-06-15, 24", "2004-02-29, 2005-02-28, 0", "2004-02-29, 2005-03-01, 1"})
        void calculateAgeAsOf(LocalDate birth, LocalDate asOf, int expected) {
            assertThat(DateUtil.calculateAge(birth, asOf)).isEqualTo(expected);
        }

        @Test
        void calculateAgeNullSafe() {
            assertThat(DateUtil.calculateAge(null)).isZero();
            assertThat(DateUtil.calculateAge(LocalDate.of(2000, 1, 1), null)).isZero();
        }
    }

    @Nested
    class RelativeToToday {
        // These derive their inputs from the same clock DateUtil uses, so they are run-date independent.
        private final LocalDate today = LocalDate.now();

        @Test
        void pastFutureTodayEtc() {
            assertThat(DateUtil.isToday(today)).isTrue();
            assertThat(DateUtil.isPast(today)).isFalse();
            assertThat(DateUtil.isFuture(today)).isFalse();
            assertThat(DateUtil.isPast(today.minusDays(1))).isTrue();
            assertThat(DateUtil.isFuture(today.plusDays(1))).isTrue();
            assertThat(DateUtil.isYesterday(today.minusDays(1))).isTrue();
            assertThat(DateUtil.isTomorrow(today.plusDays(1))).isTrue();
            assertThat(DateUtil.isThisWeek(today)).isTrue();
            assertThat(DateUtil.isThisWeek(today.plusWeeks(1))).isFalse();
            assertThat(DateUtil.isThisMonth(today)).isTrue();
            assertThat(DateUtil.isThisMonth(today.minusYears(1))).isFalse();
            assertThat(DateUtil.isThisYear(today)).isTrue();
            assertThat(DateUtil.isThisYear(today.plusYears(1))).isFalse();
            assertThat(DateUtil.isToday(null)).isFalse();
        }

        @Test
        void currentAcademicPeriodMatchesDateBasedLookup() {
            assertThat(DateUtil.getCurrentAcademicYear()).isEqualTo(DateUtil.getAcademicYearForDate(today));
            assertThat(DateUtil.getCurrentSemester()).isEqualTo(DateUtil.getSemesterForDate(today));
            assertThat(DateUtil.getAcademicYearForDate(null)).isEqualTo(DateUtil.getCurrentAcademicYear());
            assertThat(DateUtil.now(ZoneOffset.UTC)).isNotNull();
        }

        @Test
        void relativeDescription() {
            assertThat(DateUtil.getRelativeDescription(today)).isEqualTo("Today");
            assertThat(DateUtil.getRelativeDescription(today.plusDays(1))).isEqualTo("Tomorrow");
            assertThat(DateUtil.getRelativeDescription(today.minusDays(1))).isEqualTo("Yesterday");
            assertThat(DateUtil.getRelativeDescription(today.plusDays(5))).isEqualTo("In 5 days");
            assertThat(DateUtil.getRelativeDescription(today.minusDays(7))).isEqualTo("7 days ago");
            assertThat(DateUtil.getRelativeDescription(today.plusDays(21))).isEqualTo("In 3 weeks");
            assertThat(DateUtil.getRelativeDescription(today.minusDays(15))).isEqualTo("2 weeks ago");
            LocalDate far = today.plusDays(100);
            assertThat(DateUtil.getRelativeDescription(far)).isEqualTo(DateUtil.formatAcademic(far));
            assertThat(DateUtil.getRelativeDescription(null)).isEqualTo("Unknown");
        }
    }

    @Nested
    class Ranges {
        @Test
        void isBetweenIsInclusive() {
            LocalDate s = LocalDate.of(2024, 1, 1);
            LocalDate e = LocalDate.of(2024, 1, 31);
            assertThat(DateUtil.isBetween(s, s, e)).isTrue();
            assertThat(DateUtil.isBetween(e, s, e)).isTrue();
            assertThat(DateUtil.isBetween(s.minusDays(1), s, e)).isFalse();
            assertThat(DateUtil.isBetween(e.plusDays(1), s, e)).isFalse();
            assertThat(DateUtil.isBetween(null, s, e)).isFalse();
        }

        @ParameterizedTest
        @CsvSource({"2024-03-16, true", "2024-03-17, true", "2024-03-18, false", "2024-03-22, false"})
        void weekend(LocalDate date, boolean weekend) {
            assertThat(DateUtil.isWeekend(date)).isEqualTo(weekend);
            assertThat(DateUtil.isWeekday(date)).isEqualTo(!weekend);
        }

        @Test
        void dateRangesPartitionIntoWeekdaysAndWeekends() {
            LocalDate s = LocalDate.of(2024, 3, 1);
            LocalDate e = LocalDate.of(2024, 3, 31);
            List<LocalDate> all = DateUtil.dateRange(s, e);
            assertThat(all).hasSize(31).startsWith(s).endsWith(e).isSorted();
            assertThat(DateUtil.weekdayRange(s, e)).hasSize(21);
            assertThat(DateUtil.weekendRange(s, e)).hasSize(10);
            assertThat(DateUtil.dateRange(s, s)).containsExactly(s);
            assertThat(DateUtil.dateRange(e, s)).isEmpty();
            assertThat(DateUtil.dateRange(null, s)).isEmpty();
        }

        @Test
        void datesForDayOfWeek() {
            assertThat(DateUtil.getDatesForDayOfWeek(2024, Month.FEBRUARY, DayOfWeek.THURSDAY))
                .containsExactly(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 8), LocalDate.of(2024, 2, 15),
                                 LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 29));
        }
    }

    @Nested
    class AcademicCalendar {
        @Test
        void academicYearBounds() {
            assertThat(DateUtil.getAcademicYearStart(2024)).isEqualTo(LocalDate.of(2024, 8, 15));
            assertThat(DateUtil.getAcademicYearEnd(2024)).isEqualTo(LocalDate.of(2025, 5, 31));
        }

        @ParameterizedTest
        @CsvSource({"2024-08-14, 2023", "2024-08-15, 2024", "2025-01-10, 2024", "2024-12-31, 2024"})
        void academicYearForDate(LocalDate date, int expected) {
            assertThat(DateUtil.getAcademicYearForDate(date)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"2024-01-01, SPRING", "2024-05-31, SPRING", "2024-06-01, SUMMER", "2024-07-31, SUMMER",
                    "2024-08-01, FALL", "2024-12-31, FALL"})
        void semesterForDate(LocalDate date, Semester expected) {
            assertThat(DateUtil.getSemesterForDate(date)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"SPRING, 2024-01-15, 2024-05-15", "SUMMER, 2024-06-01, 2024-08-15", "FALL, 2024-08-15, 2024-12-15"})
        void semesterBoundsAreInsideSemester(Semester semester, LocalDate start, LocalDate end) {
            assertThat(DateUtil.getSemesterStart(semester, 2024)).isEqualTo(start);
            assertThat(DateUtil.getSemesterEnd(semester, 2024)).isEqualTo(end);
            assertThat(DateUtil.isInSemester(start, semester, 2024)).isTrue();
            assertThat(DateUtil.isInSemester(end, semester, 2024)).isTrue();
            assertThat(DateUtil.isInSemester(start.minusDays(1), semester, 2024)).isFalse();
            assertThat(DateUtil.isInSemester(end.plusDays(1), semester, 2024)).isFalse();
            assertThat(DateUtil.isInSemester(null, semester, 2024)).isFalse();
        }
    }

    @Nested
    class Boundaries {
        private final LocalDate wed = LocalDate.of(2024, 2, 14);

        @Test
        void startAndEndOfPeriods() {
            assertThat(DateUtil.startOfDay(wed)).isEqualTo(LocalDateTime.of(2024, 2, 14, 0, 0));
            assertThat(DateUtil.endOfDay(wed)).isEqualTo(LocalDateTime.of(2024, 2, 14, 23, 59, 59));
            assertThat(DateUtil.startOfWeek(wed)).isEqualTo(LocalDate.of(2024, 2, 12));
            assertThat(DateUtil.endOfWeek(wed)).isEqualTo(LocalDate.of(2024, 2, 18));
            assertThat(DateUtil.startOfMonth(wed)).isEqualTo(LocalDate.of(2024, 2, 1));
            assertThat(DateUtil.endOfMonth(wed)).isEqualTo(LocalDate.of(2024, 2, 29));
            assertThat(DateUtil.startOfYear(wed)).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(DateUtil.endOfYear(wed)).isEqualTo(LocalDate.of(2024, 12, 31));
            assertThat(DateUtil.startOfWeek(null)).isNull();
        }

        @Test
        void weekBoundariesOnMondayAndSunday() {
            LocalDate monday = LocalDate.of(2024, 2, 12);
            LocalDate sunday = LocalDate.of(2024, 2, 18);
            assertThat(DateUtil.startOfWeek(monday)).isEqualTo(monday);
            assertThat(DateUtil.endOfWeek(sunday)).isEqualTo(sunday);
        }

        @Test
        void minMax() {
            LocalDate a = LocalDate.of(2024, 1, 1);
            LocalDate b = LocalDate.of(2024, 6, 1);
            assertThat(DateUtil.min(a, b)).isEqualTo(a);
            assertThat(DateUtil.max(a, b)).isEqualTo(b);
            assertThat(DateUtil.min(null, b)).isEqualTo(b);
            assertThat(DateUtil.max(a, null)).isEqualTo(a);
        }

        @ParameterizedTest
        @CsvSource({"2024, true", "2023, false", "1900, false", "2000, true"})
        void leapYears(int year, boolean leap) {
            assertThat(DateUtil.isLeapYear(year)).isEqualTo(leap);
            assertThat(DateUtil.getDaysInYear(year)).isEqualTo(leap ? 366 : 365);
            assertThat(DateUtil.getDaysInMonth(year, Month.FEBRUARY)).isEqualTo(leap ? 29 : 28);
        }

        @ParameterizedTest
        @CsvSource({"1, 1", "3, 1", "4, 2", "6, 2", "7, 3", "9, 3", "10, 4", "12, 4"})
        void quarter(int month, int quarter) {
            assertThat(DateUtil.getQuarter(LocalDate.of(2024, month, 10))).isEqualTo(quarter);
        }

        @Test
        void weekOfYearIsWithinBounds() {
            assertThat(DateUtil.getWeekOfYear(LocalDate.of(2024, 6, 15))).isBetween(1, 53);
            assertThat(DateUtil.getWeekOfYear(null)).isZero();
            assertThat(DateUtil.getQuarter(null)).isZero();
        }
    }

    @Test
    void factoryMethods() {
        assertThat(DateUtil.of(2024, 2, 29)).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(DateUtil.of(2024, Month.MARCH, 1)).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(DateUtil.of(2024, 3, 1, 9, 30)).isEqualTo(LocalDateTime.of(2024, 3, 1, 9, 30));
        assertThat(DateUtil.of(2024, 3, 1, 9, 30, 15)).isEqualTo(LocalDateTime.of(2024, 3, 1, 9, 30, 15));
        assertThat(Optional.of(DateUtil.nowDateTime())).isPresent();
    }
}
