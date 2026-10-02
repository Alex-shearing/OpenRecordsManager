# Contributing

## Vocabulary

### Extension model

- **Plugin** — `ServiceLoader` extension (JAR / `plugin.json`) that registers components with the host via
  `RegistrationContext`.
- **Component** — anything a plugin or builtin code provides: auth providers, config keys, templates, actions, search
  fields, file stores, etc.
- **ComponentType** — kind tag for a component (`object_property`, `record_type`, `file_store`,
  `search_field_provider`, …).
- **ResourceIdentifier** — stable `source:item` id (`source` = plugin id, `item` = local name), e.g. `builtin:title`,
  `auth_local:local_auth`.
- **ComponentReference** — typed handle to a component by type + id (or an inline value); used for dependencies and
  assignments.
- **Component Catalog** (`ComponentCatalog` / `catalog`) — in-memory registries of loaded components, keyed by
  `ComponentType` and `ResourceIdentifier`. Distinct from persisted data.
- **TemplateComponent** — JSON-shaped, registerable definition (`ObjectPropertyTemplate`, `RecordTypeTemplate`,
  `ListTemplate`, …).
- **Registration** — copying a template (and optionally its dependencies) from the catalog into persisted entities.

### Domain data

- **Data Repository** (`DataRepository` / `repository`) — facade over persisted domain entities (users, records,
  properties, plugins, …). Not the component catalog.
- **ObjectProperty** — field definition for users/records (typed by **PropertyType**). May be builtin (column-backed) or
  dynamic (EAV / `*_property_value`).
- **BuiltinProperty** — column-backed property on a holder entity (`@BuiltinProperty`), still represented as an
  `ObjectProperty` in the catalog/DB.
- **ObjectPropertyHolder** — user or record value bag: builtin columns plus dynamic property values.
- **RecordType** — schema for records: which properties apply, optional security filter, content rules.
- **List / ListElement** — controlled vocabulary and its entries; referenced by list-typed object properties.
- **RecordAction / UserAction** — plugin-provided operations on a record or user (availability + typed input).

## REST controllers

Controller handler method names become OpenAPI `operationId`s. Name every handler with a **verb + noun** that uniquely
identifies the action across the API — do not use bare verbs like `get`, `create`, `update`, `search`, or `getAll`.

Good: `getLocation`, `searchGroups`, `createLocationRelationship`, `listLocationRelationshipTypes`.  
Bad: `get`, `create`, `update`, `search`, `getAll`, `listRelationships` (noun too generic when another controller may
share it).

Prefer camelCase (`getAuthProvider`, `createList`) over underscored prefixes unless an existing controller already
uses that style.

## Packages / nullness

Every Java package must have a `package-info.java` annotated with `@NullMarked` (`org.jspecify.annotations.NullMarked`)
so types default to non-null. Use `@Nullable` only where null is intentional.

```java
@NullMarked
package com.openrecordsmanager.example;

import org.jspecify.annotations.NullMarked;
```

## Testing

Prefer the lowest layer that still proves the behavior you care about. Do not climb into MockMvc (or the full web stack)
unless HTTP, security filters, cookies, or status-code mapping is the subject of the test.

### Layer choice

| Goal                                                                                     | Prefer                                                            | Avoid                                           |
|------------------------------------------------------------------------------------------|-------------------------------------------------------------------|-------------------------------------------------|
| Pure helpers / algorithms                                                                | JUnit unit test (`*Test`)                                         | `@SpringBootTest`                               |
| Domain / use-case behavior                                                               | Call the **service** (or shared executor) under `@SpringBootTest` | MockMvc around the same logic                   |
| Schema / metamodel / DB                                                                  | `@SpringBootTest` + repositories or factories                     | HTTP                                            |
| Auth login/logout/refresh, CSRF, redirects, upgrade gates, actuator, degraded HTTP modes | `@SpringBootTest` + `@AutoConfigureMockMvc`                       | Re-implementing the filter chain in a unit test |

**Service-first rule:** if the assertion is about create/update/search/merge/validation outcomes, call `UserService`,
`RecordService`, `PluginService`, `AuthService`, `ObjectSearchExecutor`, etc. directly. MockMvc adds auth headers, JSON
envelopes, and status mapping noise without improving coverage of that logic.

