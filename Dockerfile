# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp

# Create a non-root group and user
RUN addgroup -S spring && adduser -S spring -G spring

# Run subsequent commands and the app as non-root user
USER spring:spring

COPY --from=builder /app/target/wfotracker-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
