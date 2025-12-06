# Simple Docker build - use Gradle to build and run
FROM gradle:8.5-jdk17

# Set working directory
WORKDIR /app

# Copy everything
COPY . .

# Build the application
RUN gradle clean bootJar --no-daemon

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8443

# Run the application
CMD ["java", "-jar", "build/libs/gridee-backend-0.0.1-SNAPSHOT.jar"]
