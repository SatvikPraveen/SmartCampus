# syntax=docker/dockerfile:1.6
# Multi-stage build for the SmartCampus REST service.

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && java -Djarmode=layertools -jar target/smartcampus.jar extract --destination target/layers

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
# Dependency layers first so application changes do not invalidate them.
COPY --from=build /workspace/target/layers/dependencies/ ./
COPY --from=build /workspace/target/layers/spring-boot-loader/ ./
COPY --from=build /workspace/target/layers/snapshot-dependencies/ ./
COPY --from=build /workspace/target/layers/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Probes: /actuator/health/liveness and /actuator/health/readiness
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

LABEL org.opencontainers.image.title="SmartCampus" \
      org.opencontainers.image.description="Course timetabling REST service" \
      org.opencontainers.image.source="https://github.com/SatvikPraveen/SmartCampus" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.authors="Satvik Praveen"
