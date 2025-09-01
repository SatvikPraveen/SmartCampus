// File location: src/main/java/functional/Predicates.java
package functional;

import models.*;
import enums.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Collection of commonly used predicates for campus entities
 * Provides reusable predicate functions for filtering and validation
 */
public final class Predicates {
    
    private Predicates() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== USER PREDICATES ====================
    
    public static final Predicate<User> IS_ACTIVE = user -> 
        user != null && user.isActive();
    
    public static final Predicate<User> IS_INACTIVE = IS_ACTIVE.negate();
    
    public static final Predicate<User> IS_STUDENT = user -> 
        user instanceof Student;
    
    public static final Predicate<User> IS_PROFESSOR = user -> 
        user instanceof Professor;
    
    public static final Predicate<User> IS_ADMIN = user -> 
        user instanceof Admin;
    
    public static final Predicate<User> HAS_EMAIL = user -> 
        user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty();
    
    public static final Predicate<User> HAS_PHONE = user -> 
        user != null && user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty();
    
    public static final Predicate<User> IS_VERIFIED = user -> 
        user != null && user.isEmailVerified();
    
    public static Predicate<User> hasRole(UserRole role) {
        return user -> user != null && user.getUserRole() == role;
    }
    
    public static Predicate<User> nameContains(String searchTerm) {
        String search = searchTerm == null ? "" : searchTerm.toLowerCase().trim();
        return user -> user != null && 
            (user.getFirstName().toLowerCase().contains(search) ||
             user.getLastName().toLowerCase().contains(search));
    }
    
    public static Predicate<User> emailDomain(String domain) {
        return user -> user != null && user.getEmail() != null &&
            user.getEmail().toLowerCase().endsWith("@" + domain.toLowerCase());
    }
    
    public static Predicate<User> createdAfter(LocalDate date) {
        return user -> user != null && user.getCreatedDate() != null &&
            user.getCreatedDate().isAfter(date);
    }
    
    public static Predicate<User> createdBefore(LocalDate date) {
        return user -> user != null && user.getCreatedDate() != null &&
            user.getCreatedDate().isBefore(date);
    }
    
    // ==================== STUDENT PREDICATES ====================
    
    public static final Predicate<Student> IS_UNDERGRADUATE = student -> 
        student != null && student.isUndergraduate();
    
    public static final Predicate<Student> IS_GRADUATE = student -> 
        student != null && student.isGraduate();
    
    public static final Predicate<Student> IS_FULL_TIME = student -> 
        student != null && student.isFullTime();
    
    public static final Predicate<Student> IS_PART_TIME = IS_FULL_TIME.negate();
    
    public static final Predicate<Student> IS_HONORS = student -> 
        student != null && student.isHonorsStudent();
    
    public static final Predicate<Student> IS_INTERNATIONAL = student -> 
        student != null && student.isInternationalStudent();
    
    public static final Predicate<Student> IS_DOMESTIC = IS_INTERNATIONAL.negate();
    
    public static final Predicate<Student> IS_GRADUATED = student -> 
        student != null && student.isGraduated();
    
    public static final Predicate<Student> IS_ENROLLED = student -> 
        student != null && !student.isGraduated() && student.isActive();
    
    public static Predicate<Student> hasGpaAbove(double minGpa) {
        return student -> student != null && student.getGpa() >= minGpa;
    }
    
    public static Predicate<Student> hasGpaBelow(double maxGpa) {
        return student -> student != null && student.getGpa() <= maxGpa;
    }
    
    public static Predicate<Student> hasGpaBetween(double minGpa, double maxGpa) {
        return student -> student != null && 
            student.getGpa() >= minGpa && student.getGpa() <= maxGpa;
    }
    
    public static Predicate<Student> inMajor(String major) {
        return student -> student != null && student.getMajor() != null &&
            student.getMajor().equalsIgnoreCase(major.trim());
    }
    
    public static Predicate<Student> hasMinor(String minor) {
        return student -> student != null && student.getMinor() != null &&
            student.getMinor().equalsIgnoreCase(minor.trim());
    }
    
    public static Predicate<Student> inYear(int academicYear) {
        return student -> student != null && student.getAcademicYear() == academicYear;
    }
    
    public static Predicate<Student> inDepartment(String department) {
        return student -> student != null && student.getDepartment() != null &&
            student.getDepartment().getName().equalsIgnoreCase(department.trim());
    }
    
