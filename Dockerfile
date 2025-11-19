# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21-jammy AS build
WORKDIR /workspace

# Copy pom and download dependencies first (cache)
COPY pom.xml .
RUN mvn -B -e -q dependency:go-offline

# Copy source and build
COPY . .
RUN mvn -B -DskipTests clean package

# Stage 2: Run runtime image
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copy jar from build stage (adjust pattern if your artifact has a classifier)
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
