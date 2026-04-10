# Multi-stage build for chatfabric-backend
# Stage 1: Build
FROM maven:3.8-openjdk-8 AS builder

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:8-jre-slim

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/chatfabric-backend-0.0.1-SNAPSHOT.jar chatfabric-backend.jar

# Expose ports
# 8080 for HTTP
# 8081 for WebSocket (if needed)
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "chatfabric-backend.jar"]
