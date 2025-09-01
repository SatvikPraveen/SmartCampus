// File location: src/main/java/enums/EnrollmentStatus.java

package enums;

import java.util.*;

/**
 * Enumeration defining enrollment statuses in the SmartCampus system
 * Tracks student enrollment lifecycle and academic standing
 */
public enum EnrollmentStatus {
    
    // Application and admission statuses
    APPLIED("Applied", "Student has submitted application", false, false, false, EnrollmentPhase.APPLICATION),
    
    UNDER_REVIEW("Under Review", "Application is being reviewed", false, false, false, EnrollmentPhase.APPLICATION),
    
    ADMITTED("Admitted", "Student has been admitted", false, false, false, EnrollmentPhase.ADMISSION),
    
    CONDITIONALLY_ADMITTED("Conditionally Admitted", "Student admitted with conditions", false, false, false, EnrollmentPhase.ADMISSION),
    
    WAITLISTED("Waitlisted", "Student is on admission waitlist", false, false, false, EnrollmentPhase.ADMISSION),
    
    ADMISSION_DEFERRED("Admission Deferred", "Admission decision deferred", false, false, false, EnrollmentPhase.ADMISSION),
    
    REJECTED("Rejected", "Application has been rejected", false, false, false, EnrollmentPhase.ADMISSION),
    
    // Enrollment confirmation statuses
    DEPOSIT_PAID("Deposit Paid", "Student has paid enrollment deposit", false, false, false, EnrollmentPhase.CONFIRMATION),
    
    ENROLLED("Enrolled", "Student is officially enrolled", true, true, true, EnrollmentPhase.ACTIVE),
    
    CONFIRMED("Confirmed", "Enrollment has been confirmed", true, true, true, EnrollmentPhase.ACTIVE),
    
    // Active enrollment statuses
    FULL_TIME("Full-time", "Student enrolled full-time", true, true, true, EnrollmentPhase.ACTIVE),
    
    PART_TIME("Part-time", "Student enrolled part-time", true, true, true, EnrollmentPhase.ACTIVE),
    
    AUDIT("Audit", "Student auditing courses", true, true, false, EnrollmentPhase.ACTIVE),
    
    VISITING("Visiting", "Visiting student from another institution", true, true, true, EnrollmentPhase.ACTIVE),
    
    EXCHANGE("Exchange", "Exchange student", true, true, true, EnrollmentPhase.ACTIVE),
    
    // Academic standing statuses
    GOOD_STANDING("Good Standing", "Student in good academic standing", true, true, true, EnrollmentPhase.ACTIVE),
    
    ACADEMIC_WARNING("Academic Warning", "Student on academic warning", true, true, true, EnrollmentPhase.ACADEMIC_CONCERN),
    
    ACADEMIC_PROBATION("Academic Probation", "Student on academic probation", true, true, true, EnrollmentPhase.ACADEMIC_CONCERN),
    
    ACADEMIC_SUSPENSION("Academic Suspension", "Student academically suspended", false, false, false, EnrollmentPhase.ACADEMIC_CONCERN),
    
    ACADEMIC_DISMISSAL("Academic Dismissal", "Student academically dismissed", false, false, false, EnrollmentPhase.ACADEMIC_CONCERN),
    
    // Leave and absence statuses
    LEAVE_OF_ABSENCE("Leave of Absence", "Student on approved leave", false, false, false, EnrollmentPhase.INACTIVE),
    
    MEDICAL_LEAVE("Medical Leave", "Student on medical leave", false, false, false, EnrollmentPhase.INACTIVE),
    
    PERSONAL_LEAVE("Personal Leave", "Student on personal leave", false, false, false, EnrollmentPhase.INACTIVE),
    
    MILITARY_LEAVE("Military Leave", "Student on military leave", false, false, false, EnrollmentPhase.INACTIVE),
    
    // Transfer and withdrawal statuses
    TRANSFER_OUT("Transfer Out", "Student transferring to another institution", false, false, false, EnrollmentPhase.SEPARATION),
    