    public static Predicate<Student> hasAdvisor(String advisorId) {
        return student -> student != null && student.getAdvisorId() != null &&
            student.getAdvisorId().equals(advisorId);
    }
    
    public static Predicate<Student> enrolledInCourse(String courseId) {
        return student -> student != null && student.getEnrolledCourses() != null &&
            student.getEnrolledCourses().stream()
                .anyMatch(enrollment -> enrollment.getCourseId().equals(courseId));
    }
    
    public static Predicate<Student> completedCreditsAbove(int minCredits) {
        return student -> student != null && student.getCompletedCredits() >= minCredits;
    }
    
    public static Predicate<Student> onAcademicProbation() {
        return student -> student != null && student.isOnAcademicProbation();
    }
    
    public static Predicate<Student> onDeansListEligible() {
        return student -> student != null && student.getGpa() >= 3.5;
    }
    
    // ==================== PROFESSOR PREDICATES ====================
    
    public static final Predicate<Professor> IS_TENURE_TRACK = professor -> 
        professor != null && professor.isTenureTrack();
    
    public static final Predicate<Professor> IS_TENURED = professor -> 
        professor != null && professor.isTenured();
    
    public static final Predicate<Professor> IS_ADJUNCT = professor -> 
        professor != null && professor.isAdjunct();
    
    public static final Predicate<Professor> IS_EMERITUS = professor -> 
        professor != null && professor.isEmeritus();
    
    public static final Predicate<Professor> IS_DEPARTMENT_HEAD = professor -> 
        professor != null && professor.isDepartmentHead();
    
    public static Predicate<Professor> hasRank(String rank) {
        return professor -> professor != null && professor.getRank() != null &&
            professor.getRank().equalsIgnoreCase(rank.trim());
    }
    
    public static Predicate<Professor> inDepartment(Department department) {
        return professor -> professor != null && professor.getDepartment() != null &&
            professor.getDepartment().equals(department);
    }
    
    public static Predicate<Professor> teachingCourse(String courseId) {
        return professor -> professor != null && professor.getTeachingCourses() != null &&
            professor.getTeachingCourses().stream()
                .anyMatch(course -> course.getCourseId().equals(courseId));
    }
    
    public static Predicate<Professor> hasResearchInterest(String interest) {
        return professor -> professor != null && professor.getResearchInterests() != null &&
            professor.getResearchInterests().stream()
                .anyMatch(research -> research.toLowerCase().contains(interest.toLowerCase()));
    }
    
    public static Predicate<Professor> hasOfficeHours() {
        return professor -> professor != null && professor.getOfficeHours() != null &&
            !professor.getOfficeHours().isEmpty();
    }
    
    public static Predicate<Professor> availableForAdvising() {
        return professor -> professor != null && professor.isAvailableForAdvising();
    }
    
    public static Predicate<Professor> hasPublications() {
        return professor -> professor != null && professor.getPublicationsCount() > 0;
    }
    
    public static Predicate<Professor> hiredAfter(LocalDate date) {
        return professor -> professor != null && professor.getHireDate() != null &&
            professor.getHireDate().isAfter(date);
    }
    
    // ==================== COURSE PREDICATES ====================
    
    public static final Predicate<Course> IS_ACTIVE = course -> 
        course != null && course.getStatus() == CourseStatus.ACTIVE;
    
    public static final Predicate<Course> IS_CANCELLED = course -> 
        course != null && course.getStatus() == CourseStatus.CANCELLED;
    
    public static final Predicate<Course> IS_FULL = course -> 
        course != null && course.isFull();
    
    public static final Predicate<Course> HAS_AVAILABILITY = IS_FULL.negate();
    
    public static final Predicate<Course> IS_ONLINE = course -> 
        course != null && course.isOnline();
    
    public static final Predicate<Course> IS_HYBRID = course -> 
        course != null && course.isHybrid();
    
    public static final Predicate<Course> IS_IN_PERSON = course -> 
        course != null && !course.isOnline() && !course.isHybrid();
    
    public static final Predicate<Course> HAS_PREREQUISITES = course -> 
        course != null && course.getPrerequisites() != null && !course.getPrerequisites().isEmpty();
    
    public static final Predicate<Course> NO_PREREQUISITES = HAS_PREREQUISITES.negate();
    
