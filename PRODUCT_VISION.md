# Kafka Scenario Lab

## Product Idea

Kafka Scenario Lab is an interactive refresher for Java developers who want to
understand Kafka behavior through observation instead of memorizing definitions.

A user should be able to clone the repository, start the local environment, and
work through increasingly difficult scenarios before an interview or while
learning Kafka without production experience.

## Scenario Experience

Each completed scenario should:

1. Show the initial system topology.
2. State the practical problem this Kafka feature solves.
3. Reference every related item from `KAFKA_100_PROBLEMS_AND_PATTERNS.md`.
4. Let the user predict what will happen.
5. Run a real Kafka-backed experiment.
6. Visualize messages, component state, partition ownership, or failures.
7. Explain why Kafka behaved that way.
8. Include a failure or edge-case variant where it adds value.
9. Finish with a concise interview-ready takeaway.
10. Reference the before and after commits so the implementation change is easy to demonstrate.

The scenario-to-backlog relationship is many-to-many. The backend stores the
practical purpose and backlog item numbers in `ScenarioGraph`; the UI displays
them before the learner starts the experiment.

The UI is not intended to become a general Kafka administration console. Actions
remain constrained by the selected scenario so that every interaction teaches a
specific concept and can be reset to a known state.

## Learning Progression

The first scenarios establish the mental model:

- application, producer, consumer, and broker roles
- topics, partitions, and offsets
- keys and ordering
- consumer groups and partition assignment
- rebalancing and consumer failure

Later scenarios cover operational and reliability behavior:

- offset commits and delivery guarantees
- duplicate handling and idempotent consumers
- retries, poison records, and dead-letter topics
- broker failures and consumer lag
- transactions, outbox, schema evolution, and scaling

The detailed backlog lives in `KAFKA_100_PROBLEMS_AND_PATTERNS.md`.

## Architecture Direction

- The browser renders scenario topology and runtime events.
- The mediator starts, commands, resets, and stops scenario environments.
- Scenario applications contain the Kafka behavior being studied.
- Kafka experiments should produce runtime facts; the visualizer should display
  those facts rather than pretend that scripted animation is proof of behavior.
- Tests validate the mediator and scenario implementations, but JUnit is not the
  user-facing scenario runner.

## Current Milestone

The repository currently has seven ordered visual scenarios. `001 System Ready`
checks the Kafka-facing Spring wiring. `002 Trade Event Flow` starts its own
isolated Spring Boot context, publishes a real record, captures Kafka partition
and offset metadata, and confirms that its listener receives the matching event.
`003 Topic, Partition, Offset` appends records to two explicit partitions and
verifies their partition-local offsets on both producer and consumer sides.
`004 Message Key and Partition Selection` sends related order events with the
same key and verifies that Kafka routes them to one partition.

The next milestone is `005 Ordering Within One Partition`: verify the ordering
guarantee for related records after key-based routing.
