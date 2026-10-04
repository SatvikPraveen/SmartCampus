// File location: src/main/java/patterns/AdapterService.java

package patterns;

import interfaces.Reportable.ReportType;
import models.*;
import services.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * Adapter pattern implementation for integrating external systems and legacy components
 * Provides adapters for different data formats, external APIs, and legacy systems
 */
public class AdapterService {
    
    /**
     * Apply a single "First Last" display name to a user's first and last name fields
     */
    private static void applyFullName(User user, String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        user.setFirstName(parts[0]);
        if (parts.length > 1) {
            user.setLastName(parts[1]);
        }
    }
    
    /**
     * Adapter for integrating with legacy student information systems
     */
    public static class LegacyStudentAdapter {
        
        private final LegacyStudentSystem legacySystem;
        private final StudentService modernService;
        
        public LegacyStudentAdapter(LegacyStudentSystem legacySystem, StudentService modernService) {
            this.legacySystem = legacySystem;
            this.modernService = modernService;
        }
        
        public Student createStudent(Student student) {
            // Convert modern Student to legacy format
            LegacyStudentRecord legacyRecord = convertToLegacyFormat(student);
            
            // Use legacy system to create
            LegacyStudentRecord created = legacySystem.addStudent(legacyRecord);
            
            // Convert back to modern format
            return convertFromLegacyFormat(created);
        }
        
        public Student updateStudent(Student student) {
            LegacyStudentRecord legacyRecord = convertToLegacyFormat(student);
            LegacyStudentRecord updated = legacySystem.updateStudent(legacyRecord);
            return convertFromLegacyFormat(updated);
        }
        
        public void deleteStudent(String studentId) {
            legacySystem.removeStudent(studentId);
        }
        
        public Student getStudentById(String id) {
            LegacyStudentRecord record = legacySystem.findStudent(id);
            return record != null ? convertFromLegacyFormat(record) : null;
        }
        
