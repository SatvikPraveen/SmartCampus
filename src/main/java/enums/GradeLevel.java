// File location: src/main/java/enums/GradeLevel.java
package enums;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumeration defining grade levels and academic performance categories
 * Includes GPA calculations, letter grades, and academic standing
 */
public enum GradeLevel {
    
    // Standard letter grades with plus/minus variations
    A_PLUS("A+", 4.0, 97.0, 100.0, "Exceptional", GradeCategory.EXCELLENT),
    A("A", 4.0, 93.0, 96.9, "Excellent", GradeCategory.EXCELLENT),
    A_MINUS("A-", 3.7, 90.0, 92.9, "Excellent", GradeCategory.EXCELLENT),
    
    B_PLUS("B+", 3.3, 87.0, 89.9, "Good", GradeCategory.GOOD),
    B("B", 3.0, 83.0, 86.9, "Good", GradeCategory.GOOD),
    B_MINUS("B-", 2.7, 80.0, 82.9, "Good", GradeCategory.GOOD),
    
    C_PLUS("C+", 2.3, 77.0, 79.9, "Satisfactory", GradeCategory.SATISFACTORY),
    C("C", 2.0, 73.0, 76.9, "Satisfactory", GradeCategory.SATISFACTORY),
    C_MINUS("C-", 1.7, 70.0, 72.9, "Satisfactory", GradeCategory.SATISFACTORY),
    
    D_PLUS("D+", 1.3, 67.0, 69.9, "Below Average", GradeCategory.BELOW_AVERAGE),
    D("D", 1.0, 63.0, 66.9, "Below Average", GradeCategory.BELOW_AVERAGE),
    D_MINUS("D-", 0.7, 60.0, 62.9, "Below Average", GradeCategory.BELOW_AVERAGE),
    
    F("F", 0.0, 0.0, 59.9, "Failing", GradeCategory.FAILING),
    
    // Special grades
    INCOMPLETE("I", null, null, null, "Incomplete", GradeCategory.SPECIAL),
    WITHDRAW("W", null, null, null, "Withdrawal", GradeCategory.SPECIAL),
    WITHDRAW_PASSING("WP", null, null, null, "Withdraw Passing", GradeCategory.SPECIAL),
    WITHDRAW_FAILING("WF", 0.0, null, null, "Withdraw Failing", GradeCategory.SPECIAL),
    
    PASS("P", null, null, null, "Pass", GradeCategory.PASS_FAIL),
    NO_PASS("NP", 0.0, null, null, "No Pass", GradeCategory.PASS_FAIL),
    
    SATISFACTORY("S", null, null, null, "Satisfactory", GradeCategory.PASS_FAIL),
    UNSATISFACTORY("U", 0.0, null, null, "Unsatisfactory", GradeCategory.PASS_FAIL),
    
    CREDIT("CR", null, null, null, "Credit", GradeCategory.PASS_FAIL),
    NO_CREDIT("NC", null, null, null, "No Credit", GradeCategory.PASS_FAIL),
    
    AUDIT("AU", null, null, null, "Audit", GradeCategory.SPECIAL),
    IN_PROGRESS("IP", null, null, null, "In Progress", GradeCategory.SPECIAL),
    
    // Graduate level grades
    GRADUATE_A("GA", 4.0, 90.0, 100.0, "Graduate A", GradeCategory.GRADUATE),
    GRADUATE_B("GB", 3.0, 80.0, 89.9, "Graduate B", GradeCategory.GRADUATE),
    GRADUATE_C("GC", 2.0, 70.0, 79.9, "Graduate C", GradeCategory.GRADUATE),
    GRADUATE_F("GF", 0.0, 0.0, 69.9, "Graduate F", GradeCategory.GRADUATE),
    
    // Honors and special recognition
    SUMMA_CUM_LAUDE("SCL", 4.0, 98.0, 100.0, "Summa Cum Laude", GradeCategory.HONORS),
    MAGNA_CUM_LAUDE("MCL", 3.8, 95.0, 97.9, "Magna Cum Laude", GradeCategory.HONORS),
    CUM_LAUDE("CL", 3.5, 90.0, 94.9, "Cum Laude", GradeCategory.HONORS),
    
    // International grading equivalents
    DISTINCTION("DIST", 4.0, 85.0, 100.0, "Distinction", GradeCategory.INTERNATIONAL),
    MERIT("MERIT", 3.0, 70.0, 84.9, "Merit", GradeCategory.INTERNATIONAL),
    PASS_INT("PASS", 2.0, 50.0, 69.9, "Pass", GradeCategory.INTERNATIONAL),
    FAIL_INT("FAIL", 0.0, 0.0, 49.9, "Fail", GradeCategory.INTERNATIONAL);
    
