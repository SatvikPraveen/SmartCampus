package reflection;

import annotations.Async;
import annotations.Audited;
import annotations.Cacheable;
import annotations.Validator;
import annotations.Validator.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reflection.DynamicProxy.InvocationContext;
import reflection.DynamicProxy.ValidationException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(10)
class DynamicProxyTest {

    // ==================== FIXTURES ====================

    public interface Greeter {
        String greet(String name);
    }

    public interface CampusService {
        String plain(String value);

        @Cacheable
        String cached(String key);

        String notNull(@Validator(type = Type.NOT_NULL) String value) throws ValidationException;

        String nonEmpty(@Validator(type = Type.NOT_EMPTY) String value) throws ValidationException;

        String email(@Validator(type = Type.EMAIL) String value) throws ValidationException;

        String positive(@Validator(type = Type.POSITIVE) Integer value) throws ValidationException;

        @Audited
        String audited(String value);

        @Audited
        String validatedAndAudited(@Validator(type = Type.NOT_NULL) String value) throws ValidationException;

        @Async(strategy = Async.Strategy.FIRE_AND_FORGET)
        void fireAndForget(String value);

        @Async(strategy = Async.Strategy.FIRE_AND_FORGET)
        void validatedFireAndForget(@Validator(type = Type.NOT_NULL) String value) throws ValidationException;

        @Async(strategy = Async.Strategy.FUTURE)
        Object future(String value);

        @Async(strategy = Async.Strategy.CALLBACK)
        String callback(String value);

        void fail();
    }

    /** Records calls and the thread they ran on; optional latches let tests coordinate. */
    static class RecordingService implements CampusService {
        final List<String> calls = new CopyOnWriteArrayList<>();
        final AtomicReference<Thread> asyncThread = new AtomicReference<>();
        final CountDownLatch asyncDone = new CountDownLatch(1);
        volatile CountDownLatch entered = new CountDownLatch(0);
        volatile CountDownLatch release = new CountDownLatch(0);

        @Override public String plain(String value) { calls.add("plain:" + value); return "plain:" + value; }
        @Override public String cached(String key) { calls.add("cached:" + key); return "value-" + key; }
        @Override public String notNull(String value) { calls.add("notNull:" + value); return value; }
        @Override public String nonEmpty(String value) { calls.add("nonEmpty:" + value); return value; }
        @Override public String email(String value) { calls.add("email:" + value); return value; }
        @Override public String positive(Integer value) { calls.add("positive:" + value); return "ok"; }
        @Override public String audited(String value) {
            calls.add("audited:" + value);
            if ("boom".equals(value)) {
                throw new IllegalStateException("audited failure");
            }
            return value.toUpperCase();
        }

        @Override public String validatedAndAudited(String value) {
            calls.add("validatedAndAudited:" + value);
            if ("block".equals(value)) {
                entered.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return value;
        }

        @Override public void fireAndForget(String value) {
            calls.add("fireAndForget:" + value);
            asyncThread.set(Thread.currentThread());
            asyncDone.countDown();
        }

        @Override public void validatedFireAndForget(String value) {
            calls.add("validatedFireAndForget:" + value);
            asyncThread.set(Thread.currentThread());
            asyncDone.countDown();
        }

        @Override public Object future(String value) { return "computed-" + value + "-on-" + Thread.currentThread().getName(); }
        @Override public String callback(String value) { return "sync-" + value; }
        @Override public void fail() { throw new IllegalArgumentException("bad input"); }
    }

    static class NoInterfaces { }

    static class SimpleGreeter implements Greeter {
        private final String prefix;
        SimpleGreeter(String prefix) { this.prefix = prefix; }
        @Override public String greet(String name) { return prefix + " " + name; }
    }

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;
    private RecordingService target;
    private CampusService proxy;

    @BeforeEach
    void setUp() {
        DynamicProxy.clearCache();
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
        target = new RecordingService();
        proxy = DynamicProxy.createProxy(CampusService.class, target);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        DynamicProxy.clearCache();
    }

