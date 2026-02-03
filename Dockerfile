# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user
RUN groupadd -r flowforge && useradd -r -g flowforge flowforge

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R flowforge:flowforge /app

USER flowforge

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
