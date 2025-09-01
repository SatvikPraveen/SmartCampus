// File location: src/main/java/functional/Collectors.java
package functional;

import models.*;
import enums.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Custom collectors for campus entities and common operations
 * Provides specialized collection operations for academic data processing
 */
public final class Collectors {
    
    private Collectors() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== STUDENT COLLECTORS ====================
    
    /**
     * Collects students by their academic classification
     */
    public static Collector<Student, ?, Map<String, List<Student>>> byClassification() {
        return java.util.stream.Collectors.groupingBy(student -> {
            int year = student.getAcademicYear();
            if (year == 1) return "Freshman";
            if (year == 2) return "Sophomore"; 
            if (year == 3) return "Junior";
            if (year == 4) return "Senior";
            if (year > 4) return "Graduate";
            return "Unknown";
        });
    }
    
    /**
     * Collects students by their major
     */
    public static Collector<Student, ?, Map<String, List<Student>>> byMajor() {
        return java.util.stream.Collectors.groupingBy(
            student -> student.getMajor() != null ? student.getMajor() : "Undeclared"
        );
    }
    
    /**
     * Collects students by GPA ranges
     */
    public static Collector<Student, ?, Map<String, List<Student>>> byGpaRange() {
        return java.util.stream.Collectors.groupingBy(student -> {
            double gpa = student.getGpa();
            if (gpa >= 3.7) return "Excellent (3.7-4.0)";
            if (gpa >= 3.0) return "Good (3.0-3.69)";
            if (gpa >= 2.0) return "Satisfactory (2.0-2.99)";
            if (gpa >= 1.0) return "Below Average (1.0-1.99)";
            return "Poor (0.0-0.99)";
        });
    }
    
    /**
     * Calculates overall GPA for a collection of students
     */
    public static Collector<Student, ?, Double> averageGpa() {
        return java.util.stream.Collectors.averagingDouble(Student::getGpa);
    }
    
    /**
     * Collects academic statistics for students
     */
    public static Collector<Student, ?, AcademicStatistics> academicStatistics() {
        return Collector.of(
            AcademicStatistics::new,
            (stats, student) -> {
                stats.addStudent(student);
                stats.addGpa(student.getGpa());
                stats.addCredits(student.getCompletedCredits());
            },
            (stats1, stats2) -> {
                stats1.merge(stats2);
                return stats1;
            }
        );
    }
    
    /**
     * Collects students with honors (GPA >= 3.5)
     */
    public static Collector<Student, ?, List<Student>> honorsStudents() {
        return java.util.stream.Collectors.filtering(
            student -> student.getGpa() >= 3.5,
            java.util.stream.Collectors.toList()
        );
    }
    
    /**
     * Groups students by department
     */
    public static Collector<Student, ?, Map<Department, List<Student>>> byDepartment() {
        return java.util.stream.Collectors.groupingBy(Student::getDepartment);
    }
    
    // ==================== COURSE COLLECTORS ====================
    
    /**
     * Groups courses by department
     */
    public static Collector<Course, ?, Map<String, List<Course>>> coursesByDepartment() {
        return java.util.stream.Collectors.groupingBy(Course::getDepartmentCode);
    }
    
    /**
     * Groups courses by semester
     */
    public static Collector<Course, ?, Map<Semester, List<Course>>> coursesBySemester() {
        return java.util.stream.Collectors.groupingBy(Course::getSemester);
    }
    
    /**
     * Groups courses by level (undergraduate/graduate)
     */
    public static Collector<Course, ?, Map<String, List<Course>>> coursesByLevel() {
        return java.util.stream.Collectors.groupingBy(course -> {
            String courseNumber = course.getCourseNumber();
            if (courseNumber != null && !courseNumber.isEmpty()) {
                char firstDigit = courseNumber.charAt(0);
                return (firstDigit >= '5' && firstDigit <= '9') ? "Graduate" : "Undergraduate";
            }
            return "Unknown";
        });
    }
    
    /**
     * Groups courses by enrollment status
     */
    public static Collector<Course, ?, Map<String, List<Course>>> coursesByEnrollmentStatus() {
        return java.util.stream.Collectors.groupingBy(course -> {
            double enrollmentPercentage = (double) course.getCurrentEnrollment() / course.getMaxEnrollment();
            if (enrollmentPercentage >= 1.0) return "Full";
            if (enrollmentPercentage >= 0.8) return "Nearly Full";
            if (enrollmentPercentage >= 0.5) return "Moderate";
            if (enrollmentPercentage > 0) return "Low";
            return "Empty";
        });
    }
    
