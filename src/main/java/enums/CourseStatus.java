// File location: src/main/java/enums/CourseStatus.java

package enums;

import java.util.*;

/**
 * Enumeration defining course statuses in the SmartCampus system
 * Includes lifecycle states, enrollment management, and scheduling information
 */
public enum CourseStatus {
    
    // Planning and preparation phases
    DRAFT("Draft", "Course is being planned and designed", false, false, false, CoursePhase.PLANNING),
    
    UNDER_REVIEW("Under Review", "Course is being reviewed for approval", false, false, false, CoursePhase.PLANNING),
    
    APPROVED("Approved", "Course has been approved but not yet scheduled", false, false, false, CoursePhase.PLANNING),
    
    SCHEDULED("Scheduled", "Course has been scheduled for upcoming semester", false, true, false, CoursePhase.SCHEDULED),
    
    // Active enrollment phases
    ENROLLMENT_OPEN("Enrollment Open", "Course is open for student enrollment", true, true, false, CoursePhase.ENROLLMENT),
    
    ENROLLMENT_RESTRICTED("Enrollment Restricted", "Course has enrollment restrictions or prerequisites", true, true, false, CoursePhase.ENROLLMENT),
    
    WAITLIST_AVAILABLE("Waitlist Available", "Course is full but waitlist is available", true, true, false, CoursePhase.ENROLLMENT),
    
    ENROLLMENT_CLOSED("Enrollment Closed", "Course enrollment has been closed", false, true, false, CoursePhase.ENROLLMENT),
    
    // Active course phases
    IN_PROGRESS("In Progress", "Course is currently being taught", false, false, true, CoursePhase.ACTIVE),
    
    MID_SEMESTER("Mid-Semester", "Course is at mid-point of semester", false, false, true, CoursePhase.ACTIVE),
    
    FINAL_EXAMS("Final Exams", "Course is in final examination period", false, false, true, CoursePhase.ACTIVE),
    
    GRADING_PERIOD("Grading Period", "Course has ended, grades being finalized", false, false, false, CoursePhase.GRADING),
    
    // Completion phases
    COMPLETED("Completed", "Course has been successfully completed", false, false, false, CoursePhase.COMPLETED),
    
    GRADES_POSTED("Grades Posted", "Final grades have been posted", false, false, false, CoursePhase.COMPLETED),
    
    // Cancellation and suspension
    CANCELLED("Cancelled", "Course has been cancelled", false, false, false, CoursePhase.CANCELLED),
    
    SUSPENDED("Suspended", "Course has been temporarily suspended", false, false, false, CoursePhase.CANCELLED),
    
    POSTPONED("Postponed", "Course has been postponed to future semester", false, false, false, CoursePhase.CANCELLED),
    
    // Special statuses
    FULL("Full", "Course has reached maximum capacity", false, true, true, CoursePhase.ENROLLMENT),
    
    UNDER_ENROLLED("Under-enrolled", "Course has insufficient enrollment", true, true, false, CoursePhase.ENROLLMENT),
    
    OVERBOOKED("Overbooked", "Course has more enrollments than capacity", false, true, true, CoursePhase.ENROLLMENT),
    
    // Administrative statuses
    INACTIVE("Inactive", "Course is not currently offered", false, false, false, CoursePhase.INACTIVE),
    
    ARCHIVED("Archived", "Course has been archived for historical purposes", false, false, false, CoursePhase.INACTIVE);
    
    private final String displayName;
    private final String description;
    private final boolean allowsEnrollment;
    private final boolean isVisible;
    private final boolean isActive;
    private final CoursePhase phase;
    
