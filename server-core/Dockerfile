# syntax=docker/dockerfile:1

# -----------------------------------------------------------------------------
# web: build the static SPA (cached independently of Java sources)
# -----------------------------------------------------------------------------
FROM node:22-bookworm AS web
WORKDIR /web

COPY server-web/package.json server-web/package-lock.json server-web/.npmrc ./
RUN npm ci --legacy-peer-deps

COPY server-web/vite.config.ts server-web/tsconfig.json server-web/openapi-ts.config.ts ./
COPY server-web/prettier.config.js server-web/.prettierignore ./
COPY server-web/src src
COPY server-web/static static
RUN npm run build

# -----------------------------------------------------------------------------
# api: build the Spring Boot jar + plugin jars (no Node)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS api
WORKDIR /workspace

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

COPY gradlew ./
COPY gradle gradle

RUN chmod +x gradlew \
    && ./gradlew

COPY settings.gradle build.gradle ./
COPY plugin-api plugin-api
COPY plugins plugins
COPY server-core server-core

RUN ./gradlew :server-core:bootJar :copyPlugins --no-daemon -x test

# -----------------------------------------------------------------------------
# runtime: dist layout (jar, plugins, scripts, static, deploy) + JRE
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/data

COPY --from=api /workspace/server-core/build/libs/server-core-*.jar ./
COPY --from=api /workspace/server-core/plugins/ ./plugins/
COPY --from=api /workspace/server-core/config.yml ./
COPY --from=web /web/build/ ./static/
COPY scripts/start.sh ./

RUN chmod +x ./start.sh

ENV SERVER_PLUGINS_DIRECTORY=/app/plugins \
    SERVER_WEB_DIRECTORY=/app/static \
    APP_LOGGING_PATH=/app/data/logs/openrecordsmanager.log

EXPOSE 8080
VOLUME ["/app/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/api/health/liveness || exit 1

CMD ["./start.sh"]
