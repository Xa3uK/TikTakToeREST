# Tic Tac Toe API

A multiplayer Tic Tac Toe game over REST. Two players register, join matchmaking, and take turns making moves. The server tracks game state and detects wins and draws.

## Tech Stack

- **Kotlin 2.2** + **Spring Boot 4.0** (Spring MVC)
- **Spring Data JDBC** + **PostgreSQL** — persistence with Flyway migrations
- **BCrypt** (`spring-security-crypto`) — password hashing
- **springdoc-openapi 2.8.6** — Swagger UI at `/swagger-ui.html`
- **Docker Compose** — local setup with one command

## Running Locally

**Prerequisites:** Docker

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

To run without Docker, start PostgreSQL separately and configure `application.yml`, then:

```bash
./gradlew bootRun
```

## API Overview

### Players

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/players` | Register a new player → 201 |
| `GET` | `/api/v1/players/{playerId}/games` | List all games for a player → 200 |

### Games

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/games/join` | Join matchmaking — creates or joins a game → 200 |
| `GET` | `/api/v1/games/{gameId}` | Get game state (add `?playerId=` for personal context) → 200 |
| `POST` | `/api/v1/games/{gameId}/moves` | Make a move → 200 |

### Example flow

```bash
# Register two players
curl -X POST http://localhost:8080/api/v1/players \
  -H 'Content-Type: application/json' \
  -d '{"username": "alice", "password": "secret"}'
# → {"playerId": 1, "username": "alice"}

curl -X POST http://localhost:8080/api/v1/players \
  -H 'Content-Type: application/json' \
  -d '{"username": "bob", "password": "secret"}'
# → {"playerId": 2, "username": "bob"}

# Alice joins matchmaking → WAITING game created
curl -X POST http://localhost:8080/api/v1/games/join \
  -H 'Content-Type: application/json' \
  -d '{"playerId": 1, "password": "secret"}'

# Bob joins matchmaking → game transitions to IN_PROGRESS
curl -X POST http://localhost:8080/api/v1/games/join \
  -H 'Content-Type: application/json' \
  -d '{"playerId": 2, "password": "secret"}'
# → {"gameId": 1, "status": "IN_PROGRESS", "yourSymbol": "O", "yourTurn": false, ...}

# Alice (X) makes the first move
curl -X POST http://localhost:8080/api/v1/games/1/moves \
  -H 'Content-Type: application/json' \
  -d '{"playerId": 1, "password": "secret", "row": 0, "col": 0}'
```

### Game response fields

| Field | Description |
|-------|-------------|
| `board` | 5-element list rendering the 3×3 grid with separators |
| `yourSymbol` | `"X"` or `"O"` — present when `playerId` is provided |
| `yourTurn` | `true` if it is your turn — present when `playerId` is provided |
| `nextTurn` | Symbol of the player who moves next (spectator-friendly) |
| `availableMoves` | List of `{row, col}` objects — present only on your turn |
| `status` | `WAITING` / `IN_PROGRESS` / `FINISHED` |
| `winner` | `"X"`, `"O"`, or `null` (draw) — present when `FINISHED` |

## Authentication

Each move request requires the player's ID and password. There are no sessions or tokens — credentials are verified per request using BCrypt.

## Concurrency

Games use **optimistic locking** (`@Version` column). If two requests update the same game simultaneously, one will receive `409 Conflict` with `{"error": "...retry"}`. The client should retry.

## Error Responses

All errors return `{"error": "<message>"}` with the appropriate HTTP status:

| Status | Cause |
|--------|-------|
| `400` | Username already taken |
| `401` | Wrong player ID or password |
| `404` | Player or game not found |
| `409` | Invalid move, game not in progress, wrong turn, cell occupied, or concurrent update conflict |

## Database

Flyway migrations in `src/main/resources/db/migration/`:

- `V1__create_tables.sql` — `players` and `games` tables
- `V2__add_updated_at_trigger.sql` — PostgreSQL trigger to auto-update `updated_at`

## Tests

```bash
./gradlew test
```

Three layers:

| Layer | Location | What it tests |
|-------|----------|---------------|
| Unit | `unit/service/` | Service logic with MockK mocks — no Spring context |
| Web | `web/controller/` | HTTP routes, status codes, JSON shape — MockMvc standaloneSetup |
| Integration | `integration/` | Full stack against a real PostgreSQL (Testcontainers) |
