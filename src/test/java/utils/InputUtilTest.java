package utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the interactive prompts with in-memory scanners; stdout is captured so that
 * re-prompt messages can be asserted on and so the build log stays clean.
 */
class InputUtilTest {

    private PrintStream originalOut;
    private Scanner originalScanner;
    private ByteArrayOutputStream out;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        originalScanner = InputUtil.getDefaultScanner();
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restore() {
        System.setOut(originalOut);
        InputUtil.setDefaultScanner(originalScanner);
    }

    private static Scanner input(String... lines) {
        return new Scanner(String.join("\n", lines) + "\n");
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void getStringTrimsAndRepromptsOnEmptyWhenNotAllowed() {
        assertThat(InputUtil.getString(input("   ", "  Ada  "), "Name: ", false)).isEqualTo("Ada");
        assertThat(output()).contains("Input cannot be empty");
        assertThat(InputUtil.getString(input(""), "Opt: ", true)).isEmpty();
    }

    @Test
    void getStringWithValidatorRepromptsUntilValid() {
        String result = InputUtil.getString(input("abc", "12345"), "Zip: ", s -> s.matches("\\d{5}"), "Bad zip");
        assertThat(result).isEqualTo("12345");
        assertThat(output()).contains("Bad zip");
    }

    @Test
    void getStringWithLengthBoundsUsesDefaultScanner() {
        InputUtil.setDefaultScanner(input("a", "abcdefghijk", "abcd"));
        assertThat(InputUtil.getString("Code: ", 2, 10)).isEqualTo("abcd");
        assertThat(output()).contains("between 2 and 10");
    }

    @Test
    void getIntRejectsNonIntegersAndOutOfRange() {
        assertThat(InputUtil.getInt(input("x", "4.5", " 42 "), "n: ")).isEqualTo(42);
        assertThat(output()).contains("Please enter a valid integer.");
        assertThat(InputUtil.getInt(input("0", "11", "10"), "n: ", 1, 10)).isEqualTo(10);
        assertThat(output()).contains("between 1 and 10");
    }

    @Test
    void getDoubleRejectsNonNumbersAndOutOfRange() {
        assertThat(InputUtil.getDouble(input("abc", "3.5"), "x: ")).isEqualTo(3.5);
        assertThat(InputUtil.getDouble(input("4.01", "-0.1", "4.0"), "gpa: ", 0.0, 4.0)).isEqualTo(4.0);
        assertThat(output()).contains("Please enter a valid number.");
    }

    @ParameterizedTest
    @CsvSource({"y, true", "YES, true", "true, true", "n, false", "No, false", "FALSE, false"})
    void getBooleanAcceptsCommonSpellings(String line, boolean expected) {
        assertThat(InputUtil.getBoolean(input(line), "Continue?")).isEqualTo(expected);
    }

    @Test
    void getBooleanRepromptsOnUnknownAnswer() {
        assertThat(InputUtil.getBoolean(input("maybe", "y"), "Continue?")).isTrue();
        assertThat(output()).contains("(y/n)").contains("Please enter 'y'");
    }

    @Test
    void getDateAndTimeRequireExactFormat() {
        assertThat(InputUtil.getDate(input("03/15/2024", "2024-13-01", "2024-03-15"), "Date"))
            .isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(InputUtil.getTime(input("2pm", "25:00", "14:30"), "Time")).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    void emailAndPhoneUseValidationRules() {
        InputUtil.setDefaultScanner(input("nope", "ada@campus.edu", "123", "555-123-4567"));
        assertThat(InputUtil.getEmail("Email: ")).isEqualTo("ada@campus.edu");
        assertThat(InputUtil.getPhoneNumber("Phone: ")).isEqualTo("555-123-4567");
        assertThat(output()).contains("valid email").contains("valid phone");
    }

    @Test
    void optionalPhoneReturnsNullForEmptyOrInvalid() {
        InputUtil.setDefaultScanner(input("", "123", "(555) 123-4567"));
        assertThat(InputUtil.getOptionalPhoneNumber("Phone")).isNull();
        assertThat(InputUtil.getOptionalPhoneNumber("Phone")).isNull();
        assertThat(InputUtil.getOptionalPhoneNumber("Phone")).isEqualTo("(555) 123-4567");
    }

    @Test
    void getChoiceIsOneBasedAndValidatesRange() {
        String[] options = {"Fall", "Spring", "Summer"};
        assertThat(InputUtil.getChoice(input("0", "4", "2"), "Pick", options)).isEqualTo("Spring");
        assertThat(InputUtil.getChoice(input("3"), "Pick", List.of("a", "b", "c"))).isEqualTo("c");
        assertThat(output()).contains("1. Fall").contains("3. Summer");
    }

    @Test
    void getMultipleChoicesDeduplicatesAndSkipsInvalidEntries() {
        String[] options = {"A", "B", "C"};
        List<String> chosen = InputUtil.getMultipleChoices(input("3, 1", "1", "9", "x", "done"), "Pick", options);
        assertThat(chosen).containsExactly("C", "A");
        assertThat(output()).contains("Already selected: A").contains("Invalid choice: 9");
    }

    @Test
    void confirmWithDefault() {
        assertThat(InputUtil.confirm(input(""), "Sure?", true)).isTrue();
        assertThat(InputUtil.confirm(input(""), "Sure?", false)).isFalse();
        assertThat(InputUtil.confirm(input("yes"), "Sure?", false)).isTrue();
        assertThat(InputUtil.confirm(input("whatever"), "Sure?", true)).isFalse();
        assertThat(output()).contains("(Y/n)").contains("(y/N)");
    }

    @Test
    void confirmWithoutDefaultDelegatesToBoolean() {
        InputUtil.setDefaultScanner(input("n"));
        assertThat(InputUtil.confirm("Delete?")).isFalse();
    }

    @Test
    void pauseConsumesOneLine() {
        InputUtil.setDefaultScanner(input("", "next"));
        InputUtil.pause();
        assertThat(InputUtil.getString("> ", true)).isEqualTo("next");
    }

    @Test
    void exhaustedInputSurfacesAsException() {
        assertThatThrownBy(() -> InputUtil.getInt(input("x"), "n: ")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void sanitizeInput() {
        assertThat(InputUtil.sanitizeInput("  <script>alert('x')</script>   hi  ")).isEqualTo("scriptalert(x)/script hi");
        assertThat(InputUtil.sanitizeInput("a \t\n b")).isEqualTo("a b");
        assertThat(InputUtil.sanitizeInput(null)).isNull();
    }

    @Test
    void displayFormatting() {
        assertThat(InputUtil.formatDate(null)).isEqualTo("N/A");
        assertThat(InputUtil.formatDate(LocalDate.of(2024, 3, 5))).matches("\\S+ 05, 2024");
        assertThat(InputUtil.formatTime(LocalTime.of(9, 5))).isEqualTo("09:05");
        assertThat(InputUtil.formatTime(null)).isEqualTo("N/A");
    }
}
