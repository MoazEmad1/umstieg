FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew :collector:bootJar --no-daemon

FROM eclipse-temurin:21.0.13_9-jre-noble
RUN mkdir -p /data/raw /data/archive && chown ubuntu:ubuntu /data/raw /data/archive
USER ubuntu
WORKDIR /app
COPY --from=build /src/services/collector/build/libs/collector.jar app.jar
ENV COLLECTOR_DATA_DIR=/data/raw
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
