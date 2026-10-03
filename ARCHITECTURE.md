# Architecture

## Project layout

```text
frontend/                         Existing React frontend
src/main/java/.../config          Runtime and CORS configuration
src/main/java/.../repository      SQLite access with JdbcClient
src/main/java/.../service         Beacon and request-metadata logic
src/main/java/.../web             Legacy-compatible and JSON API controllers
src/main/resources/db/migration   Flyway schema migrations
src/test                         End-to-end integration tests
```

## Existing API compatibility

| Endpoint                          | Behavior                                                                |
| --------------------------------- | ----------------------------------------------------------------------- |
| `GET /generateUUID`               | Creates a tracking record and returns its full pixel URL as plain text. |
| `GET /emailBeacon?UUID=...`       | Records a valid beacon visit and always returns a no-cache 1×1 GIF.     |
| `GET /emailBeaconStatus?UUID=...` | Returns `Unread...` or the legacy-shaped visit list.                    |

The dashboard uses the consolidated JSON API:

| Endpoint                              | Behavior                                                       |
| ------------------------------------- | -------------------------------------------------------------- |
| `GET /api/beacons`                    | Lists saved beacons with status summaries.                     |
| `POST /api/beacons`                   | Creates a beacon.                                              |
| `GET /api/beacons/{uuid}`             | Returns one beacon and its status summary.                     |
| `PATCH /api/beacons/{uuid}`           | Updates a beacon name.                                         |
| `DELETE /api/beacons/{uuid}`          | Deletes a beacon and all of its visits.                        |
| `GET /api/beacons/{uuid}/visits`      | Lists the recorded visits for a beacon.                        |
| `POST /api/beacons/{uuid}/test-visit` | Records a labeled test load without affecting estimated opens. |

## Move an existing SQLite database

The Flyway migration deliberately uses the original tables (`trackingLinks` and `linkVisits`). To carry existing data forward:

1. Stop the old Node process so SQLite is no longer being written.
2. Copy its `database.db` into this repository as `email-beacon.db`, or point `BEACON_DB_PATH` at an absolute copy.
3. Start the Spring application. Flyway baselines the non-empty database at version 0, applies the compatible indexes, and keeps existing rows.
4. Keep the original database backup until the generated, hit, and status flows have been checked.

Do not run the Node and Spring applications against the same SQLite file at the same time.

## Configuration

| Environment variable                  | Default                                       | Purpose                                                                                       |
| ------------------------------------- | --------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `PORT`                                | `8080`                                        | HTTP port.                                                                                    |
| `BEACON_DB_PATH`                      | `./email-beacon.db`                           | SQLite file path.                                                                             |
| `BEACON_PUBLIC_BASE_URL`              | `http://localhost:8080`                       | Public origin placed into generated tracking URLs. Set this in deployment.                    |
| `BEACON_ALLOWED_ORIGINS`              | `http://localhost:5173,http://localhost:3000` | Comma-separated origins allowed during separate frontend development.                         |
| `BEACON_TRUST_FORWARDED_HEADERS`      | `false`                                       | Uses the first `X-Forwarded-For` value only when deployment has a trusted reverse proxy.      |
| `BEACON_DEDUPLICATION_WINDOW_SECONDS` | `60`                                          | Time window in which the same visitor fingerprint is labeled as a duplicate.                  |
| `BEACON_VISITOR_HASH_SALT`            | local-only placeholder                        | Secret used to hash the IP/user-agent fingerprint. Set a stable, private value in deployment. |

Request metadata currently stores the IP address, user agent, referrer, and accepted language. The old `geoip-lite` lookup is not silently replaced with a network service; adding an explicitly configured local GeoLite database is a follow-up so deployments can make the privacy and data-source choice themselves.
