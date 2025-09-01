# Location: Dockerfile
# SmartCampus Backend - Multi-stage Docker Build
# This Dockerfile creates an optimized production image for the SmartCampus Backend

# Build stage
FROM maven:3.9.5-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven files for dependency caching
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install necessary packages for production
RUN apk add --no-cache \
    curl \
    tzdata \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Create application user for security
RUN addgroup -S smartcampus && \
    adduser -S smartcampus -G smartcampus

# Set timezone
ENV TZ=UTC

# Set working directory
WORKDIR /app

# Copy application JAR from builder stage
COPY --from=builder /app/target/smartcampus-backend.jar app.jar

# Create necessary directories
RUN mkdir -p /app/logs /app/uploads /app/config && \
    chown -R smartcampus:smartcampus /app

# Copy additional configuration files if they exist
COPY --chown=smartcampus:smartcampus config/ /app/config/ 2>/dev/null || true

# Switch to non-root user
USER smartcampus

# Environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -XX:+UseCompressedOops \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=UTC"

ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

# Start application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for better image management
LABEL maintainer="SmartCampus Development Team <dev-team@smartcampus.com>"
LABEL version="1.0.0"
LABEL description="SmartCampus Backend - Campus Management System API"
LABEL org.opencontainers.image.title="SmartCampus Backend"
LABEL org.opencontainers.image.description="RESTful API for comprehensive campus management"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="SmartCampus"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.source="https://github.com/smartcampus/smartcampus-backend"
LABEL org.opencontainers.image.documentation="https://docs.smartcampus.com"

# Development stage (optional - for development with hot reload)
FROM runtime AS development
USER root
RUN apk add --no-cache maven
USER smartcampus
ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.devtools.restart.enabled=true -jar app.jar"]