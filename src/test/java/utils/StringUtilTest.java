package utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class StringUtilTest {

    @Test
    void constructorIsNotAccessible() throws Exception {
        var ctor = StringUtil.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThatThrownBy(ctor::newInstance).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    class Blankness {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", " \n "})
        void blankInputs(String s) {
            assertThat(StringUtil.isBlank(s)).isTrue();
            assertThat(StringUtil.isNotBlank(s)).isFalse();
        }

        @Test
        void emptyVsBlank() {
            assertThat(StringUtil.isEmpty(" ")).isFalse();
            assertThat(StringUtil.isEmpty("")).isTrue();
            assertThat(StringUtil.isEmpty(null)).isTrue();
            assertThat(StringUtil.isNotEmpty(" ")).isTrue();
        }

        @Test
        void varargAggregates() {
            assertThat(StringUtil.anyBlank("a", " ")).isTrue();
            assertThat(StringUtil.anyBlank("a", "b")).isFalse();
            assertThat(StringUtil.allBlank(null, "", " ")).isTrue();
            assertThat(StringUtil.allBlank(null, "x")).isFalse();
            assertThat(StringUtil.anyNotBlank(null, "x")).isTrue();
            assertThat(StringUtil.allNotBlank("a", "b")).isTrue();
            assertThat(StringUtil.allNotBlank("a", null)).isFalse();
        }

        @Test
        void defaults() {
            assertThat(StringUtil.defaultIfNull(null, "d")).isEqualTo("d");
            assertThat(StringUtil.defaultIfNull("", "d")).isEmpty();
            assertThat(StringUtil.defaultIfEmpty("", "d")).isEqualTo("d");
            assertThat(StringUtil.defaultIfEmpty(" ", "d")).isEqualTo(" ");
            assertThat(StringUtil.defaultIfBlank(" ", "d")).isEqualTo("d");
            assertThat(StringUtil.defaultString(null)).isEmpty();
            assertThat(StringUtil.firstNonBlank(null, " ", "x", "y")).isEqualTo("x");
            assertThat(StringUtil.firstNonBlank(null, " ")).isEmpty();
        }
    }

    @Nested
    class TrimmingAndStripping {
        @Test
        void trimVariants() {
            assertThat(StringUtil.trim(null)).isNull();
            assertThat(StringUtil.trim("  a ")).isEqualTo("a");
            assertThat(StringUtil.trimToEmpty(null)).isEmpty();
            assertThat(StringUtil.trimToNull("   ")).isNull();
            assertThat(StringUtil.trimToNull(" a ")).isEqualTo("a");
        }

        @Test
        void whitespaceHandling() {
            assertThat(StringUtil.removeWhitespace(" a b\tc\n")).isEqualTo("abc");
            assertThat(StringUtil.normalizeWhitespace("  a   b\t\tc  ")).isEqualTo("a b c");
            assertThat(StringUtil.stripStart("  a  ")).isEqualTo("a  ");
            assertThat(StringUtil.stripEnd("  a  ")).isEqualTo("  a");
            assertThat(StringUtil.removeWhitespace(null)).isNull();
        }

        @Test
        void stripCharsTreatsArgumentAsASetOfLiteralCharacters() {
            assertThat(StringUtil.strip("xxhixyx", "xy")).isEqualTo("hi");
            assertThat(StringUtil.stripStart("--a--", "-")).isEqualTo("a--");
            assertThat(StringUtil.stripEnd("--a--", "-")).isEqualTo("--a");
            // regex metacharacters must be treated literally
            assertThat(StringUtil.strip("]^a^]", "]^")).isEqualTo("a");
            assertThat(StringUtil.strip(".*a*.", ".*")).isEqualTo("a");
            assertThat(StringUtil.strip("abc", null)).isEqualTo("abc");
        }
    }

    @Nested
    class CaseConversion {
        @Test
        void titleAndSentenceCase() {
            assertThat(StringUtil.titleCase("hELLO wORLD")).isEqualTo("Hello World");
            assertThat(StringUtil.sentenceCase("hELLO wORLD")).isEqualTo("Hello world");
            assertThat(StringUtil.titleCase("")).isEmpty();
            assertThat(StringUtil.titleCase(null)).isNull();
        }

        @ParameterizedTest
        @CsvSource({
            "hello world, helloWorld, HelloWorld",
            "first_name, firstName, FirstName",
            "kebab-case-string, kebabCaseString, KebabCaseString",
            "single, single, Single"
        })
        void camelAndPascal(String in, String camel, String pascal) {
            assertThat(StringUtil.camelCase(in)).isEqualTo(camel);
            assertThat(StringUtil.pascalCase(in)).isEqualTo(pascal);
        }

        @ParameterizedTest
        @CsvSource({
            "helloWorld, hello_world, hello-world",
            "Hello World, hello_world, hello-world",
            "studentId, student_id, student-id"
        })
        void snakeAndKebab(String in, String snake, String kebab) {
            assertThat(StringUtil.snakeCase(in)).isEqualTo(snake);
            assertThat(StringUtil.kebabCase(in)).isEqualTo(kebab);
        }
    }

    @Nested
    class Padding {
        @Test
        void padsToRequestedWidth() {
            assertThat(StringUtil.leftPad("7", 3, '0')).isEqualTo("007");
            assertThat(StringUtil.rightPad("ab", 4)).isEqualTo("ab  ");
            assertThat(StringUtil.leftPad("abcd", 2)).isEqualTo("abcd");
            assertThat(StringUtil.leftPad(null, 2)).isNull();
        }

        @ParameterizedTest
        @CsvSource({"ab, 6, '**ab**'", "ab, 5, '*ab**'", "abc, 2, 'abc'"})
        void centerPutsExtraPaddingOnTheRight(String s, int size, String expected) {
            assertThat(StringUtil.center(s, size, '*')).isEqualTo(expected);
        }
    }

    @Nested
    class Truncation {
        @Test
        void truncate() {
            assertThat(StringUtil.truncate("abcdef", 3)).isEqualTo("abc");
            assertThat(StringUtil.truncate("abc", 3)).isEqualTo("abc");
            assertThat(StringUtil.truncate(null, 3)).isNull();
        }

        @Test
        void truncateWithSuffixNeverExceedsMaxLength() {
            assertThat(StringUtil.truncate("Hello World", 8, "...")).isEqualTo("Hello...").hasSize(8);
            assertThat(StringUtil.ellipsize("Hello World", 11)).isEqualTo("Hello World");
            assertThat(StringUtil.truncate("Hello World", 6, null)).isEqualTo("Hel...");
        }

        @Test
        void truncateWithSuffixLongerThanMaxLengthDoesNotThrow() {
            assertThat(StringUtil.truncate("Hello World", 2, "...")).hasSizeLessThanOrEqualTo(2);
            assertThat(StringUtil.ellipsize("Hello World", 0)).isEmpty();
        }

        @Test
        void truncateWordsBreaksAtWordBoundary() {
            assertThat(StringUtil.truncateWords("The quick brown fox", 12)).isEqualTo("The quick...");
            assertThat(StringUtil.truncateWords("short", 10)).isEqualTo("short");
            assertThat(StringUtil.truncateWords("Supercalifragilistic", 5)).isEqualTo("Super");
        }
    }

    @Nested
    class JoinSplitRepeat {
        @Test
        void repeat() {
            assertThat(StringUtil.repeat("ab", 3)).isEqualTo("ababab");
            assertThat(StringUtil.repeat("ab", 0)).isEmpty();
            assertThat(StringUtil.repeat("ab", -1)).isEmpty();
            assertThat(StringUtil.repeat('x', 2)).isEqualTo("xx");
            assertThat(StringUtil.repeat(null, 2)).isNull();
        }

        @Test
        void join() {
            assertThat(StringUtil.join(new String[]{"a", "b"}, "|")).isEqualTo("a|b");
            assertThat(StringUtil.join(List.of("a", "b"), "-")).isEqualTo("a-b");
            assertThat(StringUtil.joinComma("a", "b", "c")).isEqualTo("a, b, c");
            assertThat(StringUtil.joinSpace("a", "b")).isEqualTo("a b");
            assertThat(StringUtil.join((String[]) null, ",")).isNull();
        }

        @Test
        void splitTreatsDelimiterLiterally() {
            assertThat(StringUtil.splitAndTrim(" a | b |c ", "|")).containsExactly("a", "b", "c");
            assertThat(StringUtil.splitAndFilter("a,, ,b,", ",")).containsExactly("a", "b");
            assertThat(StringUtil.splitAndTrim("1.2.3", ".")).containsExactly("1", "2", "3");
            assertThat(StringUtil.splitAndTrim(null, ",")).isNull();
        }
    }

    @Nested
    class Comparison {
        @Test
        void nullSafeComparisons() {
            assertThat(StringUtil.equalsIgnoreCase(null, null)).isTrue();
            assertThat(StringUtil.equalsIgnoreCase("A", null)).isFalse();
            assertThat(StringUtil.equalsIgnoreCase("abc", "ABC")).isTrue();
            assertThat(StringUtil.equals(null, null)).isTrue();
            assertThat(StringUtil.equals("a", "A")).isFalse();
            assertThat(StringUtil.startsWithIgnoreCase("Hello", "hE")).isTrue();
            assertThat(StringUtil.endsWithIgnoreCase("Hello", "LO")).isTrue();
            assertThat(StringUtil.containsIgnoreCase("Hello", "ELL")).isTrue();
            assertThat(StringUtil.containsIgnoreCase(null, "a")).isFalse();
        }
    }

    @Nested
    class Validation {
        @ParameterizedTest
        @ValueSource(strings = {"john.doe@example.com", "a+b@sub.domain.org", "x_y@d.io"})
        void validEmails(String email) {
            assertThat(StringUtil.isValidEmail(email)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"plain", "a@b", "@example.com", "a@example.c", "a b@example.com"})
        void invalidEmails(String email) {
            assertThat(StringUtil.isValidEmail(email)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"+14155552671", "(415) 555-2671", "415 555 2671"})
        void validPhones(String phone) {
            assertThat(StringUtil.isValidPhoneNumber(phone)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0123456", "abc", "+", "1"})
        void invalidPhones(String phone) {
            assertThat(StringUtil.isValidPhoneNumber(phone)).isFalse();
        }

        @Test
        void characterClasses() {
            assertThat(StringUtil.isAlpha("abcXYZ")).isTrue();
            assertThat(StringUtil.isAlpha("abc1")).isFalse();
            assertThat(StringUtil.isAlpha("")).isFalse();
            assertThat(StringUtil.isAlphanumeric("abc123")).isTrue();
            assertThat(StringUtil.isAlphanumeric("abc 123")).isFalse();
            assertThat(StringUtil.isNumeric("0123")).isTrue();
            assertThat(StringUtil.isNumeric("-1")).isFalse();
        }

        @ParameterizedTest
        @CsvSource({"1.5, true", ".5, true", "10, true", "1., false", "-1.5, false", "1.2.3, false"})
        void isDecimal(String s, boolean expected) {
            assertThat(StringUtil.isDecimal(s)).isEqualTo(expected);
        }

        @Test
        void integerAndDoubleParsing() {
            assertThat(StringUtil.isInteger("-42")).isTrue();
            assertThat(StringUtil.isInteger("2147483648")).isFalse();
            assertThat(StringUtil.isInteger("4.2")).isFalse();
            assertThat(StringUtil.isDouble("-4.2e3")).isTrue();
            assertThat(StringUtil.isDouble("abc")).isFalse();
            assertThat(StringUtil.isDouble(null)).isFalse();
        }
    }

    @Nested
    class Encoding {
        @Test
        void normalizeRemovesDiacritics() {
            assertThat(StringUtil.normalize("Café Ångström")).isEqualTo("Cafe Angstrom");
            assertThat(StringUtil.toAscii("naïve – résumé")).isEqualTo("naive  resume");
        }

        @Test
        void htmlEscapeRoundTrip() {
            String raw = "<a href=\"x\">Tom & Jerry's</a>";
            String escaped = StringUtil.escapeHtml(raw);
            assertThat(escaped).isEqualTo("&lt;a href=&quot;x&quot;&gt;Tom &amp; Jerry&#x27;s&lt;/a&gt;");
            assertThat(StringUtil.unescapeHtml(escaped)).isEqualTo(raw);
        }

        @Test
        void urlEncodeRoundTrip() {
            String raw = "a b&c=d/é";
            String encoded = StringUtil.urlEncode(raw);
            assertThat(encoded).isEqualTo("a+b%26c%3Dd%2F%C3%A9");
            assertThat(StringUtil.urlDecode(encoded)).isEqualTo(raw);
        }
    }

    @Nested
    class Misc {
        @Test
        void reverse() {
            assertThat(StringUtil.reverse("abc")).isEqualTo("cba");
            assertThat(StringUtil.reverse(null)).isNull();
        }

        @ParameterizedTest
        @CsvSource({"aaaa, aa, 2", "abcabc, abc, 2", "abc, d, 0", "abc, abcd, 0"})
        void countOccurrencesIsNonOverlapping(String s, String sub, int expected) {
            assertThat(StringUtil.countOccurrences(s, sub)).isEqualTo(expected);
        }

        @Test
        void countOccurrencesOfEmptyStringTerminates() {
            int count = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> StringUtil.countOccurrences("abc", ""));
            assertThat(count).isZero();
        }

        @Test
        void substringBetween() {
            assertThat(StringUtil.substringBetween("x[abc]y", "[", "]")).isEqualTo("abc");
            assertThat(StringUtil.substringBetween("x[abc", "[", "]")).isNull();
            assertThat(StringUtil.substringBetween("xabc]", "[", "]")).isNull();
            assertThat(StringUtil.substringBetween("<<>>", "<<", ">>")).isEmpty();
        }

        @Test
        void prefixAndSuffixAreIdempotent() {
            assertThat(StringUtil.addPrefix("CS101", "CS")).isEqualTo("CS101");
            assertThat(StringUtil.addPrefix("101", "CS")).isEqualTo("CS101");
            assertThat(StringUtil.addSuffix("file.txt", ".txt")).isEqualTo("file.txt");
            assertThat(StringUtil.addSuffix("file", ".txt")).isEqualTo("file.txt");
            assertThat(StringUtil.removePrefix("CS101", "CS")).isEqualTo("101");
            assertThat(StringUtil.removePrefix("101", "CS")).isEqualTo("101");
            assertThat(StringUtil.removeSuffix("file.txt", ".txt")).isEqualTo("file");
            assertThat(StringUtil.removeSuffix(StringUtil.addSuffix("x", "!"), "!")).isEqualTo("x");
        }

        @ParameterizedTest
        @CsvSource({"kitten, sitting, 3", "'', abc, 3", "abc, abc, 0", "flaw, lawn, 2", "abc, '', 3"})
        void levenshtein(String a, String b, int expected) {
            assertThat(StringUtil.levenshteinDistance(a, b)).isEqualTo(expected);
            assertThat(StringUtil.levenshteinDistance(b, a)).isEqualTo(expected);
        }

        @Test
        void levenshteinNull() {
            assertThat(StringUtil.levenshteinDistance(null, "a")).isEqualTo(-1);
        }

        @Test
        void similarity() {
            assertThat(StringUtil.similarity("abc", "abc")).isEqualTo(1.0);
            assertThat(StringUtil.similarity("", "")).isEqualTo(1.0);
            assertThat(StringUtil.similarity(null, null)).isEqualTo(1.0);
            assertThat(StringUtil.similarity("abc", null)).isEqualTo(0.0);
            assertThat(StringUtil.similarity("abcd", "abcf")).isEqualTo(0.75);
            assertThat(StringUtil.similarity("abc", "xyz")).isEqualTo(0.0);
        }

        @Test
        void randomStringRespectsLengthAndCharset() {
            assertThat(StringUtil.randomString(50, "ab")).hasSize(50).matches("[ab]+");
            assertThat(StringUtil.randomString(20)).hasSize(20).matches("[A-Za-z0-9]+");
            assertThat(StringUtil.randomString(0)).isEmpty();
            assertThat(StringUtil.randomString(5, "")).isEmpty();
        }

        @Test
        void masking() {
            assertThat(StringUtil.mask("1234567890", '#', 2, 3)).isEqualTo("12#####890");
            assertThat(StringUtil.mask("abc", '*', 2, 2)).isEqualTo("abc");
            assertThat(StringUtil.maskEmail("john.doe@example.com")).isEqualTo("j******e@example.com");
            assertThat(StringUtil.maskEmail("ab@example.com")).isEqualTo("a*@example.com");
            assertThat(StringUtil.maskEmail("not-an-email")).isEqualTo("not-an-email");
            assertThat(StringUtil.maskPhoneNumber("5551234567")).isEqualTo("******4567");
            assertThat(StringUtil.maskPhoneNumber("123")).isEqualTo("123");
        }
    }
}
