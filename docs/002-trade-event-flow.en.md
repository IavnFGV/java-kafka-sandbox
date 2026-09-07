# 002. The first real message through Kafka

[Русский](002-trade-event-flow.ru.md)

## Before / after

- Before: `8132885` — Trade Event Flow existed only as a predefined animation.
- After: `18fec16` — a separate Spring Boot application publishes a real record and waits for it to arrive through Kafka.

`001 System Ready` checked only the structure of the Spring context: the required
beans existed, but no real message had been sent. In `002 Trade Event Flow`, we
follow the complete route for the first time:

`Client → Publisher → Kafka topic → Listener`.

A green node now represents a confirmed fact, not an assumption made by the
visualizer. The publisher turns green after Kafka acknowledges the record, the
topic after its `partition` and `offset` are obtained, and the listener only after
receiving the exact event created by the current run.

## Why there are so many classes

The scenario has three parts: environment management, the Kafka experiment, and
result visualization. This keeps Kafka code unaware of the boxes on the screen
and prevents the visualizer from showing success before a real result arrives.

### Scenario lifecycle

[`TradeFlowEnvironment`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowEnvironment.java#L19) manages a separate Spring Boot context. The first Play
starts [`TradeFlowScenarioApplication`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioApplication.java#L21) on an available HTTP port and retains a
reference to the context. Pressing Play again **does not create another context**:
the existing application is reused. Stop calls `close()`, and only the next Play
creates a new context.

The environment also sets a unique `group.id`, a topic name, and JSON settings,
then communicates with the scenario application through an internal HTTP API.
This makes the boundary explicit: the mediator does not retrieve Kafka beans
directly from another context; it sends a command and receives a structured response.

`TradeFlowScenarioApplication` configures this small application. It creates a
`NewTopic` with one partition and one replica, and registers the publisher,
listener, tracker, and experiment. The `scenario.trade-flow.enabled=true`
property prevents these beans from accidentally appearing in the main application
or another scenario.

[`TradeFlowScenarioController`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioController.java#L16) exposes internal endpoints:

- `GET /status` reports that the components have been created;
- `POST /reset` clears pending waits;
- `POST /commands/send-and-receive` runs the real experiment.

[`TradeFlowScenarioCommandRequest`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioCommandRequest.java#L3) contains the name of a particular run.
[`TradeFlowScenarioStatus`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioStatus.java#L3) is a result snapshot: component readiness, `eventId`,
topic, partition, offset, and any error.

### The Kafka experiment

[`TradeFlowEvent`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/model/TradeFlowEvent.java#L3) is the scenario's own message model. It has a unique `eventId`,
a business key `tradeId`, a symbol, an event type, and a creation time. The Kafka
record key is `tradeId`; later this can demonstrate partition selection and
ordering for identical keys.

[`TradeFlowPublisher`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/producer/TradeFlowPublisher.java#L20) is a thin wrapper around
`KafkaTemplate<String, TradeFlowEvent>`. It sends the event and returns a
`CompletableFuture<SendResult<...>>`. Completing the future confirms that the
broker accepted the record and provides partition and offset metadata. It does
not yet mean that a consumer has processed the message.

[`TradeFlowListener`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/consumer/TradeFlowListener.java#L16) subscribes through `@KafkaListener` and passes the received
message to the tracker. The listener deliberately knows nothing about HTTP or
visualization.

[`TradeFlowEventTracker`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowEventTracker.java#L13) connects two asynchronous moments. Before sending, the
experiment registers a wait by `eventId` and receives a `CompletableFuture`.
When the listener receives a message, the tracker finds the wait with the same
`eventId` and completes its future. A `ConcurrentHashMap` is needed because the
experiment and Kafka listener run on different threads. An old topic message
cannot falsely complete a new run.

[`TradeFlowExperiment`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowExperiment.java#L27) coordinates one run:

1. Creates an event with a new `eventId`.
2. Registers the wait before sending, so a fast response cannot be missed.
3. Waits up to ten seconds for Kafka acknowledgement.
4. Separately waits up to another ten seconds for a matching listener event.
5. Returns a `TradeFlowScenarioStatus` with confirmed facts or an error.

### From result to screen

[`TradeFlowScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowScenarioStarter.java#L51) implements the shared [`ScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/spi/ScenarioStarter.java#L15). The mediator
calls its `send-and-receive` command. The starter asks the environment to run the
experiment, then converts the result into visualizer events: highlighting the
request, publisher, broker, topic, consume edge, and listener. Short delays make
the animation readable; the Kafka check relies on actual responses, not a timer.

[`TradeFlowScenarioTest`](../src/test/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowScenarioTest.java#L21) follows the same public path through the mediator and
real Kafka. It checks READY statuses for nodes and edges, partition information
in the runtime log, and session completion. The environment is always stopped in
`finally`, so the test does not leave a nested Spring context running.

## What to remember

A successful `KafkaTemplate.send()` confirms a write to Kafka, but not end-to-end
consumer processing. These are two separate observable events. The scenario
therefore waits separately for broker acknowledgement and a listener message.

Kafka is shown as a container, with the topic as a node inside it. In later
scenarios, topics can contain partitions, and a Kafka cluster can contain
multiple brokers and replicas, while keeping the visual model familiar.
