# 012. Consumer Failure and Partition Takeover

[Русский](012-consumer-failure-partition-takeover.ru.md)

## What we check

The scenario covers questions #13, #41, and #97: what happens to a partition when
its consumer disappears, why messages are retained, and where temporary lag comes from.

Initially, two consumers in one group share two partitions. The backend does not
assume a particular distribution; it obtains it from `onPartitionsAssigned`.
It then selects the actual owner of Partition 1 and stops that listener container.

While ownership changes, the producer sends a new record to the released
partition. The record is stored in Kafka while a consumer is temporarily absent.
After rebalance, the remaining member receives both partitions and reads the
waiting record at its offset.

## How the experiment works

[`TakeoverTracker`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java#L7) stores real assignment and revocation callbacks and waits for
records by unique `eventId`. [`TakeoverExperiment`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverExperiment.java#L6) performs this sequence:

1. Start both controlled consumers.
2. Wait for a distribution with two owners.
3. Confirm processing of one record from each partition.
4. Stop P1's owner and immediately publish a record to P1.
5. Wait until the survivor owns both partitions.
6. Confirm delivery of the waiting record and continued processing.

The UI builds ownership only from Kafka's result. During the transition,
`Temporary Lag` is highlighted as waiting; the record then moves to the survivor,
whose node shows ownership of P0 and P1.

## Stop versus a real crash

The playground uses a controlled `MessageListenerContainer.stop()` through
[`TakeoverConsumerControl`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverConsumerControl.java#L3). This consumer leaves the group gracefully, so the
coordinator learns about the change quickly. A process crash has no graceful
leave: Kafka typically waits for missing heartbeats until `session.timeout.ms`,
and the partition's unavailability window can be longer.

The experiment therefore demonstrates reassignment and record retention, but does
not measure crash-detection latency. Killing an actual container remains a
separate future extension of the mediator infrastructure.

## Practical takeaway

Kafka stores partition records independently of the current consumer. Losing a
consumer does not mean losing data: after membership changes, the partition is
assigned to a surviving member. However, lag grows during detection and rebalance,
and a record after the last committed offset may be processed again. A handler
must therefore account for idempotency and a correct commit strategy.

## Before and after

Before implementation: `d977767`.

After implementation: `6d5d278`.

## Core implementation

- [`TakeoverConsumerControl`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverConsumerControl.java#L3) — Stops an individual listener container.
