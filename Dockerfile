# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY --from=builder /app/target/wfotracker-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Xms128m","-Xmx256m","-jar","app.jar"]
