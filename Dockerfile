# Use Java 21 runtime
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy built JAR into container
COPY target/spring-boot-docker.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
