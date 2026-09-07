# 011. Rebalance When a Second Consumer Joins

[Русский](011-rebalance-when-second-consumer-joins.ru.md)

## What we check

The scenario answers questions #41 and #94: what happens to a consumer group
during scaling, and why can a new consumer temporarily pause processing?

The topic has two partitions. Initially, only Consumer A starts, so Kafka assigns
both to it. After a control publication, Consumer B starts. The membership
change triggers a rebalance: previous assignments are revoked, the group agrees
on a new distribution, and each consumer receives a separate partition.

The UI does not assume who gets P0 or P1. [`RebalanceTracker`](../src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceTracker.java#L34) receives real
`onPartitionsRevoked` and `onPartitionsAssigned` callbacks and builds ownership
maps from Kafka's response.

## Sequence

[`RebalanceExperiment`](../src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceExperiment.java#L20) performs these steps:

1. Start Consumer A's controlled listener container on its own.
2. Wait for the actual assignment of both partitions to one owner.
3. Send a record to each partition and check its actual handler through the tracker.
4. Start Consumer B.
5. Wait for an ownership map with two different owners.
6. Send a new pair of records to confirm the new distribution works.

Listener containers declare `autoStartup=false` and are started by
[`RebalanceConsumerControl`](../src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceConsumerControl.java#L11). The scenario therefore controls when the join occurs
without manually simulating a rebalance.

## Practical meaning

Rebalance enables scaling and recovery, but it is a coordination operation.
Processing can pause while ownership changes. Long callbacks, frequent restarts,
unstable consumers, and poorly chosen timeouts can turn useful redistribution
into a rebalance storm.

Once stable, the group again follows the rule that a partition belongs to only
one consumer within the group. Before scaling, one consumer can own several
partitions.

Each `Play` creates new topics and group IDs, so the demonstration begins with
fresh group membership.

## Before and after

Before implementation: `f55bd4a`.

After implementation: `ba04c13`.

## Core implementation

- [`RebalanceExperiment`](../src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceExperiment.java#L20) — Starts consumers in sequence and verifies ownership before and after the join.
