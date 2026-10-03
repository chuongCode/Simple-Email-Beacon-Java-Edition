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
