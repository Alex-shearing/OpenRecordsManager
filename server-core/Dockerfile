# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Prepare the gradle environment
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

COPY gradlew ./
COPY gradle gradle

RUN chmod +x gradlew \
    && ./gradlew

# Prepare build
COPY settings.gradle build.gradle ./
COPY plugin-api plugin-api
COPY server-core server-core
COPY plugins plugins

RUN ./gradlew :server-core:bootJar :copyPlugins --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN mkdir -p /app/data /app/plugins /app/logs

COPY --from=build /workspace/server-core/build/libs/server-core-*.jar /app/server-core.jar
COPY --from=build /workspace/server-core/plugins/ /app/plugins/
COPY server-core/config.yml /app/config.yml

ENV SERVER_PLUGINS_DIRECTORY=/app/plugins \
    SERVER_DATABASE_PRIMARY_URL=jdbc:sqlite:/app/data/orm_system.db \
    SERVER_DATABASE_READ_ONLY_URL=jdbc:sqlite:/app/data/orm_system.db \
    APP_LOGGING_PATH=/app/logs/openrecordsmanager.log \
    SERVER_WEB_DIRECTORY=

EXPOSE 8080
VOLUME ["/app/data", "/app/plugins", "/app/logs"]

CMD ["java", "-jar", "/app/server-core.jar"]
