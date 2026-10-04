package events;

import events.Event.Category;
import events.Event.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    private static TestEvent plain() {
        return new TestEvent("Thing", "payload");
    }

    /** Reconstructs an event with an explicit timestamp and metadata. */
    private static TestEvent at(LocalDateTime timestamp, Map<String, Object> metadata) {
        return new TestEvent("evt-1", "Thing", timestamp, "src", "corr", 3, Priority.LOW,
                null, null, null, metadata, "old", true);
    }

    @Nested
    class Construction {

        @Test
        void basicConstructorFillsDefaults() {
            LocalDateTime before = LocalDateTime.now();
            TestEvent event = plain();

            assertThat(UUID.fromString(event.getEventId())).isNotNull();
            assertThat(event.getEventType()).isEqualTo("Thing");
            assertThat(event.getTimestamp()).isBetween(before, LocalDateTime.now());
            assertThat(event.getSourceSystem()).isEqualTo("campus-management-system");
            assertThat(UUID.fromString(event.getCorrelationId())).isNotNull();
            assertThat(event.getVersion()).isEqualTo(1);
            assertThat(event.getPriority()).isEqualTo(Priority.NORMAL);
            assertThat(event.hasAggregate()).isFalse();
            assertThat(event.getAggregateId()).isNull();
            assertThat(event.getAggregateType()).isNull();
            assertThat(event.getAggregateVersion()).isNull();
        }

        @Test
        void eachEventGetsItsOwnIdsAndCorrelation() {
            TestEvent a = plain();
            TestEvent b = plain();
            assertThat(a.getEventId()).isNotEqualTo(b.getEventId());
            assertThat(a.getCorrelationId()).isNotEqualTo(b.getCorrelationId());
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void defaultMetadataIsInitialised() {
            assertThat(plain().getMetadata())
                    .containsEntry("createdBy", "system")
                    .containsKeys("environment", "version", "hostname");
        }

        @Test
        void priorityConstructorKeepsPriorityAndDefaultsNullToNormal() {
            assertThat(new TestEvent("T", Priority.CRITICAL, "x").getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(new TestEvent("T", (Priority) null, "x").getPriority()).isEqualTo(Priority.NORMAL);
        }

        @Test
        void aggregateConstructorRecordsAggregate() {
            TestEvent event = new TestEvent("T", "S1", "Student", 4L);

            assertThat(event.hasAggregate()).isTrue();
            assertThat(event.getAggregateId()).isEqualTo("S1");
            assertThat(event.getAggregateType()).isEqualTo("Student");
            assertThat(event.getAggregateVersion()).isEqualTo(4L);
            assertThat(event.isAggregateType("Student")).isTrue();
            assertThat(event.isAggregateType("Course")).isFalse();
            assertThat(event.isForAggregate("Student", "S1")).isTrue();
            assertThat(event.isForAggregate("Student", "S2")).isFalse();
            assertThat(event.isForAggregate("Course", "S1")).isFalse();
        }

        @Test
        void aggregateNeedsBothIdAndType() {
            assertThat(new TestEvent("T", "S1", null, null).hasAggregate()).isFalse();
            assertThat(new TestEvent("T", null, "Student", null).hasAggregate()).isFalse();
        }

        @Test
        void reconstructionKeepsGivenValuesAndCopiesMetadata() {
            LocalDateTime ts = LocalDateTime.of(2024, 1, 2, 3, 4);
            Map<String, Object> metadata = new HashMap<>(Map.of("k", "v"));
            TestEvent event = at(ts, metadata);
            metadata.put("later", 1);

            assertThat(event.getEventId()).isEqualTo("evt-1");
            assertThat(event.getTimestamp()).isEqualTo(ts);
            assertThat(event.getSourceSystem()).isEqualTo("src");
            assertThat(event.getCorrelationId()).isEqualTo("corr");
            assertThat(event.getVersion()).isEqualTo(3);
            assertThat(event.getPriority()).isEqualTo(Priority.LOW);
            assertThat(event.getMetadata()).containsExactlyEntriesOf(Map.of("k", "v"));
            assertThat(at(ts, null).getMetadata()).isEmpty();
        }
    }

    @Nested
    class Metadata {

        @Test
        void addGetRemoveAndHas() {
            TestEvent event = plain();
            event.addMetadata("count", 3);

            assertThat(event.hasMetadata("count")).isTrue();
            assertThat(event.getMetadata("count")).isEqualTo(3);
            assertThat(event.getMetadata("count", Integer.class)).isEqualTo(3);
            assertThat(event.getMetadata("count", String.class)).isNull();
            assertThat(event.getMetadata("missing", String.class)).isNull();
            assertThat(event.removeMetadata("count")).isEqualTo(3);
            assertThat(event.hasMetadata("count")).isFalse();
            assertThat(event.getMetadata("count")).isNull();
        }

        @Test
        void nullKeysAndValuesAreIgnored() {
            TestEvent event = plain();
            int before = event.getMetadata().size();

            event.addMetadata(null, "v");
            event.addMetadata("k", null);
            event.addMetadata((Map<String, Object>) null);

            assertThat(event.getMetadata()).hasSize(before);
            assertThat(event.hasMetadata("k")).isFalse();
        }

        @Test
        void addAllMergesEntries() {
            TestEvent event = plain();
            event.addMetadata(Map.of("a", 1, "createdBy", "alice"));

            assertThat(event.getMetadata()).containsEntry("a", 1).containsEntry("createdBy", "alice");
        }

        @Test
        void getMetadataReturnsDefensiveCopy() {
            TestEvent event = plain();
            event.getMetadata().put("injected", true);
            assertThat(event.hasMetadata("injected")).isFalse();
        }

        @Test
        void clearMetadataRestoresDefaults() {
            TestEvent event = plain();
            event.addMetadata("custom", 1);

            event.resetMetadata();

            assertThat(event.hasMetadata("custom")).isFalse();
            assertThat(event.getMetadata()).containsEntry("createdBy", "system").hasSize(4);
        }
    }

    @Nested
    class Age {

        @Test
        void newEventIsRecentAndNotStale() {
            TestEvent event = plain();
            assertThat(event.getAgeInMillis()).isBetween(0L, 60_000L);
            assertThat(event.isRecent(60)).isTrue();
            assertThat(event.isStale(60)).isFalse();
        }

        @Test
        void oldEventIsStaleAndNotRecent() {
            TestEvent event = at(LocalDateTime.now().minus(2, ChronoUnit.MINUTES), null);
            assertThat(event.getAgeInMillis()).isGreaterThanOrEqualTo(120_000L);
            assertThat(event.isRecent(60)).isFalse();
            assertThat(event.isStale(60)).isTrue();
        }
    }

    @Nested
    class Copies {

        @Test
        void withVersionKeepsIdentityAndChangesVersion() {
            TestEvent original = new TestEvent("T", "S1", "Student", 2L);
            original.addMetadata("k", "v");

            Event copy = original.withVersion(5);

            assertThat(copy).isInstanceOf(TestEvent.class);
            assertThat(copy.getVersion()).isEqualTo(5);
            assertThat(original.getVersion()).isEqualTo(1);
            assertThat(copy.getEventId()).isEqualTo(original.getEventId());
            assertThat(copy.getTimestamp()).isEqualTo(original.getTimestamp());
            assertThat(copy.getCorrelationId()).isEqualTo(original.getCorrelationId());
            assertThat(copy.getAggregateId()).isEqualTo("S1");
            assertThat(copy.getAggregateVersion()).isEqualTo(2L);
            assertThat(copy.getMetadata()).isEqualTo(original.getMetadata());
            assertThat(((TestEvent) copy).getLabel()).isEqualTo("aggregate");
        }

        @Test
        void withCorrelationIdAndWithPriorityChangeOnlyThatField() {
            TestEvent original = plain();

            Event correlated = original.withCorrelationId("corr-9");
            Event urgent = original.withPriority(Priority.CRITICAL);

            assertThat(correlated.getCorrelationId()).isEqualTo("corr-9");
            assertThat(correlated.getPriority()).isEqualTo(Priority.NORMAL);
            assertThat(urgent.getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(urgent.getCorrelationId()).isEqualTo(original.getCorrelationId());
            assertThat(original.getPriority()).isEqualTo(Priority.NORMAL);
        }

        @Test
        void copiesDoNotShareMetadataWithOriginal() {
            TestEvent original = plain();
            Event copy = original.withVersion(2);

            copy.addMetadata("onlyCopy", true);

            assertThat(original.hasMetadata("onlyCopy")).isFalse();
        }
    }

    @Nested
    class Serialization {

        @Test
        void toMapContainsCoreFieldsAndPayload() {
            TestEvent event = new TestEvent("T", "S1", "Student", 7L);

            Map<String, Object> map = event.toMap();

            assertThat(map)
                    .containsEntry("eventId", event.getEventId())
                    .containsEntry("eventType", "T")
                    .containsEntry("timestamp", event.getTimestamp().toString())
                    .containsEntry("sourceSystem", event.getSourceSystem())
                    .containsEntry("correlationId", event.getCorrelationId())
                    .containsEntry("version", 1)
                    .containsEntry("priority", "NORMAL")
                    .containsEntry("category", "SYSTEM")
                    .containsEntry("aggregateId", "S1")
                    .containsEntry("aggregateType", "Student")
                    .containsEntry("aggregateVersion", 7L)
                    .containsEntry("payload", "aggregate")
                    .containsEntry("description", "Test event aggregate")
                    .containsEntry("valid", true)
                    .containsEntry("metadata", event.getMetadata());
        }

        @Test
        void toMapOmitsAbsentAggregateFields() {
            assertThat(plain().toMap()).doesNotContainKeys("aggregateId", "aggregateType", "aggregateVersion");
        }

        // Regression: toMap()/toLogEntry() exposed the event's internal metadata map, bypassing the
        // defensive copy made by getMetadata().
        @Test
        void serializedMetadataIsDetachedFromTheEvent() {
            TestEvent event = plain();

            @SuppressWarnings("unchecked")
            Map<String, Object> fromMap = (Map<String, Object>) event.toMap().get("metadata");
            fromMap.put("leak1", true);
            @SuppressWarnings("unchecked")
            Map<String, Object> fromLog = (Map<String, Object>) event.toLogEntry().get("event.metadata");
            fromLog.put("leak2", true);

            assertThat(event.hasMetadata("leak1")).isFalse();
            assertThat(event.hasMetadata("leak2")).isFalse();
        }

        @Test
        void logEntryIncludesAggregatePayloadAndMetadataWhenPresent() {
            TestEvent event = new TestEvent("T", "S1", "Student", 7L);

            Map<String, Object> log = event.toLogEntry();

            assertThat(log)
                    .containsEntry("@timestamp", event.getTimestamp().toString())
                    .containsEntry("event.id", event.getEventId())
                    .containsEntry("event.type", "T")
                    .containsEntry("event.category", "SYSTEM")
                    .containsEntry("event.priority", "NORMAL")
                    .containsEntry("event.source", event.getSourceSystem())
                    .containsEntry("event.correlation_id", event.getCorrelationId())
                    .containsEntry("event.version", 1)
                    .containsEntry("event.description", "Test event aggregate")
                    .containsEntry("aggregate.type", "Student")
                    .containsEntry("aggregate.id", "S1")
                    .containsEntry("aggregate.version", 7L)
                    .containsEntry("event.payload", "aggregate")
                    .containsKey("event.metadata");
        }

        @Test
        void logEntryOmitsMissingAggregatePayloadAndMetadata() {
            TestEvent event = new TestEvent("evt", "T", LocalDateTime.now(), "src", "c", 1, Priority.HIGH,
                    null, null, null, null, null, true);

            assertThat(event.toLogEntry())
                    .doesNotContainKeys("aggregate.type", "aggregate.id", "aggregate.version",
                            "event.payload", "event.metadata")
                    .containsEntry("event.priority", "HIGH");
        }
    }

    @Nested
    class EqualityAndText {

        @Test
        void equalityIsBasedOnIdentityFields() {
            TestEvent event = plain();

            assertThat(event).isEqualTo(event);
            assertThat(event).isNotEqualTo(null).isNotEqualTo("x");
            // A copy keeps id/type/timestamp/aggregate, so it is the same event even with another version
            assertThat(event.withVersion(9)).isEqualTo(event).hasSameHashCodeAs(event);
            assertThat(new TestEvent.Special("Thing", "payload")).isNotEqualTo(event);
        }

        @Test
        void toStringDescribesEvent() {
            TestEvent event = new TestEvent("T", "S1", "Student", 7L);

            assertThat(event.toString())
                    .startsWith("TestEvent{")
                    .contains("eventId='" + event.getEventId() + "'", "eventType='T'", "priority=NORMAL",
                            "category=SYSTEM", "aggregateType='Student'", "aggregateId='S1'",
                            "aggregateVersion=7", "valid=true", "description='Test event aggregate'");
            assertThat(plain().toString()).doesNotContain("aggregateType");
        }

        @Test
        void priorityAndCategoryEnumsExposeLabels() {
            assertThat(Priority.LOW.getLevel()).isLessThan(Priority.NORMAL.getLevel());
            assertThat(Priority.HIGH.getLevel()).isLessThan(Priority.CRITICAL.getLevel());
            assertThat(Priority.CRITICAL.getDescription()).isEqualTo("Critical Priority");
            assertThat(Category.AUDIT.getDisplayName()).isEqualTo("Audit Event");
            assertThat(Category.AUDIT.getDescription()).isEqualTo("Audit trail and compliance events");
            assertThat(Category.values()).hasSize(7);
        }
    }
}