    /**
     * Calculates course statistics
     */
    public static Collector<Course, ?, CourseStatistics> courseStatistics() {
        return Collector.of(
            CourseStatistics::new,
            (stats, course) -> {
                stats.addCourse(course);
                stats.addEnrollment(course.getCurrentEnrollment());
                stats.addCredits(course.getCredits());
            },
            (stats1, stats2) -> {
                stats1.merge(stats2);
                return stats1;
            }
        );
    }
    
    /**
     * Collects courses with availability
     */
    public static Collector<Course, ?, List<Course>> availableCourses() {
        return java.util.stream.Collectors.filtering(
            course -> course.getCurrentEnrollment() < course.getMaxEnrollment(),
            java.util.stream.Collectors.toList()
        );
    }
    
    /**
     * Groups courses by credit hours
     */
    public static Collector<Course, ?, Map<Integer, List<Course>>> coursesByCredits() {
        return java.util.stream.Collectors.groupingBy(Course::getCredits);
    }
    
    // ==================== ENROLLMENT COLLECTORS ====================
    
    /**
     * Groups enrollments by semester
     */
    public static Collector<Enrollment, ?, Map<Semester, List<Enrollment>>> enrollmentsBySemester() {
        return java.util.stream.Collectors.groupingBy(Enrollment::getSemester);
    }
    
    /**
     * Groups enrollments by status
     */
    public static Collector<Enrollment, ?, Map<EnrollmentStatus, List<Enrollment>>> enrollmentsByStatus() {
        return java.util.stream.Collectors.groupingBy(Enrollment::getStatus);
    }
    
    /**
     * Groups enrollments by grade level
     */
    public static Collector<Enrollment, ?, Map<GradeLevel, List<Enrollment>>> enrollmentsByGrade() {
        return java.util.stream.Collectors.groupingBy(
            enrollment -> enrollment.getFinalGrade() != null ? 
                enrollment.getFinalGrade() : GradeLevel.INCOMPLETE
        );
    }
    
    /**
     * Calculates GPA from enrollments
     */
    public static Collector<Enrollment, ?, Double> gpaFromEnrollments() {
        return Collector.of(
            GpaCalculator::new,
            (calc, enrollment) -> {
                if (enrollment.getFinalGrade() != null && enrollment.getFinalGrade().affectsGpa()) {
                    Course course = enrollment.getCourse();
                    int credits = course != null ? course.getCredits() : 0;
                    calc.addGrade(enrollment.getFinalGrade(), credits);
                }
            },
            GpaCalculator::merge,
            GpaCalculator::calculateGpa
        );
    }
    
    /**
     * Collects passing enrollments
     */
    public static Collector<Enrollment, ?, List<Enrollment>> passingEnrollments() {
        return java.util.stream.Collectors.filtering(
            enrollment -> enrollment.getFinalGrade() != null && 
                         enrollment.getFinalGrade().isPassingGrade(),
            java.util.stream.Collectors.toList()
        );
    }
    
    /**
     * Collects failing enrollments
     */
    public static Collector<Enrollment, ?, List<Enrollment>> failingEnrollments() {
        return java.util.stream.Collectors.filtering(
            enrollment -> enrollment.getFinalGrade() != null && 
                         enrollment.getFinalGrade().isFailingGrade(),
            java.util.stream.Collectors.toList()
        );
    }
    
    /**
     * Calculates total credits from enrollments
     */
    public static Collector<Enrollment, ?, Integer> totalCredits() {
        return java.util.stream.Collectors.summingInt(enrollment -> {
            Course course = enrollment.getCourse();
            return course != null ? course.getCredits() : 0;
        });
    }
    
    // ==================== PROFESSOR COLLECTORS ====================
    
    /**
     * Groups professors by rank
     */
    public static Collector<Professor, ?, Map<String, List<Professor>>> professorsByRank() {
        return java.util.stream.Collectors.groupingBy(
            professor -> professor.getRank() != null ? professor.getRank() : "Unknown"
        );
    }
    
    /**
     * Groups professors by department
     */
    public static Collector<Professor, ?, Map<Department, List<Professor>>> professorsByDepartment() {
        return java.util.stream.Collectors.groupingBy(Professor::getDepartment);
    }
    
    /**
     * Groups professors by employment status
     */
    public static Collector<Professor, ?, Map<String, List<Professor>>> professorsByEmploymentStatus() {
        return java.util.stream.Collectors.groupingBy(professor -> {
            if (professor.isTenured()) return "Tenured";
            if (professor.isTenureTrack()) return "Tenure Track";
            if (professor.isAdjunct()) return "Adjunct";
            if (professor.isEmeritus()) return "Emeritus";
            return "Other";
        });
    }
    
