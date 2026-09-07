# 010. Earliest vs Latest

[Русский](010-earliest-vs-latest.ru.md)

## Practical problem

A new service connects to an existing Kafka topic that already contains events.
Should it rebuild state from the entire history or process only new events?
`auto.offset.reset` controls this choice, but only when the consumer group has
no valid committed offset.

Scenario 010 covers question #16 from the Kafka backlog and compares two new,
independent groups on one stream.

## Experiment

The topic has one partition to make Kafka coordinates obvious. Both listeners
are created with `autoStartup=false`. Their policies are declared directly in
[`EarliestConsumer`](../src/main/java/io/drozda/sandbox/scenario/earliestvslatest/consumer/EarliestConsumer.java#L20)
and
[`LatestConsumer`](../src/main/java/io/drozda/sandbox/scenario/earliestvslatest/consumer/LatestConsumer.java#L20).

[`EarliestVsLatestExperiment`](../src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/EarliestVsLatestExperiment.java#L26)
executes a strict sequence:

1. The producer publishes three historical records and receives Kafka acknowledgements.
2. Two new groups start: `earliest` and `latest`.
3. The backend waits for both groups' assignments without relying on an arbitrary `sleep`.
4. The producer publishes two more live records.
5. Verification compares the offsets actually received.

The result:

- `earliest` reads offsets `0, 1, 2, 3, 4`;
- `latest` reads only offsets `3, 4`.

The timeline first shows history, then its replay only into `earliest`. Delivery
of each live record to both groups is combined into one `PARALLEL` frame.

## Important limitation

`auto.offset.reset` is not a “read everything again” command. If the group has
already saved an offset, Kafka resumes from it regardless of `earliest` or
`latest`. The policy applies when an offset is missing or is no longer available
due to retention.

Every repeated `Play` therefore recreates the isolated scenario application with
unique topics and group IDs. This is not a production recommendation; it makes
the behavior of a new group reproducible.

## Choosing a policy

`earliest` suits projections, migrations, auditing, and services that need
accumulated history. `latest` suits consumers interested only in future signals,
but requires deliberately accepting that historical events will be skipped.

## Before and after

Before the scenario: `59935db`.

After implementation: `280b5f8`.