    private String stdout() {
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    // ==================== TESTS ====================

    @Nested
    class Creation {

        @Test
        void proxyForTargetImplementsItsInterfacesAndDelegates() {
            Greeter greeter = DynamicProxy.createProxy(new SimpleGreeter("Hi"));

            assertThat(DynamicProxy.isProxy(greeter)).isTrue();
            assertThat(greeter.greet("Ann")).isEqualTo("Hi Ann");
        }

        @Test
        void sameTargetReusesCachedProxyUntilCacheCleared() {
            Greeter greeter = new SimpleGreeter("Hi");
            Greeter first = DynamicProxy.createProxy(greeter);

            assertThat(DynamicProxy.createProxy(greeter)).isSameAs(first);

            DynamicProxy.clearCache();
            assertThat(DynamicProxy.createProxy(greeter)).isNotSameAs(first);
        }

        // Regression: the proxy cache was keyed by the target's class, so proxying a second
        // instance of the same class returned the first instance's proxy (calls went to the wrong object).
        @Test
        void distinctTargetsOfSameClassGetTheirOwnProxies() {
            Greeter hello = DynamicProxy.createProxy(new SimpleGreeter("Hello"));
            Greeter howdy = DynamicProxy.createProxy(new SimpleGreeter("Howdy"));

            assertThat(howdy).isNotSameAs(hello);
            assertThat(hello.greet("Bo")).isEqualTo("Hello Bo");
            assertThat(howdy.greet("Bo")).isEqualTo("Howdy Bo");
        }

        @Test
        void targetWithoutInterfacesIsRejected() {
            assertThatThrownBy(() -> DynamicProxy.createProxy(new NoInterfaces()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one interface");
        }

        @Test
        void interfaceProxyRequiresAnInterface() {
            assertThatThrownBy(() -> DynamicProxy.createProxy(SimpleGreeter.class, new SimpleGreeter("x")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Class must be an interface");
        }

        @Test
        void customHandlerProxyRoutesEveryCallToHandler() {
            InvocationHandler handler = (p, m, a) -> m.getName() + ":" + a[0];
            Greeter greeter = DynamicProxy.createProxy(Greeter.class, handler);

            assertThat(greeter.greet("Ann")).isEqualTo("greet:Ann");
        }
    }

    @Nested
    class TargetAccess {

        @Test
        void getTargetUnwrapsEnhancedProxy() {
            assertThat(DynamicProxy.getTarget(proxy)).isSameAs(target);
        }

        @Test
        void getTargetReturnsNonProxyUnchanged() {
            Object plain = new Object();

            assertThat(DynamicProxy.isProxy(plain)).isFalse();
            assertThat(DynamicProxy.getTarget(plain)).isSameAs(plain);
        }

        @Test
        void getTargetReturnsForeignProxyItself() {
            Greeter mock = DynamicProxy.createMockProxy(Greeter.class, (m, a) -> "mock");

            assertThat(DynamicProxy.getTarget(mock)).isSameAs(mock);
        }
    }

    @Nested
    class ObjectMethods {

        @Test
        void toStringAndHashCodeDescribeTarget() {
            assertThat(proxy.toString()).isEqualTo("Proxy[RecordingService@" + target.hashCode() + "]");
            assertThat(proxy.hashCode()).isEqualTo(target.hashCode());
        }

        @Test
        void proxiesOfSameTargetAreEqual() {
            CampusService other = DynamicProxy.createProxy(CampusService.class, target);

            assertThat(proxy.equals(other)).isTrue();
            assertThat(proxy.equals(DynamicProxy.createProxy(CampusService.class, new RecordingService()))).isFalse();
        }

        @Test
        void proxyIsNotEqualToNullNonProxyOrForeignProxy() {
            CampusService foreign = DynamicProxy.createMockProxy(CampusService.class, (m, a) -> null);

            assertThat(proxy.equals(null)).isFalse();
            assertThat(proxy.equals(target)).isFalse();
            assertThat(proxy.equals(foreign)).isFalse();
        }
    }

    @Nested
    class Interception {

        @Test
        void unannotatedMethodDelegatesDirectly() {
            assertThat(proxy.plain("x")).isEqualTo("plain:x");
            assertThat(target.calls).containsExactly("plain:x");
        }

        @Test
        void cacheableMethodReturnsTargetValueIncludingForNullKey() {
            assertThat(proxy.cached("k1")).isEqualTo("value-k1");
            assertThat(proxy.cached(null)).isEqualTo("value-null");
        }

        // Regression: target exceptions surfaced as UndeclaredThrowableException wrapping an
        // InvocationTargetException instead of the original exception.
        @Test
        void targetExceptionPropagatesUnwrapped() {
            assertThatThrownBy(proxy::fail)
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("bad input");
        }

        @Test
        void auditedMethodLogsBeforeAndAfter() {
            assertThat(proxy.audited("ok")).isEqualTo("OK");

            assertThat(stdout())
                    .contains("AUDIT: BEFORE - audited")
                    .contains("AUDIT: AFTER - audited")
                    .doesNotContain("AUDIT: ERROR");
        }

        @Test
        void auditedMethodLogsErrorAndRethrowsOriginalException() {
            assertThatThrownBy(() -> proxy.audited("boom"))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("audited failure");

            assertThat(stdout()).contains("AUDIT: ERROR - audited").doesNotContain("AUDIT: AFTER");
        }
    }

    @Nested
    class Validation {

        @Test
        void notNullRejectsNullWithoutInvokingTarget() {
            assertThatThrownBy(() -> proxy.notNull(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageEndingWith("cannot be null");
            assertThat(target.calls).isEmpty();
        }

        @Test
        void notEmptyRejectsNullAndEmptyString() {
            assertThatThrownBy(() -> proxy.nonEmpty("")).hasMessageEndingWith("cannot be empty");
            assertThatThrownBy(() -> proxy.nonEmpty(null)).isInstanceOf(ValidationException.class);
            assertThat(target.calls).isEmpty();
        }

        @Test
        void emailRejectsMalformedAddress() {
            assertThatThrownBy(() -> proxy.email("nope")).hasMessageEndingWith("must be a valid email");
        }

        @Test
        void validArgumentsReachTarget() throws Exception {
            assertThat(proxy.notNull("a")).isEqualTo("a");
            assertThat(proxy.nonEmpty("b")).isEqualTo("b");
            assertThat(proxy.email("x@y.edu")).isEqualTo("x@y.edu");
            assertThat(proxy.email(null)).isNull();
            assertThat(proxy.positive(-1)).isEqualTo("ok"); // POSITIVE is not enforced by the proxy
            assertThat(target.calls).containsExactly(
                    "notNull:a", "nonEmpty:b", "email:x@y.edu", "email:null", "positive:-1");
        }
    }

    @Nested
    class Chaining {

        // Regression: each interceptor invoked the target itself instead of proceeding down the
        // chain, so only the first interceptor ever ran (validation suppressed auditing).
        @Test
        void validationAndAuditBothApply() throws Exception {
            assertThat(proxy.validatedAndAudited("v")).isEqualTo("v");

            assertThat(target.calls).containsExactly("validatedAndAudited:v");
            assertThat(stdout())
                    .contains("AUDIT: BEFORE - validatedAndAudited")
                    .contains("AUDIT: AFTER - validatedAndAudited");
        }

        @Test
        void validationFailureShortCircuitsBeforeAudit() {
            assertThatThrownBy(() -> proxy.validatedAndAudited(null)).isInstanceOf(ValidationException.class);

            assertThat(target.calls).isEmpty();
            assertThat(stdout()).doesNotContain("AUDIT");
        }

        // Regression: with validation first in the chain, @Async was ignored and the method ran synchronously.
        @Test
        void validatedAsyncMethodStillRunsOnAnotherThread() throws Exception {
            proxy.validatedFireAndForget("go");

            assertThat(target.asyncDone.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(target.asyncThread.get()).isNotSameAs(Thread.currentThread());
            assertThat(target.calls).containsExactly("validatedFireAndForget:go");
        }

        // Regression: the chain kept its position in a shared field, so a call arriving while another
        // was in flight started mid-chain and skipped validation entirely.
        @Test
        void concurrentInvocationIsStillValidated() throws Exception {
            target.entered = new CountDownLatch(1);
            target.release = new CountDownLatch(1);

            CompletableFuture<String> inFlight = CompletableFuture.supplyAsync(() -> {
                try {
                    return proxy.validatedAndAudited("block");
                } catch (ValidationException e) {
                    throw new IllegalStateException(e);
                }
            });
            assertThat(target.entered.await(5, TimeUnit.SECONDS)).isTrue();

            try {
                assertThatThrownBy(() -> proxy.validatedAndAudited(null)).isInstanceOf(ValidationException.class);
            } finally {
                target.release.countDown();
            }

            assertThat(inFlight.get(5, TimeUnit.SECONDS)).isEqualTo("block");
            assertThat(target.calls).containsExactly("validatedAndAudited:block");
        }
    }

    @Nested
    class AsyncExecution {

        @Test
        void fireAndForgetReturnsImmediatelyAndRunsOnAnotherThread() throws Exception {
            proxy.fireAndForget("x");

            assertThat(target.asyncDone.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(target.asyncThread.get()).isNotSameAs(Thread.currentThread());
            assertThat(target.calls).containsExactly("fireAndForget:x");
        }

        @Test
        void futureStrategyWrapsResultInFuture() throws Exception {
            Object result = proxy.future("job");

            assertThat(result).isInstanceOf(Future.class);
            Object value = ((Future<?>) result).get(5, TimeUnit.SECONDS);
            assertThat(value).asString()
                    .startsWith("computed-job-on-")
                    .doesNotEndWith(Thread.currentThread().getName());
        }

        @Test
        void otherStrategiesExecuteSynchronously() {
            assertThat(proxy.callback("c")).isEqualTo("sync-c");
        }
    }

    @Nested
    class LazyAndMockProxies {

        @Test
        void lazyProxyCreatesTargetOnFirstCallOnly() {
            AtomicInteger created = new AtomicInteger();
            Greeter lazy = DynamicProxy.createLazyProxy(Greeter.class, () -> {
                created.incrementAndGet();
                return new SimpleGreeter("Lazy");
            });

            assertThat(created).hasValue(0);
            assertThat(lazy.greet("A")).isEqualTo("Lazy A");
            assertThat(lazy.greet("B")).isEqualTo("Lazy B");
            assertThat(created).hasValue(1);
        }

        // Regression: exceptions from the lazily loaded target surfaced as UndeclaredThrowableException.
        @Test
        void lazyProxyPropagatesTargetExceptionUnwrapped() {
            Greeter lazy = DynamicProxy.createLazyProxy(Greeter.class, () -> name -> {
                throw new IllegalStateException("no greeting for " + name);
            });

            assertThatThrownBy(() -> lazy.greet("Z"))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("no greeting for Z");
        }

        @Test
        void mockProxyDelegatesToBehaviour() {
            Greeter mock = DynamicProxy.createMockProxy(Greeter.class,
                    (method, args) -> "mocked " + method.getName() + "(" + args[0] + ")");

            assertThat(mock.greet("Q")).isEqualTo("mocked greet(Q)");
            assertThat(Proxy.isProxyClass(mock.getClass())).isTrue();
        }
    }

    @Nested
    class Context {

        @Test
        void invocationContextExposesCallDataAndAttributes() throws Throwable {
            SimpleGreeter greeter = new SimpleGreeter("Yo");
            InvocationContext context = new InvocationContext(
                    "proxy", greeter, Greeter.class.getMethod("greet", String.class), new Object[]{"Al"});
            context.setAttribute("k", 1);

            assertThat(context.getProxy()).isEqualTo("proxy");
            assertThat(context.getTarget()).isSameAs(greeter);
            assertThat(context.getMethod().getName()).isEqualTo("greet");
            assertThat(context.getArguments()).containsExactly("Al");
            assertThat(context.getAttribute("k")).isEqualTo(1);
            assertThat(context.getAttributes()).containsEntry("k", 1);
            assertThat(context.proceed()).isEqualTo("Yo Al");
        }

        @Test
        void exceptionTypesCarryMessageAndCause() {
            Throwable cause = new RuntimeException("root");

            assertThat(new ValidationException("v", cause)).hasMessage("v").hasCause(cause);
            assertThat(new DynamicProxy.ProxyCreationException("p")).hasMessage("p");
            assertThat(new DynamicProxy.ProxyCreationException("p", cause)).hasCause(cause);
        }
    }
}