    /**
     * Calculates average years of service
     */
    public static Collector<Professor, ?, Double> averageYearsOfService() {
        return java.util.stream.Collectors.averagingLong(professor -> {
            if (professor.getHireDate() != null) {
                return java.time.temporal.ChronoUnit.YEARS.between(
                    professor.getHireDate(), 
                    java.time.LocalDate.now()
                );
            }
            return 0L;
        });
    }
    
    /**
     * Collects tenured professors
     */
    public static Collector<Professor, ?, List<Professor>> tenuredProfessors() {
        return java.util.stream.Collectors.filtering(
            Professor::isTenured,
            java.util.stream.Collectors.toList()
        );
    }
    
    // ==================== GRADE COLLECTORS ====================
    
    /**
     * Groups grades by level
     */
    public static Collector<Grade, ?, Map<GradeLevel, List<Grade>>> gradesByLevel() {
        return java.util.stream.Collectors.groupingBy(Grade::getGradeLevel);
    }
    
    /**
     * Groups grades by category
     */
    public static Collector<Grade, ?, Map<GradeLevel.GradeCategory, List<Grade>>> gradesByCategory() {
        return java.util.stream.Collectors.groupingBy(
            grade -> grade.getGradeLevel().getCategory()
        );
    }
    
    /**
     * Calculates grade distribution
     */
    public static Collector<Grade, ?, Map<GradeLevel, Long>> gradeDistribution() {
        return java.util.stream.Collectors.groupingBy(
            Grade::getGradeLevel,
            java.util.stream.Collectors.counting()
        );
    }
    
    /**
     * Calculates average percentage
     */
    public static Collector<Grade, ?, Double> averagePercentage() {
        return java.util.stream.Collectors.averagingDouble(Grade::getPercentage);
    }
    
    // ==================== DEPARTMENT COLLECTORS ====================
    
    /**
     * Groups departments by budget range
     */
    public static Collector<Department, ?, Map<String, List<Department>>> departmentsByBudgetRange() {
        return java.util.stream.Collectors.groupingBy(department -> {
            double budget = department.getBudget();
            if (budget >= 10000000) return "Very High (>$10M)";
            if (budget >= 5000000) return "High ($5M-$10M)";
            if (budget >= 1000000) return "Medium ($1M-$5M)";
            if (budget > 0) return "Low (<$1M)";
            return "No Budget";
        });
    }
    
    /**
     * Calculates total budget
     */
    public static Collector<Department, ?, Double> totalBudget() {
        return java.util.stream.Collectors.summingDouble(Department::getBudget);
    }
    
    /**
     * Calculates total faculty count
     */
    public static Collector<Department, ?, Integer> totalFacultyCount() {
        return java.util.stream.Collectors.summingInt(Department::getFacultyCount);
    }
    
    /**
     * Calculates total student count
     */
    public static Collector<Department, ?, Integer> totalStudentCount() {
        return java.util.stream.Collectors.summingInt(Department::getStudentCount);
    }
    
    // ==================== CUSTOM COLLECTORS ====================
    
    /**
     * Collects into a sorted list
     */
    public static <T> Collector<T, ?, List<T>> toSortedList(Comparator<T> comparator) {
        return Collector.of(
            ArrayList::new,
            List::add,
            (list1, list2) -> { list1.addAll(list2); return list1; },
            list -> { list.sort(comparator); return list; }
        );
    }
    
    /**
     * Collects into a limited list (top N)
     */
    public static <T> Collector<T, ?, List<T>> toTopN(int n, Comparator<T> comparator) {
        return Collector.of(
            () -> new PriorityQueue<>(comparator.reversed()),
            (queue, item) -> {
                queue.add(item);
                if (queue.size() > n) {
                    queue.poll();
                }
            },
            (queue1, queue2) -> {
                queue1.addAll(queue2);
                while (queue1.size() > n) {
                    queue1.poll();
                }
                return queue1;
            },
            queue -> {
                List<T> result = new ArrayList<>(queue);
                result.sort(comparator.reversed());
                return result;
            }
        );
    }
    
    /**
     * Collects into a frequency map
     */
    public static <T> Collector<T, ?, Map<T, Long>> toFrequencyMap() {
        return java.util.stream.Collectors.groupingBy(
            Function.identity(),
            java.util.stream.Collectors.counting()
        );
    }
    
    /**
     * Collects into a percentage map
     */
    public static <T> Collector<T, ?, Map<T, Double>> toPercentageMap() {
        return Collector.of(
            HashMap::new,
            (map, item) -> map.merge(item, 1.0, Double::sum),
            (map1, map2) -> {
                map2.forEach((key, value) -> map1.merge(key, value, Double::sum));
                return map1;
            },
            map -> {
                double total = map.values().stream().mapToDouble(Double::doubleValue).sum();
                Map<T, Double> percentageMap = new HashMap<>();
                map.forEach((key, count) -> percentageMap.put(key, (count / total) * 100));
                return percentageMap;
            }
        );
    }
    
