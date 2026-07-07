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

What is still intentionally missing:

- starting external Spring Boot scenario apps
- starting/stopping Kafka containers
- cleaning external resources between scenarios
- multi-scenario starter/environment implementations beyond `system-ready`

Most likely next step:

- add the second scenario as a real starter/environment pair
- keep it in-process first
- only after that introduce external process/container orchestration
