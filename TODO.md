# TODO

## Users & auth

- User hidden/sensitve properties are shown in the audit log
- Only create audit log for component registrations that actually made a change
- `POST /api/auth/signup` — wire through `AuthService` (endpoint exists but returns `null`; see `AuthController` TODO).

## Relationships

- Improve relationships system to include Record->Record, Location->Record

## Permissions System

System for assigning permissions to explicit locations

## User types

Implement same as record types
