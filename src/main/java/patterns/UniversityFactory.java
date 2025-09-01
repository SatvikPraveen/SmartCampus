// File location: src/main/java/patterns/UniversityFactory.java

package patterns;

import models.*;
import java.util.*;

/**
 * Factory pattern implementation for creating university-related objects
 * Provides centralized object creation with consistent initialization
 */
public class UniversityFactory {
    
    // Factory for creating different types of universities
    public static class UniversityTypeFactory {
        
        /**
         * Create a public university
         */
        public static University createPublicUniversity(String name, String location) {
            return new University.Builder()
                .name(name)
                .location(location)
                .type(UniversityType.PUBLIC)
                .accreditation("State Accredited")
                .establishedYear(1950 + new Random().nextInt(50))
                .build();
        }
        
        /**
         * Create a private university
         */
        public static University createPrivateUniversity(String name, String location) {
            return new University.Builder()
                .name(name)
                .location(location)
                .type(UniversityType.PRIVATE)
                .accreditation("Private Accredited")
                .establishedYear(1900 + new Random().nextInt(100))
                .build();
        }
        
        /**
         * Create a community college
         */
        public static University createCommunityCollege(String name, String location) {
            return new University.Builder()
                .name(name)
                .location(location)
                .type(UniversityType.COMMUNITY_COLLEGE)
                .accreditation("Community College Accredited")
                .establishedYear(1960 + new Random().nextInt(40))
                .build();
        }
        
        /**
         * Create an online university
         */
        public static University createOnlineUniversity(String name) {
            return new University.Builder()
                .name(name)
                .location("Online")
                .type(UniversityType.ONLINE)
                .accreditation("Online Education Accredited")
                .establishedYear(1990 + new Random().nextInt(30))
                .build();
        }
    }
    
    // Factory for creating different types of departments
    public static class DepartmentFactory {
        
        /**
         * Create Computer Science department
         */
        public static Department createComputerScienceDepartment() {
            return new Department(
                "CS",
                "Computer Science",
                "Dr. Alan Turing",
                "Technology Building, Floor 3",
                1970,
                250
            );
        }
        
        /**
         * Create Mathematics department
         */
        public static Department createMathematicsDepartment() {
            return new Department(
                "MATH",
                "Mathematics",
                "Dr. Emmy Noether",
                "Science Building, Floor 2",
                1965,
                180
            );
        }
        
        /**
         * Create Engineering department
         */
        public static Department createEngineeringDepartment() {
            return new Department(
                "ENG",
                "Engineering",
                "Dr. Nikola Tesla",
                "Engineering Complex, Floor 1",
                1955,
                300
            );
        }
        
        /**
         * Create Business department
         */
        public static Department createBusinessDepartment() {
            return new Department(
                "BUS",
                "Business Administration",
                "Dr. Peter Drucker",
                "Business Center, Floor 4",
                1960,
                400
            );
        }
        
        /**
         * Create Arts department
         */
        public static Department createArtsDepartment() {
            return new Department(
                "ART",
                "Fine Arts",
                "Dr. Maya Angelou",
                "Arts Building, Floor 1",
                1950,
                150
            );
        }
        
        /**
         * Create department by type
         */
        public static Department createDepartment(DepartmentType type) {
            switch (type) {
                case COMPUTER_SCIENCE:
                    return createComputerScienceDepartment();
                case MATHEMATICS:
                    return createMathematicsDepartment();
                case ENGINEERING:
                    return createEngineeringDepartment();
                case BUSINESS:
                    return createBusinessDepartment();
                case ARTS:
                    return createArtsDepartment();
                default:
                    throw new IllegalArgumentException("Unknown department type: " + type);
            }
        }
        
        /**
         * Create multiple departments
         */
        public static List<Department> createStandardDepartments() {
            return Arrays.asList(
                createComputerScienceDepartment(),
                createMathematicsDepartment(),
                createEngineeringDepartment(),
                createBusinessDepartment(),
                createArtsDepartment()
            );
        }
    }
    
    // Factory for creating different types of users
    public static class UserFactory {
        
        private static final Random random = new Random();
        private static final String[] FIRST_NAMES = {
            "John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Emily",
            "James", "Maria", "William", "Jennifer", "Richard", "Patricia", "Charles", "Linda"
        };
        private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas"
        };
        
        /**
         * Create a student with random data
         */
        public static Student createRandomStudent(Department department) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@university.edu";
            String id = "STU" + String.format("%06d", random.nextInt(999999));
            
