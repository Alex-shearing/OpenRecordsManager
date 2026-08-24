# Deploying Open Records Manager

Self-hosting uses two containers: **api** (Spring Boot) and **web** (SvelteKit / Node).

## Quick start

```bash
docker compose up --build
```

- Web UI: http://localhost:3000
- API: http://localhost:8080

## Configuration

Configure both services with environment variables in [`docker-compose.yml`](docker-compose.yml) (or your orchestrator).

### API (Spring Boot relaxed binding)

| Variable                               | Purpose                                     |
|----------------------------------------|---------------------------------------------|
| `SERVER_DATABASE_PRIMARY_URL`          | JDBC URL for the primary database           |
| `SERVER_DATABASE_READ_ONLY_URL`        | JDBC URL for read replica (optional)        |
| `SERVER_PLUGINS_DIRECTORY`             | Plugin jar directory                        |
| `APP_SECURITY_COOKIE_SECURE`           | `true` behind HTTPS; `false` for local HTTP |
| `APP_SECURITY_CORS_ALLOWED_ORIGINS`    | Comma-separated web origins                 |
| `APP_SECURITY_CORS_ALLOWED_HEADERS`    | Comma-separated request headers             |
| `APP_LOGGING_PATH`                     | Log file path                               |
| `AUTH_AUTH_LOCAL_ENABLE_DEFAULT_ADMIN` | Enable default local admin                  |

You can still place a `config.yml` next to the API process for non-Docker installs; env vars override it.

### Web

| Variable           | Purpose                                                                                                    |
|--------------------|------------------------------------------------------------------------------------------------------------|
| `UI_API_BASE_URL`  | **Required.** Public API URL the browser calls (e.g. `http://localhost:8080` or `https://api.example.com`) |
| `UI_PRODUCT_NAME`  | Product name / logo text                                                                                   |
| `UI_LOGO_URL`      | Optional logo image URL                                                                                    |
| `UI_FAVICON_URL`   | Optional favicon URL                                                                                       |
| `UI_PRIMARY_COLOR` | Brand color (CSS)                                                                                          |
| `UI_SUPPORT_URL`   | Optional support link                                                                                      |
| `ORIGIN`           | Public UI URL for SvelteKit (default in image: `http://localhost:3000`)                                    |

The web app always calls the API at `UI_API_BASE_URL` (cross-origin). Set `APP_SECURITY_CORS_ALLOWED_ORIGINS` to the web
origin. For production HTTPS, set `APP_SECURITY_COOKIE_SECURE=true`.

## Production notes

- Terminate TLS at a reverse proxy or load balancer.
- Set web `ORIGIN` to the public UI URL.
- Set `UI_API_BASE_URL` to the public API URL.
- Cross-origin auth cookies need `Secure` (HTTPS) in real browsers.

## Local development (without Docker)

```bash
# Terminal 1 — API
./gradlew bootRun

# Terminal 2 — UI (point the browser at the API)
cd server-web && UI_API_BASE_URL=http://localhost:8080 npm run dev
```
