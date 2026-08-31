# TODO

## Search

- `GET /api/search` (or equivalent) — cross-entity search backing the header SearchBar “All” mode (records, lists, and
  future entity types in one query). This endpoint will also be used for individual object searching.

## Users & auth

- User hidden/sensitve properties are shown in the audit log
- Only create audit log for component registrations that actually made a change
- `POST /api/auth/signup` — wire through `AuthService` (endpoint exists but returns `null`; see `AuthController` TODO).

## Locations (future)

- Location entity (possibly just a record extension?) + CRUD/search API — the web SearchBar already exposes a “Location”
  filter, but no location concept exists in the backend yet. Define the domain model and routes before building UI.
