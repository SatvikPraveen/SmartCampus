package interfaces;

import interfaces.Enrollable.EnrollmentStatistics;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnrollableTest {

    @Test
    void statisticsComputeEnrollmentRateAsPercentage() {
        EnrollmentStatistics s = new EnrollmentStatistics(15, 3, 2, 1, 20);

        assertThat(s.getEnrolled()).isEqualTo(15);
        assertThat(s.getWaitlisted()).isEqualTo(3);
        assertThat(s.getDropped()).isEqualTo(2);
        assertThat(s.getCompleted()).isEqualTo(1);
        assertThat(s.getCapacity()).isEqualTo(20);
        assertThat(s.getEnrollmentRate()).isCloseTo(75.0, within(1e-9));
    }

    @Test
    void zeroCapacityYieldsZeroRateInsteadOfDivisionByZero() {
        assertThat(new EnrollmentStatistics(5, 0, 0, 0, 0).getEnrollmentRate()).isZero();
    }

    @Test
    void toStringFormatsRateToOneDecimal() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            assertThat(new EnrollmentStatistics(1, 2, 3, 4, 3)).hasToString(
                    "EnrollmentStatistics{enrolled=1, waitlisted=2, dropped=3, completed=4, capacity=3, rate=33.3%}");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