    WITHDRAWN("Withdrawn", "Student has withdrawn from institution", false, false, false, EnrollmentPhase.SEPARATION),
    
    VOLUNTARY_WITHDRAWAL("Voluntary Withdrawal", "Student voluntarily withdrew", false, false, false, EnrollmentPhase.SEPARATION),
    
    ADMINISTRATIVE_WITHDRAWAL("Administrative Withdrawal", "Student administratively withdrawn", false, false, false, EnrollmentPhase.SEPARATION),
    
    // Completion statuses
    GRADUATED("Graduated", "Student has graduated", false, false, false, EnrollmentPhase.COMPLETED),
    
    DEGREE_CONFERRED("Degree Conferred", "Student's degree has been conferred", false, false, false, EnrollmentPhase.COMPLETED),
    
    CERTIFICATE_COMPLETED("Certificate Completed", "Student completed certificate program", false, false, false, EnrollmentPhase.COMPLETED),
    
    // Special statuses
    DEREGISTERED("Deregistered", "Student has been deregistered", false, false, false, EnrollmentPhase.INACTIVE),
    
    NON_MATRICULATED("Non-matriculated", "Student not formally matriculated", true, true, false, EnrollmentPhase.ACTIVE),
    
    CONTINUING_EDUCATION("Continuing Education", "Student in continuing education program", true, true, false, EnrollmentPhase.ACTIVE),
    
    DECEASED("Deceased", "Student is deceased", false, false, false, EnrollmentPhase.COMPLETED),
    
    // Administrative statuses
    INACTIVE("Inactive", "Student account is inactive", false, false, false, EnrollmentPhase.INACTIVE),
    
    ARCHIVED("Archived", "Student record has been archived", false, false, false, EnrollmentPhase.INACTIVE);
    
    private final String displayName;
    private final String description;
    private final boolean canEnrollInCourses;
    private final boolean hasSystemAccess;
    private final boolean countsTowardEnrollment;
    private final EnrollmentPhase phase;
    
