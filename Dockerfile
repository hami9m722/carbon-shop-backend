# Build stage
FROM gradle:8.5-jdk21 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the application
RUN gradle build --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Environment variables
ENV JAVA_OPTS=""

# Expose the port your application runs on
EXPOSE 8080

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]