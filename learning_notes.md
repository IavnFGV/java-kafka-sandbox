# Learning Notes

## Kafka

### Session Log

#### 2026-09-07 — Scenario Package Naming

- Decision: scenario package names follow the full learning scenario meaning, not catalog numbers or abbreviated implementation terms
- Current packages: `systemready`, `tradeeventflow`, `topicpartitionoffsetbasics`, `messagekeypartitionselection`, `orderingwithinonepartition`, `parallelorderswithoutglobalordering`, `onepartitiontwoconsumersonegroup`
- Reason: catalog order may change, while a semantic package name keeps ownership obvious in code and avoids mass renumbering
- Isolation fix: scenarios 002-005 now create unique topics as well as unique groups, so historical JSON type headers cannot break runs after Java package refactoring

#### 2026-09-07 — Code-Guided Nodes Direction

- Future UX: clicking a topology node opens its relevant source locations and practical implementation notes
- Architecture decision: add structured backend `sourceReferences` to scenario nodes instead of guessing Java paths from UI labels
- Preferred flow: inspect a short source snippet in a side panel, then optionally follow a stable repository permalink
- Scenario definition of done should eventually link the producer, listener, experiment, tracker, and integration test

#### 2026-09-07 — Scenario 008

- Topic: two partitions assigned across two consumers in one group
- Behavior reproduced: real assignment gave P0 and P1 different owners; six records were consumed only by the owner of their partition
- Practical takeaway: partitions are the group parallelism slots, while consumer count alone does not create work
- Boundary: equal partition and consumer counts do not guarantee balanced traffic or processing cost
- Files to revisit: `ParallelGroupExperiment`, `ParallelGroupTracker`, `ParallelGroupConsumerA`, `ParallelGroupConsumerB`, `ParallelGroupScenarioStarter`
- Implementation commit: `4b9f5bc`
- Route decision: group the remaining 100-question backlog into causal laboratory series rather than creating one card per question
- Next scenario: multiple consumer groups reading the same topic, covering backlog #15

#### 2026-07-07

- Topic: consumer groups and rebalance
- Goal: understand how Kafka distributes partitions across consumers in the same group, and what changes when a second consumer joins
- Expected visualizer view: one consumer first owns all partitions, then ownership is redistributed after another consumer joins the same group
- Files to watch:
  - `KAFKA_100_PROBLEMS_AND_PATTERNS.md`
  - `VISUALIZER_CONTEXT.md`
  - `src/main/java/io/drozda/sandbox/visualization/ScenarioCatalog.java`
  - test files we add for consumer-group behavior
- Practical takeaway: consumer scaling is really partition assignment, not "every consumer gets every message"
- Interview takeaway: inside one consumer group, one partition can be assigned to only one consumer at a time

### Quick Refresh

- Consumer group = a set of consumers cooperating on one logical subscription
- Different consumer groups can each read the same topic independently
- Inside the same group, Kafka balances partitions across consumers
- Ordering is preserved per partition, not across the whole topic
- Rebalance happens when consumers join, leave, or partitions change

#### 2026-09-06

- Topic: producer acknowledgement versus end-to-end delivery
- Behavior reproduced: an isolated scenario app published a unique event, Kafka returned partition and offset, and a listener received the matching `eventId`
- Files to revisit: `TradeFlowEnvironment`, `TradeFlowExperiment`, `TradeFlowPublisher`, `TradeFlowListener`
- Practical takeaway: a successful producer send confirms Kafka acceptance, not consumer processing
- Visual model: Kafka broker is a container; topics and later partitions live inside the Kafka side of the topology

#### 2026-09-06 — Scenario 003

- Topic: topic, partition, and offset coordinates
- Behavior reproduced: records A and C were appended to partition 0, while B was appended to partition 1
- Consumer behavior: one listener subscribed to the topic and was assigned both partitions
- Verification: producer `SendResult` coordinates matched listener `ConsumerRecord` coordinates
- Practical takeaway: offsets are local to a partition; `topic + partition + offset` identifies a record
- Interview takeaway: a consumer normally subscribes to a topic, then its group receives partition assignments

#### 2026-09-06 — Learning Route Decision

- Problem found: the original visual scenario order jumped from partition basics directly to consumer-group failures while earlier P1 producer concepts were still missing
- Decision: finish the producer/partition mental model before consumer groups
- Next scenario: `004 Message Key and Partition Selection`, covering backlog #10
- Planned sequence: `005` ordering inside one partition; `006` no global ordering; `007–010` consumer groups and offset reset; `011–012` rebalance and failure
- Existing static consumer-group scenarios were renumbered but kept their stable scenario IDs
- Rule: every implementation must state its practical purpose and maintain explicit many-to-many backlog mapping in the backend, UI, article, and roadmap