    CourseStatus(String displayName, String description, boolean allowsEnrollment, 
                boolean isVisible, boolean isActive, CoursePhase phase) {
        this.displayName = displayName;
        this.description = description;
        this.allowsEnrollment = allowsEnrollment;
        this.isVisible = isVisible;
        this.isActive = isActive;
        this.phase = phase;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean allowsEnrollment() {
        return allowsEnrollment;
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public CoursePhase getPhase() {
        return phase;
    }
    
    /**
     * Check if this status allows transitions to another status
     */
    public boolean canTransitionTo(CourseStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }
    
    /**
     * Get all allowed status transitions from current status
     */
    public List<CourseStatus> getAllowedTransitions() {
        switch (this) {
            case DRAFT:
                return Arrays.asList(UNDER_REVIEW, CANCELLED);
            
            case UNDER_REVIEW:
                return Arrays.asList(APPROVED, DRAFT, CANCELLED);
            
            case APPROVED:
                return Arrays.asList(SCHEDULED, CANCELLED, INACTIVE);
            
            case SCHEDULED:
                return Arrays.asList(ENROLLMENT_OPEN, CANCELLED, POSTPONED);
            
            case ENROLLMENT_OPEN:
                return Arrays.asList(ENROLLMENT_RESTRICTED, FULL, UNDER_ENROLLED, 
                                   ENROLLMENT_CLOSED, CANCELLED);
            
            case ENROLLMENT_RESTRICTED:
                return Arrays.asList(ENROLLMENT_OPEN, ENROLLMENT_CLOSED, CANCELLED);
            
            case WAITLIST_AVAILABLE:
                return Arrays.asList(ENROLLMENT_OPEN, ENROLLMENT_CLOSED, FULL);
            
            case FULL:
                return Arrays.asList(WAITLIST_AVAILABLE, ENROLLMENT_CLOSED, OVERBOOKED, IN_PROGRESS);
            
            case UNDER_ENROLLED:
                return Arrays.asList(ENROLLMENT_OPEN, CANCELLED, IN_PROGRESS);
            
            case OVERBOOKED:
                return Arrays.asList(FULL, IN_PROGRESS);
            
            case ENROLLMENT_CLOSED:
                return Arrays.asList(IN_PROGRESS, CANCELLED);
            
            case IN_PROGRESS:
                return Arrays.asList(MID_SEMESTER, FINAL_EXAMS, SUSPENDED, CANCELLED);
            
            case MID_SEMESTER:
                return Arrays.asList(FINAL_EXAMS, SUSPENDED, CANCELLED);
            
            case FINAL_EXAMS:
                return Arrays.asList(GRADING_PERIOD, SUSPENDED);
            
            case GRADING_PERIOD:
                return Arrays.asList(COMPLETED, GRADES_POSTED);
            
            case COMPLETED:
                return Arrays.asList(GRADES_POSTED, ARCHIVED);
            
            case GRADES_POSTED:
                return Arrays.asList(ARCHIVED);
            
            case CANCELLED:
                return Arrays.asList(ARCHIVED, INACTIVE);
            
            case SUSPENDED:
                return Arrays.asList(IN_PROGRESS, CANCELLED, POSTPONED);
            
            case POSTPONED:
                return Arrays.asList(SCHEDULED, CANCELLED);
            
            case INACTIVE:
                return Arrays.asList(DRAFT, ARCHIVED);
            
            case ARCHIVED:
                return Arrays.asList(); // Terminal state
            
            default:
                return Arrays.asList();
        }
    }
    
    /**
     * Get statuses by phase
     */
    public static List<CourseStatus> getStatusesByPhase(CoursePhase phase) {
        List<CourseStatus> statuses = new ArrayList<>();
        for (CourseStatus status : CourseStatus.values()) {
            if (status.phase == phase) {
                statuses.add(status);
            }
        }
        return statuses;
    }
    
    /**
     * Get all enrollment-related statuses
     */
    public static List<CourseStatus> getEnrollmentStatuses() {
        return Arrays.asList(ENROLLMENT_OPEN, ENROLLMENT_RESTRICTED, WAITLIST_AVAILABLE, 
                           ENROLLMENT_CLOSED, FULL, UNDER_ENROLLED, OVERBOOKED);
    }
    
    /**
     * Get all active course statuses
     */
    public static List<CourseStatus> getActiveStatuses() {
        return Arrays.asList(IN_PROGRESS, MID_SEMESTER, FINAL_EXAMS);
    }
    
    /**
     * Get all completion-related statuses
     */
    public static List<CourseStatus> getCompletionStatuses() {
        return Arrays.asList(GRADING_PERIOD, COMPLETED, GRADES_POSTED);
    }
    
    /**
     * Get all cancellation-related statuses
     */
    public static List<CourseStatus> getCancellationStatuses() {
        return Arrays.asList(CANCELLED, SUSPENDED, POSTPONED);
    }
    
    /**
     * Check if status is in enrollment phase
     */
    public boolean isEnrollmentPhase() {
        return getEnrollmentStatuses().contains(this);
    }
    
    /**
     * Check if status is in active phase
     */
    public boolean isActivePhase() {
        return getActiveStatuses().contains(this);
    }
    
    /**
     * Check if status is in completion phase
     */
    public boolean isCompletionPhase() {
        return getCompletionStatuses().contains(this);
    }
    
    /**
     * Check if status represents cancellation
     */
    public boolean isCancellationStatus() {
        return getCancellationStatuses().contains(this);
    }
    
    /**
     * Get next logical status in course lifecycle
     */
    public Optional<CourseStatus> getNextStatus() {
        switch (this) {
            case DRAFT: return Optional.of(UNDER_REVIEW);
            case UNDER_REVIEW: return Optional.of(APPROVED);
            case APPROVED: return Optional.of(SCHEDULED);
            case SCHEDULED: return Optional.of(ENROLLMENT_OPEN);
            case ENROLLMENT_OPEN: return Optional.of(ENROLLMENT_CLOSED);
            case ENROLLMENT_CLOSED: return Optional.of(IN_PROGRESS);
            case IN_PROGRESS: return Optional.of(MID_SEMESTER);
            case MID_SEMESTER: return Optional.of(FINAL_EXAMS);
            case FINAL_EXAMS: return Optional.of(GRADING_PERIOD);
            case GRADING_PERIOD: return Optional.of(COMPLETED);
            case COMPLETED: return Optional.of(GRADES_POSTED);
            case GRADES_POSTED: return Optional.of(ARCHIVED);
            default: return Optional.empty();
        }
    }
    
    /**
     * Get previous logical status in course lifecycle
     */
    public Optional<CourseStatus> getPreviousStatus() {
        switch (this) {
            case UNDER_REVIEW: return Optional.of(DRAFT);
            case APPROVED: return Optional.of(UNDER_REVIEW);
            case SCHEDULED: return Optional.of(APPROVED);
            case ENROLLMENT_OPEN: return Optional.of(SCHEDULED);
            case ENROLLMENT_CLOSED: return Optional.of(ENROLLMENT_OPEN);
            case IN_PROGRESS: return Optional.of(ENROLLMENT_CLOSED);
            case MID_SEMESTER: return Optional.of(IN_PROGRESS);
            case FINAL_EXAMS: return Optional.of(MID_SEMESTER);
            case GRADING_PERIOD: return Optional.of(FINAL_EXAMS);
            case COMPLETED: return Optional.of(GRADING_PERIOD);
            case GRADES_POSTED: return Optional.of(COMPLETED);
            case ARCHIVED: return Optional.of(GRADES_POSTED);
            default: return Optional.empty();
        }
    }
    
    /**
     * Check if status is terminal (no further transitions)
     */
    public boolean isTerminal() {
        return getAllowedTransitions().isEmpty();
    }
    
    /**
     * Get status priority for sorting (lower numbers = higher priority)
     */
    public int getPriority() {
        switch (this.phase) {
            case ACTIVE: return 1;
            case ENROLLMENT: return 2;
            case GRADING: return 3;
            case SCHEDULED: return 4;
            case PLANNING: return 5;
            case COMPLETED: return 6;
            case CANCELLED: return 7;
            case INACTIVE: return 8;
            default: return 9;
        }
    }
    
    /**
     * Find status by display name
     */
    public static Optional<CourseStatus> findByDisplayName(String displayName) {
        for (CourseStatus status : CourseStatus.values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get status color for UI display
     */
    public StatusColor getColor() {
        switch (this.phase) {
            case PLANNING: return StatusColor.BLUE;
            case SCHEDULED: return StatusColor.PURPLE;
            case ENROLLMENT: return StatusColor.ORANGE;
            case ACTIVE: return StatusColor.GREEN;
            case GRADING: return StatusColor.YELLOW;
            case COMPLETED: return StatusColor.GRAY;
            case CANCELLED: return StatusColor.RED;
            case INACTIVE: return StatusColor.LIGHT_GRAY;
            default: return StatusColor.GRAY;
        }
    }
    
    /**
     * Get status icon for UI display
     */
    public String getIcon() {
        switch (this) {
            case DRAFT: return "📝";
            case UNDER_REVIEW: return "👀";
            case APPROVED: return "✅";
            case SCHEDULED: return "📅";
            case ENROLLMENT_OPEN: return "🔓";
            case ENROLLMENT_RESTRICTED: return "🔒";
            case WAITLIST_AVAILABLE: return "⏳";
            case ENROLLMENT_CLOSED: return "🚫";
            case FULL: return "🏠";
            case UNDER_ENROLLED: return "📉";
            case OVERBOOKED: return "📈";
            case IN_PROGRESS: return "▶️";
            case MID_SEMESTER: return "📖";
            case FINAL_EXAMS: return "📋";
            case GRADING_PERIOD: return "✏️";
            case COMPLETED: return "🎓";
            case GRADES_POSTED: return "📊";
            case CANCELLED: return "❌";
            case SUSPENDED: return "⏸️";
            case POSTPONED: return "⏭️";
            case INACTIVE: return "💤";
            case ARCHIVED: return "📦";
            default: return "❓";
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", displayName, description);
    }
    
    /**
     * Course phase enumeration
     */
    public enum CoursePhase {
        PLANNING("Planning", "Course design and approval phase"),
        SCHEDULED("Scheduled", "Course has been scheduled"),
        ENROLLMENT("Enrollment", "Student enrollment phase"),
        ACTIVE("Active", "Course is being taught"),
        GRADING("Grading", "Grading and evaluation phase"),
        COMPLETED("Completed", "Course has been completed"),
        CANCELLED("Cancelled", "Course has been cancelled or suspended"),
        INACTIVE("Inactive", "Course is not active");
        
        private final String displayName;
        private final String description;
        
        CoursePhase(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Status color enumeration for UI
     */
    public enum StatusColor {
        RED("#FF4444"),
        ORANGE("#FF8800"),
        YELLOW("#FFDD00"),
        GREEN("#44AA44"),
        BLUE("#4488FF"),
        PURPLE("#8844FF"),
        GRAY("#888888"),
        LIGHT_GRAY("#CCCCCC");
        
        private final String hexCode;
        
        StatusColor(String hexCode) {
            this.hexCode = hexCode;
        }
        
        public String getHexCode() { return hexCode; }
    }
}