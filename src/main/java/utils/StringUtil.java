// File location: src/main/java/utils/StringUtil.java
package utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.text.Normalizer;

/**
 * Utility class for string manipulation and validation
 * Provides common string operations for academic data processing
 */
public final class StringUtil {
    
    private StringUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== CONSTANTS ====================
    
    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String NEWLINE = "\n";
    public static final String CARRIAGE_RETURN = "\r";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    
    // Common patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^[A-Za-z]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d*\\.?\\d+$");
    
    // ==================== NULL AND EMPTY CHECKS ====================
    
    /**
     * Checks if string is null
     */
    public static boolean isNull(String str) {
        return str == null;
    }
    
    /**
     * Checks if string is not null
     */
    public static boolean isNotNull(String str) {
        return str != null;
    }
    
    /**
     * Checks if string is empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * Checks if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * Checks if string is blank (null, empty, or only whitespace)
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Checks if string is not blank
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * Checks if any of the strings is blank
     */
    public static boolean anyBlank(String... strings) {
        return Arrays.stream(strings).anyMatch(StringUtil::isBlank);
    }
    
    /**
     * Checks if all strings are blank
     */
    public static boolean allBlank(String... strings) {
        return Arrays.stream(strings).allMatch(StringUtil::isBlank);
    }
    
    /**
     * Checks if any of the strings is not blank
     */
    public static boolean anyNotBlank(String... strings) {
        return Arrays.stream(strings).anyMatch(StringUtil::isNotBlank);
    }
    
    /**
     * Checks if all strings are not blank
     */
    public static boolean allNotBlank(String... strings) {
        return Arrays.stream(strings).allMatch(StringUtil::isNotBlank);
    }
    
    // ==================== DEFAULT VALUES ====================
    
    /**
     * Returns string or default if null
     */
    public static String defaultIfNull(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }
    
    /**
     * Returns string or default if empty
     */
    public static String defaultIfEmpty(String str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }
    
    /**
     * Returns string or default if blank
     */
    public static String defaultIfBlank(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }
    
    /**
     * Returns string or empty string if null
     */
    public static String defaultString(String str) {
        return defaultIfNull(str, EMPTY);
    }
    
    /**
     * Returns first non-blank string
     */
    public static String firstNonBlank(String... strings) {
        return Arrays.stream(strings)
            .filter(StringUtil::isNotBlank)
            .findFirst()
            .orElse(EMPTY);
    }
    
    // ==================== TRIMMING AND CLEANING ====================
    
    /**
     * Trims string safely (handles null)
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }
    
    /**
     * Trims string and returns empty if null
     */
    public static String trimToEmpty(String str) {
        return str == null ? EMPTY : str.trim();
    }
    
    /**
     * Trims string and returns null if empty
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }
    
    /**
     * Removes all whitespace from string
     */
    public static String removeWhitespace(String str) {
        if (str == null) return null;
        return str.replaceAll("\\s+", EMPTY);
    }
    
    /**
     * Normalizes whitespace (collapses multiple spaces to single space)
     */
    public static String normalizeWhitespace(String str) {
        if (str == null) return null;
        return str.replaceAll("\\s+", SPACE).trim();
    }
    
    /**
     * Removes leading whitespace
     */
    public static String stripStart(String str) {
        if (str == null) return null;
        return str.replaceAll("^\\s+", EMPTY);
    }
    
    /**
     * Removes trailing whitespace
     */
    public static String stripEnd(String str) {
        if (str == null) return null;
        return str.replaceAll("\\s+$", EMPTY);
    }
    
    /**
     * Removes specified characters from start and end
     */
    public static String strip(String str, String stripChars) {
        if (str == null || stripChars == null) return str;
        return stripEnd(stripStart(str, stripChars), stripChars);
    }
    
    /**
     * Removes specified characters from start
     */
    public static String stripStart(String str, String stripChars) {
        if (str == null || stripChars == null) return str;
        String pattern = "^[" + Pattern.quote(stripChars) + "]+";
        return str.replaceAll(pattern, EMPTY);
    }
    
