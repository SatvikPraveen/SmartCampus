package interfaces;

import interfaces.Auditable.AuditAction;
import interfaces.Auditable.AuditLevel;
import interfaces.Auditable.AuditRecord;
import interfaces.Auditable.AuditSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AuditableTest {

    /** Records every audit event so the default convenience methods can be verified. */
    private static final class RecordingAuditable implements Auditable {
        final List<AuditRecord> records = new ArrayList<>();

        @Override
        public AuditRecord logAuditEvent(AuditAction action, String entityId, String userId, String details) {
            return logAuditEvent(action, AuditLevel.INFO, entityId, userId, details);
        }

        @Override
        public AuditRecord logAuditEvent(AuditAction action, AuditLevel level, String entityId, String userId,
                                         String details) {
            return logAuditEvent(action, level, entityId, userId, details, null);
        }

        @Override
        public AuditRecord logAuditEvent(AuditAction action, AuditLevel level, String entityId, String userId,
                                         String details, Map<String, Object> metadata) {
            AuditRecord r = new AuditRecord("A" + records.size(), action, level, entityId, "Entity", userId, null,
                    details, metadata, null, null, null);
            records.add(r);
            return r;
        }

        @Override public List<AuditRecord> getAuditHistory(String entityId) { return records; }
        @Override public List<AuditRecord> getAuditHistory(String e, LocalDateTime s, LocalDateTime t) { return records; }
        @Override public List<AuditRecord> getUserAuditHistory(String userId) { return records; }
        @Override public List<AuditRecord> getAuditHistoryByAction(AuditAction action) { return records; }
        @Override public List<AuditRecord> getAuditHistoryByLevel(AuditLevel level) { return records; }
        @Override public List<AuditRecord> searchAuditLogs(Map<String, Object> criteria) { return records; }
        @Override public List<AuditRecord> getRecentAuditEvents(int limit) { return records; }
        @Override public Map<String, Object> getAuditStatistics() { return Map.of(); }
        @Override public int archiveAuditRecords(LocalDateTime beforeDate) { return 0; }
        @Override public int purgeAuditRecords(LocalDateTime beforeDate) { return 0; }
        @Override public boolean exportAuditLogs(LocalDateTime s, LocalDateTime e, String f, String p) { return false; }
    }

    private static AuditRecord record(AuditAction action, AuditLevel level) {
        return new AuditRecord("id", action, level, "E1", "Student", "U1", null, "d");
    }

    @Nested
    class DefaultMethods {

        private RecordingAuditable auditable;

        @BeforeEach
        void setUp() {
            auditable = new RecordingAuditable();
        }

        @Test
        void logCreateUpdateAndDelete() {
            AuditRecord created = auditable.logCreate("E1", "U1");
            AuditRecord updated = auditable.logUpdate("E1", "U1", "name changed");
            AuditRecord deleted = auditable.logDelete("E1", "U1");

            assertThat(created.getAction()).isEqualTo(AuditAction.CREATE);
            assertThat(created.getDetails()).isEqualTo("Entity created");
            assertThat(updated.getAction()).isEqualTo(AuditAction.UPDATE);
            assertThat(updated.getDetails()).isEqualTo("Entity updated: name changed");
            assertThat(deleted.getAction()).isEqualTo(AuditAction.DELETE);
            assertThat(deleted.getDetails()).isEqualTo("Entity deleted");
            assertThat(auditable.records).containsExactly(created, updated, deleted);
            assertThat(auditable.records).allSatisfy(r -> {
                assertThat(r.getEntityId()).isEqualTo("E1");
                assertThat(r.getUserId()).isEqualTo("U1");
            });
        }

        @Test
        void logDataAccessIsASecurityEvent() {
            AuditRecord r = auditable.logDataAccess("E1", "U1", "export");

            assertThat(r.getAction()).isEqualTo(AuditAction.ACCESS);
            assertThat(r.getLevel()).isEqualTo(AuditLevel.SECURITY);
            assertThat(r.getDetails()).isEqualTo("Data accessed: export");
            assertThat(r.isSecurityRelated()).isTrue();
        }
    }

    @Nested
    class AuditRecords {

        @Test
        void shortConstructorDefaults() {
            AuditRecord r = new AuditRecord("id", AuditAction.GRADE, AuditLevel.INFO, "E1", "Grade", "U1", "Prof X", "d");

            assertThat(r.getAuditId()).isEqualTo("id");
            assertThat(r.getEntityType()).isEqualTo("Grade");
            assertThat(r.getUserName()).isEqualTo("Prof X");
            assertThat(r.getMetadata()).isEmpty();
            assertThat(r.getIpAddress()).isNull();
            assertThat(r.getUserAgent()).isNull();
            assertThat(r.getSessionId()).isNull();
            assertThat(r.getTimestamp()).isNotNull();
        }

        @Test
        void fullConstructorKeepsRequestDetails() {
            AuditRecord r = new AuditRecord("id", AuditAction.LOGIN, AuditLevel.INFO, "E1", "User", "U1", null, "d",
                    Map.of("k", "v"), "1.2.3.4", "ua", "s1");

            assertThat(r.getMetadata()).containsEntry("k", "v");
            assertThat(r.getIpAddress()).isEqualTo("1.2.3.4");
            assertThat(r.getUserAgent()).isEqualTo("ua");
            assertThat(r.getSessionId()).isEqualTo("s1");
        }

        @ParameterizedTest
        @EnumSource(AuditLevel.class)
        void securityRelatedByLevel(AuditLevel level) {
            boolean expected = level == AuditLevel.SECURITY || level == AuditLevel.CRITICAL;
            assertThat(record(AuditAction.UPDATE, level).isSecurityRelated()).isEqualTo(expected);
        }

        @ParameterizedTest
        @EnumSource(AuditAction.class)
        void securityRelatedByAction(AuditAction action) {
            Set<AuditAction> security = EnumSet.of(AuditAction.LOGIN, AuditAction.LOGOUT, AuditAction.ACCESS);
            assertThat(record(action, AuditLevel.INFO).isSecurityRelated()).isEqualTo(security.contains(action));
        }

        @ParameterizedTest
        @EnumSource(AuditLevel.class)
        void errorLevels(AuditLevel level) {
            boolean expected = level == AuditLevel.ERROR || level == AuditLevel.CRITICAL;
            assertThat(record(AuditAction.UPDATE, level).isError()).isEqualTo(expected);
        }

        @Test
        void summaryPrefersUserNameOverId() {
            AuditRecord named = new AuditRecord("id", AuditAction.ENROLL, AuditLevel.ADMIN, "C1", "Course", "U1",
                    "Alice", "enrolled");

            assertThat(named.getSummary()).isEqualTo("[Administrative] Enroll by Alice on C1: enrolled");
            assertThat(record(AuditAction.DROP, AuditLevel.WARNING).getSummary())
                    .isEqualTo("[Warning] Drop by U1 on E1: d");
        }

        @Test
        void toStringShowsIdentity() {
            assertThat(record(AuditAction.CREATE, AuditLevel.INFO).toString())
                    .startsWith("AuditRecord{id='id', action=CREATE, level=INFO, entity='E1', user='U1'");
        }
    }

    @Nested
    class SearchCriteria {

        @Test
        void fluentBuilderSetsAllFields() {
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = start.plusDays(1);
            AuditSearchCriteria c = new AuditSearchCriteria()
                    .withEntityId("E1").withUserId("U1").withAction(AuditAction.UPDATE).withLevel(AuditLevel.ERROR)
                    .withDateRange(start, end).withDetails("grade").withIpAddress("ip").withSessionId("s");

            assertThat(c.getEntityId()).isEqualTo("E1");
            assertThat(c.getUserId()).isEqualTo("U1");
            assertThat(c.getAction()).isEqualTo(AuditAction.UPDATE);
            assertThat(c.getLevel()).isEqualTo(AuditLevel.ERROR);
            assertThat(c.getStartDate()).isEqualTo(start);
            assertThat(c.getEndDate()).isEqualTo(end);
            assertThat(c.getDetails()).isEqualTo("grade");
            assertThat(c.getIpAddress()).isEqualTo("ip");
            assertThat(c.getSessionId()).isEqualTo("s");
            assertThat(c.toString()).isEqualTo("AuditSearchCriteria{entity='E1', user='U1', action=UPDATE, level=ERROR, "
                    + "dateRange=2024-01-01T00:00 to 2024-01-02T00:00}");
        }

        @Test
        void twoArgConstructor() {
            AuditSearchCriteria c = new AuditSearchCriteria("E1", "U1");

            assertThat(c.getEntityId()).isEqualTo("E1");
            assertThat(c.getUserId()).isEqualTo("U1");
            assertThat(c.getAction()).isNull();
        }
    }

    @Test
    void enumsHaveDistinctDisplayNames() {
        assertThat(Stream.of(AuditAction.values()).map(AuditAction::getDisplayName)).doesNotHaveDuplicates();
        assertThat(Stream.of(AuditLevel.values()).map(AuditLevel::getDisplayName)).doesNotHaveDuplicates();
        assertThat(AuditLevel.ADMIN.getDisplayName()).isEqualTo("Administrative");
    }
}
