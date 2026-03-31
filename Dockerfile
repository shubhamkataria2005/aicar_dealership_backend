FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy gradle wrapper files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test

EXPOSE 8080

CMD ["java", "-jar", "build/libs/*.jar"]