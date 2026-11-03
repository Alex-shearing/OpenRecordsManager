# TODO

## Users & auth

- User hidden/sensitve properties are shown in the audit log
- Only create audit log for component registrations that actually made a change
- `POST /api/auth/signup` — wire through `AuthService` (endpoint exists but returns `null`; see `AuthController` TODO).

## Permissions System

System for assigning permissions to explicit locations

## Investigate deployment strategy

Maybe go back to the old reverse-proxy with two separate processes strategy. This would also allow an nginx
reverse-proxy to enable HTTP/3.
We still need to support the deployment methodology of non-containerized too, preferably with a single installer/zip
that can be used on Windows and Linux.