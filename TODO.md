# TODO

## Backend — missing API routes

These endpoints are needed for core records-management workflows but are not implemented yet. The web client cannot
fully function without them.

### Records

- Record get/create/update by id is implemented at `/api/records` and `/api/records/{id}`.

### Search

- `GET /api/search` (or equivalent) — cross-entity search backing the header SearchBar “All” mode (records, lists, and
  future entity types in one query). This endpoint will also be used for individual object searching.

### Users & auth

- `GET /api/users` — list or search users for administration (user get/create/update by id is implemented at `/api/user` and `/api/user/{id}`).
- `POST /api/auth/signup` — wire through `AuthService` (endpoint exists but returns `null`; see `AuthController` TODO).

### Locations (future)

- Location entity + CRUD/search API — the web SearchBar already exposes a “Location” filter, but no location concept
  exists in the backend yet. Define the domain model and routes before building UI.

### Plugins

- Allow plugins to create new database tables
