# Kafka 100: Patterns, Problems, and Interview Traps

This file is the working backlog for turning this repository into a practical Kafka training lab.

Goal:
- move from “basic Kafka familiarity” to “I can explain, test, debug, and visualize Kafka behavior”
- practice through Spring Boot integration tests plus the custom visualizer
- cover both real production problems and common interview topics

How to use this list:
- treat each item as a candidate scenario or test
- for each item, aim to add:
  - a short explanation
  - an integration test
  - optional visualizer support
  - notes about failure modes and tradeoffs

## Scenario Learning Roadmap

| Scenario | Status | Practical focus | Backlog items |
|---|---|---|---|
| `001 System Ready` | Implemented | Distinguish Spring wiring failures from Kafka runtime failures | Foundation |
| `002 Trade Event Flow` | Implemented | Decouple a producer from asynchronous consumers through a durable event log | #1 |
| `003 Topic, Partition, Offset` | Implemented | Scale a stream into ordered shards and address consumer progress precisely | #2; introduces #11, #12 |
| `004 Message Key and Partition Selection` | Implemented | Keep related entity events in the same ordered shard | #10 |
| `005 Ordering Within One Partition` | Implemented | Preserve the order of related events | #11 |
| `006 Parallel Orders Without Global Ordering` | Implemented | Remove head-of-line blocking while preserving per-order sequence | #12 |
| `007 One Partition, Two Consumers` | Implemented | Observe one active owner, one idle member, and takeover after owner shutdown | #13, #14 |
| `008 Two Partitions, Two Consumers` | Implemented | Scale one service by distributing two partition owners across two group members | #13, #14 |
| `009 Multiple Consumer Groups` | Implemented | Let independent services process the same event stream | #15 |
| `010 Earliest vs Latest` | Implemented | Control where a new group begins reading | #16 |
| `011 Rebalance on Join` | Static UI | Understand ownership changes during deploys and autoscaling | #41, #94 |
| `012 Consumer Failure` | Static UI | Recover partition processing after an instance disappears | #13, #41, #97 |

The mapping is many-to-many: one scenario may cover several backlog questions,
and one question may require several scenarios. Keep `ScenarioGraph.backlogItems`
and this table synchronized when scenarios are implemented or reordered.

## Consolidated Scenario Route

Backlog numbers remain a knowledge index, not a one-scenario-per-question plan.
Build one experiment when several questions describe one causal chain:

| Laboratory series | Candidate experiments | Backlog coverage |
|---|---:|---|
| Complete core group route | 4 | #15, #16, #41, #42, #94, #97 |
| Producer durability and throughput | 5 | #3-9, #21-30, #91-93, #99 |
| Commits, delivery, and idempotency | 5 | #31-37, #42-45, #59-60 |
| Retry, poison records, and DLT | 5 | #38-40, #46-58 |
| Transactions and exactly-once boundaries | 4 | #61-70, #81-82 |
| Retention and compacted state | 3 | #17-20, #87-90 |
| Schema evolution | 4 | #71-80 |
| Distributed workflows | 4 | #81-90 |
| Operations and cluster behavior | 4 | #91-100 |

This route is intentionally approximate and overlapping. A scenario is complete
only when it has a practical problem, real execution, assertions, visualization,
failure boundary, and article. Prefer extending a coherent experiment over adding
a nearly identical card solely to claim another backlog number.

Suggested priority:
- `P1` = core foundations, do first
- `P2` = highly practical, do next
- `P3` = advanced architecture and scaling

Reference sources used to curate this list:
- Apache Kafka docs: https://kafka.apache.org/documentation/
- Kafka design docs: https://kafka.apache.org/43/design/design/
- Confluent delivery guarantees: https://docs.confluent.io/kafka/design/delivery-semantics.html
- Confluent guarantees course: https://developer.confluent.io/courses/architecture/guarantees/
- Confluent DLQ guide: https://www.confluent.io/learn/kafka-dead-letter-queue/
- Kafka Streams core concepts: https://kafka.apache.org/33/streams/core-concepts/
- Saga pattern: https://microservices.io/patterns/data/saga.html

