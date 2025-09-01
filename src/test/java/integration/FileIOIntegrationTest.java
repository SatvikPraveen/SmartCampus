// File location: src/test/java/integration/FileIOIntegrationTest.java

package com.smartcampus.test.integration;

import com.smartcampus.services.FileIOService;
import com.smartcampus.services.DataImportService;
import com.smartcampus.services.ReportGenerationService;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.Department;
import com.smartcampus.models.dto.ImportResult;
import com.smartcampus.models.enums.ReportFormat;
import com.smartcampus.config.TestFileConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Integration tests for file I/O operations
 * Tests CSV import/export, report generation, and file handling
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(classes = TestFileConfig.class)
@ActiveProfiles("test")
@DisplayName("File I/O Integration Tests")
class FileIOIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Autowired
    private FileIOService fileIOService;

    @Autowired
    private DataImportService dataImportService;

    @Autowired
    private ReportGenerationService reportGenerationService;

    private Path testFilesDirectory;
    private List<Student> testStudents;
    private List<Course> testCourses;
    private Department testDepartment;

    @BeforeEach
    void setUp() throws IOException {
        testFilesDirectory = tempDirectory.resolve("test-files");
        Files.createDirectories(testFilesDirectory);

        // Create test data
        testDepartment = new Department();
        testDepartment.setId(1L);
        testDepartment.setCode("CS");
        testDepartment.setName("Computer Science");

        testStudents = createTestStudents();
        testCourses = createTestCourses();
        
        // Create test CSV files
        createTestStudentCsvFile();
        createTestCoursesCsvFile();
        createTestEnrollmentsCsvFile();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up any remaining test files
        if (Files.exists(testFilesDirectory)) {
            Files.walk(testFilesDirectory)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Nested
    @DisplayName("CSV Import Tests")
    class CsvImportTests {

        @Test
        @DisplayName("Should import students from CSV file")
        void shouldImportStudentsFromCsvFile() throws IOException {
            // Arrange
            Path studentCsvFile = testFilesDirectory.resolve("students.csv");
            assertTrue(Files.exists(studentCsvFile));

            // Act
            ImportResult result = dataImportService.importStudentsFromCsv(studentCsvFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getTotalRecords()).isEqualTo(3);
            assertThat(result.getSuccessfulImports()).isEqualTo(3);
            assertThat(result.getFailedImports()).isEqualTo(0);
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should handle invalid CSV data gracefully")
        void shouldHandleInvalidCsvDataGracefully() throws IOException {
            // Arrange
            Path invalidCsvFile = createInvalidStudentCsvFile();

            // Act
            ImportResult result = dataImportService.importStudentsFromCsv(invalidCsvFile);

            // Assert
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getTotalRecords()).isEqualTo(4);
            assertThat(result.getSuccessfulImports()).isEqualTo(2); // 2 valid records
            assertThat(result.getFailedImports()).isEqualTo(2); // 2 invalid records
            assertThat(result.getErrors()).hasSize(2);
        }

        @Test
        @DisplayName("Should import courses from CSV file")
        void shouldImportCoursesFromCsvFile() throws IOException {
            // Arrange
            Path coursesCsvFile = testFilesDirectory.resolve("courses.csv");

            // Act
            ImportResult result = dataImportService.importCoursesFromCsv(coursesCsvFile);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getTotalRecords()).isEqualTo(2);
            assertThat(result.getSuccessfulImports()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should import enrollments from CSV file")
        void shouldImportEnrollmentsFromCsvFile() throws IOException {
            // Arrange
            Path enrollmentsCsvFile = testFilesDirectory.resolve("enrollments.csv");

            // Act
            ImportResult result = dataImportService.importEnrollmentsFromCsv(enrollmentsCsvFile);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getTotalRecords()).isEqualTo(4);
            assertThat(result.getSuccessfulImports()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should handle large CSV files efficiently")
        void shouldHandleLargeCsvFilesEfficiently() throws IOException {
            // Arrange
            Path largeCsvFile = createLargeStudentCsvFile(10000);
            long startTime = System.currentTimeMillis();

            // Act
            ImportResult result = dataImportService.importStudentsFromCsvBatched(largeCsvFile, 500);
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getTotalRecords()).isEqualTo(10000);
            assertThat(duration).isLessThan(30000); // Should complete within 30 seconds
        }

        @Test
        @DisplayName("Should validate CSV headers")
        void shouldValidateCsvHeaders() throws IOException {
            // Arrange
            Path invalidHeadersCsvFile = createCsvFileWithInvalidHeaders();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                dataImportService.importStudentsFromCsv(invalidHeadersCsvFile);
            });
        }

        @Test
        @DisplayName("Should handle CSV files with different encodings")
        void shouldHandleCsvFilesWithDifferentEncodings() throws IOException {
            // Arrange
            Path utf8CsvFile = createCsvFileWithUtf8Encoding();
            Path iso88591CsvFile = createCsvFileWithIso88591Encoding();

            // Act
            ImportResult utf8Result = dataImportService.importStudentsFromCsv(utf8CsvFile);
            ImportResult isoResult = dataImportService.importStudentsFromCsvWithEncoding(iso88591CsvFile, "ISO-8859-1");

            // Assert
            assertThat(utf8Result.isSuccessful()).isTrue();
            assertThat(isoResult.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("Should handle multipart file upload")
        void shouldHandleMultipartFileUpload() throws IOException {
            // Arrange
            String csvContent = "student_id,first_name,last_name,email\n" +
                              "CS2024001,John,Doe,john.doe@test.com\n" +
                              "CS2024002,Jane,Smith,jane.smith@test.com";
            
            MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csvContent.getBytes()
            );

            // Act
            ImportResult result = dataImportService.importStudentsFromMultipartFile(mockFile);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getTotalRecords()).isEqualTo(2);
            assertThat(result.getSuccessfulImports()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("CSV Export Tests")
    class CsvExportTests {

        @Test
        @DisplayName("Should export students to CSV file")
        void shouldExportStudentsToCsvFile() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("exported-students.csv");

            // Act
            fileIOService.exportStudentsToCsv(testStudents, outputFile);

            // Assert
            assertTrue(Files.exists(outputFile));
            
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines).hasSize(4); // Header + 3 students
            assertThat(lines.get(0)).contains("student_id,first_name,last_name,email");
            assertThat(lines.get(1)).contains("CS2024001,John,Doe,john.doe@test.com");
        }

        @Test
        @DisplayName("Should export courses to CSV file")
        void shouldExportCoursesToCsvFile() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("exported-courses.csv");

            // Act
            fileIOService.exportCoursesToCsv(testCourses, outputFile);

            // Assert
            assertTrue(Files.exists(outputFile));
            
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines).hasSize(3); // Header + 2 courses
            assertThat(lines.get(0)).contains("course_code,course_name,credits");
        }

        @Test
        @DisplayName("Should handle empty data export")
        void shouldHandleEmptyDataExport() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("empty-export.csv");
            List<Student> emptyList = Arrays.asList();

            // Act
            fileIOService.exportStudentsToCsv(emptyList, outputFile);

            // Assert
            assertTrue(Files.exists(outputFile));
            
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines).hasSize(1); // Only header
        }

        @Test
        @DisplayName("Should export with custom CSV format")
        void shouldExportWithCustomCsvFormat() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("custom-format.csv");

            // Act
            fileIOService.exportStudentsToCsvWithCustomFormat(testStudents, outputFile, ';', '"');

            // Assert
            assertTrue(Files.exists(outputFile));
            
            String content = Files.readString(outputFile);
            assertThat(content).contains(";"); // Custom delimiter
            assertThat(content).contains("\""); // Custom quote character
        }

        @Test
        @DisplayName("Should export large datasets efficiently")
        void shouldExportLargeDatasetsEfficiently() throws IOException {
            // Arrange
            List<Student> largeDataset = createLargeStudentDataset(50000);
            Path outputFile = tempDirectory.resolve("large-export.csv");
            
            long startTime = System.currentTimeMillis();

            // Act
            fileIOService.exportStudentsToCsvStreaming(largeDataset, outputFile);
            
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertTrue(Files.exists(outputFile));
            assertThat(Files.size(outputFile)).isGreaterThan(1000000); // Should be substantial file
            assertThat(duration).isLessThan(10000); // Should complete within 10 seconds
        }
    }

    @Nested
    @DisplayName("Report Generation Tests")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should generate PDF student report")
        void shouldGeneratePdfStudentReport() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("student-report.pdf");

            // Act
            reportGenerationService.generateStudentReport(testStudents, outputFile, ReportFormat.PDF);

            // Assert
            assertTrue(Files.exists(outputFile));
            assertThat(Files.size(outputFile)).isGreaterThan(1000); // PDF should have some content
            
            // Verify PDF structure
            byte[] pdfBytes = Files.readAllBytes(outputFile);
            assertThat(new String(pdfBytes)).startsWith("%PDF"); // PDF header
        }

        @Test
        @DisplayName("Should generate Excel enrollment report")
        void shouldGenerateExcelEnrollmentReport() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("enrollment-report.xlsx");
            List<Enrollment> enrollments = createTestEnrollments();

            // Act
            reportGenerationService.generateEnrollmentReport(enrollments, outputFile, ReportFormat.EXCEL);

            // Assert
            assertTrue(Files.exists(outputFile));
            assertThat(Files.size(outputFile)).isGreaterThan(5000); // Excel file should have content
        }

        @Test
        @DisplayName("Should generate HTML course report")
        void shouldGenerateHtmlCourseReport() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("course-report.html");

            // Act
            reportGenerationService.generateCourseReport(testCourses, outputFile, ReportFormat.HTML);

            // Assert
            assertTrue(Files.exists(outputFile));
            
            String htmlContent = Files.readString(outputFile);
            assertThat(htmlContent).contains("<html>");
            assertThat(htmlContent).contains("CS101"); // Course code should be present
            assertThat(htmlContent).contains("Introduction to Programming"); // Course name
        }

        @Test
        @DisplayName("Should generate report with charts and graphs")
        void shouldGenerateReportWithChartsAndGraphs() throws IOException {
            // Arrange
            Path outputFile = tempDirectory.resolve("analytics-report.pdf");

            // Act
            reportGenerationService.generateAnalyticsReport(testStudents, testCourses, outputFile);

            // Assert
            assertTrue(Files.exists(outputFile));
            assertThat(Files.size(outputFile)).isGreaterThan(50000); // Report with charts should be larger
        }

        @Test
        @DisplayName("Should generate customized report template")
        void shouldGenerateCustomizedReportTemplate() throws IOException {
            // Arrange
            Path templateFile = createCustomReportTemplate();
            Path outputFile = tempDirectory.resolve("custom-report.pdf");

            // Act
            reportGenerationService.generateReportFromTemplate(testStudents, templateFile, outputFile);

            // Assert
            assertTrue(Files.exists(outputFile));
            assertThat(Files.size(outputFile)).isGreaterThan(1000);
        }
    }

    @Nested
    @DisplayName("File System Operations Tests")
    class FileSystemOperationsTests {

        @Test
        @DisplayName("Should create directory structure")
        void shouldCreateDirectoryStructure() throws IOException {
            // Arrange
            Path targetPath = tempDirectory.resolve("reports/2024/students");

            // Act
            fileIOService.createDirectoryStructure(targetPath);

            // Assert
            assertTrue(Files.exists(targetPath));
            assertTrue(Files.isDirectory(targetPath));
        }

        @Test
        @DisplayName("Should copy files between directories")
        void shouldCopyFilesBetweenDirectories() throws IOException {
            // Arrange
            Path sourceFile = testFilesDirectory.resolve("students.csv");
            Path targetDir = tempDirectory.resolve("backup");
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve("students-backup.csv");

            // Act
            fileIOService.copyFile(sourceFile, targetFile);

            // Assert
            assertTrue(Files.exists(targetFile));
            assertThat(Files.size(targetFile)).isEqualTo(Files.size(sourceFile));
        }

        @Test
        @DisplayName("Should move files between directories")
        void shouldMoveFilesBetweenDirectories() throws IOException {
            // Arrange
            Path sourceFile = tempDirectory.resolve("temp-file.txt");
            Files.writeString(sourceFile, "temporary content");
            Path targetFile = tempDirectory.resolve("moved-file.txt");

            // Act
            fileIOService.moveFile(sourceFile, targetFile);

            // Assert
            assertFalse(Files.exists(sourceFile));
            assertTrue(Files.exists(targetFile));
            assertThat(Files.readString(targetFile)).isEqualTo("temporary content");
        }

        @Test
        @DisplayName("Should delete files and directories safely")
        void shouldDeleteFilesAndDirectoriesSafely() throws IOException {
            // Arrange
            Path testDir = tempDirectory.resolve("to-delete");
            Files.createDirectories(testDir);
            Path testFile = testDir.resolve("test-file.txt");
            Files.writeString(testFile, "content");

            // Act
            fileIOService.deleteFileOrDirectory(testDir);

            // Assert
            assertFalse(Files.exists(testDir));
            assertFalse(Files.exists(testFile));
        }

        @Test
        @DisplayName("Should list files with filtering")
        void shouldListFilesWithFiltering() throws IOException {
            // Arrange
            Files.writeString(tempDirectory.resolve("file1.csv"), "csv content");
            Files.writeString(tempDirectory.resolve("file2.txt"), "txt content");
            Files.writeString(tempDirectory.resolve("file3.csv"), "csv content");

            // Act
            List<Path> csvFiles = fileIOService.listFilesWithExtension(tempDirectory, ".csv");

            // Assert
            assertThat(csvFiles).hasSize(2);
            assertThat(csvFiles).allMatch(path -> path.toString().endsWith(".csv"));
        }

        @Test
        @DisplayName("Should calculate directory size")
        void shouldCalculateDirectorySize() throws IOException {
            // Arrange
            Files.writeString(tempDirectory.resolve("file1.txt"), "Hello World!");
            Files.writeString(tempDirectory.resolve("file2.txt"), "Another file content");

            // Act
            long directorySize = fileIOService.calculateDirectorySize(tempDirectory);

            // Assert
            assertThat(directorySize).isGreaterThan(20); // Should be sum of file sizes
        }
    }

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("Should validate file format")
        void shouldValidateFileFormat() throws IOException {
            // Arrange
            Path csvFile = testFilesDirectory.resolve("students.csv");
            Path textFile = tempDirectory.resolve("not-csv.txt");
            Files.writeString(textFile, "This is not CSV content");

            // Act & Assert
            assertTrue(fileIOService.isValidCsvFile(csvFile));
            assertFalse(fileIOService.isValidCsvFile(textFile));
        }

        @Test
        @DisplayName("Should validate file size limits")
        void shouldValidateFileSizeLimits() throws IOException {
            // Arrange
            Path smallFile = tempDirectory.resolve("small.csv");
            Path largeFile = tempDirectory.resolve("large.csv");
            
            Files.writeString(smallFile, "header\nrow1");
            
            // Create large file
            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 100000; i++) {
                largeContent.append("row").append(i).append("\n");
            }
            Files.writeString(largeFile, largeContent.toString());

            // Act & Assert
            assertTrue(fileIOService.isWithinSizeLimit(smallFile, 1024 * 1024)); // 1MB limit
            assertFalse(fileIOService.isWithinSizeLimit(largeFile, 1024)); // 1KB limit
        }

        @Test
        @DisplayName("Should validate CSV structure")
        void shouldValidateCsvStructure() throws IOException {
            // Arrange
            Path validCsv = testFilesDirectory.resolve("students.csv");
            Path malformedCsv = createMalformedCsvFile();

            // Act & Assert
            assertTrue(fileIOService.hasValidCsvStructure(validCsv));
            assertFalse(fileIOService.hasValidCsvStructure(malformedCsv));
        }

        @Test
        @DisplayName("Should check file permissions")
        void shouldCheckFilePermissions() throws IOException {
            // Arrange
            Path readableFile = tempDirectory.resolve("readable.csv");
            Files.writeString(readableFile, "content");
            
            Path readOnlyFile = tempDirectory.resolve("readonly.csv");
            Files.writeString(readOnlyFile, "content");
            readOnlyFile.toFile().setReadOnly();

            // Act & Assert
            assertTrue(fileIOService.isReadable(readableFile));
            assertTrue(fileIOService.isReadable(readOnlyFile));
            assertTrue(fileIOService.isWritable(readableFile));
            assertFalse(fileIOService.isWritable(readOnlyFile));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle file not found errors")
        void shouldHandleFileNotFoundErrors() {
            // Arrange
            Path nonExistentFile = tempDirectory.resolve("does-not-exist.csv");

            // Act & Assert
            assertThrows(FileNotFoundException.class, () -> {
                dataImportService.importStudentsFromCsv(nonExistentFile);
            });
        }

        @Test
        @DisplayName("Should handle permission denied errors")
        void shouldHandlePermissionDeniedErrors() throws IOException {
            // Arrange
            Path restrictedDir = tempDirectory.resolve("restricted");
            Files.createDirectories(restrictedDir);
            restrictedDir.toFile().setReadOnly();
            
            Path targetFile = restrictedDir.resolve("cannot-write.csv");

            // Act & Assert
            assertThrows(IOException.class, () -> {
                fileIOService.exportStudentsToCsv(testStudents, targetFile);
            });
        }

        @Test
        @DisplayName("Should handle corrupted file data")
        void shouldHandleCorruptedFileData() throws IOException {
            // Arrange
            Path corruptedFile = tempDirectory.resolve("corrupted.csv");
            byte[] corruptedData = new byte[]{0x00, 0x01, 0x02, 0x03}; // Not valid text
            Files.write(corruptedFile, corruptedData);

            // Act
            ImportResult result = dataImportService.importStudentsFromCsv(corruptedFile);

            // Assert
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle disk space issues gracefully")
        void shouldHandleDiskSpaceIssuesGracefully() throws IOException {
            // This test would require a more complex setup to simulate disk space issues
            // For now, we'll test the error handling mechanism
            
            // Arrange
            Path outputFile = tempDirectory.resolve("large-output.csv");
            List<Student> veryLargeDataset = createLargeStudentDataset(1000000);

            // Act & Assert
            // In a real scenario with limited disk space, this would throw IOException
            assertDoesNotThrow(() -> {
                try {
                    fileIOService.exportStudentsToCsvWithSpaceCheck(veryLargeDataset, outputFile);
                } catch (IOException e) {
                    // Expected in real low-disk scenarios
                    assertThat(e.getMessage()).contains("disk space");
                }
            });
        }
    }

    // Helper methods for creating test data and files
    private List<Student> createTestStudents() {
        Student student1 = new Student();
        student1.setId(1L);
        student1.setStudentId("CS2024001");
        student1.setFirstName("John");
        student1.setLastName("Doe");
        student1.setEmail("john.doe@test.com");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setStudentId("CS2024002");
        student2.setFirstName("Jane");
        student2.setLastName("Smith");
        student2.setEmail("jane.smith@test.com");

        Student student3 = new Student();
        student3.setId(3L);
        student3.setStudentId("CS2024003");
        student3.setFirstName("Bob");
        student3.setLastName("Johnson");
        student3.setEmail("bob.johnson@test.com");

        return Arrays.asList(student1, student2, student3);
    }

    private List<Course> createTestCourses() {
        Course course1 = new Course();
        course1.setId(1L);
        course1.setCourseCode("CS101");
        course1.setCourseName("Introduction to Programming");
        course1.setCredits(3);

        Course course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("CS201");
        course2.setCourseName("Data Structures");
        course2.setCredits(4);

        return Arrays.asList(course1, course2);
    }

    private List<Enrollment> createTestEnrollments() {
        List<Enrollment> enrollments = new ArrayList<>();
        
        for (Student student : testStudents) {
            for (Course course : testCourses) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(course);
                enrollment.setEnrollmentDate(LocalDate.now().minusDays(30));
                enrollments.add(enrollment);
            }
        }
        
        return enrollments;
    }

    private void createTestStudentCsvFile() throws IOException {
        String csvContent = """
            student_id,first_name,last_name,email,department_id,admission_date,status,year_level,gpa
            CS2024001,John,Doe,john.doe@test.com,1,2024-08-15,ACTIVE,1,3.50
            CS2024002,Jane,Smith,jane.smith@test.com,1,2024-08-15,ACTIVE,1,3.75
            CS2024003,Bob,Johnson,bob.johnson@test.com,1,2024-08-15,ACTIVE,2,3.25
            """;
        
        Files.writeString(testFilesDirectory.resolve("students.csv"), csvContent);
    }

    private void createTestCoursesCsvFile() throws IOException {
        String csvContent = """
            course_code,course_name,credits,department_id,professor_id,semester,year,max_enrollment
            CS101,Introduction to Programming,3,1,1,FALL,2024,30
            CS201,Data Structures,4,1,1,FALL,2024,25
            """;
        
        Files.writeString(testFilesDirectory.resolve("courses.csv"), csvContent);
    }

    private void createTestEnrollmentsCsvFile() throws IOException {
        String csvContent = """
            student_id,course_id,enrollment_date,status,attendance_percentage,participation_score
            1,1,2024-08-26,ENROLLED,95.0,88.5
            2,1,2024-08-26,ENROLLED,92.0,90.0
            3,2,2024-08-26,ENROLLED,88.0,85.0
            1,2,2024-08-26,ENROLLED,90.0,87.5
            """;
        
        Files.writeString(testFilesDirectory.resolve("enrollments.csv"), csvContent);
    }

    private Path createInvalidStudentCsvFile() throws IOException {
        String csvContent = """
            student_id,first_name,last_name,email,department_id,admission_date,status,year_level,gpa
            CS2024001,John,Doe,john.doe@test.com,1,2024-08-15,ACTIVE,1,3.50
            CS2024002,Jane,Smith,invalid-email,1,2024-08-15,ACTIVE,1,3.75
            ,Bob,Johnson,bob.johnson@test.com,1,2024-08-15,ACTIVE,2,3.25
            CS2024004,Alice,Wilson,alice.wilson@test.com,1,invalid-date,ACTIVE,1,5.00
            """;
        
        Path invalidFile = tempDirectory.resolve("invalid-students.csv");
        Files.writeString(invalidFile, csvContent);
        return invalidFile;
    }

    private Path createLargeStudentCsvFile(int recordCount) throws IOException {
        Path largeFile = tempDirectory.resolve("large-students.csv");
        
        try (BufferedWriter writer = Files.newBufferedWriter(largeFile)) {
            writer.write("student_id,first_name,last_name,email,department_id,admission_date,status,year_level,gpa\n");
            
            for (int i = 1; i <= recordCount; i++) {
                writer.write(String.format(
                    "CS%07d,Student%d,LastName%d,student%d@test.com,1,2024-08-15,ACTIVE,1,%.2f\n",
                    i, i, i, i, 2.0 + (i % 2)
                ));
            }
        }
        
        return largeFile;
    }

    private List<Student> createLargeStudentDataset(int size) {
        List<Student> students = new ArrayList<>();
        
        for (int i = 1; i <= size; i++) {
            Student student = new Student();
            student.setId((long) i);
            student.setStudentId(String.format("CS%07d", i));
            student.setFirstName("Student" + i);
            student.setLastName("LastName" + i);
            student.setEmail(String.format("student%d@test.com", i));
            students.add(student);
        }
        
        return students;
    }

    private Path createCsvFileWithInvalidHeaders() throws IOException {
        String csvContent = """
            wrong_header1,wrong_header2,wrong_header3
            value1,value2,value3
            """;
        
        Path invalidFile = tempDirectory.resolve("invalid-headers.csv");
        Files.writeString(invalidFile, csvContent);
        return invalidFile;
    }

    private Path createCsvFileWithUtf8Encoding() throws IOException {
        String csvContent = """
            student_id,first_name,last_name,email
            CS2024001,José,García,jose.garcia@test.com
            CS2024002,François,Müller,francois.muller@test.com
            """;
        
        Path utf8File = tempDirectory.resolve("utf8-students.csv");
        Files.writeString(utf8File, csvContent, java.nio.charset.StandardCharsets.UTF_8);
        return utf8File;
    }

    private Path createCsvFileWithIso88591Encoding() throws IOException {
        String csvContent = """
            student_id,first_name,last_name,email
            CS2024001,José,García,jose.garcia@test.com
            CS2024002,François,Müller,francois.muller@test.com
            """;
        
        Path isoFile = tempDirectory.resolve("iso-students.csv");
        Files.writeString(isoFile, csvContent, java.nio.charset.Charset.forName("ISO-8859-1"));
        return isoFile;
    }

    private Path createMalformedCsvFile() throws IOException {
        String malformedContent = """
            student_id,first_name,last_name,email
            CS2024001,"John,Doe",john.doe@test.com
            CS2024002,Jane,"Smith,jane.smith@test.com
            CS2024003,Bob,Johnson,bob.johnson@test.com,extra,columns
            """;
        
        Path malformedFile = tempDirectory.resolve("malformed.csv");
        Files.writeString(malformedFile, malformedContent);
        return malformedFile;
    }

    private Path createCustomReportTemplate() throws IOException {
        String templateContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Student Report Template</title>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .header { background-color: #f0f0f0; padding: 10px; }
                    .content { margin: 20px; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Student Report</h1>
                    <p>Generated on: {{CURRENT_DATE}}</p>
                </div>
                <div class="content">
                    <h2>Student Information</h2>
                    {{STUDENT_TABLE}}
                </div>
            </body>
            </html>
            """;
        
        Path templateFile = tempDirectory.resolve("student-report-template.html");
        Files.writeString(templateFile, templateContent);
        return templateFile;
    }
}