        public List<Student> getAllStudents() {
            List<LegacyStudentRecord> legacyRecords = legacySystem.getAllStudents();
            return legacyRecords.stream()
                .map(this::convertFromLegacyFormat)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        public List<Student> getStudentsByDepartment(Department department) {
            List<LegacyStudentRecord> records = legacySystem.getStudentsByDept(department.getDepartmentId());
            return records.stream()
                .map(this::convertFromLegacyFormat)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        private LegacyStudentRecord convertToLegacyFormat(Student student) {
            LegacyStudentRecord record = new LegacyStudentRecord();
            record.setStudentId(student.getStudentId());
            record.setFullName(student.getFullName());
            record.setEmailAddress(student.getEmail());
            record.setDeptCode(student.getDepartmentId() != null ? student.getDepartmentId() : "");
            record.setEnrollDate(student.getEnrollmentDate());
            return record;
        }
        
        private Student convertFromLegacyFormat(LegacyStudentRecord record) {
            Student student = new Student();
            student.setUserId(record.getStudentId());
            student.setStudentId(record.getStudentId());
            applyFullName(student, record.getFullName());
            student.setEmail(record.getEmailAddress());
            if (record.getDeptCode() != null && !record.getDeptCode().isEmpty()) {
                student.setDepartmentId(record.getDeptCode());
            }
            if (record.getEnrollDate() != null) {
                student.setEnrollmentDate(record.getEnrollDate());
            }
            return student;
        }
    }
    
    /**
     * Adapter for external grade reporting systems
     */
    public static class ExternalGradeReportAdapter {
        
        private final ExternalReportingAPI externalAPI;
        private final ReportService internalReportService;
        
        public ExternalGradeReportAdapter(ExternalReportingAPI externalAPI, ReportService internalReportService) {
            this.externalAPI = externalAPI;
            this.internalReportService = internalReportService;
        }
        
        public String generateStudentTranscript(Student student) {
            // Get internal grade report data
            String internalTranscript = internalReportService
                .generateReport(ReportType.GRADE_REPORT, Map.of("studentId", student.getStudentId()))
                .getContent();
            
            // Convert to external format
            ExternalTranscriptRequest request = new ExternalTranscriptRequest();
            request.setStudentId(student.getStudentId());
            request.setStudentName(student.getFullName());
            request.setInternalData(internalTranscript);
            
            // Submit to external system
            ExternalTranscriptResponse response = externalAPI.generateTranscript(request);
            
            return response.getFormattedTranscript();
        }
        
        public String generateCourseReport(Course course) {
            String internalReport = internalReportService
                .generateReport(ReportType.ENROLLMENT_REPORT, Map.of("courseId", course.getCourseId()))
                .getContent();
            
            ExternalCourseReportRequest request = new ExternalCourseReportRequest();
            request.setCourseCode(course.getCourseCode());
            request.setCourseName(course.getCourseName());
            request.setInternalData(internalReport);
            
            ExternalCourseReportResponse response = externalAPI.generateCourseReport(request);
            
            return response.getFormattedReport();
        }
        
        public Map<String, Object> getUniversityStatistics() {
            return internalReportService.getSummaryStatistics();
        }
    }
    
    /**
     * Adapter for different data format conversions
     */
    public static class DataFormatAdapter {
        
        /**
         * CSV to JSON adapter for student data
         */
        public static class CsvToJsonStudentAdapter {
            
            public List<Map<String, Object>> convertStudentCsvToJson(List<String> csvLines) {
                List<Map<String, Object>> jsonData = new ArrayList<>();
                
                if (csvLines.isEmpty()) {
                    return jsonData;
                }
                
                // Parse header
                String[] headers = parseCsvLine(csvLines.get(0));
                
                // Parse data lines
                for (int i = 1; i < csvLines.size(); i++) {
                    String[] values = parseCsvLine(csvLines.get(i));
                    Map<String, Object> record = new HashMap<>();
                    
                    for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                        record.put(headers[j], values[j]);
                    }
                    
                    jsonData.add(record);
                }
                
                return jsonData;
            }
            
            public List<String> convertStudentJsonToCsv(List<Map<String, Object>> jsonData) {
                List<String> csvLines = new ArrayList<>();
                
                if (jsonData.isEmpty()) {
                    return csvLines;
                }
                
                // Extract headers from first record
                Set<String> headerSet = jsonData.get(0).keySet();
                String[] headers = headerSet.toArray(new String[0]);
                csvLines.add(String.join(",", headers));
                
                // Convert each record
                for (Map<String, Object> record : jsonData) {
                    String[] values = new String[headers.length];
                    for (int i = 0; i < headers.length; i++) {
                        Object value = record.get(headers[i]);
                        values[i] = value != null ? value.toString() : "";
                    }
                    csvLines.add(String.join(",", values));
                }
                
                return csvLines;
            }
            
            private String[] parseCsvLine(String line) {
                return line.split(",", -1); // keep trailing empty fields
            }
        }
        
        /**
         * XML to Object adapter for course data
         */
        public static class XmlToCourseAdapter {
            
            public Course convertXmlToCourse(String xmlData) {
                // Simplified XML parsing - in real implementation, use proper XML parser
                Map<String, String> data = parseSimpleXml(xmlData);
                
                Course course = new Course(
                    data.get("courseId"),
                    data.get("courseCode"),
                    data.get("name"),
                    data.get("description"),
                    Integer.parseInt(data.getOrDefault("credits", "3")),
                    emptyToNull(data.get("departmentId"))
                );
                course.setProfessorId(emptyToNull(data.get("professorId")));
                course.setSemester(emptyToNull(data.get("semester")));
                if (data.containsKey("year")) {
                    course.setYear(Integer.parseInt(data.get("year")));
                }
                course.setMaxEnrollment(Integer.parseInt(data.getOrDefault("capacity", "30")));
                return course;
            }
            
            public String convertCourseToXml(Course course) {
                StringBuilder xml = new StringBuilder();
                xml.append("<course>");
                xml.append("<courseId>").append(course.getCourseId()).append("</courseId>");
                xml.append("<courseCode>").append(course.getCourseCode()).append("</courseCode>");
                xml.append("<name>").append(escapeXml(course.getCourseName())).append("</name>");
                xml.append("<description>").append(escapeXml(course.getDescription())).append("</description>");
                xml.append("<credits>").append(course.getCredits()).append("</credits>");
                xml.append("<departmentId>").append(
                    course.getDepartmentId() != null ? course.getDepartmentId() : ""
                ).append("</departmentId>");
                xml.append("<professorId>").append(
                    course.getProfessorId() != null ? course.getProfessorId() : ""
                ).append("</professorId>");
                xml.append("<semester>").append(
                    course.getSemester() != null ? escapeXml(course.getSemester()) : ""
                ).append("</semester>");
                xml.append("<year>").append(course.getYear()).append("</year>");
                xml.append("<capacity>").append(course.getMaxEnrollment()).append("</capacity>");
                xml.append("<enrolled>").append(course.getEnrolledStudentIds().size()).append("</enrolled>");
                xml.append("</course>");
                return xml.toString();
            }
            
            private Map<String, String> parseSimpleXml(String xml) {
                Map<String, String> data = new HashMap<>();
                // Simplified XML parsing - extract the text of every leaf element <tag>value</tag>,
                // whether elements are on separate lines or all on one line (as convertCourseToXml emits)
                java.util.regex.Matcher m = LEAF_ELEMENT.matcher(xml);
                while (m.find()) {
                    data.put(m.group(1), unescapeXml(m.group(2).trim()));
                }
                return data;
            }

            private static final java.util.regex.Pattern LEAF_ELEMENT =
                java.util.regex.Pattern.compile("<(\\w+)>([^<]*)</\\1>");

            private String unescapeXml(String text) {
                return text.replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&quot;", "\"")
                          .replace("&apos;", "'")
                          .replace("&amp;", "&");
            }
            
            private String escapeXml(String text) {
                if (text == null) return "";
                return text.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;")
                          .replace("\"", "&quot;")
                          .replace("'", "&apos;");
            }
            
            private String emptyToNull(String value) {
                return value != null && !value.isEmpty() ? value : null;
            }
        }
    }
    
