// File: src/main/java/interfaces/Enrollable.java
package interfaces;

import models.Enrollment;
import java.util.List;

/**
 * Enrollable interface defining enrollment-related operations.
 * This interface provides a contract for entities that can manage enrollments.
 * 
 * Key Java concepts demonstrated:
 * - Interface definition and contracts
 * - Method signatures without implementation
 * - Documentation with JavaDoc
 * - Generic return types
 * - Exception handling in interface methods
 */
public interface Enrollable {
    
    /**
     * Enroll a student in a course.
     * 
     * @param studentId The ID of the student to enroll
     * @param courseId The ID of the course to enroll in
     * @param semester The semester for enrollment
     * @param year The year for enrollment
     * @return true if enrollment was successful, false otherwise
     */
    boolean enrollStudent(String studentId, String courseId, String semester, int year);
    
    /**
     * Drop a student from a course.
     * 
     * @param studentId The ID of the student to drop
     * @param courseId The ID of the course to drop from
     * @param reason The reason for dropping (optional)
     * @return true if drop was successful, false otherwise
     */
    boolean dropStudent(String studentId, String courseId, String reason);
    
    /**
     * Add a student to the waitlist for a course.
     * 
     * @param studentId The ID of the student to waitlist
     * @param courseId The ID of the course to waitlist for
     * @param semester The semester for waitlisting
     * @param year The year for waitlisting
     * @return true if waitlisting was successful, false otherwise
     */
    boolean addToWaitlist(String studentId, String courseId, String semester, int year);
    
    /**
     * Remove a student from the waitlist.
     * 
     * @param studentId The ID of the student to remove from waitlist
     * @param courseId The ID of the course to remove from waitlist
     * @return true if removal was successful, false otherwise
     */
    boolean removeFromWaitlist(String studentId, String courseId);
    
    /**
     * Process waitlist by moving students from waitlist to enrolled status.
     * 
     * @param courseId The ID of the course to process waitlist for
     * @param numberOfStudents The number of students to move from waitlist
     * @return Number of students successfully enrolled from waitlist
     */
    int processWaitlist(String courseId, int numberOfStudents);
    
    /**
     * Check if a student is enrolled in a specific course.
     * 
     * @param studentId The ID of the student to check
     * @param courseId The ID of the course to check
     * @return true if student is enrolled, false otherwise
     */
    boolean isStudentEnrolled(String studentId, String courseId);
    
    /**
     * Check if a student is on the waitlist for a specific course.
     * 
     * @param studentId The ID of the student to check
     * @param courseId The ID of the course to check
     * @return true if student is waitlisted, false otherwise
     */
    boolean isStudentWaitlisted(String studentId, String courseId);
    
    /**
     * Get all enrollments for a specific student.
     * 
     * @param studentId The ID of the student
     * @return List of enrollments for the student
     */
    List<Enrollment> getStudentEnrollments(String studentId);
    
    /**
     * Get all enrollments for a specific course.
     * 
     * @param courseId The ID of the course
     * @return List of enrollments for the course
     */
    List<Enrollment> getCourseEnrollments(String courseId);
    
    /**
     * Get current enrollment count for a course.
     * 
     * @param courseId The ID of the course
     * @return Current number of enrolled students
     */
    int getCurrentEnrollmentCount(String courseId);
    
    /**
     * Get current waitlist count for a course.
     * 
     * @param courseId The ID of the course
     * @return Current number of waitlisted students
     */
    int getCurrentWaitlistCount(String courseId);
    
    /**
     * Check if a course has available spots for enrollment.
     * 
     * @param courseId The ID of the course to check
     * @return true if course has available spots, false otherwise
     */
    boolean hasAvailableSpots(String courseId);
    
    /**
     * Get the maximum enrollment capacity for a course.
     * 
     * @param courseId The ID of the course
     * @return Maximum enrollment capacity
     */
    int getMaxEnrollmentCapacity(String courseId);
    
    /**
     * Transfer a student from one course to another.
     * 
     * @param studentId The ID of the student to transfer
     * @param fromCourseId The ID of the course to transfer from
     * @param toCourseId The ID of the course to transfer to
     * @param semester The semester for the transfer
     * @param year The year for the transfer
     * @return true if transfer was successful, false otherwise
     */
    boolean transferStudent(String studentId, String fromCourseId, String toCourseId, String semester, int year);
    
    /**
     * Bulk enroll multiple students in a course.
     * 
     * @param studentIds List of student IDs to enroll
     * @param courseId The ID of the course
     * @param semester The semester for enrollment
     * @param year The year for enrollment
     * @return Number of students successfully enrolled
     */
    int bulkEnrollStudents(List<String> studentIds, String courseId, String semester, int year);
    
    /**
     * Get enrollment statistics for a course.
     * 
     * @param courseId The ID of the course
     * @return Enrollment statistics including counts, waitlist, etc.
     */
    EnrollmentStatistics getEnrollmentStatistics(String courseId);
    
    /**
     * Inner class to hold enrollment statistics.
     */
    class EnrollmentStatistics {
        private final int enrolled;
        private final int waitlisted;
        private final int dropped;
        private final int completed;
        private final int capacity;
        private final double enrollmentRate;
        
        public EnrollmentStatistics(int enrolled, int waitlisted, int dropped, int completed, int capacity) {
            this.enrolled = enrolled;
            this.waitlisted = waitlisted;
            this.dropped = dropped;
            this.completed = completed;
            this.capacity = capacity;
            this.enrollmentRate = capacity > 0 ? (double) enrolled / capacity * 100.0 : 0.0;
        }
        
        public int getEnrolled() { return enrolled; }
        public int getWaitlisted() { return waitlisted; }
        public int getDropped() { return dropped; }
        public int getCompleted() { return completed; }
        public int getCapacity() { return capacity; }
        public double getEnrollmentRate() { return enrollmentRate; }
        
        @Override
        public String toString() {
            return String.format("EnrollmentStatistics{enrolled=%d, waitlisted=%d, dropped=%d, completed=%d, capacity=%d, rate=%.1f%%}",
                enrolled, waitlisted, dropped, completed, capacity, enrollmentRate);
        }
    }
}