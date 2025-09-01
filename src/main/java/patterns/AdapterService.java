// File location: src/main/java/patterns/AdapterService.java

package patterns;

import models.*;
import services.*;
import java.util.*;
import java.util.function.Function;

/**
 * Adapter pattern implementation for integrating external systems and legacy components
 * Provides adapters for different data formats, external APIs, and legacy systems
 */
public class AdapterService {
    
    /**
     * Adapter for integrating with legacy student information systems
     */
    public static class LegacyStudentAdapter implements StudentService {
        
        private final LegacyStudentSystem legacySystem;
        private final StudentService modernService;
        
        public LegacyStudentAdapter(LegacyStudentSystem legacySystem, StudentService modernService) {
            this.legacySystem = legacySystem;
            this.modernService = modernService;
        }
        
        @Override
        public Student createStudent(Student student) {
            // Convert modern Student to legacy format
            LegacyStudentRecord legacyRecord = convertToLegacyFormat(student);
            
            // Use legacy system to create
            LegacyStudentRecord created = legacySystem.addStudent(legacyRecord);
            
            // Convert back to modern format
            return convertFromLegacyFormat(created);
        }
        
        @Override
        public Student updateStudent(Student student) {
            LegacyStudentRecord legacyRecord = convertToLegacyFormat(student);
            LegacyStudentRecord updated = legacySystem.updateStudent(legacyRecord);
            return convertFromLegacyFormat(updated);
        }
        
        @Override
        public void deleteStudent(String studentId) {
            legacySystem.removeStudent(studentId);
        }
        
        @Override
        public Student getStudentById(String id) {
            LegacyStudentRecord record = legacySystem.findStudent(id);
            return record != null ? convertFromLegacyFormat(record) : null;
        }
        
        @Override
        public List<Student> getAllStudents() {
            List<LegacyStudentRecord> legacyRecords = legacySystem.getAllStudents();
            return legacyRecords.stream()
                .map(this::convertFromLegacyFormat)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        @Override
        public List<Student> getStudentsByDepartment(Department department) {
            // Legacy system uses department codes
            List<LegacyStudentRecord> records = legacySystem.getStudentsByDept(department.getDepartmentCode());
            return records.stream()
                .map(this::convertFromLegacyFormat)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        private LegacyStudentRecord convertToLegacyFormat(Student student) {
            LegacyStudentRecord record = new LegacyStudentRecord();
            record.setStudentId(student.getId());
            record.setFullName(student.getName());
            record.setEmailAddress(student.getEmail());
            record.setDeptCode(student.getDepartment() != null ? student.getDepartment().getDepartmentCode() : "");
            record.setEnrollDate(student.getEnrollmentDate());
            return record;
        }
        
        private Student convertFromLegacyFormat(LegacyStudentRecord record) {
            // Create a minimal Department object for the conversion
            Department dept = record.getDeptCode() != null && !record.getDeptCode().isEmpty() ?
                new Department(record.getDeptCode(), "", "", "", 0, 0) : null;
            
            return new Student(
                record.getStudentId(),
                record.getFullName(),
                record.getEmailAddress(),
                dept,
                record.getEnrollDate()
            );
        }
    }
    
    /**
     * Adapter for external grade reporting systems
     */
    public static class ExternalGradeReportAdapter implements ReportService {
        
        private final ExternalReportingAPI externalAPI;
        private final ReportService internalReportService;
        
        public ExternalGradeReportAdapter(ExternalReportingAPI externalAPI, ReportService internalReportService) {
            this.externalAPI = externalAPI;
            this.internalReportService = internalReportService;
        }
        
        @Override
        public String generateStudentTranscript(Student student) {
            // Get internal transcript
            String internalTranscript = internalReportService.generateStudentTranscript(student);
            
            // Convert to external format
            ExternalTranscriptRequest request = new ExternalTranscriptRequest();
            request.setStudentId(student.getId());
            request.setStudentName(student.getName());
            request.setInternalData(internalTranscript);
            
            // Submit to external system
            ExternalTranscriptResponse response = externalAPI.generateTranscript(request);
            
            return response.getFormattedTranscript();
        }
        
        @Override
        public String generateCourseReport(Course course) {
            String internalReport = internalReportService.generateCourseReport(course);
            
            ExternalCourseReportRequest request = new ExternalCourseReportRequest();
            request.setCourseCode(course.getCourseCode());
            request.setCourseName(course.getName());
            request.setInternalData(internalReport);
            
            ExternalCourseReportResponse response = externalAPI.generateCourseReport(request);
            
            return response.getFormattedReport();
        }
        
        @Override
        public String generateDepartmentReport(Department department) {
            // Delegate to internal service since external API doesn't support department reports
            return internalReportService.generateDepartmentReport(department);
        }
        
        @Override
        public Map<String, Object> getUniversityStatistics() {
            return internalReportService.getUniversityStatistics();
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
                return line.split(",");
            }
        }
        
        /**
         * XML to Object adapter for course data
         */
        public static class XmlToCourseAdapter {
            
            public Course convertXmlToCourse(String xmlData) {
                // Simplified XML parsing - in real implementation, use proper XML parser
                Map<String, String> data = parseSimpleXml(xmlData);
                
                return new Course(
                    data.get("courseCode"),
                    data.get("name"),
                    data.get("description"),
                    Integer.parseInt(data.getOrDefault("credits", "3")),
                    createDepartmentFromCode(data.get("departmentCode")),
                    createProfessorFromId(data.get("professorId")),
                    data.get("semester"),
                    data.get("academicYear"),
                    Integer.parseInt(data.getOrDefault("capacity", "30")),
                    Integer.parseInt(data.getOrDefault("enrolled", "0"))
                );
            }
            
            public String convertCourseToXml(Course course) {
                StringBuilder xml = new StringBuilder();
                xml.append("<course>");
                xml.append("<courseCode>").append(course.getCourseCode()).append("</courseCode>");
                xml.append("<name>").append(escapeXml(course.getName())).append("</name>");
                xml.append("<description>").append(escapeXml(course.getDescription())).append("</description>");
                xml.append("<credits>").append(course.getCredits()).append("</credits>");
                xml.append("<departmentCode>").append(
                    course.getDepartment() != null ? course.getDepartment().getDepartmentCode() : ""
                ).append("</departmentCode>");
                xml.append("<professorId>").append(
                    course.getProfessor() != null ? course.getProfessor().getId() : ""
                ).append("</professorId>");
                xml.append("<semester>").append(course.getSemester()).append("</semester>");
                xml.append("<academicYear>").append(course.getAcademicYear()).append("</academicYear>");
                xml.append("<capacity>").append(course.getCapacity()).append("</capacity>");
                xml.append("<enrolled>").append(course.getEnrolledStudents()).append("</enrolled>");
                xml.append("</course>");
                return xml.toString();
            }
            
            private Map<String, String> parseSimpleXml(String xml) {
                Map<String, String> data = new HashMap<>();
                // Simplified XML parsing - extract text between tags
                String[] lines = xml.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("<") && line.endsWith(">") && !line.startsWith("</")) {
                        int tagEnd = line.indexOf('>');
                        int closeTagStart = line.lastIndexOf('<');
                        if (tagEnd > 0 && closeTagStart > tagEnd) {
                            String tagName = line.substring(1, tagEnd);
                            String value = line.substring(tagEnd + 1, closeTagStart);
                            data.put(tagName, value);
                        }
                    }
                }
                return data;
            }
            
