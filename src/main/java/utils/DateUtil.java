// File location: src/main/java/utils/DateUtil.java
package utils;

import enums.Semester;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for date and time operations
 * Provides academic calendar support and common date manipulations
 */
public final class DateUtil {
    
    private DateUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== CONSTANTS ====================
    
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter US_DATETIME = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    public static final DateTimeFormatter ACADEMIC_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    public static final DateTimeFormatter ACADEMIC_DATETIME = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' h:mm a");
    public static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter TIME_12_HOUR = DateTimeFormatter.ofPattern("h:mm a");
    
    // Academic year typically starts in August
    public static final Month ACADEMIC_YEAR_START = Month.AUGUST;
    public static final int ACADEMIC_YEAR_START_DAY = 15;
    
    // ==================== CURRENT DATE/TIME ====================
    
    /**
     * Gets current date
     */
    public static LocalDate now() {
        return LocalDate.now();
    }
    
    /**
     * Gets current date in specific timezone
     */
    public static LocalDate now(ZoneId zoneId) {
        return LocalDate.now(zoneId);
    }
    
    /**
     * Gets current datetime
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Gets current datetime in specific timezone
     */
    public static LocalDateTime nowDateTime(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }
    
    /**
     * Gets current time
     */
    public static LocalTime nowTime() {
        return LocalTime.now();
    }
    
    /**
     * Gets current academic year
     */
    public static int getCurrentAcademicYear() {
        LocalDate now = now();
        LocalDate academicYearStart = LocalDate.of(now.getYear(), ACADEMIC_YEAR_START, ACADEMIC_YEAR_START_DAY);
        
        if (now.isBefore(academicYearStart)) {
            return now.getYear() - 1;
        } else {
            return now.getYear();
        }
    }
    
    /**
     * Gets current semester based on date
     */
    public static Semester getCurrentSemester() {
        LocalDate now = now();
        Month month = now.getMonth();
        
        switch (month) {
            case JANUARY: case FEBRUARY: case MARCH: case APRIL: case MAY:
                return Semester.SPRING;
            case JUNE: case JULY:
                return Semester.SUMMER;
            case AUGUST: case SEPTEMBER: case OCTOBER: case NOVEMBER: case DECEMBER:
                return Semester.FALL;
            default:
                return Semester.FALL;
        }
    }
    
    // ==================== DATE CREATION ====================
    
    /**
     * Creates date from year, month, day
     */
    public static LocalDate of(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }
    
    /**
     * Creates date from year, month enum, day
     */
    public static LocalDate of(int year, Month month, int day) {
        return LocalDate.of(year, month, day);
    }
    
    /**
     * Creates datetime from components
     */
    public static LocalDateTime of(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }
    
    /**
     * Creates datetime from components with seconds
     */
    public static LocalDateTime of(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }
    
    /**
     * Creates date from string with default format
     */
    public static Optional<LocalDate> parseDate(String dateString) {
        return parseDate(dateString, ISO_DATE);
    }
    
    /**
     * Creates date from string with custom format
     */
    public static Optional<LocalDate> parseDate(String dateString, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDate.parse(dateString, formatter));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Creates datetime from string with default format
     */
    public static Optional<LocalDateTime> parseDateTime(String dateTimeString) {
        return parseDateTime(dateTimeString, ISO_DATETIME);
    }
    
