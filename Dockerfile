# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
ARG PROFILE=dl
ENV ENV=${PROFILE}
WORKDIR /workspace/app
COPY . .
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y curl iputils-ping && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/app/build/libs/*.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
ENV LOG_DIR=/app/logs
EXPOSE 8080
VOLUME ["/app/logs"]
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
