FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY gradlew .
COPY .gradle .gradle
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Grant execution rights on the gradlew script
RUN chmod +x gradlew

# Build the application
RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/build/libs
COPY --from=build ${DEPENDENCY}/*.jar app.jar

# Create a non-root user for security (good practice for Podman too)
RUN addgroup -S orgolink && adduser -S orgolink -G orgolink
USER orgolink:orgolink

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
