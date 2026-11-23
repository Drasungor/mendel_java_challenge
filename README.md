# Mendel Java Challenge

REST API built with **Spring Boot 4.2** on **Java 25 (LTS)**, Maven, and an in-memory **H2** database.
Runs in Docker: no local Java or Maven installation required.

## Requirements

- [Docker](https://docs.docker.com/get-docker/) with Docker Compose v2 (`docker compose version`)
- `make` (preinstalled on most Linux/macOS systems; on Ubuntu/WSL: `sudo apt install make`)
- Only for the `local-*` commands: JDK 25 (`java -version`)

## Quick start (Makefile)

Run `make` (or `make help`) to list every command.

| Command | What it does |
|---|---|
| `make docker` | **Full Docker run**: runs the tests, then starts the server only if they pass |
| `make docker-server` | Builds and starts only the server in Docker (foreground, Ctrl+C to stop) |
| `make docker-server-bg` | Same, in the background |
| `make docker-test` | Builds and runs only the test suite in Docker |
| `make docker-logs` | Follows the logs of the background server |
| `make docker-stop` | Stops and removes the containers |
| `make docker-clean` | Stops the containers and deletes the project's Docker images |
| `make local-server` | Starts the server locally, without Docker (`./mvnw spring-boot:run`) |
| `make local-test` | Runs the tests locally, without Docker (`./mvnw test`) |
| `make local-clean` | Deletes the local build output (`target/`) |

The `local-*` commands first check that JDK 25+ is installed and print a clear error if not.

The sections below show the underlying commands, in case you prefer to run them directly.

## Running the server

```bash
docker compose up --build        # build the image and start the server (Ctrl+C to stop)
docker compose up --build -d     # same, in the background
docker compose logs -f app       # follow logs when running in the background
docker compose down              # stop and remove the container
```

The API is served at http://localhost:8080. Check it is up with:

```bash
curl http://localhost:8080/actuator/health     # {"status":"UP",...}
```

`--build` rebuilds the image so it includes your latest code changes. Only the layers
affected by a change are rebuilt: editing Java code does not re-download dependencies.

The database lives in memory, so **all data is lost when the container stops**. No volumes
are needed.

## Running the tests

```bash
docker compose run --rm --build test
```

This builds the `test` image (JDK + all dependencies, pre-fetched at build time) and runs
`./mvnw test` inside it fully offline. The command exits with Maven's status code (0 = all
tests passed), so it can be used as-is in CI. `--rm` removes the container afterwards.

The `test` service belongs to the `test` compose profile, so `docker compose up` never starts it.

## Docker setup

`Dockerfile` is a multi-stage build:

| Stage | Base image | Purpose |
|---|---|---|
| `deps` | `eclipse-temurin:25-jdk-alpine` | Maven Wrapper + all dependencies (cached until `pom.xml` changes) |
| `test` | `deps` | Pre-compiled code; runs the test suite as its command |
| `build` | `deps` | Packages the jar and splits it into Spring Boot layers |
| `runtime` | `eclipse-temurin:25-jre-alpine` | **Final server image**: JRE only, no JDK, Maven or sources |

The runtime image:
- runs as an unprivileged `spring` user, not root;
- sizes the JVM heap from the container memory limit (`-XX:MaxRAMPercentage=75`);
- has a `HEALTHCHECK` against `/actuator/health` (shown in `docker ps`);
- shuts down gracefully: in-flight requests finish when the container stops.

## Project layout

```
src/main/java/com/mendel/challenge/
├── ChallengeApplication.java   # entry point
├── controller/                 # HTTP layer: @RestController, request mapping, @Valid
├── service/                    # business logic, @Transactional boundaries
├── repository/                 # data layer: Spring Data JPA repositories
├── model/                      # JPA @Entity classes
├── dto/                        # data records exchanged between layers (see below)
└── exception/                  # custom exceptions + global handler (problem+json errors)
```

Dependencies flow **controller → service → repository** and are wired by Spring through
**constructor injection** (final fields, single constructor, no `@Autowired` needed).

The DTOs keep HTTP details out of the service:

