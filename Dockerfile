# Stage 1: Build the application
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon -x test

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "app.jar"]