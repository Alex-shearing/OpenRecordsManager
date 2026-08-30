# Deploying Open Records Manager

Deploying ORM takes two components:

- **API** (Spring Boot JAR)
- **Web Client** (Static Web App, optionally deployed by the API)

## Quick start (Docker)

```bash
docker compose up --build
```

- Web UI: http://localhost:3000
- API: http://localhost:8080

## Quick start (no Docker)

1. Build distributions: `./gradlew distZip webDistZip`
2. Unpack `build/distributions/open-records-manager-*.zip` for the API
3. Run the API with `./start.sh` or `.\start.ps1` (Java 25+)
4. Host the UI from `build/distributions/orm-web-static-*.zip` using one of the options below

### Option A — Co-host UI from the API process

```bash
mkdir -p static
unzip orm-web-static-*.zip -d static
./start.sh
```

Browse http://localhost:8080. Same-origin API calls use the empty `apiBaseUrl` baked into `index.html`.

### Option B — Host the static files yourself

Unpack `orm-web-static-*.zip` into your web root and use the samples shipped inside that zip:

- `deploy/orm-web.conf` — nginx + SPA fallback + `/api` proxy
- `deploy/web.config` — IIS URL Rewrite + ARR proxy

For a **same-origin** setup, proxy `/api` to the JAR and leave `apiBaseUrl` empty in `index.html` (`window.__ORM_UI__`).

For a **cross-origin** setup, patch `apiBaseUrl` in `index.html` to the public API URL and allow the UI
origin in `app.security.cors` settings.

You can still place a `config.yml` next to the API process for non-Docker installs; env vars override it. Web branding
keys are **not** under `server.*`, so they can also be set centrally in the database via the config API.

Only the API URL is host-local. Branding always comes from the API (`GET /api/web`).

## Production notes

- Terminate TLS at a reverse proxy or load balancer.
- Prefer same-origin (API + UI behind one hostname) for simpler cookies.
- Cross-origin auth cookies need `Secure` (HTTPS) and matching CORS origins.

## Local development (without Docker)

```bash
# Terminal 1 — API
./gradlew bootRun

# Terminal 2 — UI (Vite proxies /api to :8080; index.html has empty apiBaseUrl)
cd server-web && npm run dev
```
