// File location: src/main/java/io/JsonProcessor.java

package io;

import models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Handles JSON processing for the SmartCampus system
 * Provides serialization and deserialization capabilities for all entity types
 */
public class JsonProcessor {
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Serialize object to JSON string
     */
    public static <T> String toJson(T object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Serialize object to JSON file
     */
    public static <T> void writeToJsonFile(T object, Path jsonFile) throws IOException {
        FileUtil.createDirectoriesIfNotExists(jsonFile.getParent());
        objectMapper.writeValue(jsonFile.toFile(), object);
    }
    
    /**
     * Deserialize JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }
    
    /**
     * Deserialize JSON string to object with TypeReference
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) throws IOException {
        return objectMapper.readValue(json, typeRef);
    }
    
    /**
     * Deserialize JSON file to object
     */
    public static <T> T readFromJsonFile(Path jsonFile, Class<T> clazz) throws IOException {
        if (!Files.exists(jsonFile)) {
            throw new FileNotFoundException("JSON file not found: " + jsonFile);
        }
        return objectMapper.readValue(jsonFile.toFile(), clazz);
    }
    
    /**
     * Deserialize JSON file to object with TypeReference
     */
    public static <T> T readFromJsonFile(Path jsonFile, TypeReference<T> typeRef) throws IOException {
        if (!Files.exists(jsonFile)) {
            throw new FileNotFoundException("JSON file not found: " + jsonFile);
        }
        return objectMapper.readValue(jsonFile.toFile(), typeRef);
    }
    
    /**
     * Read students from JSON file
     */
    public static List<Student> readStudents(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, new TypeReference<List<Student>>() {});
    }
    
    /**
     * Write students to JSON file
     */
    public static void writeStudents(List<Student> students, Path jsonFile) throws IOException {
        writeToJsonFile(students, jsonFile);
    }
    
