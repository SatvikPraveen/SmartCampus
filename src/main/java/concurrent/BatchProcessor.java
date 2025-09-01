// File location: src/main/java/concurrent/BatchProcessor.java

package concurrent;

import models.*;
import repositories.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles batch processing operations using Executor framework
 * Provides efficient processing of large datasets with configurable parallelism
 */
public class BatchProcessor {
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final ForkJoinPool forkJoinPool;
    private final int batchSize;
    private final int maxConcurrentBatches;
    
    public BatchProcessor(int threadPoolSize, int batchSize, int maxConcurrentBatches) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.forkJoinPool = new ForkJoinPool(threadPoolSize);
        this.batchSize = batchSize;
        this.maxConcurrentBatches = maxConcurrentBatches;
    }
    
    /**
     * Process students in batches
     */
    public CompletableFuture<BatchResult<Student>> processStudentsBatch(
            List<Student> students, Function<Student, Student> processor) {
        
        return processBatch(students, processor, "Student Processing");
    }
    
    /**
     * Process courses in batches
     */
    public CompletableFuture<BatchResult<Course>> processCoursesBatch(
            List<Course> courses, Function<Course, Course> processor) {
        
        return processBatch(courses, processor, "Course Processing");
    }
    
    /**
     * Process enrollments in batches
     */
    public CompletableFuture<BatchResult<Enrollment>> processEnrollmentsBatch(
            List<Enrollment> enrollments, Function<Enrollment, Enrollment> processor) {
        
        return processBatch(enrollments, processor, "Enrollment Processing");
    }
    
    /**
     * Generic batch processing method
     */
    public <T> CompletableFuture<BatchResult<T>> processBatch(
            List<T> items, Function<T, T> processor, String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<T> processedItems = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            AtomicInteger processedCount = new AtomicInteger(0);
            
            try {
                // Split items into batches
                List<List<T>> batches = createBatches(items, batchSize);
                
                // Create semaphore to limit concurrent batches
                Semaphore semaphore = new Semaphore(maxConcurrentBatches);
                
                // Process batches concurrently
                List<CompletableFuture<BatchChunkResult<T>>> batchFutures = 
                    batches.stream()
                        .map(batch -> processBatchChunk(batch, processor, semaphore))
                        .collect(Collectors.toList());
                
                // Wait for all batches to complete
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
                
                // Collect results
                for (CompletableFuture<BatchChunkResult<T>> future : batchFutures) {
                    BatchChunkResult<T> result = future.join();
                    processedItems.addAll(result.getProcessedItems());
                    errors.addAll(result.getErrors());
                    processedCount.addAndGet(result.getProcessedCount());
                }
                
                long endTime = System.currentTimeMillis();
                
                return new BatchResult<>(
                    true,
                    operationName + " completed successfully",
                    processedItems,
                    processedCount.get(),
                    items.size(),
                    errors,
                    endTime - startTime
                );
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                errors.add("Batch processing failed: " + e.getMessage());
                
                return new BatchResult<>(
                    false,
                    operationName + " failed",
                    processedItems,
                    processedCount.get(),
                    items.size(),
                    errors,
                    endTime - startTime
                );
            }
        }, executorService);
    }
    
    /**
     * Process a single batch chunk
     */
    private <T> CompletableFuture<BatchChunkResult<T>> processBatchChunk(
            List<T> batch, Function<T, T> processor, Semaphore semaphore) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                semaphore.acquire();
                
                List<T> processedItems = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                int processedCount = 0;
                
                for (T item : batch) {
                    try {
                        T processedItem = processor.apply(item);
                        processedItems.add(processedItem);
                        processedCount++;
                    } catch (Exception e) {
                        errors.add("Error processing item: " + e.getMessage());
                    }
                }
                
                return new BatchChunkResult<>(processedItems, processedCount, errors);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new BatchChunkResult<>(
                    new ArrayList<>(), 0, 
                    Arrays.asList("Batch processing interrupted"));
            } finally {
                semaphore.release();
            }
        }, executorService);
    }
    
    /**
     * Parallel batch processing with custom thread pool
     */
    public <T> CompletableFuture<BatchResult<T>> processParallelBatch(
            List<T> items, Function<T, T> processor, String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                List<T> processedItems = items.parallelStream()
                    .map(processor)
                    .collect(Collectors.toList());
                
                long endTime = System.currentTimeMillis();
                
                return new BatchResult<>(
                    true,
                    operationName + " completed successfully",
                    processedItems,
                    processedItems.size(),
                    items.size(),
                    new ArrayList<>(),
                    endTime - startTime
                );
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                
                return new BatchResult<>(
                    false,
                    operationName + " failed: " + e.getMessage(),
                    new ArrayList<>(),
                    0,
                    items.size(),
                    Arrays.asList(e.getMessage()),
                    endTime - startTime
                );
            }
        }, forkJoinPool);
    }
    
    /**
     * Batch processing with consumer (no return value)
     */
    public <T> CompletableFuture<BatchResult<T>> processBatchWithConsumer(
            List<T> items, Consumer<T> processor, String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<String> errors = new ArrayList<>();
            AtomicInteger processedCount = new AtomicInteger(0);
            
            try {
                // Split items into batches
                List<List<T>> batches = createBatches(items, batchSize);
                
                // Process batches concurrently
                List<CompletableFuture<Void>> batchFutures = batches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> {
                        for (T item : batch) {
                            try {
                                processor.accept(item);
                                processedCount.incrementAndGet();
                            } catch (Exception e) {
                                synchronized (errors) {
                                    errors.add("Error processing item: " + e.getMessage());
                                }
                            }
                        }
                    }, executorService))
                    .collect(Collectors.toList());
                
                // Wait for all batches to complete
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
                
                long endTime = System.currentTimeMillis();
                
                return new BatchResult<>(
                    true,
                    operationName + " completed successfully",
                    new ArrayList<>(), // No return items for consumer
                    processedCount.get(),
                    items.size(),
                    errors,
                    endTime - startTime
                );
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                errors.add("Batch processing failed: " + e.getMessage());
                
                return new BatchResult<>(
                    false,
                    operationName + " failed",
                    new ArrayList<>(),
                    processedCount.get(),
                    items.size(),
                    errors,
                    endTime - startTime
                );
            }
        }, executorService);
    }
    
    /**
     * Scheduled batch processing
     */
    public ScheduledFuture<BatchResult<Student>> scheduleStudentBatchProcessing(
            Supplier<List<Student>> dataSupplier, 
            Function<Student, Student> processor,
            long delay, long period, TimeUnit unit) {
        
        return scheduledExecutor.scheduleAtFixedRate(() -> {
            List<Student> students = dataSupplier.get();
            processStudentsBatch(students, processor)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Scheduled batch processing failed: " + 
                                         throwable.getMessage());
                    } else {
                        System.out.println("Scheduled batch processing completed: " + 
                                         result.getMessage());
                    }
                });
        }, delay, period, unit);
    }
    
    /**
     * Batch processing with progress monitoring
     */
    public <T> CompletableFuture<BatchResult<T>> processBatchWithProgress(
            List<T> items, Function<T, T> processor, 
            Consumer<ProgressInfo> progressCallback, String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<T> processedItems = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            AtomicInteger processedCount = new AtomicInteger(0);
            
            try {
                List<List<T>> batches = createBatches(items, batchSize);
                int totalBatches = batches.size();
                AtomicInteger completedBatches = new AtomicInteger(0);
                
                List<CompletableFuture<BatchChunkResult<T>>> batchFutures = 
                    batches.stream()
                        .map(batch -> processBatchChunkWithProgress(
                            batch, processor, progressCallback, 
                            completedBatches, totalBatches, items.size()))
                        .collect(Collectors.toList());
                
                // Wait for all batches to complete
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
                
                // Collect results
                for (CompletableFuture<BatchChunkResult<T>> future : batchFutures) {
                    BatchChunkResult<T> result = future.join();
                    processedItems.addAll(result.getProcessedItems());
                    errors.addAll(result.getErrors());
                    processedCount.addAndGet(result.getProcessedCount());
                }
                
                long endTime = System.currentTimeMillis();
                
                // Final progress update
                progressCallback.accept(new ProgressInfo(100.0, items.size(), items.size()));
                
                return new BatchResult<>(
                    true,
                    operationName + " completed successfully",
                    processedItems,
                    processedCount.get(),
                    items.size(),
                    errors,
                    endTime - startTime
                );
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                errors.add("Batch processing failed: " + e.getMessage());
                
                return new BatchResult<>(
                    false,
                    operationName + " failed",
                    processedItems,
                    processedCount.get(),
                    items.size(),
                    errors,
                    endTime - startTime
                );
            }
        }, executorService);
    }
    
    /**
     * Process batch chunk with progress updates
     */
    private <T> CompletableFuture<BatchChunkResult<T>> processBatchChunkWithProgress(
            List<T> batch, Function<T, T> processor,
            Consumer<ProgressInfo> progressCallback,
            AtomicInteger completedBatches, int totalBatches, int totalItems) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<T> processedItems = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int processedCount = 0;
            
            for (T item : batch) {
                try {
                    T processedItem = processor.apply(item);
                    processedItems.add(processedItem);
                    processedCount++;
                } catch (Exception e) {
                    errors.add("Error processing item: " + e.getMessage());
                }
            }
            
            // Update progress
            int completed = completedBatches.incrementAndGet();
            double progressPercentage = (double) completed / totalBatches * 100;
            int processedItemsCount = completed * batchSize;
            if (processedItemsCount > totalItems) {
                processedItemsCount = totalItems;
            }
            
            progressCallback.accept(new ProgressInfo(
                progressPercentage, processedItemsCount, totalItems));
            
            return new BatchChunkResult<>(processedItems, processedCount, errors);
        }, executorService);
    }
    
    /**
     * Create batches from list of items
     */
    private <T> List<List<T>> createBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            batches.add(new ArrayList<>(items.subList(i, endIndex)));
        }
        
        return batches;
    }
    
    /**
     * Shutdown the batch processor
     */
    public void shutdown() {
        executorService.shutdown();
        scheduledExecutor.shutdown();
        forkJoinPool.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            if (!forkJoinPool.awaitTermination(30, TimeUnit.SECONDS)) {
                forkJoinPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            forkJoinPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes
    
    public static class BatchResult<T> {
        private final boolean success;
        private final String message;
        private final List<T> processedItems;
        private final int processedCount;
        private final int totalCount;
        private final List<String> errors;
        private final long processingTime;
        
        public BatchResult(boolean success, String message, List<T> processedItems,
                          int processedCount, int totalCount, List<String> errors,
                          long processingTime) {
            this.success = success;
            this.message = message;
            this.processedItems = processedItems;
            this.processedCount = processedCount;
            this.totalCount = totalCount;
            this.errors = errors;
            this.processingTime = processingTime;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<T> getProcessedItems() { return processedItems; }
        public int getProcessedCount() { return processedCount; }
        public int getTotalCount() { return totalCount; }
        public List<String> getErrors() { return errors; }
        public long getProcessingTime() { return processingTime; }
        
        public double getSuccessRate() {
            return totalCount > 0 ? (double) processedCount / totalCount * 100 : 0;
        }
    }
    
    private static class BatchChunkResult<T> {
        private final List<T> processedItems;
        private final int processedCount;
        private final List<String> errors;
        
        public BatchChunkResult(List<T> processedItems, int processedCount, 
                               List<String> errors) {
            this.processedItems = processedItems;
            this.processedCount = processedCount;
            this.errors = errors;
        }
        
        public List<T> getProcessedItems() { return processedItems; }
        public int getProcessedCount() { return processedCount; }
        public List<String> getErrors() { return errors; }
    }
    
    public static class ProgressInfo {
        private final double percentage;
        private final int processedItems;
        private final int totalItems;
        
        public ProgressInfo(double percentage, int processedItems, int totalItems) {
            this.percentage = percentage;
            this.processedItems = processedItems;
            this.totalItems = totalItems;
        }
        
        public double getPercentage() { return percentage; }
        public int getProcessedItems() { return processedItems; }
        public int getTotalItems() { return totalItems; }
        
        @Override
        public String toString() {
            return String.format("Progress: %.1f%% (%d/%d)", 
                               percentage, processedItems, totalItems);
        }
    }
}