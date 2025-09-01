// File: src/main/java/utils/InputUtil.java
package utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

/**
 * InputUtil class providing static utility methods for handling user input.
 * 
 * Key Java concepts covered:
 * - Static utility methods
 * - Scanner usage for input handling
 * - Generic methods
 * - Functional interfaces (Predicate)
 * - Exception handling
 * - Method overloading
 * - Input validation and sanitization
 * - Type parsing and conversion
 */
public final class InputUtil {
    
    // Default Scanner instance
    private static Scanner defaultScanner = new Scanner(System.in);
    
    // Date and time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    // Common prompts
    private static final String INVALID_INPUT_MESSAGE = "Invalid input. Please try again.";
    private static final String EMPTY_INPUT_MESSAGE = "Input cannot be empty. Please try again.";
    
    // Private constructor to prevent instantiation
    private InputUtil() {
        throw new UnsupportedOperationException("InputUtil is a utility class and cannot be instantiated");
    }
    
    // String input methods
    
    /**
     * Prompts user for string input with validation.
     * @param prompt The prompt message to display
     * @param allowEmpty Whether empty input is allowed
     * @return validated string input
     */
    public static String getString(String prompt, boolean allowEmpty) {
        return getString(defaultScanner, prompt, allowEmpty);
    }
    
