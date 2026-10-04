// File location: src/main/java/io/CsvProcessor.java

package io;

import models.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles CSV file processing for the SmartCampus system
 * Provides read/write operations for all entity types
 */
public class CsvProcessor {
    
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_QUOTE = "\"";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // CSV Headers for different entities
    public static final String[] STUDENT_HEADERS = {
        "id", "firstName", "lastName", "email", "departmentId", "major", "academicYear", "enrollmentDate"
    };
    
    public static final String[] PROFESSOR_HEADERS = {
        "id", "firstName", "lastName", "email", "departmentId", "academicRank",
        "specialization", "officeLocation", "yearsOfExperience"
    };
    
    public static final String[] COURSE_HEADERS = {
        "courseId", "courseCode", "name", "description", "credits", "departmentId",
        "professorId", "difficultyLevel", "semester", "year", "capacity"
    };
    
    public static final String[] DEPARTMENT_HEADERS = {
        "departmentId", "departmentCode", "name", "description", "location",
        "headOfDepartmentId", "establishedYear"
    };
    
    public static final String[] ENROLLMENT_HEADERS = {
        "enrollmentId", "studentId", "courseId", "enrollmentDate", "semester", "year", "status"
    };
    
    public static final String[] GRADE_HEADERS = {
        "gradeId", "enrollmentId", "studentId", "courseId", "assignmentName", "component",
        "pointsEarned", "pointsPossible", "semester", "year", "dateGraded", "feedback"
    };
    
