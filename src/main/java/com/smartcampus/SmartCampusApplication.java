// Location: src/main/java/com/smartcampus/SmartCampusApplication.java
package com.smartcampus;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

/**
 * SmartCampus REST service.
 *
 * <p>Exposes the course-timetabling engine in {@link scheduling} over HTTP, together with
 * Spring Boot Actuator health/info endpoints and OpenAPI documentation. The service is
 * stateless and needs no external database, so it starts with {@code mvn spring-boot:run}
 * or {@code java -jar target/smartcampus.jar}.</p>
 *
 * @author Satvik Praveen
 */
@SpringBootApplication(scanBasePackages = "com.smartcampus")
@OpenAPIDefinition(
    info = @Info(
        title = "SmartCampus API",
        version = "2.0.0",
        description = """
            Course timetabling as a service. Submit events (with enrolled students and an
            instructor), rooms and a weekly slot grid; receive a timetable produced by a
            selectable solver together with an itemised hard/soft constraint cost.
            """,
        contact = @Contact(name = "Satvik Praveen", url = "https://github.com/SatvikPraveen/SmartCampus"),
        license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")
    )
)
public class SmartCampusApplication {

    private static final Logger logger = LoggerFactory.getLogger(SmartCampusApplication.class);

    public static void main(String[] args) {
        Environment env = SpringApplication.run(SmartCampusApplication.class, args).getEnvironment();
        logger.info("SmartCampus started on port {} (profiles: {}); API docs at /swagger-ui.html",
                env.getProperty("local.server.port", env.getProperty("server.port", "8080")),
                String.join(",", env.getActiveProfiles()));
    }
}
