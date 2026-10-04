package enums;

import enums.EnrollmentStatus.EnrollmentPhase;
import enums.EnrollmentStatus.EnrollmentType;
import enums.EnrollmentStatus.StatusColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
            "APPLIED, UNDER_REVIEW, true",
            "APPLIED, ENROLLED, false",
            "UNDER_REVIEW, WAITLISTED, true",
            "WAITLISTED, ADMITTED, true",
            "WAITLISTED, ENROLLED, false",
            "REJECTED, ADMITTED, false",
            "ADMITTED, ENROLLED, true",
            "ACADEMIC_PROBATION, GOOD_STANDING, true",
            "ACADEMIC_PROBATION, WITHDRAWN, false",
            "WITHDRAWN, ENROLLED, true",
            "GRADUATED, ENROLLED, false",
            "ARCHIVED, ENROLLED, false"
    })
    void canTransitionTo(EnrollmentStatus from, EnrollmentStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EnrollmentStatus.class)
    void noStatusTransitionsToItself(EnrollmentStatus status) {
        assertThat(status.getAllowedTransitions()).doesNotContain(status);
    }

    @Test
    void archivedIsTheOnlyTerminalStatus() {
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            assertThat(status.getAllowedTransitions().isEmpty())
                    .as("%s terminal", status)
                    .isEqualTo(status == EnrollmentStatus.ARCHIVED);
        }
    }

    @ParameterizedTest
    @EnumSource(EnrollmentStatus.class)
    void everyStatusCanEventuallyBeArchived(EnrollmentStatus start) {
        Set<EnrollmentStatus> seen = EnumSet.of(start);
        Deque<EnrollmentStatus> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            for (EnrollmentStatus next : queue.poll().getAllowedTransitions()) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        assertThat(seen).contains(EnrollmentStatus.ARCHIVED);
    }

    @ParameterizedTest
    @EnumSource(EnrollmentStatus.class)
    void countingTowardEnrollmentImpliesCourseAccess(EnrollmentStatus status) {
        if (status.countsTowardEnrollment()) {
            assertThat(status.canEnrollInCourses()).isTrue();
        }
        if (status.canEnrollInCourses()) {
            assertThat(status.hasSystemAccess()).isTrue();
        }
    }

    @Test
    void activeStatusesCanEnrollAndSeparatedOrCompletedCannot() {
        assertThat(EnrollmentStatus.getActiveStatuses()).allMatch(EnrollmentStatus::canEnrollInCourses);
        List<EnrollmentStatus> gone = new ArrayList<>(EnrollmentStatus.getSeparationStatuses());
        gone.addAll(EnrollmentStatus.getCompletionStatuses());
        assertThat(gone).noneMatch(EnrollmentStatus::canEnrollInCourses)
                .noneMatch(EnrollmentStatus::hasSystemAccess);
    }

    @Test
    void statusGroupsAreDisjoint() {
        List<List<EnrollmentStatus>> groups = List.of(
                EnrollmentStatus.getActiveStatuses(), EnrollmentStatus.getInactiveStatuses(),
                EnrollmentStatus.getAcademicConcernStatuses(), EnrollmentStatus.getSeparationStatuses(),
                EnrollmentStatus.getCompletionStatuses());
        for (int i = 0; i < groups.size(); i++) {
            for (int j = i + 1; j < groups.size(); j++) {
                assertThat(groups.get(i)).doesNotContainAnyElementsOf(groups.get(j));
            }
        }
        assertThat(EnrollmentStatus.ACADEMIC_PROBATION.isAcademicConcern()).isTrue();
        assertThat(EnrollmentStatus.WITHDRAWN.isSeparation()).isTrue();
        assertThat(EnrollmentStatus.GRADUATED.isCompletion()).isTrue();
        assertThat(EnrollmentStatus.FULL_TIME.isActive()).isTrue();
        assertThat(EnrollmentStatus.MEDICAL_LEAVE.isActive()).isFalse();
    }

    @Test
    void phasesPartitionAllStatuses() {
        List<EnrollmentStatus> all = new ArrayList<>();
        for (EnrollmentPhase phase : EnrollmentPhase.values()) {
            List<EnrollmentStatus> inPhase = EnrollmentStatus.getStatusesByPhase(phase);
            assertThat(inPhase).allMatch(s -> s.getPhase() == phase);
            all.addAll(inPhase);
        }
        assertThat(all).containsExactlyInAnyOrder(EnrollmentStatus.values());
    }

    @ParameterizedTest(name = "{0} -> priority {1}, color {2}")
    @CsvSource({
            "ENROLLED, 1, GREEN",
            "ACADEMIC_PROBATION, 2, YELLOW",
            "DEPOSIT_PAID, 3, ORANGE",
            "ADMITTED, 4, PURPLE",
            "APPLIED, 5, BLUE",
            "MEDICAL_LEAVE, 6, GRAY",
            "WITHDRAWN, 7, RED",
            "GRADUATED, 8, DARK_GREEN"
    })
    void priorityAndColorFollowPhase(EnrollmentStatus status, int priority, StatusColor color) {
        assertThat(status.getPriority()).isEqualTo(priority);
        assertThat(status.getColor()).isEqualTo(color);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "FULL_TIME, FULL_TIME", "PART_TIME, PART_TIME", "AUDIT, NON_CREDIT",
            "CONTINUING_EDUCATION, NON_CREDIT", "VISITING, TEMPORARY", "EXCHANGE, TEMPORARY",
            "ENROLLED, STANDARD", "GRADUATED, STANDARD"
    })
    void enrollmentType(EnrollmentStatus status, EnrollmentType expected) {
        assertThat(status.getEnrollmentType()).isEqualTo(expected);
    }

    @Test
    void findByDisplayNameIsCaseInsensitive() {
        assertThat(EnrollmentStatus.findByDisplayName("full-time")).contains(EnrollmentStatus.FULL_TIME);
        assertThat(EnrollmentStatus.findByDisplayName("Academic Probation")).contains(EnrollmentStatus.ACADEMIC_PROBATION);
        assertThat(EnrollmentStatus.findByDisplayName("nope")).isEmpty();
    }

    @Test
    void temporaryStatuses() {
        assertThat(EnrollmentStatus.MEDICAL_LEAVE.isTemporary()).isTrue();
        assertThat(EnrollmentStatus.WAITLISTED.isTemporary()).isTrue();
        assertThat(EnrollmentStatus.ENROLLED.isTemporary()).isFalse();
        assertThat(EnrollmentStatus.ACADEMIC_DISMISSAL.isTemporary()).isFalse();
    }

    @Test
    void toStringCombinesNameAndDescription() {
        assertThat(EnrollmentStatus.ENROLLED.toString()).isEqualTo("Enrolled: Student is officially enrolled");
        assertThat(StatusColor.RED.getHexCode()).isEqualTo("#FF4444");
    }
}