    /**
     * Prompts user for string input with validation using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param allowEmpty Whether empty input is allowed
     * @return validated string input
     */
    public static String getString(Scanner scanner, String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            
            if (!allowEmpty && input.isEmpty()) {
                System.out.println(EMPTY_INPUT_MESSAGE);
                continue;
            }
            
            return input;
        }
    }
    
    /**
     * Prompts user for string input with custom validation.
     * @param prompt The prompt message to display
     * @param validator Predicate to validate the input
     * @param errorMessage Error message to show for invalid input
     * @return validated string input
     */
    public static String getString(String prompt, Predicate<String> validator, String errorMessage) {
        return getString(defaultScanner, prompt, validator, errorMessage);
    }
    
    /**
     * Prompts user for string input with custom validation using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param validator Predicate to validate the input
     * @param errorMessage Error message to show for invalid input
     * @return validated string input
     */
    public static String getString(Scanner scanner, String prompt, Predicate<String> validator, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            
            if (validator.test(input)) {
                return input;
            }
            
            System.out.println(errorMessage);
        }
    }
    
    /**
     * Prompts user for string input with length constraints.
     * @param prompt The prompt message to display
     * @param minLength Minimum allowed length
     * @param maxLength Maximum allowed length
     * @return validated string input
     */
    public static String getString(String prompt, int minLength, int maxLength) {
        return getString(prompt, 
            input -> input.length() >= minLength && input.length() <= maxLength,
            String.format("Input must be between %d and %d characters long.", minLength, maxLength));
    }
    
    // Numeric input methods
    
    /**
     * Prompts user for integer input.
     * @param prompt The prompt message to display
     * @return integer input
     */
    public static int getInt(String prompt) {
        return getInt(defaultScanner, prompt);
    }
    
    /**
     * Prompts user for integer input using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @return integer input
     */
    public static int getInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }
    
    /**
     * Prompts user for integer input within a range.
     * @param prompt The prompt message to display
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return integer input within range
     */
    public static int getInt(String prompt, int min, int max) {
        return getInt(defaultScanner, prompt, min, max);
    }
    
    /**
     * Prompts user for integer input within a range using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return integer input within range
     */
    public static int getInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            int value = getInt(scanner, prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Please enter a value between %d and %d.%n", min, max);
        }
    }
    
    /**
     * Prompts user for double input.
     * @param prompt The prompt message to display
     * @return double input
     */
    public static double getDouble(String prompt) {
        return getDouble(defaultScanner, prompt);
    }
    
    /**
     * Prompts user for double input using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @return double input
     */
    public static double getDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine().trim();
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
    
    /**
     * Prompts user for double input within a range.
     * @param prompt The prompt message to display
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return double input within range
     */
    public static double getDouble(String prompt, double min, double max) {
        return getDouble(defaultScanner, prompt, min, max);
    }
    
    /**
     * Prompts user for double input within a range using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return double input within range
     */
    public static double getDouble(Scanner scanner, String prompt, double min, double max) {
        while (true) {
            double value = getDouble(scanner, prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Please enter a value between %.2f and %.2f.%n", min, max);
        }
    }
    
    // Boolean input methods
    
    /**
     * Prompts user for yes/no input.
     * @param prompt The prompt message to display
     * @return true for yes, false for no
     */
    public static boolean getBoolean(String prompt) {
        return getBoolean(defaultScanner, prompt);
    }
    
    /**
     * Prompts user for yes/no input using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @return true for yes, false for no
     */
    public static boolean getBoolean(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            
            if (input.equals("y") || input.equals("yes") || input.equals("true")) {
                return true;
            } else if (input.equals("n") || input.equals("no") || input.equals("false")) {
                return false;
            }
            
            System.out.println("Please enter 'y' for yes or 'n' for no.");
        }
    }
    
    // Date and time input methods
    
    /**
     * Prompts user for date input in yyyy-MM-dd format.
     * @param prompt The prompt message to display
     * @return LocalDate input
     */
    public static LocalDate getDate(String prompt) {
        return getDate(defaultScanner, prompt);
    }
    
    /**
     * Prompts user for date input using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @return LocalDate input
     */
    public static LocalDate getDate(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd): ");
            String input = scanner.nextLine().trim();
            
            try {
                return LocalDate.parse(input, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Please enter date in yyyy-MM-dd format (e.g., 2024-03-15).");
            }
        }
    }
    
    /**
     * Prompts user for time input in HH:mm format.
     * @param prompt The prompt message to display
     * @return LocalTime input
     */
    public static LocalTime getTime(String prompt) {
        return getTime(defaultScanner, prompt);
    }
    
    /**
     * Prompts user for time input using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @return LocalTime input
     */
    public static LocalTime getTime(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (HH:mm): ");
            String input = scanner.nextLine().trim();
            
            try {
                return LocalTime.parse(input, TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Please enter time in HH:mm format (e.g., 14:30).");
            }
        }
    }
    
    // Email and phone input methods
    
    /**
     * Prompts user for email input with validation.
     * @param prompt The prompt message to display
     * @return validated email address
     */
    public static String getEmail(String prompt) {
        return getString(prompt, ValidationUtil::isValidEmail, "Please enter a valid email address.");
    }
    
    /**
     * Prompts user for phone number input with validation.
     * @param prompt The prompt message to display
     * @return validated phone number
     */
    public static String getPhoneNumber(String prompt) {
        return getString(prompt, ValidationUtil::isValidPhoneNumber, "Please enter a valid phone number.");
    }
    
    /**
     * Prompts user for optional phone number input.
     * @param prompt The prompt message to display
     * @return validated phone number or null if empty
     */
    public static String getOptionalPhoneNumber(String prompt) {
        String phone = getString(prompt + " (optional)", true);
        if (phone.isEmpty()) {
            return null;
        }
        
        if (!ValidationUtil.isValidPhoneNumber(phone)) {
            System.out.println("Invalid phone number format. Skipping...");
            return null;
        }
        
        return phone;
    }
    
    // Choice selection methods
    
    /**
     * Prompts user to select from a list of options.
     * @param prompt The prompt message to display
     * @param options Array of options to choose from
     * @param <T> Type of the options
     * @return selected option
     */
    public static <T> T getChoice(String prompt, T[] options) {
        return getChoice(defaultScanner, prompt, options);
    }
    
    /**
     * Prompts user to select from a list of options using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param options Array of options to choose from
     * @param <T> Type of the options
     * @return selected option
     */
    public static <T> T getChoice(Scanner scanner, String prompt, T[] options) {
        while (true) {
            System.out.println(prompt);
            for (int i = 0; i < options.length; i++) {
                System.out.printf("%d. %s%n", i + 1, options[i]);
            }
            
            int choice = getInt(scanner, "Enter your choice (1-" + options.length + "): ", 1, options.length);
            return options[choice - 1];
        }
    }
    
    /**
     * Prompts user to select from a list of options.
     * @param prompt The prompt message to display
     * @param options List of options to choose from
     * @param <T> Type of the options
     * @return selected option
     */
    public static <T> T getChoice(String prompt, List<T> options) {
        return getChoice(defaultScanner, prompt, options);
    }
    
    /**
     * Prompts user to select from a list of options using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param options List of options to choose from
     * @param <T> Type of the options
     * @return selected option
     */
    public static <T> T getChoice(Scanner scanner, String prompt, List<T> options) {
        while (true) {
            System.out.println(prompt);
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, options.get(i));
            }
            
            int choice = getInt(scanner, "Enter your choice (1-" + options.size() + "): ", 1, options.size());
            return options.get(choice - 1);
        }
    }
    
    // Multi-selection methods
    
    /**
     * Prompts user to select multiple items from a list.
     * @param prompt The prompt message to display
     * @param options Array of options to choose from
     * @param <T> Type of the options
     * @return list of selected options
     */
    public static <T> List<T> getMultipleChoices(String prompt, T[] options) {
        return getMultipleChoices(defaultScanner, prompt, options);
    }
    
    /**
     * Prompts user to select multiple items from a list using custom scanner.
     * @param scanner The scanner to use for input
     * @param prompt The prompt message to display
     * @param options Array of options to choose from
     * @param <T> Type of the options
     * @return list of selected options
     */
    public static <T> List<T> getMultipleChoices(Scanner scanner, String prompt, T[] options) {
        List<T> selected = new ArrayList<>();
        
        System.out.println(prompt);
        for (int i = 0; i < options.length; i++) {
            System.out.printf("%d. %s%n", i + 1, options[i]);
        }
        
        while (true) {
            String input = getString(scanner, "Enter choice numbers separated by commas (or 'done' to finish): ", false);
            
            if (input.equalsIgnoreCase("done")) {
                break;
            }
            
            try {
                String[] parts = input.split(",");
                for (String part : parts) {
                    int choice = Integer.parseInt(part.trim());
                    if (choice >= 1 && choice <= options.length) {
                        T option = options[choice - 1];
                        if (!selected.contains(option)) {
                            selected.add(option);
                            System.out.println("Added: " + option);
                        } else {
                            System.out.println("Already selected: " + option);
                        }
                    } else {
                        System.out.println("Invalid choice: " + choice);
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter valid numbers separated by commas.");
            }
        }
        
        return selected;
    }
    
    // Confirmation methods
    
    /**
     * Prompts user for confirmation.
     * @param message The confirmation message
     * @return true if confirmed, false otherwise
     */
    public static boolean confirm(String message) {
        return getBoolean(message);
    }
    
    /**
     * Prompts user for confirmation with default value.
     * @param message The confirmation message
     * @param defaultValue Default value if user just presses enter
     * @return true if confirmed, false otherwise
     */
    public static boolean confirm(String message, boolean defaultValue) {
        return confirm(defaultScanner, message, defaultValue);
    }
    
    /**
     * Prompts user for confirmation with default value using custom scanner.
     * @param scanner The scanner to use for input
     * @param message The confirmation message
     * @param defaultValue Default value if user just presses enter
     * @return true if confirmed, false otherwise
     */
    public static boolean confirm(Scanner scanner, String message, boolean defaultValue) {
        String defaultText = defaultValue ? "Y/n" : "y/N";
        System.out.print(message + " (" + defaultText + "): ");
        String input = scanner.nextLine().trim().toLowerCase();
        
        if (input.isEmpty()) {
            return defaultValue;
        }
        
        return input.equals("y") || input.equals("yes") || input.equals("true");
    }
    
    // Utility methods
    
    /**
     * Pauses execution until user presses enter.
     */
    public static void pause() {
        pause("Press Enter to continue...");
    }
    
    /**
     * Pauses execution with custom message.
     * @param message The message to display
     */
    public static void pause(String message) {
        System.out.print(message);
        defaultScanner.nextLine();
    }
    
    /**
     * Clears the console screen (platform dependent).
     */
    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing fails, just print some newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    /**
     * Sets the default scanner for all input operations.
     * @param scanner The scanner to use as default
     */
    public static void setDefaultScanner(Scanner scanner) {
        defaultScanner = scanner;
    }
    
    /**
     * Gets the current default scanner.
     * @return the default scanner
     */
    public static Scanner getDefaultScanner() {
        return defaultScanner;
    }
    
    /**
     * Safely closes the default scanner.
     */
    public static void closeDefaultScanner() {
        if (defaultScanner != null) {
            defaultScanner.close();
        }
    }
    
    /**
     * Sanitizes string input by removing potentially harmful characters.
     * @param input The input to sanitize
     * @return sanitized input
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim()
                   .replaceAll("[<>\"'&]", "") // Remove HTML/XML dangerous chars
                   .replaceAll("\\s+", " ");   // Normalize whitespace
    }
    
    /**
     * Formats a date for display.
     * @param date The date to format
     * @return formatted date string
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE_FORMATTER) : "N/A";
    }
    
    /**
     * Formats a time for display.
     * @param time The time to format
     * @return formatted time string
     */
    public static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "N/A";
    }
}