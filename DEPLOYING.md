# Deploying Open Records Manager

Deploying ORM takes two pieces shipped together in one distribution:

- **API** (Spring Boot JAR)
- **Web Client** (static files under `./static`, cohosted by the API)

## Quick start (Docker)

```bash
docker compose up --build
```

- App (UI + API): http://localhost:8080

## Quick start (no Docker)

1. Build the distribution: `./gradlew distZip`
2. Unpack `build/distributions/open-records-manager-*.zip`
3. Run with `./start.sh` or `.\start.ps1` (Java 25+)
4. Browse http://localhost:8080 — the API serves `./static` same-origin

### PostgreSQL, MariaDB, or SQL Server (Docker)

Each database runs as an optional Compose profile. Pass the matching env file so the API connects to that service:

```bash
# PostgreSQL
docker compose --profile postgres --env-file docker/env/postgres.env up --build

# MariaDB
docker compose --profile mariadb --env-file docker/env/mariadb.env up --build

# SQL Server (SQL authentication; schema migrates into master for local dev)
docker compose --profile sqlserver --env-file docker/env/sqlserver.env up --build
```

Or set the same variables yourself (see commented examples in [`server-core/config.yml`](server-core/config.yml)):

```bash
export SERVER_DATABASE_PRIMARY_URL=jdbc:postgresql://localhost:5432/orm
export SERVER_DATABASE_PRIMARY_USERNAME=orm
export SERVER_DATABASE_PRIMARY_PASSWORD=orm
export SERVER_DATABASE_READ_ONLY_URL=
./gradlew bootRun
```

### Optional — Host the static files yourself

Copy the `static/` directory from the distribution and use the sample configs under `deploy/`:

- `deploy/orm-web.conf` — nginx + SPA fallback + `/api` proxy
- `deploy/web.config` — IIS URL Rewrite + ARR proxy

Set `server.web-directory=none` (or `SERVER_WEB_DIRECTORY=none`) on the API so it does not also serve the UI.

For a **same-origin** setup, proxy `/api` to the JAR and leave `apiBaseUrl` empty in
`index.html` (`window.__ORM_UI__`).

For a **cross-origin** setup, patch `apiBaseUrl` in `index.html` to the public API URL and allow the UI origin in
`app.security.cors` settings. The API exposes the CSRF token in the `X-CSRF-TOKEN` **response** header (also listed in
CORS exposed headers).

You can still place a `config.yml` next to the API process for non-Docker installs; env vars override it. Web branding
keys are **not** under `server.*`, so they can also be set centrally in the database via the config API.

Only the API URL is host-local. Branding always comes from the API (`GET /api/web`).

## Local development (without Docker)

```bash
# Terminal 1 — API
./gradlew bootRun

# Terminal 2 — UI (Vite proxies /api to :8080; index.html has empty apiBaseUrl)
cd server-web && npm run dev
```
