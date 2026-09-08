# Visualizer Architecture

This guide describes the implemented architecture on `main`. The
[scenario index](docs/README.md) contains all 12 scenarios and their two supplementary
articles; [the roadmap](KAFKA_100_PROBLEMS_AND_PATTERNS.md) separates implemented
coverage from future experiments.

For a step-by-step code walkthrough using scenarios 001 and 002, read
[How the platform works (RU)](docs/platform-architecture.ru.md) /
[English](docs/platform-architecture.en.md), also accessible from the site header.

## Execution path

1. The browser loads the catalog and selected scenario from `/api/scenarios`.
2. Play sends `POST /api/scenarios/{scenarioId}/run`, including optional scenario parameters.
3. [ScenarioMediatorService](src/main/java/io/drozda/sandbox/mediator/ScenarioMediatorService.java#L47)
   starts the matching environment and invokes its starter through
   [ScenarioStarter](src/main/java/io/drozda/sandbox/scenario/spi/ScenarioStarter.java).
4. The environment owns a nested Spring Boot application in the same JVM. It calls
   that application's internal HTTP endpoints on a random port. For a readable
   example, see [TradeFlowEnvironment](src/main/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowEnvironment.java#L35).
5. Scenario-owned publishers, listeners, trackers, and experiments perform the work.
   [TradeFlowExperiment](src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowExperiment.java#L27)
   waits separately for broker acknowledgement and a matching listener event.
6. The starter translates the result into runtime facts and animation signals.
   [ScenarioRuntimeService](src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java)
   retains transitions; the browser receives and replays them.

All 12 catalog scenarios have starters and environments. `system-ready` verifies
Spring wiring only. Scenarios 002–012 send and consume real Kafka records; static
steps remain as baseline topology and legacy step navigation, not evidence of success.
Tests exercise the same mediator path but are not the user-facing runner.
The former `VisualAction` AOP logger and its dependency have been removed; starters
emit runtime events explicitly. The old root-level publisher integration test was
replaced by the isolated 002 experiment and the 001 lifecycle/isolation checks.

## Scenario ownership and lifecycle

[ScenarioEnvironment](src/main/java/io/drozda/sandbox/scenario/spi/ScenarioEnvironment.java)
exposes start, stop, reset, and status. Environments isolate scenario beans using
conditional configuration, scenario-owned models, topics, and consumer groups.
The [catalog source mapping](src/main/java/io/drozda/sandbox/visualization/ScenarioCatalog.java#L30)
lists the semantic package for every scenario ID.

- 001 reuses its context while checking injected components. Its publisher, listener,
  model, and probe belong to `scenario.systemready`; conditional configuration
  imports them only inside the scenario application. The listener does not auto-start.
  Scenario-specific topic/group and JSON settings are passed by its environment.
- 002–009 reuse their context on repeated Play; a new context gets unique topic/group names.
- 010–012 recreate the context for each experiment, with new topics and group IDs.
- Stop closes the context and its listeners; the controller also clears the visual runtime.
- Reset calls scenario-specific in-memory reset logic and starts the environment if needed.
  It does not delete topics or reset Kafka offsets globally.

The mediator does not manage Docker, external JVMs, or broker lifecycle. Its HTTP
boundary separates application contexts but does not provide process isolation.
Topic cleanup and external orchestration remain backlog work.

## Models and runtime journal

[ScenarioGraph](src/main/java/io/drozda/sandbox/visualization/ScenarioGraph.java)
contains topology, legacy steps, practical purpose, backlog IDs, and `sourceRoot`.
The [catalog](src/main/java/io/drozda/sandbox/visualization/ScenarioCatalog.java)
builds these graphs in Java. Nodes describe components and carry explicit `sourceReferences` (label, file path,
line, and explanation). [ScenarioSourceCatalog](src/main/java/io/drozda/sandbox/visualization/ScenarioSourceCatalog.java)
binds these references to stable scenario/node IDs; UI labels are not used to guess paths.
Edges describe connections,
and steps provide the initial/scripted view.

There are two runtime models:

- [ScenarioRuntimeState](src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeState.java)
  stores the legacy current step index.
- [ActiveScenarioRuntimeState](src/main/java/io/drozda/sandbox/visualization/ActiveScenarioRuntimeState.java)
  stores the active scenario, session flags, node statuses/details, edge statuses,
  active signals, recent log, and latest event description.

[RuntimeEventRequest](src/main/java/io/drozda/sandbox/visualization/RuntimeEventRequest.java)
supports component ready/busy/waiting/failed, signal started/finished/delivered,
runtime reset, and session completion. Type/status strings are normalized by the
runtime service. `playbackGroup` associates simultaneous visual actions.

[ScenarioTimelineEvent](src/main/java/io/drozda/sandbox/visualization/ScenarioTimelineEvent.java#L3)
contains a monotonic sequence, `before`/`after` snapshots, `visibleInTimeline`,
`animated`, and `playbackGroup`. The service retains up to 1,000 events globally
in memory; the active runtime log contains only its latest 24 lines.

Long polling returns retained events after a cursor plus the current snapshot.
The browser first reads `/runtime/head`, then requests `/runtime/updates?after=...`.
[DeferredResult waiting](src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L72)
avoids holding a servlet thread while waiting for updates. The controller bounds
request timeouts to 1–120 seconds.

The journal is bounded and not durable. Clients that fall behind its retention
window receive the retained suffix; an explicit cursor-gap response is not implemented.
There is one global active runtime rather than independent concurrent user sessions.

## Frontend

Spring Boot serves [index.html](src/main/resources/static/index.html) at `/`, with
[styles.css](src/main/resources/static/styles.css) and
[app.js](src/main/resources/static/app.js). There is no frontend build step.

The browser keeps scenario-local event lists, playback frames, logs, timers, and
render tokens. Incoming events are deduplicated by sequence. Consecutive visible
events with the same `playbackGroup` share a frame, while hidden technical updates
are folded into the preceding frame's final state.

Playback renders the frame's starting snapshot, then its transition, then its
final state. Animated frames use 3,200 ms and static frames use 700 ms. These are
presentation delays; scenario code can finish before playback does. Some starters
also use short pauses when emitting visual events.

The timeline supports Previous, Replay, Next, Play all, and direct frame selection.
A new Play clears the selected scenario's timeline; reloading begins at the current
backend head and discards the old local history. Stop pauses playback and clears
client state after stopping the environment. The UI disables Play/Stop while its
run request is in flight, but the backend does not isolate concurrent callers.

The title’s **Show description on GitHub** link opens the selected scenario’s English
article in a new tab; the article includes a Russian-language link. The frontend
maps stable scenario IDs to document filenames in `scenarioDescriptionPath()`.

Nodes can be dragged, container nodes resized, and child positions constrained
inside containers. Clicking a node opens its description and a list of concrete source references.
Each reference opens a file on GitHub `main` at the configured line, with its path
and a short explanation. Publishers link to send code; consumers link to their
listener and tracker; broker nodes link to configuration; verification nodes link
to experiments and tests. Shared listener implementations deliberately share links.
These are branch-relative links, not commit-pinned permalinks. Source snippets are
not embedded in the application.

The hue picker stores its theme in `localStorage`. Component type colors identify
nodes; runtime highlights indicate ready, busy, waiting, and failed states.
Runtime log and legend are dialogs. Animated signals include a moving dot and path.

## HTTP API

All routes below are relative to `/api/scenarios` and implemented by
[ScenarioGraphController](src/main/java/io/drozda/sandbox/visualization/ScenarioGraphController.java).

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/` | List catalog scenarios |
| GET | `/{scenarioId}` | Selected graph |
| GET | `/{scenarioId}/commands` | Available commands |
| POST | `/{scenarioId}/run` | Run default command |
| POST | `/{scenarioId}/commands/{commandId}` | Run named command |
| GET | `/{scenarioId}/environment` | Environment status |
| POST | `/{scenarioId}/environment/start` | Start environment |
| POST | `/{scenarioId}/environment/stop` | Stop environment and reset visual state |
| POST | `/{scenarioId}/environment/reset` | Reset scenario state |
| GET | `/{scenarioId}/runtime` | Legacy step state |
| POST | `/{scenarioId}/runtime/next`, `/previous`, `/reset` | Legacy step navigation under the same prefix |
| GET | `/runtime/active` | Latest active snapshot |
| GET | `/runtime/head` | Current revision without replaying history |
| GET | `/runtime/updates?after={revision}&timeoutMs=120000` | Long-poll updates |
| POST | `/runtime/session/start`, `/step`, `/complete` | Session control under the same prefix |
| POST | `/runtime/event` | Apply a runtime event |

Run/command requests accept `invocationName` and a string-to-string `parameters`
map. Scenario 004 uses `keyStrategy`; 006 uses `topology`.

## Validation and remaining work

[ScenarioCatalogTest](src/test/java/io/drozda/sandbox/visualization/ScenarioCatalogTest.java)
checks purpose, backlog IDs, and unique scenario order.
[ScenarioSourceReferenceTest](src/test/java/io/drozda/sandbox/visualization/ScenarioSourceReferenceTest.java)
checks source directories, every node’s file/line references, distinct consumer
bindings, and JSON serialization of the API metadata.
[ScenarioRuntimeServiceTest](src/test/java/io/drozda/sandbox/visualization/ScenarioRuntimeServiceTest.java)
checks runtime transitions, waiting, and timeline behavior. Scenario integration
tests require a reachable Kafka broker; see [README](README.md) for commands.

Pending work includes explicit cursor-gap detection, independent sessions,
embedded source snippets, topic cleanup, and external process/container control.
Kafka learning priorities are recorded in the roadmap rather than repeated here.
