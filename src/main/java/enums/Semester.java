// File location: src/main/java/enums/Semester.java
package enums;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumeration defining academic semesters and terms
 * Includes scheduling, ordering, and academic calendar functionality
 */
public enum Semester {
    
    // Traditional semesters
    SPRING("Spring", "SPR", 1, Month.JANUARY, Month.MAY, 15, SemesterType.TRADITIONAL),
    SUMMER("Summer", "SUM", 2, Month.JUNE, Month.AUGUST, 12, SemesterType.TRADITIONAL),
    FALL("Fall", "FALL", 3, Month.AUGUST, Month.DECEMBER, 15, SemesterType.TRADITIONAL),
    
    // Quarter system
    WINTER_QUARTER("Winter Quarter", "WQ", 1, Month.JANUARY, Month.MARCH, 10, SemesterType.QUARTER),
    SPRING_QUARTER("Spring Quarter", "SPQ", 2, Month.APRIL, Month.JUNE, 10, SemesterType.QUARTER),
    SUMMER_QUARTER("Summer Quarter", "SUQ", 3, Month.JULY, Month.SEPTEMBER, 10, SemesterType.QUARTER),
    FALL_QUARTER("Fall Quarter", "FQ", 4, Month.OCTOBER, Month.DECEMBER, 10, SemesterType.QUARTER),
    
    // Trimester system
    FALL_TRIMESTER("Fall Trimester", "FT", 1, Month.AUGUST, Month.NOVEMBER, 12, SemesterType.TRIMESTER),
    WINTER_TRIMESTER("Winter Trimester", "WT", 2, Month.DECEMBER, Month.MARCH, 12, SemesterType.TRIMESTER),
    SPRING_TRIMESTER("Spring Trimester", "ST", 3, Month.APRIL, Month.JULY, 12, SemesterType.TRIMESTER),
    
    // Intensive/Short terms
    WINTER_INTERSESSION("Winter Intersession", "WI", 0, Month.DECEMBER, Month.JANUARY, 3, SemesterType.INTENSIVE),
    SUMMER_SESSION_I("Summer Session I", "SS1", 0, Month.MAY, Month.JUNE, 6, SemesterType.INTENSIVE),
    SUMMER_SESSION_II("Summer Session II", "SS2", 0, Month.JULY, Month.AUGUST, 6, SemesterType.INTENSIVE),
    MAY_TERM("May Term", "MAY", 0, Month.MAY, Month.MAY, 3, SemesterType.INTENSIVE),
    
    // Special terms
    YEAR_LONG("Year Long", "YR", 0, Month.AUGUST, Month.MAY, 30, SemesterType.SPECIAL),
    INDEPENDENT_STUDY("Independent Study", "IS", 0, null, null, 0, SemesterType.SPECIAL),
    PRACTICUM("Practicum", "PRAC", 0, null, null, 0, SemesterType.SPECIAL),
    THESIS("Thesis", "THES", 0, null, null, 0, SemesterType.SPECIAL),
    
    // International terms
    MICHAELMAS("Michaelmas", "MICH", 1, Month.OCTOBER, Month.DECEMBER, 8, SemesterType.INTERNATIONAL),
    HILARY("Hilary", "HIL", 2, Month.JANUARY, Month.MARCH, 8, SemesterType.INTERNATIONAL),
    TRINITY("Trinity", "TRIN", 3, Month.APRIL, Month.JUNE, 8, SemesterType.INTERNATIONAL);
    
    // Semester type enumeration
    public enum SemesterType {
        TRADITIONAL("Traditional Semester", 2, 15),
        QUARTER("Quarter System", 4, 10),
        TRIMESTER("Trimester System", 3, 12),
        INTENSIVE("Intensive Term", 0, 6),
        SPECIAL("Special Term", 0, 0),
        INTERNATIONAL("International Term", 3, 8);
        
        private final String description;
        private final int termsPerYear;
        private final int typicalWeeks;
        
        SemesterType(String description, int termsPerYear, int typicalWeeks) {
            this.description = description;
            this.termsPerYear = termsPerYear;
            this.typicalWeeks = typicalWeeks;
        }
        
        public String getDescription() { return description; }
        public int getTermsPerYear() { return termsPerYear; }
        public int getTypicalWeeks() { return typicalWeeks; }
    }
    