    EnrollmentStatus(String displayName, String description, boolean canEnrollInCourses,
                    boolean hasSystemAccess, boolean countsTowardEnrollment, EnrollmentPhase phase) {
        this.displayName = displayName;
        this.description = description;
        this.canEnrollInCourses = canEnrollInCourses;
        this.hasSystemAccess = hasSystemAccess;
        this.countsTowardEnrollment = countsTowardEnrollment;
        this.phase = phase;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canEnrollInCourses() {
        return canEnrollInCourses;
    }
    
    public boolean hasSystemAccess() {
        return hasSystemAccess;
    }
    
    public boolean countsTowardEnrollment() {
        return countsTowardEnrollment;
    }
    
    public EnrollmentPhase getPhase() {
        return phase;
    }
    
    /**
     * Check if this status allows transitions to another status
     */
    public boolean canTransitionTo(EnrollmentStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }
    
    /**
     * Get all allowed status transitions from current status
     */
    public List<EnrollmentStatus> getAllowedTransitions() {
        switch (this) {
            case APPLIED:
                return Arrays.asList(UNDER_REVIEW, WITHDRAWN);
            
            case UNDER_REVIEW:
                return Arrays.asList(ADMITTED, CONDITIONALLY_ADMITTED, WAITLISTED, 
                                   ADMISSION_DEFERRED, REJECTED);
            
            case ADMITTED:
                return Arrays.asList(DEPOSIT_PAID, ENROLLED, ADMISSION_DEFERRED, WITHDRAWN);
            
            case CONDITIONALLY_ADMITTED:
                return Arrays.asList(ADMITTED, DEPOSIT_PAID, ENROLLED, REJECTED);
            
            case WAITLISTED:
                return Arrays.asList(ADMITTED, REJECTED, WITHDRAWN);
            
            case ADMISSION_DEFERRED:
                return Arrays.asList(UNDER_REVIEW, WITHDRAWN);
            
            case REJECTED:
                return Arrays.asList(ARCHIVED);
            
            case DEPOSIT_PAID:
                return Arrays.asList(ENROLLED, CONFIRMED, WITHDRAWN);
            
            case ENROLLED:
                return Arrays.asList(CONFIRMED, FULL_TIME, PART_TIME, AUDIT, VISITING, 
                                   EXCHANGE, WITHDRAWN);
            
            case CONFIRMED:
                return Arrays.asList(FULL_TIME, PART_TIME, GOOD_STANDING, WITHDRAWN);
            
            case FULL_TIME:
                return Arrays.asList(PART_TIME, GOOD_STANDING, ACADEMIC_WARNING, 
                                   LEAVE_OF_ABSENCE, WITHDRAWN, GRADUATED);
            
            case PART_TIME:
                return Arrays.asList(FULL_TIME, GOOD_STANDING, ACADEMIC_WARNING, 
                                   LEAVE_OF_ABSENCE, WITHDRAWN, GRADUATED);
            
            case AUDIT:
                return Arrays.asList(ENROLLED, WITHDRAWN);
            
            case VISITING:
                return Arrays.asList(TRANSFER_OUT, WITHDRAWN);
            
            case EXCHANGE:
                return Arrays.asList(TRANSFER_OUT, WITHDRAWN);
            
            case GOOD_STANDING:
                return Arrays.asList(ACADEMIC_WARNING, LEAVE_OF_ABSENCE, WITHDRAWN, GRADUATED);
            
            case ACADEMIC_WARNING:
                return Arrays.asList(GOOD_STANDING, ACADEMIC_PROBATION, WITHDRAWN);
            
            case ACADEMIC_PROBATION:
                return Arrays.asList(GOOD_STANDING, ACADEMIC_SUSPENSION, ACADEMIC_DISMISSAL);
            
            case ACADEMIC_SUSPENSION:
                return Arrays.asList(GOOD_STANDING, ACADEMIC_DISMISSAL, WITHDRAWN);
            
            case ACADEMIC_DISMISSAL:
                return Arrays.asList(ARCHIVED);
            
            case LEAVE_OF_ABSENCE:
                return Arrays.asList(ENROLLED, GOOD_STANDING, WITHDRAWN);
            
            case MEDICAL_LEAVE:
                return Arrays.asList(ENROLLED, GOOD_STANDING, WITHDRAWN);
            
            case PERSONAL_LEAVE:
                return Arrays.asList(ENROLLED, GOOD_STANDING, WITHDRAWN);
            
            case MILITARY_LEAVE:
                return Arrays.asList(ENROLLED, GOOD_STANDING, WITHDRAWN);
            
            case TRANSFER_OUT:
                return Arrays.asList(ARCHIVED);
            
            case WITHDRAWN:
                return Arrays.asList(ENROLLED, ARCHIVED);
            
            case VOLUNTARY_WITHDRAWAL:
                return Arrays.asList(ENROLLED, ARCHIVED);
            
            case ADMINISTRATIVE_WITHDRAWAL:
                return Arrays.asList(ENROLLED, ARCHIVED);
            
            case GRADUATED:
                return Arrays.asList(DEGREE_CONFERRED, CONTINUING_EDUCATION);
            
            case DEGREE_CONFERRED:
                return Arrays.asList(ARCHIVED, CONTINUING_EDUCATION);
            
            case CERTIFICATE_COMPLETED:
                return Arrays.asList(ENROLLED, ARCHIVED);
            
            case DEREGISTERED:
                return Arrays.asList(ENROLLED, INACTIVE);
            
            case NON_MATRICULATED:
                return Arrays.asList(ENROLLED, WITHDRAWN);
            
            case CONTINUING_EDUCATION:
                return Arrays.asList(INACTIVE, ARCHIVED);
            
            case DECEASED:
                return Arrays.asList(ARCHIVED);
            
            case INACTIVE:
                return Arrays.asList(ENROLLED, ARCHIVED);
            
            case ARCHIVED:
                return Arrays.asList(); // Terminal state
            
            default:
                return Arrays.asList();
        }
    }
    
    /**
     * Get statuses by phase
     */
    public static List<EnrollmentStatus> getStatusesByPhase(EnrollmentPhase phase) {
        List<EnrollmentStatus> statuses = new ArrayList<>();
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            if (status.phase == phase) {
                statuses.add(status);
            }
        }
        return statuses;
    }
    
