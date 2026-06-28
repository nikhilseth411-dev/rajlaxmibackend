# ================================================================
# RajLaxmi Jewellers — Multi-Stage Dockerfile
# Stage 1: Build with Maven
# Stage 2: Run with minimal JRE (smaller image)
# ================================================================

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first (layer cache — faster rebuilds)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -q

# Stage 2: Runtime (lean image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S rajlaxmi && adduser -S rajlaxmi -G rajlaxmi

# Copy built jar from builder stage
COPY --from=builder /app/target/rajlaxmi-jewellers-1.0.0.jar app.jar

# Create uploads directory
RUN mkdir -p uploads/products && chown -R rajlaxmi:rajlaxmi /app

USER rajlaxmi

EXPOSE 8080

# Health check for Railway/Render
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xmx512m", \
  "-Xms256m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
