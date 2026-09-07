# 005-a. How to avoid losing fast events between Kafka and the visualizer

[Русский](005-a-lossless-scenario-timeline.ru.md)

## Before / after

- Before: `64aea0d` — the timeline idea was in the backlog, but the backend still returned only the latest runtime snapshot, and SVG animation restarted after every redraw.
- Backend: `a86c50b` — a journal of sequential transitions with `before` and `after` states was added; long polling returns all retained events after a cursor.
- UI: `ee84560` — the browser queues transitions, plays them at a learning-friendly speed, and lets the user return to any step.
- Refinement: `d2a38f2` — the backend distinguishes teaching frames from technical updates, the timeline is fixed at the bottom, and the log and legend open on demand.

## How the problem surfaced

In Kafka scenarios, the backend works much faster than a person. A producer can
send several records, a consumer can receive them, and verification can finish
in a fraction of a second. Meanwhile, the visualizer needs to show a sequence:
message publication, partition selection, consumer delivery, and final verification.

The first solution stored only the current [`ActiveScenarioRuntimeState`](../src/main/java/io/drozda/sandbox/visualization/ActiveScenarioRuntimeState.java#L6). Each
change increased the revision and woke a long-poll request. However, the backend
could perform several logical transitions between requests:

```text
revision 20: message published
revision 21: partition selected
revision 22: consumer received message
```

If the browser requested data only after revision 22, it received the latest
state. The frontend could not reconstruct revisions 20 and 21 because the server
no longer stored them. Adding a queue only in JavaScript would not have been
enough: events the client never received cannot be queued.

There was a second problem. `render()` replaces the entire SVG contents. Removing
the old markup also removes SVG `animate`, so the red dot starts from the beginning
of its path every time. Frequent runtime updates made motion look like flickering.

## The backend as a transition journal

The unit of exchange is now a `before -> after` transition, rather than just the
latest state. Its core is shown below in abbreviated form; the complete model is in
[`ScenarioTimelineEvent`](../src/main/java/io/drozda/sandbox/visualization/ScenarioTimelineEvent.java#L3):

```java
public record ScenarioTimelineEvent(
        long sequence,
        ActiveScenarioRuntimeState before,
        ActiveScenarioRuntimeState after
) {}
```

`sequence` defines a shared monotonic order. `before` reconstructs the frame before
the action, while `after` contains the transition result and the active animation
signal. This format allows a step to be replayed locally without running Kafka
or the nested Spring context again.

The journal lives alongside the current runtime in
[`ScenarioRuntimeService`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L36).
When publishing a new state, the service retains the previous snapshot, increments
the revision, and appends an event to the timeline. The write is atomic in
[`publishRuntime()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L282),
so a client cannot see a new sequence before its event is in the journal.

Long polling remains, but the response now means something different.
[`runtimeUpdateAfter()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L303)
selects all events with `sequence > afterRevision`. The response contract contains
an `events` list and the latest runtime snapshot, as shown in
[`ScenarioRuntimeUpdate`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeUpdate.java#L5).

The browser still calls:

```http
GET /api/scenarios/runtime/updates?after=22
```

But the response can contain revisions 23, 24, and 25 together. HTTP request speed
no longer determines which logical transitions the user sees.

## Why the `head` endpoint was added

The backend journal outlives a browser tab, while we decided that the user-facing
timeline should last only until page reload. On startup, the UI therefore first
calls [`/runtime/head`](../src/main/java/io/drozda/sandbox/visualization/ScenarioGraphController.java#L105)
and remembers the current revision. Long polling then requests only new events.

This distinction matters:

- the backend retains a small buffer for reliable delivery;
- the browser timeline represents the current learning session's history;
- reloading deliberately starts a new local history;
- a new Play also clears the selected scenario’s timeline before recording another run.

## Browser queue and playback

The frontend maintains a separate timeline for each scenario. It contains an
event list, current index, automatic continuation mode, timer, and token for
cancelling stale animation. The structure is created in
[`timelineFor()`](../src/main/resources/static/app.js#L249).

Long polling receives a batch and adds events without duplicates in
[`receiveTimelineEvents()`](../src/main/resources/static/app.js#L214). The backend
is not slowed down: the Kafka experiment may already have finished while the UI
is still calmly showing the first transitions.

The main logic is in
[`showTimelineStep()`](../src/main/resources/static/app.js#L282). It first sets the
`before` snapshot and renders the initial state. On the next animation frame,
it applies the transition state so the SVG signal starts moving. A step with an
active signal gets 3200 ms; an ordinary state change gets 700 ms.

These delays control presentation only. They do not add `sleep` to the producer,
consumer, or Kafka. The experiment remains real, while its explanation runs at
a comfortable speed for a person.

## Navigating history

A list of recorded transitions and four commands appear below the graph. The
markup is in [`index.html`](../src/main/resources/static/index.html#L89), and cards
are built in [`renderTimeline()`](../src/main/resources/static/app.js#L399).

- `Previous` plays the preceding transition.
- `Replay` shows the current transition again, from `before` to `after`.
- `Next` performs one subsequent transition and stops.
- `Play all` continues automatic playback from the selected position.

Clicking a card always enters learning mode: autoplay stops, the selected
transition plays once, and the screen remains in its final state. This lets a
user repeatedly inspect which partition received a record and which consumer
owned it, without creating new Kafka messages.

`Stop` and navigating to another page cancel the timer and increment
`renderToken`. A delayed callback from an old step can no longer unexpectedly
redraw a new scenario.

## How missing transitions are checked

The unit test
[`shouldReturnEveryRuntimeTransitionAfterSequenceCursor()`](../src/test/java/io/drozda/sandbox/visualization/ScenarioRuntimeServiceTest.java#L203)
remembers a cursor, then performs three runtime changes before reading the
response. It requires exactly three events with consecutive sequence numbers
and the correct session-start and session-completion types. The test covers the
original problem: the backend performed more than one step between client reads.

After the implementation change, the full `./gradlew test` suite passed, including
Kafka integration scenarios and nested Spring Boot contexts.

## Limitations of the first version

The journal is in memory and limited to 1000 transitions. This is enough for
short learning runs, but a slow client could theoretically fall behind the
retention window. A future improvement should return an explicit cursor-gap
indicator so the UI does not present an incomplete history as complete.

In the second iteration, backend events gained `visibleInTimeline` and `animated`
flags. Starting a moving signal becomes a numbered frame; signal completion,
node-text updates, and housekeeping changes remain technical events. Static
partition assignments and state changes appear as `STATE` but do not consume
an animation sequence number.

The frontend does not discard hidden updates. It attaches them to the final
`after` of the preceding visible frame. Once the red dot finishes, the user sees
the updated partition label and final component statuses. The `Technical events`
switch reveals the original backend revision sequence for debugging without
mixing it into the main teaching sequence.

The timeline is fixed at the bottom of the screen, like an editing track. The
runtime log and legend have moved into modal dialogs so infrequently used
diagnostics do not take permanent space beside the graph.

Classification currently depends on the runtime event's type and status. If a
future scenario needs a different meaning for the same technical event, the next
step will be to let scenarios set these flags explicitly.

The practical lesson extends beyond this visualizer: when clients need change
history, a latest-state API is insufficient. A snapshot answers “what is the
state now?”, while an event journal answers “how did we get here?”. A learning
visualization needs the second answer.