    /**
     * Adapter for third-party authentication systems
     */
    public static class ExternalAuthAdapter {
        
        private final ExternalAuthProvider externalProvider;
        private final AuthService internalAuthService;
        private final Map<String, User> externalUserCache;
        private AuthService.AuthenticationResult internalAuthResult;
        
        public ExternalAuthAdapter(ExternalAuthProvider externalProvider, AuthService internalAuthService) {
            this.externalProvider = externalProvider;
            this.internalAuthService = internalAuthService;
            this.externalUserCache = new HashMap<>();
        }
        
        public boolean authenticate(String username, String password) {
            // Try external authentication first
            ExternalAuthResult result = externalProvider.authenticate(username, password);
            
            if (result.isSuccessful()) {
                // Cache user information
                User user = convertExternalUser(result.getUserInfo());
                externalUserCache.put(username, user);
                return true;
            }
            
            // Fall back to internal authentication
            AuthService.AuthenticationResult internalResult = internalAuthService.authenticate(username, password);
            if (internalResult.isSuccess()) {
                internalAuthResult = internalResult;
                return true;
            }
            return false;
        }
        
        public User getCurrentUser() {
            String currentUsername = externalProvider.getCurrentUsername();
            if (currentUsername != null && externalUserCache.containsKey(currentUsername)) {
                return externalUserCache.get(currentUsername);
            }
            return internalAuthResult != null ? internalAuthResult.getUser() : null;
        }
        
        public boolean hasPermission(User user, String resource, AuthService.PermissionLevel permission) {
            // Check external permissions first
            if (externalUserCache.containsValue(user)) {
                ExternalPermissionResult result = externalProvider.checkPermission(
                    user.getEmail(), resource + ":" + permission.name());
                if (result.hasPermission()) {
                    return true;
                }
            }
            
            // Fall back to internal permission check
            return internalAuthService.hasPermission(user.getUserId(), resource, permission);
        }
        
        public void logout() {
            externalProvider.logout();
            if (internalAuthResult != null && internalAuthResult.getSession() != null) {
                internalAuthService.logout(internalAuthResult.getSession().getSessionToken());
            }
            internalAuthResult = null;
            externalUserCache.clear();
        }
        
        public boolean changePassword(String oldPassword, String newPassword) {
            // External systems typically don't allow password changes through this interface
            User currentUser = getCurrentUser();
            return currentUser != null &&
                   internalAuthService.changePassword(currentUser.getUserId(), oldPassword, newPassword);
        }
        
