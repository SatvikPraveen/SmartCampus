// File: src/main/java/app/App.java
package app;

import models.*;
import models.Student.AcademicYear;
import models.Professor.AcademicRank;
import models.Professor.EmploymentStatus;
import models.Course.CourseStatus;
import models.Course.DifficultyLevel;
import exceptions.InvalidInputException;
import exceptions.UserNotFoundException;
import utils.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Main application class for SmartCampus University Management System.
 * 
 * Key Java concepts covered:
 * - Main method and application entry point
 * - Exception handling (try-catch blocks)
 * - Scanner for user input
 * - Object instantiation and method calls
 * - Polymorphism in action
 * - Static methods
 * - Array and List usage
 * - String formatting
 * - Method organization and separation of concerns
 */
public class App {
    
    // Application constants
    private static final String APP_NAME = "SmartCampus University Management System";
    private static final String VERSION = "1.0.0";
    private static final String SEPARATOR = "=".repeat(60);
    
    // Scanner for user input (static to share across methods)
    private static Scanner scanner = new Scanner(System.in);
    
    /**
     * Main method - entry point of the application.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println(SEPARATOR);
        System.out.println("Welcome to " + APP_NAME + " v" + VERSION);
        System.out.println(SEPARATOR);
        
        try {
            // Initialize application
            initializeApplication();
            
            // Demonstrate core functionality
            demonstrateFeatures();
            
            // Run interactive menu
            runInteractiveMenu();
            
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (scanner != null) {
                scanner.close();
            }
            System.out.println("\nThank you for using " + APP_NAME + "!");
        }
    }
    
    /**
     * Initialize the application with sample data.
     */
    private static void initializeApplication() {
        System.out.println("Initializing SmartCampus system...");
        
        // This is where we would typically load data from files or database
        // For now, we'll demonstrate with sample data creation
        
        System.out.println("✓ System components loaded");
        System.out.println("✓ Validation utilities ready");
        System.out.println("✓ Exception handling configured");
        System.out.println("✓ Models initialized");
        System.out.println();
    }
    
    /**
     * Demonstrate the core features of the system.
     */
    private static void demonstrateFeatures() {
        System.out.println("=== FEATURE DEMONSTRATION ===\n");
        
        try {
            // Demonstrate User hierarchy and polymorphism
            demonstratePolymorphism();
            
            // Demonstrate validation
            demonstrateValidation();
            
            // Demonstrate exception handling
            demonstrateExceptionHandling();
            
            // Demonstrate business logic
            demonstrateBusinessLogic();
            
        } catch (Exception e) {
            System.err.println("Error during demonstration: " + e.getMessage());
        }
        
        System.out.println("\n" + SEPARATOR);
    }
    
    /**
     * Demonstrate polymorphism with User hierarchy.
     */
    private static void demonstratePolymorphism() {
        System.out.println("🔹 POLYMORPHISM DEMONSTRATION\n");
        
        // Create different types of users
        Student student = new Student("USR001", "Alice", "Johnson", "alice@university.edu", 
                                    "(555) 123-4567", "STU123456", "Computer Science", AcademicYear.SOPHOMORE);
        
        Professor professor = new Professor("USR002", "Dr. Robert", "Smith", "robert.smith@university.edu",
                                          "(555) 987-6543", "PROF12345", "DEPT_CS", AcademicRank.ASSOCIATE, "Machine Learning");
        
        // Store in User array (polymorphism)
        User[] users = {student, professor};
        
        // Demonstrate polymorphic behavior
        for (User user : users) {
            System.out.println("Processing user: " + user.getFullName());
            System.out.println("Role: " + user.getRole()); // Polymorphic method call
            user.displayInfo(); // Polymorphic method call
            System.out.println();
        }
    }
    