    /**
     * Creates datetime from string with custom format
     */
    public static Optional<LocalDateTime> parseDateTime(String dateTimeString, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(dateTimeString, formatter));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Tries to parse date with multiple formats
     */
    public static Optional<LocalDate> parseFlexibleDate(String dateString) {
        List<DateTimeFormatter> formatters = Arrays.asList(
            ISO_DATE,
            US_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );
        
        for (DateTimeFormatter formatter : formatters) {
            Optional<LocalDate> result = parseDate(dateString, formatter);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
    
    // ==================== DATE FORMATTING ====================
    
    /**
     * Formats date with default format
     */
    public static String format(LocalDate date) {
        return date == null ? "" : date.format(ISO_DATE);
    }
    
    /**
     * Formats date with custom format
     */
    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date == null ? "" : date.format(formatter);
    }
    
    /**
     * Formats datetime with default format
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(ISO_DATETIME);
    }
    
    /**
     * Formats datetime with custom format
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime == null ? "" : dateTime.format(formatter);
    }
    
    /**
     * Formats date for academic display
     */
    public static String formatAcademic(LocalDate date) {
        return format(date, ACADEMIC_DATE);
    }
    
    /**
     * Formats datetime for academic display
     */
    public static String formatAcademic(LocalDateTime dateTime) {
        return format(dateTime, ACADEMIC_DATETIME);
    }
    
    /**
     * Formats date as US format
     */
    public static String formatUS(LocalDate date) {
        return format(date, US_DATE);
    }
    
    /**
     * Formats time as 12-hour format
     */
    public static String format12Hour(LocalTime time) {
        return time == null ? "" : time.format(TIME_12_HOUR);
    }
    
    /**
     * Formats time as 24-hour format
     */
    public static String format24Hour(LocalTime time) {
        return time == null ? "" : time.format(TIME_ONLY);
    }
    
    // ==================== DATE CALCULATIONS ====================
    
    /**
     * Adds days to date
     */
    public static LocalDate addDays(LocalDate date, long days) {
        return date == null ? null : date.plusDays(days);
    }
    
    /**
     * Adds weeks to date
     */
    public static LocalDate addWeeks(LocalDate date, long weeks) {
        return date == null ? null : date.plusWeeks(weeks);
    }
    
    /**
     * Adds months to date
     */
    public static LocalDate addMonths(LocalDate date, long months) {
        return date == null ? null : date.plusMonths(months);
    }
    
    /**
     * Adds years to date
     */
    public static LocalDate addYears(LocalDate date, long years) {
        return date == null ? null : date.plusYears(years);
    }
    
    /**
     * Subtracts days from date
     */
    public static LocalDate subtractDays(LocalDate date, long days) {
        return date == null ? null : date.minusDays(days);
    }
    
    /**
     * Subtracts weeks from date
     */
    public static LocalDate subtractWeeks(LocalDate date, long weeks) {
        return date == null ? null : date.minusWeeks(weeks);
    }
    
    /**
     * Subtracts months from date
     */
    public static LocalDate subtractMonths(LocalDate date, long months) {
        return date == null ? null : date.minusMonths(months);
    }
    
    /**
     * Subtracts years from date
     */
    public static LocalDate subtractYears(LocalDate date, long years) {
        return date == null ? null : date.minusYears(years);
    }
    
    /**
     * Calculates days between two dates
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * Calculates weeks between two dates
     */
    public static long weeksBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.WEEKS.between(start, end);
    }
    
