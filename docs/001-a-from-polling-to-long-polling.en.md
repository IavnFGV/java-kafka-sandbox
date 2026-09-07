# 001-a. Why the visualizer kept polling the backend

[Русский](001-a-from-polling-to-long-polling.ru.md)

## Before / after

- Before: `f8fdc4d` — the browser requested runtime state every 500 ms and rebuilt the SVG.
- After: `e73162c` — one long-poll request waits for a new revision, and the screen updates only after a change.

After running the first scenario, we discovered a problem: every 500 milliseconds,
the browser called `GET /api/scenarios/runtime/active`, even when nothing was
happening. JavaScript then redrew the SVG despite receiving the same response.

This approach is called short polling. It is simple: the client periodically
asks the server whether anything has changed. It is convenient for a prototype,
but continuous requests create noise, and unnecessary redraws can interfere with
graph interactions. For example, the entire SVG might be recreated while a node
is being dragged.

There are three proportionate ways to address this. We could keep short polling,
but compare states and call `render()` only when something changes. We could
switch to long polling. A third option is a persistent Server-Sent Events stream,
through which the backend sends updates itself.

The next step is long polling. The browser sends a request, but the backend does
not respond immediately. It waits for a new runtime event or a timeout. After
receiving a response, the client updates the screen and immediately opens the
next waiting request. When there are no events, almost no pointless message
exchange occurs.

Long polling is a reasonable choice for the project's current state: it works
over ordinary HTTP, requires no WebSocket, and illustrates waiting for changes.
The server needs a runtime-state version and a waiting mechanism that does not
block a servlet thread, such as `DeferredResult`.

For a large, continuous, one-way event stream, Server-Sent Events would be more
natural: the backend keeps a connection open and sends events itself. For now,
however, the problem is not critical, scenarios run infrequently, and a separate
streaming contract would complicate this small prototype. We therefore use long
polling and keep SSE as an understandable next step if event and user counts grow.

The practical lesson: polling is convenient for a prototype, but request frequency
should not replace an event model. The interface should update when the state
actually changes.

## Core implementation

- [`awaitRuntimeUpdate()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L72) registers a `DeferredResult`, handles timeout, and returns an already available revision.
- [`publishRuntime()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L282) stores the transition and completes waiting requests.
- [`runtimeUpdates()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioGraphController.java#L111) bounds the HTTP request timeout.
- [`pollRuntimeUpdates()`](../src/main/resources/static/app.js#L194) sends the cursor, receives events, and opens the next request.
