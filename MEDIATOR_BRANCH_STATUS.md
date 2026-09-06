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

What is still intentionally missing:

- starting external Spring Boot scenario apps
- starting/stopping Kafka containers
- cleaning external resources between scenarios
- starter/environment implementations for scenarios `004` and later

Most likely next step:

- implement one partition/two consumers/one group as the next real experiment
- observe assignment and the idle second consumer
- only after that introduce external process/container orchestration
