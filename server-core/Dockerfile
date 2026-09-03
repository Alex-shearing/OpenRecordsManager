# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

# Node is required for Gradle's buildWeb task (included by distZip)
COPY --from=node:22-bookworm /usr/local /usr/local

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY gradlew ./
COPY gradle gradle

RUN chmod +x gradlew \
    && ./gradlew

COPY settings.gradle build.gradle ./
COPY scripts scripts
COPY plugin-api plugin-api
COPY server-web server-web
COPY plugins plugins
COPY server-core server-core

RUN ./gradlew distZip --no-daemon -x test \
    && mkdir -p /dist \
    && unzip -q build/distributions/open-records-manager-*.zip -d /dist \
    && mv /dist/open-records-manager-* /dist/app

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/data

COPY --from=build /dist/app/ /app/

RUN chmod +x /app/start.sh

ENV SERVER_PLUGINS_DIRECTORY=/app/plugins \
    SERVER_WEB_DIRECTORY=/app/static \
    APP_LOGGING_PATH=/app/data/logs/openrecordsmanager.log

EXPOSE 8080
VOLUME ["/app/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/api/health/liveness || exit 1

CMD ["./start.sh"]
