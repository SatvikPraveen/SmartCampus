// File location: src/main/java/enums/Priority.java
package enums;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumeration defining priority levels for various campus operations
 * Includes urgency, escalation, and time-based priority management
 */
public enum Priority {
    
    // Emergency priorities
    CRITICAL("Critical", "CRIT", 1, Duration.ofMinutes(15), true, PriorityCategory.EMERGENCY),
    URGENT("Urgent", "URG", 2, Duration.ofHours(1), true, PriorityCategory.EMERGENCY),
    EMERGENCY("Emergency", "EMERG", 3, Duration.ofHours(2), true, PriorityCategory.EMERGENCY),
    
    // High priorities
    HIGH("High", "HIGH", 4, Duration.ofHours(4), false, PriorityCategory.HIGH),
    IMPORTANT("Important", "IMP", 5, Duration.ofHours(8), false, PriorityCategory.HIGH),
    EXPEDITED("Expedited", "EXP", 6, Duration.ofHours(12), false, PriorityCategory.HIGH),
    
    // Medium priorities
    NORMAL("Normal", "NORM", 7, Duration.ofDays(1), false, PriorityCategory.MEDIUM),
    STANDARD("Standard", "STD", 8, Duration.ofDays(2), false, PriorityCategory.MEDIUM),
    ROUTINE("Routine", "ROUT", 9, Duration.ofDays(3), false, PriorityCategory.MEDIUM),
    
    // Low priorities
    LOW("Low", "LOW", 10, Duration.ofDays(7), false, PriorityCategory.LOW),
    DEFERRED("Deferred", "DEF", 11, Duration.ofDays(14), false, PriorityCategory.LOW),
    BACKLOG("Backlog", "BACK", 12, Duration.ofDays(30), false, PriorityCategory.LOW),
    
    // Special priorities
    SCHEDULED("Scheduled", "SCHED", 13, null, false, PriorityCategory.SPECIAL),
    ON_HOLD("On Hold", "HOLD", 14, null, false, PriorityCategory.SPECIAL),
    CANCELLED("Cancelled", "CANC", 15, null, false, PriorityCategory.SPECIAL),
    COMPLETED("Completed", "COMP", 16, null, false, PriorityCategory.SPECIAL),
    
    // Academic specific priorities
    ACADEMIC_EMERGENCY("Academic Emergency", "ACAD_EMERG", 2, Duration.ofHours(2), true, PriorityCategory.ACADEMIC),
    GRADE_DEADLINE("Grade Deadline", "GRADE_DL", 4, Duration.ofHours(6), false, PriorityCategory.ACADEMIC),
    ENROLLMENT_DEADLINE("Enrollment Deadline", "ENROLL_DL", 5, Duration.ofHours(12), false, PriorityCategory.ACADEMIC),
    TRANSCRIPT_REQUEST("Transcript Request", "TRANS_REQ", 7, Duration.ofDays(2), false, PriorityCategory.ACADEMIC),
    COURSE_PLANNING("Course Planning", "COURSE_PLAN", 10, Duration.ofDays(7), false, PriorityCategory.ACADEMIC),
    
    // Administrative priorities
    COMPLIANCE("Compliance", "COMPL", 3, Duration.ofHours(4), true, PriorityCategory.ADMINISTRATIVE),
    AUDIT_REQUIRED("Audit Required", "AUDIT", 5, Duration.ofDays(1), false, PriorityCategory.ADMINISTRATIVE),
    POLICY_UPDATE("Policy Update", "POLICY", 8, Duration.ofDays(5), false, PriorityCategory.ADMINISTRATIVE),
    DOCUMENTATION("Documentation", "DOC", 11, Duration.ofDays(14), false, PriorityCategory.ADMINISTRATIVE);
    
    // Priority category enumeration
    public enum PriorityCategory {
        EMERGENCY("Emergency", "Immediate attention required", 1, 3),
        HIGH("High Priority", "Urgent but not emergency", 4, 6),
        MEDIUM("Medium Priority", "Standard processing time", 7, 9),
        LOW("Low Priority", "Can be delayed if necessary", 10, 12),
        SPECIAL("Special Status", "Non-standard priority", 13, 16),
        ACADEMIC("Academic Priority", "Education-related urgency", 2, 10),
        ADMINISTRATIVE("Administrative Priority", "Operational urgency", 3, 11);
        
        private final String displayName;
        private final String description;
        private final int minLevel;
        private final int maxLevel;
        
        PriorityCategory(String displayName, String description, int minLevel, int maxLevel) {
            this.displayName = displayName;
            this.description = description;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getMinLevel() { return minLevel; }
        public int getMaxLevel() { return maxLevel; }
        
        public boolean includesLevel(int level) {
            return level >= minLevel && level <= maxLevel;
        }
    }
    
