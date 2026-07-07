# Mediator Direction

This branch starts a package-level separation between:

- `io.drozda.sandbox.mediator`
- `io.drozda.sandbox.scenario.spi`
- `io.drozda.sandbox.scenario.systemready`

## Current Scope

The current mediator is intentionally small:

- expose scenario commands
- execute scenario starters through a stable interface
- keep scenario orchestration out of JUnit tests

The first starter is `system-ready`.

It verifies the presence of the baseline Spring Boot beans through a dedicated probe:

- `TradeEventPublisher`
- `TradeEventListener`
- `KafkaTemplate`

and then emits visual runtime updates for the `001 System Ready` scenario.

## What Is Explicitly Deferred

The following ideas are intentionally documented but not implemented yet:

- start/stop Kafka containers
- start/stop separate scenario Spring Boot apps
- Docker orchestration from the mediator
- cleanup of external infrastructure per scenario

These will come later, after the mediator contract and scenario starter model are stable.
