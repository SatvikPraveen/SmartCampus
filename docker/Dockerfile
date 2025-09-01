# Location: docker/Dockerfile
# SmartCampus Multi-stage Docker Build
# Optimized for production deployment with minimal image size

# Build arguments
ARG JAVA_VERSION=17
ARG MAVEN_VERSION=3.9.5
ARG BASE_IMAGE=eclipse-temurin

# Stage 1: Build environment
FROM ${BASE_IMAGE}:${JAVA_VERSION}-jdk-jammy as builder

LABEL maintainer="SmartCampus Team <team@smartcampus.edu>"
LABEL description="SmartCampus University Management System - Build Stage"

# Install build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzC /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

ENV MAVEN_HOME=/opt/maven
ENV MAVEN_CONFIG=/root/.m2

# Set working directory
WORKDIR /build

# Copy Maven configuration and dependencies first (for better caching)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download dependencies (cached layer)
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/
COPY docs/ docs/
COPY scripts/ scripts/

# Build arguments for build metadata
ARG VERSION=1.0.0
ARG BUILD_DATE
ARG VCS_REF
ARG BUILD_NUMBER

# Build application
RUN ./mvnw clean package -DskipTests \
    -Dbuild.version=${VERSION} \
    -Dbuild.date=${BUILD_DATE} \
    -Dgit.commit=${VCS_REF} \
    -Dbuild.number=${BUILD_NUMBER} \
    && echo "Build completed successfully"

# Stage 2: Runtime environment
FROM ${BASE_IMAGE}:${JAVA_VERSION}-jre-jammy as runtime

LABEL maintainer="SmartCampus Team <team@smartcampus.edu>"
LABEL description="SmartCampus University Management System"
LABEL version="${VERSION}"
LABEL vcs-ref="${VCS_REF}"
LABEL build-date="${BUILD_DATE}"

# Build arguments
ARG VERSION=1.0.0
ARG BUILD_DATE
ARG VCS_REF

# Install runtime dependencies and security updates
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    fontconfig \
    tzdata \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Create application user and group
RUN groupadd -r smartcampus && \
    useradd -r -g smartcampus -d /opt/smartcampus -s /sbin/nologin \
    -c "SmartCampus application user" smartcampus

# Set up application directory
WORKDIR /opt/smartcampus

# Create necessary directories
RUN mkdir -p /opt/smartcampus/config \
    /opt/smartcampus/logs \
    /opt/smartcampus/data \
    /opt/smartcampus/temp \
    && chown -R smartcampus:smartcampus /opt/smartcampus

# Copy application JAR from build stage
COPY --from=builder --chown=smartcampus:smartcampus /build/target/smartcampus-*.jar app.jar

# Copy configuration files
COPY --chown=smartcampus:smartcampus src/main/resources/config/ config/
COPY --chown=smartcampus:smartcampus docker/entrypoint.sh .
COPY --chown=smartcampus:smartcampus docker/healthcheck.sh .

# Make scripts executable
RUN chmod +x entrypoint.sh healthcheck.sh

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8080 \
    LOGGING_LEVEL_ROOT=INFO \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,info,prometheus \
    MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when_authorized \
    APP_HOME=/opt/smartcampus

# Expose port
EXPOSE ${SERVER_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD ./healthcheck.sh

# Set security context
USER smartcampus

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start application
CMD ["./entrypoint.sh"]

# Metadata labels
LABEL org.opencontainers.image.title="SmartCampus"
LABEL org.opencontainers.image.description="University Management System built with Java and Spring Boot"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.revision="${VCS_REF}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/your-org/smartcampus"
LABEL org.opencontainers.image.url="https://github.com/your-org/smartcampus"
LABEL org.opencontainers.image.documentation="https://github.com/your-org/smartcampus/tree/main/docs"
LABEL org.opencontainers.image.vendor="SmartCampus Team"
LABEL org.opencontainers.image.licenses="MIT"