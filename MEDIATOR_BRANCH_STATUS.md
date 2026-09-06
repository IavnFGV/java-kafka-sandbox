# Mediator Branch Status

Branch:

- `feature/mediator-scenario-starters`

Current architecture:

- `io.drozda.sandbox.mediator`
  - mediator-facing commands
  - environment lifecycle status
  - central `ScenarioMediatorService`
- `io.drozda.sandbox.scenario.spi`
  - `ScenarioStarter`
  - `ScenarioEnvironment`
- `io.drozda.sandbox.scenario.systemready`
  - `SystemReadyScenarioStarter`
  - `SystemReadyEnvironment`
  - `SystemReadyProbe`
  - `app/SystemReadyScenarioApplication`
  - `app/SystemReadyScenarioController`

What works now:

- mediator can list scenario commands
- mediator can execute `system-ready` baseline command
- mediator can `start/stop/reset/status` the `system-ready` environment
- `system-ready` environment starts a separate Spring Boot scenario app on a random local port
- mediator sends HTTP commands to that scenario app
- controller exposes command and environment endpoints
- tests cover mediator command execution and environment lifecycle
- UI Play runs the default backend command
- UI Stop closes the scenario environment and restores baseline topology
- runtime events are retained in a per-scenario browser log until reload
- runtime changes reach the browser through versioned long polling
- container nodes can be resized and keep child nodes inside their bounds
- `trade-flow` starts an isolated Spring context with scenario-owned Kafka components
- `trade-flow` publishes a unique event and confirms matching listener receipt
- Kafka broker containers can hold topic nodes and later partition nodes
- `topic-partition-offsets` appends records to two explicit partitions
- one scenario consumer receives both partitions and verifies their local offsets
- `key-partitioning` verifies that one business key maps to one partition without explicit partition selection
- mediator commands accept scenario-specific string parameters without coupling the mediator to a concrete scenario
- `key-partitioning` lets the learner choose no key, a unique event key, or stable order ID; only order ID satisfies the ordering-affinity goal
- `key-partitioning` runs three concurrent consumers in one group, captures real rebalance assignments, and animates each record through its partition to the consumer that received it

What is still intentionally missing:

- starting external Spring Boot scenario apps
- starting/stopping Kafka containers
- scenario-owned Kafka cleanup (`Clean`: delete and recreate topics via Admin API)
- full Kafka container/volume reset as a separate destructive maintenance action
- starter/environment implementations for scenarios `005` and later

Most likely next steps:

- add the probability/sample-size explanation to scenario `004`
- implement `005 Ordering Within One Partition` for backlog item #11
- prove the observed order of related records after key-based routing
- only after that introduce external process/container orchestration
