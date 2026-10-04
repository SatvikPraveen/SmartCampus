package interfaces;

import interfaces.EventListener.Event;
import interfaces.EventListener.EventHandlingException;
import interfaces.EventListener.EventPriority;
import interfaces.EventListener.ProcessingMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventListenerTest {

    /** A listener that only implements the abstract methods, leaving every default untouched. */
    private static final class MinimalListener implements EventListener<String> {
        @Override public void handleEvent(Event<String> event) { event.markProcessed(); }
        @Override public CompletableFuture<Void> handleEventAsync(Event<String> event) {
            return CompletableFuture.runAsync(event::markProcessed);
        }
        @Override public boolean canHandle(String eventType) { return "GRADE_POSTED".equals(eventType); }
        @Override public boolean canHandle(Event<String> event) { return canHandle(event.getEventType()); }
        @Override public EventPriority getPriority() { return EventPriority.HIGH; }
        @Override public ProcessingMode getProcessingMode() { return ProcessingMode.SYNCHRONOUS; }
        @Override public String[] getSupportedEventTypes() { return new String[]{"GRADE_POSTED"}; }
    }

    private static Event<String> event() {
        return new Event<>("GRADE_POSTED", "A+", "GradeService");
    }

    @Nested
    class DefaultMethods {

        private final MinimalListener listener = new MinimalListener();

        @Test
        void lifecycleHooksAreNoOps() {
            Event<String> e = event();

            assertThatCode(() -> {
                listener.beforeEventHandling(e);
                listener.afterEventHandling(e, "result");
                listener.onEventHandlingError(e, new RuntimeException());
                listener.onEventHandlingCancelled(e);
            }).doesNotThrowAnyException();
            assertThat(e.isProcessed()).isFalse();
            assertThat(e.isCancelled()).isFalse();
        }

        @Test
        void defaultsForIdEnablementAndTimeout() {
            assertThat(listener.getListenerId())
                    .isEqualTo("MinimalListener@" + System.identityHashCode(listener));
            assertThat(listener.isEnabled()).isTrue();
            assertThat(listener.getTimeoutMillis()).isEqualTo(30_000);
        }

        @Test
        void timeoutThrowsWithEventType() {
            assertThatThrownBy(() -> listener.onEventTimeout(event()))
                    .isInstanceOf(EventHandlingException.class)
                    .hasMessage("Event handling timed out for event: GRADE_POSTED");
        }

        @Test
        void stubHandlesEventsSyncAndAsync() throws Exception {
            Event<String> sync = event();
            Event<String> async = event();

            listener.handleEvent(sync);
            listener.handleEventAsync(async).get();

            assertThat(sync.isProcessed()).isTrue();
            assertThat(async.isProcessed()).isTrue();
            assertThat(listener.canHandle(sync)).isTrue();
            assertThat(listener.canHandle("OTHER")).isFalse();
        }
    }

    @Nested
    class Events {

        @Test
        void basicConstructorDefaults() {
            Event<String> e = event();

            assertThat(e.getEventId()).startsWith("EVT_");
            assertThat(e.getEventType()).isEqualTo("GRADE_POSTED");
            assertThat(e.getPayload()).isEqualTo("A+");
            assertThat(e.getSource()).isEqualTo("GradeService");
            assertThat(e.getTimestamp()).isNotNull();
            assertThat(e.getMetadata()).isEmpty();
            assertThat(e.getPriority()).isEqualTo(EventPriority.NORMAL);
            assertThat(e.getCorrelationId()).isNull();
            assertThat(e.getUserId()).isNull();
        }

        @Test
        void priorityConstructor() {
            assertThat(new Event<>("T", 1, "src", EventPriority.HIGHEST).getPriority())
                    .isEqualTo(EventPriority.HIGHEST);
        }

        // Regression: the 4-arg constructor stored a null priority while the full constructor defaulted it to NORMAL.
        @Test
        void nullPriorityDefaultsToNormalInEveryConstructor() {
            assertThat(new Event<>("T", 1, "src", null).getPriority()).isEqualTo(EventPriority.NORMAL);
            assertThat(new Event<>("T", 1, "src", null, null, null, null).getPriority())
                    .isEqualTo(EventPriority.NORMAL);
        }

        @Test
        void fullConstructorAndMetadataAccess() {
            Event<String> e = new Event<>("T", "p", "src", Map.of("count", 3, "who", "bob"),
                    EventPriority.LOW, "corr", "U1");

            assertThat(e.getCorrelationId()).isEqualTo("corr");
            assertThat(e.getUserId()).isEqualTo("U1");
            assertThat(e.getMetadata("who")).isEqualTo("bob");
            assertThat(e.getMetadata("count", Integer.class)).isEqualTo(3);
            assertThat(e.getMetadata("count", Number.class)).isEqualTo(3);
            assertThat(e.getMetadata("count", String.class)).isNull();
            assertThat(e.getMetadata("missing", String.class)).isNull();
            assertThat(e.hasMetadata("who")).isTrue();
            assertThat(e.hasMetadata("missing")).isFalse();
            assertThat(new Event<>("T", "p", "src", null, null, null, null).getMetadata()).isEmpty();
        }

        @Test
        void cancelAndProcessFlags() {
            Event<String> e = event();

            e.cancel();
            e.markProcessed();

            assertThat(e.isCancelled()).isTrue();
            assertThat(e.isProcessed()).isTrue();
            assertThat(e.toString()).startsWith("Event{id='EVT_")
                    .contains("type='GRADE_POSTED'", "source='GradeService'", "priority=NORMAL",
                            "cancelled=true", "processed=true");
        }

        @Test
        void ageAndExpiry() {
            Event<String> e = event();

            assertThat(e.getAgeMillis()).isGreaterThanOrEqualTo(0);
            assertThat(e.isExpired(60_000)).isFalse();
            assertThat(e.isExpired(-1)).isTrue();
        }

        @Test
        void withPayloadCopiesEverythingButPayload() {
            Map<String, Object> meta = new HashMap<>(Map.of("k", "v"));
            Event<String> original = new Event<>("T", "p", "src", meta, EventPriority.HIGH, "corr", "U1");

            Event<Integer> copy = original.withPayload(99);

            assertThat(copy.getPayload()).isEqualTo(99);
            assertThat(copy.getEventType()).isEqualTo("T");
            assertThat(copy.getSource()).isEqualTo("src");
            assertThat(copy.getPriority()).isEqualTo(EventPriority.HIGH);
            assertThat(copy.getCorrelationId()).isEqualTo("corr");
            assertThat(copy.getUserId()).isEqualTo("U1");
            assertThat(copy.getMetadata()).containsEntry("k", "v");
            assertThat(copy.isProcessed()).isFalse();
        }
    }

    @Nested
    class Exceptions {

        @Test
        void simpleConstructors() {
            Throwable cause = new RuntimeException();
            EventHandlingException plain = new EventHandlingException("m");
            EventHandlingException withCause = new EventHandlingException("m", cause);

            assertThat(plain).hasMessage("m");
            assertThat(plain.getEventType()).isNull();
            assertThat(plain.getEventId()).isNull();
            assertThat(plain.getListenerId()).isNull();
            assertThat(withCause).hasCause(cause);
        }

        @Test
        void detailedConstructors() {
            EventHandlingException e = new EventHandlingException("T", "E1", "L1", "boom");
            assertThat(e).hasMessage("Event handling failed for event 'T' (ID: E1) in listener 'L1': boom");
            assertThat(e.getEventType()).isEqualTo("T");
            assertThat(e.getEventId()).isEqualTo("E1");
            assertThat(e.getListenerId()).isEqualTo("L1");

            Throwable cause = new IllegalStateException();
            assertThat(new EventHandlingException("T", "E1", "L1", "boom", cause))
                    .hasCause(cause).hasMessageEndingWith("in listener 'L1': boom");
        }
    }

    @Test
    void enums() {
        assertThat(Stream.of(EventPriority.values()).map(EventPriority::getLevel)).containsExactly(1, 2, 3, 4, 5);
        assertThat(EventPriority.HIGHEST.getDisplayName()).isEqualTo("Highest");
        assertThat(ProcessingMode.BATCH.getDisplayName()).isEqualTo("Batch");
    }
}
