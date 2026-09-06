# Java Kafka Sandbox

Reusable Spring Boot sandbox repository for learning Kafka locally with a clean, devcontainer-friendly setup.

The long-term goal is an interactive Kafka scenario lab: instead of only reading
theory, a Java developer can run focused experiments, observe Kafka behavior, and
refresh interview-ready mental models. See `PRODUCT_VISION.md` for the intended
learning experience and `KAFKA_100_PROBLEMS_AND_PATTERNS.md` for the scenario backlog.

Scenario notes:

- `docs/001-system-ready.md`

This sandbox is for:

- Practicing Kafka concepts with small Spring Boot exercises.
- Running one local Kafka broker for development and experiments.
- Keeping Java 17, Gradle wrapper, tests, and devcontainer support ready to use.

This sandbox is not currently:

- Coding-challenge jar submissions.
- A completed training application.
- Production-ready infrastructure, security, or cloud setup.
- A full event-driven architecture example out of the box.

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

Start local Kafka:

```bash
docker compose up -d kafka
```

Run the Spring Boot app:

```bash
./gradlew bootRun
```

Validate the Docker Compose file:

```bash
docker compose config
```

Kafka is exposed on `localhost:9092` for simple local learning. If the Spring app later runs in another container instead of inside the devcontainer, advertised listeners may need adjustment.

## Teacher-Agent Rule

When using Codex for learning, ask it to explain, ask questions, give small tasks, and review your code. Do not ask it to implement the whole learning project for you.

## Suggested First Exercise

Build a tiny end-to-end Kafka learning flow:

- Create a `TradeEvent` DTO.
- Add `POST /events/trade`.
- Publish accepted events with `KafkaTemplate`.
- Consume them with `@KafkaListener`.
- Add a simple in-memory idempotency check using `eventId`.