    // Instance variables
    private final String fullName;
    private final String abbreviation;
    private final int order;
    private final Month startMonth;
    private final Month endMonth;
    private final int typicalWeeks;
    private final SemesterType type;
    
    // Static data structures
    private static final Map<SemesterType, List<Semester>> TYPE_MAP;
    private static final Map<String, Semester> ABBREVIATION_MAP;
    private static final List<Semester> TRADITIONAL_ORDER;
    
    static {
        TYPE_MAP = Arrays.stream(values())
            .collect(Collectors.groupingBy(Semester::getType));
            
        ABBREVIATION_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(
                Semester::getAbbreviation,
                semester -> semester,
                (existing, replacement) -> existing
            ));
            
        TRADITIONAL_ORDER = Arrays.asList(SPRING, SUMMER, FALL);
    }
    
    // Constructor
    Semester(String fullName, String abbreviation, int order, Month startMonth, 
             Month endMonth, int typicalWeeks, SemesterType type) {
        this.fullName = fullName;
        this.abbreviation = abbreviation;
        this.order = order;
        this.startMonth = startMonth;
        this.endMonth = endMonth;
        this.typicalWeeks = typicalWeeks;
        this.type = type;
    }
    
    // Getters
    public String getFullName() { return fullName; }
    public String getAbbreviation() { return abbreviation; }
    public int getOrder() { return order; }
    public Month getStartMonth() { return startMonth; }
    public Month getEndMonth() { return endMonth; }
    public int getTypicalWeeks() { return typicalWeeks; }
    public SemesterType getType() { return type; }
    
    // Utility methods
    public boolean isTraditional() {
        return type == SemesterType.TRADITIONAL;
    }
    
    public boolean isQuarter() {
        return type == SemesterType.QUARTER;
    }
    
    public boolean isIntensive() {
        return type == SemesterType.INTENSIVE;
    }
    
    public boolean isSpecial() {
        return type == SemesterType.SPECIAL;
    }
    
    public boolean isSummer() {
        return this == SUMMER || this == SUMMER_QUARTER || 
               this == SUMMER_SESSION_I || this == SUMMER_SESSION_II;
    }
    
    public boolean containsMonth(Month month) {
        if (startMonth == null || endMonth == null) {
            return false;
        }
        
        if (startMonth.getValue() <= endMonth.getValue()) {
            return month.getValue() >= startMonth.getValue() && 
                   month.getValue() <= endMonth.getValue();
        } else {
            // Crosses year boundary (e.g., December to March)
            return month.getValue() >= startMonth.getValue() || 
                   month.getValue() <= endMonth.getValue();
        }
    }
    
    public boolean containsDate(LocalDate date) {
        return containsMonth(date.getMonth());
    }
    
    public LocalDate getStartDate(int year) {
        if (startMonth == null) return null;
        return LocalDate.of(year, startMonth, 1);
    }
    
    public LocalDate getEndDate(int year) {
        if (endMonth == null) return null;
        int actualYear = (startMonth != null && startMonth.getValue() > endMonth.getValue()) 
            ? year + 1 : year;
        return LocalDate.of(actualYear, endMonth, endMonth.length(Year.isLeap(actualYear)));
    }
    
    // Static utility methods
    public static Semester fromAbbreviation(String abbreviation) {
        return ABBREVIATION_MAP.get(abbreviation.toUpperCase());
    }
    
    public static List<Semester> getByType(SemesterType type) {
        return TYPE_MAP.getOrDefault(type, new ArrayList<>());
    }
    
    public static List<Semester> getTraditionalSemesters() {
        return new ArrayList<>(TRADITIONAL_ORDER);
    }
    
    public static List<Semester> getQuarters() {
        return getByType(SemesterType.QUARTER);
    }
    
    public static List<Semester> getTrimesters() {
        return getByType(SemesterType.TRIMESTER);
    }
    
    public static List<Semester> getIntensiveTerms() {
        return getByType(SemesterType.INTENSIVE);
    }
    
    public static Semester getCurrentSemester() {
        Month currentMonth = LocalDate.now().getMonth();
        return Arrays.stream(values())
            .filter(semester -> semester.containsMonth(currentMonth))
            .min(Comparator.comparing(Semester::getOrder))
            .orElse(FALL);
    }
    
    public static Semester getCurrentSemester(SemesterType type) {
        Month currentMonth = LocalDate.now().getMonth();
        return getByType(type).stream()
            .filter(semester -> semester.containsMonth(currentMonth))
            .min(Comparator.comparing(Semester::getOrder))
            .orElse(getByType(type).isEmpty() ? null : getByType(type).get(0));
    }
    
    public static List<Semester> getSemestersForYear(int year, SemesterType type) {
        return getByType(type).stream()
            .sorted(Comparator.comparing(Semester::getOrder))
            .collect(Collectors.toList());
    }
    
    public static Semester getNextSemester(Semester current) {
        List<Semester> sameType = getByType(current.getType());
        if (sameType.isEmpty()) return null;
        
        sameType.sort(Comparator.comparing(Semester::getOrder));
        
        int currentIndex = sameType.indexOf(current);
        if (currentIndex == -1 || currentIndex == sameType.size() - 1) {
            return sameType.get(0); // Wrap around to first semester
        }
        
        return sameType.get(currentIndex + 1);
    }
    
    public static Semester getPreviousSemester(Semester current) {
        List<Semester> sameType = getByType(current.getType());
        if (sameType.isEmpty()) return null;
        
        sameType.sort(Comparator.comparing(Semester::getOrder));
        
        int currentIndex = sameType.indexOf(current);
        if (currentIndex <= 0) {
            return sameType.get(sameType.size() - 1); // Wrap around to last semester
        }
        
        return sameType.get(currentIndex - 1);
    }
    
    // Academic year methods
    public static class AcademicYear {
        private final int startYear;
        private final List<Semester> semesters;
        private final SemesterType type;
        
        public AcademicYear(int startYear, SemesterType type) {
            this.startYear = startYear;
            this.type = type;
            this.semesters = getSemestersForYear(startYear, type);
        }
        
        public int getStartYear() { return startYear; }
        public int getEndYear() { return startYear + 1; }
        public List<Semester> getSemesters() { return new ArrayList<>(semesters); }
        public SemesterType getType() { return type; }
        
        public String getYearString() {
            return startYear + "-" + (startYear + 1);
        }
        
        public boolean contains(Semester semester) {
            return semesters.contains(semester);
        }
        
        public Semester getFirstSemester() {
            return semesters.isEmpty() ? null : semesters.get(0);
        }
        
        public Semester getLastSemester() {
            return semesters.isEmpty() ? null : semesters.get(semesters.size() - 1);
        }
    }
    
    public static AcademicYear getCurrentAcademicYear(SemesterType type) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        
        // If we're in spring semester, it's typically the second year of the academic year
        if (now.getMonth().getValue() <= 6) {
            year = year - 1;
        }
        
        return new AcademicYear(year, type);
    }
    
    public static List<AcademicYear> getAcademicYearRange(int startYear, int endYear, SemesterType type) {
        List<AcademicYear> years = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            years.add(new AcademicYear(year, type));
        }
        return years;
    }
    
    // Comparison and validation methods
    public static boolean isValidSemesterSequence(List<Semester> semesters) {
        if (semesters.isEmpty()) return true;
        
        SemesterType type = semesters.get(0).getType();
        return semesters.stream().allMatch(s -> s.getType() == type);
    }
    
    public static List<Semester> sortSemesters(List<Semester> semesters) {
        return semesters.stream()
            .sorted(Comparator.comparing(Semester::getOrder))
            .collect(Collectors.toList());
    }
    
    public static Map<SemesterType, List<Semester>> groupByType(List<Semester> semesters) {
        return semesters.stream()
            .collect(Collectors.groupingBy(Semester::getType));
    }
    
    // Credit calculation methods
    public double getCreditMultiplier() {
        switch (type) {
            case QUARTER:
                return 0.67; // Quarter credits typically worth 2/3 of semester credits
            case TRIMESTER:
                return 0.75; // Trimester credits worth 3/4 of semester credits
            case INTENSIVE:
                return typicalWeeks / 15.0; // Based on typical 15-week semester
            default:
                return 1.0;
        }
    }
    
    public int convertCreditsToSemester(int credits) {
        return (int) Math.round(credits * getCreditMultiplier());
    }
    
    public int convertCreditsFromSemester(int semesterCredits) {
        return (int) Math.round(semesterCredits / getCreditMultiplier());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName);
        if (!abbreviation.equals(fullName)) {
            sb.append(" (").append(abbreviation).append(")");
        }
        if (typicalWeeks > 0) {
            sb.append(" - ").append(typicalWeeks).append(" weeks");
        }
        return sb.toString();
    }
}