        private User convertExternalUser(ExternalUserInfo userInfo) {
            // Convert external user format to internal User format
            if (userInfo.getUserType().equals("STUDENT")) {
                Student student = new Student();
                student.setStudentId(userInfo.getId());
                populateUser(student, userInfo);
                return student;
            } else if (userInfo.getUserType().equals("FACULTY")) {
                // Department, rank and specialization are not available from the external system
                Professor professor = new Professor();
                professor.setProfessorId(userInfo.getId());
                populateUser(professor, userInfo);
                return professor;
            } else {
                // Default to creating a basic User object
                User user = new User() {
                    @Override
                    public String getRole() {
                        return userInfo.getUserType();
                    }
                    
                    @Override
                    public void displayInfo() {
                        System.out.println("User: " + getFullName() + " (" + getEmail() + ")");
                    }
                };
                populateUser(user, userInfo);
                return user;
            }
        }
        
        private void populateUser(User user, ExternalUserInfo userInfo) {
            user.setUserId(userInfo.getId());
            applyFullName(user, userInfo.getDisplayName());
            user.setEmail(userInfo.getEmail());
        }
    }
    
    /**
     * Adapter for different database systems
     */
    public static class DatabaseAdapter {
        
        private final String sourceDialect;
        private final String targetDialect;
        
        public DatabaseAdapter(String sourceDialect, String targetDialect) {
            this.sourceDialect = sourceDialect;
            this.targetDialect = targetDialect;
        }
        
        /**
         * Adapt SQL queries between different database dialects
         */
        public String adaptQuery(String originalQuery) {
            String adaptedQuery = originalQuery;
            
            // Convert from MySQL to PostgreSQL
            if ("mysql".equalsIgnoreCase(sourceDialect) && "postgresql".equalsIgnoreCase(targetDialect)) {
                adaptedQuery = adaptedQuery.replace("AUTO_INCREMENT", "SERIAL");
                adaptedQuery = adaptedQuery.replace("TINYINT", "BOOLEAN");
                adaptedQuery = adaptedQuery.replace("DATETIME", "TIMESTAMP");
                adaptedQuery = adaptedQuery.replace("LIMIT ?", "LIMIT ?");
            }
            
            // Convert from PostgreSQL to MySQL
            if ("postgresql".equalsIgnoreCase(sourceDialect) && "mysql".equalsIgnoreCase(targetDialect)) {
                adaptedQuery = adaptedQuery.replace("SERIAL", "AUTO_INCREMENT");
                adaptedQuery = adaptedQuery.replace("BOOLEAN", "TINYINT");
                adaptedQuery = adaptedQuery.replace("TIMESTAMP", "DATETIME");
            }
            
            // Convert from Oracle to MySQL
            if ("oracle".equalsIgnoreCase(sourceDialect) && "mysql".equalsIgnoreCase(targetDialect)) {
                adaptedQuery = adaptedQuery.replace("NUMBER", "INT");
                adaptedQuery = adaptedQuery.replace("VARCHAR2", "VARCHAR");
                adaptedQuery = adaptedQuery.replace("SYSDATE", "NOW()");
            }
            
            return adaptedQuery;
        }
        
        /**
         * Adapt data types between database systems
         */
        public String adaptDataType(String originalType) {
            Map<String, Map<String, String>> typeMap = createDataTypeMapping();
            
            Map<String, String> targetMapping = typeMap.get(
                (sourceDialect + "_to_" + targetDialect).toLowerCase(Locale.ROOT));
            return targetMapping != null ? targetMapping.getOrDefault(originalType.toUpperCase(), originalType) : originalType;
        }
        
        private Map<String, Map<String, String>> createDataTypeMapping() {
            Map<String, Map<String, String>> typeMap = new HashMap<>();
            
            // MySQL to PostgreSQL
            Map<String, String> mysqlToPostgres = new HashMap<>();
            mysqlToPostgres.put("TINYINT", "BOOLEAN");
            mysqlToPostgres.put("DATETIME", "TIMESTAMP");
            mysqlToPostgres.put("TEXT", "TEXT");
            typeMap.put("mysql_to_postgresql", mysqlToPostgres);
            
            // PostgreSQL to MySQL
            Map<String, String> postgrestoMysql = new HashMap<>();
            postgrestoMysql.put("BOOLEAN", "TINYINT");
            postgrestoMysql.put("TIMESTAMP", "DATETIME");
            typeMap.put("postgresql_to_mysql", postgrestoMysql);
            
            return typeMap;
        }
    }
    
