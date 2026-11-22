# Mendel Java Challenge

REST API built with **Spring Boot 4.2** on **Java 25 (LTS)**, Maven, and an in-memory **H2** database.

## Requirements

- JDK 25 (`java -version`). Maven is **not** required; the Maven Wrapper (`./mvnw`) downloads it.

## Commands

| Task | Command |
|---|---|
| Run the server (http://localhost:8080) | `./mvnw spring-boot:run` |
| Run the test suite | `./mvnw test` |
| Build an executable jar | `./mvnw clean package` then `java -jar target/challenge-0.0.1-SNAPSHOT.jar` |

On Windows (outside WSL), use `mvnw.cmd` instead of `./mvnw`.

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

## Useful URLs (while running)

- `GET/POST /api/items`, `GET /api/items/{id}`: example endpoints
- `/actuator/health`: health check
- `/h2-console`: database console (JDBC URL `jdbc:h2:mem:challengedb`, user `sa`, empty password)

## Configuration

- `src/main/resources/application.properties`: app config (port, datasource, JPA, actuator)
- `src/test/resources/application-test.properties`: overrides for tests, activated with `@ActiveProfiles("test")`

The database is in memory: all data is lost when the app stops.
