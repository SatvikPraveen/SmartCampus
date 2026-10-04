package exceptions;

import exceptions.SystemException.ComponentType;
import exceptions.SystemException.ErrorCode;
import exceptions.SystemException.Severity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SystemExceptionTest {

    private static final Set<ErrorCode> RECOVERABLE = EnumSet.of(
            ErrorCode.TIMEOUT_ERROR, ErrorCode.NETWORK_ERROR, ErrorCode.SERVICE_UNAVAILABLE,
            ErrorCode.RESOURCE_EXHAUSTED, ErrorCode.THREAD_POOL_EXHAUSTED, ErrorCode.CONNECTION_POOL_ERROR,
            ErrorCode.RATE_LIMIT_EXCEEDED, ErrorCode.CIRCUIT_BREAKER_OPEN, ErrorCode.EXTERNAL_API_ERROR);

    private static final Set<ErrorCode> AVAILABILITY = EnumSet.of(
            ErrorCode.SERVICE_UNAVAILABLE, ErrorCode.STARTUP_ERROR, ErrorCode.SHUTDOWN_ERROR,
            ErrorCode.HEALTH_CHECK_FAILED, ErrorCode.CLUSTER_ERROR, ErrorCode.LOAD_BALANCER_ERROR,
            ErrorCode.API_GATEWAY_ERROR);

    private static SystemException of(ErrorCode code) {
        return new SystemException(code, null);
    }

    private static SystemException withSeverity(Severity severity) {
        return SystemException.builder(ErrorCode.UNKNOWN_SYSTEM_ERROR).severity(severity).build();
    }

    @Nested
    class ErrorCodes {

        @Test
        void codesAreUniqueAndPrefixed() {
            List<String> codes = Stream.of(ErrorCode.values()).map(ErrorCode::getCode).toList();
            assertThat(codes).doesNotHaveDuplicates().allMatch(c -> c.matches("SYS_\\d{3}"));
            assertThat(ErrorCode.UNKNOWN_SYSTEM_ERROR.getCode()).isEqualTo("SYS_999");
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void everyCodeHasADescriptionUsedAsDefaultMessage(ErrorCode code) {
            assertThat(code.getDescription()).isNotBlank();
            assertThat(of(code).getMessage()).isEqualTo(code.getDescription());
        }

        @Test
        void componentTypesHaveDisplayNames() {
            assertThat(ComponentType.API_GATEWAY.getDisplayName()).isEqualTo("API Gateway");
            assertThat(Stream.of(ComponentType.values()).map(ComponentType::getDisplayName))
                    .doesNotHaveDuplicates().allMatch(n -> !n.isBlank());
        }

        @Test
        void severityLevelsAreOrdered() {
            assertThat(Stream.of(Severity.values()).map(Severity::getLevel)).containsExactly(1, 2, 3, 4);
            assertThat(Severity.CRITICAL.getDisplayName()).isEqualTo("Critical");
        }
    }

    @Nested
    class Constructors {

        @Test
        void simpleConstructorDefaultsContext() {
            SystemException e = new SystemException(ErrorCode.CACHE_ERROR, "boom");

            assertThat(e.getMessage()).isEqualTo("boom");
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CACHE_ERROR);
            assertThat(e.getComponentType()).isEqualTo(ComponentType.UNKNOWN);
            assertThat(e.getComponentName()).isNull();
            assertThat(e.getOperation()).isNull();
            assertThat(e.getTimestamp()).isNotNull();
            assertThat(e.getCorrelationId()).isNotBlank();
            assertThat(e.getSystemContext()).isEmpty();
            assertThat(e.getHostName()).isNotBlank();
            assertThat(e.getApplicationVersion()).isNotNull();
            assertThat(e.getEnvironmentName()).isNotNull();
            assertThat(e.getSeverity()).isEqualTo(Severity.MEDIUM);
            assertThat(e.getCause()).isNull();
        }

        @Test
        void causeConstructorChainsCause() {
            IOException cause = new IOException("disk");
            SystemException e = new SystemException(ErrorCode.FILE_SYSTEM_ERROR, null, cause);

            assertThat(e).hasCause(cause).hasMessage("File system error");
        }

        @Test
        void correlationIdsAreUniquePerInstance() {
            assertThat(of(ErrorCode.CACHE_ERROR).getCorrelationId())
                    .isNotEqualTo(of(ErrorCode.CACHE_ERROR).getCorrelationId());
        }

        @Test
        void detailedConstructorAppendsComponentContext() {
            SystemException e = new SystemException(ErrorCode.CACHE_ERROR, ComponentType.CACHE, "redis",
                    "get", "Cache down", null);

            assertThat(e.getMessage()).isEqualTo("Cache down [Component: Cache, Name: redis, Operation: get]");
            assertThat(e.getComponentName()).isEqualTo("redis");
            assertThat(e.getOperation()).isEqualTo("get");
        }

        @Test
        void detailedMessageOmitsUnknownComponentAndFallsBackToDescriptionForBlankMessage() {
            SystemException e = new SystemException(ErrorCode.TIMEOUT_ERROR, ComponentType.UNKNOWN, null,
                    "sync", "   ", null);

            assertThat(e.getMessage()).isEqualTo("Operation timeout [Operation: sync]");
        }

        @Test
        void detailedMessageWithoutAnyContextHasNoBrackets() {
            SystemException e = new SystemException(ErrorCode.TIMEOUT_ERROR, ComponentType.UNKNOWN, null,
                    null, null, null);

            assertThat(e.getMessage()).isEqualTo("Operation timeout");
        }

        @Test
        void nameOnlyContextHasNoLeadingComma() {
            SystemException e = new SystemException(ErrorCode.PLUGIN_ERROR, ComponentType.UNKNOWN, "p1",
                    null, "x", null);

            assertThat(e.getMessage()).isEqualTo("x [Name: p1]");
        }

        @Test
        void fullConstructorHonoursExplicitCorrelationIdAndSeverity() {
            SystemException e = new SystemException(ErrorCode.CACHE_ERROR, ComponentType.CACHE, null, null,
                    "corr-1", Severity.CRITICAL, "m", null);

            assertThat(e.getCorrelationId()).isEqualTo("corr-1");
            assertThat(e.getSeverity()).isEqualTo(Severity.CRITICAL);
        }

        @Test
        void fullConstructorDerivesMissingCorrelationIdAndSeverity() {
            SystemException e = new SystemException(ErrorCode.MEMORY_ERROR, ComponentType.UNKNOWN, null, null,
                    null, null, "m", null);

            assertThat(e.getCorrelationId()).isNotBlank();
            assertThat(e.getSeverity()).isEqualTo(Severity.CRITICAL);
        }

        // Regression: a null component type used to NPE while building the message.
        @Test
        void nullComponentTypeIsTreatedAsUnknown() {
            assertThatCode(() -> new SystemException(ErrorCode.CACHE_ERROR, null, "c", "op", "m", null))
                    .doesNotThrowAnyException();

            SystemException e = SystemException.builder(ErrorCode.CACHE_ERROR)
                    .componentType(null).message("m").build();

            assertThat(e.getComponentType()).isEqualTo(ComponentType.UNKNOWN);
            assertThat(e.getMessage()).isEqualTo("m");
            assertThat(e.toString()).contains("component=Unknown");
            assertThat(e.toMonitoringAlert()).containsEntry("component", "Unknown");
        }
    }

    @Nested
    class Builder {

        @Test
        void buildsConsistentState() {
            RuntimeException cause = new RuntimeException("root");
            SystemException e = SystemException.builder(ErrorCode.SCHEDULER_ERROR)
                    .componentType(ComponentType.SCHEDULER)
                    .componentName("quartz")
                    .operation("fire")
                    .message("Job failed")
                    .cause(cause)
                    .correlationId("c-42")
                    .addContext("job", "nightly")
                    .addContext(Map.of("attempt", 3))
                    .build();

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULER_ERROR);
            assertThat(e.getComponentType()).isEqualTo(ComponentType.SCHEDULER);
            assertThat(e.getMessage()).isEqualTo("Job failed [Component: Scheduler, Name: quartz, Operation: fire]");
            assertThat(e).hasCause(cause);
            assertThat(e.getCorrelationId()).isEqualTo("c-42");
            assertThat(e.getSeverity()).isEqualTo(Severity.LOW);
            assertThat(e.getSystemContext()).containsOnly(Map.entry("job", "nightly"), Map.entry("attempt", 3));
        }

        @Test
        void builderDefaultsToUnknownComponent() {
            assertThat(SystemException.builder(ErrorCode.CACHE_ERROR).build().getComponentType())
                    .isEqualTo(ComponentType.UNKNOWN);
        }
    }

    @Nested
    class Context {

        @Test
        void getSystemContextReturnsDefensiveCopy() {
            SystemException e = SystemException.builder(ErrorCode.CACHE_ERROR).addContext("k", "v").build();

            e.getSystemContext().put("k", "tampered");
            e.getSystemContext().clear();

            assertThat(e.getSystemContext()).containsOnly(Map.entry("k", "v"));
        }

        @Test
        void typedLookupChecksType() {
            SystemException e = SystemException.builder(ErrorCode.CACHE_ERROR).addContext("n", 5L).build();

            assertThat(e.getSystemContext("n")).isEqualTo(5L);
            assertThat(e.getSystemContext("n", Long.class)).isEqualTo(5L);
            assertThat(e.getSystemContext("n", String.class)).isNull();
            assertThat(e.getSystemContext("missing", Long.class)).isNull();
            assertThat(e.getSystemContext("missing")).isNull();
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void configurationError() {
            Exception cause = new IllegalStateException("bad");
            SystemException e = SystemException.configurationError("db.url", "missing", cause);

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFIGURATION_ERROR);
            assertThat(e.getMessage()).isEqualTo("Configuration error for key 'db.url': missing");
            assertThat(e.getSystemContext()).containsEntry("configKey", "db.url").containsEntry("reason", "missing");
            assertThat(e).hasCause(cause);
            assertThat(e.getSeverity()).isEqualTo(Severity.HIGH);
        }

        @Test
        void serviceUnavailable() {
            SystemException e = SystemException.serviceUnavailable("mail", "down");

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(e.getComponentName()).isEqualTo("mail");
            assertThat(e.getMessage()).isEqualTo("Service 'mail' is unavailable: down [Name: mail]");
        }

        @Test
        void resourceExhausted() {
            SystemException e = SystemException.resourceExhausted("Memory", "heap", 90, 100);

            assertThat(e.getMessage()).isEqualTo("Memory 'heap' exhausted: 90/100");
            assertThat(e.getSystemContext()).containsEntry("currentUsage", 90L).containsEntry("maxCapacity", 100L);
        }

        @Test
        void networkError() {
            SystemException e = SystemException.networkError("h", 80, "connect", null);

            assertThat(e.getComponentType()).isEqualTo(ComponentType.NETWORK);
            assertThat(e.getMessage()).startsWith("Network error during connect to h:80");
            assertThat(e.getSystemContext()).containsEntry("port", 80);
        }

        @Test
        void timeoutError() {
            SystemException e = SystemException.timeoutError("sync", 500, null);

            assertThat(e.getMessage()).startsWith("Operation 'sync' timed out after 500ms");
            assertThat(e.getSystemContext("timeoutMs", Long.class)).isEqualTo(500L);
        }

        @Test
        void databaseErrorMentionsTableOnlyWhenGiven() {
            assertThat(SystemException.databaseError("insert", "users", null).getMessage())
                    .startsWith("Database error during insert on table users");
            SystemException noTable = SystemException.databaseError("insert", null, null);
            assertThat(noTable.getMessage()).startsWith("Database error during insert [");
            assertThat(noTable.getComponentType()).isEqualTo(ComponentType.DATABASE);
        }

        @Test
        void cacheFileSystemExternalApiAndRateLimit() {
            assertThat(SystemException.cacheError("c", "put", null).getMessage())
                    .startsWith("Cache error in 'c' during put");
            assertThat(SystemException.fileSystemError("/tmp", "write", null).getSystemContext())
                    .containsEntry("path", "/tmp");

            SystemException api = SystemException.externalApiError("Stripe", "/pay", 502, "bad gw", null);
            assertThat(api.getComponentType()).isEqualTo(ComponentType.INTEGRATION);
            assertThat(api.getMessage()).startsWith("External API error from Stripe at /pay (status: 502)");
            assertThat(api.getSystemContext()).containsEntry("statusCode", 502).containsEntry("response", "bad gw");

            SystemException rl = SystemException.rateLimitExceeded("api", 120, 100);
            assertThat(rl.getComponentType()).isEqualTo(ComponentType.RATE_LIMITER);
            assertThat(rl.getMessage()).startsWith("Rate limit exceeded for 'api': 120/100");
        }
    }

    @Nested
    class Classification {

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void recoverableCodes(ErrorCode code) {
            assertThat(of(code).isRecoverable()).isEqualTo(RECOVERABLE.contains(code));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void availabilityCodes(ErrorCode code) {
            assertThat(of(code).affectsAvailability()).isEqualTo(AVAILABILITY.contains(code));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "MEMORY_ERROR, CRITICAL", "DISK_SPACE_ERROR, CRITICAL", "STARTUP_ERROR, CRITICAL",
                "SHUTDOWN_ERROR, CRITICAL", "SECURITY_ERROR, CRITICAL", "HEALTH_CHECK_FAILED, CRITICAL",
                "SERVICE_UNAVAILABLE, HIGH", "RESOURCE_EXHAUSTED, HIGH", "NETWORK_ERROR, HIGH",
                "CONFIGURATION_ERROR, HIGH", "BACKUP_ERROR, HIGH", "RESTORE_ERROR, HIGH",
                "TIMEOUT_ERROR, MEDIUM", "CACHE_ERROR, MEDIUM", "FILE_SYSTEM_ERROR, MEDIUM",
                "THREAD_POOL_EXHAUSTED, MEDIUM", "CONNECTION_POOL_ERROR, MEDIUM", "EXTERNAL_API_ERROR, MEDIUM",
                "RATE_LIMIT_EXCEEDED, MEDIUM", "SCHEDULER_ERROR, LOW", "MESSAGE_QUEUE_ERROR, LOW",
                "LOGGING_ERROR, LOW", "MONITORING_ERROR, LOW", "NOTIFICATION_ERROR, LOW",
                "PLUGIN_ERROR, MEDIUM"
        })
        void severityIsDerivedFromErrorCode(ErrorCode code, Severity expected) {
            assertThat(of(code).getSeverity()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}: priority {1}, immediate={2}")
        @CsvSource({"CRITICAL, 1, true", "HIGH, 2, true", "MEDIUM, 3, false", "LOW, 4, false"})
        void severityDrivesPriorityAndAttention(Severity severity, int priority, boolean immediate) {
            SystemException e = withSeverity(severity);

            assertThat(e.getAlertPriority()).isEqualTo(priority);
            assertThat(e.requiresImmediateAttention()).isEqualTo(immediate);
        }

        @Test
        void alertChannelsEscalateWithSeverity() {
            assertThat(withSeverity(Severity.CRITICAL).getRecommendedAlertChannels())
                    .containsExactly("PAGER", "SMS", "EMAIL", "SLACK");
            assertThat(withSeverity(Severity.HIGH).getRecommendedAlertChannels()).containsExactly("EMAIL", "SLACK");
            assertThat(withSeverity(Severity.MEDIUM).getRecommendedAlertChannels()).containsExactly("SLACK");
            assertThat(withSeverity(Severity.LOW).getRecommendedAlertChannels()).containsExactly("LOG");
        }

        @Test
        void performanceImpactPerSeverity() {
            assertThat(withSeverity(Severity.CRITICAL).getPerformanceImpact()).startsWith("Service disruption");
            assertThat(withSeverity(Severity.HIGH).getPerformanceImpact()).startsWith("Significant");
            assertThat(withSeverity(Severity.MEDIUM).getPerformanceImpact()).startsWith("Minor");
            assertThat(withSeverity(Severity.LOW).getPerformanceImpact()).startsWith("Minimal");
        }

        @Test
        void autoRecoveryRequiresRecoverableAndHighSeverity() {
            assertThat(of(ErrorCode.NETWORK_ERROR).shouldTriggerAutoRecovery()).isTrue();   // HIGH + recoverable
            assertThat(of(ErrorCode.TIMEOUT_ERROR).shouldTriggerAutoRecovery()).isFalse();  // MEDIUM
            assertThat(of(ErrorCode.MEMORY_ERROR).shouldTriggerAutoRecovery()).isFalse();   // not recoverable
            assertThat(SystemException.builder(ErrorCode.TIMEOUT_ERROR).severity(Severity.CRITICAL).build()
                    .shouldTriggerAutoRecovery()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "SERVICE_UNAVAILABLE | RESTART_SERVICE,HEALTH_CHECK",
                "RESOURCE_EXHAUSTED | SCALE_UP,CLEAR_CACHE",
                "CACHE_ERROR | CLEAR_CACHE,RESTART_CACHE_SERVICE",
                "NETWORK_ERROR | RETRY_WITH_BACKOFF,FAILOVER",
                "TIMEOUT_ERROR | RETRY_WITH_BACKOFF",
                "THREAD_POOL_EXHAUSTED | SCALE_THREAD_POOL,REJECT_NEW_REQUESTS",
                "CONNECTION_POOL_ERROR | RECREATE_CONNECTIONS,SCALE_CONNECTION_POOL",
                "CIRCUIT_BREAKER_OPEN | WAIT_FOR_RECOVERY,HEALTH_CHECK"
        })
        void autoRecoveryActions(ErrorCode code, String actions) {
            assertThat(of(code).getAutoRecoveryActions()).containsExactly(actions.split(","));
        }

        @Test
        void noAutoRecoveryActionsForOtherCodes() {
            assertThat(of(ErrorCode.LICENSE_ERROR).getAutoRecoveryActions()).isEmpty();
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "CONFIGURATION_ERROR | Check system configuration",
                "SERVICE_UNAVAILABLE | Check service health",
                "RESOURCE_EXHAUSTED | Scale up resources",
                "NETWORK_ERROR | Check network connectivity",
                "TIMEOUT_ERROR | Increase timeout values",
                "CACHE_ERROR | Clear cache",
                "FILE_SYSTEM_ERROR | Check file system permissions",
                "RATE_LIMIT_EXCEEDED | Reduce request rate",
                "EXTERNAL_API_ERROR | Check external API status",
                "LICENSE_ERROR | Contact system administrator"
        })
        void recoverySuggestions(ErrorCode code, String prefix) {
            assertThat(of(code).getRecoverySuggestion()).startsWith(prefix);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = '|', value = {
                "DATABASE | High - Data operations affected",
                "AUTHENTICATION | High - User access affected",
                "AUTHORIZATION | High - User access affected",
                "NOTIFICATION | Medium - Communication services affected",
                "EMAIL | Medium - Communication services affected",
                "SMS | Medium - Communication services affected",
                "CACHE | Medium - Performance may be degraded",
                "SEARCH | Medium - Performance may be degraded",
                "ANALYTICS | Low - Non-critical features affected",
                "REPORTING | Low - Non-critical features affected",
                "WORKFLOW | Medium - System functionality may be affected"
        })
        void businessImpactByComponent(ComponentType type, String expected) {
            SystemException e = SystemException.builder(ErrorCode.PLUGIN_ERROR).componentType(type).build();

            assertThat(e.getBusinessImpact()).isEqualTo(expected);
        }

        @Test
        void availabilityImpactTrumpsComponent() {
            SystemException e = SystemException.builder(ErrorCode.STARTUP_ERROR)
                    .componentType(ComponentType.ANALYTICS).build();

            assertThat(e.getBusinessImpact()).isEqualTo("High - Service availability affected");
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    class Serialization {

        private SystemException sample() {
            return SystemException.builder(ErrorCode.NETWORK_ERROR)
                    .componentType(ComponentType.NETWORK)
                    .componentName("edge")
                    .operation("connect")
                    .message("unreachable")
                    .correlationId("corr")
                    .addContext("host", "h")
                    .cause(new IOException("refused"))
                    .build();
        }

        @Test
        void monitoringAlertCarriesClassificationAndCause() {
            Map<String, Object> alert = sample().toMonitoringAlert();

            assertThat(alert)
                    .containsEntry("alertType", "SYSTEM_EXCEPTION")
                    .containsEntry("severity", "HIGH")
                    .containsEntry("errorCode", "SYS_006")
                    .containsEntry("component", "Network")
                    .containsEntry("componentName", "edge")
                    .containsEntry("operation", "connect")
                    .containsEntry("correlationId", "corr")
                    .containsEntry("recoverable", true)
                    .containsEntry("affectsAvailability", false)
                    .containsEntry("requiresImmediateAttention", true)
                    .containsEntry("context", Map.of("host", "h"))
                    .containsKeys("timestamp", "hostName", "environment", "version", "message");
            @SuppressWarnings("unchecked")
            Map<String, Object> cause = (Map<String, Object>) alert.get("cause");
            assertThat(cause).containsEntry("type", "IOException").containsEntry("message", "refused")
                    .containsKey("location");
        }

        @Test
        void monitoringAlertOmitsEmptyContextAndCause() {
            Map<String, Object> alert = of(ErrorCode.CACHE_ERROR).toMonitoringAlert();

            assertThat(alert).doesNotContainKeys("context", "cause");
        }

        @Test
        void causeWithoutStackTraceHasNoLocation() {
            IOException cause = new IOException("x");
            cause.setStackTrace(new StackTraceElement[0]);
            SystemException e = new SystemException(ErrorCode.CACHE_ERROR, "m", cause);

            @SuppressWarnings("unchecked")
            Map<String, Object> causeInfo = (Map<String, Object>) e.toMonitoringAlert().get("cause");
            assertThat(causeInfo).doesNotContainKey("location");
            assertThat(e.getDiagnosticInfo()).doesNotContainKey("stackTrace");
        }

        @Test
        void diagnosticInfoIncludesRuntimeThreadAndTrimmedStackTrace() {
            Map<String, Object> d = sample().getDiagnosticInfo();

            assertThat(d).containsEntry("errorCode", "SYS_006")
                    .containsEntry("errorDescription", "Network connectivity error")
                    .containsEntry("severity", "HIGH")
                    .containsEntry("componentType", "Network")
                    .containsEntry("systemContext", Map.of("host", "h"))
                    .containsKeys("javaVersion", "osName", "osVersion", "runtime", "thread");
            assertThat((Map<String, Object>) d.get("runtime")).containsKeys("maxMemory", "usedMemory", "availableProcessors");
            assertThat((Map<String, Object>) d.get("thread")).containsKeys("activeThreadCount", "currentThreadName");
            assertThat((List<Object>) d.get("stackTrace")).hasSizeBetween(1, 5);
        }

        @Test
        void diagnosticInfoWithoutContextOrCause() {
            assertThat(of(ErrorCode.CACHE_ERROR).getDiagnosticInfo()).doesNotContainKeys("systemContext", "stackTrace");
        }

        @Test
        void structuredLogEntryHasTags() {
            Map<String, Object> log = sample().toStructuredLogEntry();

            assertThat(log).containsEntry("level", "HIGH").containsEntry("logger", "SystemException")
                    .containsEntry("correlationId", "corr").containsEntry("context", Map.of("host", "h"));
            assertThat((Map<String, Object>) log.get("exception")).containsEntry("code", "SYS_006");
            assertThat((Map<String, Object>) log.get("component")).containsEntry("type", "Network").containsEntry("name", "edge");
            assertThat((Map<String, Object>) log.get("cause")).containsEntry("type", "java.io.IOException");
            assertThat((List<Object>) log.get("tags")).containsExactly("system-exception", "high", "network", "recoverable");
        }

        @Test
        void structuredLogEntryTagsAvailabilityImpact() {
            Map<String, Object> log = of(ErrorCode.STARTUP_ERROR).toStructuredLogEntry();

            assertThat((List<Object>) log.get("tags")).containsExactly("system-exception", "critical", "unknown",
                    "availability-impact");
            assertThat(log).doesNotContainKeys("context", "cause");
        }

        @Test
        void toStringIncludesOptionalPartsOnlyWhenPresent() {
            String full = sample().toString();
            assertThat(full).startsWith("SystemException{errorCode=SYS_006, severity=HIGH")
                    .contains("componentName='edge'", "operation='connect'", "correlationId='corr'",
                            "recoverable=true", "context={host=h}", "cause=IOException: refused")
                    .endsWith("}");

            String bare = of(ErrorCode.CACHE_ERROR).toString();
            assertThat(bare).doesNotContain("componentName=", "operation=", "context=", "cause=");
        }
    }

    @Test
    void recoverableSetMatchesDocumentedNonRecoverableCodes() {
        Set<ErrorCode> nonRecoverable = Stream.of(ErrorCode.values())
                .filter(c -> !of(c).isRecoverable()).collect(Collectors.toSet());
        assertThat(nonRecoverable).contains(ErrorCode.CONFIGURATION_ERROR, ErrorCode.MEMORY_ERROR,
                ErrorCode.VERSION_MISMATCH, ErrorCode.CLASS_LOADING_ERROR);
    }
}
