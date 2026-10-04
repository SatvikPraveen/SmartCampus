// File location: src/main/java/concurrent/ConcurrentGradeCalculator.java

package concurrent;

import models.*;
import repositories.CourseRepository;
import repositories.EnrollmentRepository;
import repositories.StudentRepository;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;

/**
 * Handles parallel processing of grade calculations
 * Provides efficient computation of grades, GPAs, and statistical analysis
 * based on the final grades recorded on enrollments
 */
public class ConcurrentGradeCalculator {
    
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final ForkJoinPool forkJoinPool;
    private final ExecutorService executorService;
    
    public ConcurrentGradeCalculator(EnrollmentRepository enrollmentRepository,
                                   StudentRepository studentRepository,
                                   CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.forkJoinPool = new ForkJoinPool();
        this.executorService = Executors.newWorkStealingPool();
    }
    
    /**
     * Calculate GPA for a student concurrently
     */
    public CompletableFuture<GPAResult> calculateStudentGPAAsync(Student student) {
        return CompletableFuture.supplyAsync(() -> {
            List<Enrollment> grades = graded(enrollmentRepository.findByStudent(student));
            
            if (grades.isEmpty()) {
                return new GPAResult(student, 0.0, 0, Collections.emptyMap());
            }
            
            List<Enrollment> gpaGrades = grades.parallelStream()
                .filter(Enrollment::countsTowardGpa)
                .collect(Collectors.toList());
            
            // Calculate GPA using parallel processing
            double totalGradePoints = gpaGrades.parallelStream()
                .mapToDouble(Enrollment::getGpaPoints)
                .sum();
            
            int totalCredits = gpaGrades.parallelStream()
                .mapToInt(Enrollment::getCreditHours)
                .sum();
            
            double gpa = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
            
            // Calculate grade distribution
            Map<String, Integer> gradeDistribution = grades.parallelStream()
                .collect(Collectors.groupingBy(
                    ConcurrentGradeCalculator::letterOf,
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
            List<Enrollment> grades = graded(enrollmentRepository.findByCourse(course));
            
            if (grades.isEmpty()) {
                return new CourseStatistics(course, 0, 0.0, 0.0, 0.0, 
                                          Collections.emptyMap());
            }
            
            // Use parallel streams for statistical calculations
            DoubleSummaryStatistics stats = grades.parallelStream()
                .mapToDouble(Enrollment::getNumericGrade)
                .summaryStatistics();
            
            // Calculate grade distribution
            Map<String, Long> gradeDistribution = grades.parallelStream()
                .collect(Collectors.groupingBy(
                    ConcurrentGradeCalculator::letterOf,
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
            List<Enrollment> allGrades = graded(enrollmentRepository.findByDepartment(department));
            
            if (allGrades.isEmpty()) {
                return new DepartmentStatistics(department, Collections.emptyMap(),
                                              0.0, Collections.emptyMap());
            }
            
            // Use Fork/Join for large dataset processing
            DepartmentStatisticsTask task = new DepartmentStatisticsTask(
                allGrades, 0, allGrades.size());
            DepartmentStatisticsResult result = forkJoinPool.invoke(task);
            
            Map<Course, Double> courseAverages = new HashMap<>();
            result.getCourseAverages().forEach((courseId, average) ->
                courseRepository.findById(courseId)
                    .ifPresent(course -> courseAverages.put(course, average)));
            
            return new DepartmentStatistics(
                department,
                courseAverages,
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
            List<Enrollment> semesterGrades = findSemesterGrades(semester, academicYear);
            
            if (semesterGrades.isEmpty()) {
                return new SemesterStatistics(semester, academicYear, 0, 0.0,
                                            Collections.emptyMap(), Collections.emptyMap());
            }
            
            // Parallel processing of semester data
            int totalStudents = (int) semesterGrades.parallelStream()
                .map(Enrollment::getStudentId)
                .distinct()
                .count();
            
            double averageGrade = semesterGrades.parallelStream()
                .mapToDouble(Enrollment::getNumericGrade)
                .average()
                .orElse(0.0);
            
            // Grade distribution by department (keyed by department ID)
            Map<String, String> courseDepartments = new ConcurrentHashMap<>();
            semesterGrades.stream()
                .map(Enrollment::getCourseId)
                .distinct()
                .forEach(courseId -> courseRepository.findById(courseId)
                    .map(Course::getDepartmentId)
                    .ifPresent(departmentId -> courseDepartments.put(courseId, departmentId)));
            
            Map<String, Map<String, Long>> departmentDistribution = 
                semesterGrades.parallelStream()
                    .filter(grade -> courseDepartments.containsKey(grade.getCourseId()))
                    .collect(Collectors.groupingBy(
                        grade -> courseDepartments.get(grade.getCourseId()),
                        Collectors.groupingBy(
                            ConcurrentGradeCalculator::letterOf,
                            Collectors.counting()
                        )
                    ));
            
            // Overall grade distribution
            Map<String, Long> overallDistribution = semesterGrades.parallelStream()
                .collect(Collectors.groupingBy(
                    ConcurrentGradeCalculator::letterOf,
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
            List<Enrollment> historicalGrades = graded(enrollmentRepository.findByDepartment(department));
            
            // Group by semester (chronologically) and calculate averages
            Map<String, List<Enrollment>> bySemester = historicalGrades.stream()
                .collect(Collectors.groupingBy(
                    grade -> grade.getSemester() + " " + grade.getYear(),
                    TreeMap::new,
                    Collectors.toList()
                ));
            
            List<String> orderedTerms = bySemester.entrySet().stream()
                .sorted(Comparator.comparing(
                    (Map.Entry<String, List<Enrollment>> entry) -> entry.getValue().get(0),
                    Comparator.comparingInt(Enrollment::getYear)
                              .thenComparingInt(grade -> termOrder(grade.getSemester()))))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            List<String> recentTerms = orderedTerms.subList(
                Math.max(0, orderedTerms.size() - numberOfSemesters), orderedTerms.size());
            
            Map<String, Double> semesterAverages = new LinkedHashMap<>();
            for (String term : recentTerms) {
                semesterAverages.put(term, bySemester.get(term).parallelStream()
                    .mapToDouble(Enrollment::getNumericGrade)
                    .average()
                    .orElse(0.0));
            }
            
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
            List<Enrollment> allGrades = graded(enrollmentRepository.findAll());
            
            // Group grades by student and calculate GPAs in parallel
            Map<String, List<Enrollment>> studentGrades = allGrades.parallelStream()
                .collect(Collectors.groupingBy(Enrollment::getStudentId));
            
            return studentsMatching(studentGrades, gpa -> gpa < threshold);
        }, executorService);
    }
    
    /**
     * Calculate honor roll students concurrently
     */
    public CompletableFuture<List<Student>> calculateHonorRollAsync(
            double minGPA, String semester, String academicYear) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Enrollment> semesterGrades = findSemesterGrades(semester, academicYear);
            
            Map<String, List<Enrollment>> studentGrades = semesterGrades.parallelStream()
                .collect(Collectors.groupingBy(Enrollment::getStudentId));
            
            return studentsMatching(studentGrades, gpa -> gpa >= minGPA);
        }, executorService);
    }
    
    // Helper methods
    
    private static List<Enrollment> graded(List<Enrollment> enrollments) {
        return enrollments.stream()
            .filter(enrollment -> enrollment.getGrade() != null
                && enrollment.getGrade() != Enrollment.Grade.NOT_GRADED
                && enrollment.getStudentId() != null
                && enrollment.getCourseId() != null)
            .collect(Collectors.toList());
    }
    
    private static String letterOf(Enrollment enrollment) {
        return enrollment.getGrade().getLetter();
    }
    
    private List<Enrollment> findSemesterGrades(String semester, String academicYear) {
        return graded(enrollmentRepository.findByAcademicYear(academicYear)).stream()
            .filter(enrollment -> semester.equalsIgnoreCase(enrollment.getSemester()))
            .collect(Collectors.toList());
    }
    
    private List<Student> studentsMatching(Map<String, List<Enrollment>> studentGrades,
                                           DoublePredicate gpaFilter) {
        return studentGrades.entrySet().parallelStream()
            .filter(entry -> gpaFilter.test(calculateGPA(entry.getValue())))
            .map(entry -> studentRepository.findById(entry.getKey()))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }
    
    private static int termOrder(String semester) {
        if (semester == null) return 0;
        String normalized = semester.toLowerCase();
        if (normalized.contains("spring")) return 1;
        if (normalized.contains("summer")) return 2;
        if (normalized.contains("fall")) return 3;
        return 0;
    }
    
    private double calculateGPA(List<Enrollment> grades) {
        if (grades.isEmpty()) return 0.0;
        
        double totalGradePoints = grades.stream()
            .filter(Enrollment::countsTowardGpa)
            .mapToDouble(Enrollment::getGpaPoints)
            .sum();
        
        int totalCredits = grades.stream()
            .filter(Enrollment::countsTowardGpa)
            .mapToInt(Enrollment::getCreditHours)
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
        private final Map<String, Map<String, Long>> departmentDistribution;
        private final Map<String, Long> overallDistribution;
        
        public SemesterStatistics(String semester, String academicYear,
                                int totalStudents, double averageGrade,
                                Map<String, Map<String, Long>> departmentDistribution,
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
        public Map<String, Map<String, Long>> getDepartmentDistribution() { return departmentDistribution; }
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
        private final List<Enrollment> grades;
        private final int start;
        private final int end;
        
        public DepartmentStatisticsTask(List<Enrollment> grades, int start, int end) {
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
            Map<String, double[]> courseTotals = new HashMap<>();
            Map<String, Long> gradeDistribution = new HashMap<>();
            double totalGrade = 0.0;
            
            for (int i = start; i < end; i++) {
                Enrollment grade = grades.get(i);
                
                double[] totals = courseTotals.computeIfAbsent(grade.getCourseId(), k -> new double[2]);
                totals[0] += grade.getNumericGrade();
                totals[1]++;
                
                gradeDistribution.merge(letterOf(grade), 1L, Long::sum);
                totalGrade += grade.getNumericGrade();
            }
            
            return new DepartmentStatisticsResult(courseTotals, totalGrade, end - start, gradeDistribution);
        }
        
        // Partial results carry sums and counts (not averages) so that merging halves of
        // different sizes yields the exact, count-weighted average.
        private DepartmentStatisticsResult mergeResults(DepartmentStatisticsResult left, 
                                                       DepartmentStatisticsResult right) {
            Map<String, double[]> mergedCourseTotals = new HashMap<>();
            left.courseTotals.forEach((course, totals) -> mergedCourseTotals.put(course, totals.clone()));
            right.courseTotals.forEach((course, totals) -> 
                mergedCourseTotals.merge(course, totals.clone(),
                    (a, b) -> new double[] { a[0] + b[0], a[1] + b[1] }));
            
            Map<String, Long> mergedGradeDistribution = new HashMap<>(left.getGradeDistribution());
            right.getGradeDistribution().forEach((grade, count) -> 
                mergedGradeDistribution.merge(grade, count, Long::sum));
            
            return new DepartmentStatisticsResult(mergedCourseTotals,
                                                left.totalGrade + right.totalGrade,
                                                left.count + right.count,
                                                mergedGradeDistribution);
        }
    }
    
    private static class DepartmentStatisticsResult {
        private final Map<String, double[]> courseTotals; // courseId -> {sum, count}
        private final double totalGrade;
        private final long count;
        private final Map<String, Long> gradeDistribution;
        
        public DepartmentStatisticsResult(Map<String, double[]> courseTotals,
                                        double totalGrade, long count,
                                        Map<String, Long> gradeDistribution) {
            this.courseTotals = courseTotals;
            this.totalGrade = totalGrade;
            this.count = count;
            this.gradeDistribution = gradeDistribution;
        }
        
        public Map<String, Double> getCourseAverages() {
            Map<String, Double> averages = new HashMap<>();
            courseTotals.forEach((course, totals) -> averages.put(course, totals[0] / totals[1]));
            return averages;
        }
        public double getOverallAverage() { return count > 0 ? totalGrade / count : 0.0; }
        public Map<String, Long> getGradeDistribution() { return gradeDistribution; }
    }
}