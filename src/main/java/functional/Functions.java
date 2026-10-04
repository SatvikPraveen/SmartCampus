// File location: src/main/java/functional/Functions.java
package functional;

import models.*;
import enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Collection of commonly used functions for campus entities
 * Provides reusable function implementations for data transformation and extraction
 */
public final class Functions {
    
    private Functions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== USER FUNCTIONS ====================
    
    public static final Function<User, String> GET_FULL_NAME = user -> 
        user == null ? "" : user.getFirstName() + " " + user.getLastName();
    
    public static final Function<User, String> GET_EMAIL = user -> 
        user == null ? "" : user.getEmail();
    
    public static final Function<User, String> GET_FORMATTED_NAME = user -> {
        if (user == null) return "";
        return user.getLastName() + ", " + user.getFirstName();
    };
    
    public static final Function<User, String> GET_INITIALS = user -> {
        if (user == null || user.getFirstName() == null || user.getLastName() == null) return "";
        return user.getFirstName().substring(0, 1).toUpperCase() + 
               user.getLastName().substring(0, 1).toUpperCase();
    };
    
    public static final Function<User, String> GET_EMAIL_DOMAIN = user -> {
        if (user == null || user.getEmail() == null) return "";
        int atIndex = user.getEmail().indexOf('@');
        return atIndex > 0 ? user.getEmail().substring(atIndex + 1) : "";
    };
    
    public static final Function<User, String> GET_USER_ROLE = user -> 
        user == null ? null : user.getRole();
    
    public static final Function<User, String> GET_USER_TYPE = user -> {
        if (user instanceof Student) return "Student";
        if (user instanceof Professor) return "Professor";
        if (user instanceof Admin) return "Admin";
        return "Unknown";
    };
    
    public static final Function<User, LocalDate> GET_CREATED_DATE = user -> 
        user == null || user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDate();
    
