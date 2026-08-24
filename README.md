# Open Records Manager

## Contribution

## Building

To build sources locally, use the following instructions.

### Requirements

- Java 25 - Required to build/run `server-core` and all plugins
- Node.js and NPM - Required to build/run `server-web`

### Workflow

There are multiple tasks that can be used to assist in the development workflow:

- `gradlew runApp` / `gradlew bootRun` - runs the Spring Boot API (plugins included with `runApp`)
- `gradlew copyPlugins` - builds all the plugins and places them in the `server-core/plugins` directory
- `cd server-web && UI_API_BASE_URL=http://localhost:8080 npm run dev` - runs the SvelteKit UI in development mode
- `cd server-web && npm run build` - production Node build (used by the web Docker image)
- `docker compose up --build` - runs API + web containers (see [`DEPLOYING.md`](DEPLOYING.md))

If you are editing primarily for the website, run `gradlew bootRun` on the parent project and
`UI_API_BASE_URL=http://localhost:8080 npm run dev` in the `server-web` project.

### API configuration

- [`server-core/src/main/resources/application.properties`](server-core/src/main/resources/application.properties) —
  Spring Boot wiring; imports `./config.yml`
- [`server-core/config.yml`](server-core/config.yml) — application settings (edit / ship this file next to the process)
