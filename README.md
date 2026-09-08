# Java Kafka Sandbox

An interactive Kafka lab for Java developers. Run a scenario against a local Kafka
broker, inspect real partition assignments and record offsets, then replay its
visual timeline at your own pace.

The application includes **12 implemented scenarios**: Spring wiring, end-to-end
message delivery, partitions and offsets, message keys, ordering, consumer groups,
offset reset, rebalance, and consumer takeover. Scenario 001 checks isolated Spring bean
wiring with its Kafka listener stopped; scenarios 002–012 execute Kafka experiments.

Read the [scenario articles in Russian or English](docs/README.md), with links to
the implementation lines, or consult the [learning roadmap](KAFKA_100_PROBLEMS_AND_PATTERNS.md).

## Start the lab

The checked-in setup uses Java 17, the Gradle wrapper, Spring Boot 3.5.16, and one
Kafka 4.3.1 broker in KRaft mode. Versions come from [build.gradle](build.gradle)
and [docker-compose.yml](docker-compose.yml).

1. On the **Docker host**, start Kafka from the repository directory:

   ```bash
   docker compose up -d kafka
   docker compose logs -f kafka
   ```

   Wait for the broker to finish starting; Ctrl+C exits the log viewer without stopping Kafka.

2. Open the repository in VS Code and run **Dev Containers: Reopen in Container**.
   The [devcontainer configuration](.devcontainer/devcontainer.json) supplies Java 17,
   forwards port 8080, and mounts the host's `.codex`, read-only `.ssh`, and Gradle cache
   directories. Ensure the mount sources exist. Docker CLI/socket access is not
   configured inside this devcontainer; run Compose commands on the host.

3. In the devcontainer terminal, start the application:

   ```bash
   ./gradlew bootRun
   ```

4. Open [http://localhost:8080](http://localhost:8080). Select a scenario and press
   **Play**. Start with `001 System Ready`, then `002 Trade Event Flow`.

### Kafka networking

Both [application.yml](src/main/resources/application.yml) and the broker's advertised
listener use `host.docker.internal:9092`. That name must resolve to the Docker host
from the Java process, including the nested scenario applications. The Compose
port is published as 9092, but the broker still advertises `host.docker.internal`.

On a Linux Docker setup that does not provide this name automatically, configure
host-gateway resolution for the devcontainer. For a host-only Java setup, either
make this name resolve to the host or change **both** the broker's advertised
listener and Spring's bootstrap servers to an address reachable from that Java
process. Changing only the bootstrap address does not change the address returned
in Kafka metadata. Restart the affected services after changing their configuration.

## Use the visualizer

- **Play** runs the selected backend experiment and records its transitions.
- **Previous**, **Replay**, **Next**, and **Play all** navigate captured frames without
  rerunning Kafka. A new Play clears that scenario's local timeline.
- **Stop** closes the scenario's Spring context and resets the visual state. It does
  not delete Kafka topics or stop the broker. Play and Stop are disabled while a run is in flight.
- **Technical events** reveals backend revisions hidden from the teaching sequence.
- **Show description on GitHub** beside the scenario title opens its English article
  in a new tab; each article links to its Russian translation.
- Click a node to see its concrete implementation references: classes, configuration,
  and tests, with explanations and GitHub line links.
  Drag nodes or resize containers to adjust the graph.

Browser history is local to the page and disappears on reload. The backend retains
up to 1,000 transitions in memory. There is one shared active runtime, so use one
scenario run at a time. See [the architecture guide](VISUALIZER_CONTEXT.md) for details.

## Tests

Start Kafka first and use the same networking setup as the application. The full
suite includes real Kafka integration tests and nested Spring Boot contexts:

```bash
./gradlew test
```

For the catalog, source-directory, and runtime unit tests without Kafka:

```bash
./gradlew test --tests 'io.drozda.sandbox.visualization.ScenarioCatalogTest' --tests 'io.drozda.sandbox.visualization.ScenarioSourceReferenceTest' --tests 'io.drozda.sandbox.visualization.ScenarioRuntimeServiceTest'
```

Validate Compose on the Docker host with `docker compose config`.

## Lifecycle and limitations

Scenario applications are separate Spring contexts in the **same JVM**, communicating
through internal HTTP endpoints on random ports. They are not separate processes.
Scenarios 002–009 reuse their context for repeated Play; 010–012 recreate it for each
experiment so new-group behavior starts with fresh topics and group IDs.

Stop preserves broker data; reset clears scenario state rather than truncating topics.
A topic-cleanup action is still planned. The supplied Compose file declares no persistent
volume, so removing/recreating the broker container is not a data-preserving operation.
To pause the broker without removing its container, run `docker compose stop kafka` on the host.

This is a local learning lab with a single broker and plaintext networking. Consumer
failure scenarios stop listener containers gracefully; they do not measure process-crash
detection or prove exactly-once processing. Replication, commit strategies, retries,
DLT, and transactions remain learning backlog items.

## Project documentation

- [How the platform works: RU](docs/platform-architecture.ru.md) / [EN](docs/platform-architecture.en.md) — interfaces, call flow, nested Spring context lifecycle, and UI event delivery; also linked from the site header.

- [Scenario articles: RU / EN](docs/README.md)
- [Architecture and API](VISUALIZER_CONTEXT.md)
- [Product direction](PRODUCT_VISION.md)
- [100-topic backlog and implemented mappings](KAFKA_100_PROBLEMS_AND_PATTERNS.md)
- [Learning diary](learning_notes.md)
- [Next-session starting prompt](NEXT_SESSION_PROMPT.md)
- [Optional mentoring workflow](devcontainer_teacher_agent_prompt.md)
