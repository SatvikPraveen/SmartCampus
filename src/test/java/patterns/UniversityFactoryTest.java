package patterns;

import models.Admin;
import models.Course;
import models.Department;
import models.Professor;
import models.Student;
import models.University;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import patterns.UniversityFactory.AdminRole;
import patterns.UniversityFactory.CourseFactory;
import patterns.UniversityFactory.DepartmentFactory;
import patterns.UniversityFactory.DepartmentType;
import patterns.UniversityFactory.UniversitySystem;
import patterns.UniversityFactory.UniversitySystemFactory;
import patterns.UniversityFactory.UniversityTypeFactory;
import patterns.UniversityFactory.UserFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniversityFactoryTest {

    private static Professor professor(String id) {
        return new Professor(id, "Grace", "Hopper", id.toLowerCase() + "@university.edu", null, id,
                             "DEPT_CS", Professor.AcademicRank.ASSISTANT, "Compilers");
    }

    @Nested
    class Universities {

        @Test
        void publicUniversityIsAccreditedWithAbbreviation() {
            University u = UniversityTypeFactory.createPublicUniversity("State University of Florida", "Tampa");

            assertThat(u.getName()).isEqualTo("State University of Florida");
            assertThat(u.getShortName()).isEqualTo("SUF");
            assertThat(u.getType()).isEqualTo(University.UniversityType.PUBLIC);
            assertThat(u.getCity()).isEqualTo("Tampa");
            assertThat(u.getAccreditationStatus()).isEqualTo(University.AccreditationStatus.FULLY_ACCREDITED);
            assertThat(u.getFoundedDate().getYear()).isBetween(1950, 1999);
        }

        @Test
        void otherUniversityTypesUseTheirFoundingRanges() {
            University priv = UniversityTypeFactory.createPrivateUniversity("Ivy College", "Boston");
            University cc = UniversityTypeFactory.createCommunityCollege("Valley Community College", "Fresno");
            University online = UniversityTypeFactory.createOnlineUniversity("Open Web University");

            assertThat(priv.getType()).isEqualTo(University.UniversityType.PRIVATE);
            assertThat(priv.getFoundedDate().getYear()).isBetween(1900, 1999);
            assertThat(cc.getType()).isEqualTo(University.UniversityType.COMMUNITY_COLLEGE);
            assertThat(cc.getFoundedDate().getYear()).isBetween(1960, 1999);
            assertThat(online.getType()).isEqualTo(University.UniversityType.ONLINE);
            assertThat(online.getCity()).isEqualTo("Online");
            assertThat(online.getFoundedDate().getYear()).isBetween(1990, 2019);
        }

        @Test
        void abbreviationFallsBackToNameWithoutCapitals() {
            assertThat(UniversityTypeFactory.createPublicUniversity("  tech  ", "X").getShortName()).isEqualTo("tech");
        }
    }

    @Nested
    class Departments {

        @ParameterizedTest
        @EnumSource(DepartmentType.class)
        void createDepartmentDispatchesOnType(DepartmentType type) {
            Department d = DepartmentFactory.createDepartment(type);

            String expectedCode = switch (type) {
                case COMPUTER_SCIENCE -> "CS";
                case MATHEMATICS -> "MATH";
                case ENGINEERING -> "ENG";
                case BUSINESS -> "BUS";
                case ARTS -> "ART";
            };
            assertThat(d.getDepartmentCode()).isEqualTo(expectedCode);
            assertThat(d.getDepartmentId()).startsWith("DEPT_" + expectedCode + "_");
            assertThat(d.getEstablishedYear()).matches("19\\d\\d");
        }

        @Test
        void computerScienceDepartmentHasExpectedDetails() {
            Department cs = DepartmentFactory.createComputerScienceDepartment();

            assertThat(cs.getDepartmentName()).isEqualTo("Computer Science");
            assertThat(cs.getDescription()).isEqualTo("Department of Computer Science");
            assertThat(cs.getLocation()).isEqualTo("Technology Building, Floor 3");
            assertThat(cs.getEstablishedYear()).isEqualTo("1970");
        }

        @Test
        void standardDepartmentsAreFiveDistinctDepartments() {
            List<Department> departments = DepartmentFactory.createStandardDepartments();

            assertThat(departments).extracting(Department::getDepartmentCode)
                .containsExactly("CS", "MATH", "ENG", "BUS", "ART");
            assertThat(departments).doesNotHaveDuplicates();
        }
    }

    @Nested
    class Users {

        @Test
        void randomStudentBelongsToDepartment() {
            Department cs = DepartmentFactory.createComputerScienceDepartment();

            Student s = UserFactory.createRandomStudent(cs);

            assertThat(s.getStudentId()).matches("STU\\d{6}");
            assertThat(s.getUserId()).isEqualTo(s.getStudentId());
            assertThat(s.getDepartmentId()).isEqualTo(cs.getDepartmentId());
            assertThat(s.getMajor()).isEqualTo("Computer Science");
            assertThat(s.getAcademicYear()).isEqualTo(Student.AcademicYear.FRESHMAN);
            assertThat(s.getEmail()).isEqualTo(
                s.getFirstName().toLowerCase() + "." + s.getLastName().toLowerCase() + "@university.edu");
        }

        @Test
        void randomProfessorGetsDepartmentSpecialization() {
            Department math = DepartmentFactory.createMathematicsDepartment();

            Professor p = UserFactory.createRandomProfessor(math);

            assertThat(p.getProfessorId()).matches("PROF\\d{6}");
            assertThat(p.getDepartmentId()).isEqualTo(math.getDepartmentId());
            assertThat(p.getSpecialization()).isIn(
                "Pure Mathematics", "Applied Mathematics", "Statistics", "Mathematical Physics");
            assertThat(p.getOfficeLocation()).startsWith("Science Building, Floor 2, Room ");
            assertThat(p.getYearsOfExperience()).isBetween(1, 30);
        }

        @Test
        void unknownDepartmentCodeGetsGeneralSpecialization() {
            Department other = new Department("DEPT_X", "XYZ", "Other", "d", "Annex");

            assertThat(UserFactory.createRandomProfessor(other).getSpecialization()).isEqualTo("General Studies");
        }

        @Test
        void bulkCreationReturnsRequestedCounts() {
            Department art = DepartmentFactory.createArtsDepartment();

            assertThat(UserFactory.createStudents(art, 7)).hasSize(7);
            assertThat(UserFactory.createProfessors(art, 4)).hasSize(4)
                .allSatisfy(p -> assertThat(p.getSpecialization())
                    .isIn("Painting", "Sculpture", "Digital Art", "Art History"));
            assertThat(UserFactory.createStudents(art, 0)).isEmpty();
        }

        @Test
        void adminRoleMapsToLevelAndDepartment() {
            Admin sys = UserFactory.createAdmin("System Administrator", "a@u.edu", AdminRole.SYSTEM_ADMIN);
            Admin reg = UserFactory.createAdmin("Academic Registrar", "r@u.edu", AdminRole.REGISTRAR);
            Admin aa = UserFactory.createAdmin("Dean Office", "d@u.edu", AdminRole.ACADEMIC_AFFAIRS);
            Admin ss = UserFactory.createAdmin("Student Helper", "s@u.edu", AdminRole.STUDENT_SERVICES);
            Admin it = UserFactory.createAdmin("Help Desk", "h@u.edu", AdminRole.IT_SUPPORT);

            assertThat(sys.getAdminLevel()).isEqualTo(Admin.AdminLevel.SYSTEM_ADMIN);
            assertThat(sys.getDepartment()).isEqualTo(Admin.Department.IT_SERVICES);
            assertThat(reg.getAdminLevel()).isEqualTo(Admin.AdminLevel.SENIOR_ADMIN);
            assertThat(reg.getDepartment()).isEqualTo(Admin.Department.REGISTRAR);
            assertThat(aa.getAdminLevel()).isEqualTo(Admin.AdminLevel.SENIOR_ADMIN);
            assertThat(aa.getDepartment()).isEqualTo(Admin.Department.ACADEMIC_AFFAIRS);
            assertThat(ss.getAdminLevel()).isEqualTo(Admin.AdminLevel.JUNIOR_ADMIN);
            assertThat(ss.getDepartment()).isEqualTo(Admin.Department.STUDENT_SERVICES);
            assertThat(it.getAdminLevel()).isEqualTo(Admin.AdminLevel.JUNIOR_ADMIN);
            assertThat(it.getDepartment()).isEqualTo(Admin.Department.IT_SERVICES);

            assertThat(reg.getFirstName()).isEqualTo("Academic");
            assertThat(reg.getLastName()).isEqualTo("Registrar");
            assertThat(reg.getEmail()).isEqualTo("r@u.edu");
            assertThat(reg.getUserId()).matches("ADM\\d{6}");
        }

        @Test
        void singleWordAdminNameIsUsedForBothNames() {
            Admin a = UserFactory.createAdmin(" Root ", "root@u.edu", AdminRole.IT_SUPPORT);

            assertThat(a.getFirstName()).isEqualTo("Root");
            assertThat(a.getLastName()).isEqualTo("Root");
        }
    }

    @Nested
    class Courses {

        private final Department cs = DepartmentFactory.createComputerScienceDepartment();
        private final Professor prof = professor("PROF000001");

        @Test
        void courseKindsHaveExpectedShape() {
            Course intro = CourseFactory.createIntroductoryCourse("Logic", cs, prof);
            Course adv = CourseFactory.createAdvancedCourse("Compilers", cs, prof);
            Course grad = CourseFactory.createGraduateCourse("Type Theory", cs, prof);
            Course lab = CourseFactory.createLaboratoryCourse("Robotics", cs, prof);
            Course sem = CourseFactory.createSeminarCourse("Ethics", cs, prof);

            assertThat(intro.getCourseCode()).isEqualTo("CS101");
            assertThat(intro.getCourseName()).isEqualTo("Introduction to Logic");
            assertThat(intro.getDescription()).contains("fundamentals of logic");
            assertThat(intro.getCredits()).isEqualTo(3);
            assertThat(intro.getMaxEnrollment()).isEqualTo(30);
            assertThat(intro.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.BEGINNER);
            assertThat(intro.getProfessorId()).isEqualTo("PROF000001");
            assertThat(intro.getDepartmentId()).isEqualTo(cs.getDepartmentId());
            assertThat(intro.getSemester()).isIn("Fall", "Spring", "Summer");

            assertThat(adv.getCourseCode()).matches("CS[34]\\d\\d");
            assertThat(adv.getCredits()).isEqualTo(4);
            assertThat(adv.getMaxEnrollment()).isEqualTo(20);
            assertThat(adv.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.ADVANCED);

            assertThat(grad.getCourseCode()).matches("CS5\\d\\d");
            assertThat(grad.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.EXPERT);
            assertThat(grad.getMaxEnrollment()).isEqualTo(15);

            assertThat(lab.getCourseCode()).matches("CS2\\d\\dL");
            assertThat(lab.getCredits()).isEqualTo(1);
            assertThat(lab.getCourseName()).isEqualTo("Robotics Laboratory");

            assertThat(sem.getCourseCode()).matches("CS4\\d\\dS");
            assertThat(sem.getCredits()).isEqualTo(2);
            assertThat(sem.getMaxEnrollment()).isEqualTo(10);
        }

        @Test
        void curriculumRequiresAProfessor() {
            assertThatThrownBy(() -> CourseFactory.createStandardCurriculum(cs, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void curriculumSizesPerDepartment() {
            List<Professor> profs = List.of(prof, professor("PROF000002"));

            assertThat(CourseFactory.createStandardCurriculum(cs, profs)).hasSize(7);
            assertThat(CourseFactory.createStandardCurriculum(DepartmentFactory.createMathematicsDepartment(), profs)).hasSize(6);
            assertThat(CourseFactory.createStandardCurriculum(DepartmentFactory.createEngineeringDepartment(), profs)).hasSize(5);
            assertThat(CourseFactory.createStandardCurriculum(DepartmentFactory.createBusinessDepartment(), profs)).hasSize(6);
            assertThat(CourseFactory.createStandardCurriculum(DepartmentFactory.createArtsDepartment(), profs)).hasSize(6);
        }

        @Test
        void curriculumSplitsCoursesBetweenTwoProfessors() {
            List<Course> courses = CourseFactory.createStandardCurriculum(cs, List.of(prof, professor("PROF000002")));

            assertThat(courses).extracting(Course::getProfessorId).containsOnly("PROF000001", "PROF000002")
                .contains("PROF000001", "PROF000002");
            assertThat(CourseFactory.createStandardCurriculum(cs, List.of(prof)))
                .extracting(Course::getProfessorId).containsOnly("PROF000001");
        }

        @Test
        void unknownDepartmentGetsGenericCurriculum() {
            Department other = new Department("DEPT_HIS", "HIS", "History", "d", "Hall");

            List<Course> courses = CourseFactory.createStandardCurriculum(other, List.of(prof));

            assertThat(courses).extracting(Course::getCourseName)
                .containsExactly("Introduction to General Studies", "Advanced Research Methods");
        }

        // Regression: curricula contained two introductory "XX101" courses (and could randomly draw the same
        // advanced/graduate number twice); course codes double as course IDs, so the IDs collided.
        @RepeatedTest(50)
        void curriculumCourseIdsAreUnique() {
            List<Professor> profs = List.of(prof, professor("PROF000002"));
            for (Department d : DepartmentFactory.createStandardDepartments()) {
                List<Course> courses = CourseFactory.createStandardCurriculum(d, profs);

                assertThat(courses).extracting(Course::getCourseId).doesNotHaveDuplicates();
                assertThat(courses).allSatisfy(c -> {
                    assertThat(c.getCourseId()).isEqualTo(c.getCourseCode());
                    assertThat(c.getCourseCode()).startsWith(d.getDepartmentCode());
                });
            }
        }

        @Test
        void renumberedIntroductoryCourseKeepsPrefix() {
            List<Course> courses = CourseFactory.createStandardCurriculum(cs, List.of(prof));

            assertThat(courses.get(0).getCourseCode()).isEqualTo("CS101");
            assertThat(courses.get(1).getCourseCode()).isEqualTo("CS102");
            assertThat(courses.get(1).getCourseName()).isEqualTo("Introduction to Computer Science");
        }
    }

    @Nested
    class Systems {

        @Test
        void completeSystemWiresEverythingTogether() {
            UniversitySystem system = UniversitySystemFactory.createCompleteUniversitySystem("Gulf Coast University", "Tampa");

            assertThat(system.getUniversity().getName()).isEqualTo("Gulf Coast University");
            assertThat(system.getUniversity().getType()).isEqualTo(University.UniversityType.PUBLIC);
            assertThat(system.getDepartments()).hasSize(5);
            assertThat(system.getAdmins()).hasSize(3);

            Map<Department, List<Professor>> profs = system.getDepartmentProfessors();
            Map<Department, List<Student>> students = system.getDepartmentStudents();
            assertThat(profs.keySet()).containsExactlyInAnyOrderElementsOf(system.getDepartments());
            assertThat(students.keySet()).containsExactlyInAnyOrderElementsOf(system.getDepartments());
            profs.values().forEach(list -> assertThat(list).hasSizeBetween(3, 5));
            students.values().forEach(list -> assertThat(list).hasSizeBetween(20, 49));

            assertThat(system.getTotalProfessors()).isEqualTo(profs.values().stream().mapToInt(List::size).sum());
            assertThat(system.getTotalStudents()).isEqualTo(students.values().stream().mapToInt(List::size).sum());
            assertThat(system.getTotalCourses()).isEqualTo(7 + 6 + 5 + 6 + 6).isEqualTo(system.getCourses().size());
            assertThat(system.toString())
                .contains("university='Gulf Coast University'")
                .contains("departments=5")
                .contains("courses=30")
                .contains("admins=3");
        }

        @Test
        void testSystemUsesTestNames() {
            UniversitySystem system = UniversitySystemFactory.createTestUniversitySystem();

            assertThat(system.getUniversity().getName()).isEqualTo("Test University");
            assertThat(system.getUniversity().getCity()).isEqualTo("Test City");
        }
    }
}
