# 005. Ordering within one partition

[Русский](005-ordering-within-one-partition.ru.md)

## Before / after

- Basic model: `553a11f` — the event gained an explicit business `sequence` number.
- Implementation: `c18e577` — a separate Spring Boot application verifies ordering against real Kafka.

## Why this matters

Scenario `004` answered **where** Kafka routes related records. A stable `orderId`
key placed one order's events in one partition. Scenario `005` checks the next
contract: **in what order** these records are written and delivered to the listener.

A partition is an append-only log. The broker assigns increasing offsets, and
the consumer reads the log sequentially:

```text
sequence 0 CREATED   -> offset N
sequence 1 VALIDATED -> offset N+1
sequence 2 RESERVED  -> offset N+2
sequence 3 PAID      -> offset N+3
sequence 4 PACKED    -> offset N+4
sequence 5 SHIPPED   -> offset N+5
```

`sequence` belongs to the payload and expresses the expected business order.
Kafka assigns `offset`, which expresses the record's actual position in the
partition. These are different coordinates that the experiment deliberately compares.

## The real experiment

[OrderedEventPublisher](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/producer/OrderedEventPublisher.java#L19)
sends each event with `orderId` as its key. Sends are sequential, waiting for
broker acknowledgement in
[PartitionOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/PartitionOrderingExperiment.java#L44).

The topic has three partitions, and the listener starts three consumers in one
group through
[`concurrency = "3"`](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/consumer/OrderedEventListener.java#L21).
After rebalance, each consumer receives one partition. All six events with the
same key go to one partition and are processed by its current owner.

The experiment checks three properties:

1. All observations have the same `partition`.
2. Each successive `offset` is greater than the previous one.
3. Callback order contains `sequence 0..5` without rearrangement.

These checks are in
[PartitionOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/PartitionOrderingExperiment.java#L51).
Producer metadata is also matched to received `ConsumerRecord` objects by
`eventId`, not list position. Coordinate verification therefore cannot
accidentally hide a callback-order violation.

## How callback order is recorded

In `004`, the tracker created a separate `CompletableFuture` for each `eventId`.
This is convenient for checking delivery, but the original futures list can
return results in a predefined order regardless of the actual callback order.

In `005`,
[OrderedEventTracker](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/OrderedEventTracker.java#L28)
first registers the expected set of IDs. Each actual listener invocation adds
the record to the shared `receivedInCallbackOrder` list when it arrives:

```java
receivedInCallbackOrder.add(new TrackedOrderedRecord(
    record, consumerId(Thread.currentThread().getName())
));
```

Once all records arrive, the tracker completes one future with an immutable copy
of the list. Subsequent verification therefore sees the real listener callback sequence.

The tracker also waits for a stable `3 consumers x 1 partition` assignment in
[awaitStableAssignments](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/OrderedEventTracker.java#L63),
so an intermediate phase of the initial rebalance is not mistaken for the final topology.

## Visualization

[PartitionOrderingScenarioStarter](../src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/PartitionOrderingScenarioStarter.java#L56)
builds actual assignment edges. Each record then follows the animated path
`publisher -> partition -> consumer`, and the partition shows its latest sequence
and offset. The `Order Verification` node turns green only when all three checks pass.

## Boundary of the guarantee

Kafka guarantees record order within a partition and sequential delivery to one
consumer in that group. The guarantee does not extend across partitions. Nor
does it guarantee the **completion order** of business processing if the listener
hands records to a parallel executor, reactive pipeline, or another asynchronous
mechanism. In that case, the application must preserve order itself or provide
idempotency and state version checks.

The scenario covers backlog `#11 Ordering guarantees within a partition`.
