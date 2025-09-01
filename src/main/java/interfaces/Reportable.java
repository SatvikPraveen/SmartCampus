// File: src/main/java/interfaces/Reportable.java
package interfaces;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Reportable interface defining reporting and analytics operations.
 * This interface provides a contract for entities that can generate reports and analytics.
 * 
 * Key Java concepts demonstrated:
 * - Interface definition with multiple method signatures
 * - Generic types and collections
 * - Nested classes and enums in interfaces
 * - Default methods (Java 8+ feature)
 * - Static methods in interfaces
 */
public interface Reportable {
    
    /**
     * Report types enumeration.
     */
    enum ReportType {
        ENROLLMENT_REPORT("Enrollment Report"),
        GRADE_REPORT("Grade Report"),
        ATTENDANCE_REPORT("Attendance Report"),
        FINANCIAL_REPORT("Financial Report"),
        PERFORMANCE_REPORT("Performance Report"),
        DEMOGRAPHIC_REPORT("Demographic Report"),
        COURSE_EVALUATION_REPORT("Course Evaluation Report"),
        GRADUATION_REPORT("Graduation Report"),
        RETENTION_REPORT("Retention Report"),
        STATISTICAL_SUMMARY("Statistical Summary");
        
        private final String displayName;
        
        ReportType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Report format enumeration.
     */
    enum ReportFormat {
        PDF("PDF"),
        CSV("CSV"),
        EXCEL("Excel"),
        HTML("HTML"),
        JSON("JSON"),
        XML("XML"),
        TEXT("Plain Text");
        
        private final String displayName;
        
        ReportFormat(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Generate a basic report.
     * 
     * @param reportType The type of report to generate
     * @return ReportData containing the report information
     */
    ReportData generateReport(ReportType reportType);
    
    /**
     * Generate a report with specific parameters.
     * 
     * @param reportType The type of report to generate
     * @param parameters Map of parameters for the report
     * @return ReportData containing the report information
     */
    ReportData generateReport(ReportType reportType, Map<String, Object> parameters);
    
    /**
     * Generate a report for a specific date range.
     * 
     * @param reportType The type of report to generate
     * @param startDate The start date for the report
     * @param endDate The end date for the report
     * @return ReportData containing the report information
     */
    ReportData generateReportForDateRange(ReportType reportType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Export a report in the specified format.
     * 
     * @param reportData The report data to export
     * @param format The format to export to
     * @param filePath The file path to save the report
     * @return true if export was successful, false otherwise
     */
    boolean exportReport(ReportData reportData, ReportFormat format, String filePath);
    
    /**
     * Get available report types.
     * 
     * @return List of available report types
     */
    List<ReportType> getAvailableReportTypes();
    
    /**
     * Get supported export formats.
     * 
     * @return List of supported export formats
     */
    List<ReportFormat> getSupportedFormats();
    
    /**
     * Schedule a recurring report.
     * 
     * @param reportType The type of report to schedule
     * @param frequency The frequency of the report (daily, weekly, monthly)
     * @param recipients List of recipients for the report
     * @return Scheduled report ID if successful, null otherwise
     */
    String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients);
    
    /**
     * Cancel a scheduled report.
     * 
     * @param scheduledReportId The ID of the scheduled report to cancel
     * @return true if cancellation was successful, false otherwise
     */
    boolean cancelScheduledReport(String scheduledReportId);
    
    /**
     * Get report history.
     * 
     * @param reportType The type of reports to get history for (null for all types)
     * @param limit Maximum number of reports to return
     * @return List of historical reports
     */
    List<ReportMetadata> getReportHistory(ReportType reportType, int limit);
    
    /**
     * Get summary statistics.
     * 
     * @return Map of key statistics
     */
    Map<String, Object> getSummaryStatistics();
    
    /**
     * Default method to format report title (Java 8+ feature).
     * 
     * @param reportType The report type
     * @param timestamp The timestamp when the report was generated
     * @return Formatted report title
     */
    default String formatReportTitle(ReportType reportType, LocalDateTime timestamp) {
        return String.format("%s - Generated on %s", 
            reportType.getDisplayName(), 
            timestamp.toString().replace("T", " "));
    }
    
    /**
     * Default method to validate report parameters.
     * 
     * @param parameters The parameters to validate
     * @return true if parameters are valid, false otherwise
     */
    default boolean validateReportParameters(Map<String, Object> parameters) {
        return parameters != null && !parameters.isEmpty();
    }
    
    /**
     * Static utility method to create default parameters.
     * 
     * @return Map with default report parameters
     */
    static Map<String, Object> createDefaultParameters() {
        return Map.of(
            "includeInactive", false,
            "includeArchived", false,
            "sortBy", "name",
            "sortOrder", "ASC"
        );
    }
    
    /**
     * Inner class representing report data.
     */
    class ReportData {
        private final String reportId;
        private final ReportType reportType;
        private final String title;
        private final LocalDateTime generatedAt;
        private final String content;
        private final Map<String, Object> metadata;
        private final List<String> columns;
        private final List<Map<String, Object>> rows;
        
        public ReportData(String reportId, ReportType reportType, String title, String content) {
            this.reportId = reportId;
            this.reportType = reportType;
            this.title = title;
            this.content = content;
            this.generatedAt = LocalDateTime.now();
            this.metadata = Map.of();
            this.columns = List.of();
            this.rows = List.of();
        }
        
        public ReportData(String reportId, ReportType reportType, String title, 
                         List<String> columns, List<Map<String, Object>> rows, 
                         Map<String, Object> metadata) {
            this.reportId = reportId;
            this.reportType = reportType;
            this.title = title;
            this.columns = columns;
            this.rows = rows;
            this.metadata = metadata;
            this.generatedAt = LocalDateTime.now();
            this.content = "";
        }
        
        // Getters
        public String getReportId() { return reportId; }
        public ReportType getReportType() { return reportType; }
        public String getTitle() { return title; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public List<String> getColumns() { return columns; }
        public List<Map<String, Object>> getRows() { return rows; }
        
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return columns.size(); }
        
        @Override
        public String toString() {
            return String.format("ReportData{id='%s', type='%s', title='%s', rows=%d, generated=%s}",
                reportId, reportType.getDisplayName(), title, getRowCount(), generatedAt);
        }
    }
    
    /**
     * Inner class representing report metadata.
     */
    class ReportMetadata {
        private final String reportId;
        private final ReportType reportType;
        private final String title;
        private final LocalDateTime generatedAt;
        private final String generatedBy;
        private final long fileSize;
        private final ReportFormat format;
        private final String filePath;
        
        public ReportMetadata(String reportId, ReportType reportType, String title, 
                             LocalDateTime generatedAt, String generatedBy, 
                             long fileSize, ReportFormat format, String filePath) {
            this.reportId = reportId;
            this.reportType = reportType;
            this.title = title;
            this.generatedAt = generatedAt;
            this.generatedBy = generatedBy;
            this.fileSize = fileSize;
            this.format = format;
            this.filePath = filePath;
        }
        
        // Getters
        public String getReportId() { return reportId; }
        public ReportType getReportType() { return reportType; }
        public String getTitle() { return title; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public String getGeneratedBy() { return generatedBy; }
        public long getFileSize() { return fileSize; }
        public ReportFormat getFormat() { return format; }
        public String getFilePath() { return filePath; }
        
        @Override
        public String toString() {
            return String.format("ReportMetadata{id='%s', type='%s', generated=%s, size=%d bytes}",
                reportId, reportType.getDisplayName(), generatedAt, fileSize);
        }
    }
}