            return new Student(id, firstName + " " + lastName, email, department, new Date());
        }
        
        /**
         * Create a professor with random data
         */
        public static Professor createRandomProfessor(Department department) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@university.edu";
            String id = "PROF" + String.format("%06d", random.nextInt(999999));
            String specialization = getRandomSpecialization(department);
            String office = department.getLocation() + ", Room " + (100 + random.nextInt(300));
            int experience = 1 + random.nextInt(30);
            
            return new Professor(id, "Dr. " + firstName + " " + lastName, email, 
                               department, specialization, office, experience);
        }
        
        /**
         * Create an admin user
         */
        public static Admin createAdmin(String name, String email, AdminRole role) {
            String id = "ADM" + String.format("%06d", random.nextInt(999999));
            return new Admin(id, name, email, role);
        }
        
        /**
         * Create multiple students
         */
        public static List<Student> createStudents(Department department, int count) {
            List<Student> students = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                students.add(createRandomStudent(department));
            }
            return students;
        }
        
        /**
         * Create multiple professors
         */
        public static List<Professor> createProfessors(Department department, int count) {
            List<Professor> professors = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                professors.add(createRandomProfessor(department));
            }
            return professors;
        }
        
        private static String getRandomSpecialization(Department department) {
            Map<String, String[]> specializations = new HashMap<>();
            specializations.put("CS", new String[]{"Artificial Intelligence", "Software Engineering", "Data Science", "Cybersecurity"});
            specializations.put("MATH", new String[]{"Pure Mathematics", "Applied Mathematics", "Statistics", "Mathematical Physics"});
            specializations.put("ENG", new String[]{"Mechanical Engineering", "Electrical Engineering", "Civil Engineering", "Chemical Engineering"});
            specializations.put("BUS", new String[]{"Marketing", "Finance", "Management", "Operations"});
            specializations.put("ART", new String[]{"Painting", "Sculpture", "Digital Art", "Art History"});
            
            String[] options = specializations.getOrDefault(department.getDepartmentCode(), new String[]{"General Studies"});
            return options[random.nextInt(options.length)];
        }
    }
    
    // Factory for creating different types of courses
    public static class CourseFactory {
        
        private static final Random random = new Random();
        
        /**
         * Create introductory course
         */
        public static Course createIntroductoryCourse(String subject, Department department, Professor professor) {
            String courseCode = department.getDepartmentCode() + "101";
            String name = "Introduction to " + subject;
            String description = "An introductory course covering the fundamentals of " + subject.toLowerCase();
            
            return new Course(courseCode, name, description, 3, department, professor,
                            getCurrentSemester(), getCurrentAcademicYear(), 30, 0);
        }
        
        /**
         * Create advanced course
         */
        public static Course createAdvancedCourse(String subject, Department department, Professor professor) {
            String courseCode = department.getDepartmentCode() + (300 + random.nextInt(200));
            String name = "Advanced " + subject;
            String description = "An advanced course in " + subject.toLowerCase() + " for upper-level students";
            
            return new Course(courseCode, name, description, 4, department, professor,
                            getCurrentSemester(), getCurrentAcademicYear(), 20, 0);
        }
        
        /**
         * Create graduate course
         */
        public static Course createGraduateCourse(String subject, Department department, Professor professor) {
            String courseCode = department.getDepartmentCode() + (500 + random.nextInt(100));
            String name = "Graduate " + subject;
            String description = "A graduate-level course in " + subject.toLowerCase();
            
            return new Course(courseCode, name, description, 3, department, professor,
                            getCurrentSemester(), getCurrentAcademicYear(), 15, 0);
        }
        
        /**
         * Create laboratory course
         */
        public static Course createLaboratoryCourse(String subject, Department department, Professor professor) {
            String courseCode = department.getDepartmentCode() + (200 + random.nextInt(100)) + "L";
            String name = subject + " Laboratory";
            String description = "Hands-on laboratory experience in " + subject.toLowerCase();
            
            return new Course(courseCode, name, description, 1, department, professor,
                            getCurrentSemester(), getCurrentAcademicYear(), 12, 0);
        }
        
        /**
         * Create seminar course
         */
        public static Course createSeminarCourse(String topic, Department department, Professor professor) {
            String courseCode = department.getDepartmentCode() + (400 + random.nextInt(100)) + "S";
            String name = "Seminar in " + topic;
            String description = "A seminar course focusing on current topics in " + topic.toLowerCase();
            
            return new Course(courseCode, name, description, 2, department, professor,
                            getCurrentSemester(), getCurrentAcademicYear(), 10, 0);
        }
        
        /**
         * Create standard curriculum for a department
         */
        public static List<Course> createStandardCurriculum(Department department, List<Professor> professors) {
            if (professors.isEmpty()) {
                throw new IllegalArgumentException("At least one professor is required");
            }
            
            List<Course> courses = new ArrayList<>();
            Professor mainProfessor = professors.get(0);
            
            switch (department.getDepartmentCode()) {
                case "CS":
                    courses.addAll(createComputerScienceCurriculum(department, professors));
                    break;
                case "MATH":
                    courses.addAll(createMathematicsCurriculum(department, professors));
                    break;
                case "ENG":
                    courses.addAll(createEngineeringCurriculum(department, professors));
                    break;
                case "BUS":
                    courses.addAll(createBusinessCurriculum(department, professors));
                    break;
                case "ART":
                    courses.addAll(createArtsCurriculum(department, professors));
                    break;
                default:
                    // Generic courses
                    courses.add(createIntroductoryCourse("General Studies", department, mainProfessor));
                    courses.add(createAdvancedCourse("Research Methods", department, mainProfessor));
            }
            
            return courses;
        }
        
        private static List<Course> createComputerScienceCurriculum(Department department, List<Professor> professors) {
            List<Course> courses = new ArrayList<>();
            Professor prof1 = professors.get(0);
            Professor prof2 = professors.size() > 1 ? professors.get(1) : prof1;
            
            courses.add(createIntroductoryCourse("Programming", department, prof1));
            courses.add(createIntroductoryCourse("Computer Science", department, prof1));
            courses.add(createAdvancedCourse("Data Structures", department, prof1));
            courses.add(createAdvancedCourse("Algorithms", department, prof2));
            courses.add(createGraduateCourse("Machine Learning", department, prof2));
            courses.add(createLaboratoryCourse("Programming", department, prof1));
            courses.add(createSeminarCourse("Artificial Intelligence", department, prof2));
            
            return courses;
        }
        
        private static List<Course> createMathematicsCurriculum(Department department, List<Professor> professors) {
            List<Course> courses = new ArrayList<>();
            Professor prof1 = professors.get(0);
            Professor prof2 = professors.size() > 1 ? professors.get(1) : prof1;
            
            courses.add(createIntroductoryCourse("Calculus", department, prof1));
            courses.add(createIntroductoryCourse("Statistics", department, prof1));
            courses.add(createAdvancedCourse("Linear Algebra", department, prof1));
            courses.add(createAdvancedCourse("Differential Equations", department, prof2));
            courses.add(createGraduateCourse("Real Analysis", department, prof2));
            courses.add(createSeminarCourse("Mathematical Modeling", department, prof2));
            
            return courses;
        }
        
        private static List<Course> createEngineeringCurriculum(Department department, List<Professor> professors) {
            List<Course> courses = new ArrayList<>();
            Professor prof1 = professors.get(0);
            Professor prof2 = professors.size() > 1 ? professors.get(1) : prof1;
            
            courses.add(createIntroductoryCourse("Engineering", department, prof1));
            courses.add(createAdvancedCourse("Thermodynamics", department, prof1));
            courses.add(createAdvancedCourse("Mechanics", department, prof2));
            courses.add(createLaboratoryCourse("Engineering", department, prof1));
            courses.add(createGraduateCourse("Advanced Materials", department, prof2));
            
            return courses;
        }
        
        private static List<Course> createBusinessCurriculum(Department department, List<Professor> professors) {
            List<Course> courses = new ArrayList<>();
            Professor prof1 = professors.get(0);
            Professor prof2 = professors.size() > 1 ? professors.get(1) : prof1;
            
            courses.add(createIntroductoryCourse("Business", department, prof1));
            courses.add(createIntroductoryCourse("Economics", department, prof1));
            courses.add(createAdvancedCourse("Marketing", department, prof1));
            courses.add(createAdvancedCourse("Finance", department, prof2));
            courses.add(createGraduateCourse("Strategic Management", department, prof2));
            courses.add(createSeminarCourse("Entrepreneurship", department, prof2));
            
            return courses;
        }
        
        private static List<Course> createArtsCurriculum(Department department, List<Professor> professors) {
            List<Course> courses = new ArrayList<>();
            Professor prof1 = professors.get(0);
            Professor prof2 = professors.size() > 1 ? professors.get(1) : prof1;
            
            courses.add(createIntroductoryCourse("Art History", department, prof1));
            courses.add(createIntroductoryCourse("Drawing", department, prof1));
            courses.add(createAdvancedCourse("Painting", department, prof1));
            courses.add(createAdvancedCourse("Sculpture", department, prof2));
            courses.add(createLaboratoryCourse("Digital Art", department, prof2));
            courses.add(createSeminarCourse("Contemporary Art", department, prof2));
            
            return courses;
        }
        
        private static String getCurrentSemester() {
            Calendar cal = Calendar.getInstance();
            int month = cal.get(Calendar.MONTH);
            
            if (month >= Calendar.AUGUST || month <= Calendar.DECEMBER) {
                return "Fall";
            } else if (month >= Calendar.JANUARY && month <= Calendar.MAY) {
                return "Spring";
            } else {
                return "Summer";
            }
        }
        
        private static String getCurrentAcademicYear() {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            
            if (month >= Calendar.AUGUST) {
                return year + "-" + (year + 1);
            } else {
                return (year - 1) + "-" + year;
            }
        }
    }
    
    // Factory for creating complete university systems
    public static class UniversitySystemFactory {
        
        /**
         * Create a complete university system with departments, users, and courses
         */
        public static UniversitySystem createCompleteUniversitySystem(String universityName, String location) {
            // Create university
            University university = UniversityTypeFactory.createPublicUniversity(universityName, location);
            
            // Create departments
            List<Department> departments = DepartmentFactory.createStandardDepartments();
            
            // Create users for each department
            Map<Department, List<Professor>> departmentProfessors = new HashMap<>();
            Map<Department, List<Student>> departmentStudents = new HashMap<>();
            List<Course> allCourses = new ArrayList<>();
            
            for (Department department : departments) {
                // Create professors
                List<Professor> professors = UserFactory.createProfessors(department, 3 + new Random().nextInt(3));
                departmentProfessors.put(department, professors);
                
                // Create students
                List<Student> students = UserFactory.createStudents(department, 20 + new Random().nextInt(30));
                departmentStudents.put(department, students);
                
                // Create courses
                List<Course> courses = CourseFactory.createStandardCurriculum(department, professors);
                allCourses.addAll(courses);
            }
            
            // Create admin users
            List<Admin> admins = Arrays.asList(
                UserFactory.createAdmin("System Administrator", "admin@university.edu", AdminRole.SYSTEM_ADMIN),
                UserFactory.createAdmin("Academic Registrar", "registrar@university.edu", AdminRole.REGISTRAR),
                UserFactory.createAdmin("IT Support", "it@university.edu", AdminRole.IT_SUPPORT)
            );
            
            return new UniversitySystem(university, departments, departmentProfessors, 
                                      departmentStudents, allCourses, admins);
        }
        
        /**
         * Create a small university system for testing
         */
        public static UniversitySystem createTestUniversitySystem() {
            return createCompleteUniversitySystem("Test University", "Test City");
        }
    }
    
    // Enums for factory configurations
    public enum UniversityType {
        PUBLIC, PRIVATE, COMMUNITY_COLLEGE, ONLINE
    }
    
    public enum DepartmentType {
        COMPUTER_SCIENCE, MATHEMATICS, ENGINEERING, BUSINESS, ARTS
    }
    
    public enum AdminRole {
        SYSTEM_ADMIN, REGISTRAR, IT_SUPPORT, ACADEMIC_AFFAIRS, STUDENT_SERVICES
    }
    
    // Data structure to hold complete university system
    public static class UniversitySystem {
        private final University university;
        private final List<Department> departments;
        private final Map<Department, List<Professor>> departmentProfessors;
        private final Map<Department, List<Student>> departmentStudents;
        private final List<Course> courses;
        private final List<Admin> admins;
        
        public UniversitySystem(University university, List<Department> departments,
                              Map<Department, List<Professor>> departmentProfessors,
                              Map<Department, List<Student>> departmentStudents,
                              List<Course> courses, List<Admin> admins) {
            this.university = university;
            this.departments = departments;
            this.departmentProfessors = departmentProfessors;
            this.departmentStudents = departmentStudents;
            this.courses = courses;
            this.admins = admins;
        }
        
        // Getters
        public University getUniversity() { return university; }
        public List<Department> getDepartments() { return departments; }
        public Map<Department, List<Professor>> getDepartmentProfessors() { return departmentProfessors; }
        public Map<Department, List<Student>> getDepartmentStudents() { return departmentStudents; }
        public List<Course> getCourses() { return courses; }
        public List<Admin> getAdmins() { return admins; }
        
        public int getTotalStudents() {
            return departmentStudents.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
        
        public int getTotalProfessors() {
            return departmentProfessors.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
        
        public int getTotalCourses() {
            return courses.size();
        }
        
        @Override
        public String toString() {
            return String.format("UniversitySystem{university='%s', departments=%d, students=%d, professors=%d, courses=%d, admins=%d}",
                               university.getName(), departments.size(), getTotalStudents(), 
                               getTotalProfessors(), getTotalCourses(), admins.size());
        }
    }
}