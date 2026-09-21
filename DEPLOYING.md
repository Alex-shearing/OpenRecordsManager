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

### Optional — Mock OIDC (local testing)

Start [Soluto oidc-server-mock](https://github.com/Soluto/oidc-server-mock) as an optional Compose
profile. Prefer running the ORM API on the host (`./gradlew bootRun`) so browser redirects and
server-side discovery both use `localhost`:

```bash
# Mock IdP only (recommended for OIDC testing with bootRun / npm run dev)
docker compose --profile oidc up -d
```

To bring up the mock together with the ORM API container instead:

```bash
docker compose --profile oidc up --build -d
```

The API service maps `localhost` to the Docker host (`extra_hosts`), so issuer
`http://localhost:4011` works for server-side discovery when the mock is published on host port 4011. Prefer that over the Compose service hostname unless discovery endpoint URLs match what the
API can reach.

Wire ORM to the mock:

1. In ORM, create an `oidc_auth` provider with settings (Compose defaults):

```json
{
  "clientId": "orm-local",
  "secret": "orm-local-secret",
  "uri": "http://localhost:4011",
  "scope": "openid profile",
  "usernameClaim": "preferred_username"
}
```

2. Set `MOCK_OIDC_REDIRECT_URI` to the exact callback for that provider —
   `http://localhost:8080/api/auth/callback/<orm-provider-uuid>` — then recreate the mock
   (`MOCK_OIDC_REDIRECT_URI=... docker compose --profile oidc up -d --force-recreate mock-oidc-server`).
   The mock requires an exact redirect URI (no wildcards).
3. Ensure `app.security.public-base-url` is `http://localhost:8080` (Compose API default, or set
   `APP_SECURITY_PUBLIC_BASE_URL`).
4. Create an ORM user whose **username** is `oidcuser` (or whatever `MOCK_OIDC_USERNAME` is) and
   set that user’s auth provider to the OIDC provider you created. The default `admin` local user
   will not work for OIDC login.
5. Sign in via the OIDC provider; the mock login UI is at http://localhost:4011  
   (defaults: `oidcuser` / `password`).

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