    // Grade category enumeration
    public enum GradeCategory {
        EXCELLENT("Excellent Performance", 3.7, 4.0),
        GOOD("Good Performance", 2.7, 3.6),
        SATISFACTORY("Satisfactory Performance", 1.7, 2.6),
        BELOW_AVERAGE("Below Average Performance", 0.7, 1.6),
        FAILING("Failing Performance", 0.0, 0.6),
        SPECIAL("Special Status", null, null),
        PASS_FAIL("Pass/Fail", null, null),
        GRADUATE("Graduate Level", 2.0, 4.0),
        HONORS("Academic Honors", 3.5, 4.0),
        INTERNATIONAL("International Grading", 0.0, 4.0);
        
        private final String description;
        private final Double minGpa;
        private final Double maxGpa;
        
        GradeCategory(String description, Double minGpa, Double maxGpa) {
            this.description = description;
            this.minGpa = minGpa;
            this.maxGpa = maxGpa;
        }
        
        public String getDescription() { return description; }
        public Double getMinGpa() { return minGpa; }
        public Double getMaxGpa() { return maxGpa; }
        
        public boolean includesGpa(Double gpa) {
            if (gpa == null || minGpa == null || maxGpa == null) {
                return false;
            }
            return gpa >= minGpa && gpa <= maxGpa;
        }
    }
    
    // Instance variables
    private final String letterGrade;
    private final Double gpaPoints;
    private final Double minPercentage;
    private final Double maxPercentage;
    private final String description;
    private final GradeCategory category;
    
    // Static maps for quick lookups
    private static final Map<String, GradeLevel> LETTER_GRADE_MAP;
    private static final Map<GradeCategory, List<GradeLevel>> CATEGORY_MAP;
    private static final List<GradeLevel> STANDARD_GRADES;
    private static final List<GradeLevel> PASS_FAIL_GRADES;
    
    static {
        LETTER_GRADE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(
                GradeLevel::getLetterGrade,
                grade -> grade,
                (existing, replacement) -> existing
            ));
            
        CATEGORY_MAP = Arrays.stream(values())
            .collect(Collectors.groupingBy(GradeLevel::getCategory));
            
        STANDARD_GRADES = Arrays.stream(values())
            .filter(grade -> grade.getGpaPoints() != null && 
                    grade.getMinPercentage() != null &&
                    !grade.getCategory().equals(GradeCategory.SPECIAL))
            .sorted(Comparator.comparing(GradeLevel::getGpaPoints).reversed())
            .collect(Collectors.toList());
            
