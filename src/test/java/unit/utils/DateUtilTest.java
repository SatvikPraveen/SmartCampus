// File location: src/test/java/unit/utils/DateUtilTest.java

package com.smartcampus.test.unit.utils;

import com.smartcampus.utils.DateUtil;
import com.smartcampus.models.enums.Semester;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Unit tests for the DateUtil class
 * Tests various date and time utility methods
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Date Util Tests")
class DateUtilTest {

    @Nested
    @DisplayName("Date Formatting Tests")
    class DateFormattingTests {

        @Test
        @DisplayName("Should format date as string")
        void shouldFormatDateAsString() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            
            assertThat(DateUtil.formatDate(date, "yyyy-MM-dd")).isEqualTo("2024-08-15");
            assertThat(DateUtil.formatDate(date, "MM/dd/yyyy")).isEqualTo("08/15/2024");
            assertThat(DateUtil.formatDate(date, "dd-MMM-yyyy")).isEqualTo("15-Aug-2024");
        }

        @Test
        @DisplayName("Should format date with default pattern")
        void shouldFormatDateWithDefaultPattern() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            String formatted = DateUtil.formatDate(date);
            
            assertThat(formatted).isEqualTo("2024-08-15");
        }

        @Test
        @DisplayName("Should format datetime as string")
        void shouldFormatDateTimeAsString() {
            LocalDateTime dateTime = LocalDateTime.of(2024, 8, 15, 10, 30, 45);
            
            assertThat(DateUtil.formatDateTime(dateTime, "yyyy-MM-dd HH:mm:ss"))
                .isEqualTo("2024-08-15 10:30:45");
            assertThat(DateUtil.formatDateTime(dateTime, "MM/dd/yyyy hh:mm a"))
                .isEqualTo("08/15/2024 10:30 AM");
        }

        @Test
        @DisplayName("Should format time as string")
        void shouldFormatTimeAsString() {
            LocalTime time = LocalTime.of(14, 30, 0);
            
            assertThat(DateUtil.formatTime(time, "HH:mm")).isEqualTo("14:30");
            assertThat(DateUtil.formatTime(time, "hh:mm a")).isEqualTo("02:30 PM");
            assertThat(DateUtil.formatTime(time, "HH:mm:ss")).isEqualTo("14:30:00");
        }

        @Test
        @DisplayName("Should handle null dates in formatting")
        void shouldHandleNullDatesInFormatting() {
            assertThat(DateUtil.formatDate(null)).isNull();
            assertThat(DateUtil.formatDateTime(null)).isNull();
            assertThat(DateUtil.formatTime(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {

        @Test
        @DisplayName("Should parse date from string")
        void shouldParseDateFromString() {
            LocalDate expected = LocalDate.of(2024, 8, 15);
            
            assertThat(DateUtil.parseDate("2024-08-15", "yyyy-MM-dd")).isEqualTo(expected);
            assertThat(DateUtil.parseDate("08/15/2024", "MM/dd/yyyy")).isEqualTo(expected);
            assertThat(DateUtil.parseDate("15-Aug-2024", "dd-MMM-yyyy")).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should parse date with default pattern")
        void shouldParseDateWithDefaultPattern() {
            LocalDate expected = LocalDate.of(2024, 8, 15);
            LocalDate actual = DateUtil.parseDate("2024-08-15");
            
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should parse datetime from string")
        void shouldParseDateTimeFromString() {
            LocalDateTime expected = LocalDateTime.of(2024, 8, 15, 10, 30, 45);
            
            assertThat(DateUtil.parseDateTime("2024-08-15 10:30:45", "yyyy-MM-dd HH:mm:ss"))
                .isEqualTo(expected);
            assertThat(DateUtil.parseDateTime("08/15/2024 10:30:45 AM", "MM/dd/yyyy hh:mm:ss a"))
                .isEqualTo(expected);
        }

        @Test
        @DisplayName("Should parse time from string")
        void shouldParseTimeFromString() {
            LocalTime expected = LocalTime.of(14, 30, 0);
            
            assertThat(DateUtil.parseTime("14:30", "HH:mm")).isEqualTo(expected);
            assertThat(DateUtil.parseTime("02:30 PM", "hh:mm a")).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle invalid date strings")
        void shouldHandleInvalidDateStrings() {
            assertThrows(RuntimeException.class, () -> {
                DateUtil.parseDate("invalid-date", "yyyy-MM-dd");
            });
            
            assertThrows(RuntimeException.class, () -> {
                DateUtil.parseDate("2024-13-01", "yyyy-MM-dd"); // Invalid month
            });
        }
    }

    @Nested
    @DisplayName("Date Calculation Tests")
    class DateCalculationTests {

        @Test
        @DisplayName("Should add days to date")
        void shouldAddDaysToDate() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            LocalDate expected = LocalDate.of(2024, 8, 20);
            
            assertThat(DateUtil.addDays(date, 5)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should subtract days from date")
        void shouldSubtractDaysFromDate() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            LocalDate expected = LocalDate.of(2024, 8, 10);
            
            assertThat(DateUtil.addDays(date, -5)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should add weeks to date")
        void shouldAddWeeksToDate() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            LocalDate expected = LocalDate.of(2024, 8, 29);
            
            assertThat(DateUtil.addWeeks(date, 2)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should add months to date")
        void shouldAddMonthsToDate() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            LocalDate expected = LocalDate.of(2024, 10, 15);
            
            assertThat(DateUtil.addMonths(date, 2)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should add years to date")
        void shouldAddYearsToDate() {
            LocalDate date = LocalDate.of(2024, 8, 15);
            LocalDate expected = LocalDate.of(2026, 8, 15);
            
            assertThat(DateUtil.addYears(date, 2)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should calculate days between dates")
        void shouldCalculateDaysBetweenDates() {
            LocalDate startDate = LocalDate.of(2024, 8, 15);
            LocalDate endDate = LocalDate.of(2024, 8, 25);
            
            assertThat(DateUtil.daysBetween(startDate, endDate)).isEqualTo(10);
            assertThat(DateUtil.daysBetween(endDate, startDate)).isEqualTo(-10);
        }

        @Test
        @DisplayName("Should calculate weeks between dates")
        void shouldCalculateWeeksBetweenDates() {
            LocalDate startDate = LocalDate.of(2024, 8, 1);
            LocalDate endDate = LocalDate.of(2024, 8, 15);
            
            assertThat(DateUtil.weeksBetween(startDate, endDate)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate months between dates")
        void shouldCalculateMonthsBetweenDates() {
            LocalDate startDate = LocalDate.of(2024, 6, 15);
            LocalDate endDate = LocalDate.of(2024, 8, 15);
            
            assertThat(DateUtil.monthsBetween(startDate, endDate)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate years between dates")
        void shouldCalculateYearsBetweenDates() {
            LocalDate startDate = LocalDate.of(2020, 8, 15);
            LocalDate endDate = LocalDate.of(2024, 8, 15);
            
            assertThat(DateUtil.yearsBetween(startDate, endDate)).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Date Comparison Tests")
    class DateComparisonTests {

        @Test
        @DisplayName("Should check if date is before another date")
        void shouldCheckIfDateIsBeforeAnotherDate() {
            LocalDate date1 = LocalDate.of(2024, 8, 15);
            LocalDate date2 = LocalDate.of(2024, 8, 20);
            
            assertTrue(DateUtil.isBefore(date1, date2));
            assertFalse(DateUtil.isBefore(date2, date1));
            assertFalse(DateUtil.isBefore(date1, date1));
        }

        @Test
        @DisplayName("Should check if date is after another date")
        void shouldCheckIfDateIsAfterAnotherDate() {
            LocalDate date1 = LocalDate.of(2024, 8, 15);
            LocalDate date2 = LocalDate.of(2024, 8, 20);
            
            assertFalse(DateUtil.isAfter(date1, date2));
            assertTrue(DateUtil.isAfter(date2, date1));
            assertFalse(DateUtil.isAfter(date1, date1));
        }

        @Test
        @DisplayName("Should check if date is between two dates")
        void shouldCheckIfDateIsBetweenTwoDates() {
            LocalDate startDate = LocalDate.of(2024, 8, 10);
            LocalDate testDate = LocalDate.of(2024, 8, 15);
            LocalDate endDate = LocalDate.of(2024, 8, 20);
            
            assertTrue(DateUtil.isBetween(testDate, startDate, endDate));
            assertTrue(DateUtil.isBetween(startDate, startDate, endDate)); // inclusive
            assertTrue(DateUtil.isBetween(endDate, startDate, endDate)); // inclusive
            assertFalse(DateUtil.isBetween(LocalDate.of(2024, 8, 25), startDate, endDate));
        }

        @Test
        @DisplayName("Should check if date is in the past")
        void shouldCheckIfDateIsInThePast() {
            LocalDate pastDate = LocalDate.now().minusDays(1);
            LocalDate futureDate = LocalDate.now().plusDays(1);
            LocalDate today = LocalDate.now();
            
            assertTrue(DateUtil.isInPast(pastDate));
            assertFalse(DateUtil.isInPast(futureDate));
            assertFalse(DateUtil.isInPast(today));
        }

        @Test
        @DisplayName("Should check if date is in the future")
        void shouldCheckIfDateIsInTheFuture() {
            LocalDate pastDate = LocalDate.now().minusDays(1);
            LocalDate futureDate = LocalDate.now().plusDays(1);
            LocalDate today = LocalDate.now();
            
            assertFalse(DateUtil.isInFuture(pastDate));
            assertTrue(DateUtil.isInFuture(futureDate));
            assertFalse(DateUtil.isInFuture(today));
        }

        @Test
        @DisplayName("Should check if date is today")
        void shouldCheckIfDateIsToday() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            
            assertTrue(DateUtil.isToday(today));
            assertFalse(DateUtil.isToday(yesterday));
            assertFalse(DateUtil.isToday(tomorrow));
        }
    }

    @Nested
    @DisplayName("Academic Calendar Tests")
    class AcademicCalendarTests {

        @Test
        @DisplayName("Should determine current academic year")
        void shouldDetermineCurrentAcademicYear() {
            // Academic year typically starts in August/September
            LocalDate dateInFall = LocalDate.of(2024, 9, 15);
            LocalDate dateInSpring = LocalDate.of(2025, 3, 15);
            
            assertThat(DateUtil.getAcademicYear(dateInFall)).isEqualTo(2024);
            assertThat(DateUtil.getAcademicYear(dateInSpring)).isEqualTo(2024); // Still 2024 academic year
        }

        @Test
        @DisplayName("Should determine semester from date")
        void shouldDetermineSemesterFromDate() {
            LocalDate fallDate = LocalDate.of(2024, 9, 15);
            LocalDate springDate = LocalDate.of(2025, 2, 15);
            LocalDate summerDate = LocalDate.of(2024, 7, 15);
            
            assertThat(DateUtil.getSemesterFromDate(fallDate)).isEqualTo(Semester.FALL);
            assertThat(DateUtil.getSemesterFromDate(springDate)).isEqualTo(Semester.SPRING);
            assertThat(DateUtil.getSemesterFromDate(summerDate)).isEqualTo(Semester.SUMMER);
        }

        @ParameterizedTest
        @DisplayName("Should get semester start date")
        @EnumSource(Semester.class)
        void shouldGetSemesterStartDate(Semester semester) {
            LocalDate startDate = DateUtil.getSemesterStartDate(semester, 2024);
            
            assertThat(startDate).isNotNull();
            assertThat(startDate.getYear()).isEqualTo(2024);
        }

        @ParameterizedTest
        @DisplayName("Should get semester end date")
        @EnumSource(Semester.class)
        void shouldGetSemesterEndDate(Semester semester) {
            LocalDate endDate = DateUtil.getSemesterEndDate(semester, 2024);
            
            assertThat(endDate).isNotNull();
            if (semester == Semester.SPRING) {
                assertThat(endDate.getYear()).isEqualTo(2025); // Spring semester ends next calendar year
            } else {
                assertThat(endDate.getYear()).isEqualTo(2024);
            }
        }

        @Test
        @DisplayName("Should calculate semester duration")
        void shouldCalculateSemesterDuration() {
            LocalDate startDate = DateUtil.getSemesterStartDate(Semester.FALL, 2024);
            LocalDate endDate = DateUtil.getSemesterEndDate(Semester.FALL, 2024);
            
            long duration = DateUtil.daysBetween(startDate, endDate);
            assertThat(duration).isGreaterThan(90); // Semester should be at least 3 months
            assertThat(duration).isLessThan(150); // But less than 5 months
        }

        @Test
        @DisplayName("Should check if date is within academic year")
        void shouldCheckIfDateIsWithinAcademicYear() {
            LocalDate fallDate = LocalDate.of(2024, 9, 15);
            LocalDate springDate = LocalDate.of(2025, 2, 15);
            LocalDate summerBeforeDate = LocalDate.of(2024, 7, 15);
            
            assertTrue(DateUtil.isWithinAcademicYear(fallDate, 2024));
            assertTrue(DateUtil.isWithinAcademicYear(springDate, 2024));
            assertFalse(DateUtil.isWithinAcademicYear(summerBeforeDate, 2024));
        }

        @Test
        @DisplayName("Should get registration deadline dates")
        void shouldGetRegistrationDeadlineDates() {
            LocalDate fallRegistration = DateUtil.getRegistrationDeadline(Semester.FALL, 2024);
            LocalDate springRegistration = DateUtil.getRegistrationDeadline(Semester.SPRING, 2024);
            
            assertThat(fallRegistration).isNotNull();
            assertThat(springRegistration).isNotNull();
            
            // Registration should be before semester starts
            assertTrue(DateUtil.isBefore(fallRegistration, DateUtil.getSemesterStartDate(Semester.FALL, 2024)));
            assertTrue(DateUtil.isBefore(springRegistration, DateUtil.getSemesterStartDate(Semester.SPRING, 2024)));
        }
    }

    @Nested
    @DisplayName("Weekday and Weekend Tests")
    class WeekdayAndWeekendTests {

        @Test
        @DisplayName("Should identify weekdays")
        void shouldIdentifyWeekdays() {
            LocalDate monday = LocalDate.of(2024, 8, 12); // Monday
            LocalDate tuesday = LocalDate.of(2024, 8, 13); // Tuesday
            LocalDate wednesday = LocalDate.of(2024, 8, 14); // Wednesday
            LocalDate thursday = LocalDate.of(2024, 8, 15); // Thursday
            LocalDate friday = LocalDate.of(2024, 8, 16); // Friday
            
            assertTrue(DateUtil.isWeekday(monday));
            assertTrue(DateUtil.isWeekday(tuesday));
            assertTrue(DateUtil.isWeekday(wednesday));
            assertTrue(DateUtil.isWeekday(thursday));
            assertTrue(DateUtil.isWeekday(friday));
        }

        @Test
        @DisplayName("Should identify weekends")
        void shouldIdentifyWeekends() {
            LocalDate saturday = LocalDate.of(2024, 8, 17); // Saturday
            LocalDate sunday = LocalDate.of(2024, 8, 18); // Sunday
            
            assertTrue(DateUtil.isWeekend(saturday));
            assertTrue(DateUtil.isWeekend(sunday));
            assertFalse(DateUtil.isWeekend(LocalDate.of(2024, 8, 16))); // Friday
        }

        @Test
        @DisplayName("Should get next weekday")
        void shouldGetNextWeekday() {
            LocalDate friday = LocalDate.of(2024, 8, 16); // Friday
            LocalDate saturday = LocalDate.of(2024, 8, 17); // Saturday
            LocalDate sunday = LocalDate.of(2024, 8, 18); // Sunday
            LocalDate monday = LocalDate.of(2024, 8, 19); // Monday
            
            assertThat(DateUtil.getNextWeekday(friday)).isEqualTo(monday);
            assertThat(DateUtil.getNextWeekday(saturday)).isEqualTo(monday);
            assertThat(DateUtil.getNextWeekday(sunday)).isEqualTo(monday);
            assertThat(DateUtil.getNextWeekday(LocalDate.of(2024, 8, 12))).isEqualTo(LocalDate.of(2024, 8, 13)); // Monday to Tuesday
        }

        @Test
        @DisplayName("Should get previous weekday")
        void shouldGetPreviousWeekday() {
            LocalDate monday = LocalDate.of(2024, 8, 19); // Monday
            LocalDate saturday = LocalDate.of(2024, 8, 17); // Saturday
            LocalDate sunday = LocalDate.of(2024, 8, 18); // Sunday
            LocalDate previousFriday = LocalDate.of(2024, 8, 16); // Previous Friday
            
            assertThat(DateUtil.getPreviousWeekday(monday)).isEqualTo(previousFriday);
            assertThat(DateUtil.getPreviousWeekday(saturday)).isEqualTo(previousFriday);
            assertThat(DateUtil.getPreviousWeekday(sunday)).isEqualTo(previousFriday);
        }

        @Test
        @DisplayName("Should count weekdays between dates")
        void shouldCountWeekdaysBetweenDates() {
            LocalDate startDate = LocalDate.of(2024, 8, 12); // Monday
            LocalDate endDate = LocalDate.of(2024, 8, 16); // Friday
            
            assertThat(DateUtil.countWeekdays(startDate, endDate)).isEqualTo(5); // Mon, Tue, Wed, Thu, Fri
            
            LocalDate startWithWeekend = LocalDate.of(2024, 8, 10); // Saturday
            LocalDate endWithWeekend = LocalDate.of(2024, 8, 18); // Sunday
            
            assertThat(DateUtil.countWeekdays(startWithWeekend, endWithWeekend)).isEqualTo(5); // Only weekdays counted
        }

        @Test
        @DisplayName("Should get weekdays in date range")
        void shouldGetWeekdaysInDateRange() {
            LocalDate startDate = LocalDate.of(2024, 8, 15); // Thursday
            LocalDate endDate = LocalDate.of(2024, 8, 20); // Tuesday
            
            List<LocalDate> weekdays = DateUtil.getWeekdaysInRange(startDate, endDate);
            
            assertThat(weekdays).hasSize(4); // Thu, Fri, Mon, Tue
            assertThat(weekdays).contains(
                LocalDate.of(2024, 8, 15), // Thursday
                LocalDate.of(2024, 8, 16), // Friday
                LocalDate.of(2024, 8, 19), // Monday
                LocalDate.of(2024, 8, 20)  // Tuesday
            );
        }
    }

    @Nested
    @DisplayName("Holiday and Break Tests")
    class HolidayAndBreakTests {

        @Test
        @DisplayName("Should identify common holidays")
        void shouldIdentifyCommonHolidays() {
            LocalDate newYearsDay = LocalDate.of(2024, 1, 1);
            LocalDate christmas = LocalDate.of(2024, 12, 25);
            LocalDate independenceDay = LocalDate.of(2024, 7, 4);
            
            assertTrue(DateUtil.isHoliday(newYearsDay));
            assertTrue(DateUtil.isHoliday(christmas));
            assertTrue(DateUtil.isHoliday(independenceDay));
            assertFalse(DateUtil.isHoliday(LocalDate.of(2024, 8, 15))); // Random date
        }

        @Test
        @DisplayName("Should get holiday list for year")
        void shouldGetHolidayListForYear() {
            List<LocalDate> holidays2024 = DateUtil.getHolidays(2024);
            
            assertThat(holidays2024).isNotEmpty();
            assertThat(holidays2024).contains(
                LocalDate.of(2024, 1, 1), // New Year's Day
                LocalDate.of(2024, 7, 4), // Independence Day
                LocalDate.of(2024, 12, 25) // Christmas
            );
        }

        @Test
        @DisplayName("Should calculate business days excluding holidays")
        void shouldCalculateBusinessDaysExcludingHolidays() {
            LocalDate startDate = LocalDate.of(2024, 12, 23); // Monday before Christmas
            LocalDate endDate = LocalDate.of(2024, 12, 27); // Friday after Christmas
            
            // Should exclude Christmas Day (Dec 25) from business day count
            int businessDays = DateUtil.countBusinessDays(startDate, endDate);
            assertThat(businessDays).isLessThan(5); // Less than full week due to holiday
        }

        @Test
        @DisplayName("Should check if date is during academic break")
        void shouldCheckIfDateIsDuringAcademicBreak() {
            LocalDate winterBreak = LocalDate.of(2024, 12, 25);
            LocalDate springBreak = LocalDate.of(2024, 3, 15);
            LocalDate summerBreak = LocalDate.of(2024, 7, 15);
            LocalDate regularSemester = LocalDate.of(2024, 10, 15);
            
            assertTrue(DateUtil.isAcademicBreak(winterBreak));
            assertTrue(DateUtil.isAcademicBreak(springBreak));
            assertTrue(DateUtil.isAcademicBreak(summerBreak));
            assertFalse(DateUtil.isAcademicBreak(regularSemester));
        }
    }

    @Nested
    @DisplayName("Age and Birth Date Tests")
    class AgeAndBirthDateTests {

        @Test
        @DisplayName("Should calculate age from birth date")
        void shouldCalculateAgeFromBirthDate() {
            LocalDate birthDate = LocalDate.of(2000, 8, 15);
            LocalDate referenceDate = LocalDate.of(2024, 8, 15); // Exact birthday
            
            assertThat(DateUtil.calculateAge(birthDate, referenceDate)).isEqualTo(24);
            
            // Before birthday this year
            LocalDate beforeBirthday = LocalDate.of(2024, 8, 14);
            assertThat(DateUtil.calculateAge(birthDate, beforeBirthday)).isEqualTo(23);
            
            // After birthday this year
            LocalDate afterBirthday = LocalDate.of(2024, 8, 16);
            assertThat(DateUtil.calculateAge(birthDate, afterBirthday)).isEqualTo(24);
        }

        @Test
        @DisplayName("Should calculate current age")
        void shouldCalculateCurrentAge() {
            LocalDate birthDate = LocalDate.now().minusYears(20);
            int age = DateUtil.calculateCurrentAge(birthDate);
            
            assertThat(age).isBetween(19, 20); // Depending on whether birthday has occurred this year
        }

        @Test
        @DisplayName("Should check if person is adult")
        void shouldCheckIfPersonIsAdult() {
            LocalDate adultBirthDate = LocalDate.now().minusYears(25);
            LocalDate minorBirthDate = LocalDate.now().minusYears(16);
            
            assertTrue(DateUtil.isAdult(adultBirthDate));
            assertFalse(DateUtil.isAdult(minorBirthDate));
        }

        @Test
        @DisplayName("Should get birth year from age")
        void shouldGetBirthYearFromAge() {
            int currentYear = LocalDate.now().getYear();
            int age = 20;
            
            int birthYear = DateUtil.getBirthYearFromAge(age);
            assertThat(birthYear).isBetween(currentYear - age - 1, currentYear - age);
        }
    }

    @Nested
    @DisplayName("Time Zone and Conversion Tests")
    class TimeZoneAndConversionTests {

        @Test
        @DisplayName("Should convert between time zones")
        void shouldConvertBetweenTimeZones() {
            ZonedDateTime easternTime = ZonedDateTime.of(2024, 8, 15, 14, 30, 0, 0, 
                ZoneId.of("America/New_York"));
            
            ZonedDateTime pacificTime = DateUtil.convertTimeZone(easternTime, ZoneId.of("America/Los_Angeles"));
            
            assertThat(pacificTime.getHour()).isEqualTo(11); // 3 hours behind
            assertThat(pacificTime.getMinute()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should get current time in different zones")
        void shouldGetCurrentTimeInDifferentZones() {
            ZonedDateTime utc = DateUtil.getCurrentTimeInZone(ZoneId.of("UTC"));
            ZonedDateTime eastern = DateUtil.getCurrentTimeInZone(ZoneId.of("America/New_York"));
            ZonedDateTime pacific = DateUtil.getCurrentTimeInZone(ZoneId.of("America/Los_Angeles"));
            
            assertThat(utc).isNotNull();
            assertThat(eastern).isNotNull();
            assertThat(pacific).isNotNull();
            
            // Verify time zone differences (approximately)
            long hoursDiff = Duration.between(pacific.toInstant(), eastern.toInstant()).toHours();
            assertThat(Math.abs(hoursDiff)).isLessThanOrEqualTo(3); // Account for daylight saving
        }

        @Test
        @DisplayName("Should convert local date to UTC")
        void shouldConvertLocalDateToUtc() {
            LocalDateTime localDateTime = LocalDateTime.of(2024, 8, 15, 14, 30, 0);
            ZoneId localZone = ZoneId.of("America/New_York");
            
            ZonedDateTime utc = DateUtil.toUtc(localDateTime, localZone);
            
            assertThat(utc.getZone()).isEqualTo(ZoneId.of("UTC"));
        }
    }

    @Nested
    @DisplayName("Schedule and Recurrence Tests")
    class ScheduleAndRecurrenceTests {

        @Test
        @DisplayName("Should generate dates for weekly recurrence")
        void shouldGenerateDatesForWeeklyRecurrence() {
            LocalDate startDate = LocalDate.of(2024, 8, 15); // Thursday
            LocalDate endDate = LocalDate.of(2024, 9, 15);
            
            List<LocalDate> weeklyDates = DateUtil.generateWeeklyDates(startDate, endDate, DayOfWeek.THURSDAY);
            
            assertThat(weeklyDates).isNotEmpty();
            assertThat(weeklyDates.get(0)).isEqualTo(startDate);
            
            // Verify all dates are Thursdays
            weeklyDates.forEach(date -> 
                assertThat(date.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY)
            );
        }

        @Test
        @DisplayName("Should generate dates for MWF pattern")
        void shouldGenerateDatesForMwfPattern() {
            LocalDate startDate = LocalDate.of(2024, 8, 12); // Monday
            LocalDate endDate = LocalDate.of(2024, 8, 23); // Friday of next week
            
            List<LocalDate> mwfDates = DateUtil.generateMwfDates(startDate, endDate);
            
            assertThat(mwfDates).isNotEmpty();
            
            // Verify only Monday, Wednesday, Friday dates
            mwfDates.forEach(date -> {
                DayOfWeek day = date.getDayOfWeek();
                assertTrue(day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY || day == DayOfWeek.FRIDAY);
            });
        }

        @Test
        @DisplayName("Should generate dates for TTh pattern")
        void shouldGenerateDatesForTthPattern() {
            LocalDate startDate = LocalDate.of(2024, 8, 13); // Tuesday
            LocalDate endDate = LocalDate.of(2024, 8, 29); // Thursday of next week
            
            List<LocalDate> tthDates = DateUtil.generateTthDates(startDate, endDate);
            
            assertThat(tthDates).isNotEmpty();
            
            // Verify only Tuesday, Thursday dates
            tthDates.forEach(date -> {
                DayOfWeek day = date.getDayOfWeek();
                assertTrue(day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY);
            });
        }

        @Test
        @DisplayName("Should calculate class meeting dates for semester")
        void shouldCalculateClassMeetingDatesForSemester() {
            LocalDate semesterStart = LocalDate.of(2024, 8, 26);
            LocalDate semesterEnd = LocalDate.of(2024, 12, 15);
            String schedule = "MWF";
            
            List<LocalDate> classDates = DateUtil.getClassMeetingDates(semesterStart, semesterEnd, schedule);
            
            assertThat(classDates).isNotEmpty();
            assertThat(classDates.size()).isGreaterThan(30); // Should have many class meetings
            
            // Verify dates are within semester and match schedule
            classDates.forEach(date -> {
                assertTrue(DateUtil.isBetween(date, semesterStart, semesterEnd));
                DayOfWeek day = date.getDayOfWeek();
                assertTrue(day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY || day == DayOfWeek.FRIDAY);
            });
        }
    }

    @Nested
    @DisplayName("Validation and Edge Cases Tests")
    class ValidationAndEdgeCasesTests {

        @Test
        @DisplayName("Should handle leap year calculations")
        void shouldHandleLeapYearCalculations() {
            assertTrue(DateUtil.isLeapYear(2024)); // Leap year
            assertFalse(DateUtil.isLeapYear(2023)); // Not leap year
            assertFalse(DateUtil.isLeapYear(1900)); // Not leap year (century rule)
            assertTrue(DateUtil.isLeapYear(2000)); // Leap year (400 rule)
        }

        @Test
        @DisplayName("Should get last day of month")
        void shouldGetLastDayOfMonth() {
            assertThat(DateUtil.getLastDayOfMonth(2024, 2)).isEqualTo(29); // Leap year February
            assertThat(DateUtil.getLastDayOfMonth(2023, 2)).isEqualTo(28); // Regular February
            assertThat(DateUtil.getLastDayOfMonth(2024, 4)).isEqualTo(30); // April
            assertThat(DateUtil.getLastDayOfMonth(2024, 8)).isEqualTo(31); // August
        }

        @Test
        @DisplayName("Should handle month boundaries")
        void shouldHandleMonthBoundaries() {
            LocalDate endOfJanuary = LocalDate.of(2024, 1, 31);
            LocalDate startOfFebruary = LocalDate.of(2024, 2, 1);
            
            assertThat(DateUtil.addDays(endOfJanuary, 1)).isEqualTo(startOfFebruary);
            assertThat(DateUtil.addDays(startOfFebruary, -1)).isEqualTo(endOfJanuary);
        }

        @Test
        @DisplayName("Should handle year boundaries")
        void shouldHandleYearBoundaries() {
            LocalDate endOf2023 = LocalDate.of(2023, 12, 31);
            LocalDate startOf2024 = LocalDate.of(2024, 1, 1);
            
            assertThat(DateUtil.addDays(endOf2023, 1)).isEqualTo(startOf2024);
            assertThat(DateUtil.addYears(endOf2023, 1)).isEqualTo(LocalDate.of(2024, 12, 31));
        }

        @Test
        @DisplayName("Should handle null date inputs")
        void shouldHandleNullDateInputs() {
            assertNull(DateUtil.formatDate(null));
            assertNull(DateUtil.addDays(null, 5));
            assertNull(DateUtil.addMonths(null, 2));
            
            assertThrows(IllegalArgumentException.class, () -> {
                DateUtil.daysBetween(null, LocalDate.now());
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                DateUtil.daysBetween(LocalDate.now(), null);
            });
        }

        @Test
        @DisplayName("Should validate date ranges")
        void shouldValidateDateRanges() {
            LocalDate startDate = LocalDate.of(2024, 8, 15);
            LocalDate endDate = LocalDate.of(2024, 8, 10);
            
            assertThrows(IllegalArgumentException.class, () -> {
                DateUtil.validateDateRange(startDate, endDate); // End before start
            });
            
            assertDoesNotThrow(() -> {
                DateUtil.validateDateRange(LocalDate.of(2024, 8, 10), LocalDate.of(2024, 8, 15));
            });
        }
    }

    @Nested
    @DisplayName("Performance and Utility Tests")
    class PerformanceAndUtilityTests {

        @Test
        @DisplayName("Should efficiently generate large date ranges")
        void shouldEfficientlyGenerateLargeDateRanges() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);
            
            long startTime = System.currentTimeMillis();
            List<LocalDate> allDates = DateUtil.getDateRange(startDate, endDate);
            long endTime = System.currentTimeMillis();
            
            assertThat(allDates).hasSize(366); // 2024 is a leap year
            assertThat(endTime - startTime).isLessThan(100); // Should be fast
        }

        @Test
        @DisplayName("Should cache frequently used calculations")
        void shouldCacheFrequentlyUsedCalculations() {
            // Multiple calls to same calculation should be fast due to caching
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                DateUtil.getHolidays(2024);
                DateUtil.isLeapYear(2024);
                DateUtil.getSemesterStartDate(Semester.FALL, 2024);
            }
            long endTime = System.currentTimeMillis();
            
            // Should complete quickly due to caching
            assertThat(endTime - startTime).isLessThan(50);
        }

        @Test
        @DisplayName("Should provide consistent results across calls")
        void shouldProvideConsistentResultsAcrossCalls() {
            LocalDate testDate = LocalDate.of(2024, 8, 15);
            
            // Multiple calls should return same results
            for (int i = 0; i < 10; i++) {
                assertThat(DateUtil.isWeekday(testDate)).isTrue();
                assertThat(DateUtil.getDayOfWeek(testDate)).isEqualTo(DayOfWeek.THURSDAY);
                assertThat(DateUtil.getAcademicYear(testDate)).isEqualTo(2024);
            }
        }
    }
}