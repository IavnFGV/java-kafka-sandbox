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
2. State the question or behavior being explored.
3. Let the user predict what will happen.
4. Run a real Kafka-backed experiment.
5. Visualize messages, component state, partition ownership, or failures.
6. Explain why Kafka behaved that way.
7. Include a failure or edge-case variant where it adds value.
8. Finish with a concise interview-ready takeaway.
9. Reference the before and after commits so the implementation change is easy to demonstrate.

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

The next milestone is `003 Topic, Partition, Offset`: expose the internal Kafka
shape introduced by the broker and topic containers and turn the existing static
explanation into a real experiment.
