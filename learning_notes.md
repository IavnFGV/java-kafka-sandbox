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

## ETL Mapping

## AWS Basics

## Adaptiq Pitch
