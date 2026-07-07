# Mediator Direction

This branch starts a package-level separation between:

- `io.drozda.sandbox.mediator`
- `io.drozda.sandbox.scenario.spi`
- `io.drozda.sandbox.scenario.systemready`

## Current Scope

The current mediator is intentionally small:

- expose scenario commands
- expose scenario environment lifecycle
- execute scenario starters through a stable interface
- keep scenario orchestration out of JUnit tests

The first starter is `system-ready`.

The first environment is also `system-ready`.

It starts a dedicated Spring Boot scenario app on an ephemeral port and verifies the presence of the baseline Spring Boot beans there through a dedicated probe:

- `TradeEventPublisher`
- `TradeEventListener`
- `KafkaTemplate`

and then emits visual runtime updates for the `001 System Ready` scenario.

At this stage the environment is already separated from the mediator command flow:

- mediator starts the `system-ready` scenario app
- mediator calls scenario-app HTTP endpoints
- mediator stops and resets that scenario app through an environment contract

What is still missing is external orchestration of Kafka containers and fully separate JVM/container lifecycle beyond this first in-repo scenario app.

## What Is Explicitly Deferred

The following ideas are intentionally documented but not implemented yet:

- start/stop Kafka containers
- start/stop separate scenario Spring Boot apps
- Docker orchestration from the mediator
- cleanup of external infrastructure per scenario

These will come later, after the mediator contract and scenario starter model are stable.
