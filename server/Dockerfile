# Stage 1: Build the application
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app
COPY . .
# Build the server distribution
RUN ./gradlew :server:installDist --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the install distribution from the build stage
COPY --from=build /app/server/build/install/server .

# Ensure execution permissions
RUN chmod +x ./bin/server

EXPOSE 8080

# Allow passing JAVA_OPTS for memory limits (Critical for t3.micro)
CMD ["./bin/server"]
