FROM openjdk:11-jre-slim as builder
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:11-jre-slim

LABEL org.opencontainers.image.source="https://github.com/dschanoeh/HealthBuddy"
LABEL org.opencontainers.image.description="A service that periodically queries health endpoints and generates alerts"

WORKDIR /app
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./
VOLUME /app/application.yaml

HEALTHCHECK CMD curl --fail http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