    /**
     * Read professors from JSON file
     */
    public static List<Professor> readProfessors(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, new TypeReference<List<Professor>>() {});
    }
    
    /**
     * Write professors to JSON file
     */
    public static void writeProfessors(List<Professor> professors, Path jsonFile) throws IOException {
        writeToJsonFile(professors, jsonFile);
    }
    
    /**
     * Read courses from JSON file
     */
    public static List<Course> readCourses(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, new TypeReference<List<Course>>() {});
    }
    
    /**
     * Write courses to JSON file
     */
    public static void writeCourses(List<Course> courses, Path jsonFile) throws IOException {
        writeToJsonFile(courses, jsonFile);
    }
    
    /**
     * Read departments from JSON file
     */
    public static List<Department> readDepartments(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, new TypeReference<List<Department>>() {});
    }
    
    /**
     * Write departments to JSON file
     */
    public static void writeDepartments(List<Department> departments, Path jsonFile) throws IOException {
        writeToJsonFile(departments, jsonFile);
    }
    
    /**
     * Read enrollments from JSON file
     */
    public static List<Enrollment> readEnrollments(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, new TypeReference<List<Enrollment>>() {});
    }
    
    /**
     * Write enrollments to JSON file
     */
    public static void writeEnrollments(List<Enrollment> enrollments, Path jsonFile) throws IOException {
        writeToJsonFile(enrollments, jsonFile);
    }
    
    /**
     * Read grades from JSON file
     */
    public static List<Grade> readGrades(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, new TypeReference<List<Grade>>() {});
    }
    
    /**
     * Write grades to JSON file
     */
    public static void writeGrades(List<Grade> grades, Path jsonFile) throws IOException {
        writeToJsonFile(grades, jsonFile);
    }
    
    /**
     * Create a complete university data export in JSON format
     */
    public static void exportUniversityData(Path jsonFile, List<Student> students,
                                          List<Professor> professors, List<Course> courses,
                                          List<Department> departments, List<Enrollment> enrollments,
                                          List<Grade> grades) throws IOException {
        
        UniversityDataExport export = new UniversityDataExport();
        export.setStudents(students);
        export.setProfessors(professors);
        export.setCourses(courses);
        export.setDepartments(departments);
        export.setEnrollments(enrollments);
        export.setGrades(grades);
        export.setExportDate(new Date());
        export.setVersion("1.0");
        
        writeToJsonFile(export, jsonFile);
    }
    
    /**
     * Import complete university data from JSON format
     */
    public static UniversityDataExport importUniversityData(Path jsonFile) throws IOException {
        return readFromJsonFile(jsonFile, UniversityDataExport.class);
    }
    
    /**
     * Convert object to pretty JSON string
     */
    public static <T> String toPrettyJson(T object) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }
    
    /**
     * Convert object to compact JSON string
     */
    public static <T> String toCompactJson(T object) throws IOException {
        ObjectMapper compactMapper = new ObjectMapper();
        compactMapper.registerModule(new JavaTimeModule());
        compactMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        compactMapper.disable(SerializationFeature.INDENT_OUTPUT);
        return compactMapper.writeValueAsString(object);
    }
    
    /**
     * Parse JSON and extract specific field values
     */
    public static List<String> extractFieldValues(Path jsonFile, String fieldPath) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonFile.toFile());
        List<String> values = new ArrayList<>();
        extractFieldValuesRecursive(rootNode, fieldPath, values);
        return values;
    }
    
    /**
     * Recursively extract field values from JSON node
     */
    private static void extractFieldValuesRecursive(JsonNode node, String fieldPath, List<String> values) {
        if (node.isArray()) {
            for (JsonNode arrayItem : node) {
                extractFieldValuesRecursive(arrayItem, fieldPath, values);
            }
        } else if (node.isObject()) {
            JsonNode fieldNode = node.at("/" + fieldPath.replace(".", "/"));
            if (!fieldNode.isMissingNode()) {
                values.add(fieldNode.asText());
            }
            
            // Also check direct children
            node.fields().forEachRemaining(entry -> {
                extractFieldValuesRecursive(entry.getValue(), fieldPath, values);
            });
        }
    }
    
    /**
     * Validate JSON file structure
     */
    public static JsonValidationResult validateJsonFile(Path jsonFile, Class<?> expectedClass) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            if (!Files.exists(jsonFile)) {
                errors.add("JSON file does not exist: " + jsonFile);
                return new JsonValidationResult(false, errors, warnings);
            }
            
            // Try to parse the JSON
            JsonNode rootNode = objectMapper.readTree(jsonFile.toFile());
            
            if (rootNode == null) {
                errors.add("JSON file is empty or invalid");
                return new JsonValidationResult(false, errors, warnings);
            }
            
            // Try to deserialize to expected class
            objectMapper.readValue(jsonFile.toFile(), expectedClass);
            
            // Additional validations can be added here
            
        } catch (IOException e) {
            errors.add("JSON parsing error: " + e.getMessage());
        } catch (Exception e) {
            errors.add("JSON validation error: " + e.getMessage());
        }
        
        return new JsonValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Merge multiple JSON files containing arrays
     */
    public static void mergeJsonArrayFiles(List<Path> jsonFiles, Path outputFile) throws IOException {
        List<JsonNode> allArrays = new ArrayList<>();
        
        for (Path jsonFile : jsonFiles) {
            if (Files.exists(jsonFile)) {
                JsonNode arrayNode = objectMapper.readTree(jsonFile.toFile());
                if (arrayNode.isArray()) {
                    allArrays.add(arrayNode);
                }
            }
        }
        
        // Merge all arrays
        List<JsonNode> mergedItems = new ArrayList<>();
        for (JsonNode array : allArrays) {
            for (JsonNode item : array) {
                mergedItems.add(item);
            }
        }
        
        objectMapper.writeValue(outputFile.toFile(), mergedItems);
    }
    
    /**
     * Filter JSON array based on field criteria
     */
    public static void filterJsonArray(Path inputFile, Path outputFile, 
                                     String fieldPath, String expectedValue) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputFile.toFile());
        
        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("Input JSON must be an array");
        }
        
        List<JsonNode> filteredItems = new ArrayList<>();
        
        for (JsonNode item : rootNode) {
            JsonNode fieldNode = item.at("/" + fieldPath.replace(".", "/"));
            if (!fieldNode.isMissingNode() && expectedValue.equals(fieldNode.asText())) {
                filteredItems.add(item);
            }
        }
        
        objectMapper.writeValue(outputFile.toFile(), filteredItems);
    }
    
    /**
     * Transform JSON structure using custom mapping
     */
    public static <T, R> void transformJsonArray(Path inputFile, Path outputFile,
                                               Class<T> inputClass, Class<R> outputClass,
                                               Function<T, R> transformer) throws IOException {
        
        List<T> inputObjects = readFromJsonFile(inputFile, 
            TypeFactory.defaultInstance().constructCollectionType(List.class, inputClass));
        
        List<R> outputObjects = inputObjects.stream()
                .map(transformer)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        writeToJsonFile(outputObjects, outputFile);
    }
    
    /**
     * Create JSON schema-like structure for validation
     */
    public static void generateJsonSchema(Class<?> clazz, Path schemaFile) throws IOException {
        // This is a simplified schema generation
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("class", clazz.getSimpleName());
        schema.put("properties", analyzeClassStructure(clazz));
        
        writeToJsonFile(schema, schemaFile);
    }
    
    /**
     * Analyze class structure for schema generation
     */
    private static Map<String, Object> analyzeClassStructure(Class<?> clazz) {
        Map<String, Object> properties = new HashMap<>();
        
        Arrays.stream(clazz.getDeclaredFields())
              .forEach(field -> {
                  Map<String, Object> fieldInfo = new HashMap<>();
                  fieldInfo.put("type", field.getType().getSimpleName());
                  fieldInfo.put("required", true); // Simplified
                  properties.put(field.getName(), fieldInfo);
              });
        
        return properties;
    }
    
    /**
     * Create backup of JSON data with compression
     */
    public static void createJsonBackup(List<Path> jsonFiles, Path backupFile) throws IOException {
        Map<String, Object> backup = new HashMap<>();
        backup.put("backupDate", new Date());
        backup.put("version", "1.0");
        
        Map<String, Object> files = new HashMap<>();
        for (Path jsonFile : jsonFiles) {
            if (Files.exists(jsonFile)) {
                String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
                files.put(jsonFile.getFileName().toString(), content);
            }
        }
        backup.put("files", files);
        
        writeToJsonFile(backup, backupFile);
    }
    
    /**
     * Restore JSON data from backup
     */
    public static void restoreJsonBackup(Path backupFile, Path restoreDirectory) throws IOException {
        JsonNode backupNode = objectMapper.readTree(backupFile.toFile());
        JsonNode filesNode = backupNode.get("files");
        
        if (filesNode != null && filesNode.isObject()) {
            filesNode.fields().forEachRemaining(entry -> {
                try {
                    String fileName = entry.getKey();
                    String content = entry.getValue().asText();
                    Path outputFile = restoreDirectory.resolve(fileName);
                    Files.writeString(outputFile, content, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to restore file: " + entry.getKey(), e);
                }
            });
        }
    }
    
    // Inner classes
    
    public static class UniversityDataExport {
        private List<Student> students;
        private List<Professor> professors;
        private List<Course> courses;
        private List<Department> departments;
        private List<Enrollment> enrollments;
        private List<Grade> grades;
        private Date exportDate;
        private String version;
        
        // Getters and setters
        public List<Student> getStudents() { return students; }
        public void setStudents(List<Student> students) { this.students = students; }
        
        public List<Professor> getProfessors() { return professors; }
        public void setProfessors(List<Professor> professors) { this.professors = professors; }
        
        public List<Course> getCourses() { return courses; }
        public void setCourses(List<Course> courses) { this.courses = courses; }
        
        public List<Department> getDepartments() { return departments; }
        public void setDepartments(List<Department> departments) { this.departments = departments; }
        
        public List<Enrollment> getEnrollments() { return enrollments; }
        public void setEnrollments(List<Enrollment> enrollments) { this.enrollments = enrollments; }
        
        public List<Grade> getGrades() { return grades; }
        public void setGrades(List<Grade> grades) { this.grades = grades; }
        
        public Date getExportDate() { return exportDate; }
        public void setExportDate(Date exportDate) { this.exportDate = exportDate; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
    
    public static class JsonValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public JsonValidationResult(boolean valid, List<String> errors, List<String> warnings) {
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
        
        @Override
        public String toString() {
            return String.format("JsonValidationResult{valid=%s, errors=%d, warnings=%d}",
                               valid, errors.size(), warnings.size());
        }
    }
    
    /**
     * Custom JSON serialization configuration
     */
    public static class JsonConfig {
        private boolean prettyPrint = true;
        private boolean includeNulls = false;
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";
        
        public boolean isPrettyPrint() { return prettyPrint; }
        public void setPrettyPrint(boolean prettyPrint) { this.prettyPrint = prettyPrint; }
        
        public boolean isIncludeNulls() { return includeNulls; }
        public void setIncludeNulls(boolean includeNulls) { this.includeNulls = includeNulls; }
        
        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
        
        public ObjectMapper createMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            if (prettyPrint) {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
            } else {
                mapper.disable(SerializationFeature.INDENT_OUTPUT);
            }
            
            if (!includeNulls) {
                mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            }
            
            if (dateFormat != null) {
                mapper.setDateFormat(new java.text.SimpleDateFormat(dateFormat));
            }
            
            return mapper;
        }
    }
}