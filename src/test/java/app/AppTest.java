package app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the console application end to end by feeding scripted input and capturing output.
 * App reads from a static Scanner created at class-load time, so each run swaps in a fresh
 * Scanner over the scripted input; the real System streams are restored after every test.
 */
// Separate thread so a regressed (non-terminating) menu loop fails the test instead of hanging the build
@Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class AppTest {

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void captureStreams() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    /** Runs App.main with the given lines as console input and returns everything printed to stdout. */
    private String run(String... lines) throws Exception {
        String input = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        Field scanner = App.class.getDeclaredField("scanner");
        scanner.setAccessible(true);
        scanner.set(null, new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));

        App.main(new String[0]);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Nested
    class StartupAndExit {

        @Test
        void exitImmediatelyAfterFeatureDemonstration() throws Exception {
            String output = run("0");

            assertThat(output)
                    .contains("Welcome to SmartCampus University Management System v1.0.0")
                    .contains("POLYMORPHISM DEMONSTRATION")
                    .contains("Processing user: Alice Johnson")
                    .contains("VALIDATION DEMONSTRATION")
                    .contains("Email 'valid@email.com' is VALID")
                    .contains("Email 'invalid-email' is INVALID")
                    .contains("EXCEPTION HANDLING DEMONSTRATION")
                    .contains("Caught UserNotFoundException")
                    .contains("BUSINESS LOGIC DEMONSTRATION")
                    .contains("Student STU123456 enrollment: SUCCESS")
                    .contains("Enrolled students: 3")
                    .contains("=== INTERACTIVE MENU ===")
                    .endsWith("Thank you for using SmartCampus University Management System!" + System.lineSeparator());
            assertThat(output).doesNotContain("Error during demonstration");
            assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
        }

        // Regression: on end of input the menu caught NoSuchElementException as a generic error
        // and looped forever printing the menu; it must terminate instead.
        @Test
        void endOfInputTerminatesTheMenu() throws Exception {
            String output = run();

            assertThat(output).contains("=== INTERACTIVE MENU ===")
                    .contains("Thank you for using")
                    .doesNotContain("Error: No line found");
        }

        @Test
        void endOfInputInsideASubmenuAlsoTerminates() throws Exception {
            String output = run("1", "Jane");

            assertThat(output).contains("Error creating student: No line found").contains("Thank you for using");
        }
    }

    @Nested
    class MenuNavigation {

        @Test
        void nonNumericAndUnknownChoicesAreRejected() throws Exception {
            String output = run("abc", "9", "0");

            assertThat(output).contains("Please enter a valid number.").contains("Invalid choice. Please try again.");
        }

        @Test
        void systemInformationIsShown() throws Exception {
            String output = run("6", "0");

            assertThat(output).contains("--- System Information ---")
                    .contains("Version: 1.0.0")
                    .contains("Java Version: " + System.getProperty("java.version"));
        }
    }

    @Nested
    class CreateStudent {

        @Test
        void validInputCreatesStudent() throws Exception {
            String output = run("1", "Jane", "Doe", "jane.doe@university.edu", "", "STU654321", "Mathematics", "1", "0");

            assertThat(output).contains("Student created successfully").contains("Jane");
        }

        @Test
        void invalidEmailIsReported() throws Exception {
            String output = run("1", "Jane", "Doe", "not-an-email", "", "STU654321", "Mathematics", "1", "0");

            assertThat(output).contains("Error creating student:").doesNotContain("Student created successfully");
        }

        @Test
        void invalidPhoneIsReported() throws Exception {
            String output = run("1", "Jane", "Doe", "jane@university.edu", "12", "STU654321", "Mathematics", "1", "0");

            assertThat(output).contains("Error creating student:").doesNotContain("Student created successfully");
        }

        @Test
        void invalidStudentIdIsReported() throws Exception {
            String output = run("1", "Jane", "Doe", "jane@university.edu", "", "BAD", "Mathematics", "1", "0");

            assertThat(output).contains("Error creating student:").contains("BAD");
        }

        @Test
        void outOfRangeAcademicYearIsReported() throws Exception {
            String output = run("1", "Jane", "Doe", "jane@university.edu", "", "STU654321", "Mathematics", "99", "0");

            assertThat(output).contains("Error creating student: Invalid academic year choice");
        }
    }

    @Nested
    class CreateProfessor {

        @Test
        void validInputCreatesProfessor() throws Exception {
            String output = run("2", "Ada", "Lovelace", "ada@university.edu", "(555) 222-3333",
                    "PROF54321", "DEPT_CS", "Algorithms", "1", "0");

            assertThat(output).contains("Professor created successfully").contains("Lovelace");
        }

        @Test
        void invalidRankIsReported() throws Exception {
            String output = run("2", "Ada", "Lovelace", "ada@university.edu", "(555) 222-3333",
                    "PROF54321", "DEPT_CS", "Algorithms", "0", "0");

            assertThat(output).contains("Error creating professor: Invalid academic rank choice");
        }

        @Test
        void invalidEmailPhoneAndIdAreReported() throws Exception {
            String output = run(
                    "2", "Ada", "Lovelace", "bad", "(555) 222-3333", "PROF54321", "D", "S", "1",
                    "2", "Ada", "Lovelace", "ada@university.edu", "nope", "PROF54321", "D", "S", "1",
                    "2", "Ada", "Lovelace", "ada@university.edu", "(555) 222-3333", "X1", "D", "S", "1",
                    "0");

            assertThat(output.split("Error creating professor:", -1)).hasSize(4);
            assertThat(output).doesNotContain("Professor created successfully");
        }
    }

    @Nested
    class CreateCourse {

        @Test
        void validInputCreatesCourse() throws Exception {
            String output = run("3", "CS201", "Data Structures", "Lists and trees", "3", "DEPT_CS", "0");

            assertThat(output).contains("Course created successfully").contains("Data Structures");
        }

        @Test
        void invalidCodeAndCreditsAreReported() throws Exception {
            String output = run(
                    "3", "101CS", "Data Structures", "d", "3", "DEPT_CS",
                    "3", "CS201", "Data Structures", "d", "12", "DEPT_CS",
                    "3", "CS201", "Data Structures", "d", "three",
                    "0");

            assertThat(output.split("Error creating course:", -1)).hasSize(4);
            assertThat(output).doesNotContain("Course created successfully");
        }
    }

    @Nested
    class CreateDepartment {

        @Test
        void validInputCreatesDepartment() throws Exception {
            String output = run("4", "MATH", "Mathematics", "Numbers", "Building B", "0");

            assertThat(output).contains("Department created successfully").contains("Mathematics");
        }

        @Test
        void invalidCodeIsReported() throws Exception {
            String output = run("4", "math1", "Mathematics", "Numbers", "Building B", "0");

            assertThat(output).contains("Error creating department:").doesNotContain("Department created successfully");
        }
    }

    @Nested
    class ValidationTesting {

        @Test
        void validValuesAreReportedValidAndPhoneIsNormalized() throws Exception {
            String output = run("5", "x@y.edu", "(555) 123-4567", "3.5", "0");

            assertThat(output).contains("Email 'x@y.edu' is VALID")
                    .contains("Phone '(555) 123-4567' is VALID")
                    .contains("Normalized format:")
                    .contains("GPA 3.5 is VALID");
        }

        @Test
        void invalidValuesAreReportedInvalid() throws Exception {
            String output = run("5", "nope", "123", "7.0", "5", "a@b.edu", "x", "abc", "0");

            assertThat(output).contains("Email 'nope' is INVALID")
                    .contains("Phone '123' is INVALID")
                    .contains("GPA 7.0 is INVALID")
                    .contains("Invalid number format")
                    .doesNotContain("Normalized format:");
        }
    }
}
