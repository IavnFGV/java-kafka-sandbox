# Learning Notes

## Kafka

### Session Log

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

## ETL Mapping

## AWS Basics

## Adaptiq Pitch