## 1. Core Kafka Foundations

1. `P1` Topic vs queue mental model
2. `P1` Topic, partition, offset basics
3. `P1` Leader and follower replicas
4. `P1` Replication factor and durability tradeoff
5. `P1` `acks=0|1|all`
6. `P1` `min.insync.replicas`
7. `P1` Producer retries
8. `P1` Batching and linger
9. `P1` Compression basics
10. `P1` Message key and partition selection - route related entity events into one partition (`004`)
11. `P1` Ordering guarantees within a partition - verify send order, offsets, and consume order (`005`)
12. `P1` Why global ordering does not exist across partitions - compare one ordered queue with two parallel ordered shards (`006`)
13. `P1` Consumer group mechanics - observe assignment and takeover after membership changes (`007`)
14. `P1` One partition assigned to one consumer in a group - prove that a second group member remains idle (`007`)
15. `P1` Multiple consumer groups reading the same topic
16. `P1` Auto offset reset: `earliest` vs `latest`
17. `P1` Retention by time
18. `P1` Retention by size
19. `P1` Log compaction
20. `P1` Tombstone records

## 2. Producer Reliability and Throughput

21. `P1` Idempotent producer
22. `P1` Duplicate production without idempotence
23. `P1` Retry plus ordering risk
24. `P1` Throughput vs latency tuning
25. `P2` Large messages and request sizing
26. `P2` Producer backpressure under slow broker
27. `P2` Broker unavailable during send
28. `P2` Not enough in-sync replicas
29. `P2` Safe producer config for strong durability
30. `P2` Fast producer config for non-critical events

## 3. Consumer Semantics and Offset Control

31. `P1` Auto commit vs manual commit
32. `P1` Commit before processing
33. `P1` Commit after processing
34. `P1` At-most-once delivery
35. `P1` At-least-once delivery
36. `P1` Why duplicates happen
37. `P1` Idempotent consumer pattern
38. `P2` Poison pill record
39. `P2` Consumer deserialization failure
40. `P2` Long-running consumer and poll interval issues
41. `P2` Rebalance during processing
42. `P2` Consumer lag and what it means
43. `P2` Pausing and resuming consumption
44. `P2` Slow consumer vs hot partition
45. `P2` Parallel processing inside one consumer

## 4. Failure Handling Patterns

46. `P1` Retry in-place inside listener
47. `P1` Retry topic pattern
48. `P1` Exponential backoff retry
49. `P1` Dead letter topic pattern
50. `P1` When DLQ is appropriate
51. `P2` When DLQ breaks ordering expectations
52. `P2` Parking-lot topic
53. `P2` Replay from DLQ
54. `P2` Error classification: transient vs permanent
55. `P2` Skip bad record and continue
56. `P2` Stop the consumer on critical failure
57. `P2` Circuit breaker around downstream dependency
58. `P2` Kafka available but database unavailable
59. `P2` Consumer restart and replay behavior
60. `P2` Duplicate after retry or restart

## 5. Transactions and Exactly-Once

61. `P1` What “exactly-once” really means in Kafka
62. `P1` Idempotence vs transactions
63. `P1` Read-process-write transaction flow
64. `P1` Consumer isolation level
65. `P2` Aborted transactions
66. `P2` Why EOS does not solve every duplication problem
67. `P2` External DB side effects and EOS boundary
68. `P2` Transaction timeout behavior
69. `P2` Producer fencing
70. `P2` Transactional outbox vs Kafka transactions

## 6. Data and Schema Evolution

71. `P2` JSON without schema discipline
72. `P2` Avro or schema-based messaging
73. `P2` Backward compatibility
74. `P2` Forward compatibility
75. `P2` Breaking schema change
76. `P2` Event versioning strategy
77. `P2` Null fields vs missing fields
78. `P2` Consumer handling of unknown fields
79. `P2` Schema evolution in mixed-version systems
80. `P2` Topic per event type vs shared topic

## 7. Distributed Architecture Patterns

