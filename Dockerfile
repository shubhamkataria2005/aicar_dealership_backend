# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install gradle
RUN apk add --no-cache gradle

# Copy source code
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Build the application
RUN gradle build -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]