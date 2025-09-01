// File: src/main/java/services/CourseService.java
package services;

import models.Course;
import models.Course.CourseStatus;
import models.Course.DifficultyLevel;
import models.Enrollment;
import models.Student;
import models.Professor;
import interfaces.Searchable;
import interfaces.Reportable;
import interfaces.Enrollable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CourseService class providing course-related business operations.
 * This service demonstrates extensive use of Lambda expressions and functional programming.
 * 
 * Key Java concepts demonstrated:
 * - Extensive Lambda expressions usage
 * - Method references and functional interfaces
 * - Stream API with complex operations
 * - Custom collectors and grouping
 * - Function composition and chaining
 * - Optional handling with functional style
 * - Parallel stream processing
 * - Interface implementation with functional approach
 */
public class CourseService implements Searchable<Course>, Reportable, Enrollable {
    
    // Instance fields
    private final Map<String, Course> courses;
    private final Map<String, List<Enrollment>> courseEnrollments; // courseId -> enrollments
    private final Map<String, String> courseInstructors; // courseId -> professorId
    private final Map<String, List<String>> coursePrerequisites; // courseId -> prerequisite courseIds
    private final Map<String, Integer> enrollmentCounts;
    private final Map<String, Integer> waitlistCounts;
    
    // Lambda expressions for common operations
    private final Function<Course, String> courseToString = course -> 
        String.format("%s: %s (%d credits)", course.getCourseCode(), course.getCourseName(), course.getCreditHours());
    
    private final Predicate<Course> isActiveCourse = course -> 
        course.getStatus() == CourseStatus.ACTIVE;
    
    private final Predicate<Course> hasAvailableSeats = course -> 
        enrollmentCounts.getOrDefault(course.getCourseId(), 0) < course.getMaxEnrollment();
    
    private final Function<Course, Double> calculateEnrollmentRate = course -> {
        int enrolled = enrollmentCounts.getOrDefault(course.getCourseId(), 0);
        return course.getMaxEnrollment() > 0 ? (enrolled * 100.0) / course.getMaxEnrollment() : 0.0;
    };
    
    // Comparators using lambda expressions
    private final Comparator<Course> byCourseName = (c1, c2) -> c1.getCourseName().compareTo(c2.getCourseName());
    private final Comparator<Course> byCourseCode = Comparator.comparing(Course::getCourseCode);
    private final Comparator<Course> byCreditHours = Comparator.comparing(Course::getCreditHours);
    private final Comparator<Course> byEnrollmentRate = Comparator.comparing(calculateEnrollmentRate);
    
    // Statistics cache
    private volatile Map<String, Object> cachedStatistics;
    private volatile LocalDateTime lastStatisticsUpdate;
    
    /**
     * Constructor initializing the service.
     */
    public CourseService() {
        this.courses = new ConcurrentHashMap<>();
        this.courseEnrollments = new ConcurrentHashMap<>();
        this.courseInstructors = new ConcurrentHashMap<>();
        this.coursePrerequisites = new ConcurrentHashMap<>();
        this.enrollmentCounts = new ConcurrentHashMap<>();
        this.waitlistCounts = new ConcurrentHashMap<>();
        this.cachedStatistics = new HashMap<>();
        this.lastStatisticsUpdate = LocalDateTime.now();
    }
    
    // Core CRUD operations with lambda expressions
    