    public static final Function<User, Long> GET_DAYS_SINCE_CREATION = user -> {
        if (user == null || user.getCreatedAt() == null) return 0L;
        return java.time.temporal.ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now());
    };
    
    public static final Function<User, String> GET_DISPLAY_NAME = user -> {
        if (user == null) return "Unknown User";
        String fullName = GET_FULL_NAME.apply(user);
        return fullName.trim().isEmpty() ? user.getUserId() : fullName;
    };
    
    // ==================== STUDENT FUNCTIONS ====================
    
    public static final Function<Student, Double> GET_GPA = student -> 
        student == null ? 0.0 : student.getGpa();
    
    public static final Function<Student, String> GET_MAJOR = student -> 
        student == null ? "" : student.getMajor();
    
    public static final Function<Student, Integer> GET_ACADEMIC_YEAR = student -> 
        student == null || student.getAcademicYear() == null ? 0 : student.getAcademicYear().getYear();
    
    public static final Function<Student, Integer> GET_COMPLETED_CREDITS = student -> 
        student == null ? 0 : student.getTotalCredits();
    
    public static final Function<Student, String> GET_CLASSIFICATION = student -> {
        if (student == null) return "Unknown";
        int year = GET_ACADEMIC_YEAR.apply(student);
        if (year == 1) return "Freshman";
        if (year == 2) return "Sophomore";
        if (year == 3) return "Junior";
        if (year == 4) return "Senior";
        if (year > 4) return "Graduate";
        return "Unknown";
    };
    
    public static final Function<Student, String> GET_ACADEMIC_STATUS = student -> {
        if (student == null) return "Unknown";
        if (student.isOnProbation()) return "Academic Probation";
        if (student.getGpa() >= 3.5) return "Good Standing";
        if (student.getGpa() >= 2.0) return "Satisfactory";
        return "Poor Standing";
    };
    
    public static final Function<Student, String> GET_DEPARTMENT_ID = student -> 
        student == null ? "" : student.getDepartmentId();
    
    public static final Function<Student, List<String>> GET_COURSE_IDS = student -> {
        if (student == null || student.getEnrolledCourseIds() == null) return new ArrayList<>();
        return new ArrayList<>(student.getEnrolledCourseIds());
    };
    
    // ==================== PROFESSOR FUNCTIONS ====================
    
    public static final Function<Professor, String> GET_RANK = professor -> 
        professor == null || professor.getAcademicRank() == null ? "" : professor.getAcademicRank().getTitle();
    
    public static final Function<Professor, String> GET_PROFESSOR_DEPARTMENT_ID = professor -> 
        professor == null ? "" : professor.getDepartmentId();
    
    public static final Function<Professor, String> GET_OFFICE_LOCATION = professor -> 
        professor == null ? "" : professor.getOfficeLocation();
    
    public static final Function<Professor, String> GET_RESEARCH_AREA = professor -> 
        professor == null ? "" : professor.getResearchArea();
    
    public static final Function<Professor, List<String>> GET_TEACHING_COURSE_IDS = professor -> {
        if (professor == null || professor.getTeachingCourseIds() == null) return new ArrayList<>();
        return new ArrayList<>(professor.getTeachingCourseIds());
    };
    
    public static final Function<Professor, Integer> GET_TEACHING_LOAD = professor -> {
        if (professor == null || professor.getTeachingCourseIds() == null) return 0;
        return professor.getTeachingCourseIds().size();
    };
    
    public static final Function<Professor, LocalDate> GET_HIRE_DATE = professor -> 
        professor == null ? null : professor.getHireDate();
    
    public static final Function<Professor, Long> GET_YEARS_OF_SERVICE = professor -> {
        if (professor == null || professor.getHireDate() == null) return 0L;
        return java.time.temporal.ChronoUnit.YEARS.between(professor.getHireDate(), LocalDate.now());
    };
    
    /**
     * Tenure-track status is derived from rank: untenured assistant and
     * associate professors are on the tenure track.
     */
    public static final Function<Professor, String> GET_EMPLOYMENT_STATUS = professor -> {
        if (professor == null) return "Unknown";
        if (professor.isTenured()) return "Tenured";
        Professor.AcademicRank rank = professor.getAcademicRank();
        if (rank == Professor.AcademicRank.ASSISTANT || rank == Professor.AcademicRank.ASSOCIATE) return "Tenure Track";
        if (rank == Professor.AcademicRank.ADJUNCT) return "Adjunct";
        if (rank == Professor.AcademicRank.EMERITUS) return "Emeritus";
        return "Other";
    };
    
    // ==================== COURSE FUNCTIONS ====================
    
    public static final Function<Course, String> GET_COURSE_CODE = course -> 
        course == null ? "" : course.getCourseCode();
    
    public static final Function<Course, String> GET_COURSE_TITLE = course -> 
        course == null ? "" : course.getCourseName();
    
    public static final Function<Course, String> GET_COURSE_DESCRIPTION = course -> 
        course == null ? "" : course.getDescription();
    
    public static final Function<Course, Integer> GET_CREDITS = course -> 
        course == null ? 0 : course.getCredits();
    
    public static final Function<Course, String> GET_COURSE_DEPARTMENT_ID = course -> 
        course == null ? "" : course.getDepartmentId();
    
    /**
     * Numeric part of the course code (e.g. "101" for "CS101")
     */
    public static final Function<Course, String> GET_COURSE_NUMBER = course -> {
        if (course == null || course.getCourseCode() == null) return "";
        return course.getCourseCode().replaceFirst("^\\D+", "").trim();
    };
    
    public static final Function<Course, Integer> GET_COURSE_LEVEL = course -> {
        String courseNumber = GET_COURSE_NUMBER.apply(course);
        try {
            return Integer.parseInt(courseNumber.substring(0, 1));
        } catch (Exception e) {
            return 0;
        }
    };
    
    public static final Function<Course, String> GET_INSTRUCTOR_ID = course -> 
        course == null ? "" : course.getProfessorId();
    
    public static final Function<Course, String> GET_SEMESTER = course -> 
        course == null ? null : course.getSemester();
    
    public static final Function<Course, Course.CourseStatus> GET_STATUS = course -> 
        course == null ? null : course.getStatus();
    
    public static final Function<Course, Integer> GET_MAX_ENROLLMENT = course -> 
        course == null ? 0 : course.getMaxEnrollment();
    
    public static final Function<Course, Integer> GET_CURRENT_ENROLLMENT = course -> 
        course == null ? 0 : course.getEnrolledStudentIds().size();
    
    public static final Function<Course, Integer> GET_AVAILABLE_SPOTS = course -> 
        course == null ? 0 : course.getAvailableSeats();
    
    public static final Function<Course, Double> GET_ENROLLMENT_PERCENTAGE = course -> 
        course == null ? 0.0 : course.getEnrollmentPercentage();
    
    public static final Function<Course, String> GET_SCHEDULE = course -> {
        if (course == null) return "";
        StringBuilder schedule = new StringBuilder();
        if (course.getMeetingDays() != null) {
            schedule.append(String.join(", ", course.getMeetingDays()));
        }
        if (course.getStartTime() != null && course.getEndTime() != null) {
            if (schedule.length() > 0) schedule.append(' ');
            schedule.append(course.getStartTime()).append('-').append(course.getEndTime());
        }
        return schedule.toString();
    };
    
    public static final Function<Course, String> GET_COURSE_LOCATION = course -> {
        if (course == null) return "";
        String building = course.getBuilding() != null ? course.getBuilding() : "";
        String classroom = course.getClassroom() != null ? course.getClassroom() : "";
        return (building + " " + classroom).trim();
    };
    
    public static final Function<Course, List<String>> GET_PREREQUISITES = course -> 
        course == null ? new ArrayList<>() : course.getPrerequisiteCourseIds();
    
    // ==================== ENROLLMENT FUNCTIONS ====================
    
    public static final Function<Enrollment, String> GET_STUDENT_ID = enrollment -> 
        enrollment == null ? "" : enrollment.getStudentId();
    
    public static final Function<Enrollment, String> GET_ENROLLMENT_COURSE_ID = enrollment -> 
        enrollment == null ? "" : enrollment.getCourseId();
    
    public static final Function<Enrollment, String> GET_ENROLLMENT_SEMESTER = enrollment -> 
        enrollment == null ? null : enrollment.getSemester();
    
    public static final Function<Enrollment, Enrollment.EnrollmentStatus> GET_ENROLLMENT_STATUS = enrollment -> 
        enrollment == null ? null : enrollment.getStatus();
    
    public static final Function<Enrollment, LocalDate> GET_ENROLLMENT_DATE = enrollment -> 
        enrollment == null || enrollment.getEnrollmentDate() == null ? null : enrollment.getEnrollmentDate().toLocalDate();
    
    public static final Function<Enrollment, Enrollment.Grade> GET_FINAL_GRADE = enrollment -> 
        enrollment == null ? null : enrollment.getGrade();
    
    public static final Function<Enrollment, Double> GET_GRADE_POINTS = enrollment -> {
        if (enrollment == null || enrollment.getGrade() == null) return 0.0;
        return enrollment.getGrade().getPoints();
    };
    
    /**
     * Days since enrollment; for completed or dropped enrollments the
     * last modification (status change) marks the end.
     */
    public static final Function<Enrollment, Long> GET_ENROLLMENT_DURATION_DAYS = enrollment -> {
        if (enrollment == null || enrollment.getEnrollmentDate() == null) return 0L;
        boolean ended = enrollment.getStatus() == Enrollment.EnrollmentStatus.COMPLETED || 
                        enrollment.getStatus() == Enrollment.EnrollmentStatus.DROPPED;
        LocalDateTime endDate = ended && enrollment.getLastModified() != null
                          ? enrollment.getLastModified() : LocalDateTime.now();
        return java.time.temporal.ChronoUnit.DAYS.between(enrollment.getEnrollmentDate(), endDate);
    };
    
    // ==================== DEPARTMENT FUNCTIONS ====================
    
    public static final Function<Department, String> GET_DEPARTMENT_NAME = department -> 
        department == null ? "" : department.getDepartmentName();
    
    public static final Function<Department, String> GET_DEPARTMENT_CODE = department -> 
        department == null ? "" : department.getDepartmentCode();
    
    public static final Function<Department, String> GET_DEPARTMENT_HEAD_ID = department -> 
        department == null ? null : department.getHeadOfDepartmentId();
    
    public static final Function<Department, Double> GET_BUDGET = department -> {
        if (department == null) return 0.0;
        BigDecimal budget = department.getAnnualBudget();
        return budget != null ? budget.doubleValue() : 0.0;
    };
    
    public static final Function<Department, Integer> GET_FACULTY_COUNT = department -> 
        department == null ? 0 : department.getProfessorCount();
    
    public static final Function<Department, Integer> GET_STUDENT_COUNT = department -> 
        department == null ? 0 : department.getStudentCount();
    
    public static final Function<Department, String> GET_DESCRIPTION = department -> 
        department == null ? "" : department.getDescription();
    
    public static final Function<Department, String> GET_LOCATION = department -> 
        department == null ? "" : department.getLocation();
    
    // ==================== GRADE FUNCTIONS ====================
    
    public static final Function<Grade, GradeLevel> GET_GRADE_LEVEL = grade -> {
        if (grade == null) return null;
        GradeLevel level = grade.getLetterGrade() != null ? GradeLevel.fromLetterGrade(grade.getLetterGrade()) : null;
        return level != null ? level : GradeLevel.fromPercentage(grade.getPercentage());
    };
    
    public static final Function<Grade, Double> GET_PERCENTAGE = grade -> 
        grade == null ? 0.0 : grade.getPercentage();
    
    public static final Function<Grade, String> GET_LETTER_GRADE = grade -> {
        if (grade == null || grade.getLetterGrade() == null) return "";
        return grade.getLetterGrade();
    };
    
    public static final Function<Grade, String> GET_GRADED_BY = grade -> 
        grade == null ? "" : grade.getGradedBy();
    
    public static final Function<Grade, LocalDateTime> GET_GRADED_DATE = grade -> 
        grade == null ? null : grade.getDateGraded();
    
    public static final Function<Grade, String> GET_COMMENTS = grade -> 
        grade == null ? "" : grade.getFeedback();
    
        // ==================== STRING TRANSFORMATION FUNCTIONS ====================
    
    public static final Function<String, String> TO_UPPER_CASE = str -> 
        str == null ? "" : str.toUpperCase();
    
    public static final Function<String, String> TO_LOWER_CASE = str -> 
        str == null ? "" : str.toLowerCase();
    
    public static final Function<String, String> TRIM = str -> 
        str == null ? "" : str.trim();
    
    public static final Function<String, String> TO_TITLE_CASE = str -> {
        if (str == null || str.isEmpty()) return "";
        return Arrays.stream(str.trim().split("\\s+"))
            .filter(word -> !word.isEmpty())
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    };
    
    public static final Function<String, Integer> STRING_LENGTH = str -> 
        str == null ? 0 : str.length();
    
    public static final Function<String, String> REVERSE_STRING = str -> {
        if (str == null) return "";
        return new StringBuilder(str).reverse().toString();
    };
    
    public static Function<String, String> truncate(int maxLength) {
        return str -> {
            if (str == null) return "";
            if (str.length() <= maxLength) return str;
            return str.substring(0, maxLength) + "...";
        };
    }
    
    public static Function<String, String> padLeft(int totalLength, char padChar) {
        return str -> {
            if (str == null) str = "";
            if (str.length() >= totalLength) return str;
            return String.valueOf(padChar).repeat(totalLength - str.length()) + str;
        };
    }
    
    public static Function<String, String> padRight(int totalLength, char padChar) {
        return str -> {
            if (str == null) str = "";
            if (str.length() >= totalLength) return str;
            return str + String.valueOf(padChar).repeat(totalLength - str.length());
        };
    }
    
    // ==================== DATE/TIME TRANSFORMATION FUNCTIONS ====================
    
    public static final Function<LocalDate, String> FORMAT_DATE = date -> 
        date == null ? "" : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    
    public static final Function<LocalDateTime, String> FORMAT_DATETIME = datetime -> 
        datetime == null ? "" : datetime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    
    public static Function<LocalDate, String> formatDate(String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return date -> date == null ? "" : date.format(formatter);
    }
    
    public static Function<LocalDateTime, String> formatDateTime(String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return datetime -> datetime == null ? "" : datetime.format(formatter);
    }
    
    public static final Function<LocalDate, Integer> GET_YEAR = date -> 
        date == null ? 0 : date.getYear();
    
    public static final Function<LocalDate, Integer> GET_MONTH = date -> 
        date == null ? 0 : date.getMonthValue();
    
    public static final Function<LocalDate, Integer> GET_DAY = date -> 
        date == null ? 0 : date.getDayOfMonth();
    
    public static final Function<LocalDate, String> GET_DAY_OF_WEEK = date -> 
        date == null ? "" : date.getDayOfWeek().toString();
    
    // ==================== NUMERIC TRANSFORMATION FUNCTIONS ====================
    
    public static final Function<Number, Double> TO_DOUBLE = number -> 
        number == null ? 0.0 : number.doubleValue();
    
    public static final Function<Number, Integer> TO_INTEGER = number -> 
        number == null ? 0 : number.intValue();
    
    public static final Function<Number, Long> TO_LONG = number -> 
        number == null ? 0L : number.longValue();
    
    public static final Function<Number, String> TO_STRING = number -> 
        number == null ? "0" : number.toString();
    
    public static Function<Double, String> formatDecimal(int decimalPlaces) {
        return number -> {
            if (number == null) return "0";
            return String.format("%." + decimalPlaces + "f", number);
        };
    }
    
    public static final Function<Double, String> TO_PERCENTAGE = number -> {
        if (number == null) return "0%";
        return String.format("%.1f%%", number);
    };
    
    public static Function<Number, Number> multiply(double factor) {
        return number -> number == null ? 0 : number.doubleValue() * factor;
    }
    
    public static Function<Number, Number> add(double addend) {
        return number -> number == null ? addend : number.doubleValue() + addend;
    }
    
    // ==================== COLLECTION TRANSFORMATION FUNCTIONS ====================
    
    public static final Function<Collection<?>, Integer> COLLECTION_SIZE = collection -> 
        collection == null ? 0 : collection.size();
    
    public static final Function<Collection<?>, Boolean> IS_EMPTY = collection -> 
        collection == null || collection.isEmpty();
    
    public static <T> Function<Collection<T>, List<T>> toList() {
        return collection -> collection == null ? new ArrayList<>() : new ArrayList<>(collection);
    }
    
    public static <T> Function<Collection<T>, Set<T>> toSet() {
        return collection -> collection == null ? new HashSet<>() : new HashSet<>(collection);
    }
    
    public static <T> Function<List<T>, T> getFirst() {
        return list -> (list == null || list.isEmpty()) ? null : list.get(0);
    }
    
    public static <T> Function<List<T>, T> getLast() {
        return list -> (list == null || list.isEmpty()) ? null : list.get(list.size() - 1);
    }
    
    public static <T> Function<List<T>, List<T>> reverse() {
        return list -> {
            if (list == null) return new ArrayList<>();
            List<T> reversed = new ArrayList<>(list);
            Collections.reverse(reversed);
            return reversed;
        };
    }
    
    // ==================== COMPOSITE FUNCTIONS ====================
    
    public static <T, R, S> Function<T, S> compose(Function<T, R> first, Function<R, S> second) {
        return first.andThen(second);
    }
    
    public static <T> Function<T, T> identity() {
        return Function.identity();
    }
    
    public static <T, R> Function<T, R> constant(R value) {
        return t -> value;
    }
    
    public static <T, R> Function<T, Optional<R>> safe(Function<T, R> function) {
        return t -> {
            try {
                return Optional.ofNullable(function.apply(t));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
    
    // ==================== UTILITY METHODS ====================
    
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        return collection.stream()
            .map(mapper)
            .collect(Collectors.toList());
    }
    
    public static <T, R> Set<R> mapToSet(Collection<T> collection, Function<T, R> mapper) {
        return collection.stream()
            .map(mapper)
            .collect(Collectors.toSet());
    }
    
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        return collection.stream()
            .collect(Collectors.groupingBy(classifier));
    }
    
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection, 
                                           Function<T, K> keyMapper, 
                                           Function<T, V> valueMapper) {
        return collection.stream()
            .collect(Collectors.toMap(keyMapper, valueMapper));
    }
}