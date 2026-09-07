# 006. Parallel orders without global ordering

[Русский](006-no-global-ordering-across-partitions.ru.md)

## Before / after

- Before the scenario: `601fbc3` — the shared timeline visualization for previous experiments was complete.
- Implementation: `50cc164` — a separate Spring Boot application, two Kafka topologies, a UI switch, and an integration test were added.

## The practical question

Scenario `004` showed how a stable business key routes one entity's events to one
partition. Scenario `005` confirmed that Kafka preserves record order within
that partition. It is easy, however, to draw the wrong conclusion: that the
entire topic is ordered.

Scenario `006` demonstrates two aspects of partitioning together:

1. Independent partitions allow different orders to be processed in parallel.
2. There is no shared processing or completion order across partitions.

The practical problem is **head-of-line blocking**. If a fast order and a slow
order land in one partition, one consumer receives a shared sequential stream.
The fast order's events may wait for intermediate stages of the slow order to
finish processing. This is correct behavior, but it increases latency for
independent work.

## Two orders

The experiment creates the same interleaved stream for both topologies:

```text
Fast: CREATED ---------------- PAID ---------------- COMPLETED
Slow:         CREATED -> VALIDATING -> RESERVED -> PACKING -> SHIPPED -> COMPLETED
```

Fast Order has three states and a short simulated processing time. Slow Order
has six states and longer processing. Events are created in
[GlobalOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingExperiment.java#L78),
and their combined publication order is explicitly defined in
[the returned list](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingExperiment.java#L85).
The delay is in the payload solely to make this learning experiment observable.
A production handler should not trust a client field that controls its work duration.

## Mode 1: one partition, one consumer

The UI defaults to `1 partition / 1 consumer`. All nine records are explicitly
sent to partition 0. A single listener processes them strictly one after another.
As a result:

- publication order and completion order match;
- each order's sequence is preserved;
- Fast Order waits for Slow Order stages ahead of it;
- throughput is limited by one consumer loop.

This is not a Kafka error. We requested one ordered queue and got exactly that.

## Mode 2: two partitions, two consumers

In `2 partitions / 2 consumers` mode, Fast Order goes to partition 0 and Slow
Order to partition 1. The choice is explicit in
[GlobalOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingExperiment.java#L55)
to make the experiment deterministic. An ordinary producer can use a stable
`orderId` key and let the partitioner choose the shard. The specific partition
number matters less than the contract: one order always uses one key.

[GlobalOrderListener](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/consumer/GlobalOrderListener.java#L21)
contains two educational subscriptions. The first has `concurrency = 1` for the
single topic. The second has `concurrency = 2` for the parallel topic. Spring
Kafka creates two child containers, meaning two real Kafka consumers in one
group. After assignment, one owns partition 0 and the other owns partition 1.

The consumer loops now work independently. Fast Order can finish while Slow
Order is still moving through its stages. Overall completion order no longer
has to match publication order. Meanwhile, `CREATED -> PAID -> COMPLETED` within
Fast Order and the entire Slow Order chain remain ordered.

This is what **Kafka provides no global ordering across partitions** means.
It does not mean random ordering within each partition.

## What the application measures

[GlobalOrderTracker](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderTracker.java#L25)
registers expected event IDs and the run's start time. The listener simulates
work, then the tracker stores the record at actual completion, together with the
consumer ID and elapsed time. The resulting list therefore reflects completion
order rather than the publication order known in advance.

The experiment checks:

- all events were published and read;
- producer and consumer observe matching `partition + offset` coordinates;
- each order's sequence is `0..N`;
- whether global completion order matches publication order;
- how many milliseconds Fast and Slow orders took to complete.

In parallel mode, Fast is expected to finish before Slow and sooner than Fast
finished in single mode. These properties are covered by
[GlobalOrderingScenarioTest](../src/test/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/GlobalOrderingScenarioTest.java#L23).

## Why this uses a separate Spring Boot application

[GlobalOrderingEnvironment](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/GlobalOrderingEnvironment.java#L31)
starts an isolated scenario Spring context. Unique topic and group names are
created for each start, preventing old offsets and records from affecting a new
learning run. Stop closes the context and all its listener containers.

The mediator does not play an invented Kafka animation. It receives observations
from the scenario application, and
[GlobalOrderingScenarioStarter](../src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/GlobalOrderingScenarioStarter.java#L54)
converts them into a timeline: publisher, actual partition, assigned consumer,
status, offset, and completion time.

## What the scenario does not prove

It does not compare maximum throughput and is not a benchmark: artificial delays,
local Kafka, and a small sample are unsuitable for that. It also does not promise
that ordinary hashing will place two business keys in different partitions.
Different keys can collide in one shard.

The narrower, useful conclusion is that a partition is the unit of ordering and
parallelism. Preserve order where it has business meaning, and avoid an
unnecessary global queue for independent entities. If a system requires a single
total order, it must use one partition or build a separate coordination mechanism,
accepting reduced scalability and greater complexity.

The scenario covers backlog `#12 Why global ordering does not exist across partitions`.