    /**
     * Calculates months between two dates
     */
    public static long monthsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.MONTHS.between(start, end);
    }
    
    /**
     * Calculates years between two dates
     */
    public static long yearsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.YEARS.between(start, end);
    }
    
    /**
     * Calculates age from birth date
     */
    public static int calculateAge(LocalDate birthDate) {
        return calculateAge(birthDate, now());
    }
    
    /**
     * Calculates age from birth date as of specific date
     */
    public static int calculateAge(LocalDate birthDate, LocalDate asOfDate) {
        if (birthDate == null || asOfDate == null) return 0;
        return Period.between(birthDate, asOfDate).getYears();
    }
    
    // ==================== DATE RANGES ====================
    
    /**
     * Checks if date is between two dates (inclusive)
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) return false;
        return (date.isEqual(start) || date.isAfter(start)) && 
               (date.isEqual(end) || date.isBefore(end));
    }
    
    /**
     * Checks if date is in the past
     */
    public static boolean isPast(LocalDate date) {
        return date != null && date.isBefore(now());
    }
    
    /**
     * Checks if date is in the future
     */
    public static boolean isFuture(LocalDate date) {
        return date != null && date.isAfter(now());
    }
    
    /**
     * Checks if date is today
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.isEqual(now());
    }
    
    /**
     * Checks if date is yesterday
     */
    public static boolean isYesterday(LocalDate date) {
        return date != null && date.isEqual(now().minusDays(1));
    }
    
    /**
     * Checks if date is tomorrow
     */
    public static boolean isTomorrow(LocalDate date) {
        return date != null && date.isEqual(now().plusDays(1));
    }
    
    /**
     * Checks if date is this week
     */
    public static boolean isThisWeek(LocalDate date) {
        if (date == null) return false;
        LocalDate startOfWeek = now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return isBetween(date, startOfWeek, endOfWeek);
    }
    
    /**
     * Checks if date is this month
     */
    public static boolean isThisMonth(LocalDate date) {
        if (date == null) return false;
        LocalDate now = now();
        return date.getYear() == now.getYear() && date.getMonth() == now.getMonth();
    }
    
    /**
     * Checks if date is this year
     */
    public static boolean isThisYear(LocalDate date) {
        if (date == null) return false;
        return date.getYear() == now().getYear();
    }
    
    /**
     * Checks if date is weekend
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
    
    /**
     * Checks if date is weekday
     */
    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }
    
    // ==================== ACADEMIC CALENDAR ====================
    
    /**
     * Gets start date of academic year
     */
    public static LocalDate getAcademicYearStart(int academicYear) {
        return LocalDate.of(academicYear, ACADEMIC_YEAR_START, ACADEMIC_YEAR_START_DAY);
    }
    
    /**
     * Gets end date of academic year
     */
    public static LocalDate getAcademicYearEnd(int academicYear) {
        return LocalDate.of(academicYear + 1, Month.MAY, 31);
    }
    
    /**
     * Gets semester start date
     */
    public static LocalDate getSemesterStart(Semester semester, int year) {
        switch (semester) {
            case SPRING:
                return LocalDate.of(year, Month.JANUARY, 15);
            case SUMMER:
                return LocalDate.of(year, Month.JUNE, 1);
            case FALL:
                return LocalDate.of(year, Month.AUGUST, 15);
            default:
                return LocalDate.of(year, Month.AUGUST, 15);
        }
    }
    
    /**
     * Gets semester end date
     */
    public static LocalDate getSemesterEnd(Semester semester, int year) {
        switch (semester) {
            case SPRING:
                return LocalDate.of(year, Month.MAY, 15);
            case SUMMER:
                return LocalDate.of(year, Month.AUGUST, 15);
            case FALL:
                return LocalDate.of(year, Month.DECEMBER, 15);
            default:
                return LocalDate.of(year, Month.DECEMBER, 15);
        }
    }
    
    /**
     * Checks if date is within semester
     */
    public static boolean isInSemester(LocalDate date, Semester semester, int year) {
        if (date == null) return false;
        LocalDate start = getSemesterStart(semester, year);
        LocalDate end = getSemesterEnd(semester, year);
        return isBetween(date, start, end);
    }
    
    /**
     * Gets academic year for given date
     */
    public static int getAcademicYearForDate(LocalDate date) {
        if (date == null) return getCurrentAcademicYear();
        
        LocalDate academicYearStart = LocalDate.of(date.getYear(), ACADEMIC_YEAR_START, ACADEMIC_YEAR_START_DAY);
        
        if (date.isBefore(academicYearStart)) {
            return date.getYear() - 1;
        } else {
            return date.getYear();
        }
    }
    
    /**
     * Gets semester for given date
     */
    public static Semester getSemesterForDate(LocalDate date) {
        if (date == null) return getCurrentSemester();
        
        Month month = date.getMonth();
        switch (month) {
            case JANUARY: case FEBRUARY: case MARCH: case APRIL: case MAY:
                return Semester.SPRING;
            case JUNE: case JULY:
                return Semester.SUMMER;
            case AUGUST: case SEPTEMBER: case OCTOBER: case NOVEMBER: case DECEMBER:
                return Semester.FALL;
            default:
                return Semester.FALL;
        }
    }
    
    // ==================== DATE SEQUENCES ====================
    
    /**
     * Generates range of dates between start and end
     */
    public static List<LocalDate> dateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            return new ArrayList<>();
        }
        
        return Stream.iterate(start, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(start, end) + 1)
                .collect(Collectors.toList());
    }
    
    /**
     * Generates weekdays between start and end
     */
    public static List<LocalDate> weekdayRange(LocalDate start, LocalDate end) {
        return dateRange(start, end).stream()
                .filter(DateUtil::isWeekday)
                .collect(Collectors.toList());
    }
    
    /**
     * Generates weekends between start and end
     */
    public static List<LocalDate> weekendRange(LocalDate start, LocalDate end) {
        return dateRange(start, end).stream()
                .filter(DateUtil::isWeekend)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all dates for a specific day of week in a month
     */
    public static List<LocalDate> getDatesForDayOfWeek(int year, Month month, DayOfWeek dayOfWeek) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastOfMonth = firstOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        
        return dateRange(firstOfMonth, lastOfMonth).stream()
                .filter(date -> date.getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets start of day for date
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
    
    /**
     * Gets end of day for date
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }
    
    /**
     * Gets start of week for date
     */
    public static LocalDate startOfWeek(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    
    /**
     * Gets end of week for date
     */
    public static LocalDate endOfWeek(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
    
    /**
     * Gets start of month for date
     */
    public static LocalDate startOfMonth(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.firstDayOfMonth());
    }
    
    /**
     * Gets end of month for date
     */
    public static LocalDate endOfMonth(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.lastDayOfMonth());
    }
    
    /**
     * Gets start of year for date
     */
    public static LocalDate startOfYear(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.firstDayOfYear());
    }
    
    /**
     * Gets end of year for date
     */
    public static LocalDate endOfYear(LocalDate date) {
        return date == null ? null : date.with(TemporalAdjusters.lastDayOfYear());
    }
    
    /**
     * Gets the minimum of two dates
     */
    public static LocalDate min(LocalDate date1, LocalDate date2) {
        if (date1 == null) return date2;
        if (date2 == null) return date1;
        return date1.isBefore(date2) ? date1 : date2;
    }
    
    /**
     * Gets the maximum of two dates
     */
    public static LocalDate max(LocalDate date1, LocalDate date2) {
        if (date1 == null) return date2;
        if (date2 == null) return date1;
        return date1.isAfter(date2) ? date1 : date2;
    }
    
    /**
     * Checks if year is leap year
     */
    public static boolean isLeapYear(int year) {
        return Year.isLeap(year);
    }
    
    /**
     * Gets number of days in month
     */
    public static int getDaysInMonth(int year, Month month) {
        return month.length(isLeapYear(year));
    }
    
    /**
     * Gets number of days in year
     */
    public static int getDaysInYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }
    
    /**
     * Gets week of year
     */
    public static int getWeekOfYear(LocalDate date) {
        if (date == null) return 0;
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        return date.get(weekFields.weekOfWeekBasedYear());
    }
    
    /**
     * Gets quarter of year (1-4)
     */
    public static int getQuarter(LocalDate date) {
        if (date == null) return 0;
        return (date.getMonthValue() - 1) / 3 + 1;
    }
    
    /**
     * Gets relative description of date
     */
    public static String getRelativeDescription(LocalDate date) {
        if (date == null) return "Unknown";
        
        LocalDate today = now();
        long daysDiff = daysBetween(today, date);
        
        if (daysDiff == 0) return "Today";
        if (daysDiff == 1) return "Tomorrow";
        if (daysDiff == -1) return "Yesterday";
        if (daysDiff > 0 && daysDiff <= 7) return "In " + daysDiff + " days";
        if (daysDiff < 0 && daysDiff >= -7) return Math.abs(daysDiff) + " days ago";
        if (daysDiff > 7 && daysDiff <= 30) return "In " + (daysDiff / 7) + " weeks";
        if (daysDiff < -7 && daysDiff >= -30) return Math.abs(daysDiff / 7) + " weeks ago";
        
        return formatAcademic(date);
    }
}