    /**
     * Demonstrate validation utilities.
     */
    private static void demonstrateValidation() {
        System.out.println("🔹 VALIDATION DEMONSTRATION\n");
        
        // Test email validation
        String[] emails = {"valid@email.com", "invalid-email", "test@domain.org"};
        for (String email : emails) {
            boolean isValid = ValidationUtil.isValidEmail(email);
            System.out.printf("Email '%s' is %s%n", email, isValid ? "VALID" : "INVALID");
        }
        
        // Test phone number validation
        String[] phones = {"(555) 123-4567", "555-123-4567", "invalid-phone"};
        for (String phone : phones) {
            boolean isValid = ValidationUtil.isValidPhoneNumber(phone);
            System.out.printf("Phone '%s' is %s%n", phone, isValid ? "VALID" : "INVALID");
        }
        
        // Test ID validations
        System.out.println("\nID Validations:");
        System.out.println("Student ID 'STU123456': " + ValidationUtil.isValidStudentId("STU123456"));
        System.out.println("Professor ID 'PROF12345': " + ValidationUtil.isValidProfessorId("PROF12345"));
        System.out.println("Course Code 'CS101': " + ValidationUtil.isValidCourseCode("CS101"));
        System.out.println("Department Code 'CS': " + ValidationUtil.isValidDepartmentCode("CS"));
        System.out.println();
    }
    
    /**
     * Demonstrate exception handling.
     */
    private static void demonstrateExceptionHandling() {
        System.out.println("🔹 EXCEPTION HANDLING DEMONSTRATION\n");
        
        // Demonstrate InvalidInputException
        try {
            Student invalidStudent = new Student();
            invalidStudent.setEmail("invalid-email"); // This should throw an exception
        } catch (InvalidInputException e) {
            System.out.println("Caught InvalidInputException: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IllegalArgumentException: " + e.getMessage());
        }
        
        // Demonstrate UserNotFoundException
        try {
            // Simulate user search that fails
            throw UserNotFoundException.student("STU999999");
        } catch (UserNotFoundException e) {
            System.out.println("Caught UserNotFoundException: " + e.getMessage());
            System.out.println("User-friendly message: " + e.getUserFriendlyMessage());
        }
        
        // Demonstrate validation exception
        try {
            ValidationUtil.validateRange(-1.5, 0.0, 4.0, "GPA");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught validation error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate business logic with course enrollment.
     */
    private static void demonstrateBusinessLogic() {
        System.out.println("🔹 BUSINESS LOGIC DEMONSTRATION\n");
        
        // Create a course
        Course javaCourse = new Course("CRS001", "CS101", "Introduction to Java Programming",
                                     "Learn Java fundamentals and OOP concepts", 3, "DEPT_CS");
        javaCourse.setProfessorId("PROF12345");
        javaCourse.setDifficultyLevel(DifficultyLevel.BEGINNER);
        javaCourse.setSemester("Fall");
        javaCourse.setYear(2024);
        javaCourse.setMaxEnrollment(25);
        javaCourse.setSchedule(Arrays.asList("Monday", "Wednesday", "Friday"), 
                              LocalTime.of(10, 0), LocalTime.of(11, 30));
        javaCourse.setClassroom("Room 101");
        javaCourse.setBuilding("Computer Science Building");
        
        // Add prerequisites and resources
        javaCourse.addPrerequisite("CS100");
        javaCourse.addTextbook("Java: The Complete Reference by Herbert Schildt");
        javaCourse.addOnlineResource("Oracle Java Documentation");
        javaCourse.addGradingComponent("Midterm Exam", 25.0);
        javaCourse.addGradingComponent("Final Exam", 35.0);
        javaCourse.addGradingComponent("Assignments", 30.0);
        javaCourse.addGradingComponent("Participation", 10.0);
        
        // Open enrollment
        javaCourse.openEnrollment();
        
        System.out.println("Created course: " + javaCourse.getCourseName());
        System.out.println("Course status: " + javaCourse.getStatus());
        System.out.println("Available seats: " + javaCourse.getAvailableSeats());
        
        // Create students and enroll them
        String[] studentIds = {"STU123456", "STU123457", "STU123458"};
        for (String studentId : studentIds) {
            boolean enrolled = javaCourse.enrollStudent(studentId);
            System.out.printf("Student %s enrollment: %s%n", studentId, enrolled ? "SUCCESS" : "FAILED");
        }
        
        System.out.println("Enrolled students: " + javaCourse.getEnrolledStudentIds().size());
        System.out.println("Remaining seats: " + javaCourse.getAvailableSeats());
        System.out.println("Enrollment percentage: " + String.format("%.1f%%", javaCourse.getEnrollmentPercentage()));
        
        // Display full course information
        System.out.println("\n--- Course Details ---");
        javaCourse.displayCourseInfo();
        
        // Create and demonstrate department
        System.out.println("\n🔹 DEPARTMENT MANAGEMENT\n");
        
        Department csDepartment = new Department("DEPT_CS", "CS", "Computer Science Department",
                                               "Leading computer science education and research", "Building A, Floor 3");
        csDepartment.setHeadOfDepartmentId("PROF12345");
        csDepartment.setEmail("cs@university.edu");
        csDepartment.setPhoneNumber("(555) 100-2000");
        csDepartment.setAnnualBudget(new BigDecimal("500000"));
        csDepartment.setEstablishedYear("1985");
        csDepartment.setAccreditation("ABET");
        
        // Add programs and courses
        csDepartment.addMajorProgram("Computer Science");
        csDepartment.addMajorProgram("Software Engineering");
        csDepartment.addMinorProgram("Data Science");
        csDepartment.addGraduateProgram("Master of Computer Science");
        
        csDepartment.addCourse("CRS001");
        csDepartment.addProfessor("PROF12345");
        
        // Add students to department
        for (String studentId : studentIds) {
            csDepartment.addStudent(studentId, "Sophomore");
        }
        
        // Add facilities and equipment
        csDepartment.addFacility("Computer Lab A");
        csDepartment.addFacility("Computer Lab B");
        csDepartment.addFacility("Research Lab");
        csDepartment.addEquipment("Dell Computers (30)", "Computer Lab A");
        csDepartment.addEquipment("Network Servers", "Research Lab");
        csDepartment.addEquipment("3D Printers", "Research Lab");
        
        // Allocate some budget
        csDepartment.allocateBudget(new BigDecimal("50000"), "New equipment");
        csDepartment.allocateBudget(new BigDecimal("25000"), "Software licenses");
        
        System.out.println("Department health status: " + csDepartment.getDepartmentHealthStatus());
        System.out.println("Student-to-faculty ratio: " + String.format("%.1f:1", csDepartment.getStudentToFacultyRatio()));
        
        // Display department information
        System.out.println("\n--- Department Details ---");
        csDepartment.displayDepartmentInfo();
        
        System.out.println();
    }
    
    /**
     * Run the interactive menu system.
     */
    private static void runInteractiveMenu() {
        System.out.println("=== INTERACTIVE MENU ===\n");
        
        boolean running = true;
        while (running) {
            displayMainMenu();
            
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                
                switch (choice) {
                    case 1:
                        createStudentInteractive();
                        break;
                    case 2:
                        createProfessorInteractive();
                        break;
                    case 3:
                        createCourseInteractive();
                        break;
                    case 4:
                        createDepartmentInteractive();
                        break;
                    case 5:
                        demonstrateValidationInteractive();
                        break;
                    case 6:
                        displaySystemInfo();
                        break;
                    case 0:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.\n");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.\n");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage() + "\n");
            }
        }
    }
    