    // Instance variables
    private final String displayName;
    private final String code;
    private final int level;
    private final Duration responseTime;
    private final boolean requiresEscalation;
    private final PriorityCategory category;
    
    // Static data structures
    private static final Map<String, Priority> CODE_MAP;
    private static final Map<PriorityCategory, List<Priority>> CATEGORY_MAP;
    private static final List<Priority> ESCALATION_PRIORITIES;
    private static final List<Priority> LEVEL_SORTED;
    
    static {
        CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(
                Priority::getCode,
                priority -> priority,
                (existing, replacement) -> existing
            ));
            
        CATEGORY_MAP = Arrays.stream(values())
            .collect(Collectors.groupingBy(Priority::getCategory));
            
        ESCALATION_PRIORITIES = Arrays.stream(values())
            .filter(Priority::isRequiresEscalation)
            .sorted(Comparator.comparing(Priority::getLevel))
            .collect(Collectors.toList());
            
        LEVEL_SORTED = Arrays.stream(values())
            .sorted(Comparator.comparing(Priority::getLevel))
            .collect(Collectors.toList());
    }
    
    // Constructor
    Priority(String displayName, String code, int level, Duration responseTime, 
             boolean requiresEscalation, PriorityCategory category) {
        this.displayName = displayName;
        this.code = code;
        this.level = level;
        this.responseTime = responseTime;
        this.requiresEscalation = requiresEscalation;
        this.category = category;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getCode() { return code; }
    public int getLevel() { return level; }
    public Duration getResponseTime() { return responseTime; }
    public boolean isRequiresEscalation() { return requiresEscalation; }
    public PriorityCategory getCategory() { return category; }
    
    // Utility methods
    public boolean isEmergency() {
        return category == PriorityCategory.EMERGENCY;
    }
    
    public boolean isHigh() {
        return category == PriorityCategory.HIGH;
    }
    
    public boolean isMedium() {
        return category == PriorityCategory.MEDIUM;
    }
    
    public boolean isLow() {
        return category == PriorityCategory.LOW;
    }
    
    public boolean isSpecial() {
        return category == PriorityCategory.SPECIAL;
    }
    
    public boolean isAcademic() {
        return category == PriorityCategory.ACADEMIC;
    }
    
    public boolean hasDeadline() {
        return responseTime != null;
    }
    
    public boolean isOverdue(LocalDateTime createdTime) {
        if (responseTime == null || createdTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(createdTime.plus(responseTime));
    }
    
    public LocalDateTime getDeadline(LocalDateTime createdTime) {
        if (responseTime == null || createdTime == null) {
            return null;
        }
        return createdTime.plus(responseTime);
    }
    
    public Duration getTimeRemaining(LocalDateTime createdTime) {
        if (responseTime == null || createdTime == null) {
            return null;
        }
        LocalDateTime deadline = createdTime.plus(responseTime);
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(deadline) ? Duration.between(now, deadline) : Duration.ZERO;
    }
    
    public double getUrgencyScore() {
        double baseScore = (17 - level) * 10.0; // Higher level = lower score, invert
        
        if (requiresEscalation) {
            baseScore += 50;
        }
        
        if (responseTime != null) {
            long hours = responseTime.toHours();
            if (hours <= 1) baseScore += 40;
            else if (hours <= 24) baseScore += 20;
            else if (hours <= 168) baseScore += 10; // 1 week
        }
        
        return Math.min(baseScore, 200); // Cap at 200
    }
    
    // Static utility methods
    public static Priority fromCode(String code) {
        return CODE_MAP.get(code.toUpperCase());
    }
    
    public static Priority fromLevel(int level) {
        return Arrays.stream(values())
            .filter(p -> p.getLevel() == level)
            .findFirst()
            .orElse(NORMAL);
    }
    
    public static List<Priority> getByCategory(PriorityCategory category) {
        return CATEGORY_MAP.getOrDefault(category, new ArrayList<>());
    }
    
    public static List<Priority> getEscalationPriorities() {
        return new ArrayList<>(ESCALATION_PRIORITIES);
    }
    
    public static List<Priority> getLevelSorted() {
        return new ArrayList<>(LEVEL_SORTED);
    }
    
    public static List<Priority> getEmergencyPriorities() {
        return getByCategory(PriorityCategory.EMERGENCY);
    }
    
    public static List<Priority> getAcademicPriorities() {
        return getByCategory(PriorityCategory.ACADEMIC);
    }
    
    public static Priority escalate(Priority current) {
        if (current == null || current.isEmergency()) {
            return current; // Can't escalate further
        }
        
        Optional<Priority> higher = LEVEL_SORTED.stream()
            .filter(p -> p.getLevel() < current.getLevel())
            .max(Comparator.comparing(Priority::getLevel));
            
        return higher.orElse(current);
    }
    
    public static Priority deescalate(Priority current) {
        if (current == null) {
            return NORMAL;
        }
        
        Optional<Priority> lower = LEVEL_SORTED.stream()
            .filter(p -> p.getLevel() > current.getLevel())
            .min(Comparator.comparing(Priority::getLevel));
            
        return lower.orElse(current);
    }
    
    public static Priority calculatePriority(boolean isUrgent, boolean isImportant, 
                                           PriorityCategory category) {
        if (isUrgent && isImportant) {
            return category == PriorityCategory.ACADEMIC ? ACADEMIC_EMERGENCY : CRITICAL;
        } else if (isUrgent) {
            return HIGH;
        } else if (isImportant) {
            return IMPORTANT;
        } else {
            return NORMAL;
        }
    }
    
    // Time-based priority management
    public static class PrioritySchedule {
        private final Priority priority;
        private final LocalDateTime createdTime;
        private final LocalDateTime lastUpdated;
        
        public PrioritySchedule(Priority priority) {
            this.priority = priority;
            this.createdTime = LocalDateTime.now();
            this.lastUpdated = LocalDateTime.now();
        }
        
        public Priority getPriority() { return priority; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        
        public boolean isOverdue() {
            return priority.isOverdue(createdTime);
        }
        
        public LocalDateTime getDeadline() {
            return priority.getDeadline(createdTime);
        }
        
        public Duration getTimeRemaining() {
            return priority.getTimeRemaining(createdTime);
        }
        
        public double getUrgencyScore() {
            double baseScore = priority.getUrgencyScore();
            
            // Add time pressure factor
            if (priority.hasDeadline()) {
                Duration remaining = getTimeRemaining();
                if (remaining != null) {
                    double hoursRemaining = remaining.toHours();
                    double originalHours = priority.getResponseTime().toHours();
                    double timeFactor = Math.max(0, 1 - (hoursRemaining / originalHours));
                    baseScore += timeFactor * 50; // Up to 50 points for time pressure
                }
            }
            
            return baseScore;
        }
        
        public Priority suggestEscalation() {
            if (isOverdue()) {
                return Priority.escalate(priority);
            }
            
            Duration remaining = getTimeRemaining();
            if (remaining != null && remaining.toHours() <= 1 && priority.getLevel() > 5) {
                return Priority.escalate(priority);
            }
            
            return priority;
        }
    }
    
    // Comparison and sorting
    public static List<Priority> sortByUrgency(List<Priority> priorities) {
        return priorities.stream()
            .sorted(Comparator.comparing(Priority::getUrgencyScore).reversed())
            .collect(Collectors.toList());
    }
    
    public static List<PrioritySchedule> sortByDeadline(List<PrioritySchedule> schedules) {
        return schedules.stream()
            .sorted((s1, s2) -> {
                LocalDateTime d1 = s1.getDeadline();
                LocalDateTime d2 = s2.getDeadline();
                
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                
                return d1.compareTo(d2);
            })
            .collect(Collectors.toList());
    }
    
    public static Map<PriorityCategory, Long> getPriorityDistribution(List<Priority> priorities) {
        return priorities.stream()
            .collect(Collectors.groupingBy(
                Priority::getCategory,
                Collectors.counting()
            ));
    }
    
    public static List<Priority> getOverduePriorities(List<PrioritySchedule> schedules) {
        return schedules.stream()
            .filter(PrioritySchedule::isOverdue)
            .map(PrioritySchedule::getPriority)
            .collect(Collectors.toList());
    }
    
    public static boolean shouldEscalate(Priority priority, LocalDateTime createdTime) {
        if (priority.isOverdue(createdTime)) {
            return true;
        }
        
        Duration remaining = priority.getTimeRemaining(createdTime);
        if (remaining != null && remaining.toHours() <= 2 && priority.getLevel() > 6) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(displayName).append(" (").append(code).append(")");
        sb.append(" - Level ").append(level);
        
        if (responseTime != null) {
            long hours = responseTime.toHours();
            if (hours < 24) {
                sb.append(" - ").append(hours).append(" hours");
            } else {
                sb.append(" - ").append(hours / 24).append(" days");
            }
        }
        
        if (requiresEscalation) {
            sb.append(" [ESCALATION REQUIRED]");
        }
        
        return sb.toString();
    }
}