    // Mock classes representing external systems and APIs
    
    public static class LegacyStudentSystem {
        private final Map<String, LegacyStudentRecord> students = new HashMap<>();
        
        public LegacyStudentRecord addStudent(LegacyStudentRecord record) {
            students.put(record.getStudentId(), record);
            return record;
        }
        
        public LegacyStudentRecord updateStudent(LegacyStudentRecord record) {
            students.put(record.getStudentId(), record);
            return record;
        }
        
        public void removeStudent(String studentId) {
            students.remove(studentId);
        }
        
        public LegacyStudentRecord findStudent(String studentId) {
            return students.get(studentId);
        }
        
        public List<LegacyStudentRecord> getAllStudents() {
            return new ArrayList<>(students.values());
        }
        
        public List<LegacyStudentRecord> getStudentsByDept(String deptCode) {
            return students.values().stream()
                .filter(record -> deptCode.equals(record.getDeptCode()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }
    
    public static class LegacyStudentRecord {
        private String studentId;
        private String fullName;
        private String emailAddress;
        private String deptCode;
        private LocalDate enrollDate;
        
        // Getters and setters
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
        
        public String getDeptCode() { return deptCode; }
        public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
        
        public LocalDate getEnrollDate() { return enrollDate; }
        public void setEnrollDate(LocalDate enrollDate) { this.enrollDate = enrollDate; }
    }
    
    // External API mock classes
    public interface ExternalReportingAPI {
        ExternalTranscriptResponse generateTranscript(ExternalTranscriptRequest request);
        ExternalCourseReportResponse generateCourseReport(ExternalCourseReportRequest request);
    }
    
    public static class ExternalTranscriptRequest {
        private String studentId;
        private String studentName;
        private String internalData;
        
        // Getters and setters
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        
        public String getInternalData() { return internalData; }
        public void setInternalData(String internalData) { this.internalData = internalData; }
    }
    
    public static class ExternalTranscriptResponse {
        private String formattedTranscript;
        
        public ExternalTranscriptResponse(String formattedTranscript) {
            this.formattedTranscript = formattedTranscript;
        }
        
        public String getFormattedTranscript() { return formattedTranscript; }
    }
    
    public static class ExternalCourseReportRequest {
        private String courseCode;
        private String courseName;
        private String internalData;
        
        // Getters and setters
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public String getInternalData() { return internalData; }
        public void setInternalData(String internalData) { this.internalData = internalData; }
    }
    
    public static class ExternalCourseReportResponse {
        private String formattedReport;
        
        public ExternalCourseReportResponse(String formattedReport) {
            this.formattedReport = formattedReport;
        }
        
        public String getFormattedReport() { return formattedReport; }
    }
    
    // External authentication system mock classes
    public interface ExternalAuthProvider {
        ExternalAuthResult authenticate(String username, String password);
        ExternalPermissionResult checkPermission(String userEmail, String permission);
        String getCurrentUsername();
        void logout();
    }
    
    public static class ExternalAuthResult {
        private final boolean successful;
        private final ExternalUserInfo userInfo;
        
        public ExternalAuthResult(boolean successful, ExternalUserInfo userInfo) {
            this.successful = successful;
            this.userInfo = userInfo;
        }
        
        public boolean isSuccessful() { return successful; }
        public ExternalUserInfo getUserInfo() { return userInfo; }
    }
    
    public static class ExternalUserInfo {
        private String id;
        private String displayName;
        private String email;
        private String userType;
        
        public ExternalUserInfo(String id, String displayName, String email, String userType) {
            this.id = id;
            this.displayName = displayName;
            this.email = email;
            this.userType = userType;
        }
        
        // Getters
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getEmail() { return email; }
        public String getUserType() { return userType; }
    }
    
    public static class ExternalPermissionResult {
        private final boolean hasPermission;
        private final String reason;
        
        public ExternalPermissionResult(boolean hasPermission, String reason) {
            this.hasPermission = hasPermission;
            this.reason = reason;
        }
        
        public boolean hasPermission() { return hasPermission; }
        public String getReason() { return reason; }
    }
}