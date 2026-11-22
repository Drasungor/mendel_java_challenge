# Shortcuts for the different ways of running the project.
# Run `make` or `make help` to list them.

COMPOSE := docker compose
MVNW    := ./mvnw

.DEFAULT_GOAL := help
.PHONY: help \
	docker docker-server docker-server-bg docker-test docker-logs docker-stop docker-clean \
	local-server local-test local-clean check-java

## ---------- Docker ----------

docker: ## Full Docker run: run the tests, then start the server only if they pass
	$(MAKE) docker-test
	$(MAKE) docker-server

docker-server: ## Build and start the server in Docker (foreground, Ctrl+C to stop)
	$(COMPOSE) up --build app

docker-server-bg: ## Build and start the server in Docker in the background
	$(COMPOSE) up --build -d app

docker-test: ## Build and run the test suite in Docker
	$(COMPOSE) run --rm --build test

docker-logs: ## Follow the logs of the background server
	$(COMPOSE) logs -f app

docker-stop: ## Stop and remove the Docker containers
	$(COMPOSE) --profile test down

docker-clean: ## Stop containers and delete the project's Docker images
	$(COMPOSE) --profile test down --rmi local

## ---------- Local (no Docker, requires JDK 25) ----------

local-server: check-java ## Start the server locally with the Maven Wrapper
	$(MVNW) spring-boot:run

local-test: check-java ## Run the test suite locally with the Maven Wrapper
	$(MVNW) test

local-clean: ## Delete local build output (target/)
	$(MVNW) -q clean

# Fails early with a clear message instead of Maven's "release version 25 not supported"
check-java:
	@java -version >/dev/null 2>&1 || { echo "Error: java not found. Install JDK 25 or use the docker-* targets."; exit 1; }
	@v=$$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'); \
	if [ "$$v" -lt 25 ]; then \
		echo "Error: JDK 25+ required, found Java $$v. Install JDK 25 or use the docker-* targets."; exit 1; \
	fi

## ---------- Help ----------

help: ## List available commands
	@echo "Usage: make <target>"
	@awk 'BEGIN {FS = ":.*## "} \
		/^## / { printf "\n\033[1m%s\033[0m\n", substr($$0, 4) } \
		/^[a-zA-Z_-]+:.*## / { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