    public static Predicate<Course> inDepartment(String departmentCode) {
        return course -> course != null && course.getDepartmentCode() != null &&
            course.getDepartmentCode().equalsIgnoreCase(departmentCode.trim());
    }
    
    public static Predicate<Course> hasCredits(int credits) {
        return course -> course != null && course.getCredits() == credits;
    }
    
    public static Predicate<Course> creditsInRange(int minCredits, int maxCredits) {
        return course -> course != null && 
            course.getCredits() >= minCredits && course.getCredits() <= maxCredits;
    }
    
    public static Predicate<Course> taughtBy(String professorId) {
        return course -> course != null && course.getInstructorId() != null &&
            course.getInstructorId().equals(professorId);
    }
    
    public static Predicate<Course> scheduledFor(Semester semester) {
        return course -> course != null && course.getSemester() == semester;
    }
    
    public static Predicate<Course> meetsDuringTime(String timeSlot) {
        return course -> course != null && course.getSchedule() != null &&
            course.getSchedule().contains(timeSlot);
    }
    
    public static Predicate<Course> hasEnrollmentAbove(int minEnrollment) {
        return course -> course != null && course.getCurrentEnrollment() >= minEnrollment;
    }
    
    public static Predicate<Course> hasEnrollmentBelow(int maxEnrollment) {
        return course -> course != null && course.getCurrentEnrollment() <= maxEnrollment;
    }
    
    public static Predicate<Course> enrollmentBetween(int minEnrollment, int maxEnrollment) {
        return course -> course != null && 
            course.getCurrentEnrollment() >= minEnrollment && 
            course.getCurrentEnrollment() <= maxEnrollment;
    }
    
    public static Predicate<Course> courseLevel(int level) {
        return course -> course != null && course.getCourseNumber() != null &&
            course.getCourseNumber().startsWith(String.valueOf(level));
    }
    
    public static Predicate<Course> isUndergraduate() {
        return course -> course != null && course.getCourseNumber() != null &&
            course.getCourseNumber().matches("^[1-4]\\d{2}.*");
    }
    
    public static Predicate<Course> isGraduate() {
        return course -> course != null && course.getCourseNumber() != null &&
            course.getCourseNumber().matches("^[5-9]\\d{2}.*");
    }
    
    public static Predicate<Course> titleContains(String searchTerm) {
        String search = searchTerm == null ? "" : searchTerm.toLowerCase().trim();
        return course -> course != null && course.getTitle() != null &&
            course.getTitle().toLowerCase().contains(search);
    }
    
    public static Predicate<Course> descriptionContains(String searchTerm) {
        String search = searchTerm == null ? "" : searchTerm.toLowerCase().trim();
        return course -> course != null && course.getDescription() != null &&
            course.getDescription().toLowerCase().contains(search);
    }
    
    // ==================== ENROLLMENT PREDICATES ====================
    
    public static final Predicate<Enrollment> IS_ACTIVE_ENROLLMENT = enrollment -> 
        enrollment != null && enrollment.getStatus() == EnrollmentStatus.ENROLLED;
    
    public static final Predicate<Enrollment> IS_DROPPED = enrollment -> 
        enrollment != null && enrollment.getStatus() == EnrollmentStatus.DROPPED;
    
    public static final Predicate<Enrollment> IS_COMPLETED = enrollment -> 
        enrollment != null && enrollment.getStatus() == EnrollmentStatus.COMPLETED;
    
    public static final Predicate<Enrollment> IS_WITHDRAWN = enrollment -> 
        enrollment != null && enrollment.getStatus() == EnrollmentStatus.WITHDRAWN;
    
    public static final Predicate<Enrollment> HAS_GRADE = enrollment -> 
        enrollment != null && enrollment.getFinalGrade() != null;
    
    public static final Predicate<Enrollment> IS_PASSING = enrollment -> 
        enrollment != null && enrollment.getFinalGrade() != null &&
        enrollment.getFinalGrade().isPassingGrade();
    
    public static final Predicate<Enrollment> IS_FAILING = enrollment -> 
        enrollment != null && enrollment.getFinalGrade() != null &&
        enrollment.getFinalGrade().isFailingGrade();
    
    public static Predicate<Enrollment> enrolledInSemester(Semester semester) {
        return enrollment -> enrollment != null && enrollment.getSemester() == semester;
    }
    