**MockMvc rule:** use it only when the test would be meaningless without the servlet/security stack (e.g. token cookies
cleared on logout, `503` + upgrade header from the schema gate, CSRF header requirements, degraded session mode on
`/api/user/me`).

Do **not** keep a full-boot MockMvc test that stubs the service just to assert JSON wiring. That pays Spring Boot cost
for almost nothing. Either test the service, or use a narrow web slice if you truly need controller serialization alone.

### Naming

- `*Test` — no Spring context, or only lightweight extensions (Mockito). Pure logic.
- `*IntegrationTest` — `@SpringBootTest`, real DB (usually SQLite via test support), and optionally MockMvc when HTTP is
  in scope.

Name the class after the behavior, not the HTTP verb (`UserSearchIntegrationTest`, not `PostUserSearchMockMvcTest`).

### Package layout

Place tests under the same package tree as the code under test:

- `com.openrecordsmanager.search.SearchWildcardTest` next to search helpers
- `com.openrecordsmanager.record.RecordSecurityFilterTest` next to `Record`

### Spring Boot cost

Use `@SpringBootTest` only when you need:

- JPA / Hibernate metamodel
- real JDBC / Flyway
- Spring Security filter chain
- component catalog + plugin loading

Otherwise, write a plain unit test. Examples that should stay unit-level: `SearchWildcard`, `SearchOperatorSupport`,
JSON schema helpers, path sanitizers.

### Shared fixtures

Reuse existing test support; do not add production APIs just for tests to use:

- [`SqliteTestSupport`](server-core/src/test/java/com/openrecordsmanager/database/SqliteTestSupport.java) — named
  in-memory primary DB per test class
- [`TestAuthTokens`](server-core/src/test/java/com/openrecordsmanager/auth/TestAuthTokens.java) — issue tokens when a
  test actually needs HTTP auth
- [`AuditTestSupport`](server-core/src/test/java/com/openrecordsmanager/audit/AuditTestSupport.java) — wrap service calls
  that need a capture-enabled `AuditContext` (see below)

Typical `@DynamicPropertySource` trio for server-core integration tests:

```java

@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    SqliteTestSupport.registerPrimaryMemoryDatabase(registry, MyIntegrationTest.class);
    registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
    registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
}
```

Add extra keys only when the scenario needs them (audit spool dir, custom plugins directory, read-only DB URL, etc.).

### Assert public behavior

- Drive searches through `ObjectSearchExecutor.searchIds` or domain `*Service.search` — not package-private merge/SQL
  helpers exposed for tests.
- Drive schemas through `ObjectSearchSchema.of` — not direct package-private column resolvers unless you are testing
  that type in its own package.
- Prefer exceptions from the service (`ResourceNotFoundException`, `ResourceInUseException`, `ApiException`)
  over HTTP status codes when the test is service-level.
- Do not add `installForTest`, package-visible hooks, or wider visibility solely so a test can reach internals.

### Audit context in service tests

Service methods annotated with `@RequiresAuditComment` still run the aspect under `@SpringBootTest`. When policies may
require a comment, wrap calls with [`AuditTestSupport`](server-core/src/test/java/com/openrecordsmanager/audit/AuditTestSupport.java):

```java
UserResponse created = AuditTestSupport.withAudit(admin, () -> userService.create(...));
```

Alternatively disable comment-required on the relevant audit policies in `@BeforeEach` when the test is not about audit
comments.

### What MockMvc suites should look like

When HTTP **is** the subject:

```java

@SpringBootTest
@AutoConfigureMockMvc
class AuthLogoutIntegrationTest {
    // DynamicPropertySource trio…
    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestAuthTokens testAuthTokens;
    // assert cookies, status, headers — not business permutations already covered at service level
}
```

Keep permutations (many filter combinations, property shapes, merge modes) out of these classes; put them on the
service/executor.

### Checklist before adding a test

1. Can this be a unit test with no Spring? If yes, stop there.
2. If it needs DB/Spring, can I call a service/executor and assert return values / DB state?
3. Only if the failure mode is HTTP/security/filter-specific, add MockMvc.
4. Same package as production code; correct `*Test` / `*IntegrationTest` suffix.
5. No new production visibility or test-only hooks.
6. Reuse `SqliteTestSupport` / `TestAuthTokens` / `AuditTestSupport` instead of ad-hoc boot or audit wiring.
