# syntax=docker/dockerfile:1
# Multi-stage Dockerfile for ApexCare Clinical Management System

# Stage 1: Build self-contained Spring Boot JAR
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Copy Gradle wrapper and configuration files first (maximizes layer caching)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY server/build.gradle ./server/
COPY desktop/build.gradle ./desktop/

RUN chmod +x gradlew

# Copy webapp assets and server source code
COPY webapp ./webapp
COPY server/src ./server/src

# Package production self-contained fat JAR
RUN ./gradlew :server:bootJar --no-daemon

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root system group and user
RUN groupadd -r hms && useradd -r -g hms -m -d /app hms

# Prepare persistent data directory
RUN mkdir -p /app/data && chown -R hms:hms /app

# Copy compiled JAR from builder
COPY --from=builder /workspace/server/build/libs/server-*.jar /app/app.jar

USER hms

# Default environment configuration
ENV PORT=8088 \
    SPRING_PROFILES_ACTIVE=dev,demo \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8088 10000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8088}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
