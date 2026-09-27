# TODO

## Users & auth

- User hidden/sensitve properties are shown in the audit log
- Only create audit log for component registrations that actually made a change
- `POST /api/auth/signup` — wire through `AuthService` (endpoint exists but returns `null`; see `AuthController` TODO).

## Locations (future)

- Location entity (possibly just a record extension?) + CRUD/search API — the web SearchBar already exposes a “Location”
  filter, but no location concept exists in the backend yet. Define the domain model and routes before building UI.

## Translations

Remove hardcoded string names/descriptions from plugins and buitlin, do it as an id to translation map in resources

## Cleanup Tests

## User types

Implement same as record types