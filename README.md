# Simple Email Beacon — Spring

This repository consolidates the original [Node/Express backend](https://github.com/chuongCode/Simple-Email-Beacon) and [React frontend](https://github.com/cszach/simple-web-beacon-frontend) into one Spring Boot application.

The React interface is intentionally unchanged. Maven builds it and places it inside the Spring Boot JAR, so the UI and API are served from the same process and origin.

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC and `JdbcClient`
- SQLite with Flyway migrations
- React 18 / Create React App (carried over from the frontend repository)

`JdbcClient` is used instead of JPA so the Spring implementation can keep the original SQLite table and column names. That makes an existing `database.db` straightforward to carry forward.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the project layout.

## Run the complete application

Install a Java 21 JDK, then run:

```bash
./mvnw clean package
java -jar target/email-beacon-0.1.0-SNAPSHOT.jar
```

Open <http://localhost:8080>. The Maven build downloads its own Maven, Node, and npm distributions; only Java is required on the host.

The dashboard loads saved beacons from SQLite, refreshes their status every 15 seconds, and lets you rename or delete them. Use **Copy URL** for the raw tracking URL, **Copy HTML** for a ready-to-paste image tag, **Test hit** to verify tracking without changing the estimated-open count, and **Visits** to inspect recorded loads.

### How tracking accuracy works

Every image request is retained as a raw load, then labeled as likely human, mail proxy, automated, test, or unknown. Requests from the same hashed IP-address/user-agent combination inside the deduplication window are also marked as duplicates. The dashboard uses those signals to show:

- **Total loads:** every request received by the pixel endpoint.
- **Estimated unique opens:** distinct visitors after excluding known automated and test loads.
- **Likely human:** loads whose user agent looks like a normal browser or mail client.
- **Mail proxy:** loads identified as an email provider's image proxy.
- **Automated:** recognizable bots, scanners, previews, and command-line clients.
- **Duplicate:** a repeated visitor fingerprint during the configured time window.
- **Test:** loads created with the dashboard's test action.

These are transparent heuristics, not proof that a particular person read an email. Mail providers can prefetch or proxy images, privacy features can hide the recipient's real address and device, and some clients block remote images entirely. The app therefore preserves raw counts and labels uncertain traffic instead of presenting every pixel request as a confirmed human open.

For backend-only development, skip the React build:

```bash
./mvnw -Dskip.frontend=true spring-boot:run
```

For React live reload, run the backend command above and then:

```bash
cd frontend
npm ci
npm start
```

The development server proxies API calls to Spring on port 8080.

## Existing API compatibility

| Endpoint | Behavior |
| --- | --- |
| `GET /generateUUID` | Creates a tracking record and returns its full pixel URL as plain text. |
| `GET /emailBeacon?UUID=...` | Records a valid beacon visit and always returns a no-cache 1×1 GIF. |
| `GET /emailBeaconStatus?UUID=...` | Returns `Unread...` or the legacy-shaped visit list. |

The dashboard uses the consolidated JSON API:

| Endpoint | Behavior |
| --- | --- |
| `GET /api/beacons` | Lists saved beacons with status summaries. |
| `POST /api/beacons` | Creates a beacon. |
| `GET /api/beacons/{uuid}` | Returns one beacon and its status summary. |
| `PATCH /api/beacons/{uuid}` | Updates a beacon name. |
| `DELETE /api/beacons/{uuid}` | Deletes a beacon and all of its visits. |
| `GET /api/beacons/{uuid}/visits` | Lists the recorded visits for a beacon. |
| `POST /api/beacons/{uuid}/test-visit` | Records a labeled test load without affecting estimated opens. |

## Move an existing SQLite database

The Flyway migration deliberately uses the original tables (`trackingLinks` and `linkVisits`). To carry existing data forward:

1. Stop the old Node process so SQLite is no longer being written.
2. Copy its `database.db` into this repository as `email-beacon.db`, or point `BEACON_DB_PATH` at an absolute copy.
3. Start the Spring application. Flyway baselines the non-empty database at version 0, applies the compatible indexes, and keeps existing rows.
4. Keep the original database backup until the generated, hit, and status flows have been checked.

Do not run the Node and Spring applications against the same SQLite file at the same time.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port. |
| `BEACON_DB_PATH` | `./email-beacon.db` | SQLite file path. |
| `BEACON_PUBLIC_BASE_URL` | `http://localhost:8080` | Public origin placed into generated tracking URLs. Set this in deployment. |
| `BEACON_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated origins allowed during separate frontend development. |
| `BEACON_TRUST_FORWARDED_HEADERS` | `false` | Uses the first `X-Forwarded-For` value only when deployment has a trusted reverse proxy. |
| `BEACON_DEDUPLICATION_WINDOW_SECONDS` | `60` | Time window in which the same visitor fingerprint is labeled as a duplicate. |
| `BEACON_VISITOR_HASH_SALT` | local-only placeholder | Secret used to hash the IP/user-agent fingerprint. Set a stable, private value in deployment. |

Request metadata currently stores the IP address, user agent, referrer, and accepted language. The old `geoip-lite` lookup is not silently replaced with a network service; adding an explicitly configured local GeoLite database is a follow-up so deployments can make the privacy and data-source choice themselves.

## Verification

```bash
./mvnw test -Dskip.frontend=true
```

The integration suite covers generation, unread status, tracking-pixel delivery, visit persistence, load classification, duplicate detection, test loads, list/create JSON flows, and the default refusal to trust spoofable forwarded-IP headers.