    public static Predicate<Enrollment> enrolledAfter(LocalDate date) {
        return enrollment -> enrollment != null && enrollment.getEnrollmentDate() != null &&
            enrollment.getEnrollmentDate().isAfter(date);
    }
    
    public static Predicate<Enrollment> hasGradeLevel(GradeLevel gradeLevel) {
        return enrollment -> enrollment != null && enrollment.getFinalGrade() == gradeLevel;
    }
    
    public static Predicate<Enrollment> hasGpaAbove(double minGpa) {
        return enrollment -> enrollment != null && enrollment.getFinalGrade() != null &&
            enrollment.getFinalGrade().getGpaPoints() != null &&
            enrollment.getFinalGrade().getGpaPoints() >= minGpa;
    }
    
    // ==================== DEPARTMENT PREDICATES ====================
    
    public static final Predicate<Department> IS_ACTIVE_DEPARTMENT = department -> 
        department != null && department.isActive();
    
    public static final Predicate<Department> HAS_HEAD = department -> 
        department != null && department.getDepartmentHead() != null;
    
    public static final Predicate<Department> HAS_BUDGET = department -> 
        department != null && department.getBudget() > 0;
    
    public static Predicate<Department> nameContains(String searchTerm) {
        String search = searchTerm == null ? "" : searchTerm.toLowerCase().trim();
        return department -> department != null && department.getName() != null &&
            department.getName().toLowerCase().contains(search);
    }
    
    public static Predicate<Department> codeEquals(String code) {
        return department -> department != null && department.getCode() != null &&
            department.getCode().equalsIgnoreCase(code.trim());
    }
    
    public static Predicate<Department> budgetAbove(double minBudget) {
        return department -> department != null && department.getBudget() >= minBudget;
    }
    
    public static Predicate<Department> facultyCountAbove(int minCount) {
        return department -> department != null && department.getFacultyCount() >= minCount;
    }
    
    public static Predicate<Department> studentCountAbove(int minCount) {
        return department -> department != null && department.getStudentCount() >= minCount;
    }
    
    // ==================== GRADE PREDICATES ====================
    
    public static final Predicate<Grade> IS_PASSING_GRADE = grade -> 
        grade != null && grade.getGradeLevel() != null && grade.getGradeLevel().isPassingGrade();
    
    public static final Predicate<Grade> IS_FAILING_GRADE = grade -> 
        grade != null && grade.getGradeLevel() != null && grade.getGradeLevel().isFailingGrade();
    
    public static final Predicate<Grade> IS_HONORS_GRADE = grade -> 
        grade != null && grade.getGradeLevel() != null && grade.getGradeLevel().isHonorsGrade();
    
    public static final Predicate<Grade> AFFECTS_GPA = grade -> 
        grade != null && grade.getGradeLevel() != null && grade.getGradeLevel().affectsGpa();
    
    public static Predicate<Grade> gradedAfter(LocalDateTime date) {
        return grade -> grade != null && grade.getGradedDate() != null &&
            grade.getGradedDate().isAfter(date);
    }
    
    public static Predicate<Grade> gradedBy(String professorId) {
        return grade -> grade != null && grade.getGradedBy() != null &&
            grade.getGradedBy().equals(professorId);
    }
    
    public static Predicate<Grade> hasPercentageAbove(double minPercentage) {
        return grade -> grade != null && grade.getPercentage() >= minPercentage;
    }
    
    // ==================== VALIDATION PREDICATES ====================
    
