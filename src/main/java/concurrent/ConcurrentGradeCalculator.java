// File location: src/main/java/concurrent/ConcurrentGradeCalculator.java

package concurrent;

import models.*;
import repositories.GradeRepository;
import repositories.EnrollmentRepository;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles parallel processing of grade calculations
 * Provides efficient computation of grades, GPAs, and statistical analysis
 */
public class ConcurrentGradeCalculator {
    
    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ForkJoinPool forkJoinPool;
    private final ExecutorService executorService;
    
    public ConcurrentGradeCalculator(GradeRepository gradeRepository,
                                   EnrollmentRepository enrollmentRepository) {
        this.gradeRepository = gradeRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.forkJoinPool = new ForkJoinPool();
        this.executorService = Executors.newWorkStealingPool();
    }
    
    /**
     * Calculate GPA for a student concurrently
     */
    public CompletableFuture<GPAResult> calculateStudentGPAAsync(Student student) {
        return CompletableFuture.supplyAsync(() -> {
            List<Grade> grades = gradeRepository.findByStudent(student);
            
            if (grades.isEmpty()) {
                return new GPAResult(student, 0.0, 0, Collections.emptyMap());
            }
            
            // Calculate GPA using parallel processing
            double totalGradePoints = grades.parallelStream()
                .mapToDouble(this::calculateGradePoints)
                .sum();
            
            int totalCredits = grades.parallelStream()
                .mapToInt(grade -> grade.getCourse().getCredits())
                .sum();
            
            double gpa = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
            
            // Calculate grade distribution
            Map<String, Integer> gradeDistribution = grades.parallelStream()
                .collect(Collectors.groupingBy(
                    Grade::getLetterGrade,
                    Collectors.summingInt(grade -> 1)
                ));
            
            return new GPAResult(student, gpa, totalCredits, gradeDistribution);
        }, executorService);
    }
    
