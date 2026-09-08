# 001. What it means for a Spring Boot application to be ready for Kafka

[Русский](001-system-ready.ru.md)

## Before / after

- Before: `11d56e5` — the broker was highlighted as available merely because `KafkaTemplate` had been created.
- After: `8dc0d2b` — the scenario checks only Spring wiring, while the broker remains an external, unverified dependency.

Before sending the first message, it helps to check the application's basic structure.
Inside the separate scenario Spring context are two components: [`TradeEventPublisher`](../src/main/java/io/drozda/sandbox/scenario/systemready/producer/TradeEventPublisher.java#L18) and
[`TradeEventListener`](../src/main/java/io/drozda/sandbox/scenario/systemready/consumer/TradeEventListener.java#L13). The publisher uses a `KafkaTemplate` created by Spring,
and the listener declares a method with `@KafkaListener`.

The Kafka broker lives outside the application. It stores records in partitions
and serves them to consumer groups. It is essential to distinguish two checks:
creating Kafka components in Spring and actually connecting to the broker.

In `001 System Ready`, the mediator starts a separate Spring Boot context.
A dedicated [`SystemReadyProbe`](../src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java#L25) receives the publisher, listener, and
`KafkaTemplate` through dependency injection. If the context starts successfully
and all dependencies are present, the visualizer highlights the application's
internal components one by one.

The Kafka broker deliberately does not turn green. Having a `KafkaTemplate`
means only that a producer client is configured in Spring. It does not prove
that the broker is reachable, the topic exists, a message has received an
acknowledgement, or the listener has received a partition assignment.

Why have such a simple scenario? It separates two classes of problems. If the
Spring context does not start, look at configuration and bean creation. If the
beans exist but a message does not get through, investigate the network, broker,
topic, serialization, consumer group, and offsets next.

A useful interview takeaway: a ready `KafkaTemplate` means client readiness,
not proof that Kafka works. Actual end-to-end readiness can only be demonstrated
by sending and receiving a real message. That is the purpose of
`002 Trade Event Flow`.

## Core implementation

- [`SystemReadyProbe`](../src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java#L25) — Checks that injected components exist.
- [`SystemReadyScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyScenarioStarter.java#L51) — Displays the Spring wiring check result.

## Component isolation

The publisher, listener, and `TradeEvent` model live under `scenario.systemready`
in `producer`, `consumer`, and `model` subpackages. The main platform context does
not create them. Only the conditional `SystemReadyScenarioApplication` imports
them, enabled by the environment with `scenario.system-ready.enabled=true`.
The environment also sets topic/group names and JSON model settings instead of
placing scenario-specific values in the shared YAML.

The listener has `autoStartup=false`: its bean and container exist, but Kafka
consumption does not start. Scenario 001 therefore works without a reachable broker.
Scenario 002 verifies actual send/receive behavior. The obsolete
`PublisherInjectionTest` was removed; `AllComponentsTest` checks isolation,
readiness, Stop, and restart.

`@VisualAction` and its aspect were removed: they were a method-logging experiment,
unrelated to the current timeline. `SystemReadyScenarioStarter` creates UI events.