#### 2026-09-06 — Scenario 004

- Topic: message key and automatic partition selection
- Behavior reproduced: CREATED, PAID, and SHIPPED for `order-42` reached one partition without an explicit partition argument
- Important boundary: a different key may still collide in the same partition; Kafka does not promise one partition per key
- Practical takeaway: use a stable business key when related events need one ordered shard
- Files to revisit: `KeyedOrderEventPublisher`, `KeyPartitioningExperiment`, `KeyPartitioningScenarioStarter`
- Interactive check: the UI now asks for `No key`, `Unique eventId`, or `Order ID`; only the stable business key completes the learning goal
- Consumer view: three concurrent consumers form one group, and the UI shows their actual partition assignments and each record's delivery path
- Spring detail: `@KafkaListener(concurrency = "3")` creates three child listener containers with separate Kafka consumers and poll threads; thread identity is used only as sandbox telemetry to correlate assignments and records
- Rebalance observation: during startup the first consumer may temporarily own all partitions; the experiment must wait for stable assignment and process partition revocations
- Ordering insight: a shared database does not remove the problem because consumers from different partitions may finish updates out of order
- Next 004 enhancement: quantify the chance of accidentally observing one partition with an unsafe strategy; do not use Student's t-test for this categorical routing experiment
- Probability check implemented: with 10 independently hashed event IDs and 3 partitions, accidental same-partition placement is `(1/3)^9`, about `0.0051%`; no-key sticky partitioning is not modeled by this formula

#### 2026-09-06 — Scenario 005

- Topic: ordering guarantees within one partition
- Behavior reproduced: sequence `0..5` used one business key, one partition, increasing offsets, and the same listener callback order
- Technical detail: one shared future completes with records appended in callback order; per-event futures would hide the observed order
- Boundary: Kafka preserves partition/poll order, not completion order after application-level parallel dispatch
- Files to revisit: `PartitionOrderingExperiment`, `OrderedEventTracker`, `OrderedEventListener`, `PartitionOrderingScenarioStarter`

#### 2026-09-07 — Scenario 006

- Topic: parallel ordered shards do not form one globally ordered stream
- Behavior reproduced: one partition and one consumer made Fast Order wait behind Slow Order; two partitions and two consumers let Fast Order complete independently
- Preserved contract: each order retained sequence `0..N` even though the global completion order differed from publish order
- Practical takeaway: partitions remove head-of-line blocking for independent entities, but Kafka ordering ends at the partition boundary
- Experiment detail: explicit partitions make the comparison deterministic; production routing should normally use the stable `orderId` key
- Files to revisit: `GlobalOrderingExperiment`, `GlobalOrderTracker`, `GlobalOrderListener`, `GlobalOrderingScenarioStarter`
- Implementation commit: `50cc164`
- Next scenario: `007 One Partition, Two Consumers`, demonstrating why consumer count above partition count does not add throughput

#### 2026-09-07 — Scenario 007

- Topic: one partition with two consumers in one group
- Behavior reproduced: Kafka assigned Partition 0 to exactly one consumer while the other healthy member remained idle
- Failure behavior: the experiment stopped the actual owner by listener-container ID; after rebalance the former idle member took over and processed the next batch
- Practical takeaway: consumers beyond the partition count add no throughput, though they can provide warm standby capacity
- Spring detail: separate `@KafkaListener` IDs plus `KafkaListenerEndpointRegistry` allow one group member to be stopped without closing the scenario application
- Correctness detail: the experiment observes the real owner instead of assuming Consumer A wins startup assignment
- Files to revisit: `SinglePartitionGroupExperiment`, `SinglePartitionGroupTracker`, `ConsumerMemberControl`, `ConsumerGroupMemberA`, `ConsumerGroupMemberB`
- Implementation commit: `f04011c`
- Next scenario: `008 Two Partitions, Two Consumers`, focusing on steady-state work distribution without the failure step

#### 2026-09-06 — Visualizer Timeline Improvement