        PASS_FAIL_GRADES = getGradesByCategory(GradeCategory.PASS_FAIL);
    }
    
    // Constructor
    GradeLevel(String letterGrade, Double gpaPoints, Double minPercentage, 
               Double maxPercentage, String description, GradeCategory category) {
        this.letterGrade = letterGrade;
        this.gpaPoints = gpaPoints;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
        this.description = description;
        this.category = category;
    }
    
    // Getters
    public String getLetterGrade() { return letterGrade; }
    public Double getGpaPoints() { return gpaPoints; }
    public Double getMinPercentage() { return minPercentage; }
    public Double getMaxPercentage() { return maxPercentage; }
    public String getDescription() { return description; }
    public GradeCategory getCategory() { return category; }
    
    // Utility methods
    public boolean isPassingGrade() {
        if (gpaPoints == null) {
            return category == GradeCategory.PASS_FAIL && 
                   (this == PASS || this == SATISFACTORY || this == CREDIT);
        }
        return gpaPoints >= 1.0;
    }
    
    public boolean isFailingGrade() {
        if (gpaPoints == null) {
            return category == GradeCategory.PASS_FAIL && 
                   (this == NO_PASS || this == UNSATISFACTORY || this == NO_CREDIT);
        }
        return gpaPoints != null && gpaPoints < 1.0;
    }
    
    public boolean affectsGpa() {
        return gpaPoints != null && category != GradeCategory.SPECIAL;
    }
    
    public boolean isHonorsGrade() {
        return category == GradeCategory.HONORS || 
               (gpaPoints != null && gpaPoints >= 3.5);
    }
    
    public boolean isWithdrawal() {
        return this == WITHDRAW || this == WITHDRAW_PASSING || this == WITHDRAW_FAILING;
    }
    
    public boolean isIncomplete() {
        return this == INCOMPLETE || this == IN_PROGRESS;
    }
    
    public boolean isInRange(double percentage) {
        return minPercentage != null && maxPercentage != null &&
               percentage >= minPercentage && percentage <= maxPercentage;
    }
    
    // Static utility methods
    public static GradeLevel fromLetterGrade(String letterGrade) {
        return LETTER_GRADE_MAP.get(letterGrade.toUpperCase());
    }
    
    public static GradeLevel fromPercentage(double percentage) {
        return STANDARD_GRADES.stream()
            .filter(grade -> grade.isInRange(percentage))
            .findFirst()
            .orElse(F);
    }
    
    public static GradeLevel fromGpaPoints(double gpaPoints) {
        return STANDARD_GRADES.stream()
            .filter(grade -> grade.getGpaPoints() != null)
            .min(Comparator.comparing(grade -> 
                Math.abs(grade.getGpaPoints() - gpaPoints)))
            .orElse(F);
    }
    
    public static List<GradeLevel> getGradesByCategory(GradeCategory category) {
        return CATEGORY_MAP.getOrDefault(category, new ArrayList<>());
    }
    
    public static List<GradeLevel> getPassingGrades() {
        return Arrays.stream(values())
            .filter(GradeLevel::isPassingGrade)
            .collect(Collectors.toList());
    }
    
    public static List<GradeLevel> getFailingGrades() {
        return Arrays.stream(values())
            .filter(GradeLevel::isFailingGrade)
            .collect(Collectors.toList());
    }
    
    public static List<GradeLevel> getGpaAffectingGrades() {
        return Arrays.stream(values())
            .filter(GradeLevel::affectsGpa)
            .collect(Collectors.toList());
    }
    
    public static List<GradeLevel> getStandardGrades() {
        return new ArrayList<>(STANDARD_GRADES);
    }
    
    public static double calculateGpa(Map<GradeLevel, Integer> gradeCredits) {
        double totalPoints = 0.0;
        int totalCredits = 0;
        
        for (Map.Entry<GradeLevel, Integer> entry : gradeCredits.entrySet()) {
            GradeLevel grade = entry.getKey();
            int credits = entry.getValue();
            
            if (grade.affectsGpa()) {
                totalPoints += grade.getGpaPoints() * credits;
                totalCredits += credits;
            }
        }
        
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }
    
    public static String getGpaDescription(double gpa) {
        if (gpa >= 3.7) return "Excellent";
        if (gpa >= 3.0) return "Good";
        if (gpa >= 2.0) return "Satisfactory";
        if (gpa >= 1.0) return "Below Average";
        return "Poor";
    }
    
    public static GradeCategory getGpaCategory(double gpa) {
        return Arrays.stream(GradeCategory.values())
            .filter(category -> category.includesGpa(gpa))
            .findFirst()
            .orElse(GradeCategory.FAILING);
    }
    
    // Advanced utility methods
    public static boolean isValidGradeProgression(GradeLevel previousGrade, GradeLevel currentGrade) {
        if (previousGrade == null || currentGrade == null) return true;
        
        // Special cases for incomplete grades
        if (previousGrade.isIncomplete()) {
            return !currentGrade.isIncomplete();
        }
        
        // Withdrawal grades can't be changed
        if (previousGrade.isWithdrawal()) {
            return false;
        }
        
        return true;
    }
    
    public static List<GradeLevel> getValidGradeOptions(GradeLevel currentGrade) {
        if (currentGrade == null) {
            return Arrays.asList(values());
        }
        
        if (currentGrade.isWithdrawal()) {
            return Arrays.asList(currentGrade); // Can't change withdrawal grades
        }
        
        if (currentGrade.isIncomplete()) {
            return getStandardGrades(); // Incomplete can be changed to any standard grade
        }
        
        return Arrays.asList(values());
    }
    
    public static Map<GradeCategory, Long> getGradeDistribution(List<GradeLevel> grades) {
        return grades.stream()
            .collect(Collectors.groupingBy(
                GradeLevel::getCategory,
                Collectors.counting()
            ));
    }
    
    public static OptionalDouble getAverageGpa(List<GradeLevel> grades) {
        List<Double> gpaValues = grades.stream()
            .filter(GradeLevel::affectsGpa)
            .map(GradeLevel::getGpaPoints)
            .collect(Collectors.toList());
            
        return gpaValues.isEmpty() ? 
            OptionalDouble.empty() : 
            OptionalDouble.of(gpaValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(letterGrade);
        if (gpaPoints != null) {
            sb.append(" (").append(gpaPoints).append(" pts)");
        }
        if (minPercentage != null && maxPercentage != null) {
            sb.append(" [").append(minPercentage).append("-").append(maxPercentage).append("%]");
        }
        sb.append(" - ").append(description);
        return sb.toString();
    }
}