    /**
     * Add a new course using functional validation.
     * 
     * @param course The course to add
     * @return true if course was added successfully, false otherwise
     */
    public boolean addCourse(Course course) {
        return Optional.ofNullable(course)
                .filter(c -> ValidationUtil.isValidString(c.getCourseId()))
                .filter(c -> !courses.containsKey(c.getCourseId()))
                .map(c -> {
                    courses.put(c.getCourseId(), c);
                    courseEnrollments.put(c.getCourseId(), new ArrayList<>());
                    coursePrerequisites.put(c.getCourseId(), new ArrayList<>());
                    enrollmentCounts.put(c.getCourseId(), 0);
                    waitlistCounts.put(c.getCourseId(), 0);
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Update an existing course using functional approach.
     * 
     * @param course The updated course data
     * @return true if course was updated successfully, false otherwise
     */
    public boolean updateCourse(Course course) {
        return Optional.ofNullable(course)
                .filter(c -> courses.containsKey(c.getCourseId()))
                .map(c -> {
                    courses.put(c.getCourseId(), c);
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Remove a course using functional validation.
     * 
     * @param courseId The ID of the course to remove
     * @return true if course was removed successfully, false otherwise
     */
    public boolean removeCourse(String courseId) {
        return Optional.ofNullable(courseId)
                .filter(ValidationUtil::isValidString)
                .map(courses::remove)
                .map(removedCourse -> {
                    courseEnrollments.remove(courseId);
                    courseInstructors.remove(courseId);
                    coursePrerequisites.remove(courseId);
                    enrollmentCounts.remove(courseId);
                    waitlistCounts.remove(courseId);
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Get course by ID using functional approach.
     * 
     * @param courseId The course ID
     * @return Optional containing the course if found
     */
    public Optional<Course> getCourseById(String courseId) {
        return Optional.ofNullable(courseId)
                .filter(ValidationUtil::isValidString)
                .map(courses::get);
    }
    
    /**
     * Get all courses using stream.
     * 
     * @return List of all courses
     */
    public List<Course> getAllCourses() {
        return courses.values().stream()
                .collect(Collectors.toList());
    }
    
    // Advanced query operations using lambda expressions and streams
    
    /**
     * Get courses by department using lambda and stream.
     * 
     * @param departmentId The department ID to filter by
     * @return List of courses in the specified department
     */
    public List<Course> getCoursesByDepartment(String departmentId) {
        return courses.values().stream()
                .filter(course -> departmentId.equals(course.getDepartmentId()))
                .sorted(byCourseCode)
                .collect(Collectors.toList());
    }
    
    /**
     * Get courses by status using method reference.
     * 
     * @param status The course status to filter by
     * @return List of courses with the specified status
     */
    public List<Course> getCoursesByStatus(CourseStatus status) {
        return courses.values().stream()
                .filter(course -> status == course.getStatus())
                .collect(Collectors.toList());
    }
    
    /**
     * Get active courses using predicate reference.
     * 
     * @return List of active courses
     */
    public List<Course> getActiveCourses() {
        return courses.values().stream()
                .filter(isActiveCourse)
                .sorted(byCourseCode)
                .collect(Collectors.toList());
    }
    
    /**
     * Get courses with available seats using lambda composition.
     * 
     * @return List of courses with available enrollment spots
     */
    public List<Course> getCoursesWithAvailableSeats() {
        return courses.values().stream()
                .filter(isActiveCourse.and(hasAvailableSeats))
                .sorted(byEnrollmentRate)
                .collect(Collectors.toList());
    }
    
    /**
     * Get courses by credit hours range using lambda expressions.
     * 
     * @param minCredits Minimum credit hours
     * @param maxCredits Maximum credit hours
     * @return List of courses within the credit range
     */
    public List<Course> getCoursesByCreditRange(int minCredits, int maxCredits) {
        return courses.values().stream()
                .filter(course -> course.getCreditHours() >= minCredits && course.getCreditHours() <= maxCredits)
                .sorted(byCreditHours.thenComparing(byCourseCode))
                .collect(Collectors.toList());
    }
    
    /**
     * Get courses by difficulty level using functional filtering.
     * 
     * @param difficultyLevel The difficulty level to filter by
     * @return List of courses with the specified difficulty
     */
    public List<Course> getCoursesByDifficulty(DifficultyLevel difficultyLevel) {
        return courses.values().stream()
                .filter(course -> difficultyLevel == course.getDifficultyLevel())
                .collect(Collectors.toList());
    }
    
    /**
     * Get courses by instructor using lambda expression.
     * 
     * @param professorId The professor ID
     * @return List of courses taught by the professor
     */
    public List<Course> getCoursesByInstructor(String professorId) {
        return courseInstructors.entrySet().stream()
                .filter(entry -> professorId.equals(entry.getValue()))
                .map(entry -> courses.get(entry.getKey()))
                .filter(Objects::nonNull)
                .sorted(byCourseCode)
                .collect(Collectors.toList());
    }
    
    /**
     * Get popular courses (high enrollment rate) using functional approach.
     * 
     * @param threshold Minimum enrollment rate threshold
     * @return List of popular courses
     */
    public List<Course> getPopularCourses(double threshold) {
        return courses.values().stream()
                .filter(course -> calculateEnrollmentRate.apply(course) >= threshold)
                .sorted(byEnrollmentRate.reversed())
                .collect(Collectors.toList());
    }
    
    // Statistical operations using functional programming
    
    /**
     * Get enrollment statistics by department using collectors.
     * 
     * @return Map of department to total enrollments
     */
    public Map<String, Integer> getEnrollmentStatisticsByDepartment() {
        return courses.values().stream()
                .collect(Collectors.groupingBy(
                    Course::getDepartmentId,
                    Collectors.summingInt(course -> enrollmentCounts.getOrDefault(course.getCourseId(), 0))
                ));
    }
    
    /**
     * Get course distribution by status using functional approach.
     * 
     * @return Map of course status to count
     */
    public Map<CourseStatus, Long> getCourseDistributionByStatus() {
        return courses.values().stream()
                .collect(Collectors.groupingBy(
                    Course::getStatus,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get average enrollment rate by department using advanced collectors.
     * 
     * @return Map of department to average enrollment rate
     */
    public Map<String, Double> getAverageEnrollmentRateByDepartment() {
        return courses.values().stream()
                .collect(Collectors.groupingBy(
                    Course::getDepartmentId,
                    Collectors.averagingDouble(calculateEnrollmentRate::apply)
                ));
    }
    
    /**
     * Get credit hour distribution using functional grouping.
     * 
     * @return Map of credit hours to course count
     */
    public Map<Integer, Long> getCreditHourDistribution() {
        return courses.values().stream()
                .collect(Collectors.groupingBy(
                    Course::getCreditHours,
                    Collectors.counting()
                ));
    }
    
    /**
     * Calculate comprehensive course statistics using lambda expressions.
     * 
     * @return Map containing various statistical measures
     */
    public Map<String, Object> calculateOverallStatistics() {
        if (isStatisticsCacheValid()) {
            return new HashMap<>(cachedStatistics);
        }
        
        List<Course> activeCourses = getActiveCourses();
        
        // Calculate statistics using functional approach
        DoubleSummaryStatistics enrollmentStats = activeCourses.stream()
                .mapToDouble(calculateEnrollmentRate::apply)
                .summaryStatistics();
        
        IntSummaryStatistics creditStats = activeCourses.stream()
                .mapToInt(Course::getCreditHours)
                .summaryStatistics();
        
        // Build statistics map using lambda expressions
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCourses", courses.size());
        statistics.put("activeCourses", activeCourses.size());
        statistics.put("totalEnrollments", getTotalEnrollments());
        statistics.put("averageEnrollmentRate", enrollmentStats.getAverage());
        statistics.put("minEnrollmentRate", enrollmentStats.getMin());
        statistics.put("maxEnrollmentRate", enrollmentStats.getMax());
        statistics.put("averageCreditHours", creditStats.getAverage());
        statistics.put("totalCreditHours", creditStats.getSum());
        statistics.put("coursesByDepartment", getCourseCountByDepartment());
        statistics.put("coursesByStatus", getCourseDistributionByStatus());
        statistics.put("enrollmentsByDepartment", getEnrollmentStatisticsByDepartment());
        statistics.put("popularCourses", getTopCoursesByEnrollment(5));
        statistics.put("availableSeats", getTotalAvailableSeats());
        statistics.put("waitlistTotal", getTotalWaitlistCount());
        
        // Cache the results
        cachedStatistics = new HashMap<>(statistics);
        lastStatisticsUpdate = LocalDateTime.now();
        
        return statistics;
    }
    
    // Course management with functional programming
    
    /**
     * Assign instructor to course using functional validation.
     * 
     * @param courseId The course ID
     * @param professorId The professor ID to assign
     * @return true if instructor was assigned successfully
     */
    public boolean assignInstructor(String courseId, String professorId) {
        return getCourseById(courseId)
                .filter(isActiveCourse)
                .map(course -> {
                    courseInstructors.put(courseId, professorId);
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Add prerequisite to course using functional approach.
     * 
     * @param courseId The course ID
     * @param prerequisiteCourseId The prerequisite course ID
     * @return true if prerequisite was added successfully
     */
    public boolean addPrerequisite(String courseId, String prerequisiteCourseId) {
        return getCourseById(courseId)
                .filter(course -> getCourseById(prerequisiteCourseId).isPresent())
                .map(course -> {
                    coursePrerequisites.computeIfAbsent(courseId, k -> new ArrayList<>())
                                     .add(prerequisiteCourseId);
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Get course prerequisites using functional retrieval.
     * 
     * @param courseId The course ID
     * @return List of prerequisite course IDs
     */
    public List<String> getCoursePrerequisites(String courseId) {
        return Optional.ofNullable(coursePrerequisites.get(courseId))
                .orElse(new ArrayList<>());
    }
    
    /**
     * Check if student meets prerequisites using functional validation.
     * 
     * @param courseId The course ID
     * @param completedCourses List of courses completed by student
     * @return true if prerequisites are met
     */
    public boolean checkPrerequisites(String courseId, List<String> completedCourses) {
        return getCoursePrerequisites(courseId).stream()
                .allMatch(completedCourses::contains);
    }
    
    // Advanced filtering using functional composition
    
    /**
     * Filter courses using custom predicate composition.
     * 
     * @param predicate The predicate to filter by
     * @return List of courses matching the predicate
     */
    public List<Course> filterCourses(Predicate<Course> predicate) {
        return courses.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Find courses using complex criteria with lambda expressions.
     * 
     * @param criteria Map of field names to expected values
     * @return List of courses matching all criteria
     */
    public List<Course> findCoursesByCriteria(Map<String, Object> criteria) {
        Predicate<Course> combinedPredicate = criteria.entrySet().stream()
                .map(this::createFieldPredicate)
                .reduce(Predicate::and)
                .orElse(course -> true);
        
        return courses.values().stream()
                .filter(combinedPredicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Get courses for specific time slot using lambda filtering.
     * 
     * @param startTime Start time
     * @param endTime End time
     * @return List of courses in the time slot
     */
    public List<Course> getCoursesInTimeSlot(LocalTime startTime, LocalTime endTime) {
        return courses.values().stream()
                .filter(course -> course.getStartTime() != null && course.getEndTime() != null)
                .filter(course -> !course.getStartTime().isAfter(endTime) && !course.getEndTime().isBefore(startTime))
                .collect(Collectors.toList());
    }
    
    /**
     * Get recommended courses for student using functional approach.
     * 
     * @param studentLevel Student's academic level
     * @param completedCourses Courses already completed
     * @param interestedDepartments Departments of interest
     * @return List of recommended courses
     */
    public List<Course> getRecommendedCourses(String studentLevel, List<String> completedCourses, 
                                            List<String> interestedDepartments) {
        Predicate<Course> isRecommended = course -> 
            isActiveCourse.test(course) &&
            hasAvailableSeats.test(course) &&
            interestedDepartments.contains(course.getDepartmentId()) &&
            checkPrerequisites(course.getCourseId(), completedCourses) &&
            !completedCourses.contains(course.getCourseId());
        
        return courses.values().stream()
                .filter(isRecommended)
                .sorted(byEnrollmentRate.reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
    
    // Enrollable interface implementation using functional programming
    
    @Override
    public boolean enrollStudent(String studentId, String courseId, String semester, int year) {
        return getCourseById(courseId)
                .filter(isActiveCourse.and(hasAvailableSeats))
                .map(course -> {
                    Enrollment enrollment = Enrollment.createEnrollment(studentId, courseId, semester, year);
                    courseEnrollments.computeIfAbsent(courseId, k -> new ArrayList<>()).add(enrollment);
                    enrollmentCounts.merge(courseId, 1, Integer::sum);
                    return true;
                })
                .orElse(false);
    }
    
    @Override
    public boolean dropStudent(String studentId, String courseId, String reason) {
        return Optional.ofNullable(courseEnrollments.get(courseId))
                .map(enrollments -> enrollments.stream()
                    .filter(enrollment -> studentId.equals(enrollment.getStudentId()))
                    .findFirst()
                    .map(enrollment -> {
                        enrollment.dropEnrollment(reason);
                        enrollmentCounts.merge(courseId, -1, Integer::sum);
                        return true;
                    })
                    .orElse(false))
                .orElse(false);
    }
    
    @Override
    public boolean addToWaitlist(String studentId, String courseId, String semester, int year) {
        return getCourseById(courseId)
                .filter(isActiveCourse)
                .map(course -> {
                    Enrollment waitlistEnrollment = Enrollment.createWaitlistedEnrollment(studentId, courseId, semester, year);
                    courseEnrollments.computeIfAbsent(courseId, k -> new ArrayList<>()).add(waitlistEnrollment);
                    waitlistCounts.merge(courseId, 1, Integer::sum);
                    return true;
                })
                .orElse(false);
    }
    
    @Override
    public boolean removeFromWaitlist(String studentId, String courseId) {
        return Optional.ofNullable(courseEnrollments.get(courseId))
                .map(enrollments -> enrollments.stream()
                    .filter(enrollment -> studentId.equals(enrollment.getStudentId()) && 
                                        enrollment.getStatus() == Enrollment.EnrollmentStatus.WAITLISTED)
                    .findFirst()
                    .map(enrollment -> {
                        enrollments.remove(enrollment);
                        waitlistCounts.merge(courseId, -1, Integer::sum);
                        return true;
                    })
                    .orElse(false))
                .orElse(false);
    }
    
    @Override
    public int processWaitlist(String courseId, int numberOfStudents) {
        return Optional.ofNullable(courseEnrollments.get(courseId))
                .map(enrollments -> enrollments.stream()
                    .filter(enrollment -> enrollment.getStatus() == Enrollment.EnrollmentStatus.WAITLISTED)
                    .limit(numberOfStudents)
                    .mapToInt(enrollment -> {
                        if (hasAvailableSeats.test(courses.get(courseId))) {
                            enrollment.enrollFromWaitlist();
                            enrollmentCounts.merge(courseId, 1, Integer::sum);
                            waitlistCounts.merge(courseId, -1, Integer::sum);
                            return 1;
                        }
                        return 0;
                    })
                    .sum())
                .orElse(0);
    }
    
    @Override
    public boolean isStudentEnrolled(String studentId, String courseId) {
        return Optional.ofNullable(courseEnrollments.get(courseId))
                .map(enrollments -> enrollments.stream()
                    .anyMatch(enrollment -> studentId.equals(enrollment.getStudentId()) && 
                                          enrollment.getStatus() == Enrollment.EnrollmentStatus.ENROLLED))
                .orElse(false);
    }
    
    @Override
    public boolean isStudentWaitlisted(String studentId, String courseId) {
        return Optional.ofNullable(courseEnrollments.get(courseId))
                .map(enrollments -> enrollments.stream()
                    .anyMatch(enrollment -> studentId.equals(enrollment.getStudentId()) && 
                                          enrollment.getStatus() == Enrollment.EnrollmentStatus.WAITLISTED))
                .orElse(false);
    }
    
    @Override
    public List<Enrollment> getStudentEnrollments(String studentId) {
        return courseEnrollments.values().stream()
                .flatMap(List::stream)
                .filter(enrollment -> studentId.equals(enrollment.getStudentId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Enrollment> getCourseEnrollments(String courseId) {
        return Optional.ofNullable(courseEnrollments.get(courseId))
                .orElse(new ArrayList<>());
    }
    
    @Override
    public int getCurrentEnrollmentCount(String courseId) {
        return enrollmentCounts.getOrDefault(courseId, 0);
    }
    
    @Override
    public int getCurrentWaitlistCount(String courseId) {
        return waitlistCounts.getOrDefault(courseId, 0);
    }
    
    @Override
    public boolean hasAvailableSpots(String courseId) {
        return getCourseById(courseId)
                .map(hasAvailableSeats::test)
                .orElse(false);
    }
    
    @Override
    public int getMaxEnrollmentCapacity(String courseId) {
        return getCourseById(courseId)
                .map(Course::getMaxEnrollment)
                .orElse(0);
    }
    
    @Override
    public boolean transferStudent(String studentId, String fromCourseId, String toCourseId, String semester, int year) {
        return dropStudent(studentId, fromCourseId, "Transfer to " + toCourseId) &&
               enrollStudent(studentId, toCourseId, semester, year);
    }
    
    @Override
    public int bulkEnrollStudents(List<String> studentIds, String courseId, String semester, int year) {
        return studentIds.stream()
                .mapToInt(studentId -> enrollStudent(studentId, courseId, semester, year) ? 1 : 0)
                .sum();
    }
    
    @Override
    public EnrollmentStatistics getEnrollmentStatistics(String courseId) {
        return getCourseById(courseId)
                .map(course -> {
                    List<Enrollment> enrollments = getCourseEnrollments(courseId);
                    
                    int enrolled = (int) enrollments.stream()
                            .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ENROLLED)
                            .count();
                    
                    int waitlisted = (int) enrollments.stream()
                            .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.WAITLISTED)
                            .count();
                    
                    int dropped = (int) enrollments.stream()
                            .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.DROPPED)
                            .count();
                    
                    int completed = (int) enrollments.stream()
                            .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.COMPLETED)
                            .count();
                    
                    return new EnrollmentStatistics(enrolled, waitlisted, dropped, completed, course.getMaxEnrollment());
                })
                .orElse(new EnrollmentStatistics(0, 0, 0, 0, 0));
    }
    
    // Searchable interface implementation using functional approach
    
    @Override
    public List<Course> search(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        Predicate<Course> matchesKeyword = course -> 
            course.getCourseName().toLowerCase().contains(lowerKeyword) ||
            course.getCourseCode().toLowerCase().contains(lowerKeyword) ||
            course.getDescription().toLowerCase().contains(lowerKeyword) ||
            course.getDepartmentId().toLowerCase().contains(lowerKeyword);
        
        return courses.values().stream()
                .filter(matchesKeyword)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Course> search(Map<String, SearchCriterion> searchCriteria) {
        Predicate<Course> combinedPredicate = searchCriteria.entrySet().stream()
                .map(entry -> createSearchPredicate(entry.getKey(), entry.getValue()))
                .reduce(Predicate::and)
                .orElse(course -> true);
        
        return courses.values().stream()
                .filter(combinedPredicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Course> search(Predicate<Course> predicate) {
        return courses.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Course> searchAndSort(String keyword, String sortBy, SortOrder sortOrder) {
        Comparator<Course> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return search(keyword).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    @Override
    public SearchResult<Course> searchWithPagination(String keyword, int page, int pageSize) {
        List<Course> allResults = search(keyword);
        int totalElements = allResults.size();
        
        List<Course> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements);
    }
    
    @Override
    public SearchResult<Course> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria,
                                                            String sortBy, SortOrder sortOrder,
                                                            int page, int pageSize) {
        Comparator<Course> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        List<Course> allResults = search(searchCriteria).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        int totalElements = allResults.size();
        
        List<Course> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements, sortBy, sortOrder);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialInput, int maxSuggestions) {
        return courses.values().stream()
                .flatMap(course -> Arrays.stream(new String[]{
                    course.getCourseName(),
                    course.getCourseCode(),
                    course.getDepartmentId(),
                    course.getCourseId()
                }))
                .filter(Objects::nonNull)
                .filter(field -> field.toLowerCase().contains(partialInput.toLowerCase()))
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Course> filter(Predicate<Course> predicate) {
        return filterCourses(predicate);
    }
    
    @Override
    public List<String> getSearchableFields() {
        return Arrays.asList("courseName", "courseCode", "description", "departmentId", 
                           "status", "difficultyLevel", "creditHours", "maxEnrollment");
    }
    
    @Override
    public List<String> getSortableFields() {
        return Arrays.asList("courseName", "courseCode", "departmentId", "creditHours", 
                           "maxEnrollment", "enrollmentRate", "status");
    }
    
    @Override
    public long countSearchResults(String keyword) {
        return search(keyword).size();
    }
    
    @Override
    public long countSearchResults(Map<String, SearchCriterion> searchCriteria) {
        return search(searchCriteria).size();
    }
    
    // Reportable interface implementation
    
    @Override
    public ReportData generateReport(ReportType reportType) {
        return generateReport(reportType, Map.of());
    }
    
    @Override
    public ReportData generateReport(ReportType reportType, Map<String, Object> parameters) {
        String reportId = "RPT_COURSE_" + System.currentTimeMillis();
        
        switch (reportType) {
            case ENROLLMENT_REPORT:
                return generateEnrollmentReport(reportId, parameters);
            case COURSE_EVALUATION_REPORT:
                return generateCourseEvaluationReport(reportId, parameters);
            case STATISTICAL_SUMMARY:
                return generateStatisticalReport(reportId, parameters);
            default:
                return new ReportData(reportId, reportType, "Unsupported Report Type", "Report type not supported");
        }
    }
    
    @Override
    public ReportData generateReportForDateRange(ReportType reportType, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> parameters = Map.of(
            "startDate", startDate,
            "endDate", endDate
        );
        return generateReport(reportType, parameters);
    }
    
    @Override
    public boolean exportReport(ReportData reportData, ReportFormat format, String filePath) {
        return true; // Simplified for demo
    }
    
    @Override
    public List<ReportType> getAvailableReportTypes() {
        return Arrays.asList(
            ReportType.ENROLLMENT_REPORT,
            ReportType.COURSE_EVALUATION_REPORT,
            ReportType.STATISTICAL_SUMMARY
        );
    }
    
    @Override
    public List<ReportFormat> getSupportedFormats() {
        return Arrays.asList(ReportFormat.PDF, ReportFormat.CSV, ReportFormat.EXCEL, ReportFormat.JSON);
    }
    
    @Override
    public String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients) {
        return "SCHED_COURSE_" + System.currentTimeMillis(); // Simplified for demo
    }
    
    @Override
    public boolean cancelScheduledReport(String scheduledReportId) {
        return true; // Simplified for demo
    }
    
    @Override
    public List<ReportMetadata> getReportHistory(ReportType reportType, int limit) {
        return new ArrayList<>(); // Simplified for demo
    }
    
    @Override
    public Map<String, Object> getSummaryStatistics() {
        return calculateOverallStatistics();
    }
    
    // Helper methods using functional programming
    
    /**
     * Create field predicate using lambda expressions.
     */
    private Predicate<Course> createFieldPredicate(Map.Entry<String, Object> criterion) {
        String field = criterion.getKey();
        Object value = criterion.getValue();
        
        return course -> Objects.equals(getFieldValue(course, field), value);
    }
    
    /**
     * Create search predicate using functional approach.
     */
    private Predicate<Course> createSearchPredicate(String field, SearchCriterion criterion) {
        return course -> {
            Object fieldValue = getFieldValue(course, field);
            if (fieldValue == null) return false;
            
            String fieldStr = fieldValue.toString().toLowerCase();
            String searchValue = criterion.getValue().toString().toLowerCase();
            
            switch (criterion.getCriteria()) {
                case EXACT_MATCH:
                    return fieldStr.equals(searchValue);
                case CONTAINS:
                    return fieldStr.contains(searchValue);
                case STARTS_WITH:
                    return fieldStr.startsWith(searchValue);
                case ENDS_WITH:
                    return fieldStr.endsWith(searchValue);
                default:
                    return false;
            }
        };
    }
    
    /**
     * Get field value from course using functional approach.
     */
    private Object getFieldValue(Course course, String field) {
        Map<String, Function<Course, Object>> fieldExtractors = Map.of(
            "coursename", Course::getCourseName,
            "coursecode", Course::getCourseCode,
            "description", Course::getDescription,
            "departmentid", Course::getDepartmentId,
            "status", Course::getStatus,
            "difficultylevel", Course::getDifficultyLevel,
            "credithours", Course::getCreditHours,
            "maxenrollment", Course::getMaxEnrollment,
            "enrollmentrate", calculateEnrollmentRate
        );
        
        return fieldExtractors.getOrDefault(field.toLowerCase(), c -> null).apply(course);
    }
    
    /**
     * Get comparator for sorting using method references.
     */
    private Comparator<Course> getComparator(String sortBy) {
        Map<String, Comparator<Course>> comparators = Map.of(
            "coursename", byCourseName,
            "coursecode", byCourseCode,
            "credithours", byCreditHours,
            "enrollmentrate", byEnrollmentRate,
            "departmentid", Comparator.comparing(Course::getDepartmentId),
            "maxenrollment", Comparator.comparing(Course::getMaxEnrollment),
            "status", Comparator.comparing(Course::getStatus)
        );
        
        return comparators.getOrDefault(sortBy.toLowerCase(), byCourseCode);
    }
    
    /**
     * Get course count by department using functional approach.
     */
    private Map<String, Long> getCourseCountByDepartment() {
        return courses.values().stream()
                .collect(Collectors.groupingBy(
                    Course::getDepartmentId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get total enrollments using functional sum.
     */
    private int getTotalEnrollments() {
        return enrollmentCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
    
    /**
     * Get top courses by enrollment using functional approach.
     */
    private List<String> getTopCoursesByEnrollment(int limit) {
        return courses.values().stream()
                .sorted(byEnrollmentRate.reversed())
                .limit(limit)
                .map(courseToString)
                .collect(Collectors.toList());
    }
    
    /**
     * Get total available seats using functional calculation.
     */
    private int getTotalAvailableSeats() {
        return courses.values().stream()
                .filter(isActiveCourse)
                .mapToInt(course -> Math.max(0, course.getMaxEnrollment() - enrollmentCounts.getOrDefault(course.getCourseId(), 0)))
                .sum();
    }
    
    /**
     * Get total waitlist count using functional sum.
     */
    private int getTotalWaitlistCount() {
        return waitlistCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
    
    /**
     * Generate enrollment report using functional approach.
     */
    private ReportData generateEnrollmentReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Course Code", "Course Name", "Department", "Enrolled", "Capacity", "Waitlist", "Rate %");
        
        List<Map<String, Object>> rows = courses.values().stream()
                .map(course -> {
                    Map<String, Object> row = new HashMap<>();
                    int enrolled = enrollmentCounts.getOrDefault(course.getCourseId(), 0);
                    int waitlist = waitlistCounts.getOrDefault(course.getCourseId(), 0);
                    double rate = calculateEnrollmentRate.apply(course);
                    
                    row.put("Course Code", course.getCourseCode());
                    row.put("Course Name", course.getCourseName());
                    row.put("Department", course.getDepartmentId());
                    row.put("Enrolled", enrolled);
                    row.put("Capacity", course.getMaxEnrollment());
                    row.put("Waitlist", waitlist);
                    row.put("Rate %", String.format("%.1f%%", rate));
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.ENROLLMENT_REPORT, "Course Enrollment Report", 
                            columns, rows, Map.of("totalCourses", courses.size()));
    }
    
    /**
     * Generate course evaluation report using functional approach.
     */
    private ReportData generateCourseEvaluationReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Course Code", "Course Name", "Status", "Difficulty", "Credits", "Enrollment Rate");
        
        List<Map<String, Object>> rows = courses.values().stream()
                .map(course -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Course Code", course.getCourseCode());
                    row.put("Course Name", course.getCourseName());
                    row.put("Status", course.getStatus().toString());
                    row.put("Difficulty", course.getDifficultyLevel().toString());
                    row.put("Credits", course.getCreditHours());
                    row.put("Enrollment Rate", String.format("%.1f%%", calculateEnrollmentRate.apply(course)));
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.COURSE_EVALUATION_REPORT, "Course Evaluation Report", 
                            columns, rows, calculateOverallStatistics());
    }
    
    /**
     * Generate statistical report using functional approach.
     */
    private ReportData generateStatisticalReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        String content = statistics.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, "Course Statistical Summary", content);
    }
    
    /**
     * Check if statistics cache is valid.
     */
    private boolean isStatisticsCacheValid() {
        return lastStatisticsUpdate != null && 
               LocalDateTime.now().minusMinutes(5).isBefore(lastStatisticsUpdate);
    }
    
    /**
     * Invalidate statistics cache.
     */
    private void invalidateStatisticsCache() {
        lastStatisticsUpdate = null;
    }
}