    /**
     * Read students from CSV file
     */
    public static List<Student> readStudents(Path csvFile, Map<String, Department> departmentMap) 
            throws IOException, ParseException {
        
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Skip header line
        return lines.stream()
                .skip(1)
                .filter(line -> !line.trim().isEmpty())
                .map(line -> parseStudentFromCsv(line, departmentMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Write students to CSV file
     */
    public static void writeStudents(List<Student> students, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(CSV_DELIMITER, STUDENT_HEADERS));
        
        for (Student student : students) {
            lines.add(formatStudentToCsv(student));
        }
        
        FileUtil.writeLines(csvFile, lines);
    }
    
    /**
     * Read professors from CSV file
     */
    public static List<Professor> readProfessors(Path csvFile, Map<String, Department> departmentMap) 
            throws IOException, ParseException {
        
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        return lines.stream()
                .skip(1)
                .filter(line -> !line.trim().isEmpty())
                .map(line -> parseProfessorFromCsv(line, departmentMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Write professors to CSV file
     */
    public static void writeProfessors(List<Professor> professors, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(CSV_DELIMITER, PROFESSOR_HEADERS));
        
        for (Professor professor : professors) {
            lines.add(formatProfessorToCsv(professor));
        }
        
        FileUtil.writeLines(csvFile, lines);
    }
    
    /**
     * Read courses from CSV file
     */
    public static List<Course> readCourses(Path csvFile, Map<String, Department> departmentMap,
                                         Map<String, Professor> professorMap) 
            throws IOException, ParseException {
        
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        return lines.stream()
                .skip(1)
                .filter(line -> !line.trim().isEmpty())
                .map(line -> parseCourseFromCsv(line, departmentMap, professorMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Write courses to CSV file
     */
    public static void writeCourses(List<Course> courses, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(CSV_DELIMITER, COURSE_HEADERS));
        
        for (Course course : courses) {
            lines.add(formatCourseToCsv(course));
        }
        
        FileUtil.writeLines(csvFile, lines);
    }
    
    /**
     * Read departments from CSV file
     */
    public static List<Department> readDepartments(Path csvFile) throws IOException, ParseException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        return lines.stream()
                .skip(1)
                .filter(line -> !line.trim().isEmpty())
                .map(CsvProcessor::parseDepartmentFromCsv)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Write departments to CSV file
     */
    public static void writeDepartments(List<Department> departments, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(CSV_DELIMITER, DEPARTMENT_HEADERS));
        
        for (Department department : departments) {
            lines.add(formatDepartmentToCsv(department));
        }
        
        FileUtil.writeLines(csvFile, lines);
    }
    
    /**
     * Read enrollments from CSV file
     */
    public static List<Enrollment> readEnrollments(Path csvFile, Map<String, Student> studentMap,
                                                  Map<String, Course> courseMap) 
            throws IOException, ParseException {
        
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        return lines.stream()
                .skip(1)
                .filter(line -> !line.trim().isEmpty())
                .map(line -> parseEnrollmentFromCsv(line, studentMap, courseMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Write enrollments to CSV file
     */
    public static void writeEnrollments(List<Enrollment> enrollments, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(CSV_DELIMITER, ENROLLMENT_HEADERS));
        
        for (Enrollment enrollment : enrollments) {
            lines.add(formatEnrollmentToCsv(enrollment));
        }
        
        FileUtil.writeLines(csvFile, lines);
    }
    
    /**
     * Read grades from CSV file
     */
    public static List<Grade> readGrades(Path csvFile, Map<String, Student> studentMap,
                                       Map<String, Course> courseMap) 
            throws IOException, ParseException {
        
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        return lines.stream()
                .skip(1)
                .filter(line -> !line.trim().isEmpty())
                .map(line -> parseGradeFromCsv(line, studentMap, courseMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Write grades to CSV file
     */
    public static void writeGrades(List<Grade> grades, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(CSV_DELIMITER, GRADE_HEADERS));
        
        for (Grade grade : grades) {
            lines.add(formatGradeToCsv(grade));
        }
        
        FileUtil.writeLines(csvFile, lines);
    }
    
    // Parsing methods for individual entities
    
    private static Student parseStudentFromCsv(String csvLine, Map<String, Department> departmentMap) {
        try {
            String[] values = parseCsvLine(csvLine);
            if (values.length < STUDENT_HEADERS.length) {
                return null;
            }
            
            String id = values[0].trim();
            String firstName = values[1].trim();
            String lastName = values[2].trim();
            String email = values[3].trim();
            String departmentId = values[4].trim();
            String major = values[5].trim();
            Student.AcademicYear academicYear = Student.AcademicYear.valueOf(values[6].trim());
            LocalDate enrollmentDate = LocalDate.parse(values[7].trim(), DATE_FORMAT);
            
            if (!departmentMap.containsKey(departmentId)) {
                System.err.println("Warning: Department not found for ID: " + departmentId);
            }
            
            Student student = new Student(id, firstName, lastName, email, null, id, major, academicYear);
            if (!departmentId.isEmpty()) {
                student.setDepartmentId(departmentId);
            }
            student.setEnrollmentDate(enrollmentDate);
            return student;
        } catch (Exception e) {
            System.err.println("Error parsing student from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    private static Professor parseProfessorFromCsv(String csvLine, Map<String, Department> departmentMap) {
        try {
            String[] values = parseCsvLine(csvLine);
            if (values.length < PROFESSOR_HEADERS.length) {
                return null;
            }
            
            String id = values[0].trim();
            String firstName = values[1].trim();
            String lastName = values[2].trim();
            String email = values[3].trim();
            String departmentId = values[4].trim();
            Professor.AcademicRank academicRank = Professor.AcademicRank.valueOf(values[5].trim());
            String specialization = values[6].trim();
            String officeLocation = values[7].trim();
            int yearsOfExperience = Integer.parseInt(values[8].trim());
            
            if (!departmentMap.containsKey(departmentId)) {
                System.err.println("Warning: Department not found for ID: " + departmentId);
            }
            
            Professor professor = new Professor(id, firstName, lastName, email, null, id,
                                                departmentId, academicRank, specialization);
            professor.setOfficeLocation(officeLocation);
            professor.setYearsOfExperience(yearsOfExperience);
            return professor;
        } catch (Exception e) {
            System.err.println("Error parsing professor from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    private static Course parseCourseFromCsv(String csvLine, Map<String, Department> departmentMap,
                                           Map<String, Professor> professorMap) {
        try {
            String[] values = parseCsvLine(csvLine);
            if (values.length < COURSE_HEADERS.length) {
                return null;
            }
            
            String courseId = values[0].trim();
            String courseCode = values[1].trim();
            String name = values[2].trim();
            String description = values[3].trim();
            int credits = Integer.parseInt(values[4].trim());
            String departmentId = values[5].trim();
            String professorId = values[6].trim();
            Course.DifficultyLevel difficultyLevel = Course.DifficultyLevel.valueOf(values[7].trim());
            String semester = values[8].trim();
            int year = Integer.parseInt(values[9].trim());
            int capacity = Integer.parseInt(values[10].trim());
            
            if (!departmentMap.containsKey(departmentId)) {
                System.err.println("Warning: Department not found for ID: " + departmentId);
            }
            if (!professorMap.containsKey(professorId)) {
                System.err.println("Warning: Professor not found for ID: " + professorId);
            }
            
            Course course = new Course(courseId, courseCode, name, description, credits, departmentId,
                                       professorId.isEmpty() ? null : professorId,
                                       difficultyLevel, semester, year);
            course.setMaxEnrollment(capacity);
            return course;
        } catch (Exception e) {
            System.err.println("Error parsing course from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    private static Department parseDepartmentFromCsv(String csvLine) {
        try {
            String[] values = parseCsvLine(csvLine);
            if (values.length < DEPARTMENT_HEADERS.length) {
                return null;
            }
            
            String departmentId = values[0].trim();
            String departmentCode = values[1].trim();
            String name = values[2].trim();
            String description = values[3].trim();
            String location = values[4].trim();
            String headOfDepartmentId = values[5].trim();
            String establishedYear = values[6].trim();
            
            Department department = new Department(departmentId, departmentCode, name, description, location);
            if (!headOfDepartmentId.isEmpty()) {
                department.setHeadOfDepartmentId(headOfDepartmentId);
            }
            if (!establishedYear.isEmpty()) {
                department.setEstablishedYear(establishedYear);
            }
            return department;
        } catch (Exception e) {
            System.err.println("Error parsing department from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    private static Enrollment parseEnrollmentFromCsv(String csvLine, Map<String, Student> studentMap,
                                                   Map<String, Course> courseMap) {
        try {
            String[] values = parseCsvLine(csvLine);
            if (values.length < ENROLLMENT_HEADERS.length) {
                return null;
            }
            
            String enrollmentId = values[0].trim();
            String studentId = values[1].trim();
            String courseId = values[2].trim();
            LocalDate enrollmentDate = LocalDate.parse(values[3].trim(), DATE_FORMAT);
            String semester = values[4].trim();
            int year = Integer.parseInt(values[5].trim());
            Enrollment.EnrollmentStatus status = Enrollment.EnrollmentStatus.valueOf(values[6].trim());
            
            if (!studentMap.containsKey(studentId)) {
                System.err.println("Warning: Student not found for ID: " + studentId);
                return null;
            }
            if (!courseMap.containsKey(courseId)) {
                System.err.println("Warning: Course not found for ID: " + courseId);
                return null;
            }
            
            Enrollment enrollment = new Enrollment(enrollmentId, studentId, courseId, semester, year);
            enrollment.setEnrollmentDate(enrollmentDate.atStartOfDay());
            enrollment.setStatus(status);
            return enrollment;
        } catch (Exception e) {
            System.err.println("Error parsing enrollment from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    private static Grade parseGradeFromCsv(String csvLine, Map<String, Student> studentMap,
                                         Map<String, Course> courseMap) {
        try {
            String[] values = parseCsvLine(csvLine);
            if (values.length < GRADE_HEADERS.length - 1) {
                return null;
            }
            
            String gradeId = values[0].trim();
            String enrollmentId = values[1].trim();
            String studentId = values[2].trim();
            String courseId = values[3].trim();
            String assignmentName = values[4].trim();
            Grade.GradeComponent component = Grade.GradeComponent.valueOf(values[5].trim());
            double pointsEarned = Double.parseDouble(values[6].trim());
            double pointsPossible = Double.parseDouble(values[7].trim());
            String semester = values[8].trim();
            int year = Integer.parseInt(values[9].trim());
            String dateGraded = values[10].trim();
            String feedback = values.length > 11 ? values[11].trim() : "";
            
            if (!studentMap.containsKey(studentId)) {
                System.err.println("Warning: Student not found for ID: " + studentId);
                return null;
            }
            if (!courseMap.containsKey(courseId)) {
                System.err.println("Warning: Course not found for ID: " + courseId);
                return null;
            }
            
            Grade grade = new Grade(gradeId, enrollmentId, studentId, courseId, assignmentName, component);
            grade.setPointsPossible(pointsPossible);
            grade.setPointsEarned(pointsEarned);
            grade.setSemester(semester);
            grade.setYear(year);
            if (!dateGraded.isEmpty()) {
                grade.setDateGraded(LocalDateTime.parse(dateGraded, DATETIME_FORMAT));
            }
            grade.setFeedback(feedback);
            return grade;
        } catch (Exception e) {
            System.err.println("Error parsing grade from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    // Formatting methods for CSV output
    
    private static String formatStudentToCsv(Student student) {
        return String.join(CSV_DELIMITER,
            escapeCsvValue(student.getStudentId()),
            escapeCsvValue(student.getFirstName()),
            escapeCsvValue(student.getLastName()),
            escapeCsvValue(student.getEmail()),
            escapeCsvValue(student.getDepartmentId()),
            escapeCsvValue(student.getMajor()),
            student.getAcademicYear() != null ? student.getAcademicYear().name() : "",
            student.getEnrollmentDate() != null ? DATE_FORMAT.format(student.getEnrollmentDate()) : ""
        );
    }
    
    private static String formatProfessorToCsv(Professor professor) {
        return String.join(CSV_DELIMITER,
            escapeCsvValue(professor.getProfessorId()),
            escapeCsvValue(professor.getFirstName()),
            escapeCsvValue(professor.getLastName()),
            escapeCsvValue(professor.getEmail()),
            escapeCsvValue(professor.getDepartmentId()),
            professor.getAcademicRank() != null ? professor.getAcademicRank().name() : "",
            escapeCsvValue(professor.getSpecialization()),
            escapeCsvValue(professor.getOfficeLocation()),
            String.valueOf(professor.getYearsOfExperience())
        );
    }
    
    private static String formatCourseToCsv(Course course) {
        return String.join(CSV_DELIMITER,
            escapeCsvValue(course.getCourseId()),
            escapeCsvValue(course.getCourseCode()),
            escapeCsvValue(course.getCourseName()),
            escapeCsvValue(course.getDescription()),
            String.valueOf(course.getCredits()),
            escapeCsvValue(course.getDepartmentId()),
            escapeCsvValue(course.getProfessorId()),
            course.getDifficultyLevel() != null ? course.getDifficultyLevel().name() : "",
            escapeCsvValue(course.getSemester()),
            String.valueOf(course.getYear()),
            String.valueOf(course.getMaxEnrollment())
        );
    }
    
    private static String formatDepartmentToCsv(Department department) {
        return String.join(CSV_DELIMITER,
            escapeCsvValue(department.getDepartmentId()),
            escapeCsvValue(department.getDepartmentCode()),
            escapeCsvValue(department.getDepartmentName()),
            escapeCsvValue(department.getDescription()),
            escapeCsvValue(department.getLocation()),
            escapeCsvValue(department.getHeadOfDepartmentId()),
            escapeCsvValue(department.getEstablishedYear())
        );
    }
    
    private static String formatEnrollmentToCsv(Enrollment enrollment) {
        return String.join(CSV_DELIMITER,
            escapeCsvValue(enrollment.getEnrollmentId()),
            escapeCsvValue(enrollment.getStudentId()),
            escapeCsvValue(enrollment.getCourseId()),
            enrollment.getEnrollmentDate() != null ? DATE_FORMAT.format(enrollment.getEnrollmentDate()) : "",
            escapeCsvValue(enrollment.getSemester()),
            String.valueOf(enrollment.getYear()),
            enrollment.getStatus() != null ? enrollment.getStatus().name() : ""
        );
    }
    
    private static String formatGradeToCsv(Grade grade) {
        return String.join(CSV_DELIMITER,
            escapeCsvValue(grade.getGradeId()),
            escapeCsvValue(grade.getEnrollmentId()),
            escapeCsvValue(grade.getStudentId()),
            escapeCsvValue(grade.getCourseId()),
            escapeCsvValue(grade.getAssignmentName()),
            grade.getComponent() != null ? grade.getComponent().name() : "",
            String.valueOf(grade.getPointsEarned()),
            String.valueOf(grade.getPointsPossible()),
            escapeCsvValue(grade.getSemester()),
            String.valueOf(grade.getYear()),
            grade.getDateGraded() != null ? DATETIME_FORMAT.format(grade.getDateGraded()) : "",
            escapeCsvValue(grade.getFeedback() != null ? grade.getFeedback() : "")
        );
    }
    
    // Utility methods for CSV processing
    
    /**
     * Parse CSV line handling quoted values and commas within quotes
     */
    private static String[] parseCsvLine(String csvLine) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (int i = 0; i < csvLine.length(); i++) {
            char c = csvLine.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < csvLine.length() && csvLine.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentValue.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of value
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        
        // Add the last value
        values.add(currentValue.toString());
        
        return values.toArray(new String[0]);
    }
    
    /**
     * Escape CSV value by adding quotes if necessary
     */
    private static String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // Escape existing quotes by doubling them
            String escapedValue = value.replace("\"", "\"\"");
            return "\"" + escapedValue + "\"";
        }
        
        return value;
    }
    
    /**
     * Create lookup maps for efficient entity resolution
     */
    public static <T, K> Map<K, T> createLookupMap(List<T> entities, Function<T, K> keyExtractor) {
        return entities.stream()
                .collect(Collectors.toMap(keyExtractor, Function.identity(), (existing, replacement) -> existing));
    }
    
    /**
     * Batch processing for large CSV files
     */
    public static <T> void processCsvInBatches(Path csvFile, int batchSize, 
                                             Function<List<String>, List<T>> processor,
                                             java.util.function.Consumer<List<T>> batchConsumer) 
            throws IOException {
        
        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            List<String> batch = new ArrayList<>();
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }
                
                if (!line.trim().isEmpty()) {
                    batch.add(line);
                    
                    if (batch.size() >= batchSize) {
                        List<T> processedBatch = processor.apply(batch);
                        batchConsumer.accept(processedBatch);
                        batch.clear();
                    }
                }
            }
            
            // Process remaining items
            if (!batch.isEmpty()) {
                List<T> processedBatch = processor.apply(batch);
                batchConsumer.accept(processedBatch);
            }
        }
    }
    
    /**
     * Validate CSV headers
     */
    public static boolean validateHeaders(Path csvFile, String[] expectedHeaders) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return false;
        }
        
        String headerLine = lines.get(0);
        String[] actualHeaders = parseCsvLine(headerLine);
        
        if (actualHeaders.length != expectedHeaders.length) {
            return false;
        }
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            if (!expectedHeaders[i].equalsIgnoreCase(actualHeaders[i].trim())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get CSV file statistics
     */
    public static CsvStatistics getCsvStatistics(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        
        if (lines.isEmpty()) {
            return new CsvStatistics(0, 0, 0, 0);
        }
        
        int totalLines = lines.size();
        int headerLines = 1;
        int dataLines = totalLines - headerLines;
        int emptyLines = (int) lines.stream()
                .skip(1)
                .mapToLong(line -> line.trim().isEmpty() ? 1 : 0)
                .sum();
        
        return new CsvStatistics(totalLines, headerLines, dataLines - emptyLines, emptyLines);
    }
    
    /**
     * Merge multiple CSV files of the same type
     */
    public static void mergeCsvFiles(List<Path> csvFiles, Path outputFile, String[] headers) throws IOException {
        List<String> allLines = new ArrayList<>();
        allLines.add(String.join(CSV_DELIMITER, headers));
        
        for (Path csvFile : csvFiles) {
            if (Files.exists(csvFile)) {
                List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    // Skip header and add data lines
                    allLines.addAll(lines.stream().skip(1).collect(Collectors.toList()));
                }
            }
        }
        
        FileUtil.writeLines(outputFile, allLines);
    }
    
    /**
     * Split large CSV file into smaller chunks
     */
    public static List<Path> splitCsvFile(Path csvFile, int linesPerFile, String baseName) throws IOException {
        List<String> allLines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (allLines.isEmpty()) {
            return Collections.emptyList();
        }
        
        String header = allLines.get(0);
        List<String> dataLines = allLines.subList(1, allLines.size());
        List<Path> outputFiles = new ArrayList<>();
        
        int fileIndex = 1;
        for (int i = 0; i < dataLines.size(); i += linesPerFile) {
            int endIndex = Math.min(i + linesPerFile, dataLines.size());
            List<String> chunk = new ArrayList<>();
            chunk.add(header);
            chunk.addAll(dataLines.subList(i, endIndex));
            
            Path outputFile = csvFile.getParent().resolve(baseName + "_part" + fileIndex + ".csv");
            FileUtil.writeLines(outputFile, chunk);
            outputFiles.add(outputFile);
            fileIndex++;
        }
        
        return outputFiles;
    }
    
    /**
     * Convert CSV to different formats
     */
    public static void convertCsvToTsv(Path csvFile, Path tsvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        List<String> tsvLines = lines.stream()
                .map(line -> {
                    String[] values = parseCsvLine(line);
                    return String.join("\t", values);
                })
                .collect(Collectors.toList());
        
        FileUtil.writeLines(tsvFile, tsvLines);
    }
    
    /**
     * Create CSV export with custom formatting
     */
    public static void exportDataWithCustomFormat(Path outputFile, String[] headers, 
                                                 List<String[]> data, String delimiter) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(delimiter, headers));
        
        for (String[] row : data) {
            String[] escapedRow = Arrays.stream(row)
                    .map(CsvProcessor::escapeCsvValue)
                    .toArray(String[]::new);
            lines.add(String.join(delimiter, escapedRow));
        }
        
        FileUtil.writeLines(outputFile, lines);
    }
    
    // Inner classes
    
    public static class CsvStatistics {
        private final int totalLines;
        private final int headerLines;
        private final int dataLines;
        private final int emptyLines;
        
        public CsvStatistics(int totalLines, int headerLines, int dataLines, int emptyLines) {
            this.totalLines = totalLines;
            this.headerLines = headerLines;
            this.dataLines = dataLines;
            this.emptyLines = emptyLines;
        }
        
        // Getters
        public int getTotalLines() { return totalLines; }
        public int getHeaderLines() { return headerLines; }
        public int getDataLines() { return dataLines; }
        public int getEmptyLines() { return emptyLines; }
        
        @Override
        public String toString() {
            return String.format("CsvStatistics{total=%d, headers=%d, data=%d, empty=%d}",
                               totalLines, headerLines, dataLines, emptyLines);
        }
    }
    
    /**
     * CSV validation result
     */
    public static class CsvValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public CsvValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
    
    /**
     * Comprehensive CSV validation
     */
    public static CsvValidationResult validateCsvFile(Path csvFile, String[] expectedHeaders, 
                                                     int expectedMinColumns) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (!Files.exists(csvFile)) {
            errors.add("CSV file does not exist: " + csvFile);
            return new CsvValidationResult(false, errors, warnings);
        }
        
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        
        if (lines.isEmpty()) {
            errors.add("CSV file is empty");
            return new CsvValidationResult(false, errors, warnings);
        }
        
        // Validate headers
        if (!validateHeaders(csvFile, expectedHeaders)) {
            errors.add("Invalid CSV headers. Expected: " + Arrays.toString(expectedHeaders));
        }
        
        // Validate data lines
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                warnings.add("Empty line at row " + (i + 1));
                continue;
            }
            
            String[] values = parseCsvLine(line);
            if (values.length < expectedMinColumns) {
                errors.add("Insufficient columns at row " + (i + 1) + 
                          ". Expected at least " + expectedMinColumns + ", found " + values.length);
            }
        }
        
        return new CsvValidationResult(errors.isEmpty(), errors, warnings);
    }
}