# Java Integration Sandbox Template

Reusable GitHub template repository for local Java/Spring Boot integration learning and experiments.

This template is for:

- Starting from a clean Spring Boot base with Java 17, Gradle, tests, and devcontainer support.
- Creating focused sandboxes for Kafka, PostgreSQL, LocalStack/AWS mocks, Redis, Testcontainers, ETL mapping, and other integrations.
- Keeping a repeatable devcontainer setup for local sandbox work.

This template is not for:

- Coding-challenge jar submissions.
- A complete demo application.
- A prebuilt integration demo for any specific technology.
- Production-ready infrastructure, security, or cloud setup.

## Open In VS Code Dev Containers

1. Install Docker Desktop or another local Docker runtime.
2. Install the VS Code Dev Containers extension.
3. Open this repository in VS Code.
4. Run `Dev Containers: Reopen in Container`.

The devcontainer uses Java 17 and mounts your host `.codex` directory plus your `.ssh` directory. The `.ssh` mount is read-only.

## Commands

Run the smoke test:

```bash
./gradlew test
```

Run the Spring Boot app:

```bash
./gradlew bootRun
```

Validate the placeholder Docker Compose file:

```bash
docker compose config
```

Use `docker-compose.yml` as the place to add local infrastructure later, for example Kafka, PostgreSQL, Redis, or LocalStack.

## Teacher-Agent Rule

When using Codex for learning, ask it to explain, ask questions, give small tasks, and review your code. Do not ask it to implement the whole learning project for you.

## Suggested First Exercise

Turn this base template into a focused sandbox:

- Pick one integration technology.
- Add only the dependencies and containers you need.
- Keep the first exercise intentionally small.
- Ask Codex to explain each step rather than generating the whole project at once.