| DTO | Used by | Purpose |
|---|---|---|
| `TransactionRequest` | controller | HTTP request body, with its validation rules |
| `TransactionResponse` | controller | HTTP response body (the JSON clients receive) |
| `TransactionTypeListResponse` | controller | HTTP response body of `GET /types/{type}`: `{"ids": [...]}` |
| `ChildrenTransactionSumResponse` | controller | HTTP response body of `GET /sum/{id}`: `{"sum": ...}` |
| `TransactionDto` | controller ↔ service | The service's own input/output type |
| `UpsertResult` | controller ↔ service | Result of a create-or-replace: the `TransactionDto` + whether it was created |

The controller converts `TransactionRequest` → `TransactionDto` before calling the service, and
`TransactionDto` → `TransactionResponse` before replying. The service never references the
HTTP types, and the JPA entity (`Transaction`) never leaves the service.

### Data model

`Transaction` entity (`model/Transaction.java`), stored in the `transactions` table:

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` (Java `Long`) | Primary key, chosen by the client in the URL of `PUT /api/transactions/{id}` |
| `amount` | `REAL` (Java `float`) | Required |
| `type` | `VARCHAR(100)` (Java `String`) | Required, not blank, max 100 characters. Indexed (`idx_transactions_type`) for fast lookups by type |
| `parent_id` | `BIGINT`, nullable | Foreign key to `transactions.id` (the parent transaction). Indexed (`idx_transactions_parent_id`) for fast lookups of a transaction's children |

### Endpoints

| Method | Path | Body | Response |
|---|---|---|---|
| `PUT` | `/api/transactions/{id}` | `{"amount": 10.5, "type": "cars", "parentId": 123}` (`parentId` optional) | `201` + the transaction if it was created, `200` if an existing one was replaced |
| `GET` | `/api/transactions` | | `200` + list of transactions |
| `GET` | `/api/transactions/{id}` | | `200` + the transaction, or `404` |
| `GET` | `/api/transactions/types/{type}` | | `200` + the ids of all transactions with that type, e.g. `{"ids": [3, 10]}`. `{"ids": []}` if there are none |
| `GET` | `/api/transactions/sum/{id}` | | `200` + the sum of the amounts of the transaction's **direct children**, e.g. `{"sum": 9502.5}`. `{"sum": 0.0}` if it has no children, `404` if the transaction doesn't exist |

A transaction looks like `{"id": 1, "amount": 10.5, "type": "cars", "parentId": null}`.
`PUT` replaces **all** fields: omitting `parentId` on an existing transaction removes its parent.

Errors: `400` if `amount` is missing, `type` is missing, blank or longer than 100 characters,
`{id}` is not a number or is negative (PUT), `parentId` equals `{id}` (a transaction can't be its own parent), or the body is malformed; `404` if `parentId` doesn't exist (or, for
`GET`, if `{id}` doesn't exist).

Example:

```bash
curl -X PUT http://localhost:8080/api/transactions/10 \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000, "type": "cars"}'

curl http://localhost:8080/api/transactions

curl http://localhost:8080/api/transactions/types/cars

curl http://localhost:8080/api/transactions/sum/10
```

Type matching is exact and case-sensitive (`cars` ≠ `Cars`). Any type is accepted: an unknown
type returns `{"ids": []}`, not an error. Types with spaces or special characters must be URL-encoded
(`home office` → `/types/home%20office`).

Health check: `GET /actuator/health`.

## Configuration

- `src/main/resources/application.properties`: app config (port, datasource, JPA, actuator)
- `src/test/resources/application-test.properties`: overrides for tests, activated with `@ActiveProfiles("test")`

Any property can be overridden per environment through environment variables in
`compose.yaml`. For example, `server.port` becomes `SERVER_PORT`.

## Running without Docker (optional)

Requires JDK 25 locally (`java -version`). Maven is not needed; the wrapper downloads it.

```bash
./mvnw spring-boot:run    # start the server
./mvnw test               # run the tests
```

Running outside Docker also enables the H2 web console at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:challengedb`, user `sa`, empty password). The console is disabled in
Docker because H2 only accepts console connections from inside the container.
