# 003. Topic, partition, and offset

[Русский](003-topic-partition-offset.ru.md)

## Before / after

- Before: `0fef6ba` — the diagram explained partitions through a predefined animation.
- After: `50e363a` — a separate application sends and reads real Kafka records.
- Clearer visualization: `1a370be` — actual records, offsets, and assignments appear inside the nodes.

## Mental model

A Kafka topic is the logical name of a stream, but records physically reside in
partitions. Each partition is a separate ordered, append-only log with its own
sequence of offsets.

A record's complete address has three parts:

`topic + partition + offset`.

An offset alone therefore does not identify anything uniquely. Two partitions
can contain records with the same offset. Kafka does not promise global ordering
across partitions.

## What the experiment does

The scenario application creates `scenario-003-partition-offsets` with two
partitions. [`PartitionedEventPublisher`](../src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/producer/PartitionedEventPublisher.java#L20) makes three explicit sends:

1. Event A to partition `0`.
2. Event B to partition `1`.
3. Event C to partition `0` again.

We select partitions explicitly to make the experiment deterministic. Key-based
selection and the default partitioner are separate topics.

Let the first offset in partition `0` be `N`, and the offset in partition `1`
be `M`. The third record must receive an offset greater than `N` in partition
`0`. We deliberately do not assert absolute values of `0, 0, 1`: the topic
persists between runs, so actual values might be `12, 7, 13`.

## How the consumer subscribes

[`PartitionedEventListener`](../src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/consumer/PartitionedEventListener.java#L17) subscribes to the entire topic through `@KafkaListener`,
not to a specific partition. Since there is one consumer, the Kafka consumer
group assigns both partitions to it:

```text
Partition 0 ─┐
             ├→ PartitionedEventListener
Partition 1 ─┘
```

The listener accepts a `ConsumerRecord`, not just a payload. This exposes the
`topic()`, `partition()`, and `offset()` of the record actually read.

## How the result is confirmed

Before sending, [`TopicPartitionOffsetsExperiment`](../src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/TopicPartitionOffsetsExperiment.java#L33) registers a wait for each
unique `eventId` in [`PartitionedEventTracker`](../src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/PartitionedEventTracker.java#L16). Registration happens first so a
fast listener cannot deliver a record before its wait exists.

The producer returns a `SendResult` with the stored record's coordinates.
The listener receives a `ConsumerRecord` with the read record's coordinates.
The experiment compares them in pairs. Success means that:

- Kafka acknowledged all three appends;
- the consumer received the three events belonging to this run;
- producer and consumer observed matching partitions and offsets;
- the second record in partition `0` has a greater offset than the first.

[`TopicPartitionOffsetsScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/TopicPartitionOffsetsScenarioStarter.java#L51) converts this result into UI events.
Actual values appear in the runtime log, and the broker, topic, partitions,
producer, and consumer are highlighted only after confirmation. Partition nodes
retain this run's contents: `A @ offset N`, `B @ offset M`, and `C @ offset N'`.
The consumer displays its assigned partitions, `0, 1`.

## Architectural boundary

As in `002`, [`TopicPartitionOffsetsEnvironment`](../src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/TopicPartitionOffsetsEnvironment.java#L19) starts a separate Spring context
on a random HTTP port. Repeated Play reuses the context but creates new events;
Stop closes the context. The internal HTTP API starts the experiment, while the
messages themselves are sent to Kafka through `KafkaTemplate`.

Scenarios `001` and `002` do not reuse `003`'s Kafka beans. This allows the next
experiment to be changed or broken without affecting earlier examples.

## Practical takeaway

A consumer usually subscribes to a topic, and Kafka assigns partitions to it.
An offset is a local position inside an assigned partition. This is why consumer
progress, ordering, and subsequent scaling are always considered per partition,
not just by topic name.
