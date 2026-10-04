package interfaces;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportMetadata;
import interfaces.Reportable.ReportType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ReportableTest {

    /** Implements only the abstract methods so the defaults can be exercised. */
    private static final class StubReportable implements Reportable {
        @Override public ReportData generateReport(ReportType t) { return new ReportData("R1", t, "t", "c"); }
        @Override public ReportData generateReport(ReportType t, Map<String, Object> p) { return generateReport(t); }
        @Override public ReportData generateReportForDateRange(ReportType t, LocalDateTime s, LocalDateTime e) {
            return generateReport(t);
        }
        @Override public boolean exportReport(ReportData d, ReportFormat f, String p) { return true; }
        @Override public List<ReportType> getAvailableReportTypes() { return List.of(ReportType.values()); }
        @Override public List<ReportFormat> getSupportedFormats() { return List.of(ReportFormat.CSV); }
        @Override public String scheduleRecurringReport(ReportType t, String f, List<String> r) { return "S1"; }
        @Override public boolean cancelScheduledReport(String id) { return true; }
        @Override public List<ReportMetadata> getReportHistory(ReportType t, int limit) { return List.of(); }
        @Override public Map<String, Object> getSummaryStatistics() { return Map.of(); }
    }

    private final Reportable reportable = new StubReportable();

    @Nested
    class DefaultMethods {

        @Test
        void formatReportTitleUsesDisplayNameAndSpaceSeparatedTimestamp() {
            String title = reportable.formatReportTitle(ReportType.GRADE_REPORT, LocalDateTime.of(2024, 5, 1, 13, 45));

            assertThat(title).isEqualTo("Grade Report - Generated on 2024-05-01 13:45");
        }

        @Test
        void validateReportParametersRequiresNonEmptyMap() {
            assertThat(reportable.validateReportParameters(null)).isFalse();
            assertThat(reportable.validateReportParameters(Map.of())).isFalse();
            assertThat(reportable.validateReportParameters(Map.of("k", 1))).isTrue();
        }

        @Test
        void defaultParameters() {
            Map<String, Object> params = Reportable.createDefaultParameters();

            assertThat(params).containsOnly(Map.entry("includeInactive", false), Map.entry("includeArchived", false),
                    Map.entry("sortBy", "name"), Map.entry("sortOrder", "ASC"));
            assertThat(reportable.validateReportParameters(params)).isTrue();
        }
    }

    @Nested
    class Data {

        @Test
        void contentReportHasNoTabularData() {
            ReportData d = reportable.generateReport(ReportType.STATISTICAL_SUMMARY);

            assertThat(d.getReportId()).isEqualTo("R1");
            assertThat(d.getReportType()).isEqualTo(ReportType.STATISTICAL_SUMMARY);
            assertThat(d.getTitle()).isEqualTo("t");
            assertThat(d.getContent()).isEqualTo("c");
            assertThat(d.getGeneratedAt()).isNotNull();
            assertThat(d.getMetadata()).isEmpty();
            assertThat(d.getColumns()).isEmpty();
            assertThat(d.getRows()).isEmpty();
            assertThat(d.getRowCount()).isZero();
            assertThat(d.getColumnCount()).isZero();
        }

        @Test
        void tabularReport() {
            ReportData d = new ReportData("R2", ReportType.ENROLLMENT_REPORT, "Enrollments",
                    List.of("student", "course"), List.of(Map.of("student", "S1", "course", "C1")), Map.of("v", 1));

            assertThat(d.getContent()).isEmpty();
            assertThat(d.getRowCount()).isEqualTo(1);
            assertThat(d.getColumnCount()).isEqualTo(2);
            assertThat(d.getMetadata()).containsEntry("v", 1);
            assertThat(d.toString()).startsWith("ReportData{id='R2', type='Enrollment Report', title='Enrollments', rows=1");
        }

        // Regression: null columns/rows/metadata made getRowCount(), getColumnCount() and toString() throw NPE.
        @Test
        void tabularReportToleratesNullCollections() {
            ReportData d = new ReportData("R3", ReportType.GRADE_REPORT, "t", null, null, null);

            assertThatCode(d::toString).doesNotThrowAnyException();
            assertThat(d.getRowCount()).isZero();
            assertThat(d.getColumnCount()).isZero();
            assertThat(d.getColumns()).isEmpty();
            assertThat(d.getMetadata()).isEmpty();
        }

        @Test
        void metadata() {
            LocalDateTime at = LocalDateTime.of(2024, 1, 2, 3, 4);
            ReportMetadata m = new ReportMetadata("R1", ReportType.FINANCIAL_REPORT, "Fin", at, "admin", 2048,
                    ReportFormat.PDF, "/tmp/r.pdf");

            assertThat(m.getReportId()).isEqualTo("R1");
            assertThat(m.getReportType()).isEqualTo(ReportType.FINANCIAL_REPORT);
            assertThat(m.getTitle()).isEqualTo("Fin");
            assertThat(m.getGeneratedAt()).isEqualTo(at);
            assertThat(m.getGeneratedBy()).isEqualTo("admin");
            assertThat(m.getFileSize()).isEqualTo(2048);
            assertThat(m.getFormat()).isEqualTo(ReportFormat.PDF);
            assertThat(m.getFilePath()).isEqualTo("/tmp/r.pdf");
            assertThat(m.toString())
                    .isEqualTo("ReportMetadata{id='R1', type='Financial Report', generated=2024-01-02T03:04, size=2048 bytes}");
        }
    }

    @Test
    void enumDisplayNames() {
        assertThat(Stream.of(ReportType.values()).map(ReportType::getDisplayName)).doesNotHaveDuplicates();
        assertThat(ReportFormat.TEXT.getDisplayName()).isEqualTo("Plain Text");
        assertThat(Stream.of(ReportFormat.values()).map(ReportFormat::getDisplayName)).doesNotHaveDuplicates();
    }
}
