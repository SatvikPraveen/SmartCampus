// Location: src/main/java/com/smartcampus/SmartCampusApplication.java
package com.smartcampus;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SmartCampus Backend Application
 * 
 * This is the main entry point for the SmartCampus Backend application.
 * It configures Spring Boot with all necessary features for a complete
 * campus management system.
 * 
 * Features enabled:
 * - JPA Auditing for entity tracking
 * - JPA Repositories for data access
 * - Caching for performance optimization
 * - Async processing for non-blocking operations
 * - Scheduled tasks for background processing
 * - Transaction management for data consistency
 * - Configuration properties scanning
 * - OpenAPI/Swagger documentation
 * 
 * @author SmartCampus Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.smartcampus")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "com.smartcampus.repository")
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@ConfigurationPropertiesScan(basePackages = "com.smartcampus.config.properties")
@OpenAPIDefinition(
    info = @Info(
        title = "SmartCampus Backend API",
        version = "1.0.0",
        description = """
            SmartCampus Backend provides comprehensive RESTful APIs for managing 
            educational institutions. This includes user management, academic 
            operations, facility booking, notifications, and administrative functions.
            
            ## Features
            - **User Management**: Multi-role authentication and authorization
            - **Academic Management**: Courses, enrollments, grades, and scheduling
            - **Facility Management**: Room booking and equipment management
            - **Communication**: Notifications and messaging system
            - **Reporting**: Analytics and administrative reports
            
            ## Authentication
            Most endpoints require JWT authentication. Use the `/api/auth/login` 
            endpoint to obtain a token, then include it in the `Authorization` 
            header as `Bearer {token}`.
            
            ## Rate Limiting
            API requests are rate-limited to ensure fair usage. Check response 
            headers for current limits and remaining quota.
            """,
        contact = @Contact(
            name = "SmartCampus Development Team",
            email = "dev-team@smartcampus.com",
            url = "https://smartcampus.com/support"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development Server"),
        @Server(url = "https://api.smartcampus.com", description = "Production Server"),
        @Server(url = "https://staging-api.smartcampus.com", description = "Staging Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authentication token. Format: Bearer {token}"
)
public class SmartCampusApplication {

    private static final Logger logger = LoggerFactory.getLogger(SmartCampusApplication.class);

    /**
     * Main method to start the SmartCampus Backend application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Configure system properties for optimal performance
        configureSystemProperties();
        
        // Start the Spring Boot application
        SpringApplication app = new SpringApplication(SmartCampusApplication.class);
        Environment env = app.run(args).getEnvironment();
        
        // Log application startup information
        logApplicationStartup(env);
    }

    /**
     * Configure system properties for optimal application performance.
     */
    private static void configureSystemProperties() {
        // JVM optimizations
        System.setProperty("java.awt.headless", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "UTC");
        
        // Networking optimizations
        System.setProperty("networkaddress.cache.ttl", "60");
        System.setProperty("networkaddress.cache.negative.ttl", "10");
        
        // Logging optimizations
        System.setProperty("logging.pattern.dateformat", "yyyy-MM-dd HH:mm:ss.SSS");
        
        // Security properties
        System.setProperty("java.security.egd", "file:/dev/./urandom");
        
        logger.debug("System properties configured for optimal performance");
    }

    /**
     * Log comprehensive application startup information.
     * 
     * @param env the Spring Environment
     */
    private static void logApplicationStartup(Environment env) {
        String applicationName = env.getProperty("spring.application.name", "SmartCampus Backend");
        String applicationVersion = env.getProperty("spring.application.version", "1.0.0");
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        String[] activeProfiles = env.getActiveProfiles();
        String profilesInfo = activeProfiles.length == 0 ? "default" : String.join(", ", activeProfiles);
        
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Unable to determine host address: {}", e.getMessage());
        }
        
        String startupTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        
        logger.info("""
            
            ----------------------------------------------------------
            🚀 {} ({}) is running successfully!
            ----------------------------------------------------------
            📅 Started at: {}
            🌍 Environment: {}
            🔗 Local URL: {}://localhost:{}{}
            🌐 External URL: {}://{}:{}{}
            📖 API Docs: {}://localhost:{}/swagger-ui.html
            ❤️  Health Check: {}://localhost:{}/actuator/health
            📊 Metrics: {}://localhost:{}/actuator/metrics
            ----------------------------------------------------------
            🎯 Ready to serve requests!
            ----------------------------------------------------------
            """,
            applicationName,
            applicationVersion,
            startupTime,
            profilesInfo,
            protocol, port, contextPath,
            protocol, hostAddress, port, contextPath,
            protocol, port,
            protocol, port,
            protocol, port
        );

        // Log important configuration information
        logConfigurationInfo(env);
        
        // Log security information
        logSecurityInfo(env);
        
        // Log database information
        logDatabaseInfo(env);
        
        // Log performance information
        logPerformanceInfo();
    }

    /**
     * Log important configuration information.
     */
    private static void logConfigurationInfo(Environment env) {
        logger.info("Configuration Summary:");
        logger.info("  📦 Spring Boot Version: {}", org.springframework.boot.SpringBootVersion.getVersion());
        logger.info("  ☕ Java Version: {}", System.getProperty("java.version"));
        logger.info("  🏠 Working Directory: {}", System.getProperty("user.dir"));
        logger.info("  📂 Temp Directory: {}", System.getProperty("java.io.tmpdir"));
        logger.info("  🌐 Default Charset: {}", java.nio.charset.Charset.defaultCharset());
        logger.info("  🕐 Timezone: {}", java.time.ZoneId.systemDefault());
    }

    /**
     * Log security-related information.
     */
    private static void logSecurityInfo(Environment env) {
        boolean jwtEnabled = env.getProperty("jwt.secret") != null;
        String corsOrigins = env.getProperty("app.cors.allowed-origins", "Not configured");
        
        logger.info("Security Configuration:");
        logger.info("  🔐 JWT Authentication: {}", jwtEnabled ? "Enabled" : "Disabled");
        logger.info("  🌍 CORS Origins: {}", corsOrigins);
        logger.info("  🛡️  HTTPS: {}", env.getProperty("server.ssl.enabled", "false"));
    }

    /**
     * Log database-related information.
     */
    private static void logDatabaseInfo(Environment env) {
        String datasourceUrl = env.getProperty("spring.datasource.url", "Not configured");
        String flywayEnabled = env.getProperty("spring.flyway.enabled", "false");
        String redisHost = env.getProperty("spring.data.redis.host", "Not configured");
        
        logger.info("Database Configuration:");
        logger.info("  🗄️  Database URL: {}", maskSensitiveUrl(datasourceUrl));
        logger.info("  🔄 Flyway Migration: {}", flywayEnabled);
        logger.info("  📦 Redis Cache: {}", redisHost.equals("Not configured") ? "Disabled" : "Enabled");
    }

    /**
     * Log performance-related information.
     */
    private static void logPerformanceInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        int availableProcessors = runtime.availableProcessors();
        
        logger.info("Performance Information:");
        logger.info("  💾 Max Memory: {} MB", maxMemory);
        logger.info("  📊 Total Memory: {} MB", totalMemory);
        logger.info("  🆓 Free Memory: {} MB", freeMemory);
        logger.info("  🖥️  Available Processors: {}", availableProcessors);
    }

    /**
     * Mask sensitive information in database URLs for logging.
     */
    private static String maskSensitiveUrl(String url) {
        if (url == null || url.equals("Not configured")) {
            return url;
        }
        
        // Mask password in JDBC URL
        return url.replaceAll("password=[^&\\s]+", "password=***");
    }
}