package patterns;

import models.Course;
import models.Department;
import models.Professor;
import models.Student;
import models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import patterns.AdapterService.DataFormatAdapter.CsvToJsonStudentAdapter;
import patterns.AdapterService.DataFormatAdapter.XmlToCourseAdapter;
import patterns.AdapterService.DatabaseAdapter;
import patterns.AdapterService.ExternalAuthAdapter;
import patterns.AdapterService.ExternalAuthProvider;
import patterns.AdapterService.ExternalAuthResult;
import patterns.AdapterService.ExternalCourseReportRequest;
import patterns.AdapterService.ExternalCourseReportResponse;
import patterns.AdapterService.ExternalGradeReportAdapter;
import patterns.AdapterService.ExternalPermissionResult;
import patterns.AdapterService.ExternalReportingAPI;
import patterns.AdapterService.ExternalTranscriptRequest;
import patterns.AdapterService.ExternalTranscriptResponse;
import patterns.AdapterService.ExternalUserInfo;
import patterns.AdapterService.LegacyStudentAdapter;
import patterns.AdapterService.LegacyStudentRecord;
import patterns.AdapterService.LegacyStudentSystem;
import services.AuthService;
import services.AuthService.PermissionLevel;
import services.CourseService;
import services.DepartmentService;
import services.EnrollmentService;
import services.GradeService;
import services.ProfessorService;
import services.ReportService;
import services.StudentService;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdapterServiceTest {

    private static Student student(String id, String first, String last, String dept) {
        Student s = new Student(id, first, last, first.toLowerCase() + "@university.edu", null, id, "CS",
                                Student.AcademicYear.FRESHMAN);
        s.setDepartmentId(dept);
        s.setEnrollmentDate(LocalDate.of(2023, 8, 20));
        return s;
    }

    @Nested
    class LegacyStudents {

        private LegacyStudentSystem legacy;
        private LegacyStudentAdapter adapter;

        @BeforeEach
        void setUp() {
            legacy = new LegacyStudentSystem();
            adapter = new LegacyStudentAdapter(legacy, new StudentService());
        }

        @Test
        void createStoresLegacyRecordAndReturnsModernStudent() {
            Student created = adapter.createStudent(student("STU000001", "Ada", "Lovelace", "DEPT_CS"));

            LegacyStudentRecord record = legacy.findStudent("STU000001");
            assertThat(record.getFullName()).isEqualTo("Ada Lovelace");
            assertThat(record.getEmailAddress()).isEqualTo("ada@university.edu");
            assertThat(record.getDeptCode()).isEqualTo("DEPT_CS");
            assertThat(record.getEnrollDate()).isEqualTo(LocalDate.of(2023, 8, 20));

            assertThat(created.getStudentId()).isEqualTo("STU000001");
            assertThat(created.getUserId()).isEqualTo("STU000001");
            assertThat(created.getFirstName()).isEqualTo("Ada");
            assertThat(created.getLastName()).isEqualTo("Lovelace");
            assertThat(created.getEmail()).isEqualTo("ada@university.edu");
            assertThat(created.getDepartmentId()).isEqualTo("DEPT_CS");
            assertThat(created.getEnrollmentDate()).isEqualTo(LocalDate.of(2023, 8, 20));
        }

        @Test
        void missingDepartmentIsStoredAsEmptyCodeAndReadBackAsNull() {
            Student s = student("STU000002", "Alan", "Turing", null);

            adapter.createStudent(s);

            assertThat(legacy.findStudent("STU000002").getDeptCode()).isEmpty();
            assertThat(adapter.getStudentById("STU000002").getDepartmentId()).isNull();
        }

        @Test
        void multiWordLastNameSurvivesRoundTrip() {
            adapter.createStudent(student("STU000003", "Vincent", "van Gogh", "DEPT_ART"));

            Student back = adapter.getStudentById("STU000003");

            assertThat(back.getFirstName()).isEqualTo("Vincent");
            assertThat(back.getLastName()).isEqualTo("van Gogh");
        }

        @Test
        void legacyRecordWithoutNameOrDateConvertsSafely() {
            LegacyStudentRecord bare = new LegacyStudentRecord();
            bare.setStudentId("STU000009");
            bare.setFullName("Cher");
            bare.setEmailAddress("cher@university.edu");
            legacy.addStudent(bare);
            LegacyStudentRecord blank = new LegacyStudentRecord();
            blank.setStudentId("STU000010");
            blank.setFullName("  ");
            blank.setEmailAddress("blank@university.edu");
            legacy.addStudent(blank);

            Student cher = adapter.getStudentById("STU000009");
            Student nameless = adapter.getStudentById("STU000010");

            assertThat(cher.getFirstName()).isEqualTo("Cher");
            assertThat(cher.getDepartmentId()).isNull();
            assertThat(nameless.getStudentId()).isEqualTo("STU000010");
        }

        @Test
        void updateDeleteAndQueries() {
            adapter.createStudent(student("STU000001", "Ada", "Lovelace", "DEPT_CS"));
            adapter.createStudent(student("STU000002", "Alan", "Turing", "DEPT_CS"));
            adapter.createStudent(student("STU000003", "Emmy", "Noether", "DEPT_MATH"));

            Student changed = student("STU000001", "Ada", "Byron", "DEPT_MATH");
            assertThat(adapter.updateStudent(changed).getLastName()).isEqualTo("Byron");
            assertThat(legacy.findStudent("STU000001").getFullName()).isEqualTo("Ada Byron");

            Department math = new Department("DEPT_MATH", "MATH", "Mathematics", "d", "Hall");
            assertThat(adapter.getStudentsByDepartment(math)).extracting(Student::getStudentId)
                .containsExactlyInAnyOrder("STU000001", "STU000003");
            assertThat(adapter.getAllStudents()).hasSize(3);

            adapter.deleteStudent("STU000002");
            assertThat(adapter.getStudentById("STU000002")).isNull();
            assertThat(adapter.getAllStudents()).extracting(Student::getStudentId)
                .containsExactlyInAnyOrder("STU000001", "STU000003");
        }
    }

    @Nested
    class GradeReports {

        private final List<ExternalTranscriptRequest> transcripts = new ArrayList<>();
        private final List<ExternalCourseReportRequest> courseReports = new ArrayList<>();
        private ExternalGradeReportAdapter adapter;

        @BeforeEach
        void setUp() {
            ReportService reports = new ReportService(new StudentService(), new ProfessorService(), new CourseService(),
                                                      new DepartmentService(), new EnrollmentService(), new GradeService());
            ExternalReportingAPI api = new ExternalReportingAPI() {
                @Override
                public ExternalTranscriptResponse generateTranscript(ExternalTranscriptRequest request) {
                    transcripts.add(request);
                    return new ExternalTranscriptResponse("TRANSCRIPT[" + request.getStudentId() + "]");
                }

                @Override
                public ExternalCourseReportResponse generateCourseReport(ExternalCourseReportRequest request) {
                    courseReports.add(request);
                    return new ExternalCourseReportResponse("COURSE[" + request.getCourseCode() + "]");
                }
            };
            adapter = new ExternalGradeReportAdapter(api, reports);
        }

        @Test
        void transcriptRequestCarriesStudentAndInternalData() {
            String transcript = adapter.generateStudentTranscript(student("STU000001", "Ada", "Lovelace", "DEPT_CS"));

            assertThat(transcript).isEqualTo("TRANSCRIPT[STU000001]");
            assertThat(transcripts).singleElement().satisfies(r -> {
                assertThat(r.getStudentId()).isEqualTo("STU000001");
                assertThat(r.getStudentName()).isEqualTo("Ada Lovelace");
                assertThat(r.getInternalData()).isNotNull();
            });
        }

        @Test
        void courseReportRequestCarriesCourseAndInternalData() {
            Course course = new Course("CS101", "CS101", "Intro", "d", 3, "DEPT_CS");

            String report = adapter.generateCourseReport(course);

            assertThat(report).isEqualTo("COURSE[CS101]");
            assertThat(courseReports).singleElement().satisfies(r -> {
                assertThat(r.getCourseCode()).isEqualTo("CS101");
                assertThat(r.getCourseName()).isEqualTo("Intro");
                assertThat(r.getInternalData()).isNotNull();
            });
        }

        @Test
        void statisticsComeFromInternalReportService() {
            assertThat(adapter.getUniversityStatistics()).isNotNull();
        }
    }

    @Nested
    class Csv {

        private final CsvToJsonStudentAdapter csv = new CsvToJsonStudentAdapter();

        @Test
        void csvRowsBecomeRecordsKeyedByHeader() {
            List<Map<String, Object>> records = csv.convertStudentCsvToJson(List.of(
                "id,name,email", "S1,Ada,ada@u.edu", "S2,Alan"));

            assertThat(records).hasSize(2);
            assertThat(records.get(0)).containsOnly(
                Map.entry("id", "S1"), Map.entry("name", "Ada"), Map.entry("email", "ada@u.edu"));
            assertThat(records.get(1)).containsOnlyKeys("id", "name");
        }

        @Test
        void emptyInputsGiveEmptyOutputs() {
            assertThat(csv.convertStudentCsvToJson(List.of())).isEmpty();
            assertThat(csv.convertStudentJsonToCsv(List.of())).isEmpty();
            assertThat(csv.convertStudentCsvToJson(List.of("id,name"))).isEmpty();
        }

        @Test
        void recordsBecomeCsvWithHeaderFromFirstRecord() {
            Map<String, Object> first = new LinkedHashMap<>();
            first.put("id", "S1");
            first.put("gpa", 3.5);
            Map<String, Object> second = new HashMap<>();
            second.put("id", "S2");

            List<String> lines = csv.convertStudentJsonToCsv(List.of(first, second));

            assertThat(lines).containsExactly("id,gpa", "S1,3.5", "S2,");
        }

        // Regression: String.split(",") dropped trailing empty fields, so a JSON->CSV->JSON round trip
        // lost keys whose values were empty (e.g. "S2," came back without the "gpa" key).
        @Test
        void roundTripPreservesEmptyTrailingFields() {
            Map<String, Object> first = new LinkedHashMap<>();
            first.put("id", "S1");
            first.put("gpa", "3.5");
            Map<String, Object> second = new LinkedHashMap<>();
            second.put("id", "S2");
            second.put("gpa", "");

            List<Map<String, Object>> back = csv.convertStudentCsvToJson(csv.convertStudentJsonToCsv(List.of(first, second)));

            assertThat(back).containsExactly(first, second);
        }
    }

    @Nested
    class Xml {

        private final XmlToCourseAdapter xml = new XmlToCourseAdapter();

        private Course sampleCourse() {
            Course c = new Course("CS101", "CS101", "Intro <C & C++>", "Pointers & \"refs\" aren't scary", 4, "DEPT_CS");
            c.setProfessorId("PROF00001");
            c.setSemester("Fall");
            c.setYear(2025);
            c.setMaxEnrollment(45);
            return c;
        }

        @Test
        void courseIsSerializedWithEscaping() {
            String out = xml.convertCourseToXml(sampleCourse());

            assertThat(out)
                .startsWith("<course>").endsWith("</course>")
                .contains("<courseId>CS101</courseId>")
                .contains("<name>Intro &lt;C &amp; C++&gt;</name>")
                .contains("<description>Pointers &amp; &quot;refs&quot; aren&apos;t scary</description>")
                .contains("<credits>4</credits>")
                .contains("<departmentId>DEPT_CS</departmentId>")
                .contains("<professorId>PROF00001</professorId>")
                .contains("<semester>Fall</semester>")
                .contains("<year>2025</year>")
                .contains("<capacity>45</capacity>")
                .contains("<enrolled>0</enrolled>");
        }

        @Test
        void multiLineXmlIsParsed() {
            String input = String.join("\n",
                "<course>",
                "  <courseId>MATH200</courseId>",
                "  <courseCode>MATH200</courseCode>",
                "  <name>Linear Algebra</name>",
                "  <description>Vectors</description>",
                "  <credits>3</credits>",
                "  <departmentId>DEPT_MATH</departmentId>",
                "  <professorId></professorId>",
                "  <semester>Spring</semester>",
                "  <year>2026</year>",
                "</course>");

            Course c = xml.convertXmlToCourse(input);

            assertThat(c.getCourseId()).isEqualTo("MATH200");
            assertThat(c.getCourseName()).isEqualTo("Linear Algebra");
            assertThat(c.getCredits()).isEqualTo(3);
            assertThat(c.getDepartmentId()).isEqualTo("DEPT_MATH");
            assertThat(c.getProfessorId()).isNull();
            assertThat(c.getSemester()).isEqualTo("Spring");
            assertThat(c.getYear()).isEqualTo(2026);
            assertThat(c.getMaxEnrollment()).isEqualTo(30); // default capacity
        }

        @Test
        void missingOptionalElementsUseDefaults() {
            Course c = xml.convertXmlToCourse(
                "<course>\n<courseId>X1</courseId>\n<courseCode>X1</courseCode>\n<name>N</name>\n</course>");

            assertThat(c.getCredits()).isEqualTo(3);
            assertThat(c.getDepartmentId()).isNull();
            assertThat(c.getSemester()).isNull();
            assertThat(c.getMaxEnrollment()).isEqualTo(30);
        }

        // Regression: convertCourseToXml emits a single line, but the parser only understood one element
        // per line, so the adapter could not read its own output (and did not unescape entities).
        @Test
        void serializedCourseRoundTrips() {
            Course original = sampleCourse();

            Course back = xml.convertXmlToCourse(xml.convertCourseToXml(original));

            assertThat(back.getCourseId()).isEqualTo("CS101");
            assertThat(back.getCourseCode()).isEqualTo("CS101");
            assertThat(back.getCourseName()).isEqualTo("Intro <C & C++>");
            assertThat(back.getDescription()).isEqualTo("Pointers & \"refs\" aren't scary");
            assertThat(back.getCredits()).isEqualTo(4);
            assertThat(back.getDepartmentId()).isEqualTo("DEPT_CS");
            assertThat(back.getProfessorId()).isEqualTo("PROF00001");
            assertThat(back.getSemester()).isEqualTo("Fall");
            assertThat(back.getYear()).isEqualTo(2025);
            assertThat(back.getMaxEnrollment()).isEqualTo(45);
        }

        // Regression: a course without semester/department/professor was written as "null"/"" and read back
        // as the literal string "null" for the semester.
        @Test
        void unsetFieldsRoundTripAsNull() {
            Course bare = new Course("ART100", "ART100", "Drawing", null, 2, null);

            String out = xml.convertCourseToXml(bare);
            Course back = xml.convertXmlToCourse(out);

            assertThat(out).contains("<semester></semester>").contains("<description></description>");
            assertThat(back.getSemester()).isNull();
            assertThat(back.getDepartmentId()).isNull();
            assertThat(back.getProfessorId()).isNull();
            assertThat(back.getDescription()).isEmpty();
        }
    }

    @Nested
    class Databases {

        @Test
        void mysqlToPostgresQueryAndTypes() {
            DatabaseAdapter adapter = new DatabaseAdapter("mysql", "postgresql");

            assertThat(adapter.adaptQuery("CREATE TABLE t (id INT AUTO_INCREMENT, ok TINYINT, at DATETIME) LIMIT ?"))
                .isEqualTo("CREATE TABLE t (id INT SERIAL, ok BOOLEAN, at TIMESTAMP) LIMIT ?");
            assertThat(adapter.adaptDataType("tinyint")).isEqualTo("BOOLEAN");
            assertThat(adapter.adaptDataType("DATETIME")).isEqualTo("TIMESTAMP");
            assertThat(adapter.adaptDataType("TEXT")).isEqualTo("TEXT");
            assertThat(adapter.adaptDataType("varchar")).isEqualTo("varchar");
        }

        @Test
        void postgresToMysqlQueryAndTypes() {
            DatabaseAdapter adapter = new DatabaseAdapter("postgresql", "mysql");

            assertThat(adapter.adaptQuery("id SERIAL, ok BOOLEAN, at TIMESTAMP"))
                .isEqualTo("id AUTO_INCREMENT, ok TINYINT, at DATETIME");
            assertThat(adapter.adaptDataType("BOOLEAN")).isEqualTo("TINYINT");
            assertThat(adapter.adaptDataType("timestamp")).isEqualTo("DATETIME");
        }

        @Test
        void oracleToMysqlQuery() {
            DatabaseAdapter adapter = new DatabaseAdapter("oracle", "mysql");

            assertThat(adapter.adaptQuery("SELECT id NUMBER, n VARCHAR2(10), SYSDATE"))
                .isEqualTo("SELECT id INT, n VARCHAR(10), NOW()");
            assertThat(adapter.adaptDataType("NUMBER")).isEqualTo("NUMBER"); // no type mapping for oracle
        }

        @Test
        void unsupportedPairLeavesInputUntouched() {
            DatabaseAdapter adapter = new DatabaseAdapter("sqlite", "mysql");

            assertThat(adapter.adaptQuery("ok BOOLEAN")).isEqualTo("ok BOOLEAN");
            assertThat(adapter.adaptDataType("BOOLEAN")).isEqualTo("BOOLEAN");
        }

        // Regression: adaptQuery matched dialect names case-insensitively but adaptDataType did not,
        // so "MySQL"/"PostgreSQL" adapted queries yet silently skipped every data-type mapping.
        @Test
        void dialectNamesAreCaseInsensitiveForDataTypes() {
            DatabaseAdapter adapter = new DatabaseAdapter("MySQL", "PostgreSQL");

            assertThat(adapter.adaptQuery("ok TINYINT")).isEqualTo("ok BOOLEAN");
            assertThat(adapter.adaptDataType("TINYINT")).isEqualTo("BOOLEAN");
        }
    }

    @Nested
    class ExternalAuth {

        private static final String ADMIN_EMAIL = "admin@smartcampus.edu";
        private static final String ADMIN_PASSWORD = "admin123";

        private FakeProvider provider;
        private AuthService auth;
        private ExternalAuthAdapter adapter;

        /** AuthService is a process-wide singleton; discard it so each test sees a clean instance. */
        private void resetAuthSingleton() throws ReflectiveOperationException {
            Field instance = AuthService.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        }

        @BeforeEach
        void setUp() throws ReflectiveOperationException {
            resetAuthSingleton();
            auth = AuthService.getInstance();
            provider = new FakeProvider();
            adapter = new ExternalAuthAdapter(provider, auth);
        }

        @AfterEach
        void tearDown() throws ReflectiveOperationException {
            resetAuthSingleton();
        }

        @Test
        void externalStudentIsConvertedAndCached() {
            provider.accept("jdoe", "pw", new ExternalUserInfo("STU000042", "Jane Q Doe", "jane@ext.edu", "STUDENT"));

            assertThat(adapter.authenticate("jdoe", "pw")).isTrue();
            provider.currentUser = "jdoe";

            User user = adapter.getCurrentUser();
            assertThat(user).isInstanceOf(Student.class);
            assertThat(((Student) user).getStudentId()).isEqualTo("STU000042");
            assertThat(user.getUserId()).isEqualTo("STU000042");
            assertThat(user.getFirstName()).isEqualTo("Jane");
            assertThat(user.getLastName()).isEqualTo("Q Doe");
            assertThat(user.getEmail()).isEqualTo("jane@ext.edu");
        }

        @Test
        void externalFacultyAndOtherUsersAreConverted() {
            provider.accept("prof", "pw", new ExternalUserInfo("PROF00007", "Alan Turing", "alan@ext.edu", "FACULTY"));
            provider.accept("guest", "pw", new ExternalUserInfo("G1", "Visiting Scholar", "guest@ext.edu", "VISITOR"));

            adapter.authenticate("prof", "pw");
            adapter.authenticate("guest", "pw");

            provider.currentUser = "prof";
            assertThat(adapter.getCurrentUser()).isInstanceOfSatisfying(Professor.class,
                p -> assertThat(p.getProfessorId()).isEqualTo("PROF00007"));
            provider.currentUser = "guest";
            User guest = adapter.getCurrentUser();
            assertThat(guest.getRole()).isEqualTo("VISITOR");
            assertThat(guest.getFullName()).isEqualTo("Visiting Scholar");
            guest.displayInfo();
        }

        @Test
        void fallsBackToInternalAuthentication() {
            assertThat(adapter.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD)).isTrue();

            User current = adapter.getCurrentUser();
            assertThat(current).isNotNull();
            assertThat(current.getEmail()).isEqualTo(ADMIN_EMAIL);
        }

        @Test
        void failsWhenBothSystemsReject() {
            assertThat(adapter.authenticate("nobody@x.edu", "wrong")).isFalse();
            assertThat(adapter.getCurrentUser()).isNull();
        }

        @Test
        void externalPermissionIsCheckedForExternalUsers() {
            provider.accept("jdoe", "pw", new ExternalUserInfo("STU000042", "Jane Doe", "jane@ext.edu", "STUDENT"));
            adapter.authenticate("jdoe", "pw");
            provider.currentUser = "jdoe";
            User jane = adapter.getCurrentUser();

            provider.grant("jane@ext.edu", "courses:READ");

            assertThat(adapter.hasPermission(jane, "courses", PermissionLevel.READ)).isTrue();
            assertThat(provider.permissionChecks).containsExactly("jane@ext.edu|courses:READ");
            // not granted externally and unknown internally
            assertThat(adapter.hasPermission(jane, "grades", PermissionLevel.ADMIN)).isFalse();
        }

        @Test
        void internalPermissionIsUsedForInternalUsers() {
            adapter.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);

            assertThat(adapter.hasPermission(adapter.getCurrentUser(), "anything", PermissionLevel.ADMIN)).isTrue();
            assertThat(provider.permissionChecks).isEmpty();
        }

        @Test
        void logoutClearsExternalAndInternalState() {
            provider.accept("jdoe", "pw", new ExternalUserInfo("STU000042", "Jane Doe", "jane@ext.edu", "STUDENT"));
            adapter.authenticate("jdoe", "pw");
            adapter.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
            provider.currentUser = "jdoe";

            adapter.logout();

            assertThat(provider.loggedOut).isTrue();
            assertThat(adapter.getCurrentUser()).isNull();
        }

        @Test
        void changePasswordDelegatesForCurrentUser() {
            assertThat(adapter.changePassword(ADMIN_PASSWORD, "n3w-Secret!")).isFalse(); // nobody logged in

            adapter.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);

            assertThat(adapter.changePassword(ADMIN_PASSWORD, "n3w-Secret!")).isTrue();
            assertThat(auth.authenticate(ADMIN_EMAIL, "n3w-Secret!").isSuccess()).isTrue();
        }

        @Test
        void valueObjectsExposeTheirFields() {
            ExternalPermissionResult denied = new ExternalPermissionResult(false, "no role");
            ExternalAuthResult result = new ExternalAuthResult(false, null);

            assertThat(denied.hasPermission()).isFalse();
            assertThat(denied.getReason()).isEqualTo("no role");
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getUserInfo()).isNull();
        }
    }

    /** In-memory stand-in for an external identity provider. */
    private static final class FakeProvider implements ExternalAuthProvider {
        private final Map<String, ExternalUserInfo> users = new HashMap<>();
        private final Map<String, String> passwords = new HashMap<>();
        private final List<String> grants = new ArrayList<>();
        private final List<String> permissionChecks = new ArrayList<>();
        private String currentUser;
        private boolean loggedOut;

        void accept(String username, String password, ExternalUserInfo info) {
            users.put(username, info);
            passwords.put(username, password);
        }

        void grant(String email, String permission) {
            grants.add(email + "|" + permission);
        }

        @Override
        public ExternalAuthResult authenticate(String username, String password) {
            boolean ok = password != null && password.equals(passwords.get(username));
            return new ExternalAuthResult(ok, ok ? users.get(username) : null);
        }

        @Override
        public ExternalPermissionResult checkPermission(String userEmail, String permission) {
            String key = userEmail + "|" + permission;
            permissionChecks.add(key);
            return new ExternalPermissionResult(grants.contains(key), "fake");
        }

        @Override
        public String getCurrentUsername() {
            return currentUser;
        }

        @Override
        public void logout() {
            loggedOut = true;
        }
    }
}