- Problem found: every long-poll update currently triggers a complete SVG render, so an active signal animation restarts; if the backend advances through several states quickly, a latest-snapshot API can also hide intermediate learning steps entirely
- Goal: preserve every meaningful scenario transition while allowing Kafka and Spring to run at their real speed and the browser to replay the result at a slower teaching speed
- Backend direction: append immutable timeline events with a monotonic `sequence`, explanatory title, `stateBefore`, transition/signals, and `stateAfter`; long polling must return all events after the client's sequence cursor rather than only the newest snapshot
- UI direction: queue received events, restore `stateBefore`, animate the transition, stop on `stateAfter`, and retain the completed timeline until page reload
- Planned controls: clicking a step replays only that transition and pauses; `Previous`, `Replay`, `Next`, and `Play all` navigate the captured run
- Important boundary: a frontend queue alone cannot recover states that the backend never returned; lossless capture must therefore be implemented backend-first
- Scope rule: only pedagogically meaningful transitions belong in the timeline; internal polling and incidental status changes remain technical telemetry
- Current temporary adjustment: moving signal dots take `3s` instead of `1.4s`, but full SVG rerenders can still restart them until timeline playback is implemented
- Implemented backend in commit `a86c50b`: `ScenarioRuntimeService` now stores each transition with monotonic sequence and `before`/`after` snapshots; one long-poll response can carry every retained event after the browser cursor
- Implemented UI in commit `ee84560`: each browser tab starts at the current journal head, records only new transitions, queues playback per scenario, and retains the history until reload
- Playback behavior: selecting a card pauses autoplay, renders the previous snapshot, then renders and animates the selected transition; navigation supports `Previous`, `Replay`, `Next`, and `Play all`
- Verification: `ScenarioRuntimeServiceTest` proves that three backend transitions produced before the request is read are returned in order; the complete Gradle test suite passed
- Remaining refinement: visible cards currently include all runtime publications, including node-detail updates; later add an explicit learning-step classification rather than guessing in JavaScript
- Retention boundary: the in-memory backend journal keeps 1,000 transitions; later expose an explicit gap indicator when a stale cursor falls behind that window
- Timeline refinement implemented in `d2a38f2`: backend events now carry `visibleInTimeline` and `animated`; only animated learning frames receive `001`, `002`, ... numbers, while important static transitions use `STATE`
- Technical updates are retained and folded into the preceding frame's final `after` state; the UI can reveal their raw sequence with `Technical events`
- Layout refinement: the timeline is fixed to the bottom like a video editor track; runtime log and legend moved to modal dialogs to keep the graph workspace focused

## ETL Mapping

## AWS Basics

## Adaptiq Pitch
## Scenario 009: Multiple Consumer Groups

- One physical Kafka record can be consumed independently by multiple consumer groups.
- Consumers sharing a `group.id` divide partitions; consumers with different `group.id` values each receive the complete stream.
- Every group owns independent committed offsets, even though all groups observe the same topic-partition-offset coordinates.
- Scenario package: `io.drozda.sandbox.scenario.multipleconsumergroups`.
- Runtime events can share a `playbackGroup`; the browser combines them into one frame for simultaneous animation while retaining every backend event.
- Next group-focused scenario: start one consumer with both partitions, add a second consumer, and observe the real rebalance.

## Scenario 010: Earliest vs Latest

- `auto.offset.reset` applies only when a consumer group has no valid committed offset.
- A new `earliest` group replays retained history; a new `latest` group starts after the log end observed on assignment.
- The experiment publishes history before starting manually controlled listeners, waits for both assignments, then publishes live records.
- Expected offsets are `0..4` for earliest and `3..4` for latest.
- Every run uses a new topic and new group IDs so repeated playback demonstrates new-group behavior.
- Scenario package: `io.drozda.sandbox.scenario.earliestvslatest`.

## Scenario 011: Rebalance When a Second Consumer Joins

- One consumer initially owns both partitions; starting a second member changes group membership and triggers a real rebalance.
- Ownership is derived from `onPartitionsRevoked` and `onPartitionsAssigned`, never hard-coded in the UI.
- Records published before and after join verify that delivery follows the ownership map in each phase.
- Rebalance enables scaling but introduces a coordination pause and can become operationally expensive when triggered repeatedly.
- Scenario package: `io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins`.

## Scenario 012: Consumer Failure and Partition Takeover

- Two consumers initially own one partition each; after one listener stops, the survivor owns both.
- A record appended while its partition has no stable owner remains in Kafka and is consumed after reassignment.
- The controlled listener stop demonstrates takeover but is faster than crash detection based on missing heartbeats.
- Rebalance can create temporary lag and at-least-once delivery means idempotent processing remains important.
- Scenario package: `io.drozda.sandbox.scenario.consumerfailureandpartitiontakeover`.

## Coverage Checkpoint After Scenario 012

- Explicit coverage: 12 of 100 unique Kafka backlog questions.
- Covered IDs: #1, #2, #10-#16, #41, #94, #97.
- Remaining: 88 questions, split into 31 P1, 49 P2, and 8 P3 items.
- Coverage is counted only from implemented roadmap mappings, not from incidental mentions.

## GitHub Source Navigation

- Every implemented `ScenarioGraph` exposes a repository-relative `sourceRoot` owned by the backend catalog.
- Clicking a graph node opens its description and a stable GitHub link to the scenario package on `main`.
- Drag and click are distinguished by pointer movement so rearranging the graph does not open the source dialog.
- A catalog test verifies that every configured source directory exists after package refactoring.
- Future refinement: add node-level file and line references for producer, listener, tracker, and experiment classes.