    /**
     * Get all active enrollment statuses
     */
    public static List<EnrollmentStatus> getActiveStatuses() {
        return Arrays.asList(ENROLLED, CONFIRMED, FULL_TIME, PART_TIME, AUDIT, 
                           VISITING, EXCHANGE, GOOD_STANDING, NON_MATRICULATED, 
                           CONTINUING_EDUCATION);
    }
    
    /**
     * Get all inactive statuses
     */
    public static List<EnrollmentStatus> getInactiveStatuses() {
        return Arrays.asList(LEAVE_OF_ABSENCE, MEDICAL_LEAVE, PERSONAL_LEAVE, 
                           MILITARY_LEAVE, DEREGISTERED, INACTIVE);
    }
    
    /**
     * Get all academic concern statuses
     */
    public static List<EnrollmentStatus> getAcademicConcernStatuses() {
        return Arrays.asList(ACADEMIC_WARNING, ACADEMIC_PROBATION, ACADEMIC_SUSPENSION, 
                           ACADEMIC_DISMISSAL);
    }
    
    /**
     * Get all separation statuses
     */
    public static List<EnrollmentStatus> getSeparationStatuses() {
        return Arrays.asList(TRANSFER_OUT, WITHDRAWN, VOLUNTARY_WITHDRAWAL, 
                           ADMINISTRATIVE_WITHDRAWAL);
    }
    
    /**
     * Get all completion statuses
     */
    public static List<EnrollmentStatus> getCompletionStatuses() {
        return Arrays.asList(GRADUATED, DEGREE_CONFERRED, CERTIFICATE_COMPLETED, DECEASED);
    }
    
    /**
     * Check if status is active
     */
    public boolean isActive() {
        return getActiveStatuses().contains(this);
    }
    
    /**
     * Check if status indicates academic concern
     */
    public boolean isAcademicConcern() {
        return getAcademicConcernStatuses().contains(this);
    }
    
    /**
     * Check if status indicates separation from institution
     */
    public boolean isSeparation() {
        return getSeparationStatuses().contains(this);
    }
    
    /**
     * Check if status indicates completion
     */
    public boolean isCompletion() {
        return getCompletionStatuses().contains(this);
    }
    
    /**
     * Check if status is temporary
     */
    public boolean isTemporary() {
        return Arrays.asList(LEAVE_OF_ABSENCE, MEDICAL_LEAVE, PERSONAL_LEAVE, 
                           MILITARY_LEAVE, ACADEMIC_WARNING, UNDER_REVIEW, 
                           WAITLISTED, ADMISSION_DEFERRED).contains(this);
    }
    
    /**
     * Get status priority for sorting (lower numbers = higher priority)
     */
    public int getPriority() {
        switch (this.phase) {
            case ACTIVE: return 1;
            case ACADEMIC_CONCERN: return 2;
            case CONFIRMATION: return 3;
            case ADMISSION: return 4;
            case APPLICATION: return 5;
            case INACTIVE: return 6;
            case SEPARATION: return 7;
            case COMPLETED: return 8;
            default: return 9;
        }
    }
    
