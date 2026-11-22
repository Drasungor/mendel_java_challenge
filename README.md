# Mendel Java Challenge

REST API built with **Spring Boot 4.2** on **Java 25 (LTS)**, Maven, and an in-memory **H2** database.
Runs in Docker: no local Java or Maven installation required.

## Requirements

- [Docker](https://docs.docker.com/get-docker/) with Docker Compose v2 (`docker compose version`)

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
├── dto/                        # request/response records exposed by the API
└── exception/                  # custom exceptions + global handler (problem+json errors)
```

Dependencies flow **controller → service → repository** and are wired by Spring through
**constructor injection** (final fields, single constructor, no `@Autowired` needed).

`Item` is an example resource that goes through every layer. Replace it with your own domain.

### Endpoints

- `GET /api/items`, `POST /api/items`, `GET /api/items/{id}`: example endpoints
- `GET /actuator/health`: health check

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
