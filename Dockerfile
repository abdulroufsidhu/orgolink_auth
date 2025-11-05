FROM eclipse-temurin:21-alpine

LABEL maintainer="Abdul Rouf Sidhu <abdulroufsidhu@gmail.com>"
LABEL description="Orgolink Authentication Service"

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew bootJar --no-daemon

# Create non-root user
RUN groupadd -r orgolink && useradd -r -g orgolink orgolink

# Create logs directory
RUN mkdir -p /app/logs && chown -R orgolink:orgolink /app/logs

# Copy built jar
RUN cp build/libs/orgolink-auth-*.jar app.jar

# Remove build files to reduce image size
RUN rm -rf gradle gradlew build.gradle.kts settings.gradle.kts src build .gradle

# Switch to non-root user
USER orgolink

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/public/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=dev"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
