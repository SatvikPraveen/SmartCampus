// File location: src/main/java/reflection/DynamicProxy.java
package reflection;

import annotations.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic proxy implementation using Java's Proxy API
 * Provides runtime interception and enhancement of method calls
 * Supports caching, validation, auditing, and async processing through proxies
 */
public class DynamicProxy {
    
    // Keyed by target identity: a proxy must only be reused for the very same target instance
    private static final Map<Object, Object> proxyCache = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final AnnotationProcessor annotationProcessor = new AnnotationProcessor();
    
    // ==================== PROXY CREATION METHODS ====================
    
    /**
     * Creates a dynamic proxy for the given object
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        Class<?> targetClass = target.getClass();
        
        // Check if a proxy already exists for this target
        Object existing = proxyCache.get(target);
        if (existing != null) {
            return (T) existing;
        }
        
        // Get all interfaces implemented by the target class
        Class<?>[] interfaces = getAllInterfaces(targetClass);
        
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Target class must implement at least one interface to be proxied");
        }
        
        // Create proxy with enhanced invocation handler
        T proxy = (T) Proxy.newProxyInstance(
            targetClass.getClassLoader(),
            interfaces,
            new EnhancedInvocationHandler(target)
        );
        
        proxyCache.put(target, proxy);
        return proxy;
    }
    
    /**
     * Creates a proxy for a specific interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, T target) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Class must be an interface");
        }
        
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new EnhancedInvocationHandler(target)
        );
    }
    
    /**
     * Creates a lazy proxy that loads the target on first access
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLazyProxy(Class<T> interfaceClass, ProxySupplier<T> supplier) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new LazyInvocationHandler<>(supplier)
        );
    }
    
    /**
     * Creates a mock proxy for testing
     */
    @SuppressWarnings("unchecked")
    public static <T> T createMockProxy(Class<T> interfaceClass, MockBehavior mockBehavior) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new MockInvocationHandler(mockBehavior)
        );
    }
    
    /**
     * Creates a proxy with custom invocation handler
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            handler
        );
    }
    
    // ==================== ENHANCED INVOCATION HANDLER ====================
    
    /**
     * Enhanced invocation handler that processes annotations and provides method interception
     */
    private static class EnhancedInvocationHandler implements InvocationHandler {
        
        private final Object target;
        private final Map<Method, MethodInterceptor> interceptors = new ConcurrentHashMap<>();
        private final ProxyMetrics metrics = new ProxyMetrics();
        
        public EnhancedInvocationHandler(Object target) {
            this.target = target;
            initializeInterceptors();
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long startTime = System.currentTimeMillis();
            
            try {
                // Handle Object methods
                if (method.getDeclaringClass() == Object.class) {
                    return handleObjectMethod(proxy, method, args);
                }
                
                // Get or create method interceptor
                MethodInterceptor interceptor = interceptors.computeIfAbsent(method, this::createInterceptor);
                
                // Execute through interceptor chain
                InvocationContext context = new InvocationContext(proxy, target, method, args);
                Object result = interceptor.intercept(context);
                
                // Update metrics
                metrics.recordInvocation(method, System.currentTimeMillis() - startTime, true);
                
                return result;
                
            } catch (InvocationTargetException e) {
                // Surface the target's own exception rather than the reflective wrapper
                metrics.recordInvocation(method, System.currentTimeMillis() - startTime, false);
                throw e.getCause();
            } catch (Exception e) {
                metrics.recordInvocation(method, System.currentTimeMillis() - startTime, false);
                throw e;
            }
        }
        
        private void initializeInterceptors() {
            // Interceptors are created lazily per method
        }
        
        private MethodInterceptor createInterceptor(Method method) {
            InterceptorChain chain = new InterceptorChain();
            
            // Add interceptors based on annotations
            addCacheInterceptor(method, chain);
            addValidationInterceptor(method, chain);
            addAuditInterceptor(method, chain);
            addAsyncInterceptor(method, chain);
            
            // Always add the actual method invocation at the end
            chain.addLast(new MethodInvocationInterceptor());
            
            return chain;
        }
        
        private void addCacheInterceptor(Method method, InterceptorChain chain) {
            if (method.isAnnotationPresent(Cacheable.class)) {
                chain.addLast(new CacheInterceptor(method.getAnnotation(Cacheable.class)));
            }
        }
        
        private void addValidationInterceptor(Method method, InterceptorChain chain) {
            if (hasValidationAnnotations(method)) {
                chain.addLast(new ValidationInterceptor());
            }
        }
        
        private void addAuditInterceptor(Method method, InterceptorChain chain) {
            if (method.isAnnotationPresent(Audited.class)) {
                chain.addLast(new AuditInterceptor(method.getAnnotation(Audited.class)));
            }
        }
        
        private void addAsyncInterceptor(Method method, InterceptorChain chain) {
            if (method.isAnnotationPresent(Async.class)) {
                chain.addLast(new AsyncInterceptor(method.getAnnotation(Async.class)));
            }
        }
        
        private boolean hasValidationAnnotations(Method method) {
            for (java.lang.reflect.Parameter param : method.getParameters()) {
                if (param.isAnnotationPresent(Validator.class)) {
                    return true;
                }
            }
            return false;
        }
        
        private Object handleObjectMethod(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            switch (methodName) {
                case "equals":
                    return proxy_equals(proxy, args[0]);
                case "hashCode":
                    return proxy_hashCode();
                case "toString":
                    return proxy_toString();
                default:
                    return method.invoke(target, args);
            }
        }
        
        private boolean proxy_equals(Object proxy, Object other) {
            return (other != null) && 
                   (Proxy.isProxyClass(other.getClass())) &&
                   (Proxy.getInvocationHandler(other) instanceof EnhancedInvocationHandler) &&
                   (((EnhancedInvocationHandler) Proxy.getInvocationHandler(other)).target.equals(this.target));
        }
        
        private int proxy_hashCode() {
            return target.hashCode();
        }
        
        private String proxy_toString() {
            return "Proxy[" + target.getClass().getSimpleName() + "@" + target.hashCode() + "]";
        }
    }
    
    // ==================== LAZY INVOCATION HANDLER ====================
    
    /**
     * Invocation handler for lazy loading proxies
     */
    private static class LazyInvocationHandler<T> implements InvocationHandler {
        
        private final ProxySupplier<T> supplier;
        private volatile T target;
        private volatile boolean initialized = false;
        
        public LazyInvocationHandler(ProxySupplier<T> supplier) {
            this.supplier = supplier;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        target = supplier.get();
                        initialized = true;
                    }
                }
            }
            
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
    
    // ==================== MOCK INVOCATION HANDLER ====================
    
    /**
     * Invocation handler for mock proxies
     */
    private static class MockInvocationHandler implements InvocationHandler {
        
        private final MockBehavior mockBehavior;
        
        public MockInvocationHandler(MockBehavior mockBehavior) {
            this.mockBehavior = mockBehavior;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return mockBehavior.handle(method, args);
        }
    }
    
    // ==================== INTERCEPTOR INTERFACES ====================
    
    /**
     * Method interceptor interface
     */
    public interface MethodInterceptor {
        Object intercept(InvocationContext context) throws Throwable;
    }
    
    /**
     * Invocation context containing method call information
     */
    public static class InvocationContext {
        private final Object proxy;
        private final Object target;
        private final Method method;
        private final Object[] arguments;
        private final Map<String, Object> attributes = new HashMap<>();
        private Next next;
        
        /** Continuation to the next element of an interceptor chain. */
        @FunctionalInterface
        interface Next {
            Object call() throws Throwable;
        }
        
        public InvocationContext(Object proxy, Object target, Method method, Object[] arguments) {
            this.proxy = proxy;
            this.target = target;
            this.method = method;
            this.arguments = arguments;
        }
        
        public Object getProxy() { return proxy; }
        public Object getTarget() { return target; }
        public Method getMethod() { return method; }
        public Object[] getArguments() { return arguments; }
        public Map<String, Object> getAttributes() { return attributes; }
        
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        public Object getAttribute(String key) {
            return attributes.get(key);
        }
        
        /**
         * Proceeds to the next interceptor in the chain, or invokes the target method
         * directly when this context is not driven by a chain.
         */
        public Object proceed() throws Throwable {
            Next n = next;
            return n != null ? n.call() : method.invoke(target, arguments);
        }
    }
    
    // ==================== INTERCEPTOR CHAIN ====================
    
    /**
     * Chain of method interceptors
     */
    private static class InterceptorChain implements MethodInterceptor {
        private final List<MethodInterceptor> interceptors = new ArrayList<>();
        
        public void addLast(MethodInterceptor interceptor) {
            interceptors.add(interceptor);
        }
        
        public void addFirst(MethodInterceptor interceptor) {
            interceptors.add(0, interceptor);
        }
        
        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            return invokeAt(0, context);
        }
        
        // The position is per invocation (not shared chain state) so concurrent calls cannot interfere
        private Object invokeAt(int index, InvocationContext context) throws Throwable {
            if (index < interceptors.size()) {
                context.next = () -> invokeAt(index + 1, context);
                return interceptors.get(index).intercept(context);
            }
            return null; // Should not reach here
        }
        
        public Object proceed(InvocationContext context) throws Throwable {
            return intercept(context);
        }
    }
    
    // ==================== SPECIFIC INTERCEPTORS ====================
    
    /**
     * Cache interceptor for @Cacheable methods
     */
    private static class CacheInterceptor implements MethodInterceptor {
        private record Entry(Object value, long expiresAtNanos) { }
        
        private final Cacheable cacheableAnnotation;
        private final long ttlNanos;
        private final Map<List<Object>, Entry> cache;
        
        public CacheInterceptor(Cacheable cacheableAnnotation) {
            this.cacheableAnnotation = cacheableAnnotation;
            this.ttlNanos = cacheableAnnotation.ttl() > 0
                    ? TimeUnit.SECONDS.toNanos(cacheableAnnotation.ttl()) : Long.MAX_VALUE;
            int maxEntries = Math.max(1, cacheableAnnotation.maxEntries());
            // access-ordered, so the eldest entry is the least recently used one
            this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<List<Object>, Entry> eldest) {
                    return size() > maxEntries;
                }
            });
        }
        
        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            List<Object> cacheKey = generateCacheKey(context);
            long now = System.nanoTime();
            
            Entry cached = cache.get(cacheKey);
            if (cached != null) {
                if (cached.expiresAtNanos() == Long.MAX_VALUE || now - cached.expiresAtNanos() < 0) {
                    return cached.value();
                }
                cache.remove(cacheKey, cached);
            }
            
            Object result = context.proceed();
            
            if (result != null || cacheableAnnotation.cacheNullValues()) {
                long expiresAt = ttlNanos == Long.MAX_VALUE ? Long.MAX_VALUE : now + ttlNanos;
                cache.put(cacheKey, new Entry(result, expiresAt));
            }
            
            return result;
        }
        
        // Keyed by the argument values themselves (not their hash codes, which can collide)
        private List<Object> generateCacheKey(InvocationContext context) {
            Object[] args = context.getArguments();
            return args == null ? List.of() : Arrays.asList(args.clone());
        }
        
        int size() {
            return cache.size();
        }
    }
    
    /**
     * Validation interceptor for @Validator methods
     */
    private static class ValidationInterceptor implements MethodInterceptor {
        
        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            // Validate parameters
            validateParameters(context);
            
            // Proceed with method execution
            return context.proceed();
        }
        
        private void validateParameters(InvocationContext context) throws ValidationException {
            java.lang.reflect.Parameter[] parameters = context.getMethod().getParameters();
            Object[] arguments = context.getArguments();
            
            for (int i = 0; i < parameters.length && i < arguments.length; i++) {
                Validator validator = parameters[i].getAnnotation(Validator.class);
                if (validator != null) {
                    validateParameter(parameters[i].getName(), arguments[i], validator);
                }
            }
        }
        
        private void validateParameter(String paramName, Object value, Validator validator) throws ValidationException {
            switch (validator.type()) {
                case NOT_NULL:
                    if (value == null) {
                        throw new ValidationException("Parameter " + paramName + " cannot be null");
                    }
                    break;
                case NOT_EMPTY:
                    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                        throw new ValidationException("Parameter " + paramName + " cannot be empty");
                    }
                    break;
                case EMAIL:
                    if (value instanceof String && !isValidEmail((String) value)) {
                        throw new ValidationException("Parameter " + paramName + " must be a valid email");
                    }
                    break;
                // Add more validation types as needed
            }
        }
        
        private boolean isValidEmail(String email) {
            return email != null && email.contains("@") && email.contains(".");
        }
    }
    
    /**
     * Audit interceptor for @Audited methods
     */
    private static class AuditInterceptor implements MethodInterceptor {
        private final Audited auditedAnnotation;
        
        public AuditInterceptor(Audited auditedAnnotation) {
            this.auditedAnnotation = auditedAnnotation;
        }
        
        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            long startTime = System.currentTimeMillis();
            String methodName = context.getMethod().getName();
            
            try {
                // Record audit entry
                recordAuditEntry(context, "BEFORE");
                
                // Proceed with method execution
                Object result = context.proceed();
                
                // Record successful completion
                recordAuditEntry(context, "AFTER", result, System.currentTimeMillis() - startTime);
                
                return result;
                
            } catch (Exception e) {
                // Record exception
                recordAuditEntry(context, "ERROR", e, System.currentTimeMillis() - startTime);
                throw e;
            }
        }
        
        private void recordAuditEntry(InvocationContext context, String phase) {
            recordAuditEntry(context, phase, null, 0);
        }
        
        private void recordAuditEntry(InvocationContext context, String phase, Object result, long duration) {
            // Placeholder - would integrate with actual audit system
            System.out.println("AUDIT: " + phase + " - " + context.getMethod().getName() + 
                             " - Duration: " + duration + "ms");
        }
    }
    
    /**
     * Async interceptor for @Async methods
     */
    private static class AsyncInterceptor implements MethodInterceptor {
        private final Async asyncAnnotation;
        
        public AsyncInterceptor(Async asyncAnnotation) {
            this.asyncAnnotation = asyncAnnotation;
        }
        
        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            if (asyncAnnotation.strategy() == Async.Strategy.FIRE_AND_FORGET) {
                // Execute asynchronously without waiting for result
                executeAsync(context);
                return null;
            } else if (asyncAnnotation.strategy() == Async.Strategy.FUTURE) {
                // Return Future for result access
                return executeAsyncWithFuture(context);
            }
            
            // Default to synchronous execution
            return context.proceed();
        }
        
        private void executeAsync(InvocationContext context) {
            Thread.startVirtualThread(() -> {
                try {
                    context.proceed();
                } catch (Throwable e) {
                    // Handle async execution errors
                    System.err.println("Async execution error: " + e.getMessage());
                }
            });
        }
        
        private java.util.concurrent.Future<Object> executeAsyncWithFuture(InvocationContext context) {
            return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return context.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
    
    /**
     * Final interceptor that performs the actual method invocation
     */
    private static class MethodInvocationInterceptor implements MethodInterceptor {
        
        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            return context.getMethod().invoke(context.getTarget(), context.getArguments());
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets all interfaces implemented by a class, including inherited ones
     */
    private static Class<?>[] getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<>();
        
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            interfaces.addAll(Arrays.asList(currentClass.getInterfaces()));
            currentClass = currentClass.getSuperclass();
        }
        
        return interfaces.toArray(new Class<?>[0]);
    }
    
    /**
     * Checks if an object is a proxy
     */
    public static boolean isProxy(Object obj) {
        return Proxy.isProxyClass(obj.getClass());
    }
    
    /**
     * Gets the target object from a proxy
     */
    public static Object getTarget(Object proxy) {
        if (!isProxy(proxy)) {
            return proxy;
        }
        
        InvocationHandler handler = Proxy.getInvocationHandler(proxy);
        if (handler instanceof EnhancedInvocationHandler) {
            return ((EnhancedInvocationHandler) handler).target;
        }
        
        return proxy;
    }
    
    /**
     * Clears the proxy cache
     */
    public static void clearCache() {
        proxyCache.clear();
    }
    
    // ==================== SUPPORTING INTERFACES ====================
    
    /**
     * Functional interface for lazy proxy suppliers
     */
    @FunctionalInterface
    public interface ProxySupplier<T> {
        T get() throws Exception;
    }
    
    /**
     * Interface for mock behavior
     */
    @FunctionalInterface
    public interface MockBehavior {
        Object handle(Method method, Object[] args) throws Throwable;
    }
    
    // ==================== METRICS AND MONITORING ====================
    
    /**
     * Proxy metrics for monitoring method invocations
     */
    private static class ProxyMetrics {
        private final Map<Method, MethodStats> methodStats = new ConcurrentHashMap<>();
        
        public void recordInvocation(Method method, long duration, boolean success) {
            methodStats.computeIfAbsent(method, k -> new MethodStats()).record(duration, success);
        }
        
        public Map<Method, MethodStats> getStats() {
            return new HashMap<>(methodStats);
        }
        
        private static class MethodStats {
            private long totalInvocations = 0;
            private long totalDuration = 0;
            private long successCount = 0;
            private long errorCount = 0;
            
            public synchronized void record(long duration, boolean success) {
                totalInvocations++;
                totalDuration += duration;
                if (success) {
                    successCount++;
                } else {
                    errorCount++;
                }
            }
            
            public long getTotalInvocations() { return totalInvocations; }
            public double getAverageDuration() { 
                return totalInvocations > 0 ? (double) totalDuration / totalInvocations : 0; 
            }
            public double getSuccessRate() { 
                return totalInvocations > 0 ? (double) successCount / totalInvocations : 0; 
            }
            public long getErrorCount() { return errorCount; }
        }
    }
    
    // ==================== EXCEPTIONS ====================
    
    /**
     * Exception thrown during validation
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown during proxy creation
     */
    public static class ProxyCreationException extends RuntimeException {
        public ProxyCreationException(String message) {
            super(message);
        }
        
        public ProxyCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}