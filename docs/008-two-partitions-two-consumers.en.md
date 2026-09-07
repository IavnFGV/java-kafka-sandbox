# 008. Two partitions, two consumers, one group

[Русский](008-two-partitions-two-consumers.ru.md)

## Before / after

- Before the scenario: `6c04ea9` — 007 demonstrated that a second consumer is idle with one partition.
- Implementation: `4b9f5bc` — a real two-partition topic, two group members, assignment observation, and verification of both processing branches were added.

## Why this matters

Scenario 007 showed the limitation: Kafka assigns a whole partition to one
consumer in a group, so one partition cannot keep two service instances busy.
008 changes only one parameter: there are now two partitions.

The group now has two independent units of work. After rebalance, Kafka can
assign one partition to each consumer. This is the basic mechanism of horizontal
Kafka consumer scaling:

```text
Partition 0 -> Consumer A
Partition 1 -> Consumer B
```

The actual assignment can be reversed. The visualizer therefore does not assume
that A always gets P0. It waits for real assignment and draws edges from the
listener callbacks' results.

## The real experiment

[ParallelGroupScenarioApplication](../src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupScenarioApplication.java#L23)
creates a topic with two partitions and two listener beans with the same group
ID. Separate beans give telemetry clear `Consumer A` and `Consumer B` identities.

Both listeners implement `ConsumerSeekAware`. For example,
[ParallelGroupConsumerA](../src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/consumer/ParallelGroupConsumerA.java#L14)
forwards both assignments and revocations to the tracker. The tracker waits for
a stable map in which P0 and P1 have different owners.

[ParallelGroupExperiment](../src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupExperiment.java#L28)
creates six records after assignment. Even sequence numbers are explicitly sent
to P0 and odd ones to P1. Explicit partition selection makes this learning
experiment deterministic. In production, the partition is usually chosen from
the message key.

The experiment checks that:

- each partition received three records;
- P0 and P1 have different owners;
- each record was processed by its partition's owner;
- producer metadata matches `ConsumerRecord.partition()` and `offset()`;
- exactly six expected event IDs were received in total.

The visualization first builds actual assignment edges, then shows every record's
path: `Producer -> Partition -> Consumer`. The `Parallel Assignment` node turns
green only after both branches pass verification.

## How this differs from scenario 006

In 006, two partitions addressed the business problem of head-of-line blocking
between fast and slow orders. The main results were completion times and the
absence of global ordering.

In 008, the payload is deliberately neutral. The focus is the consumer group's
infrastructure contract: partitions are slots for parallel work, and consumers
receive those slots as whole units. One Kafka mechanism appears in both scenarios
but answers different engineering questions.

## Practical limits

Equal partition and consumer counts do not guarantee equal load. If P0 receives
far more data or its records take longer to process, Consumer A becomes a
bottleneck while Consumer B sits idle after finishing P1. Kafka balances partition
ownership, not the cost of each record. This leads to hot keys, hot partitions,
and consumer lag.

A third consumer would receive no assignment, like the second consumer in 007.
If one of the two consumers stops, the remaining one can own both partitions
after rebalance. Later scenarios examine membership changes separately.

The integration contract is in
[ParallelGroupScenarioTest](../src/test/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/ParallelGroupScenarioTest.java#L20).

The scenario reinforces backlog `#13 Consumer group mechanics` and
`#14 One partition assigned to one consumer in a group`.
