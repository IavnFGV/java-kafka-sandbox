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
- container nodes can be resized and keep child nodes inside their bounds

What is still intentionally missing:

- starting external Spring Boot scenario apps
- starting/stopping Kafka containers
- cleaning external resources between scenarios
- multi-scenario starter/environment implementations beyond `system-ready`

Most likely next step:

- make the next scenario publish a real Kafka record and verify listener receipt
- use that experiment to establish honest broker/topic/listener readiness events
- only after that introduce external process/container orchestration
