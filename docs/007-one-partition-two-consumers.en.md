# 007. One partition, two consumers, one group

[Русский](007-one-partition-two-consumers.ru.md)

## Before / after

- Before the scenario: `1f8d3dd` — scenario 006 demonstrated parallelism across independent partitions.
- Implementation: `f04011c` — the static 007 diagram was replaced by real assignment, owner shutdown, and takeover after rebalance.

## Why this matters

After scenario 006, a natural idea arises: if consumers provide parallel
processing, simply run more of them. But Kafka scales a consumer group by
partition count, not record count.

Within one group, a partition is assigned to at most one consumer at a time.
For a topic with one partition, two consumers therefore produce this picture:

```text
Partition 0 -> Consumer A
               Consumer B: idle
```

Kafka does not give even records to Consumer A and odd records to Consumer B.
A partition is an indivisible unit of assignment. Otherwise, two handlers could
read one ordered log concurrently and disrupt its order.

The practical takeaway: a group's throughput is constrained by its partition
count. If a service deploys ten instances but its topic has three partitions,
at most three instances can work on that topic. The others remain group members
without assignments.

## Why an idle consumer can still be useful

An extra consumer adds no parallelism, but it can provide spare capacity. If the
current owner stops, group membership changes. The group coordinator starts a
rebalance, and the available consumer can receive Partition 0:

```text
before shutdown: Partition 0 -> Consumer A; Consumer B idle
 after rebalance: Partition 0 -> Consumer B; Consumer A stopped
```

This is not active-active processing of one partition. It is a change of its
single owner. There is a pause between shutdown and the new assignment, so
rebalance affects latency. Later scenarios explore rebalance and failure
detection in more detail; here we establish the basic ownership contract.

## Two real consumers

The scenario does not use `concurrency = 2` on one method because we need to
control each member separately. Two listener beans are created. For example,
[ConsumerGroupMemberA](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberA.java#L14)
has stable `ID` and `LABEL` values, and its `@KafkaListener` subscribes to the
same topic and group ID as Consumer B.

Both implement `ConsumerSeekAware`. The
[`onPartitionsAssigned`](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberA.java#L30)
callback reports the actual assignment to the tracker, and `onPartitionsRevoked`
removes old ownership. These callbacks come from the Spring Kafka listener
container in response to the real consumer-group protocol.

We cannot assume in advance that Consumer A will receive Partition 0. The result
depends on join timing, the chosen assignor, and the group's current state.
[SinglePartitionGroupTracker](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupTracker.java#L50)
therefore waits until the partition has one owner and returns that owner's label.
The other member is identified as idle only after observing the assignment.

## How one listener is stopped

Stopping the whole scenario application would remove both the owner and the
standby consumer. Targeted control is provided by
[ConsumerMemberControl](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/ConsumerMemberControl.java#L9).

Spring Kafka registers containers in `KafkaListenerEndpointRegistry` using their
`@KafkaListener(id=...)` values. The control finds the current owner's container
and calls `stop()`. The broker sees the membership change; the remaining consumer
goes through rebalance and receives Partition 0.

Before every repeated Play, `startBoth()` starts both containers. This matters
for the playground: a listener stopped during the previous run must not change
the next experiment's initial conditions.

## Experiment sequence

The main orchestration is in
[SinglePartitionGroupExperiment](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupExperiment.java#L36):

1. Start both listener containers.
2. Wait for a single owner of Partition 0.
3. Publish three `BEFORE FAILURE` records to the topic.
4. Verify that only the owner received the first batch.
5. Stop the owner's container.
6. Wait for a new owner different from the previous one.
7. Publish three more records in the `AFTER TAKEOVER` phase.
8. Verify that the previously idle consumer received the entire second batch.

Each batch registers unique event IDs before publication and waits for all
matching callbacks. Producer metadata is matched to actual `ConsumerRecord`
objects by event ID, partition, and offset. A successful send therefore cannot
be mistaken for successful processing.

The scenario uses unique topic and group names whenever a separate Spring context
starts in
[SinglePartitionGroupEnvironment](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupEnvironment.java#L30).
Old records and committed offsets do not interfere with a new start.

## What the UI shows

Initially, both consumers appear as healthy group members. After actual
assignment, only the owner receives an edge from Partition 0 and a green status.
The other is marked `WAITING`, not `FAILED`: it is healthy but has nothing to own.

The first batch is animated only through the owner. After targeted shutdown,
the owner turns red, the assignment edge moves to the remaining consumer, and
the second batch follows it. The `Assignment and Takeover` node shows the actual
names of the initial owner, idle member, and takeover owner.

## What the test checks

[SinglePartitionGroupScenarioTest](../src/test/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupScenarioTest.java#L20)
asserts observable properties:

- the initial owner and idle consumer differ;
- the takeover owner is the previously idle consumer;
- only the initial owner processed the first batch;
- only the takeover owner processed the second batch;
- all six records belong to Partition 0;
- the mediator completes the timeline, and the observer gets `READY` status.

The test also runs the experiment a second time through the mediator, verifying
repeated Play behavior that matters for an interactive playground.

## Limits of the conclusion

This scenario does not prove exactly-once processing during failure. If a
consumer performs an external side effect but fails before committing its offset,
the new owner can receive the record again. Offset commits, at-least-once
delivery, and idempotent consumers are separate topics.

A standby consumer also does not make failover instantaneous. Its speed depends
on heartbeat and session timeouts, graceful departure behavior, and the
rebalance protocol.

The scenario's main conclusion: **the number of active consumers in one group
cannot exceed its partition count, but an extra healthy consumer can take over
a partition after membership changes**.

The scenario covers backlog `#13 Consumer group mechanics` and
`#14 One partition assigned to one consumer in a group`.