    /**
     * Calculate GPAs for multiple students concurrently
     */
    public CompletableFuture<List<GPAResult>> calculateBatchStudentGPAs(
            List<Student> students) {
        
        List<CompletableFuture<GPAResult>> gpaFutures = students.stream()
            .map(this::calculateStudentGPAAsync)
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(gpaFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> gpaFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    /**
     * Calculate course statistics concurrently
     */
    public CompletableFuture<CourseStatistics> calculateCourseStatisticsAsync(
            Course course) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Grade> grades = gradeRepository.findByCourse(course);
            
            if (grades.isEmpty()) {
                return new CourseStatistics(course, 0, 0.0, 0.0, 0.0, 
                                          Collections.emptyMap());
            }
            
            // Use parallel streams for statistical calculations
            DoubleSummaryStatistics stats = grades.parallelStream()
                .mapToDouble(Grade::getNumericGrade)
                .summaryStatistics();
            
            // Calculate grade distribution
            Map<String, Long> gradeDistribution = grades.parallelStream()
                .collect(Collectors.groupingBy(
                    Grade::getLetterGrade,
                    Collectors.counting()
                ));
            
            return new CourseStatistics(
                course,
                grades.size(),
                stats.getAverage(),
                stats.getMin(),
                stats.getMax(),
                gradeDistribution
            );
        }, executorService);
    }
    
    /**
     * Calculate department-wide statistics using Fork/Join framework
     */
    public CompletableFuture<DepartmentStatistics> calculateDepartmentStatisticsAsync(
            Department department) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Grade> allGrades = gradeRepository.findByDepartment(department);
            
            if (allGrades.isEmpty()) {
                return new DepartmentStatistics(department, Collections.emptyMap(),
                                              0.0, Collections.emptyMap());
            }
            
            // Use Fork/Join for large dataset processing
            DepartmentStatisticsTask task = new DepartmentStatisticsTask(
                allGrades, 0, allGrades.size());
            DepartmentStatisticsResult result = forkJoinPool.invoke(task);
            
            return new DepartmentStatistics(
                department,
                result.getCourseAverages(),
                result.getOverallAverage(),
                result.getGradeDistribution()
            );
        }, executorService);
    }
    
    /**
     * Calculate semester statistics concurrently
     */
    public CompletableFuture<SemesterStatistics> calculateSemesterStatisticsAsync(
            String semester, String academicYear) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Grade> semesterGrades = gradeRepository.findBySemesterAndYear(
                semester, academicYear);
            
            if (semesterGrades.isEmpty()) {
                return new SemesterStatistics(semester, academicYear, 0, 0.0,
                                            Collections.emptyMap(), Collections.emptyMap());
            }
            
            // Parallel processing of semester data
            int totalStudents = semesterGrades.parallelStream()
                .collect(Collectors.toSet())
                .size();
            
            double averageGrade = semesterGrades.parallelStream()
                .mapToDouble(Grade::getNumericGrade)
                .average()
                .orElse(0.0);
            
            // Grade distribution by department
            Map<Department, Map<String, Long>> departmentDistribution = 
                semesterGrades.parallelStream()
                    .collect(Collectors.groupingBy(
                        grade -> grade.getCourse().getDepartment(),
                        Collectors.groupingBy(
                            Grade::getLetterGrade,
                            Collectors.counting()
                        )
                    ));
            
            // Overall grade distribution
            Map<String, Long> overallDistribution = semesterGrades.parallelStream()
                .collect(Collectors.groupingBy(
                    Grade::getLetterGrade,
                    Collectors.counting()
                ));
            
            return new SemesterStatistics(
                semester,
                academicYear,
                totalStudents,
                averageGrade,
                departmentDistribution,
                overallDistribution
            );
        }, executorService);
    }
    
    /**
     * Calculate grade trends over time
     */
    public CompletableFuture<GradeTrends> calculateGradeTrendsAsync(
            Department department, int numberOfSemesters) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Get historical grade data
            List<Grade> historicalGrades = gradeRepository.findByDepartmentRecent(
                department, numberOfSemesters);
            
            // Group by semester and calculate averages
            Map<String, Double> semesterAverages = historicalGrades.parallelStream()
                .collect(Collectors.groupingBy(
                    grade -> grade.getSemester() + " " + grade.getAcademicYear(),
                    Collectors.averagingDouble(Grade::getNumericGrade)
                ));
            
            // Calculate trend statistics
            List<Double> averages = new ArrayList<>(semesterAverages.values());
            double overallTrend = calculateTrend(averages);
            
            return new GradeTrends(department, semesterAverages, overallTrend);
        }, executorService);
    }
    
    /**
     * Parallel calculation of failing students
     */
    public CompletableFuture<List<Student>> findFailingStudentsAsync(
            double threshold) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Grade> allGrades = gradeRepository.findAll();
            
            // Group grades by student and calculate GPAs in parallel
            Map<Student, List<Grade>> studentGrades = allGrades.parallelStream()
                .collect(Collectors.groupingBy(Grade::getStudent));
            
            return studentGrades.entrySet().parallelStream()
                .filter(entry -> {
                    List<Grade> grades = entry.getValue();
                    double gpa = calculateGPA(grades);
                    return gpa < threshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }, executorService);
    }
    
    /**
     * Calculate honor roll students concurrently
     */
    public CompletableFuture<List<Student>> calculateHonorRollAsync(
            double minGPA, String semester, String academicYear) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Grade> semesterGrades = gradeRepository.findBySemesterAndYear(
                semester, academicYear);
            
            Map<Student, List<Grade>> studentGrades = semesterGrades.parallelStream()
                .collect(Collectors.groupingBy(Grade::getStudent));
            
            return studentGrades.entrySet().parallelStream()
                .filter(entry -> {
                    List<Grade> grades = entry.getValue();
                    double gpa = calculateGPA(grades);
                    return gpa >= minGPA;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }, executorService);
    }
    
    // Helper methods
    
    private double calculateGradePoints(Grade grade) {
        return getGradePointValue(grade.getLetterGrade()) * 
               grade.getCourse().getCredits();
    }
    
    private double getGradePointValue(String letterGrade) {
        switch (letterGrade.toUpperCase()) {
            case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.3;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.3;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.3;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return 0.0;
        }
    }
    
    private double calculateGPA(List<Grade> grades) {
        if (grades.isEmpty()) return 0.0;
        
        double totalGradePoints = grades.stream()
            .mapToDouble(this::calculateGradePoints)
            .sum();
        
        int totalCredits = grades.stream()
            .mapToInt(grade -> grade.getCourse().getCredits())
            .sum();
        
        return totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
    }
    
    private double calculateTrend(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        // Simple linear trend calculation
        double sum = 0.0;
        for (int i = 1; i < values.size(); i++) {
            sum += values.get(i) - values.get(i - 1);
        }
        return sum / (values.size() - 1);
    }
    
    public void shutdown() {
        executorService.shutdown();
        forkJoinPool.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!forkJoinPool.awaitTermination(30, TimeUnit.SECONDS)) {
                forkJoinPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            forkJoinPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes and result objects
    
    public static class GPAResult {
        private final Student student;
        private final double gpa;
        private final int totalCredits;
        private final Map<String, Integer> gradeDistribution;
        
        public GPAResult(Student student, double gpa, int totalCredits,
                        Map<String, Integer> gradeDistribution) {
            this.student = student;
            this.gpa = gpa;
            this.totalCredits = totalCredits;
            this.gradeDistribution = gradeDistribution;
        }
        
        // Getters
        public Student getStudent() { return student; }
        public double getGpa() { return gpa; }
        public int getTotalCredits() { return totalCredits; }
        public Map<String, Integer> getGradeDistribution() { return gradeDistribution; }
    }
    
    public static class CourseStatistics {
        private final Course course;
        private final int studentCount;
        private final double averageGrade;
        private final double minGrade;
        private final double maxGrade;
        private final Map<String, Long> gradeDistribution;
        
        public CourseStatistics(Course course, int studentCount, double averageGrade,
                              double minGrade, double maxGrade,
                              Map<String, Long> gradeDistribution) {
            this.course = course;
            this.studentCount = studentCount;
            this.averageGrade = averageGrade;
            this.minGrade = minGrade;
            this.maxGrade = maxGrade;
            this.gradeDistribution = gradeDistribution;
        }
        
        // Getters
        public Course getCourse() { return course; }
        public int getStudentCount() { return studentCount; }
        public double getAverageGrade() { return averageGrade; }
        public double getMinGrade() { return minGrade; }
        public double getMaxGrade() { return maxGrade; }
        public Map<String, Long> getGradeDistribution() { return gradeDistribution; }
    }
    
    public static class DepartmentStatistics {
        private final Department department;
        private final Map<Course, Double> courseAverages;
        private final double overallAverage;
        private final Map<String, Long> gradeDistribution;
        
        public DepartmentStatistics(Department department,
                                  Map<Course, Double> courseAverages,
                                  double overallAverage,
                                  Map<String, Long> gradeDistribution) {
            this.department = department;
            this.courseAverages = courseAverages;
            this.overallAverage = overallAverage;
            this.gradeDistribution = gradeDistribution;
        }
        
        // Getters
        public Department getDepartment() { return department; }
        public Map<Course, Double> getCourseAverages() { return courseAverages; }
        public double getOverallAverage() { return overallAverage; }
        public Map<String, Long> getGradeDistribution() { return gradeDistribution; }
    }
    
    public static class SemesterStatistics {
        private final String semester;
        private final String academicYear;
        private final int totalStudents;
        private final double averageGrade;
        private final Map<Department, Map<String, Long>> departmentDistribution;
        private final Map<String, Long> overallDistribution;
        
        public SemesterStatistics(String semester, String academicYear,
                                int totalStudents, double averageGrade,
                                Map<Department, Map<String, Long>> departmentDistribution,
                                Map<String, Long> overallDistribution) {
            this.semester = semester;
            this.academicYear = academicYear;
            this.totalStudents = totalStudents;
            this.averageGrade = averageGrade;
            this.departmentDistribution = departmentDistribution;
            this.overallDistribution = overallDistribution;
        }
        
        // Getters
        public String getSemester() { return semester; }
        public String getAcademicYear() { return academicYear; }
        public int getTotalStudents() { return totalStudents; }
        public double getAverageGrade() { return averageGrade; }
        public Map<Department, Map<String, Long>> getDepartmentDistribution() { return departmentDistribution; }
        public Map<String, Long> getOverallDistribution() { return overallDistribution; }
    }
    
    public static class GradeTrends {
        private final Department department;
        private final Map<String, Double> semesterAverages;
        private final double overallTrend;
        
        public GradeTrends(Department department, Map<String, Double> semesterAverages,
                          double overallTrend) {
            this.department = department;
            this.semesterAverages = semesterAverages;
            this.overallTrend = overallTrend;
        }
        
        // Getters
        public Department getDepartment() { return department; }
        public Map<String, Double> getSemesterAverages() { return semesterAverages; }
        public double getOverallTrend() { return overallTrend; }
    }
    
    // Fork/Join task for department statistics
    private static class DepartmentStatisticsTask extends RecursiveTask<DepartmentStatisticsResult> {
        private static final int THRESHOLD = 1000;
        private final List<Grade> grades;
        private final int start;
        private final int end;
        
        public DepartmentStatisticsTask(List<Grade> grades, int start, int end) {
            this.grades = grades;
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected DepartmentStatisticsResult compute() {
            if (end - start <= THRESHOLD) {
                return computeDirectly();
            } else {
                int middle = start + (end - start) / 2;
                DepartmentStatisticsTask leftTask = new DepartmentStatisticsTask(grades, start, middle);
                DepartmentStatisticsTask rightTask = new DepartmentStatisticsTask(grades, middle, end);
                
                leftTask.fork();
                DepartmentStatisticsResult rightResult = rightTask.compute();
                DepartmentStatisticsResult leftResult = leftTask.join();
                
                return mergeResults(leftResult, rightResult);
            }
        }
        
        private DepartmentStatisticsResult computeDirectly() {
            Map<Course, List<Double>> courseGrades = new HashMap<>();
            Map<String, Long> gradeDistribution = new HashMap<>();
            DoubleAdder totalGrade = new DoubleAdder();
            int count = 0;
            
            for (int i = start; i < end; i++) {
                Grade grade = grades.get(i);
                
                courseGrades.computeIfAbsent(grade.getCourse(), k -> new ArrayList<>())
                           .add(grade.getNumericGrade());
                
                gradeDistribution.merge(grade.getLetterGrade(), 1L, Long::sum);
                totalGrade.add(grade.getNumericGrade());
                count++;
            }
            
            Map<Course, Double> courseAverages = courseGrades.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                ));
            
            double overallAverage = count > 0 ? totalGrade.sum() / count : 0.0;
            
            return new DepartmentStatisticsResult(courseAverages, overallAverage, gradeDistribution);
        }
        
        private DepartmentStatisticsResult mergeResults(DepartmentStatisticsResult left, 
                                                       DepartmentStatisticsResult right) {
            Map<Course, Double> mergedCourseAverages = new HashMap<>(left.getCourseAverages());
            right.getCourseAverages().forEach((course, avg) -> 
                mergedCourseAverages.merge(course, avg, (v1, v2) -> (v1 + v2) / 2));
            
            double mergedOverallAverage = (left.getOverallAverage() + right.getOverallAverage()) / 2;
            
            Map<String, Long> mergedGradeDistribution = new HashMap<>(left.getGradeDistribution());
            right.getGradeDistribution().forEach((grade, count) -> 
                mergedGradeDistribution.merge(grade, count, Long::sum));
            
            return new DepartmentStatisticsResult(mergedCourseAverages, mergedOverallAverage, 
                                                mergedGradeDistribution);
        }
    }
    
    private static class DepartmentStatisticsResult {
        private final Map<Course, Double> courseAverages;
        private final double overallAverage;
        private final Map<String, Long> gradeDistribution;
        
        public DepartmentStatisticsResult(Map<Course, Double> courseAverages,
                                        double overallAverage,
                                        Map<String, Long> gradeDistribution) {
            this.courseAverages = courseAverages;
            this.overallAverage = overallAverage;
            this.gradeDistribution = gradeDistribution;
        }
        
        public Map<Course, Double> getCourseAverages() { return courseAverages; }
        public double getOverallAverage() { return overallAverage; }
        public Map<String, Long> getGradeDistribution() { return gradeDistribution; }
    }
}