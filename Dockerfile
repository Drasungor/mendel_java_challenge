# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1 - deps: JDK + Maven Wrapper + all project dependencies.
# Only re-runs when pom.xml or the wrapper changes, so code edits rebuild fast.
# =============================================================================
FROM eclipse-temurin:25-jdk-alpine AS deps
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Strip possible Windows line endings and make the wrapper executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
 && ./mvnw -B -q dependency:go-offline

COPY src/ src/

# =============================================================================
# Stage 2 - test: runs the test suite when the container starts.
#   docker compose run --rm test
# =============================================================================
FROM deps AS test
# Pre-compile main + test code so the container starts straight into running tests
RUN ./mvnw -B -q -o test-compile
# -o (offline): everything needed is already in the image, nothing is downloaded at run time
CMD ["./mvnw", "-B", "-o", "test"]

# =============================================================================
# Stage 3 - build: compile + package the executable jar, then split it into
# layers (dependencies change rarely, application code often) for better
# image-layer caching on rebuilds/pushes.
# =============================================================================
FROM deps AS build
RUN ./mvnw -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# =============================================================================
# Stage 4 - runtime: minimal JRE only (no JDK, no Maven, no sources).
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Run as an unprivileged user, never as root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Least-frequently-changing layers first
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# JVM is container-aware: size the heap from the container memory limit
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# exec form: the JVM is PID 1 and receives SIGTERM for graceful shutdown
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
