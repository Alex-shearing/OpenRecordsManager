# Open Records Manager

## Contribution

## Building

To build sources locally, use the following instructions.

### Requirements

- Java 25 - Required to build/run `server-core` and all plugins
- Node.js and NPM - Required to build/run `server-web` and `server-core` (not required when just running API using
  `bootRun`)

### Workflow

There are multiple tasks that can be used to assist in the development workflow:

- `gradlew runApp` - runs the whole app (Node.js required), this includes:
    - `server-core` - the base server
    - `server-web` - the website
    - all plugins included in the `plugins` directory
- `gradlew bootRun` - runs just the SpringBoot application, no website included
- `gradlew buildWeb` - builds the website to static sources, primarily used by other build tasks
- `gradlew copyPlugins` - builds all the plugins and places them in the `server-core/plugins` directory
- `cd server-web && npm run dev` - runs the Svelte website in development mode (reloads sources on change)

If you are editing primarily for the website, run `gradlew bootRun` on the parent project and `npm run dev` in
the `server-web` project.  
If you are editing primarily the `server-core` or working over both, use the `gradlew runApp` task and rerun on changes.
