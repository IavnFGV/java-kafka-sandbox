# Visualizer Context

This file explains how the current integration visualizer works in this repository.

## Purpose

The visualizer is a lightweight Spring Boot + browser UI used to display integration scenarios:
- static graph structure
- scripted scenario steps
- backend-driven runtime updates from scenario starters

It is intentionally simple and hackable.

## Main Moving Parts

### 1. Static browser assets

Files:
- `src/main/resources/static/index.html`
- `src/main/resources/static/styles.css`
- `src/main/resources/static/app.js`

How they are served:
- Spring Boot automatically serves files from `src/main/resources/static/`
- `index.html` is the welcome page at `/`
- `styles.css` is available at `/styles.css`
- `app.js` is available at `/app.js`

Browser flow:
1. browser requests `/`
2. Spring returns `index.html`
3. browser loads `/styles.css`
4. browser loads `/app.js`
5. `app.js` fetches scenario JSON from backend API and renders the graph

## 2. Scenario model

Files:
- `src/main/java/io/drozda/sandbox/visualization/ScenarioGraph.java`
- `src/main/java/io/drozda/sandbox/visualization/ScenarioNode.java`
- `src/main/java/io/drozda/sandbox/visualization/ScenarioEdge.java`
- `src/main/java/io/drozda/sandbox/visualization/ScenarioStep.java`
- `src/main/java/io/drozda/sandbox/visualization/VisualizationEvent.java`
- `src/main/java/io/drozda/sandbox/visualization/ScenarioCatalog.java`

Current idea:
- `ScenarioGraph` is the whole scenario
- `ScenarioGraph.practicalPurpose` explains the real engineering need behind it
- `ScenarioGraph.backlogItems` links it to the numbered Kafka learning backlog
- nodes describe boxes on the screen
- edges describe relations/arrows
- steps support old scripted playback mode
- `VisualizationEvent` supports step-linked visual highlights
- `ScenarioCatalog` builds hardcoded scenarios in Java

Current built-in scenarios:
- `system-ready`
- `trade-flow`
- `topic-partition-offsets`
- `consumer-group-single-partition`
- `consumer-group-two-partitions`
- `consumer-group-rebalance-join`
- `consumer-group-consumer-failure`

They are ordered from introductory topology to consumer-group failure behavior.
`system-ready`, `trade-flow`, and `topic-partition-offsets` have backend scenario
starters. The remaining scenarios are scripted visual explanations waiting for
real Kafka experiments.

## 3. Runtime state

Files:
- `src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java`
- `src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeState.java`
- `src/main/java/io/drozda/sandbox/visualization/ActiveScenarioRuntimeState.java`
- `src/main/java/io/drozda/sandbox/visualization/RuntimeEventRequest.java`
- `src/main/java/io/drozda/sandbox/visualization/RuntimeSignal.java`

There are two runtime models:

### Step runtime

Retained for scripted scenario playback:
- current step index for a scenario
- next / previous / reset behavior

### Active runtime

Used by live event-driven visualization:
- active scenario id
- current step index
- whether session is active or completed
- test name
- node statuses
- edge statuses
- active signals
- last event type
- last event label

Important point:
- `system-ready` is already moving toward event-driven behavior
- `trade-flow` still mostly fits the step-based model

## 4. Runtime events

Backend endpoint:
- `POST /api/scenarios/runtime/event`

Handled by:
- `ScenarioGraphController`
- `ScenarioRuntimeService.applyRuntimeEvent(...)`

Current event intent:
- independent runtime facts instead of rigid step numbers

Examples of event types already supported by normalization in runtime service:
- `component-ready`
- `component-busy`
- `component-waiting`
- `component-failed`
- `signal-started`
- `signal-finished`
- `signal-delivered`
- `runtime-reset`
- `session-completed`

Typical payload fields:
- `scenarioId`
- `type`
- `nodeId`
- `edgeId`
- `label`
- `fromNodeId`
- `toNodeId`
- `status`

## 5. REST API

File:
- `src/main/java/io/drozda/sandbox/visualization/ScenarioGraphController.java`

Important endpoints:

