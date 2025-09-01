// File location: src/main/java/annotations/Async.java
package annotations;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation to mark methods for asynchronous execution
 * Enables non-blocking processing and concurrent task execution
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {
    
    /**
     * Execution strategy for async operations
     */
    enum Strategy {
        FIRE_AND_FORGET,    // Execute and don't wait for result
        CALLBACK,           // Execute with callback notification
        FUTURE,             // Return Future for result access
        COMPLETABLE_FUTURE, // Return CompletableFuture
        REACTIVE,           // Use reactive streams
        CUSTOM              // Custom execution strategy
    }
    
    /**
     * Thread pool configuration
     */
    enum PoolType {
        FIXED,              // Fixed size thread pool
        CACHED,             // Cached thread pool
        SCHEDULED,          // Scheduled thread pool
        WORK_STEALING,      // Work-stealing pool
        VIRTUAL,            // Virtual threads (Java 19+)
        CUSTOM              // Custom thread pool
    }
    
    /**
     * Priority levels for async execution
     */
    enum Priority {
        LOW(1),
        NORMAL(5),
        HIGH(8),
        CRITICAL(10);
        
        private final int value;
        
        Priority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Execution strategy to use
     * @return execution strategy
     */
    Strategy strategy() default Strategy.FIRE_AND_FORGET;
    
    /**
     * Thread pool type
     * @return pool type
     */
    PoolType poolType() default PoolType.CACHED;
    
    /**
     * Name of the executor to use
     * @return executor name, empty means default
     */
    String executor() default "";
    
    /**
     * Priority of the async execution
     * @return execution priority
     */
    Priority priority() default Priority.NORMAL;
    
    /**
     * Timeout for async operation
     * @return timeout value, 0 means no timeout
     */
    long timeout() default 0;
    
    /**
     * Time unit for timeout
     * @return time unit
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * Maximum number of retry attempts
     * @return max retries, 0 means no retry
     */
    int maxRetries() default 0;
    
    /**
     * Delay between retry attempts in milliseconds
     * @return retry delay
     */
    long retryDelay() default 1000;
    
    /**
     * Whether to use exponential backoff for retries
     * @return true for exponential backoff
     */
    boolean exponentialBackoff() default false;
    
    /**
     * Condition for async execution
     * @return SpEL expression that must be true for async execution
     */
    String condition() default "";
    
    /**
     * Callback method name for completion notification
     * @return callback method name
     */
    String callback() default "";
    
    /**
     * Error handler method name for exception handling
     * @return error handler method name
     */
    String errorHandler() default "";
    
    /**
     * Whether to preserve thread context (security, transaction, etc.)
     * @return true to preserve context
     */
    boolean preserveContext() default false;
    
    /**
     * Queue capacity for async operations
     * @return queue capacity, 0 means unbounded
     */
    int queueCapacity() default 0;
    
    /**
     * Core pool size for thread pool
     * @return core pool size
     */
    int corePoolSize() default 1;
    
    /**
     * Maximum pool size for thread pool
     * @return max pool size
     */
    int maxPoolSize() default Integer.MAX_VALUE;
    
    /**
     * Keep alive time for idle threads
     * @return keep alive time
     */
    long keepAliveTime() default 60;
    
    /**
     * Time unit for keep alive time
     * @return time unit
     */
    TimeUnit keepAliveUnit() default TimeUnit.SECONDS;
    
    /**
     * Thread name prefix
     * @return thread name prefix
     */
    String threadPrefix() default "async-";
    
    /**
     * Whether threads should be daemon threads
     * @return true for daemon threads
     */
    boolean daemon() default true;
    
    /**
     * Thread priority (1-10)
     * @return thread priority
     */
    int threadPriority() default Thread.NORM_PRIORITY;
    
    /**
     * Whether to enable metrics collection
     * @return true to enable metrics
     */
    boolean enableMetrics() default true;
    
    /**
     * Tags for metrics and monitoring
     * @return array of tags
     */
    String[] tags() default {};
    
    /**
     * Custom rejection policy for when queue is full
     * @return rejection policy class
     */
    Class<?> rejectionPolicy() default Void.class;
    
    /**
     * Whether to enable circuit breaker pattern
     * @return true to enable circuit breaker
     */
    boolean circuitBreaker() default false;
    
    /**
     * Circuit breaker failure threshold
     * @return failure threshold percentage
     */
    int failureThreshold() default 50;
    
    /**
     * Circuit breaker reset timeout in seconds
     * @return reset timeout
     */
    long resetTimeout() default 60;
    
    /**
     * Whether to enable rate limiting
     * @return true to enable rate limiting
     */
    boolean rateLimited() default false;
    
    /**
     * Rate limit - operations per second
     * @return operations per second
     */
    double rateLimit() default 100.0;
    
    /**
     * Rate limit burst size
     * @return burst size
     */
    int burstSize() default 10;
    
    /**
     * Custom async processor class
     * @return processor class
     */
    Class<?> processor() default Void.class;
    
    /**
     * Whether to enable distributed execution
     * @return true for distributed execution
     */
    boolean distributed() default false;
    
    /**
     * Serialization format for distributed execution
     * @return serialization format
     */
    String serialization() default "JSON";
    
    /**
     * Whether to persist async tasks for reliability
     * @return true to persist tasks
     */
    boolean persistent() default false;
    
    /**
     * Task group for related operations
     * @return task group name
     */
    String taskGroup() default "";
    
    /**
     * Description for monitoring and documentation
     * @return task description
     */
    String description() default "";
}