81. `P2` Transactional outbox pattern
82. `P2` Polling publisher vs CDC outbox
83. `P2` Saga choreography
84. `P2` Saga orchestration
85. `P2` Compensating action design
86. `P3` CQRS read model update
87. `P3` Event sourcing basics
88. `P3` Rebuild state by replay
89. `P3` Command topic vs event topic
90. `P3` Audit trail and immutable events

## 8. Scaling, Ops, and Interview Headaches

91. `P2` Hot key / hot partition
92. `P2` Choosing partition count
93. `P2` Adding partitions and ordering impact
94. `P2` Rebalance storms
95. `P2` Monitoring broker health
96. `P2` Monitoring consumer lag
97. `P2` Detecting stuck consumers
98. `P3` Multi-cluster thinking and replication
99. `P3` Throughput tuning under pressure
100. `P3` Designing Kafka for an interview system design round

## Suggested First 20 to Build in This Repository

These are the best first scenarios for this project:

1. Topic, partition, offset
2. Key-based partitioning
3. Ordering within one partition
4. Consumer group behavior
5. Manual commit after processing
6. At-least-once duplicate scenario
7. Idempotent consumer
8. Retry with backoff
9. Dead letter topic
10. Poison pill record
11. Kafka down during publish
12. Broker reachable vs not reachable visualization
13. Consumer lag simulation
14. Rebalance on second consumer start
15. Idempotent producer
16. Transactional producer basics
17. Outbox pattern
18. Saga choreography mini-example
19. Schema evolution breaking example
20. Hot partition caused by bad key choice

## Visualizer Mapping Ideas

The visualizer should eventually be able to show:
- component ready
- component busy
- component failed
- signal started
- signal delivered
- signal retried
- signal dead-lettered
- consumer lag growing
- rebalance happening
- partition ownership changing

## Playground Infrastructure Backlog

- Add code-guided scenario nodes: clicking a graph node should open a compact source panel
  with practical guidance and explicit `sourceReferences` supplied by the backend.
- Keep source navigation structured rather than deriving paths from node labels. Each reference
  should contain a label, repository-relative path, optional line anchor, and a short explanation
  of why that code matters in the current scenario.
- Let the source panel offer two actions: inspect a small backend-served snippet without leaving
  the experiment, and open the repository permalink when one is configured. Avoid depending on
  browser-specific local IDE URL schemes as the only navigation mechanism.
- Add source references incrementally when a scenario is completed; at minimum link producer,
  listener, experiment/orchestrator, tracker, and the integration test.

- Implemented in `a86c50b`: the backend retains scenario transitions instead of exposing
  only the latest runtime snapshot. Every event has a monotonic sequence and immutable
  `before`/`after` states; long polling returns all retained events after the client cursor.
- Implemented in `ee84560`: the browser queues transitions and replays them independently
  of Kafka execution speed. Selecting a step restores its preceding state, animates the
  transition, and pauses on its resulting state; `Previous`, `Replay`, `Next`, and
  `Play all` navigate the captured run.
- Implemented in `d2a38f2`: backend flags distinguish visible and animated transitions; the primary
  track numbers only animated frames, folds technical updates into their final state,
  and can reveal the raw technical sequence on demand.
- Timeline follow-up: report an explicit cursor gap if a client falls behind the bounded
  backend retention window of 1,000 transitions.
- Add an explicit `Clean` action separate from `Stop`.
- `Stop` must only close the scenario Spring context and keep Kafka data intact.
- `Clean` should stop the scenario, delete its owned topics through Kafka Admin API,
  wait for deletion to complete, and recreate the declared topology.
- Keep full Kafka container/volume reset as a separate destructive maintenance action.
- Show the cleanup lifecycle and failures in the runtime log.
- Add integration tests proving that a cleaned topic starts again with empty partition logs.

## Build Order Recommendation

Phase 1:
- items 1-10

Phase 2:
- items 11-20

Phase 3:
- items 21-60

Phase 4:
- items 61-100

## Definition of Done for One Topic

For each topic, try to leave behind:
- one practical problem statement explaining why the behavior matters
- explicit references to every covered backlog item
- one integration test
- one short note in `learning_notes.md`
- one visual scenario or runtime event sequence
- one “what can go wrong” note
- one interview-style explanation in plain language
