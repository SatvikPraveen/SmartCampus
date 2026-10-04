package enums;

import enums.Priority.PriorityCategory;
import enums.Priority.PrioritySchedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityTest {

    // Far enough from "now" in either direction that the wall clock can never matter.
    private static final LocalDateTime LONG_AGO = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime FAR_FUTURE = LocalDateTime.now().plusYears(50);

    @ParameterizedTest
    @EnumSource(Priority.class)
    void everyLevelLiesWithinItsCategoryRange(Priority priority) {
        assertThat(priority.getCategory().includesLevel(priority.getLevel())).isTrue();
    }

    @Test
    void lookups() {
        assertThat(Priority.fromCode("crit")).isEqualTo(Priority.CRITICAL);
        assertThat(Priority.fromCode("grade_dl")).isEqualTo(Priority.GRADE_DEADLINE);
        assertThat(Priority.fromCode("nope")).isNull();
        assertThat(Priority.fromLevel(1)).isEqualTo(Priority.CRITICAL);
        assertThat(Priority.fromLevel(99)).isEqualTo(Priority.NORMAL);
    }

    @Test
    void escalateMovesToTheNextMoreUrgentLevel() {
        assertThat(Priority.escalate(Priority.NORMAL).getLevel()).isEqualTo(6);
        assertThat(Priority.escalate(Priority.LOW).getLevel()).isEqualTo(9);
        assertThat(Priority.escalate(Priority.ACADEMIC_EMERGENCY)).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void emergenciesAndNullCannotBeEscalated() {
        assertThat(Priority.escalate(Priority.CRITICAL)).isEqualTo(Priority.CRITICAL);
        assertThat(Priority.escalate(Priority.EMERGENCY)).isEqualTo(Priority.EMERGENCY);
        assertThat(Priority.escalate(null)).isNull();
    }

    @ParameterizedTest
    @EnumSource(Priority.class)
    void escalationNeverLowersUrgencyAndDeescalationNeverRaisesIt(Priority priority) {
        assertThat(Priority.escalate(priority).getLevel()).isLessThanOrEqualTo(priority.getLevel());
        assertThat(Priority.deescalate(priority).getLevel()).isGreaterThanOrEqualTo(priority.getLevel());
        if (!priority.isEmergency() && priority.getLevel() > 1) {
            assertThat(Priority.escalate(priority).getLevel()).isEqualTo(priority.getLevel() - 1);
        }
    }

    @Test
    void deescalate() {
        assertThat(Priority.deescalate(null)).isEqualTo(Priority.NORMAL);
        assertThat(Priority.deescalate(Priority.COMPLETED)).isEqualTo(Priority.COMPLETED);
        assertThat(Priority.deescalate(Priority.LOW).getLevel()).isEqualTo(11);
    }

    @ParameterizedTest(name = "urgent={0}, important={1}, {2} -> {3}")
    @CsvSource({
            "true, true, ACADEMIC, ACADEMIC_EMERGENCY",
            "true, true, HIGH, CRITICAL",
            "true, false, ACADEMIC, HIGH",
            "false, true, ACADEMIC, IMPORTANT",
            "false, false, LOW, NORMAL"
    })
    void eisenhowerMatrix(boolean urgent, boolean important, PriorityCategory category, Priority expected) {
        assertThat(Priority.calculatePriority(urgent, important, category)).isEqualTo(expected);
    }

    @Test
    void deadlineIsCreationPlusResponseTime() {
        LocalDateTime created = LocalDateTime.of(2024, 3, 1, 10, 0);
        assertThat(Priority.CRITICAL.getDeadline(created)).isEqualTo(LocalDateTime.of(2024, 3, 1, 10, 15));
        assertThat(Priority.LOW.getDeadline(created)).isEqualTo(LocalDateTime.of(2024, 3, 8, 10, 0));
        assertThat(Priority.SCHEDULED.getDeadline(created)).isNull();
        assertThat(Priority.CRITICAL.getDeadline(null)).isNull();
        assertThat(Priority.SCHEDULED.hasDeadline()).isFalse();
    }

    @Test
    void overdueAndTimeRemaining() {
        assertThat(Priority.BACKLOG.isOverdue(LONG_AGO)).isTrue();
        assertThat(Priority.BACKLOG.getTimeRemaining(LONG_AGO)).isEqualTo(Duration.ZERO);
        assertThat(Priority.CRITICAL.isOverdue(FAR_FUTURE)).isFalse();
        assertThat(Priority.CRITICAL.getTimeRemaining(FAR_FUTURE)).isGreaterThan(Duration.ofDays(365));
        assertThat(Priority.ON_HOLD.isOverdue(LONG_AGO)).isFalse();
        assertThat(Priority.ON_HOLD.getTimeRemaining(LONG_AGO)).isNull();
    }

    @Test
    void shouldEscalate() {
        assertThat(Priority.shouldEscalate(Priority.NORMAL, LONG_AGO)).isTrue();
        assertThat(Priority.shouldEscalate(Priority.NORMAL, FAR_FUTURE)).isFalse();
        assertThat(Priority.shouldEscalate(Priority.COMPLETED, LONG_AGO)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"CRITICAL, 200.0", "NORMAL, 120.0", "LOW, 80.0", "COMPLETED, 10.0", "COMPLIANCE, 210.0"})
    void urgencyScore(Priority priority, double expected) {
        assertThat(priority.getUrgencyScore()).isEqualTo(Math.min(expected, 200.0));
    }

    @ParameterizedTest
    @EnumSource(Priority.class)
    void urgencyScoreIsCappedAt200(Priority priority) {
        assertThat(priority.getUrgencyScore()).isBetween(0.0, 200.0);
    }

    @Test
    void sortByUrgencyPutsMostUrgentFirst() {
        assertThat(Priority.sortByUrgency(List.of(Priority.LOW, Priority.CRITICAL, Priority.NORMAL)))
                .containsExactly(Priority.CRITICAL, Priority.NORMAL, Priority.LOW);
    }

    @Test
    void sortByDeadlinePutsOpenEndedLast() {
        PrioritySchedule none = new PrioritySchedule(Priority.SCHEDULED);
        PrioritySchedule week = new PrioritySchedule(Priority.LOW);
        PrioritySchedule minutes = new PrioritySchedule(Priority.CRITICAL);
        assertThat(Priority.sortByDeadline(List.of(none, week, minutes)))
                .extracting(PrioritySchedule::getPriority)
                .containsExactly(Priority.CRITICAL, Priority.LOW, Priority.SCHEDULED);
        assertThat(Priority.getOverduePriorities(List.of(none, week, minutes))).isEmpty();
    }

    @Test
    void getByCategoryReturnsAnIndependentCopy() {
        List<Priority> emergency = Priority.getEmergencyPriorities();
        assertThat(emergency).containsExactly(Priority.CRITICAL, Priority.URGENT, Priority.EMERGENCY);
        emergency.clear();
        assertThat(Priority.getEmergencyPriorities()).hasSize(3);
    }

    @Test
    void escalationAndLevelListsAreSorted() {
        assertThat(Priority.getEscalationPriorities())
                .allMatch(Priority::isRequiresEscalation)
                .isSortedAccordingTo((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
        assertThat(Priority.getLevelSorted())
                .hasSize(Priority.values().length)
                .isSortedAccordingTo((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
    }

    @Test
    void distributionCountsByCategory() {
        assertThat(Priority.getPriorityDistribution(List.of(Priority.HIGH, Priority.IMPORTANT, Priority.LOW)))
                .containsEntry(PriorityCategory.HIGH, 2L)
                .containsEntry(PriorityCategory.LOW, 1L)
                .hasSize(2);
    }
}