    /**
     * Get enrollment type category
     */
    public EnrollmentType getEnrollmentType() {
        if (Arrays.asList(FULL_TIME, PART_TIME).contains(this)) {
            return this == FULL_TIME ? EnrollmentType.FULL_TIME : EnrollmentType.PART_TIME;
        }
        if (Arrays.asList(AUDIT, NON_MATRICULATED, CONTINUING_EDUCATION).contains(this)) {
            return EnrollmentType.NON_CREDIT;
        }
        if (Arrays.asList(VISITING, EXCHANGE).contains(this)) {
            return EnrollmentType.TEMPORARY;
        }
        return EnrollmentType.STANDARD;
    }
    
    /**
     * Find status by display name
     */
    public static Optional<EnrollmentStatus> findByDisplayName(String displayName) {
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
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
            case APPLICATION: return StatusColor.BLUE;
            case ADMISSION: return StatusColor.PURPLE;
            case CONFIRMATION: return StatusColor.ORANGE;
            case ACTIVE: return StatusColor.GREEN;
            case ACADEMIC_CONCERN: return StatusColor.YELLOW;
            case INACTIVE: return StatusColor.GRAY;
            case SEPARATION: return StatusColor.RED;
            case COMPLETED: return StatusColor.DARK_GREEN;
            default: return StatusColor.GRAY;
        }
    }
    
    /**
     * Get status icon for UI display
     */
    public String getIcon() {
        switch (this) {
            case APPLIED: return "📄";
            case UNDER_REVIEW: return "🔍";
            case ADMITTED: return "🎉";
            case CONDITIONALLY_ADMITTED: return "⚠️";
            case WAITLISTED: return "⏳";
            case ADMISSION_DEFERRED: return "⏸️";
            case REJECTED: return "❌";
            case DEPOSIT_PAID: return "💰";
            case ENROLLED: return "✅";
            case CONFIRMED: return "☑️";
            case FULL_TIME: return "📚";
            case PART_TIME: return "📖";
            case AUDIT: return "👂";
            case VISITING: return "🏠";
            case EXCHANGE: return "🔄";
            case GOOD_STANDING: return "⭐";
            case ACADEMIC_WARNING: return "⚠️";
            case ACADEMIC_PROBATION: return "🚨";
            case ACADEMIC_SUSPENSION: return "⛔";
            case ACADEMIC_DISMISSAL: return "🚫";
            case LEAVE_OF_ABSENCE: return "🏖️";
            case MEDICAL_LEAVE: return "🏥";
            case PERSONAL_LEAVE: return "🏡";
            case MILITARY_LEAVE: return "🪖";
            case TRANSFER_OUT: return "➡️";
            case WITHDRAWN: return "🚪";
            case GRADUATED: return "🎓";
            case DEGREE_CONFERRED: return "📜";
            case CERTIFICATE_COMPLETED: return "🏆";
            case DECEASED: return "🕊️";
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
     * Enrollment phase enumeration
     */
    public enum EnrollmentPhase {
        APPLICATION("Application", "Student application phase"),
        ADMISSION("Admission", "Admission decision phase"),
        CONFIRMATION("Confirmation", "Enrollment confirmation phase"),
        ACTIVE("Active", "Active enrollment"),
        ACADEMIC_CONCERN("Academic Concern", "Academic standing concerns"),
        INACTIVE("Inactive", "Temporarily inactive"),
        SEPARATION("Separation", "Student separation from institution"),
        COMPLETED("Completed", "Academic journey completed");
        
        private final String displayName;
        private final String description;
        
        EnrollmentPhase(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Enrollment type enumeration
     */
    public enum EnrollmentType {
        STANDARD("Standard", "Standard degree-seeking enrollment"),
        FULL_TIME("Full-time", "Full-time degree-seeking enrollment"),
        PART_TIME("Part-time", "Part-time degree-seeking enrollment"),
        NON_CREDIT("Non-credit", "Non-credit or audit enrollment"),
        TEMPORARY("Temporary", "Temporary or visiting enrollment");
        
        private final String displayName;
        private final String description;
        
        EnrollmentType(String displayName, String description) {
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
        DARK_GREEN("#228844"),
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