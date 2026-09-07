# 009. Multiple Consumer Groups

[Русский](009-multiple-consumer-groups.ru.md)

## Why this matters

In scenario 008, two consumers belonged to one group. They cooperated: Kafka
split two partitions between them, so the group processed each record once.

Scenario 009 answers a different practical question: how can several independent
services react to one event stream? For example, after an order is created, an
audit service must preserve its history, and a notification service must send an
email. Neither should “steal” the message from the other.

These services use different `group.id` values. Kafka maintains a separate read
position for each consumer group. As a result, `audit-group` and
`notification-group` independently receive all three records from one topic.
Kafka does not physically create a second copy of the record: both groups read
the same `topic-partition-offset` coordinate but maintain their own processing progress.

## What the experiment does

An isolated Spring Boot application creates a topic with one partition.
[`SharedOrderPublisher`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/producer/SharedOrderPublisher.java#L17) publishes three unique events just once. Two
`@KafkaListener` methods subscribe to the same topic using different groups:

- [`AuditGroupConsumer`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/consumer/AuditGroupConsumer.java#L15) runs in `audit-group`;
- [`NotificationGroupConsumer`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/consumer/NotificationGroupConsumer.java#L15) runs in `notification-group`.

[`MultipleConsumerGroupsTracker`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsTracker.java#L27) waits for six callbacks: three from each group.
[`MultipleConsumerGroupsExperiment`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsExperiment.java#L31) then verifies that both groups observed the
same partition and offset for each `eventId`. In the UI, one animation shows the
append to Kafka, after which the record fans out to two independent consumer groups.

The two deliveries share a `playbackGroup`. The timeline preserves all backend
events but combines signals with the same group into one visual frame. The UI
can therefore show several simultaneous actions while preserving their order
in the technical journal. An active signal now draws both a moving dot and a
complete dashed path with an arrow between node boundaries.

## Practical takeaway

A consumer group defines both scaling and a logical subscription:

- the same `group.id` means cooperative processing and partition sharing;
- different `group.id` values mean independent delivery of the full stream to each group;
- one group's offsets do not affect another's.

This lets audit, notification, analytics, and other services subscribe to one
event stream without changing the producer.

## Before and after

Before the scenario: `9244dba`.

After the scenario: `e6ec185`.

Visualization improvements: `4533a72` added arrows and simultaneous runtime
signals; `5a1bc3a` combined those signals into one `PARALLEL` timeline card and
separated `append` from the subsequent fan-out.

## Core implementation

- [`MultipleConsumerGroupsExperiment`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsExperiment.java#L59) — Verifies delivery to both groups and matching coordinates.