    /**
     * Collects unique items maintaining insertion order
     */
    public static <T> Collector<T, ?, List<T>> toUniqueList() {
        return Collector.of(
            LinkedHashSet::new,
            Set::add,
            (set1, set2) -> { set1.addAll(set2); return set1; },
            ArrayList::new
        );
    }
    
    /**
     * Collects and partitions by predicate into true/false lists
     */
    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<T> predicate) {
        return java.util.stream.Collectors.partitioningBy(predicate);
    }
    
    // ==================== HELPER CLASSES ====================
    
    /**
     * Helper class for calculating academic statistics
     */
    public static class AcademicStatistics {
        private int studentCount = 0;
        private double totalGpa = 0.0;
        private int totalCredits = 0;
        private double minGpa = Double.MAX_VALUE;
        private double maxGpa = Double.MIN_VALUE;
        
        public void addStudent(Student student) {
            studentCount++;
        }
        
        public void addGpa(double gpa) {
            totalGpa += gpa;
            minGpa = Math.min(minGpa, gpa);
            maxGpa = Math.max(maxGpa, gpa);
        }
        
        public void addCredits(int credits) {
            totalCredits += credits;
        }
        
        public void merge(AcademicStatistics other) {
            this.studentCount += other.studentCount;
            this.totalGpa += other.totalGpa;
            this.totalCredits += other.totalCredits;
            this.minGpa = Math.min(this.minGpa, other.minGpa);
            this.maxGpa = Math.max(this.maxGpa, other.maxGpa);
        }
        
        public double getAverageGpa() {
            return studentCount > 0 ? totalGpa / studentCount : 0.0;
        }
        
        public double getAverageCredits() {
            return studentCount > 0 ? (double) totalCredits / studentCount : 0.0;
        }
        
        // Getters
        public int getStudentCount() { return studentCount; }
        public double getMinGpa() { return minGpa == Double.MAX_VALUE ? 0.0 : minGpa; }
        public double getMaxGpa() { return maxGpa == Double.MIN_VALUE ? 0.0 : maxGpa; }
        public int getTotalCredits() { return totalCredits; }
    }
    
    /**
     * Helper class for calculating course statistics
     */
    public static class CourseStatistics {
        private int courseCount = 0;
        private int totalEnrollment = 0;
        private int totalCredits = 0;
        private int maxEnrollment = 0;
        private int minEnrollment = Integer.MAX_VALUE;
        
        public void addCourse(Course course) {
            courseCount++;
        }
        
        public void addEnrollment(int enrollment) {
            totalEnrollment += enrollment;
            maxEnrollment = Math.max(maxEnrollment, enrollment);
            minEnrollment = Math.min(minEnrollment, enrollment);
        }
        
        public void addCredits(int credits) {
            totalCredits += credits;
        }
        
        public void merge(CourseStatistics other) {
            this.courseCount += other.courseCount;
            this.totalEnrollment += other.totalEnrollment;
            this.totalCredits += other.totalCredits;
            this.maxEnrollment = Math.max(this.maxEnrollment, other.maxEnrollment);
            this.minEnrollment = Math.min(this.minEnrollment, other.minEnrollment);
        }
        
        public double getAverageEnrollment() {
            return courseCount > 0 ? (double) totalEnrollment / courseCount : 0.0;
        }
        
        public double getAverageCredits() {
            return courseCount > 0 ? (double) totalCredits / courseCount : 0.0;
        }
        
        // Getters
        public int getCourseCount() { return courseCount; }
        public int getTotalEnrollment() { return totalEnrollment; }
        public int getTotalCredits() { return totalCredits; }
        public int getMaxEnrollment() { return maxEnrollment; }
        public int getMinEnrollment() { return minEnrollment == Integer.MAX_VALUE ? 0 : minEnrollment; }
    }
    
    /**
     * Helper class for GPA calculation from enrollments
     */
    public static class GpaCalculator {
        private double totalPoints = 0.0;
        private int totalCredits = 0;
        
        public void addGrade(GradeLevel grade, int credits) {
            if (grade != null && grade.getGpaPoints() != null) {
                totalPoints += grade.getGpaPoints() * credits;
                totalCredits += credits;
            }
        }
        
        public GpaCalculator merge(GpaCalculator other) {
            this.totalPoints += other.totalPoints;
            this.totalCredits += other.totalCredits;
            return this;
        }
        
        public double calculateGpa() {
            return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
        }
        
        public double getTotalPoints() { return totalPoints; }
        public int getTotalCredits() { return totalCredits; }
    }
}