# Open Records Manager

## Contribution

## Building

To build sources locally, use the following instructions.

### Requirements

- Java 25 - Required to build/run `server-core` and all plugins
- Node.js and NPM - Required to build `server-web` (static SPA)

### Workflow

There are multiple tasks that can be used to assist in the development workflow:

- `gradlew runApp` / `gradlew bootRun` - runs the Spring Boot API (plugins included with `runApp`)
- `gradlew copyPlugins` - builds all the plugins and places them in the `server-core/data/plugins` directory
- `gradlew buildWeb` - builds the static web client (included under `static/` in `distZip`)
- `gradlew bootJar` / `gradlew distZip` - API jar; distribution zip also includes plugins and web client.
- `cd server-web && npm run dev` - SvelteKit UI in development mode (proxies `/api` to `:8080`)
- `cd server-web && npm run build` - production static SPA build
- `docker compose up --build` - runs the API container with embedded UI on `:8080` (see [`DEPLOYING.md`](DEPLOYING.md))

If you are editing primarily for the website, run `gradlew bootRun` on the parent project and
`npm run dev` in the `server-web` project.

### Configuration

You can configure the service in [`server-core/config.yml`](server-core/config.yml), environment variables or in the
database.