    /**
     * Display the main menu options.
     */
    private static void displayMainMenu() {
        System.out.println("Choose an option:");
        System.out.println("1. Create Student");
        System.out.println("2. Create Professor");
        System.out.println("3. Create Course");
        System.out.println("4. Create Department");
        System.out.println("5. Test Validation");
        System.out.println("6. System Information");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }
    
    /**
     * Interactive student creation.
     */
    private static void createStudentInteractive() {
        System.out.println("\n--- Create Student ---");
        
        try {
            System.out.print("Enter first name: ");
            String firstName = scanner.nextLine().trim();
            
            System.out.print("Enter last name: ");
            String lastName = scanner.nextLine().trim();
            
            System.out.print("Enter email: ");
            String email = scanner.nextLine().trim();
            
            System.out.print("Enter phone number (optional): ");
            String phone = scanner.nextLine().trim();
            if (phone.isEmpty()) phone = null;
            
            System.out.print("Enter student ID (STU######): ");
            String studentId = scanner.nextLine().trim();
            
            System.out.print("Enter major: ");
            String major = scanner.nextLine().trim();
            
            System.out.println("Select academic year:");
            AcademicYear[] years = AcademicYear.values();
            for (int i = 0; i < years.length; i++) {
                System.out.println((i + 1) + ". " + years[i]);
            }
            System.out.print("Enter choice (1-" + years.length + "): ");
            int yearChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            
            if (yearChoice < 0 || yearChoice >= years.length) {
                throw new InvalidInputException("Invalid academic year choice");
            }
            
            // Validate inputs
            ValidationUtil.requireNonBlank(firstName, "First name");
            ValidationUtil.requireNonBlank(lastName, "Last name");
            if (!ValidationUtil.isValidEmail(email)) {
                throw InvalidInputException.invalidEmail(email);
            }
            if (phone != null && !ValidationUtil.isValidPhoneNumber(phone)) {
                throw InvalidInputException.invalidPhoneNumber(phone);
            }
            if (!ValidationUtil.isValidStudentId(studentId)) {
                throw InvalidInputException.invalidFormat("studentId", studentId, "STU followed by 6 digits");
            }
            
            // Create student
            String userId = "USR_" + System.currentTimeMillis();
            Student student = new Student(userId, firstName, lastName, email, phone, 
                                        studentId, major, years[yearChoice]);
            
            System.out.println("\n✓ Student created successfully!");
            student.displayInfo();
            
        } catch (Exception e) {
            System.out.println("Error creating student: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Interactive professor creation.
     */
    private static void createProfessorInteractive() {
        System.out.println("\n--- Create Professor ---");
        
        try {
            System.out.print("Enter first name: ");
            String firstName = scanner.nextLine().trim();
            
            System.out.print("Enter last name: ");
            String lastName = scanner.nextLine().trim();
            
            System.out.print("Enter email: ");
            String email = scanner.nextLine().trim();
            
            System.out.print("Enter phone number: ");
            String phone = scanner.nextLine().trim();
            
            System.out.print("Enter professor ID (PROF#####): ");
            String professorId = scanner.nextLine().trim();
            
            System.out.print("Enter department ID: ");
            String departmentId = scanner.nextLine().trim();
            
            System.out.print("Enter specialization: ");
            String specialization = scanner.nextLine().trim();
            
            System.out.println("Select academic rank:");
            AcademicRank[] ranks = AcademicRank.values();
            for (int i = 0; i < ranks.length; i++) {
                System.out.println((i + 1) + ". " + ranks[i]);
            }
            System.out.print("Enter choice (1-" + ranks.length + "): ");
            int rankChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            
            if (rankChoice < 0 || rankChoice >= ranks.length) {
                throw new InvalidInputException("Invalid academic rank choice");
            }
            
            // Validate inputs
            ValidationUtil.requireNonBlank(firstName, "First name");
            ValidationUtil.requireNonBlank(lastName, "Last name");
            if (!ValidationUtil.isValidEmail(email)) {
                throw InvalidInputException.invalidEmail(email);
            }
            if (!ValidationUtil.isValidPhoneNumber(phone)) {
                throw InvalidInputException.invalidPhoneNumber(phone);
            }
            if (!ValidationUtil.isValidProfessorId(professorId)) {
                throw InvalidInputException.invalidFormat("professorId", professorId, "PROF followed by 5 digits");
            }
            
            // Create professor
            String userId = "USR_" + System.currentTimeMillis();
            Professor professor = new Professor(userId, firstName, lastName, email, phone,
                                              professorId, departmentId, ranks[rankChoice], specialization);
            
            System.out.println("\n✓ Professor created successfully!");
            professor.displayInfo();
            
        } catch (Exception e) {
            System.out.println("Error creating professor: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Interactive course creation.
     */
    private static void createCourseInteractive() {
        System.out.println("\n--- Create Course ---");
        
        try {
            System.out.print("Enter course code (e.g., CS101): ");
            String courseCode = scanner.nextLine().trim();
            
            System.out.print("Enter course name: ");
            String courseName = scanner.nextLine().trim();
            
            System.out.print("Enter description: ");
            String description = scanner.nextLine().trim();
            
            System.out.print("Enter credits (1-6): ");
            int credits = Integer.parseInt(scanner.nextLine().trim());
            
            System.out.print("Enter department ID: ");
            String departmentId = scanner.nextLine().trim();
            
            // Validate inputs
            if (!ValidationUtil.isValidCourseCode(courseCode)) {
                throw InvalidInputException.invalidFormat("courseCode", courseCode, "2-4 letters, 3 digits, optional letter");
            }
            ValidationUtil.requireNonBlank(courseName, "Course name");
            if (!ValidationUtil.isValidCredits(credits)) {
                throw InvalidInputException.outOfRange("credits", credits, 1, 6);
            }
            
            // Create course
            String courseId = "CRS_" + System.currentTimeMillis();
            Course course = new Course(courseId, courseCode, courseName, description, credits, departmentId);
            
            System.out.println("\n✓ Course created successfully!");
            course.displayCourseInfo();
            
        } catch (Exception e) {
            System.out.println("Error creating course: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Interactive department creation.
     */
    private static void createDepartmentInteractive() {
        System.out.println("\n--- Create Department ---");
        
        try {
            System.out.print("Enter department code (e.g., CS): ");
            String departmentCode = scanner.nextLine().trim();
            
            System.out.print("Enter department name: ");
            String departmentName = scanner.nextLine().trim();
            
            System.out.print("Enter description: ");
            String description = scanner.nextLine().trim();
            
            System.out.print("Enter location: ");
            String location = scanner.nextLine().trim();
            
            // Validate inputs
            if (!ValidationUtil.isValidDepartmentCode(departmentCode)) {
                throw InvalidInputException.invalidFormat("departmentCode", departmentCode, "2-6 uppercase letters");
            }
            ValidationUtil.requireNonBlank(departmentName, "Department name");
            
            // Create department
            String departmentId = "DEPT_" + departmentCode + "_" + System.currentTimeMillis();
            Department department = new Department(departmentId, departmentCode, departmentName, description, location);
            
            System.out.println("\n✓ Department created successfully!");
            department.displayDepartmentInfo();
            
        } catch (Exception e) {
            System.out.println("Error creating department: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Interactive validation testing.
     */
    private static void demonstrateValidationInteractive() {
        System.out.println("\n--- Validation Testing ---");
        
        System.out.print("Enter an email to validate: ");
        String email = scanner.nextLine().trim();
        boolean emailValid = ValidationUtil.isValidEmail(email);
        System.out.println("Email '" + email + "' is " + (emailValid ? "VALID" : "INVALID"));
        
        System.out.print("Enter a phone number to validate: ");
        String phone = scanner.nextLine().trim();
        boolean phoneValid = ValidationUtil.isValidPhoneNumber(phone);
        System.out.println("Phone '" + phone + "' is " + (phoneValid ? "VALID" : "INVALID"));
        
        if (phoneValid) {
            try {
                String normalized = ValidationUtil.normalizePhoneNumber(phone);
                System.out.println("Normalized format: " + normalized);
            } catch (Exception e) {
                System.out.println("Error normalizing: " + e.getMessage());
            }
        }
        
        System.out.print("Enter a GPA to validate (0.0-4.0): ");
        try {
            double gpa = Double.parseDouble(scanner.nextLine().trim());
            boolean gpaValid = ValidationUtil.isValidGPA(gpa);
            System.out.println("GPA " + gpa + " is " + (gpaValid ? "VALID" : "INVALID"));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format");
        }
        
        System.out.println();
    }
    
    /**
     * Display system information.
     */
    private static void displaySystemInfo() {
        System.out.println("\n--- System Information ---");
        System.out.println("Application: " + APP_NAME);
        System.out.println("Version: " + VERSION);
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Memory (Free/Total): " + 
                          (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB / " +
                          (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB");
        
        System.out.println("\nFeatures Implemented:");
        System.out.println("✓ Object-Oriented Programming (Inheritance, Polymorphism, Encapsulation)");
        System.out.println("✓ Custom Exception Handling");
        System.out.println("✓ Data Validation Utilities");
        System.out.println("✓ Collections Framework Usage");
        System.out.println("✓ Enum Types");
        System.out.println("✓ BigDecimal for Financial Calculations");
        System.out.println("✓ LocalDate and LocalTime for Date/Time");
        System.out.println("✓ Regular Expressions");
        System.out.println("✓ Stream API (in models)");
        System.out.println("✓ Static Methods and Utility Classes");
        System.out.println("✓ Method Overloading");
        System.out.println("✓ Defensive Copying");
        System.out.println("✓ equals() and hashCode() Implementation");
        System.out.println("✓ toString() Override");
        System.out.println();
    }
}