    public static final Predicate<String> IS_VALID_EMAIL = email -> {
        if (email == null || email.trim().isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    };
    
    public static final Predicate<String> IS_VALID_PHONE = phone -> {
        if (phone == null || phone.trim().isEmpty()) return false;
        String phoneRegex = "^\\+?[1-9]\\d{1,14}$";
        return Pattern.compile(phoneRegex).matcher(phone.replaceAll("[\\s\\-\\(\\)]", "")).matches();
    };
    
    public static final Predicate<String> IS_NOT_EMPTY = str -> 
        str != null && !str.trim().isEmpty();
    
    public static final Predicate<String> IS_NUMERIC = str -> {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    };
    
    public static Predicate<String> hasMinLength(int minLength) {
        return str -> str != null && str.length() >= minLength;
    }
    
    public static Predicate<String> hasMaxLength(int maxLength) {
        return str -> str != null && str.length() <= maxLength;
    }
    
    public static Predicate<String> lengthBetween(int minLength, int maxLength) {
        return str -> str != null && str.length() >= minLength && str.length() <= maxLength;
    }
    
    public static Predicate<String> matchesPattern(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return str -> str != null && pattern.matcher(str).matches();
    }
    
    public static Predicate<String> containsOnly(String allowedChars) {
        return str -> str != null && str.chars()
            .allMatch(c -> allowedChars.indexOf(c) >= 0);
    }
    
    // ==================== DATE/TIME PREDICATES ====================
    
    public static Predicate<LocalDate> isAfter(LocalDate date) {
        return d -> d != null && d.isAfter(date);
    }
    
    public static Predicate<LocalDate> isBefore(LocalDate date) {
        return d -> d != null && d.isBefore(date);
    }
    
    public static Predicate<LocalDate> isBetween(LocalDate start, LocalDate end) {
        return d -> d != null && (d.isEqual(start) || d.isAfter(start)) && 
                   (d.isEqual(end) || d.isBefore(end));
    }
    
    public static Predicate<LocalDate> isToday() {
        LocalDate today = LocalDate.now();
        return d -> d != null && d.isEqual(today);
    }
    
    public static Predicate<LocalDate> isThisWeek() {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return isBetween(startOfWeek, endOfWeek);
    }
    
    public static Predicate<LocalDate> isThisMonth() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        return isBetween(startOfMonth, endOfMonth);
    }
    
    // ==================== COLLECTION PREDICATES ====================
    
    public static <T> Predicate<Collection<T>> hasSize(int size) {
        return collection -> collection != null && collection.size() == size;
    }
    
    public static <T> Predicate<Collection<T>> hasSizeAbove(int minSize) {
        return collection -> collection != null && collection.size() > minSize;
    }
    
    public static <T> Predicate<Collection<T>> hasSizeBelow(int maxSize) {
        return collection -> collection != null && collection.size() < maxSize;
    }
    
    public static <T> Predicate<Collection<T>> isEmpty() {
        return collection -> collection == null || collection.isEmpty();
    }
    
    public static <T> Predicate<Collection<T>> isNotEmpty() {
        return isEmpty().negate();
    }
    
    public static <T> Predicate<Collection<T>> contains(T element) {
        return collection -> collection != null && collection.contains(element);
    }
    
    public static <T> Predicate<Collection<T>> containsAll(Collection<T> elements) {
        return collection -> collection != null && collection.containsAll(elements);
    }
    
    // ==================== NUMERIC PREDICATES ====================
    
    public static <T extends Number> Predicate<T> isPositive() {
        return number -> number != null && number.doubleValue() > 0;
    }
    
    public static <T extends Number> Predicate<T> isNegative() {
        return number -> number != null && number.doubleValue() < 0;
    }
    
    public static <T extends Number> Predicate<T> isZero() {
        return number -> number != null && number.doubleValue() == 0;
    }
    
    public static <T extends Number> Predicate<T> isGreaterThan(double value) {
        return number -> number != null && number.doubleValue() > value;
    }
    
    public static <T extends Number> Predicate<T> isLessThan(double value) {
        return number -> number != null && number.doubleValue() < value;
    }
    
    public static <T extends Number> Predicate<T> isBetween(double min, double max) {
        return number -> number != null && 
            number.doubleValue() >= min && number.doubleValue() <= max;
    }
    
    // ==================== COMPOSITE PREDICATES ====================
    
    public static <T> Predicate<T> allOf(Predicate<T>... predicates) {
        return Arrays.stream(predicates)
            .reduce(x -> true, Predicate::and);
    }
    
    public static <T> Predicate<T> anyOf(Predicate<T>... predicates) {
        return Arrays.stream(predicates)
            .reduce(x -> false, Predicate::or);
    }
    
    public static <T> Predicate<T> noneOf(Predicate<T>... predicates) {
        return anyOf(predicates).negate();
    }
    
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
    
    // ==================== UTILITY METHODS ====================
    
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream()
            .filter(predicate)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().anyMatch(predicate);
    }
    
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().allMatch(predicate);
    }
    
    public static <T> boolean noneMatch(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().noneMatch(predicate);
    }
    
    public static <T> long count(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().filter(predicate).count();
    }
    
    public static <T> Optional<T> findFirst(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().filter(predicate).findFirst();
    }
    
    public static <T> Optional<T> findAny(Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().filter(predicate).findAny();
    }
}