Scenario data:
- `GET /api/scenarios/{scenarioId}`
- `GET /api/scenarios/{scenarioId}/runtime`

Step navigation:
- `POST /api/scenarios/{scenarioId}/runtime/next`
- `POST /api/scenarios/{scenarioId}/runtime/previous`
- `POST /api/scenarios/{scenarioId}/runtime/reset`

Live runtime:
- `GET /api/scenarios/runtime/active`
- `GET /api/scenarios/runtime/updates?after={revision}`
- `POST /api/scenarios/runtime/session/start`
- `POST /api/scenarios/runtime/session/step`
- `POST /api/scenarios/runtime/session/complete`
- `POST /api/scenarios/runtime/event`

## 6. Frontend structure

File:
- `src/main/resources/static/app.js`

The file was recently reorganized so it is easier to navigate.

Current major sections:
1. constants and state
2. bootstrap
3. runtime/api functions
4. render entry point
5. view-model building
6. SVG rendering helpers
7. layout helpers
8. drag helpers
9. small utilities

Main frontend behavior:
- load initial scenario and runtime
- keep one long-poll request open until the runtime revision changes or times out
- if an active runtime exists, render live mode
- otherwise render step mode
- expose Play and Stop controls
- keep a per-scenario browser log until page reload
- allow dragging nodes in the SVG
- keep child nodes inside their container
- allow container resizing from the bottom-right corner

## 7. Current visual semantics

Node types have their own base colors:
- external
- container
- service
- broker
- consumer
- monitor

Runtime state is shown mostly by glow/highlight:
- `ready` = green glow
- `busy` = orange glow
- `failed` = red glow
- active edge = stronger active line
- signal = animated dot moving between nodes

Important design choice:
- base border colors should stay by node type
- runtime should mostly add glow, not repaint the node identity

## 8. Mediator and scenario execution

Files:
- `src/main/java/io/drozda/sandbox/mediator/ScenarioMediatorService.java`
- `src/main/java/io/drozda/sandbox/scenario/spi/ScenarioStarter.java`
- `src/main/java/io/drozda/sandbox/scenario/spi/ScenarioEnvironment.java`
- `src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyEnvironment.java`
- `src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyScenarioStarter.java`
- `src/main/java/io/drozda/sandbox/scenario/tradeflow/TradeFlowEnvironment.java`
- `src/main/java/io/drozda/sandbox/scenario/tradeflow/TradeFlowScenarioStarter.java`

How it works:
- Play calls the mediator's default command for the selected scenario
- the mediator starts the matching environment
- `system-ready` starts a dedicated nested Spring Boot context on a random port
- the mediator communicates with that scenario application over HTTP
- the starter translates the result into visual runtime events
- Stop closes the environment and resets the visual topology
- `trade-flow` owns a separate topic, publisher, listener, tracker, and Spring context
- Kafka can be rendered as a resizable broker container with a topic inside it

Tests cover mediator behavior and runtime state, but tests are no longer intended
to be the user-facing mechanism for running scenarios.

## 9. Known strengths

What already works well:
- quick iteration
- hardcoded scenarios are easy to understand
- event-driven runtime is enough for first practical demos
- integration tests can drive the browser visualization

## 10. Known weaknesses

What will likely need refactoring later:
- scenarios are hardcoded in `ScenarioCatalog`
- frontend is still plain JS in one file
- runtime status model is string-based
- there is no persistent scenario/session history
- only one active runtime session is effectively tracked at a time
- layout is manual and not auto-arranged
- `system-ready` currently checks bean creation, not real broker health
- runtime updates use long polling rather than a server-push stream such as SSE

## 11. Good next refactoring directions

Reasonable next steps:
- introduce typed enums for runtime status/event categories
- separate frontend modules further if JS grows
- replace string statuses with typed values
- support richer visual events like retry, DLQ, rebalance, lag
- support multiple runtime sessions or history playback
- turn topic/partition/offset into the next real scenario before expanding orchestration

## 12. Good next learning scenarios

Best next Kafka-driven visual scenarios:
- consumer group rebalance
- duplicate delivery with at-least-once semantics
- retry then DLQ
- key-based partition routing
- Kafka unavailable during publish
- lag growing because consumer is slow
