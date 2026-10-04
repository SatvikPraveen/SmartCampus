package enums;

import enums.CourseStatus.CoursePhase;
import enums.CourseStatus.StatusColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CourseStatusTest {

    @Test
    void happyPathFollowsAllowedTransitionsFromDraftToArchived() {
        List<CourseStatus> path = new ArrayList<>();
        CourseStatus current = CourseStatus.DRAFT;
        path.add(current);
        Optional<CourseStatus> next;
        while ((next = current.getNextStatus()).isPresent()) {
            assertThat(current.canTransitionTo(next.get()))
                    .as("%s -> %s must be an allowed transition", current, next.get())
                    .isTrue();
            current = next.get();
            path.add(current);
        }
        assertThat(path).startsWith(CourseStatus.DRAFT).endsWith(CourseStatus.ARCHIVED).doesNotHaveDuplicates();
        assertThat(path).hasSize(13);
    }

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    void previousStatusIsTheInverseOfNextStatus(CourseStatus status) {
        status.getNextStatus().ifPresent(next ->
                assertThat(next.getPreviousStatus()).contains(status));
    }

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    void noSelfTransitions(CourseStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @Test
    void onlyArchivedIsTerminal() {
        for (CourseStatus status : CourseStatus.values()) {
            assertThat(status.isTerminal()).as("%s", status).isEqualTo(status == CourseStatus.ARCHIVED);
        }
    }

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    void everyStatusCanReachArchived(CourseStatus start) {
        Set<CourseStatus> seen = EnumSet.of(start);
        Deque<CourseStatus> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            for (CourseStatus next : queue.poll().getAllowedTransitions()) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        assertThat(seen).contains(CourseStatus.ARCHIVED);
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
            "DRAFT, UNDER_REVIEW, true",
            "DRAFT, ENROLLMENT_OPEN, false",
            "ENROLLMENT_OPEN, FULL, true",
            "FULL, WAITLIST_AVAILABLE, true",
            "OVERBOOKED, ENROLLMENT_OPEN, false",
            "FINAL_EXAMS, CANCELLED, false",
            "CANCELLED, IN_PROGRESS, false",
            "SUSPENDED, IN_PROGRESS, true",
            "ARCHIVED, DRAFT, false"
    })
    void canTransitionTo(CourseStatus from, CourseStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    void statusesThatAllowEnrollmentAreVisibleEnrollmentStatuses(CourseStatus status) {
        if (status.allowsEnrollment()) {
            assertThat(status.isVisible()).isTrue();
            assertThat(status.isEnrollmentPhase()).isTrue();
            assertThat(status.getPhase()).isEqualTo(CoursePhase.ENROLLMENT);
        }
    }

    @Test
    void fullAndClosedCoursesDoNotAllowEnrollment() {
        assertThat(CourseStatus.FULL.allowsEnrollment()).isFalse();
        assertThat(CourseStatus.OVERBOOKED.allowsEnrollment()).isFalse();
        assertThat(CourseStatus.ENROLLMENT_CLOSED.allowsEnrollment()).isFalse();
        assertThat(CourseStatus.WAITLIST_AVAILABLE.allowsEnrollment()).isTrue();
    }

    @Test
    void groupHelpers() {
        assertThat(CourseStatus.MID_SEMESTER.isActivePhase()).isTrue();
        assertThat(CourseStatus.GRADES_POSTED.isCompletionPhase()).isTrue();
        assertThat(CourseStatus.POSTPONED.isCancellationStatus()).isTrue();
        assertThat(CourseStatus.DRAFT.isEnrollmentPhase()).isFalse();
        assertThat(CourseStatus.getActiveStatuses()).allMatch(CourseStatus::isActive);
    }

    @Test
    void phasesPartitionAllStatuses() {
        List<CourseStatus> all = new ArrayList<>();
        for (CoursePhase phase : CoursePhase.values()) {
            all.addAll(CourseStatus.getStatusesByPhase(phase));
        }
        assertThat(all).containsExactlyInAnyOrder(CourseStatus.values());
    }

    @ParameterizedTest(name = "{0} -> priority {1}, color {2}")
    @CsvSource({
            "IN_PROGRESS, 1, GREEN", "ENROLLMENT_OPEN, 2, ORANGE", "GRADING_PERIOD, 3, YELLOW",
            "SCHEDULED, 4, PURPLE", "DRAFT, 5, BLUE", "COMPLETED, 6, GRAY", "CANCELLED, 7, RED",
            "ARCHIVED, 8, LIGHT_GRAY"
    })
    void priorityAndColorFollowPhase(CourseStatus status, int priority, StatusColor color) {
        assertThat(status.getPriority()).isEqualTo(priority);
        assertThat(status.getColor()).isEqualTo(color);
    }

    @Test
    void findByDisplayName() {
        assertThat(CourseStatus.findByDisplayName("mid-semester")).contains(CourseStatus.MID_SEMESTER);
        assertThat(CourseStatus.findByDisplayName("under-enrolled")).contains(CourseStatus.UNDER_ENROLLED);
        assertThat(CourseStatus.findByDisplayName("bogus")).isEmpty();
    }
}
