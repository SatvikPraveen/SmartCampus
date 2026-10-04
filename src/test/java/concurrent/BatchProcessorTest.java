package concurrent;

import concurrent.BatchProcessor.BatchResult;
import concurrent.BatchProcessor.ProgressInfo;
import models.Course;
import models.Enrollment;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@Timeout(30)
class BatchProcessorTest {

    private final List<BatchProcessor> started = new ArrayList<>();

    @AfterEach
    void tearDown() {
        started.forEach(BatchProcessor::shutdown);
    }

    private BatchProcessor processor(int threads, int batchSize, int maxConcurrentBatches) {
        BatchProcessor p = new BatchProcessor(threads, batchSize, maxConcurrentBatches);
        started.add(p);
        return p;
    }

    private static List<Integer> numbers(int n) {
        return IntStream.rangeClosed(1, n).boxed().collect(Collectors.toList());
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    /** Squares, but rejects multiples of seven. */
    private static final Function<Integer, Integer> SQUARE_UNLESS_SEVENS = i -> {
        if (i % 7 == 0) {
            throw new IllegalStateException("bad " + i);
        }
        return i * i;
    };

    @ParameterizedTest
    @CsvSource({"0, 1", "-1, 1", "1, 0", "1, -2"})
    void rejectsNonPositiveBatchSizeOrConcurrency(int batchSize, int maxConcurrentBatches) {
        assertThatThrownBy(() -> new BatchProcessor(2, batchSize, maxConcurrentBatches))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    class ProcessBatch {

        @Test
        void matchesSequentialMapInOriginalOrder() throws Exception {
            List<Integer> items = numbers(103);

            BatchResult<Integer> result = await(processor(4, 10, 3).processBatch(items, i -> i * i, "Squares"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Squares completed successfully");
            assertThat(result.getProcessedItems())
                    .containsExactlyElementsOf(items.stream().map(i -> i * i).toList());
            assertThat(result.getProcessedCount()).isEqualTo(103);
            assertThat(result.getTotalCount()).isEqualTo(103);
            assertThat(result.getErrors()).isEmpty();
            assertThat(result.getSuccessRate()).isEqualTo(100.0);
            assertThat(result.getProcessingTime()).isNotNegative();
        }

        @Test
        void collectsPerItemErrorsWithoutFailingTheBatch() throws Exception {
            List<Integer> items = numbers(50);

            BatchResult<Integer> result = await(processor(3, 8, 2).processBatch(items, SQUARE_UNLESS_SEVENS, "Op"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProcessedItems()).containsExactlyElementsOf(
                    items.stream().filter(i -> i % 7 != 0).map(i -> i * i).toList());
            assertThat(result.getProcessedCount()).isEqualTo(43);
            assertThat(result.getErrors()).hasSize(7)
                    .allSatisfy(e -> assertThat(e).startsWith("Error processing item: bad "));
            assertThat(result.getSuccessRate()).isCloseTo(86.0, within(1e-9));
        }

        @Test
        void emptyInputSucceedsWithNothingProcessed() throws Exception {
            BatchResult<Integer> result = await(processor(2, 5, 1).processBatch(List.of(), i -> i, "Empty"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProcessedItems()).isEmpty();
            assertThat(result.getTotalCount()).isZero();
            assertThat(result.getSuccessRate()).isZero();
        }

        @Test
        void neverRunsMoreBatchesConcurrentlyThanAllowed() throws Exception {
            AtomicInteger inFlight = new AtomicInteger();
            AtomicInteger maxInFlight = new AtomicInteger();
            // two batches must be able to meet at the barrier, proving the limit is actually reached
            CyclicBarrier pair = new CyclicBarrier(2);
            Function<Integer, Integer> tracking = i -> {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                try {
                    pair.await(5, TimeUnit.SECONDS);
                    return i;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                } finally {
                    inFlight.decrementAndGet();
                }
            };

            BatchResult<Integer> result = await(processor(6, 1, 2).processBatch(numbers(12), tracking, "Limited"));

            assertThat(result.getErrors()).isEmpty();
            assertThat(result.getProcessedItems()).containsExactlyElementsOf(numbers(12));
            assertThat(maxInFlight.get()).isEqualTo(2);
        }

        // Regression: the coordinating task ran on the worker pool and blocked joining chunk
        // tasks queued on that same pool, so a single-threaded processor deadlocked forever.
        @Test
        void singleWorkerThreadDoesNotDeadlock() throws Exception {
            BatchProcessor single = processor(1, 2, 1);

            BatchResult<Integer> result = await(single.processBatch(numbers(9), i -> -i, "Single"));
            BatchResult<Integer> consumed = await(single.processBatchWithConsumer(numbers(9), i -> { }, "Single"));
            BatchResult<Integer> withProgress = await(single.processBatchWithProgress(
                    numbers(9), i -> -i, p -> { }, "Single"));

            assertThat(result.getProcessedItems()).containsExactlyElementsOf(
                    numbers(9).stream().map(i -> -i).toList());
            assertThat(consumed.getProcessedCount()).isEqualTo(9);
            assertThat(withProgress.getProcessedCount()).isEqualTo(9);
        }

        // Regression: concurrent batch jobs could occupy every worker thread with coordinators.
        @Test
        void concurrentJobsSaturatingThePoolAllComplete() throws Exception {
            BatchProcessor two = processor(2, 3, 2);

            List<CompletableFuture<BatchResult<Integer>>> jobs = IntStream.range(0, 6)
                    .mapToObj(j -> two.processBatch(numbers(20), i -> i + j, "Job" + j))
                    .toList();

            for (int j = 0; j < jobs.size(); j++) {
                int offset = j;
                assertThat(await(jobs.get(j)).getProcessedItems())
                        .containsExactlyElementsOf(numbers(20).stream().map(i -> i + offset).toList());
            }
        }

        @Test
        void typedConvenienceMethodsDelegateWithOperationName() throws Exception {
            BatchProcessor p = processor(2, 2, 2);
            Student student = new Student("U1", "Ann", "Lee", "ann@u.edu", null, "S1", "CS",
                    Student.AcademicYear.SENIOR);
            Course course = new Course("C1", "CS101", "Intro", "", 3, "D-CS");
            Enrollment enrollment = new Enrollment("E1", "S1", "C1", "Fall", 2024);

            BatchResult<Student> students = await(p.processStudentsBatch(List.of(student), s -> s));
            BatchResult<Course> courses = await(p.processCoursesBatch(List.of(course), c -> c));
            BatchResult<Enrollment> enrollments = await(p.processEnrollmentsBatch(List.of(enrollment), e -> e));

            assertThat(students.getMessage()).isEqualTo("Student Processing completed successfully");
            assertThat(students.getProcessedItems()).containsExactly(student);
            assertThat(courses.getMessage()).isEqualTo("Course Processing completed successfully");
            assertThat(courses.getProcessedItems()).containsExactly(course);
            assertThat(enrollments.getMessage()).isEqualTo("Enrollment Processing completed successfully");
            assertThat(enrollments.getProcessedItems()).containsExactly(enrollment);
        }
    }

    @Nested
    class ParallelBatch {

        @Test
        void matchesSequentialMapInOrder() throws Exception {
            List<Integer> items = numbers(500);

            BatchResult<Integer> result = await(processor(4, 10, 2).processParallelBatch(items, i -> i * 3, "Par"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Par completed successfully");
            assertThat(result.getProcessedItems()).containsExactlyElementsOf(items.stream().map(i -> i * 3).toList());
            assertThat(result.getProcessedCount()).isEqualTo(500);
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        void anyFailingItemFailsTheWholeBatch() throws Exception {
            BatchResult<Integer> result = await(processor(4, 10, 2)
                    .processParallelBatch(List.of(7), SQUARE_UNLESS_SEVENS, "Par"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).startsWith("Par failed: ").contains("bad 7");
            assertThat(result.getProcessedItems()).isEmpty();
            assertThat(result.getProcessedCount()).isZero();
            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getErrors()).singleElement().asString().contains("bad 7");
        }
    }

    @Nested
    class ConsumerBatch {

        @Test
        void consumesEveryItemExactlyOnceAndCountsFailures() throws Exception {
            Set<Integer> seen = Collections.synchronizedSet(new HashSet<>());
            AtomicInteger calls = new AtomicInteger();

            BatchResult<Integer> result = await(processor(4, 6, 2).processBatchWithConsumer(numbers(70), i -> {
                calls.incrementAndGet();
                SQUARE_UNLESS_SEVENS.apply(i);
                seen.add(i);
            }, "Consume"));

            assertThat(calls.get()).isEqualTo(70);
            assertThat(seen).hasSize(60).doesNotContain(7, 14, 70);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Consume completed successfully");
            assertThat(result.getProcessedItems()).isEmpty();
            assertThat(result.getProcessedCount()).isEqualTo(60);
            assertThat(result.getErrors()).hasSize(10);
        }
    }

    @Nested
    class ProgressBatch {

        @Test
        void reportsOneUpdatePerBatchPlusFinalCompletion() throws Exception {
            List<ProgressInfo> updates = Collections.synchronizedList(new ArrayList<>());

            BatchResult<Integer> result = await(processor(3, 4, 2).processBatchWithProgress(
                    numbers(10), SQUARE_UNLESS_SEVENS, updates::add, "Prog"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProcessedItems()).containsExactlyElementsOf(
                    numbers(10).stream().filter(i -> i != 7).map(i -> i * i).toList());
            assertThat(result.getErrors()).containsExactly("Error processing item: bad 7");

            // 3 batches (4, 4, 2 items) + the final 100% update, which is always last
            assertThat(updates).hasSize(4);
            assertThat(updates.subList(0, 3)).extracting(ProgressInfo::getPercentage)
                    .containsExactlyInAnyOrder(1.0 / 3 * 100, 2.0 / 3 * 100, 3.0 / 3 * 100);
            assertThat(updates.subList(0, 3)).extracting(ProgressInfo::getProcessedItems)
                    .containsExactlyInAnyOrder(4, 8, 10);
            ProgressInfo last = updates.get(3);
            assertThat(last.getPercentage()).isEqualTo(100.0);
            assertThat(last.getProcessedItems()).isEqualTo(10);
            assertThat(last.getTotalItems()).isEqualTo(10);
            assertThat(last).hasToString("Progress: 100.0% (10/10)");
        }

        @Test
        void failingProgressCallbackFailsTheBatch() throws Exception {
            BatchResult<Integer> result = await(processor(2, 5, 1).processBatchWithProgress(
                    numbers(5), i -> i, p -> { throw new IllegalStateException("listener down"); }, "Prog"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Prog failed");
            assertThat(result.getErrors()).singleElement().asString()
                    .startsWith("Batch processing failed").contains("listener down");
        }
    }

    @Test
    void scheduledProcessingRunsRepeatedlyUntilCancelled() throws Exception {
        Student student = new Student("U1", "Ann", "Lee", "ann@u.edu", null, "S1", "CS",
                Student.AcademicYear.SENIOR);
        CountDownLatch processedTwice = new CountDownLatch(2);

        ScheduledFuture<?> schedule = processor(2, 5, 1).scheduleStudentBatchProcessing(
                () -> List.of(student),
                s -> {
                    processedTwice.countDown();
                    return s;
                },
                0, 5, TimeUnit.MILLISECONDS);

        assertThat(processedTwice.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(schedule.cancel(false)).isTrue();
        assertThat(schedule.isCancelled()).isTrue();
    }

    @Test
    void shutdownRejectsNewWorkAndIsIdempotent() {
        BatchProcessor p = processor(2, 5, 1);
        p.shutdown();
        p.shutdown();

        assertThatThrownBy(() -> p.processBatch(numbers(3), i -> i, "Late"))
                .isInstanceOf(RejectedExecutionException.class);
        assertThatThrownBy(() -> p.processParallelBatch(numbers(3), i -> i, "Late"))
                .isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void interruptedPermitWaitDoesNotReleaseAPermitItNeverAcquired() throws Exception {
        BatchProcessor p = processor(2, 10, 1);
        CompletableFuture<Thread> waiter = new CompletableFuture<>();
        Semaphore noPermits = new Semaphore(0) {
            @Override
            public void acquire() throws InterruptedException {
                waiter.complete(Thread.currentThread());
                super.acquire();
            }
        };

        CompletableFuture<?> chunk = p.processBatchChunk(List.of(1, 2), Function.identity(), noPermits);
        Thread worker = waiter.get(10, TimeUnit.SECONDS);
        while (!noPermits.hasQueuedThreads()) {
            Thread.onSpinWait();
        }
        worker.interrupt();
        chunk.get(10, TimeUnit.SECONDS);

        assertThat(noPermits.availablePermits()).isZero();
    }
}