    /**
     * Removes specified characters from end
     */
    public static String stripEnd(String str, String stripChars) {
        if (str == null || stripChars == null) return str;
        String pattern = "[" + Pattern.quote(stripChars) + "]+$";
        return str.replaceAll(pattern, EMPTY);
    }
    
    // ==================== CASE CONVERSION ====================
    
    /**
     * Converts to uppercase safely
     */
    public static String upperCase(String str) {
        return str == null ? null : str.toUpperCase();
    }
    
    /**
     * Converts to lowercase safely
     */
    public static String lowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }
    
    /**
     * Converts to title case (first letter of each word capitalized)
     */
    public static String titleCase(String str) {
        if (str == null) return null;
        if (str.isEmpty()) return str;
        
        return Arrays.stream(str.split("\\s+"))
            .map(word -> word.isEmpty() ? word : 
                 Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
            .collect(Collectors.joining(SPACE));
    }
    
    /**
     * Converts to sentence case (first letter capitalized)
     */
    public static String sentenceCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }
    
    /**
     * Converts to camelCase
     */
    public static String camelCase(String str) {
        if (str == null) return null;
        if (str.isEmpty()) return str;
        
        String[] words = str.split("\\s+|_|-");
        StringBuilder result = new StringBuilder(lowerCase(words[0]));
        
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)))
                      .append(words[i].substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
    
    /**
     * Converts to PascalCase
     */
    public static String pascalCase(String str) {
        if (str == null) return null;
        String camel = camelCase(str);
        if (camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }
    
    /**
     * Converts to snake_case
     */
    public static String snakeCase(String str) {
        if (str == null) return null;
        return str.replaceAll("\\s+", "_")
                 .replaceAll("([a-z])([A-Z])", "$1_$2")
                 .toLowerCase();
    }
    
    /**
     * Converts to kebab-case
     */
    public static String kebabCase(String str) {
        if (str == null) return null;
        return str.replaceAll("\\s+", "-")
                 .replaceAll("([a-z])([A-Z])", "$1-$2")
                 .toLowerCase();
    }
    
    // ==================== PADDING AND ALIGNMENT ====================
    
    /**
     * Left pads string with spaces to specified length
     */
    public static String leftPad(String str, int size) {
        return leftPad(str, size, ' ');
    }
    
    /**
     * Left pads string with specified character to specified length
     */
    public static String leftPad(String str, int size, char padChar) {
        if (str == null) return null;
        int pads = size - str.length();
        if (pads <= 0) return str;
        return repeat(String.valueOf(padChar), pads) + str;
    }
    
    /**
     * Right pads string with spaces to specified length
     */
    public static String rightPad(String str, int size) {
        return rightPad(str, size, ' ');
    }
    
    /**
     * Right pads string with specified character to specified length
     */
    public static String rightPad(String str, int size, char padChar) {
        if (str == null) return null;
        int pads = size - str.length();
        if (pads <= 0) return str;
        return str + repeat(String.valueOf(padChar), pads);
    }
    
    /**
     * Centers string within specified length
     */
    public static String center(String str, int size) {
        return center(str, size, ' ');
    }
    
    /**
     * Centers string within specified length using specified character
     */
    public static String center(String str, int size, char padChar) {
        if (str == null) return null;
        int strLen = str.length();
        if (strLen >= size) return str;
        
        int pads = size - strLen;
        int leftPads = pads / 2;
        int rightPads = pads - leftPads;
        
        return repeat(String.valueOf(padChar), leftPads) + str + 
               repeat(String.valueOf(padChar), rightPads);
    }
    
    // ==================== TRUNCATION ====================
    
    /**
     * Truncates string to specified length
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength);
    }
    
    /**
     * Truncates string to specified length with ellipsis
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (str == null) return null;
        if (suffix == null) suffix = "...";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - suffix.length()) + suffix;
    }
    
    /**
     * Truncates string with ellipsis
     */
    public static String ellipsize(String str, int maxLength) {
        return truncate(str, maxLength, "...");
    }
    
    /**
     * Truncates words (doesn't break words)
     */
    public static String truncateWords(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        
        int lastSpace = str.lastIndexOf(' ', maxLength);
        if (lastSpace == -1) return truncate(str, maxLength);
        
        return str.substring(0, lastSpace) + "...";
    }
    
    // ==================== REPETITION ====================
    
    /**
     * Repeats string specified number of times
     */
    public static String repeat(String str, int count) {
        if (str == null) return null;
        if (count <= 0) return EMPTY;
        return str.repeat(count);
    }
    
    /**
     * Repeats character specified number of times
     */
    public static String repeat(char ch, int count) {
        if (count <= 0) return EMPTY;
        return String.valueOf(ch).repeat(count);
    }
    
    // ==================== JOINING AND SPLITTING ====================
    
    /**
     * Joins array of strings with delimiter
     */
    public static String join(String[] array, String delimiter) {
        if (array == null) return null;
        return String.join(delimiter, array);
    }
    
    /**
     * Joins collection of strings with delimiter
     */
    public static String join(Collection<String> collection, String delimiter) {
        if (collection == null) return null;
        return String.join(delimiter, collection);
    }
    
    /**
     * Joins with comma separator
     */
    public static String joinComma(String... strings) {
        return join(Arrays.asList(strings), ", ");
    }
    
    /**
     * Joins with space separator
     */
    public static String joinSpace(String... strings) {
        return join(Arrays.asList(strings), SPACE);
    }
    
    /**
     * Splits string by delimiter and trims each part
     */
    public static String[] splitAndTrim(String str, String delimiter) {
        if (str == null) return null;
        return Arrays.stream(str.split(Pattern.quote(delimiter)))
                    .map(String::trim)
                    .toArray(String[]::new);
    }
    
    /**
     * Splits string by delimiter and filters out empty parts
     */
    public static String[] splitAndFilter(String str, String delimiter) {
        if (str == null) return null;
        return Arrays.stream(str.split(Pattern.quote(delimiter)))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
    }
    
    // ==================== COMPARISON ====================
    
    /**
     * Compares strings ignoring case
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equalsIgnoreCase(str2);
    }
    
    /**
     * Compares strings safely (handles null)
     */
    public static boolean equals(String str1, String str2) {
        return Objects.equals(str1, str2);
    }
    
    /**
     * Checks if string starts with prefix (case insensitive)
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null) return false;
        return str.toLowerCase().startsWith(prefix.toLowerCase());
    }
    
    /**
     * Checks if string ends with suffix (case insensitive)
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null) return false;
        return str.toLowerCase().endsWith(suffix.toLowerCase());
    }
    
    /**
     * Checks if string contains substring (case insensitive)
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;
        return str.toLowerCase().contains(searchStr.toLowerCase());
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates phone number format
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone.replaceAll("\\s|-|\\(|\\)", "")).matches();
    }
    
    /**
     * Checks if string contains only alphabetic characters
     */
    public static boolean isAlpha(String str) {
        if (str == null) return false;
        return ALPHA_PATTERN.matcher(str).matches();
    }
    
    /**
     * Checks if string contains only alphanumeric characters
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null) return false;
        return ALPHANUMERIC_PATTERN.matcher(str).matches();
    }
    
    /**
     * Checks if string is numeric
     */
    public static boolean isNumeric(String str) {
        if (str == null) return false;
        return NUMERIC_PATTERN.matcher(str).matches();
    }
    
    /**
     * Checks if string is decimal number
     */
    public static boolean isDecimal(String str) {
        if (str == null) return false;
        return DECIMAL_PATTERN.matcher(str).matches();
    }
    
    /**
     * Checks if string is a valid integer
     */
    public static boolean isInteger(String str) {
        if (str == null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Checks if string is a valid double
     */
    public static boolean isDouble(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // ==================== ENCODING AND NORMALIZATION ====================
    
    /**
     * Normalizes string by removing accents and diacritics
     */
    public static String normalize(String str) {
        if (str == null) return null;
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                        .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
    
    /**
     * Converts string to ASCII-safe format
     */
    public static String toAscii(String str) {
        if (str == null) return null;
        return normalize(str).replaceAll("[^\\p{ASCII}]", "");
    }
    
    /**
     * Escapes HTML special characters
     */
    public static String escapeHtml(String str) {
        if (str == null) return null;
        return str.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
                 .replace("\"", "&quot;")
                 .replace("'", "&#x27;");
    }
    
    /**
     * Unescapes HTML entities
     */
    public static String unescapeHtml(String str) {
        if (str == null) return null;
        return str.replace("&amp;", "&")
                 .replace("&lt;", "<")
                 .replace("&gt;", ">")
                 .replace("&quot;", "\"")
                 .replace("&#x27;", "'");
    }
    
    /**
     * Encodes string for URL
     */
    public static String urlEncode(String str) {
        if (str == null) return null;
        return java.net.URLEncoder.encode(str, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Decodes URL-encoded string
     */
    public static String urlDecode(String str) {
        if (str == null) return null;
        return java.net.URLDecoder.decode(str, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Reverses string
     */
    public static String reverse(String str) {
        if (str == null) return null;
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * Counts occurrences of substring
     */
    public static int countOccurrences(String str, String searchStr) {
        if (str == null || searchStr == null) return 0;
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(searchStr, index)) != -1) {
            count++;
            index += searchStr.length();
        }
        return count;
    }
    
    /**
     * Gets substring between two delimiters
     */
    public static String substringBetween(String str, String start, String end) {
        if (str == null || start == null || end == null) return null;
        
        int startIndex = str.indexOf(start);
        if (startIndex == -1) return null;
        startIndex += start.length();
        
        int endIndex = str.indexOf(end, startIndex);
        if (endIndex == -1) return null;
        
        return str.substring(startIndex, endIndex);
    }
    
    /**
     * Removes prefix from string if present
     */
    public static String removePrefix(String str, String prefix) {
        if (str == null || prefix == null) return str;
        return str.startsWith(prefix) ? str.substring(prefix.length()) : str;
    }
    
    /**
     * Removes suffix from string if present
     */
    public static String removeSuffix(String str, String suffix) {
        if (str == null || suffix == null) return str;
        return str.endsWith(suffix) ? str.substring(0, str.length() - suffix.length()) : str;
    }
    
    /**
     * Adds prefix to string if not already present
     */
    public static String addPrefix(String str, String prefix) {
        if (str == null || prefix == null) return str;
        return str.startsWith(prefix) ? str : prefix + str;
    }
    
    /**
     * Adds suffix to string if not already present
     */
    public static String addSuffix(String str, String suffix) {
        if (str == null || suffix == null) return str;
        return str.endsWith(suffix) ? str : str + suffix;
    }
    
    /**
     * Calculates Levenshtein distance between two strings
     */
    public static int levenshteinDistance(String str1, String str2) {
        if (str1 == null || str2 == null) return -1;
        
        int len1 = str1.length();
        int len2 = str2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Calculates similarity percentage between two strings
     */
    public static double similarity(String str1, String str2) {
        if (str1 == null && str2 == null) return 1.0;
        if (str1 == null || str2 == null) return 0.0;
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(str1, str2);
        return (maxLength - distance) / (double) maxLength;
    }
    
    /**
     * Generates random string with specified length
     */
    public static String randomString(int length) {
        return randomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
    }
    
    /**
     * Generates random string with specified length and character set
     */
    public static String randomString(int length, String charset) {
        if (length <= 0 || charset == null || charset.isEmpty()) return EMPTY;
        
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Masks string by replacing characters with mask character
     */
    public static String mask(String str, char maskChar, int visibleStart, int visibleEnd) {
        if (str == null) return null;
        if (str.length() <= visibleStart + visibleEnd) return str;
        
        StringBuilder sb = new StringBuilder();
        sb.append(str, 0, visibleStart);
        sb.append(String.valueOf(maskChar).repeat(str.length() - visibleStart - visibleEnd));
        sb.append(str, str.length() - visibleEnd, str.length());
        
        return sb.toString();
    }
    
    /**
     * Masks email address
     */
    public static String maskEmail(String email) {
        if (!isValidEmail(email)) return email;
        
        int atIndex = email.indexOf('@');
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return mask(username, '*', 1, 0) + domain;
        } else {
            return mask(username, '*', 1, 1) + domain;
        }
    }
    
    /**
     * Masks phone number
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return mask(phone, '*', 0, 4);
    }
}