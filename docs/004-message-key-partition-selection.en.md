# 004. Message keys and partition selection

[Русский](004-message-key-partition-selection.ru.md)

## Before / after

- Before: `7516328` — key-based partitioning existed only in the learning plan.
- After: `6618e4c` — a separate application verifies routing using real Kafka records.
- Interactivity: `b4120bd` — the user selects a key strategy, and the backend runs the corresponding Kafka experiment.
- Consumer assignment: `1da60af` — three real consumers show their partition assignments and each record's complete path.
- Routing confidence: `52cbacf` — ten events reduce accidental coincidence, and assignment edges are rebuilt from runtime data.
- Compact topology: `198c903` — partition nodes show a summary, while the complete sequence remains in the runtime log.
- Stable assignment: `f7a4c55` — the experiment accounts for revocations and waits for the initial rebalance to finish.
- Learning boundary: `6023ab9` — the description separates routing in `004` from ordering in the upcoming `005` and shows how three consumers are created.

## Why this matters

In an event-driven system, several events belong to the same entity. For example,
`order-42` moves through `CREATED`, `PAID`, and `SHIPPED`. If these events land
in different partitions, they can be processed in parallel without a shared
ordering guarantee.

A Kafka message key connects routing to a business identifier:

```java
kafkaTemplate.send(topic, event.orderId(), event);
```

The producer does not supply a partition number: the default partitioner computes
it from the serialized key. As long as the partition count remains unchanged,
the same key routes to the same partition.

What matters is a single ordered log for the entity, rather than a permanent
physical consumer. Parallel processing of different partitions could write
`SHIPPED` to a shared database, then a delayed `PAID`, leaving an incorrect state.

## How `004` differs from `005`

`004` asks **where related records will go**. We choose a key and demonstrate
that one order's events reside in one partition. This scenario establishes a
necessary condition for ordering.

`005` asks **in what order records will be read** once they are in one partition.
There we compare send order, increasing offsets, and listener receive order.
`004` examines routing and co-location; `005` examines order within a partition log.

The UI offers `No key`, `Unique eventId`, and `Order ID`. All options actually
send records to Kafka, but only `Order ID` provides the green guarantee that an
order's events are routed together.

## Experiment

The topic has three partitions. The application sends ten successive states of
`order-42` and one record for another order. An abbreviated stream looks like this:

```text
order-42 CREATED
order-42 VALIDATED
...
order-42 PAID
...
order-42 DELIVERED
order-73 CREATED
```

[`KeyPartitioningExperiment`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningExperiment.java#L29) obtains producer metadata, while the listener receives
complete `ConsumerRecord` objects. The key, partition, and offset are compared
for every record. [`KeyPartitioningScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/KeyPartitioningScenarioStarter.java#L44) computes the number of distinct
partitions for `order-42` when displaying a strategy without a routing guarantee. The guarantee flag comes from the
`ORDER_ID` strategy; the experiment has no separate single-partition assertion.

Why ten records? If each unique `eventId` hashes independently and uniformly to
one of three partitions, the probability that all ten happen to land in one
partition is `(1/3)^9 ≈ 0.0051%`. With three events, it would be `1/9 ≈ 11.1%`,
so the diagram would too often look correct with the wrong key. Even a rare
coincidence does not make the scenario green: the guarantee comes from the
`Order ID` strategy, not one lucky run.

This calculation does not directly apply to `No key`: the default producer can
use sticky partitioning and send a batch of unkeyed records to one partition
for a while. This is another reason an observed coincidence is not a routing contract.

Different keys can collide in the same partition: a key creates a stable routing
group, not a dedicated partition.

In the visualizer, each partition summarizes this run's records. Empty partitions
are shown too, making it clear that eleven records need not fill three partitions
evenly. To keep a long stream inside the node, each partition displays a compact
summary: record count, offset range, and latest status. The complete sequence
remains in the runtime log at the bottom of the page.

The animation plays each record separately: `publisher → partition N`, followed
by `partition N → consumer`. This shows where Kafka routed each `CREATED`, `PAID`,
and `SHIPPED`, and which listener read it. Three real consumer instances run in
one consumer group. After rebalance, each receives one of the three partitions;
the visualizer reads actual assignments from the listener container rather than
assuming them. With the correct key, related events therefore go to one partition
and, under the current assignment, to one consumer. The ordering guarantee comes
from the shared partition; the specific consumer can change after rebalance.

The three consumers are created through a Spring Kafka parameter, not three
listener classes:

```java
@KafkaListener(
    topics = "${app.kafka.topics.key-partitioning}",
    concurrency = "3"
)
public void onEvent(ConsumerRecord<String, KeyedOrderEvent> record) {
    tracker.received(record);
}
```

Spring creates three listener containers and therefore three Kafka consumers in
one group. More precisely, the outer `ConcurrentMessageListenerContainer` manages
three child `KafkaMessageListenerContainer` instances. Each child has its own
`KafkaConsumer` and a long-lived poll thread:

```text
ConcurrentMessageListenerContainer
├── container-0 → KafkaConsumer → poll-thread-0
├── container-1 → KafkaConsumer → poll-thread-1
└── container-2 → KafkaConsumer → poll-thread-2
```

All three threads call the same `onEvent` method, so the callback signature does
not reveal which consumer delivered a record. For educational telemetry, the
tracker reads the current listener thread's name:

```java
String consumerId = consumerId(Thread.currentThread().getName());
```

The same thread participates in assignment callbacks and record processing.
[`KeyedEventTracker`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyedEventTracker.java#L53) can therefore associate a label such as `Consumer A` with its
partitions and tag the received `ConsumerRecord` accordingly. This lets the
visualizer route a record to its actual consumer even though all consumers call
the same Java method.

[`KeyedOrderEventListener`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/consumer/KeyedOrderEventListener.java#L17) implements `ConsumerSeekAware`:
`onPartitionsAssigned` forwards assignments to the tracker, and
`onPartitionsRevoked` removes revoked partitions. The starter then converts the
assignment map into dynamic `partition → consumer` edges on the screen.

Thread names are an observation tool for this learning environment, not part of
the business contract. A production handler usually needs only `topic`,
`partition`, `offset`, key, and payload; consumer or thread names can change after
a restart or rebalance.

When a consumer group starts, the first consumer to join can temporarily receive
all partitions. This is an intermediate phase, not the final assignment. The
experiment waits for a stable `3 consumers × 1 partition` state, accounts for
assignment and revocation callbacks, and only then publishes records. Dashed
`partition → consumer` edges come from runtime assignments, not the static graph.
A new run clears old edges and draws current ones after any rebalance; moving
records appear above them.

## Practical takeaway

Choose the key from the identifier of the entity whose event order matters:
`orderId`, `accountId`, or `customerId`. A poor or overly popular key can create
a hot partition, and increasing the partition count can change the
key → partition mapping. We will verify ordering itself in scenario `005`.

The scenario covers backlog `#10 Message key and partition selection`.

## Core implementation

- [`KeyedOrderEventPublisher`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/producer/KeyedOrderEventPublisher.java#L20) — Selects the key and sends to Kafka.
- [`KeyPartitioningExperiment`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningExperiment.java#L66) — Compares keys and producer/consumer coordinates.

## Additional implementation references

- [`KeyPartitioningScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/KeyPartitioningScenarioStarter.java#L100)
