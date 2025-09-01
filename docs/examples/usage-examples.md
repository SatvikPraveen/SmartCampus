# SmartCampus Usage Examples and Code Samples

**Location:** `docs/examples/usage-examples.md`

## Overview

This document provides comprehensive usage examples and code samples for the SmartCampus university management system. It demonstrates how to use the various components, services, and features of the application with practical, real-world scenarios.

## Table of Contents

1. [Basic Entity Operations](#basic-entity-operations)
2. [Service Layer Usage](#service-layer-usage)
3. [Advanced Query Examples](#advanced-query-examples)
4. [Concurrent Processing Examples](#concurrent-processing-examples)
5. [Event-Driven Programming](#event-driven-programming)
6. [Caching Examples](#caching-examples)
7. [File I/O Operations](#file-io-operations)
8. [Reporting and Analytics](#reporting-and-analytics)
9. [Security Implementation](#security-implementation)
10. [Testing Examples](#testing-examples)

## Basic Entity Operations

### Creating and Managing Students

```java
// Example 1: Creating a new student using Builder pattern
public class StudentManagementExample {

    private final StudentService studentService;
    private final ValidationUtil validationUtil;

    public void createStudentExample() {
        // Using Builder pattern for complex object creation
        Student student = new StudentBuilder()
            .setId("STU2024001")
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@smartcampus.edu")
            .setPhoneNumber("+1-555-0123")
            .setDateOfBirth(LocalDate.of(2002, 3, 15))
            .setMajor("Computer Science")
            .setEnrollmentYear(2024)
            .setGPA(3.75)
            .build();

        try {
            // Validate student data before saving
            validationUtil.validateStudent(student);

            // Save student to database
            Student savedStudent = studentService.createStudent(student);

            System.out.println("Student created successfully: " + savedStudent.getFullName());

        } catch (ValidationException e) {
            System.err.println("Validation error: " + e.getMessage());
        } catch (StudentCreationException e) {
            System.err.println("Failed to create student: " + e.getMessage());
        }
    }

    // Example 2: Updating student information
    public void updateStudentExample(String studentId) {
        Optional<Student> studentOpt = studentService.findStudentById(studentId);

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            // Update specific fields
            Student updatedStudent = new StudentBuilder()
                .from(student) // Copy existing data
                .setEmail("new.email@smartcampus.edu")
                .setPhoneNumber("+1-555-9999")
                .setMajor("Data Science") // Change major
                .build();

            try {
                studentService.updateStudent(updatedStudent);
                System.out.println("Student updated successfully");
            } catch (Exception e) {
                System.err.println("Update failed: " + e.getMessage());
            }
        } else {
            System.out.println("Student not found with ID: " + studentId);
        }
    }

    // Example 3: Searching students with various criteria
    public void searchStudentsExample() {
        // Search by major
        List<Student> csStudents = studentService.findStudentsByMajor("Computer Science");
        System.out.println("Computer Science students: " + csStudents.size());

        // Search honor students (GPA >= 3.5)
        List<Student> honorStudents = studentService.getHonorStudents();
        honorStudents.forEach(student ->
            System.out.println(student.getFullName() + " - GPA: " + student.getGPA())
        );

        // Advanced search with multiple criteria
        List<Student> filteredStudents = studentService.findStudents(
            StudentSearchCriteria.builder()
                .major("Computer Science")
                .minGPA(3.0)
                .enrollmentYear(2024)
                .build()
        );

        // Using Stream API for complex filtering
        List<Student> seniorHonorStudents = studentService.getAllStudents()
            .stream()
            .filter(s -> s.getEnrollmentYear() <= 2021) // Senior students
            .filter(s -> s.getGPA() >= 3.5) // Honor students
            .sorted(Comparator.comparing(Student::getGPA).reversed())
            .collect(Collectors.toList());
    }
}
```

### Course Management

```java
public class CourseManagementExample {

    private final CourseService courseService;
    private final ProfessorService professorService;

    // Example 1: Creating a new course
    public void createCourseExample() {
        Course course = new CourseBuilder()
            .setCourseId("CS101")
            .setTitle("Introduction to Programming")
            .setDescription("Fundamental programming concepts using Java")
            .setCredits(3)
            .setMaxEnrollment(50)
            .setSemester(Semester.FALL)
            .setYear(2024)
            .addPrerequisite("MATH101")
            .setSchedule(new Schedule("MWF", "10:00 AM", "11:00 AM", "Room 101"))
            .build();

        try {
            Course savedCourse = courseService.createCourse(course);
            System.out.println("Course created: " + savedCourse.getTitle());

            // Assign professor to course
            Optional<Professor> professor = professorService.findByEmployeeId("PROF001");
            if (professor.isPresent()) {
                courseService.assignProfessor(savedCourse.getCourseId(), professor.get().getId());
                System.out.println("Professor assigned to course");
            }

        } catch (CourseCreationException e) {
            System.err.println("Failed to create course: " + e.getMessage());
        }
    }

    // Example 2: Managing course enrollment
    public void manageCourseEnrollmentExample(String courseId) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException(courseId));

        // Check enrollment status
        int currentEnrollment = course.getCurrentEnrollment();
        int maxEnrollment = course.getMaxEnrollment();

        System.out.println("Course: " + course.getTitle());
        System.out.println("Enrollment: " + currentEnrollment + "/" + maxEnrollment);

        if (course.canAcceptEnrollment()) {
            System.out.println("Course is accepting new enrollments");
        } else {
            System.out.println("Course is full or closed");
        }

        // Get enrolled students
        List<Student> enrolledStudents = courseService.getEnrolledStudents(courseId);
        enrolledStudents.forEach(student ->
            System.out.println("Enrolled: " + student.getFullName())
        );
    }

    // Example 3: Course scheduling and conflicts
    public void courseSchedulingExample() {
        // Find courses by time slot
        List<Course> mondayMorningCourses = courseService.findCoursesBySchedule(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0)
        );

        // Check for scheduling conflicts
        boolean hasConflict = courseService.checkScheduleConflict(
            "CS101", "MATH201"
        );

        if (hasConflict) {
            System.out.println("Schedule conflict detected!");
        }

        // Generate course schedule report
        Map<String, List<Course>> scheduleByDay = courseService.getAllCourses()
            .stream()
            .filter(course -> course.getSchedule() != null)
            .collect(Collectors.groupingBy(course ->
                course.getSchedule().getDays()
            ));

        scheduleByDay.forEach((day, courses) -> {
            System.out.println(day + ":");
            courses.forEach(course ->
                System.out.println("  " + course.getCourseId() + " - " + course.getTitle())
            );
        });
    }
}
```

## Service Layer Usage

### Student Service Advanced Examples

```java
@Service
public class StudentServiceUsageExample {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final GradeService gradeService;

    // Example 1: Bulk student operations
    public void bulkStudentOperationsExample() {
        // Create multiple students using parallel processing
        List<Student> newStudents = createSampleStudents();

        CompletableFuture<List<Student>> bulkCreateFuture =
            studentService.createStudentsBulk(newStudents);

        bulkCreateFuture.thenAccept(createdStudents -> {
            System.out.println("Created " + createdStudents.size() + " students");
            createdStudents.forEach(student ->
                System.out.println("  " + student.getFullName())
            );
        }).exceptionally(throwable -> {
            System.err.println("Bulk creation failed: " + throwable.getMessage());
            return null;
        });
    }

    // Example 2: Student academic progress tracking
    public void trackAcademicProgressExample(String studentId) {
        Optional<Student> studentOpt = studentService.findStudentById(studentId);

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            // Get student's current courses
            List<Course> currentCourses = studentService.getStudentCurrentCourses(studentId);

            // Calculate current semester GPA
            double currentGPA = gradeService.calculateCurrentSemesterGPA(studentId);

            // Get academic standing
            AcademicStanding standing = studentService.getAcademicStanding(studentId);

            // Generate progress report
            StudentProgressReport report = StudentProgressReport.builder()
                .student(student)
                .currentCourses(currentCourses)
                .currentGPA(currentGPA)
                .cumulativeGPA(student.getGPA())
                .academicStanding(standing)
                .creditsCompleted(studentService.getCreditsCompleted(studentId))
                .creditsInProgress(studentService.getCreditsInProgress(studentId))
                .build();

            System.out.println("=== Academic Progress Report ===");
            System.out.println("Student: " + report.getStudent().getFullName());
            System.out.println("Current GPA: " + report.getCurrentGPA());
            System.out.println("Cumulative GPA: " + report.getCumulativeGPA());
            System.out.println("Academic Standing: " + report.getAcademicStanding());
            System.out.println("Credits Completed: " + report.getCreditsCompleted());
            System.out.println("Current Courses:");

            report.getCurrentCourses().forEach(course ->
                System.out.println("  " + course.getCourseId() + " - " + course.getTitle())
            );
        }
    }

    // Example 3: Student analytics and insights
    public void studentAnalyticsExample() {
        // Analyze student performance by major
        Map<String, Double> averageGPAByMajor = studentService.getAllStudents()
            .stream()
            .collect(Collectors.groupingBy(
                Student::getMajor,
                Collectors.averagingDouble(Student::getGPA)
            ));

        System.out.println("Average GPA by Major:");
        averageGPAByMajor.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry ->
                System.out.printf("  %s: %.2f%n", entry.getKey(), entry.getValue())
            );

        // Find students at risk (GPA < 2.0)
        List<Student> atRiskStudents = studentService.getStudentsAtRisk();
        System.out.println("\nStudents at Risk (" + atRiskStudents.size() + "):");
        atRiskStudents.forEach(student ->
            System.out.println("  " + student.getFullName() + " - GPA: " + student.getGPA())
        );

        // Enrollment trends analysis
        Map<Integer, Long> enrollmentByYear = studentService.getAllStudents()
            .stream()
            .collect(Collectors.groupingBy(
                Student::getEnrollmentYear,
                Collectors.counting()
            ));

        System.out.println("\nEnrollment by Year:");
        enrollmentByYear.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " students")
            );
    }

    private List<Student> createSampleStudents() {
        return IntStream.range(1, 11)
            .mapToObj(i -> new StudentBuilder()
                .setId("STU202400" + i)
                .setFirstName("Student" + i)
                .setLastName("Test")
                .setEmail("student" + i + "@test.edu")
                .setMajor("Computer Science")
                .setEnrollmentYear(2024)
                .setGPA(2.0 + (i * 0.2)) // GPA from 2.2 to 4.0
                .build())
            .collect(Collectors.toList());
    }
}
```

### Enrollment Service Examples

```java
@Service
public class EnrollmentServiceUsageExample {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final CourseService courseService;

    // Example 1: Complete enrollment workflow
    public void enrollmentWorkflowExample(String studentId, String courseId) {
        try {
            // Step 1: Validate student and course exist
            Student student = studentService.findStudentById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

            Course course = courseService.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

            // Step 2: Check prerequisites
            if (!enrollmentService.checkPrerequisites(studentId, courseId)) {
                List<String> missingPrereqs = enrollmentService.getMissingPrerequisites(studentId, courseId);
                System.out.println("Missing prerequisites: " + String.join(", ", missingPrereqs));
                return;
            }

            // Step 3: Check course capacity
            if (!course.canAcceptEnrollment()) {
                // Add to waitlist
                enrollmentService.addToWaitlist(studentId, courseId);
                System.out.println("Course is full. Added to waitlist.");
                return;
            }

            // Step 4: Check for schedule conflicts
            List<Course> currentCourses = studentService.getStudentCurrentCourses(studentId);
            boolean hasConflict = currentCourses.stream()
                .anyMatch(c -> courseService.checkScheduleConflict(c.getCourseId(), courseId));

            if (hasConflict) {
                System.out.println("Schedule conflict detected!");
                return;
            }

            // Step 5: Process enrollment
            CompletableFuture<Enrollment> enrollmentFuture =
                enrollmentService.enrollStudentAsync(studentId, courseId);

            Enrollment enrollment = enrollmentFuture.get(30, TimeUnit.SECONDS);

            System.out.println("Enrollment successful!");
            System.out.println("Enrollment ID: " + enrollment.getEnrollmentId());
            System.out.println("Student: " + student.getFullName());
            System.out.println("Course: " + course.getTitle());
            System.out.println("Status: " + enrollment.getStatus());

        } catch (PrerequisiteNotMetException e) {
            System.err.println("Prerequisites not met: " + e.getMessage());
        } catch (CourseFullException e) {
            System.err.println("Course is full: " + e.getMessage());
        } catch (ScheduleConflictException e) {
            System.err.println("Schedule conflict: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Enrollment failed: " + e.getMessage());
        }
    }

    // Example 2: Batch enrollment processing
    public void batchEnrollmentExample() {
        // Create enrollment requests
        List<EnrollmentRequest> requests = Arrays.asList(
            new EnrollmentRequest("STU001", "CS101"),
            new EnrollmentRequest("STU002", "CS101"),
            new EnrollmentRequest("STU003", "MATH201"),
            new EnrollmentRequest("STU004", "PHYS101")
        );

        // Process all enrollments concurrently
        List<CompletableFuture<EnrollmentResult>> futures = requests.stream()
            .map(request -> enrollmentService.processEnrollmentAsync(request))
            .collect(Collectors.toList());

        // Wait for all to complete and collect results
        CompletableFuture<List<EnrollmentResult>> allResults =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));

        try {
            List<EnrollmentResult> results = allResults.get(60, TimeUnit.SECONDS);

            // Process results
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failureCount = results.size() - successCount;

            System.out.println("Batch enrollment completed:");
            System.out.println("  Successful: " + successCount);
            System.out.println("  Failed: " + failureCount);

            // Show failed enrollments
            results.stream()
                .filter(r -> !r.isSuccess())
                .forEach(r -> System.out.println("  Failed: " + r.getStudentId() +
                    " -> " + r.getCourseId() + " (" + r.getErrorMessage() + ")"));

        } catch (Exception e) {
            System.err.println("Batch enrollment failed: " + e.getMessage());
        }
    }

    // Example 3: Waitlist management
    public void waitlistManagementExample(String courseId) {
        // Get current waitlist
        List<WaitlistEntry> waitlist = enrollmentService.getWaitlist(courseId);

        System.out.println("Waitlist for course " + courseId + ":");
        waitlist.forEach(entry ->
            System.out.println("  " + entry.getPosition() + ". " +
                entry.getStudentId() + " (added: " + entry.getDateAdded() + ")")
        );

        // Simulate a student dropping to make space
        if (!waitlist.isEmpty()) {
            System.out.println("\nProcessing waitlist...");

            // Process next student in waitlist
            CompletableFuture<Optional<Enrollment>> nextEnrollmentFuture =
                enrollmentService.processNextInWaitlist(courseId);

            nextEnrollmentFuture.thenAccept(enrollmentOpt -> {
                if (enrollmentOpt.isPresent()) {
                    Enrollment enrollment = enrollmentOpt.get();
                    System.out.println("Enrolled from waitlist: " + enrollment.getStudentId());
                } else {
                    System.out.println("No eligible students in waitlist");
                }
            });
        }
    }

    // Example 4: Enrollment statistics and reporting
    public void enrollmentStatisticsExample() {
        // Overall enrollment statistics
        EnrollmentStatistics stats = enrollmentService.getEnrollmentStatistics();

        System.out.println("=== Enrollment Statistics ===");
        System.out.println("Total Enrollments: " + stats.getTotalEnrollments());
        System.out.println("Active Enrollments: " + stats.getActiveEnrollments());
        System.out.println("Completed Enrollments: " + stats.getCompletedEnrollments());
        System.out.println("Withdrawn Enrollments: " + stats.getWithdrawnEnrollments());

        // Enrollment by course
        Map<String, Long> enrollmentsByCourse = enrollmentService.getEnrollmentsByCourse();
        System.out.println("\nEnrollments by Course:");
        enrollmentsByCourse.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry ->
                System.out.println("  " + entry.getKey() + ": " + entry.getValue())
            );

        // Peak enrollment times analysis
        Map<LocalDateTime, Long> enrollmentsByHour = enrollmentService
            .getEnrollmentsByTimePattern();

        System.out.println("\nPeak Enrollment Hours:");
        enrollmentsByHour.entrySet()
            .stream()
            .sorted(Map.Entry.<LocalDateTime, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry ->
                System.out.println("  " + entry.getKey().getHour() + ":00 - " + entry.getValue())
            );
    }
}
```

## Advanced Query Examples

### Complex Data Retrieval

```java
@Repository
public class AdvancedQueryExamples {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;

    // Example 1: Complex student queries with Stream API
    public void complexStudentQueriesExample() {
        // Find top-performing students in each major
        Map<String, Optional<Student>> topStudentsByMajor = studentRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(
                Student::getMajor,
                Collectors.maxBy(Comparator.comparing(Student::getGPA))
            ));

        System.out.println("Top students by major:");
        topStudentsByMajor.forEach((major, studentOpt) -> {
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                System.out.printf("  %s: %s (GPA: %.2f)%n",
                    major, student.getFullName(), student.getGPA());
            }
        });

        // Find students with improving grades trend
        List<Student> improvingStudents = studentRepository.findAll()
            .stream()
            .filter(this::hasImprovingGradeTrend)
            .sorted(Comparator.comparing(Student::getGPA).reversed())
            .collect(Collectors.toList());

        System.out.println("\nStudents with improving grade trends:");
        improvingStudents.forEach(student ->
            System.out.println("  " + student.getFullName() + " - Current GPA: " + student.getGPA())
        );

        // Complex multi-criteria search
        StudentSearchCriteria criteria = StudentSearchCriteria.builder()
            .majors(Arrays.asList("Computer Science", "Data Science"))
            .minGPA(3.5)
            .enrollmentYearRange(2020, 2024)
            .hasHonorsDesignation(true)
            .build();

        List<Student> complexSearchResults = performComplexStudentSearch(criteria);
        System.out.println("\nComplex search results: " + complexSearchResults.size() + " students");
    }

    private boolean hasImprovingGradeTrend(Student student) {
        List<Grade> recentGrades = gradeRepository.findByStudentIdOrderByDateDesc(
            student.getId(),
            PageRequest.of(0, 10)
        );

        if (recentGrades.size() < 3) return false;

        // Simple trend analysis - compare first half vs second half of recent grades
        double firstHalfAvg = recentGrades.subList(0, recentGrades.size()/2)
            .stream()
            .mapToDouble(Grade::getPercentage)
            .average()
            .orElse(0.0);

        double secondHalfAvg = recentGrades.subList(recentGrades.size()/2, recentGrades.size())
            .stream()
            .mapToDouble(Grade::getPercentage)
            .average()
            .orElse(0.0);

        return firstHalfAvg > secondHalfAvg + 5.0; // 5% improvement threshold
    }

    // Example 2: Course enrollment analytics
    public void courseEnrollmentAnalyticsExample() {
        // Find most popular courses by enrollment
        List<CourseEnrollmentStats> popularCourses = courseRepository.findAll()
            .stream()
            .map(this::calculateCourseStats)
            .sorted(Comparator.comparing(CourseEnrollmentStats::getEnrollmentRate).reversed())
            .limit(10)
            .collect(Collectors.toList());

        System.out.println("Most popular courses:");
        popularCourses.forEach(stats ->
            System.out.printf("  %s: %.1f%% enrollment rate (%d/%d)%n",
                stats.getCourse().getTitle(),
                stats.getEnrollmentRate() * 100,
                stats.getCurrentEnrollment(),
                stats.getMaxEnrollment())
        );

        // Analyze course completion rates
        Map<String, Double> completionRates = courseRepository.findAll()
            .stream()
            .collect(Collectors.toMap(
                Course::getCourseId,
                this::calculateCompletionRate
            ));

        System.out.println("\nCourse completion rates:");
        completionRates.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry ->
                System.out.printf("  %s: %.1f%% completion%n",
                    entry.getKey(), entry.getValue() * 100)
            );

        // Find courses with capacity issues
        List<Course> overCapacityCourses = courseRepository.findAll()
            .stream()
            .filter(course -> {
                int waitlistSize = enrollmentRepository.countByStatusAndCourseId(
                    EnrollmentStatus.WAITLISTED, course.getCourseId()
                );
                return waitlistSize > course.getMaxEnrollment() * 0.2; // >20% waitlisted
            })
            .sorted(Comparator.comparing(this::getWaitlistSize).reversed())
            .collect(Collectors.toList());

        System.out.println("\nCourses with high demand (large waitlists):");
        overCapacityCourses.forEach(course -> {
            int waitlistSize = getWaitlistSize(course);
            System.out.printf("  %s: %d students waitlisted%n",
                course.getTitle(), waitlistSize);
        });
    }

    private CourseEnrollmentStats calculateCourseStats(Course course) {
        int currentEnrollment = enrollmentRepository.countByStatusAndCourseId(
            EnrollmentStatus.ENROLLED, course.getCourseId()
        );

        double enrollmentRate = (double) currentEnrollment / course.getMaxEnrollment();

        return new CourseEnrollmentStats(course, currentEnrollment, enrollmentRate);
    }

    private double calculateCompletionRate(Course course) {
        long totalEnrollments = enrollmentRepository.countByCourseId(course.getCourseId());
        long completedEnrollments = enrollmentRepository.countByStatusAndCourseId(
            EnrollmentStatus.COMPLETED, course.getCourseId()
        );

        return totalEnrollments > 0 ? (double) completedEnrollments / totalEnrollments : 0.0;
    }

    private int getWaitlistSize(Course course) {
        return enrollmentRepository.countByStatusAndCourseId(
            EnrollmentStatus.WAITLISTED, course.getCourseId()
        );
    }

    // Example 3: Grade analysis and academic insights
    public void gradeAnalysisExample() {
        // Analyze grade distribution across all courses
        Map<GradeLevel, Long> gradeDistribution = gradeRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(
                Grade::getLetterGrade,
                Collectors.counting()
            ));

        System.out.println("Overall grade distribution:");
        gradeDistribution.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                System.out.println("  " + entry.getKey() + ": " + entry.getValue())
            );

        // Find courses with unusual grade distributions
        List<String> easyCoursesIds = courseRepository.findAll()
            .stream()
            .filter(course -> {
                double averageGrade = gradeRepository.findByCourseId(course.getCourseId())
                    .stream()
                    .mapToDouble(Grade::getPercentage)
                    .average()
                    .orElse(0.0);
                return averageGrade > 90.0; // High average indicates easy course
            })
            .map(Course::getCourseId)
            .collect(Collectors.toList());

        System.out.println("\nCourses with high average grades (>90%):");
        easyCoursesIds.forEach(courseId -> {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course != null) {
                double avg = gradeRepository.findByCourseId(courseId)
                    .stream()
                    .mapToDouble(Grade::getPercentage)
                    .average()
                    .orElse(0.0);
                System.out.printf("  %s: %.1f%% average%n", course.getTitle(), avg);
            }
        });

        // Identify students struggling in multiple courses
        Map<String, Long> failingGradesByStudent = gradeRepository.findAll()
            .stream()
            .filter(grade -> grade.getPercentage() < 60.0) // Failing grade
            .collect(Collectors.groupingBy(
                Grade::getStudentId,
                Collectors.counting()
            ));

        List<String> atRiskStudentIds = failingGradesByStudent.entrySet()
            .stream()
            .filter(entry -> entry.getValue() >= 2) // 2+ failing grades
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        System.out.println("\nStudents at risk (2+ failing grades):");
        atRiskStudentIds.forEach(studentId -> {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null) {
                long failingCount = failingGradesByStudent.get(studentId);
                System.out.println("  " + student.getFullName() +
                    " (" + failingCount + " failing grades)");
            }
        });
    }

    private List<Student> performComplexStudentSearch(StudentSearchCriteria criteria) {
        return studentRepository.findAll()
            .stream()
            .filter(student -> {
                // Major filter
                if (criteria.getMajors() != null && !criteria.getMajors().isEmpty()) {
                    if (!criteria.getMajors().contains(student.getMajor())) {
                        return false;
                    }
                }

                // GPA filter
                if (criteria.getMinGPA() != null && student.getGPA() < criteria.getMinGPA()) {
                    return false;
                }

                // Enrollment year range filter
                if (criteria.getEnrollmentYearStart() != null &&
                    student.getEnrollmentYear() < criteria.getEnrollmentYearStart()) {
                    return false;
                }

                if (criteria.getEnrollmentYearEnd() != null &&
                    student.getEnrollmentYear() > criteria.getEnrollmentYearEnd()) {
                    return false;
                }

                // Honors designation filter
                if (criteria.isHasHonorsDesignation() && student.getGPA() < 3.5) {
                    return false;
                }

                return true;
            })
            .collect(Collectors.toList());
    }
}

// Supporting classes
@Data
@Builder
class StudentSearchCriteria {
    private List<String> majors;
    private Double minGPA;
    private Double maxGPA;
    private Integer enrollmentYearStart;
    private Integer enrollmentYearEnd;
    private boolean hasHonorsDesignation;

    public Integer getEnrollmentYearRange(int start, int end) {
        this.enrollmentYearStart = start;
        this.enrollmentYearEnd = end;
        return start;
    }
}

@Data
@AllArgsConstructor
class CourseEnrollmentStats {
    private Course course;
    private int currentEnrollment;
    private double enrollmentRate;
    private int maxEnrollment;

    public CourseEnrollmentStats(Course course, int currentEnrollment, double enrollmentRate) {
        this.course = course;
        this.currentEnrollment = currentEnrollment;
        this.enrollmentRate = enrollmentRate;
        this.maxEnrollment = course.getMaxEnrollment();
    }
}
```

## Concurrent Processing Examples

### Asynchronous Operations

```java
@Service
public class ConcurrentProcessingExamples {

    private final EnrollmentProcessor enrollmentProcessor;
    private final DataSyncManager dataSyncManager;
    private final BatchProcessor batchProcessor;
    private final AsyncNotificationSender notificationSender;

    // Example 1: Concurrent enrollment processing
    public void concurrentEnrollmentExample() {
        List<EnrollmentRequest> requests = createEnrollmentRequests(100);

        System.out.println("Processing " + requests.size() + " enrollment requests concurrently...");

        // Process enrollments in parallel
        List<CompletableFuture<Enrollment>> futures = requests.stream()
            .map(enrollmentProcessor::processEnrollmentAsync)
            .collect(Collectors.toList());

        // Create a combined future that completes when all are done
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        // Handle completion with timeout
        CompletableFuture<List<EnrollmentResult>> results = allOf
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .map(enrollment -> new EnrollmentResult(enrollment, true, null))
                .collect(Collectors.toList())
            )
            .exceptionally(throwable -> {
                System.err.println("Some enrollments failed: " + throwable.getMessage());
                return futures.stream()
                    .map(future -> {
                        try {
                            Enrollment enrollment = future.get(1, TimeUnit.SECONDS);
                            return new EnrollmentResult(enrollment, true, null);
                        } catch (Exception e) {
                            return new EnrollmentResult(null, false, e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());
            });

        try {
            List<EnrollmentResult> completedResults = results.get(30, TimeUnit.SECONDS);

            long successCount = completedResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failureCount = completedResults.size() - successCount;

            System.out.println("Concurrent enrollment completed:");
            System.out.println("  Successful: " + successCount);
            System.out.println("  Failed: " + failureCount);
            System.out.println("  Total processing time: " +
                stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");

        } catch (Exception e) {
            System.err.println("Concurrent processing failed: " + e.getMessage());
        }
    }

    // Example 2: Parallel grade calculations
    public void parallelGradeCalculationExample() {
        List<String> studentIds = studentRepository.findAll()
            .stream()
            .map(Student::getId)
            .collect(Collectors.toList());

        System.out.println("Calculating GPAs for " + studentIds.size() + " students...");

        // Parallel GPA calculation using custom ForkJoinTask
        ConcurrentGradeCalculator calculator = new ConcurrentGradeCalculator();

        CompletableFuture<Map<String, Double>> gpaCalculationFuture =
            calculator.calculateGPAsParallel(studentIds);

        gpaCalculationFuture.thenAccept(gpaResults -> {
            System.out.println("GPA calculations completed:");

            // Find top 10 students
            gpaResults.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    Student student = studentRepository.findById(entry.getKey()).orElse(null);
                    if (student != null) {
                        System.out.printf("  %s: %.2f%n",
                            student.getFullName(), entry.getValue());
                    }
                });
        }).exceptionally(throwable -> {
            System.err.println("GPA calculation failed: " + throwable.getMessage());
            return null;
        });
    }

    // Example 3: Asynchronous data synchronization
    public void asyncDataSyncExample() {
        System.out.println("Starting asynchronous data synchronization...");

        // Synchronize different data types concurrently
        CompletableFuture<Void> studentSync = dataSyncManager.syncStudentData()
            .thenRun(() -> System.out.println("Student data sync completed"));

        CompletableFuture<Void> courseSync = dataSyncManager.syncCourseData()
            .thenRun(() -> System.out.println("Course data sync completed"));

        CompletableFuture<Void> gradeSync = dataSyncManager.syncGradeData()
            .thenRun(() -> System.out.println("Grade data sync completed"));

        // Combine all sync operations
        CompletableFuture<Void> allSyncs = CompletableFuture.allOf(
            studentSync, courseSync, gradeSync
        );

        allSyncs.thenRun(() -> {
            System.out.println("All data synchronization completed successfully!");

            // Trigger dependent operations
            triggerPostSyncOperations();

        }).exceptionally(throwable -> {
            System.err.println("Data synchronization failed: " + throwable.getMessage());

            // Implement retry logic or fallback
            scheduleRetrySync();
            return null;
        });

        // Continue with other operations while sync runs in background
        performOtherOperations();
    }

    // Example 4: Producer-Consumer pattern for notification processing
    public void notificationProcessingExample() {
        BlockingQueue<NotificationTask> notificationQueue = new LinkedBlockingQueue<>(1000);

        // Producer thread - generates notifications
        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    NotificationTask task = createNotificationTask(i);
                    notificationQueue.put(task);
                    Thread.sleep(50); // Simulate work
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer threads - process notifications
        List<CompletableFuture<Void>> consumers = IntStream.range(0, 3)
            .mapToObj(consumerId -> CompletableFuture.runAsync(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        NotificationTask task = notificationQueue.take();
                        processNotificationTask(task, consumerId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }))
            .collect(Collectors.toList());

        // Wait for producer to finish, then stop consumers
        producer.thenRun(() -> {
            System.out.println("Producer finished, stopping consumers...");
            consumers.forEach(consumer -> consumer.cancel(true));
        });

        // Monitor processing stats
        CompletableFuture.runAsync(() -> {
            try {
                while (!producer.isDone()) {
                    System.out.println("Queue size: " + notificationQueue.size());
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // Helper methods
    private List<EnrollmentRequest> createEnrollmentRequests(int count) {
        return IntStream.range(1, count + 1)
            .mapToObj(i -> new EnrollmentRequest("STU" + String.format("%04d", i), "CS101"))
            .collect(Collectors.toList());
    }

    private NotificationTask createNotificationTask(int id) {
        return new NotificationTask(
            "NOTIF" + id,
            "Test notification " + id,
            Arrays.asList("student" + id + "@test.edu")
        );
    }

    private void processNotificationTask(NotificationTask task, int consumerId) {
        System.out.println("Consumer " + consumerId + " processing: " + task.getId());
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void triggerPostSyncOperations() {
        System.out.println("Triggering post-sync operations...");
        // Implementation here
    }

    private void scheduleRetrySync() {
        System.out.println("Scheduling sync retry...");
        // Implementation here
    }

    private void performOtherOperations() {
        System.out.println("Performing other operations while sync runs...");
        // Implementation here
    }
}
```

## Event-Driven Programming

### Event System Implementation

```java
// Example 1: Event publishing and handling
@Component
public class EventDrivenExamples {

    private final EventManager eventManager;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public void setupEventListeners() {
        // Register event listeners
        eventManager.subscribe(StudentEnrolledEvent.class, new EnrollmentNotificationListener());
        eventManager.subscribe(GradeUpdatedEvent.class, new GradeUpdateListener());
        eventManager.subscribe(CourseCreatedEvent.class, new CourseCreationListener());

        // Register multiple listeners for the same event
        eventManager.subscribe(StudentEnrolledEvent.class, auditService);
        eventManager.subscribe(GradeUpdatedEvent.class, auditService);
    }

    // Example: Student enrollment event workflow
    public void handleStudentEnrollment(String studentId, String courseId) {
        try {
            // Perform the enrollment
            Enrollment enrollment = enrollmentService.enrollStudent(studentId, courseId);

            // Publish event
            StudentEnrolledEvent event = new StudentEnrolledEvent(
                studentId,
                courseId,
                enrollment.getEnrollmentDate(),
                enrollment.getEnrollmentId()
            );

            eventManager.publish(event);

        } catch (EnrollmentException e) {
            // Publish failure event
            EnrollmentFailedEvent failureEvent = new EnrollmentFailedEvent(
                studentId,
                courseId,
                e.getMessage(),
                LocalDateTime.now()
            );

            eventManager.publish(failureEvent);
        }
    }
}

// Event classes
public class StudentEnrolledEvent extends Event {
    private final String studentId;
    private final String courseId;
    private final LocalDateTime enrollmentDate;
    private final String enrollmentId;

    public StudentEnrolledEvent(String studentId, String courseId, LocalDateTime enrollmentDate, String enrollmentId) {
        super("STUDENT_ENROLLED");
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.enrollmentId = enrollmentId;
    }

    // Getters...
}

public class GradeUpdatedEvent extends Event {
    private final String studentId;
    private final String courseId;
    private final Grade oldGrade;
    private final Grade newGrade;
    private final String updatedBy;

    public GradeUpdatedEvent(String studentId, String courseId, Grade oldGrade, Grade newGrade, String updatedBy) {
        super("GRADE_UPDATED");
        this.studentId = studentId;
        this.courseId = courseId;
        this.oldGrade = oldGrade;
        this.newGrade = newGrade;
        this.updatedBy = updatedBy;
    }

    // Getters...
}

// Event listeners
@Component
public class EnrollmentNotificationListener implements EventListener<StudentEnrolledEvent> {

    private final NotificationService notificationService;
    private final StudentService studentService;
    private final CourseService courseService;

    @Override
    public void handle(StudentEnrolledEvent event) {
        try {
            // Get student and course details
            Student student = studentService.findStudentById(event.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException(event.getStudentId()));

            Course course = courseService.findById(event.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException(event.getCourseId()));

            // Send email notification to student
            String emailContent = String.format(
                "Dear %s,\n\nYou have been successfully enrolled in %s (%s).\n" +
                "Enrollment Date: %s\nEnrollment ID: %s\n\nBest regards,\nSmartCampus Team",
                student.getFullName(),
                course.getTitle(),
                course.getCourseId(),
                event.getEnrollmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                event.getEnrollmentId()
            );

            notificationService.sendEmailAsync(
                student.getEmail(),
                "Course Enrollment Confirmation",
                emailContent
            );

            // Send SMS notification if phone number is available
            if (student.getPhoneNumber() != null) {
                String smsContent = String.format(
                    "Enrolled in %s (%s). Enrollment ID: %s",
                    course.getTitle(),
                    course.getCourseId(),
                    event.getEnrollmentId()
                );

                notificationService.sendSMSAsync(student.getPhoneNumber(), smsContent);
            }

        } catch (Exception e) {
            System.err.println("Failed to send enrollment notification: " + e.getMessage());
        }
    }
}

@Component
public class GradeUpdateListener implements EventListener<GradeUpdatedEvent> {

    private final StudentService studentService;
    private final NotificationService notificationService;

    @Override
    public void handle(GradeUpdatedEvent event) {
        try {
            Student student = studentService.findStudentById(event.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException(event.getStudentId()));

            // Calculate impact on GPA
            double oldGPA = calculateGPAWithGrade(event.getStudentId(), event.getOldGrade());
            double newGPA = calculateGPAWithGrade(event.getStudentId(), event.getNewGrade());
            double gpaChange = newGPA - oldGPA;

            // Send notification based on grade change significance
            if (Math.abs(gpaChange) > 0.1) { // Significant GPA change
                String notificationContent = String.format(
                    "Grade updated for %s in course %s.\n" +
                    "Previous grade: %.2f%% (Letter: %s)\n" +
                    "New grade: %.2f%% (Letter: %s)\n" +
                    "GPA change: %+.3f (New GPA: %.3f)",
                    student.getFullName(),
                    event.getCourseId(),
                    event.getOldGrade().getPercentage(),
                    event.getOldGrade().getLetterGrade(),
                    event.getNewGrade().getPercentage(),
                    event.getNewGrade().getLetterGrade(),
                    gpaChange,
                    newGPA
                );

                notificationService.sendEmailAsync(
                    student.getEmail(),
                    "Grade Update Notification",
                    notificationContent
                );
            }

            // Update student's cumulative GPA
            studentService.updateGPA(event.getStudentId(), newGPA);

        } catch (Exception e) {
            System.err.println("Failed to process grade update event: " + e.getMessage());
        }
    }

    private double calculateGPAWithGrade(String studentId, Grade grade) {
        // Implementation to calculate GPA including the specified grade
        return gradeService.calculateGPAWithGrade(studentId, grade);
    }
}

// Example 2: Complex event orchestration
@Component
public class EventOrchestrationExample {

    private final EventManager eventManager;

    public void setupComplexEventHandling() {
        // Chain of events: Course creation -> Professor assignment -> Student notifications
        eventManager.subscribe(CourseCreatedEvent.class, this::handleCourseCreated);
        eventManager.subscribe(ProfessorAssignedEvent.class, this::handleProfessorAssigned);
        eventManager.subscribe(CourseReadyEvent.class, this::handleCourseReady);
    }

    private void handleCourseCreated(CourseCreatedEvent event) {
        System.out.println("Course created: " + event.getCourseId());

        // Automatically assign a professor based on department and availability
        CompletableFuture.runAsync(() -> {
            try {
                String professorId = findAvailableProfessor(event.getDepartmentId(), event.getCourseSubject());
                if (professorId != null) {
                    courseService.assignProfessor(event.getCourseId(), professorId);

                    // Publish professor assigned event
                    ProfessorAssignedEvent assignedEvent = new ProfessorAssignedEvent(
                        event.getCourseId(),
                        professorId,
                        LocalDateTime.now()
                    );

                    eventManager.publish(assignedEvent);
                }
            } catch (Exception e) {
                System.err.println("Failed to assign professor: " + e.getMessage());
            }
        });
    }

    private void handleProfessorAssigned(ProfessorAssignedEvent event) {
        System.out.println("Professor assigned to course: " + event.getCourseId());

        // Check if course is now ready for enrollment
        Course course = courseService.findById(event.getCourseId()).orElse(null);
        if (course != null && isCourseReadyForEnrollment(course)) {
            CourseReadyEvent readyEvent = new CourseReadyEvent(
                event.getCourseId(),
                LocalDateTime.now()
            );

            eventManager.publish(readyEvent);
        }
    }

    private void handleCourseReady(CourseReadyEvent event) {
        System.out.println("Course ready for enrollment: " + event.getCourseId());

        // Notify interested students
        CompletableFuture.runAsync(() -> {
            try {
                Course course = courseService.findById(event.getCourseId()).orElse(null);
                if (course != null) {
                    List<Student> interestedStudents = findInterestedStudents(course);

                    for (Student student : interestedStudents) {
                        String emailContent = String.format(
                            "A new course is now available for enrollment:\n\n" +
                            "Course: %s (%s)\n" +
                            "Credits: %d\n" +
                            "Professor: %s\n" +
                            "Schedule: %s\n\n" +
                            "Visit the registration portal to enroll.",
                            course.getTitle(),
                            course.getCourseId(),
                            course.getCredits(),
                            getProfessorName(course.getProfessorId()),
                            course.getSchedule()
                        );

                        notificationService.sendEmailAsync(
                            student.getEmail(),
                            "New Course Available: " + course.getTitle(),
                            emailContent
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to notify interested students: " + e.getMessage());
            }
        });
    }

    private String findAvailableProfessor(String departmentId, String subject) {
        // Implementation to find available professor
        return professorService.findAvailableProfessor(departmentId, subject)
            .map(Professor::getId)
            .orElse(null);
    }

    private boolean isCourseReadyForEnrollment(Course course) {
        return course.getProfessorId() != null &&
               course.getSchedule() != null &&
               course.getStatus() == CourseStatus.ACTIVE;
    }

    private List<Student> findInterestedStudents(Course course) {
        // Find students who might be interested based on major, prerequisites, etc.
        return studentService.findStudentsByMajor(course.getDepartment().getName())
            .stream()
            .filter(student -> hasPrerequisites(student, course))
            .limit(100) // Limit to avoid spam
            .collect(Collectors.toList());
    }

    private boolean hasPrerequisites(Student student, Course course) {
        return course.getPrerequisites().stream()
            .allMatch(prereq -> studentService.hasCompletedCourse(student.getId(), prereq));
    }

    private String getProfessorName(String professorId) {
        return professorService.findById(professorId)
            .map(Professor::getFullName)
            .orElse("TBA");
    }
}
```

## Caching Examples

### Multi-Level Caching Strategy

```java
@Service
public class CachingExamples {

    private final CacheManager cacheManager;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    // Example 1: Simple caching with Spring Cache annotations
    @Cacheable(value = "students", key = "#id")
    public Optional<Student> findStudentById(String id) {
        System.out.println("Loading student from database: " + id);
        return studentRepository.findById(id);
    }

    @Cacheable(value = "students", key = "#major")
    public List<Student> findStudentsByMajor(String major) {
        System.out.println("Loading students by major from database: " + major);
        return studentRepository.findByMajor(major);
    }

    @CacheEvict(value = "students", key = "#student.id")
    public Student updateStudent(Student student) {
        System.out.println("Updating student and evicting cache: " + student.getId());
        return studentRepository.save(student);
    }

    @CacheEvict(value = "students", allEntries = true)
    public void refreshStudentCache() {
        System.out.println("Refreshing entire student cache");
    }

    // Example 2: Custom caching logic
    public void customCachingExample() {
        String cacheKey = "popular-courses";

        // Try to get from cache first
        List<Course> popularCourses = (List<Course>) cacheManager.getCache("courses").get(cacheKey, List.class);

        if (popularCourses == null) {
            System.out.println("Cache miss - calculating popular courses");

            // Calculate popular courses (expensive operation)
            popularCourses = calculatePopularCourses();

            // Store in cache with TTL
            cacheManager.getCache("courses").put(cacheKey, popularCourses);

            System.out.println("Stored " + popularCourses.size() + " popular courses in cache");
        } else {
            System.out.println("Cache hit - retrieved " + popularCourses.size() + " popular courses");
        }

        // Use the data
        displayPopularCourses(popularCourses);
    }

    // Example 3: Cache warming strategy
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        System.out.println("Warming up caches...");

        CompletableFuture.runAsync(() -> {
            // Warm up student cache with frequently accessed data
            List<String> frequentMajors = Arrays.asList(
                "Computer Science", "Business Administration", "Engineering"
            );

            frequentMajors.forEach(major -> {
                try {
                    findStudentsByMajor(major);
                    Thread.sleep(100); // Avoid overwhelming the database
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Warm up course cache
            List<Course> allCourses = courseRepository.findAll();
            allCourses.stream()
                .limit(50) // Most popular courses
                .forEach(course -> {
                    cacheManager.getCache("courses").put(course.getCourseId(), course);
                });

            System.out.println("Cache warmup completed");
        });
    }

    // Example 4: Cache statistics and monitoring
    public void displayCacheStatistics() {
        System.out.println("=== Cache Statistics ===");

        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
                var stats = caffeineCache.stats();

                System.out.println("Cache: " + cacheName);
                System.out.println("  Size: " + caffeineCache.estimatedSize());
                System.out.println("  Hit Rate: " + String.format("%.2f%%", stats.hitRate() * 100));
                System.out.println("  Miss Rate: " + String.format("%.2f%%", stats.missRate() * 100));
                System.out.println("  Evictions: " + stats.evictionCount());
                System.out.println("  Load Time: " + stats.averageLoadPenalty() / 1_000_000 + "ms");
            }
        });
    }

    // Example 5: Distributed caching with Redis (if configured)
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public void distributedCachingExample(String studentId) {
        if (redisTemplate != null) {
            String cacheKey = "student:profile:" + studentId;

            // Try Redis cache first
            StudentProfileDTO profile = (StudentProfileDTO) redisTemplate.opsForValue().get(cacheKey);

            if (profile == null) {
                System.out.println("Redis cache miss - loading from database");

                // Load from database
                Student student = studentRepository.findById(studentId).orElse(null);
                if (student != null) {
                    profile = convertToProfileDTO(student);

                    // Store in Redis with expiration
                    redisTemplate.opsForValue().set(cacheKey, profile, Duration.ofHours(1));
                }
            } else {
                System.out.println("Redis cache hit");
            }

            if (profile != null) {
                System.out.println("Student Profile: " + profile.getFullName());
            }
        }
    }

    // Helper methods
    private List<Course> calculatePopularCourses() {
        // Expensive calculation - simulate with sleep
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return courseRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(Course::getCurrentEnrollment).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    private void displayPopularCourses(List<Course> courses) {
        System.out.println("Popular Courses:");
        courses.forEach(course ->
            System.out.println("  " + course.getCourseId() + " - " + course.getTitle() +
                " (Enrolled: " + course.getCurrentEnrollment() + ")")
        );
    }

    private StudentProfileDTO convertToProfileDTO(Student student) {
        return new StudentProfileDTO(
            student.getId(),
            student.getFullName(),
            student.getEmail(),
            student.getMajor(),
            student.getGPA()
        );
    }
}

// Custom cache configuration
@Configuration
@EnableCaching
public class CacheConfigurationExample {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        cacheManager.setCacheNames(Arrays.asList("students", "courses", "grades"));
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .removalListener((key, value, cause) -> {
                System.out.println("Cache entry removed: " + key + " (cause: " + cause + ")");
            });
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

## File I/O Operations

### Advanced File Processing

```java
@Service
public class FileIOExamples {

    private final Path dataDirectory = Paths.get("src/main/resources/data");
    private final Path outputDirectory = Paths.get("output");

    // Example 1: CSV file processing with NIO.2
    public void processCsvFilesExample() {
        try {
            Files.createDirectories(outputDirectory);

            // Process student data file
            Path studentFile = dataDirectory.resolve("students.csv");
            if (Files.exists(studentFile)) {
                processStudentCsvFile(studentFile);
            }

            // Process multiple CSV files in parallel
            List<Path> csvFiles = Files.list(dataDirectory)
                .filter(path -> path.toString().endsWith(".csv"))
                .collect(Collectors.toList());

            csvFiles.parallelStream().forEach(this::processCsvFile);

        } catch (IOException e) {
            System.err.println("File processing error: " + e.getMessage());
        }
    }

    private void processStudentCsvFile(Path file) throws IOException {
        System.out.println("Processing student CSV file: " + file.getFileName());

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Student> students = new ArrayList<>();

        // Skip header row
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] fields = line.split(",");

            if (fields.length >= 7) {
                try {
                    Student student = new StudentBuilder()
                        .setId(fields[0].trim())
                        .setFirstName(fields[1].trim())
                        .setLastName(fields[2].trim())
                        .setEmail(fields[3].trim())
                        .setMajor(fields[4].trim())
                        .setEnrollmentYear(Integer.parseInt(fields[5].trim()))
                        .setGPA(Double.parseDouble(fields[6].trim()))
                        .build();

                    students.add(student);

                } catch (NumberFormatException e) {
                    System.err.println("Invalid data in line " + (i + 1) + ": " + line);
                }
            }
        }

        System.out.println("Processed " + students.size() + " student records");

        // Write processed data to output file
        writeStudentReport(students);
    }

    private void processCsvFile(Path file) {
        try {
            System.out.println("Processing file: " + file.getFileName());

            long lineCount = Files.lines(file).count();
            System.out.println("  Lines: " + lineCount);

            // File size
            long fileSize = Files.size(file);
            System.out.println("  Size: " + formatFileSize(fileSize));

            // Last modified
            FileTime lastModified = Files.getLastModifiedTime(file);
            System.out.println("  Last modified: " + lastModified);

        } catch (IOException e) {
            System.err.println("Error processing file " + file + ": " + e.getMessage());
        }
    }

    // Example 2: JSON file operations
    public void jsonFileOperationsExample() {
        try {
            // Create sample data
            List<Student> students = createSampleStudents();

            // Write to JSON file
            Path jsonFile = outputDirectory.resolve("students.json");
            writeStudentsToJson(students, jsonFile);

            // Read from JSON file
            List<Student> loadedStudents = readStudentsFromJson(jsonFile);
            System.out.println("Loaded " + loadedStudents.size() + " students from JSON");

            // Pretty print JSON
            prettyPrintJson(jsonFile);

        } catch (IOException e) {
            System.err.println("JSON processing error: " + e.getMessage());
        }
    }

    private void writeStudentsToJson(List<Student> students, Path jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try (BufferedWriter writer = Files.newBufferedWriter(jsonFile, StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, students);
        }

        System.out.println("Written " + students.size() + " students to JSON file: " + jsonFile);
    }

    private List<Student> readStudentsFromJson(Path jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try (BufferedReader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            return objectMapper.readValue(reader,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Student.class));
        }
    }

    private void prettyPrintJson(Path jsonFile) throws IOException {
        String jsonContent = Files.readString(jsonFile);
        ObjectMapper objectMapper = new ObjectMapper();
        Object json = objectMapper.readValue(jsonContent, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

        System.out.println("JSON Content Preview:");
        System.out.println(prettyJson.substring(0, Math.min(500, prettyJson.length())));
        if (prettyJson.length() > 500) {
            System.out.println("...(truncated)");
        }
    }

    // Example 3: File monitoring and watching
    public void fileWatchingExample() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            dataDirectory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

            System.out.println("Watching directory: " + dataDirectory);

            CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        WatchKey key = watchService.take();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            Path filename = (Path) event.context();

                            System.out.println("File event: " + kind + " - " + filename);

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                handleFileCreated(dataDirectory.resolve(filename));
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                handleFileModified(dataDirectory.resolve(filename));
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                handleFileDeleted(filename);
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("File watching interrupted");
                }
            });

            // Simulate some file operations after a delay
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(2000);

                    // Create a test file
                    Path testFile = dataDirectory.resolve("test.txt");
                    Files.write(testFile, "Hello World".getBytes());

                    Thread.sleep(1000);

                    // Modify the file
                    Files.write(testFile, "Hello World - Modified".getBytes());

                    Thread.sleep(1000);

                    // Delete the file
                    Files.delete(testFile);

                } catch (Exception e) {
                    System.err.println("Error in file simulation: " + e.getMessage());
                }
            });

        } catch (IOException e) {
            System.err.println("File watching setup error: " + e.getMessage());
        }
    }

    private void handleFileCreated(Path file) {
        System.out.println("New file created: " + file);
        if (file.toString().endsWith(".csv")) {
            CompletableFuture.runAsync(() -> processCsvFile(file));
        }
    }

    private void handleFileModified(Path file) {
        System.out.println("File modified: " + file);
        // Could trigger reprocessing
    }

    private void handleFileDeleted(Path filename) {
        System.out.println("File deleted: " + filename);
        // Could trigger cleanup operations
    }

    // Example 4: Batch file operations
    public void batchFileOperationsExample() {
        try {
            // Create backup directory
            Path backupDir = outputDirectory.resolve("backup");
            Files.createDirectories(backupDir);

            // Copy all CSV files to backup
            List<Path> csvFiles = Files.list(dataDirectory)
                .filter(path -> path.toString().endsWith(".csv"))
                .collect(Collectors.toList());

            System.out.println("Backing up " + csvFiles.size() + " CSV files...");

            for (Path csvFile : csvFiles) {
                Path backupFile = backupDir.resolve(csvFile.getFileName());
                Files.copy(csvFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("  Backed up: " + csvFile.getFileName());
            }

            // Generate file manifest
            generateFileManifest(csvFiles, backupDir.resolve("manifest.txt"));

            // Clean up old backup files (older than 30 days)
            cleanupOldFiles(backupDir, 30);

        } catch (IOException e) {
            System.err.println("Batch operation error: " + e.getMessage());
        }
    }

    private void generateFileManifest(List<Path> files, Path manifestFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(manifestFile))) {
            writer.println("# File Manifest - Generated: " + LocalDateTime.now());
            writer.println("# Format: filename,size,last_modified,checksum");
            writer.println();

            for (Path file : files) {
                try {
                    long size = Files.size(file);
                    FileTime lastModified = Files.getLastModifiedTime(file);
                    String checksum = calculateFileChecksum(file);

                    writer.printf("%s,%d,%s,%s%n",
                        file.getFileName(),
                        size,
                        lastModified,
                        checksum);

                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error processing file " + file + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Generated manifest: " + manifestFile);
    }

    private void cleanupOldFiles(Path directory, int daysOld) throws IOException {
        Instant cutoff = Instant.now().minus(daysOld, ChronoUnit.DAYS);

        List<Path> oldFiles = Files.list(directory)
            .filter(path -> {
                try {
                    return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                } catch (IOException e) {
                    return false;
                }
            })
            .collect(Collectors.toList());

        System.out.println("Cleaning up " + oldFiles.size() + " old files...");

        for (Path oldFile : oldFiles) {
            try {
                Files.delete(oldFile);
                System.out.println("  Deleted: " + oldFile.getFileName());
            } catch (IOException e) {
                System.err.println("  Failed to delete: " + oldFile + " - " + e.getMessage());
            }
        }
    }

    private String calculateFileChecksum(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();

        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    // Helper methods
    private void writeStudentReport(List<Student> students) throws IOException {
        Path reportFile = outputDirectory.resolve("student-report.txt");

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(reportFile))) {
            writer.println("STUDENT REPORT - Generated: " + LocalDateTime.now());
            writer.println("=" .repeat(50));
            writer.println();

            // Summary statistics
            long totalStudents = students.size();
            double averageGPA = students.stream().mapToDouble(Student::getGPA).average().orElse(0.0);

            Map<String, Long> studentsByMajor = students.stream()
                .collect(Collectors.groupingBy(Student::getMajor, Collectors.counting()));

            writer.println("SUMMARY:");
            writer.println("  Total Students: " + totalStudents);
            writer.println("  Average GPA: " + String.format("%.2f", averageGPA));
            writer.println("  Majors: " + studentsByMajor.size());
            writer.println();

            writer.println("STUDENTS BY MAJOR:");
            studentsByMajor.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry ->
                    writer.println("  " + entry.getKey() + ": " + entry.getValue())
                );

            writer.println();
            writer.println("TOP 10 STUDENTS BY GPA:");
            students.stream()
                .sorted(Comparator.comparing(Student::getGPA).reversed())
                .limit(10)
                .forEach(student ->
                    writer.printf("  %s - GPA: %.2f (%s)%n",
                        student.getFullName(),
                        student.getGPA(),
                        student.getMajor())
                );
        }

        System.out.println("Student report written to: " + reportFile);
    }

    private List<Student> createSampleStudents() {
        return Arrays.asList(
            new StudentBuilder().setId("S001").setFirstName("John").setLastName("Doe")
                .setEmail("john.doe@test.edu").setMajor("Computer Science").setGPA(3.8).build(),
            new StudentBuilder().setId("S002").setFirstName("Jane").setLastName("Smith")
                .setEmail("jane.smith@test.edu").setMajor("Mathematics").setGPA(3.9).build(),
            new StudentBuilder().setId("S003").setFirstName("Bob").setLastName("Johnson")
                .setEmail("bob.johnson@test.edu").setMajor("Physics").setGPA(3.6).build()
        );
    }

    private String formatFileSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        double size = bytes;

        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }

        return String.format("%.1f %s", size, units[unit]);
    }
}
```

This comprehensive usage examples document demonstrates practical implementations of the SmartCampus system's various components and features. Each section provides real-world scenarios with complete, working code examples that showcase best practices and modern Java features.
