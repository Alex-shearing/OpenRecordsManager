# TODO

## Users & auth

- User hidden/sensitve properties are shown in the audit log
- Only create audit log for component registrations that actually made a change
- `POST /api/auth/signup` — wire through `AuthService` (endpoint exists but returns `null`; see `AuthController` TODO).

## Relationships

- Improve relationships system to include Record->Record, Location->Record

## Permissions System

System for assigning permissions to explicit locations

## Improve Initial Database setup

Make user navigate to /setup and force to set an admin password rather than using a known default password

## Investigate deployment strategy

Maybe go back to the old reverse-proxy with two separate processes strategy. This would also allow an nginx
reverse-proxy to enable HTTP/3.