            private String escapeXml(String text) {
                if (text == null) return "";
                return text.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;")
                          .replace("\"", "&quot;")
                          .replace("'", "&apos;");
            }
            
            private Department createDepartmentFromCode(String code) {
                return code != null && !code.isEmpty() ? 
                    new Department(code, "", "", "", 0, 0) : null;
            }
            
            private Professor createProfessorFromId(String id) {
                return id != null && !id.isEmpty() ? 
                    new Professor(id, "", "", null, "", "", 0) : null;
            }
        }
    }
    
    /**
     * Adapter for third-party authentication systems
     */
    public static class ExternalAuthAdapter implements AuthService {
        
        private final ExternalAuthProvider externalProvider;
        private final AuthService internalAuthService;
        private final Map<String, User> externalUserCache;
        
        public ExternalAuthAdapter(ExternalAuthProvider externalProvider, AuthService internalAuthService) {
            this.externalProvider = externalProvider;
            this.internalAuthService = internalAuthService;
            this.externalUserCache = new HashMap<>();
        }
        
        @Override
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
            return internalAuthService.authenticate(username, password);
        }
        
        @Override
        public User getCurrentUser() {
            String currentUsername = externalProvider.getCurrentUsername();
            if (currentUsername != null && externalUserCache.containsKey(currentUsername)) {
                return externalUserCache.get(currentUsername);
            }
            return internalAuthService.getCurrentUser();
        }
        
        @Override
        public boolean hasPermission(User user, String permission) {
            // Check external permissions first
            if (externalUserCache.containsValue(user)) {
                ExternalPermissionResult result = externalProvider.checkPermission(
                    user.getEmail(), permission);
                if (result.hasPermission()) {
                    return true;
                }
            }
            
            // Fall back to internal permission check
            return internalAuthService.hasPermission(user, permission);
        }
        
        @Override
        public void logout() {
            externalProvider.logout();
            internalAuthService.logout();
            externalUserCache.clear();
        }
        
        @Override
        public boolean changePassword(String oldPassword, String newPassword) {
            // External systems typically don't allow password changes through this interface
            return internalAuthService.changePassword(oldPassword, newPassword);
        }
        
        private User convertExternalUser(ExternalUserInfo userInfo) {
            // Convert external user format to internal User format
            if (userInfo.getUserType().equals("STUDENT")) {
                return new Student(
                    userInfo.getId(),
                    userInfo.getDisplayName(),
                    userInfo.getEmail(),
                    null, // Department would need to be looked up separately
                    new Date()
                );
            } else if (userInfo.getUserType().equals("FACULTY")) {
                return new Professor(
                    userInfo.getId(),
                    userInfo.getDisplayName(),
                    userInfo.getEmail(),
                    null, // Department would need to be looked up separately
                    "",   // Specialization not available from external system
                    "",   // Office location not available
                    0     // Years of experience not available
                );
            } else {
                // Default to creating a basic User object
                return new User(userInfo.getId(), userInfo.getDisplayName(), userInfo.getEmail()) {
                    // Anonymous subclass for basic user
                };
            }
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
            
            Map<String, String> targetMapping = typeMap.get(sourceDialect + "_to_" + targetDialect);
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
        private Date enrollDate;
        
        // Getters and setters
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
        
        public String getDeptCode() { return deptCode; }
        public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
        
        public Date getEnrollDate() { return enrollDate; }
        public void setEnrollDate(Date enrollDate) { this.enrollDate = enrollDate; }
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