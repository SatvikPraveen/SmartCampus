package events;

import events.EventBus.DeadLetterEvent;
import events.EventBus.EventBusStats;
import events.EventBus.EventFilter;
import events.EventBus.EventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class EventBusTest {

    private static final long TIMEOUT_S = 5;

    private final List<EventBus> buses = new ArrayList<>();

    @AfterEach
    void stopBuses() {
        buses.forEach(EventBus::stop);
    }

    /** Synchronous-by-default bus with no retries and a small dead letter queue. */
    private EventBus syncBus() {
        return bus(false, 4, 0, 5);
    }

    private EventBus bus(boolean async, int threads, int maxRetries, int maxDeadLetters) {
        EventBus bus = new EventBus("test", async, threads, maxRetries, 0, maxDeadLetters);
        buses.add(bus);
        return bus;
    }

    private static TestEvent event(String type) {
        return new TestEvent(type, type.toLowerCase());
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(TIMEOUT_S, TimeUnit.SECONDS);
    }

    private static void awaitLatch(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(TIMEOUT_S, TimeUnit.SECONDS)).as("latch released in time").isTrue();
    }

    private static Consumer<Event> failing() {
        return e -> { throw new IllegalStateException("boom"); };
    }

    @Nested
    class Construction {

        @Test
        void defaultBusIsRunningWithEmptyStats() {
            EventBus bus = new EventBus("default");
            buses.add(bus);

            EventBusStats stats = bus.getStats();
            assertThat(bus.getBusName()).isEqualTo("default");
            assertThat(bus.isRunning()).isTrue();
            assertThat(stats.getBusName()).isEqualTo("default");
            assertThat(stats.getEventsPublished()).isZero();
            assertThat(stats.getRegisteredHandlers()).isZero();
            assertThat(stats.getSuccessRate()).isZero();
            assertThat(stats.getFailureRate()).isZero();
            assertThat(stats.isRunning()).isTrue();
            assertThat(stats.getSnapshotTime()).isNotNull();
            assertThat(bus.getAllHandlers()).isEmpty();
            assertThat(bus.toString()).contains("name='default'", "running=true", "handlers=0", "published=0");
        }

        @Test
        void defaultSubscriptionsAreAsyncOnDefaultBus() {
            EventBus bus = new EventBus("default");
            buses.add(bus);
            bus.subscribe("A", e -> { });
            assertThat(bus.getHandlers("A")).singleElement().satisfies(h -> assertThat(h.isAsync()).isTrue());
        }
    }

    @Nested
    class PublishValidation {

        @Test
        void publishRejectsNullAndInvalidEventsWithFailedFuture() {
            EventBus bus = syncBus();

            assertThatThrownBy(() -> await(bus.publish(null)))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> await(bus.publish(new TestEvent("A", "x", false))))
                    .isInstanceOf(ExecutionException.class).hasRootCauseMessage("Event is not valid");
            assertThat(bus.getStats().getEventsPublished()).isZero();
        }

        @Test
        void publishSyncRejectsNullAndInvalidEvents() {
            EventBus bus = syncBus();

            assertThatThrownBy(() -> bus.publishSync(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> bus.publishSync(new TestEvent("A", "x", false)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Event is not valid");
        }

        @Test
        void stoppedBusRejectsPublishingAndSubscribing() {
            EventBus bus = syncBus();
            bus.stop();

            assertThat(bus.isRunning()).isFalse();
            assertThatThrownBy(() -> await(bus.publish(event("A"))))
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> bus.publishSync(event("A"))).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> bus.subscribe("A", e -> { })).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> bus.subscribe(TestEvent.class, e -> { }))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> bus.subscribeGlobal(e -> { })).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void publishAllWithNothingCompletesImmediately() throws Exception {
            EventBus bus = syncBus();
            assertThat(await(bus.publishAll(null))).isNull();
            assertThat(await(bus.publishAll(List.of()))).isNull();
            assertThat(bus.getStats().getEventsPublished()).isZero();
        }

        @Test
        void publishAllDeliversEveryEvent() throws Exception {
            EventBus bus = syncBus();
            List<Event> received = new CopyOnWriteArrayList<>();
            bus.subscribeGlobal(received::add);
            List<Event> events = List.of(event("A"), event("B"), event("C"));

            await(bus.publishAll(events));

            assertThat(received).containsExactlyInAnyOrderElementsOf(events);
            assertThat(bus.getStats().getEventsPublished()).isEqualTo(3);
            assertThat(bus.getStats().getEventsProcessed()).isEqualTo(3);
        }
    }

    @Nested
    class Subscriptions {

        @Test
        void typeSubscriberOnlyReceivesItsEventType() {
            EventBus bus = syncBus();
            List<Event> received = new ArrayList<>();
            String id = bus.subscribe("A", received::add);
            TestEvent a = event("A");

            bus.publishSync(a);
            bus.publishSync(event("B"));

            assertThat(received).containsExactly(a);
            assertThat(bus.getHandlers("A")).extracting(EventHandler::getHandlerId).containsExactly(id);
            assertThat(bus.getHandlers("B")).isEmpty();
            assertThat(bus.getHandlers("A").get(0).getName()).isEqualTo("Handler-" + id.substring(0, 8));
        }

        @Test
        void customSubscriptionKeepsNameFlagsAndPriority() {
            EventBus bus = syncBus();
            String id = bus.subscribe("A", e -> { }, "audit", true, 7, e -> true);

            EventHandler handler = bus.getHandlers("A").get(0);
            assertThat(handler.getHandlerId()).isEqualTo(id);
            assertThat(handler.getName()).isEqualTo("audit");
            assertThat(handler.isAsync()).isTrue();
            assertThat(handler.getPriority()).isEqualTo(7);
            assertThat(handler.getFilter()).isNotNull();
            assertThat(handler.getHandler()).isNotNull();
            assertThat(handler.getRegisteredAt()).isNotNull();
        }

        @Test
        void classSubscriberReceivesInstancesAndSubclassesOnly() {
            EventBus bus = syncBus();
            List<TestEvent.Special> specials = new ArrayList<>();
            List<TestEvent> all = new ArrayList<>();
            bus.subscribe(TestEvent.Special.class, specials::add);
            bus.subscribe(TestEvent.class, all::add);
            TestEvent plain = event("A");
            TestEvent.Special special = new TestEvent.Special("B", "s");

            bus.publishSync(plain);
            bus.publishSync(special);

            assertThat(specials).containsExactly(special);
            assertThat(all).containsExactly(plain, special);
            assertThat(bus.getHandlers("A")).isEmpty(); // class handlers are not keyed by type name
        }

        @Test
        void classSubscriberAppliesTypedFilter() {
            EventBus bus = syncBus();
            List<TestEvent> received = new ArrayList<>();
            bus.subscribe(TestEvent.class, received::add, "typed", false, 0, e -> e.getLabel().startsWith("keep"));
            TestEvent kept = new TestEvent("A", "keep-me");

            bus.publishSync(kept);
            bus.publishSync(new TestEvent("A", "drop-me"));

            assertThat(received).containsExactly(kept);
            assertThat(bus.getAllHandlers()).extracting(EventHandler::getName).containsExactly("typed");
        }

        @Test
        void globalSubscriberReceivesEveryEvent() {
            EventBus bus = syncBus();
            List<String> types = new ArrayList<>();
            bus.subscribeGlobal(e -> types.add(e.getEventType()));

            bus.publishSync(event("A"));
            bus.publishSync(event("B"));

            assertThat(types).containsExactly("A", "B");
            assertThat(bus.getAllHandlers()).singleElement()
                    .satisfies(h -> assertThat(h.getName()).startsWith("GlobalHandler-"));
        }

        @Test
        void getAllHandlersListsTypeClassAndGlobalHandlers() {
            EventBus bus = syncBus();
            String a = bus.subscribe("A", e -> { });
            String b = bus.subscribe(TestEvent.class, e -> { });
            String c = bus.subscribeGlobal(e -> { });

            assertThat(bus.getAllHandlers()).extracting(EventHandler::getHandlerId).containsExactly(a, b, c);
            assertThat(bus.getStats().getRegisteredHandlers()).isEqualTo(3);
            assertThat(bus.getStats().getHandlerStats()).containsOnlyKeys(a, b, c);
        }

        @Test
        void unsubscribedHandlersStopReceivingEvents() {
            EventBus bus = syncBus();
            List<String> calls = new ArrayList<>();
            String a = bus.subscribe("A", e -> calls.add("type"));
            String b = bus.subscribe(TestEvent.class, e -> calls.add("class"));
            String c = bus.subscribeGlobal(e -> calls.add("global"));

            assertThat(bus.unsubscribe(a)).isTrue();
            assertThat(bus.unsubscribe(b)).isTrue();
            assertThat(bus.unsubscribe(c)).isTrue();
            bus.publishSync(event("A"));

            assertThat(calls).isEmpty();
            assertThat(bus.getAllHandlers()).isEmpty();
            assertThat(bus.getStats().getHandlerStats()).isEmpty();
        }

        @Test
        void unsubscribeNullReturnsFalse() {
            assertThat(syncBus().unsubscribe(null)).isFalse();
        }

        // Regression: unsubscribe used to return true even when no handler had that id.
        @Test
        void unsubscribeReturnsFalseForUnknownOrAlreadyRemovedHandler() {
            EventBus bus = syncBus();
            String id = bus.subscribe("A", e -> { });

            assertThat(bus.unsubscribe("no-such-handler")).isFalse();
            assertThat(bus.unsubscribe(id)).isTrue();
            assertThat(bus.unsubscribe(id)).isFalse();
        }
    }

    @Nested
    class Ordering {

        @Test
        void handlersRunInDescendingPriority() {
            EventBus bus = syncBus();
            List<String> order = new ArrayList<>();
            bus.subscribe("A", e -> order.add("low"), "low", false, 1, null);
            bus.subscribeGlobal(e -> order.add("high"), "high", false, 10, null);
            bus.subscribe(TestEvent.class, e -> order.add("mid"), "mid", false, 5, null);
            bus.subscribe("A", e -> order.add("negative"), "negative", false, -3, null);

            bus.publishSync(event("A"));

            assertThat(order).containsExactly("high", "mid", "low", "negative");
        }

        @Test
        void equalPrioritiesKeepTypeThenClassThenGlobalRegistrationOrder() {
            EventBus bus = syncBus();
            List<String> order = new ArrayList<>();
            bus.subscribeGlobal(e -> order.add("global"));
            bus.subscribe(TestEvent.class, e -> order.add("class"));
            bus.subscribe("A", e -> order.add("type-1"));
            bus.subscribe("A", e -> order.add("type-2"));

            bus.publishSync(event("A"));

            assertThat(order).containsExactly("type-1", "type-2", "class", "global");
        }
    }

    @Nested
    class Filters {

        @Test
        void handlerFilterSkipsNonMatchingEvents() {
            EventBus bus = syncBus();
            List<Event> received = new ArrayList<>();
            bus.subscribeGlobal(received::add, "only-high", false, 0,
                    e -> e.getPriority() == Event.Priority.HIGH);
            TestEvent high = new TestEvent("A", Event.Priority.HIGH, "h");

            bus.publishSync(new TestEvent("A", Event.Priority.LOW, "l"));
            bus.publishSync(high);

            assertThat(received).containsExactly(high);
        }

        @Test
        void globalFilterBlocksEventsForAllHandlers() {
            EventBus bus = syncBus();
            List<Event> received = new ArrayList<>();
            bus.subscribeGlobal(received::add);
            bus.subscribe("B", received::add);
            bus.addFilter("noB", e -> !e.getEventType().equals("B"), "drop B events");
            TestEvent a = event("A");

            bus.publishSync(a);
            bus.publishSync(event("B"));

            assertThat(received).containsExactly(a);
            assertThat(bus.getStats().getEventsPublished()).isEqualTo(2);
            assertThat(bus.getStats().getEventsProcessed()).isEqualTo(1);
        }

        @Test
        void filtersCanBeListedAndRemoved() {
            EventBus bus = syncBus();
            List<Event> received = new ArrayList<>();
            bus.subscribeGlobal(received::add);
            bus.addFilter("none", e -> false, "drop everything");

            EventFilter filter = bus.getFilters().get(0);
            assertThat(filter.getFilterId()).isEqualTo("none");
            assertThat(filter.getDescription()).isEqualTo("drop everything");
            assertThat(filter.getPredicate().test(event("A"))).isFalse();

            bus.publishSync(event("A"));
            assertThat(received).isEmpty();

            assertThat(bus.removeFilter("none")).isTrue();
            assertThat(bus.removeFilter("none")).isFalse();
            assertThat(bus.getFilters()).isEmpty();
            bus.publishSync(event("A"));
            assertThat(received).hasSize(1);
        }
    }

    @Nested
    class AsyncDelivery {

        @Test
        void publishFutureCompletesOnlyAfterAsyncHandlersFinish() throws Exception {
            EventBus bus = bus(true, 4, 0, 5);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger done = new AtomicInteger();
            bus.subscribe("A", e -> {
                try {
                    release.await(TIMEOUT_S, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                done.incrementAndGet();
            });
            bus.subscribe("A", e -> done.incrementAndGet());

            CompletableFuture<Void> future = bus.publish(event("A"));
            assertThat(future).isNotDone();
            release.countDown();
            await(future);

            assertThat(done).hasValue(2);
            assertThat(bus.getStats().getEventsProcessed()).isEqualTo(1);
        }

        @Test
        void asyncHandlersRunOnBusThreads() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            List<String> threads = new CopyOnWriteArrayList<>();
            bus.subscribe("A", e -> threads.add(Thread.currentThread().getName()));

            await(bus.publish(event("A")));

            assertThat(threads).containsExactly("EventBus-test-Thread");
        }

        @Test
        void asyncHandlersRunConcurrently() throws Exception {
            EventBus bus = bus(true, 4, 0, 5);
            CountDownLatch bothStarted = new CountDownLatch(2);
            Consumer<Event> rendezvous = e -> {
                bothStarted.countDown();
                try {
                    // Each handler waits for the other: only completes if they run in parallel
                    if (!bothStarted.await(TIMEOUT_S, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("handlers did not overlap");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            };
            String h1 = bus.subscribe("A", rendezvous);
            String h2 = bus.subscribe("A", rendezvous);

            await(bus.publish(event("A")));

            assertThat(bus.getStats().getEventsFailed()).isZero();
            assertThat(bus.getStats().getHandlerStats()).containsEntry(h1, 1L).containsEntry(h2, 1L);
        }

        // Regression: processEvent ran on a pool thread and blocked (join) on async handlers submitted to
        // the same pool, so with a saturated pool (here: one thread) publish never completed.
        @Test
        void asyncHandlersDoNotDeadlockASingleThreadPool() throws Exception {
            EventBus bus = bus(true, 1, 0, 5);
            CountDownLatch handled = new CountDownLatch(2);
            bus.subscribe("A", e -> handled.countDown());
            bus.subscribe("A", e -> handled.countDown());

            await(bus.publish(event("A")));

            awaitLatch(handled);
            assertThat(bus.getStats().getEventsProcessed()).isEqualTo(1);
        }

        @Test
        void publishSyncWaitsForAsyncHandlers() {
            EventBus bus = bus(true, 2, 0, 5);
            AtomicInteger calls = new AtomicInteger();
            bus.subscribe("A", e -> calls.incrementAndGet());

            bus.publishSync(event("A"));

            assertThat(calls).hasValue(1);
            assertThat(bus.getStats().getEventsProcessed()).isEqualTo(1);
        }

        @Test
        void manyConcurrentPublishesAreAllDelivered() throws Exception {
            EventBus bus = bus(true, 3, 0, 5);
            AtomicInteger calls = new AtomicInteger();
            bus.subscribe("A", e -> calls.incrementAndGet());
            bus.subscribeGlobal(e -> calls.incrementAndGet());

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(bus.publish(event("A")));
            }
            await(CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])));

            assertThat(calls).hasValue(100);
            assertThat(bus.getStats().getEventsPublished()).isEqualTo(50);
            assertThat(bus.getStats().getEventsProcessed()).isEqualTo(50);
            assertThat(bus.getStats().getEventTypeStats()).containsEntry("A", 50L);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void failingHandlerDoesNotPreventOtherHandlers() throws Exception {
            EventBus bus = syncBus();
            List<String> calls = new CopyOnWriteArrayList<>();
            bus.subscribe("A", e -> calls.add("first"), "first", false, 3, null);
            String bad = bus.subscribe("A", failing(), "bad", false, 2, null);
            bus.subscribe("A", e -> calls.add("last"), "last", false, 1, null);

            await(bus.publish(event("A")));

            assertThat(calls).containsExactly("first", "last");
            EventHandler badHandler = bus.getAllHandlers().stream()
                    .filter(h -> h.getHandlerId().equals(bad)).findFirst().orElseThrow();
            assertThat(badHandler.getFailedCount()).isEqualTo(1);
            assertThat(badHandler.getProcessedCount()).isZero();
            assertThat(bus.getStats().getEventsFailed()).isEqualTo(1);
            assertThat(bus.getStats().getHandlerStats()).containsEntry(bad, 0L);
        }

        @Test
        void failingAsyncHandlerIsIsolatedToo() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            AtomicInteger ok = new AtomicInteger();
            bus.subscribe("A", failing());
            bus.subscribe("A", e -> ok.incrementAndGet());

            await(bus.publish(event("A")));

            assertThat(ok).hasValue(1);
            assertThat(bus.getStats().getEventsFailed()).isEqualTo(1);
        }

        @Test
        void publishSyncDoesNotPropagateHandlerFailures() {
            EventBus bus = syncBus();
            bus.subscribe("A", failing());

            bus.publishSync(event("A"));

            assertThat(bus.getStats().getEventsFailed()).isEqualTo(1);
        }

        @Test
        void withoutRetriesFailedHandlerGoesStraightToDeadLetterQueue() {
            EventBus bus = syncBus();
            bus.subscribe("A", failing());
            TestEvent event = event("A");

            bus.publishSync(event);

            assertThat(bus.getDeadLetterEvents()).singleElement().satisfies(dl -> {
                assertThat(dl.getOriginalEvent()).isSameAs(event);
                assertThat(dl.getReason()).isEqualTo("Handler execution failed after 0 retries");
                assertThat(dl.getCause()).hasMessage("boom");
                assertThat(dl.getAttemptCount()).isEqualTo(1);
                assertThat(dl.getDeadLetterTime()).isNotNull();
            });
        }

        @Test
        void retriedHandlerThatRecoversIsCountedAsProcessed() throws Exception {
            EventBus bus = bus(false, 2, 2, 5);
            AtomicInteger attempts = new AtomicInteger();
            CountDownLatch succeeded = new CountDownLatch(1);
            String id = bus.subscribe("A", e -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("transient");
                }
                succeeded.countDown();
            });

            bus.publishSync(event("A"));
            awaitLatch(succeeded);
            bus.stop(); // drains the executor so the retry's bookkeeping is finished

            EventHandler handler = bus.getAllHandlers().get(0);
            assertThat(attempts).hasValue(2);
            assertThat(handler.getFailedCount()).isEqualTo(1);
            assertThat(handler.getProcessedCount()).isEqualTo(1);
            assertThat(bus.getDeadLetterEvents()).isEmpty();
            // Regression: a successful retry was not reflected in the bus's per-handler stats
            assertThat(bus.getStats().getHandlerStats()).containsEntry(id, 1L);
        }

        @Test
        void exhaustedRetriesEndInDeadLetterQueue() throws Exception {
            EventBus bus = bus(false, 2, 2, 5);
            CountDownLatch attempts = new CountDownLatch(3);
            bus.subscribe("A", e -> {
                attempts.countDown();
                throw new IllegalStateException("permanent");
            });

            bus.publishSync(event("A"));
            awaitLatch(attempts);
            bus.stop(); // waits for the final (dead-lettering) attempt to finish

            assertThat(bus.getDeadLetterEvents()).singleElement().satisfies(dl -> {
                assertThat(dl.getReason()).isEqualTo("Handler execution failed after 2 retries");
                assertThat(dl.getAttemptCount()).isEqualTo(3);
                assertThat(dl.getCause()).hasMessage("permanent");
            });
        }

        @Test
        void processingFailureCompletesFutureAndDeadLettersEvent() throws Exception {
            EventBus bus = syncBus();
            bus.addFilter("broken", e -> { throw new IllegalStateException("filter exploded"); }, "broken");
            TestEvent event = event("A");

            assertThat(await(bus.publish(event))).isNull();

            assertThat(bus.getStats().getEventsFailed()).isEqualTo(1);
            assertThat(bus.getDeadLetterEvents()).singleElement().satisfies(dl -> {
                assertThat(dl.getOriginalEvent()).isSameAs(event);
                assertThat(dl.getReason()).isEqualTo("Event processing failed");
                assertThat(dl.getAttemptCount()).isZero();
                assertThat(dl.getCause()).hasRootCauseMessage("filter exploded");
            });
        }

        @Test
        void publishSyncWrapsProcessingFailure() {
            EventBus bus = syncBus();
            bus.addFilter("broken", e -> { throw new IllegalStateException("filter exploded"); }, "broken");

            assertThatThrownBy(() -> bus.publishSync(event("A")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to process event synchronously")
                    .hasRootCauseMessage("filter exploded");
            assertThat(bus.getDeadLetterEvents()).hasSize(1);
        }
    }

    @Nested
    class DeadLetterQueue {

        private EventBus brokenBus(int capacity) {
            EventBus bus = bus(false, 2, 0, capacity);
            bus.addFilter("broken", e -> { throw new IllegalStateException("x"); }, "broken");
            return bus;
        }

        private void publishIgnoringFailure(EventBus bus, Event event) {
            assertThatThrownBy(() -> bus.publishSync(event)).isInstanceOf(RuntimeException.class);
        }

        @Test
        void queueEvictsOldestEntriesBeyondCapacity() {
            EventBus bus = brokenBus(2);
            TestEvent first = event("A");
            TestEvent second = event("B");
            TestEvent third = event("C");

            publishIgnoringFailure(bus, first);
            publishIgnoringFailure(bus, second);
            publishIgnoringFailure(bus, third);

            assertThat(bus.getDeadLetterEvents()).extracting(DeadLetterEvent::getOriginalEvent)
                    .containsExactly(second, third);
            assertThat(bus.getStats().getDeadLetterCount()).isEqualTo(2);
        }

        @Test
        void clearEmptiesTheQueue() {
            EventBus bus = brokenBus(5);
            publishIgnoringFailure(bus, event("A"));

            bus.clearDeadLetterQueue();

            assertThat(bus.getDeadLetterEvents()).isEmpty();
        }

        @Test
        void reprocessingRepublishesTheOriginalEvent() throws Exception {
            EventBus bus = brokenBus(5);
            List<Event> received = new CopyOnWriteArrayList<>();
            bus.subscribeGlobal(received::add);
            TestEvent event = event("A");
            publishIgnoringFailure(bus, event);
            DeadLetterEvent deadLetter = bus.getDeadLetterEvents().get(0);

            bus.removeFilter("broken");
            await(bus.reprocessDeadLetterEvent(deadLetter));

            assertThat(received).containsExactly(event);
        }

        @Test
        void returnedListIsASnapshot() {
            EventBus bus = brokenBus(5);
            publishIgnoringFailure(bus, event("A"));

            bus.getDeadLetterEvents().clear();

            assertThat(bus.getDeadLetterEvents()).hasSize(1);
        }
    }

    @Nested
    class Statistics {

        @Test
        void statsTrackPublishedProcessedFailedAndPerType() {
            EventBus bus = syncBus();
            String good = bus.subscribe("A", e -> { });
            bus.subscribe("B", failing());

            bus.publishSync(event("A"));
            bus.publishSync(event("A"));
            bus.publishSync(event("B"));
            bus.publishSync(event("C"));

            EventBusStats stats = bus.getStats();
            assertThat(stats.getEventsPublished()).isEqualTo(4);
            assertThat(stats.getEventsProcessed()).isEqualTo(4);
            assertThat(stats.getEventsFailed()).isEqualTo(1);
            assertThat(stats.getEventTypeStats()).containsExactlyInAnyOrderEntriesOf(
                    java.util.Map.of("A", 2L, "B", 1L, "C", 1L));
            assertThat(stats.getHandlerStats()).containsEntry(good, 2L);
            assertThat(stats.getFailureRate()).isCloseTo(25.0, within(1e-9));
            assertThat(stats.getSuccessRate()).isCloseTo(75.0, within(1e-9));
            assertThat(bus.toString()).contains("handlers=2", "published=4", "processed=4", "failed=1");
        }

        @Test
        void statsSnapshotIsIndependentOfLaterActivity() {
            EventBus bus = syncBus();
            bus.publishSync(event("A"));
            EventBusStats snapshot = bus.getStats();

            bus.publishSync(event("A"));
            snapshot.getEventTypeStats().put("A", 99L);
            snapshot.getHandlerStats().put("x", 1L);

            assertThat(snapshot.getEventsPublished()).isEqualTo(1);
            assertThat(snapshot.getEventTypeStats()).containsExactlyEntriesOf(java.util.Map.of("A", 1L));
            assertThat(snapshot.getHandlerStats()).isEmpty();
        }

        @Test
        void resetClearsCountersAndDeadLettersButKeepsHandlers() {
            EventBus bus = syncBus();
            String id = bus.subscribe("A", e -> { });
            bus.subscribe("A", failing());
            bus.publishSync(event("A"));

            bus.resetStats();

            EventBusStats stats = bus.getStats();
            assertThat(stats.getEventsPublished()).isZero();
            assertThat(stats.getEventsProcessed()).isZero();
            assertThat(stats.getEventsFailed()).isZero();
            assertThat(stats.getEventTypeStats()).isEmpty();
            assertThat(stats.getHandlerStats()).containsEntry(id, 0L);
            assertThat(stats.getDeadLetterCount()).isZero();
            assertThat(stats.getRegisteredHandlers()).isEqualTo(2);
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void stopIsIdempotentAndStartIsNoOpWhenRunning() {
            EventBus bus = syncBus();
            bus.start();
            assertThat(bus.isRunning()).isTrue();

            bus.stop();
            bus.stop();

            assertThat(bus.isRunning()).isFalse();
            assertThat(bus.getStats().isRunning()).isFalse();
        }

        // Regression: stop() shut the executor down for good, so after start() the bus claimed to be
        // running but publish() threw RejectedExecutionException.
        @Test
        void restartedBusCanPublishAgain() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            List<Event> received = new CopyOnWriteArrayList<>();
            bus.subscribe("A", received::add);
            bus.stop();

            bus.start();
            TestEvent event = event("A");
            await(bus.publish(event));

            assertThat(bus.isRunning()).isTrue();
            assertThat(received).containsExactly(event);
        }

        @Test
        void stopWaitsForInFlightEvents() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            CountDownLatch started = new CountDownLatch(1);
            AtomicInteger finished = new AtomicInteger();
            bus.subscribe("A", e -> {
                started.countDown();
                finished.incrementAndGet();
            });

            CompletableFuture<Void> future = bus.publish(event("A"));
            awaitLatch(started);
            bus.stop();

            assertThat(finished).hasValue(1);
            await(future);
        }
    }

    @Nested
    class WaitForCompletion {

        @Test
        void idleBusIsImmediatelyComplete() {
            assertThat(syncBus().waitForCompletion(1_000)).isTrue();
        }

        @Test
        void returnsFalseWhileAnEventIsStillInFlight() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            CountDownLatch release = new CountDownLatch(1);
            bus.subscribe("A", e -> {
                try {
                    release.await(TIMEOUT_S, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });

            CompletableFuture<Void> future = bus.publish(event("A"));
            assertThat(bus.waitForCompletion(150)).isFalse();

            release.countDown();
            await(future);
            assertThat(bus.waitForCompletion(1_000)).isTrue();
        }

        // Regression: completion was computed as published == processed + failed, but a failing handler
        // increments both processed (event) and failed (handler), so this never became true again.
        @Test
        void completesAfterAHandlerFailure() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            bus.subscribe("A", failing());

            await(bus.publish(event("A")));

            assertThat(bus.waitForCompletion(1_000)).isTrue();
        }

        // Regression: an event rejected by a global filter was neither processed nor failed, so the
        // bus never reported completion.
        @Test
        void completesAfterAFilteredEvent() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            bus.addFilter("none", e -> false, "drop everything");

            await(bus.publish(event("A")));

            assertThat(bus.waitForCompletion(1_000)).isTrue();
        }

        @Test
        void interruptedWaitReturnsFalseAndKeepsInterruptFlag() throws Exception {
            EventBus bus = bus(true, 2, 0, 5);
            CountDownLatch release = new CountDownLatch(1);
            bus.subscribe("A", e -> {
                try {
                    release.await(TIMEOUT_S, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            CompletableFuture<Void> future = bus.publish(event("A"));

            Thread.currentThread().interrupt();
            try {
                assertThat(bus.waitForCompletion(5_000)).isFalse();
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } finally {
                Thread.interrupted();
                release.countDown();
            }
            await(future);
        }
    }

    @Nested
    class ValueTypes {

        @Test
        void handlersAreEqualByIdOnly() {
            EventHandler a = new EventHandler("id-1", "a", e -> { }, null, false, 0);
            EventHandler sameId = new EventHandler("id-1", "b", e -> { }, e -> true, true, 9);
            EventHandler other = new EventHandler("id-2", "a", e -> { }, null, false, 0);

            assertThat(a).isEqualTo(a).isEqualTo(sameId).hasSameHashCodeAs(sameId).isNotEqualTo(other);
            assertThat(a.equals(null)).isFalse();
            assertThat(a).isNotEqualTo("id-1");
            assertThat(a.getProcessedCount()).isZero();
            assertThat(a.getFailedCount()).isZero();
        }

        @Test
        void statsRatesAreZeroWithoutPublishedEvents() {
            EventBusStats stats = new EventBusStats("b", 0, 0, 0, 0, 0,
                    Collections.emptyMap(), Collections.emptyMap(), false);
            assertThat(stats.getSuccessRate()).isZero();
            assertThat(stats.getFailureRate()).isZero();
            assertThat(stats.isRunning()).isFalse();
        }
    }
}
