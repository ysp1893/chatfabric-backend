# Multi-stage build for chatfabric-backend

# =========================
# Stage 1: Build
# =========================
FROM maven:3.8-openjdk-8 AS builder

WORKDIR /app

# Copy only pom first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:8-jre

WORKDIR /app

# Install curl (needed for healthcheck)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy JAR from builder
COPY --from=builder /app/target/chatfabric-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Railway uses dynamic PORT)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run application
ENTRYPOINT ["java","-jar","app.jar"]