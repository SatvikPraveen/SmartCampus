package exceptions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserNotFoundExceptionTest {

    @Nested
    class Constructors {

        @Test
        void messageOnlyConstructors() {
            UserNotFoundException def = new UserNotFoundException();
            assertThat(def).isInstanceOf(RuntimeException.class).hasMessage("User not found");
            assertThat(def.getUserId()).isNull();
            assertThat(def.getUserType()).isNull();
            assertThat(def.getSearchCriteria()).isNull();

            assertThat(new UserNotFoundException("gone")).hasMessage("gone");
            Throwable cause = new IllegalStateException();
            assertThat(new UserNotFoundException("gone", cause)).hasMessage("gone").hasCause(cause);
        }

        @Test
        void idAndTypeConstructors() {
            UserNotFoundException e = new UserNotFoundException("S1", "Student");
            assertThat(e).hasMessage("Student not found with ID: S1");
            assertThat(e.getUserId()).isEqualTo("S1");
            assertThat(e.getUserType()).isEqualTo("Student");
            assertThat(e.getSearchCriteria()).isEqualTo("S1");

            assertThat(new UserNotFoundException("S1", "Student", "archived"))
                    .hasMessage("Student not found with ID: S1. archived");

            UserNotFoundException criteria = new UserNotFoundException("S1", "Student", "email: x", "none");
            assertThat(criteria).hasMessage("Student not found. Search criteria: email: x. none");
            assertThat(criteria.getSearchCriteria()).isEqualTo("email: x");

            Throwable cause = new RuntimeException();
            UserNotFoundException withCause = new UserNotFoundException("S1", "Student", cause);
            assertThat(withCause).hasMessage("Student not found with ID: S1").hasCause(cause);
            assertThat(withCause.getSearchCriteria()).isEqualTo("S1");
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void byRole() {
            assertThat(UserNotFoundException.student("S1")).hasMessage("Student not found with ID: S1");
            assertThat(UserNotFoundException.professor("P1").getUserType()).isEqualTo("Professor");
            assertThat(UserNotFoundException.admin("A1").getUserType()).isEqualTo("Administrator");
            assertThat(UserNotFoundException.inactive("S1", "Student").getMessage())
                    .isEqualTo("Student not found with ID: S1. User account is inactive or has been deactivated.");
        }

        @Test
        void byLookupCriteria() {
            UserNotFoundException email = UserNotFoundException.byEmail("a@b.c");
            assertThat(email.getUserId()).isNull();
            assertThat(email.getSearchCriteria()).isEqualTo("email: a@b.c");
            assertThat(email.hasUserId()).isFalse();

            assertThat(UserNotFoundException.byUsername("bob").getSearchCriteria()).isEqualTo("username: bob");
            assertThat(UserNotFoundException.noSearchResults("smith", "Professor").getMessage())
                    .isEqualTo("Professor not found. Search criteria: search query: smith. No users found matching the search criteria.");
        }

        @Test
        void authenticationAndEnrollmentContexts() {
            UserNotFoundException auth = UserNotFoundException.duringAuthentication("bob");
            assertThat(auth.isAuthenticationRelated()).isTrue();
            assertThat(auth.isEnrollmentRelated()).isFalse();

            UserNotFoundException enroll = UserNotFoundException.duringEnrollment("S1", "CS101");
            assertThat(enroll.getUserId()).isEqualTo("S1");
            assertThat(enroll.getSearchCriteria()).isEqualTo("student ID: S1 for course: CS101");
            assertThat(enroll.isEnrollmentRelated()).isTrue();
            assertThat(enroll.isAuthenticationRelated()).isFalse();
        }

        @Test
        void keywordDetectionIsCaseInsensitive() {
            assertThat(new UserNotFoundException("LOGIN failed").isAuthenticationRelated()).isTrue();
            assertThat(new UserNotFoundException("bad Credentials").isAuthenticationRelated()).isTrue();
            assertThat(new UserNotFoundException("COURSE roster").isEnrollmentRelated()).isTrue();
            assertThat(new UserNotFoundException("cannot enroll").isEnrollmentRelated()).isTrue();
        }

        // Regression: a null message (e.g. new UserNotFoundException((String) null)) made the keyword checks NPE.
        @Test
        void keywordChecksTolerateNullMessage() {
            UserNotFoundException e = new UserNotFoundException((String) null);

            assertThatCode(e::isAuthenticationRelated).doesNotThrowAnyException();
            assertThat(e.isAuthenticationRelated()).isFalse();
            assertThat(e.isEnrollmentRelated()).isFalse();
        }
    }

    @Nested
    class Reporting {

        @Test
        void userFriendlyMessageVariants() {
            assertThat(UserNotFoundException.student("S1").getUserFriendlyMessage())
                    .isEqualTo("The student with ID 'S1' could not be found. Please verify the ID and try again.");
            assertThat(UserNotFoundException.byEmail("x").getUserFriendlyMessage())
                    .isEqualTo("The requested user could not be found. Please check your search criteria.");
            assertThat(new UserNotFoundException().getUserFriendlyMessage())
                    .isEqualTo("The requested user could not be found. Please verify your information and try again.");
            assertThat(new UserNotFoundException(" ", " ").hasUserType()).isFalse();
        }

        @Test
        void suggestionsDependOnContext() {
            assertThat(UserNotFoundException.student("S1").getSuggestions())
                    .startsWith("Suggestions to resolve this issue:\n")
                    .contains("Verify that the ID 'S1' is correct", "Check if the user account is active",
                            "correct user category (Student)", "Contact system administrator");
            assertThat(new UserNotFoundException().getSuggestions())
                    .doesNotContain("Verify that the ID", "user category")
                    .contains("Try searching with different criteria");
        }

        @Test
        void detailedReportListsPresentFields() {
            UserNotFoundException e = new UserNotFoundException("S1", "Student", new IllegalStateException("db"));

            assertThat(e.getDetailedReport()).startsWith("UserNotFoundException Details:\n")
                    .contains("User ID: S1\n", "User Type: Student\n", "Search Criteria: S1\n", "Cause: db\n",
                            "Timestamp: ");
            assertThat(new UserNotFoundException().getDetailedReport())
                    .doesNotContain("User ID:", "User Type:", "Search Criteria:", "Cause:");
        }

        @Test
        void toStringAppendsTypeAndId() {
            assertThat(UserNotFoundException.student("S1"))
                    .hasToString("UserNotFoundException: Student not found with ID: S1 [Type: Student, ID: S1]");
            assertThat(new UserNotFoundException("S1", null, "c", "m").toString()).endsWith("[ID: S1]");
            assertThat(UserNotFoundException.byEmail("x").toString()).endsWith("[Type: User]");
            assertThat(new UserNotFoundException("plain")).hasToString("UserNotFoundException: plain");
        }

        // Regression: withContext used to drop userId/userType/searchCriteria from the copy.
        @Test
        void withContextPreservesUserDetailsAndCause() {
            Throwable cause = new IllegalStateException();
            UserNotFoundException original = new UserNotFoundException("S1", "Student", cause);

            UserNotFoundException copy = original.withContext("batch 3");

            assertThat(copy).isNotSameAs(original)
                    .hasMessage("Student not found with ID: S1 Additional context: batch 3")
                    .hasCause(cause);
            assertThat(copy.getUserId()).isEqualTo("S1");
            assertThat(copy.getUserType()).isEqualTo("Student");
            assertThat(copy.getSearchCriteria()).isEqualTo("S1");
            assertThat(copy.getUserFriendlyMessage()).contains("student with ID 'S1'");
        }
    }
}
