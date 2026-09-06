const POLL_INTERVAL_MS = 500;
const DEFAULT_SCENARIO_ID = "system-ready";
const CONTAINER_PADDING = 20;
const MIN_CONTAINER_WIDTH = 180;
const MIN_CONTAINER_HEIGHT = 140;

const state = {
  scenarios: [],
  scenario: null,
  stepIndex: 0,
  drag: null,
  activeScenarioId: null,
  activeRuntime: null,
  logsByScenario: {},
  lastPlaybackStepKeyByScenario: {},
  lastRuntimeSnapshotByScenario: {},
  runInFlight: false
};

const graph = document.getElementById("graph");
const scenarioList = document.getElementById("scenario-list");
const title = document.getElementById("scenario-title");
const summary = document.getElementById("scenario-summary");
const stepTitle = document.getElementById("step-title");
const stepDescription = document.getElementById("step-description");
const runtimeLog = document.getElementById("runtime-log");
const playBtn = document.getElementById("play-btn");
const stopBtn = document.getElementById("stop-btn");
let scenarioId = new URLSearchParams(window.location.search).get("scenario") || DEFAULT_SCENARIO_ID;

bootstrap();

function bootstrap() {
  bindControlEvents();
  bindDragEvents();
  loadInitialScenario();
  window.setInterval(syncActiveRuntime, POLL_INTERVAL_MS);
}

function loadInitialScenario() {
  Promise.all([
    fetchJson("/api/scenarios"),
    fetchJson(`/api/scenarios/${scenarioId}`),
    fetchJson(`/api/scenarios/${scenarioId}/runtime`)
  ]).then(([scenarios, scenario, runtime]) => {
    state.scenarios = scenarios;
    applyScenario(scenario);
    state.stepIndex = runtime.currentStepIndex;
    state.activeRuntime = null;
    render();
  });
}

function bindControlEvents() {
  playBtn.addEventListener("click", runScenario);
  stopBtn.addEventListener("click", stopScenario);
}

function runScenario() {
  if (state.runInFlight) {
    return;
  }

  state.runInFlight = true;
  playBtn.disabled = true;
  stopBtn.disabled = true;
  playBtn.textContent = "Running...";
  appendScenarioLog("Backend run started");
  state.activeRuntime = null;
  state.stepIndex = 0;
  render();
  fetchJson(`/api/scenarios/${scenarioId}/run`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ invocationName: `${state.scenario.title} Run` })
  }).then((runtime) => {
    state.activeRuntime = runtime;
    state.stepIndex = runtime.currentStepIndex;
    render();
  }).catch(() => {
    appendScenarioLog("Backend run failed");
    renderRuntimeLog();
  }).finally(() => {
    state.runInFlight = false;
    playBtn.disabled = false;
    stopBtn.disabled = false;
    playBtn.textContent = "Play";
  });
}

function stopScenario() {
  if (state.runInFlight) {
    return;
  }

  stopBtn.disabled = true;
  appendScenarioLog("Backend stop requested");
  fetchJson(`/api/scenarios/${scenarioId}/environment/stop`, {
    method: "POST"
  }).then(() => {
    clearClientRuntimeState(scenarioId);
    appendScenarioLog("Environment stopped");
    render();
  }).catch(() => {
    appendScenarioLog("Backend stop failed");
    renderRuntimeLog();
  }).finally(() => {
    stopBtn.disabled = false;
  });
}

function syncActiveRuntime() {
  fetchJson("/api/scenarios/runtime/active")
    .then((runtime) => {
      if (!runtime || !runtime.scenarioId) {
        state.activeRuntime = null;
        return;
      }

      const runtimeIsDrivingScreen = runtime.active || runtime.scenarioId === state.activeScenarioId;
      state.activeRuntime = runtime;

      if (!runtimeIsDrivingScreen) {
        return;
      }

      if (runtime.scenarioId !== state.activeScenarioId) {
        return fetchJson(`/api/scenarios/${runtime.scenarioId}`).then((scenario) => {
          applyScenario(scenario);
          state.stepIndex = runtime.currentStepIndex;
          render();
        });
      }

      state.stepIndex = runtime.currentStepIndex;
      render();
    })
    .catch(() => {
      // Ignore polling failures when the app is restarting.
    });
}

function fetchJson(url, options) {
  return fetch(url, options).then((response) => response.json());
}

function applyScenario(scenario) {
  state.scenario = scenario;
  state.activeScenarioId = scenario.id;
  title.textContent = scenario.title;
  summary.textContent = scenario.summary;
  graph.setAttribute("viewBox", `0 0 ${scenario.viewportWidth} ${scenario.viewportHeight}`);
  renderScenarioList();
}

function render() {
  if (!state.scenario) {
    return;
  }

  ensurePlaybackStepLogged();
  syncRuntimeLogToScenarioHistory();
  const stepView = buildCurrentStepView();
  const layout = buildLayout(state.scenario.nodes);

  stepTitle.textContent = currentTitle();
  stepDescription.textContent = currentDescription();
  renderRuntimeLog();
  graph.innerHTML = `
    <defs>
      <marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L0,6 L9,3 z" fill="rgba(31, 41, 55, 0.35)"></path>
      </marker>
    </defs>
    ${renderEdges(state.scenario.edges, layout, stepView)}
    ${renderNodes(layout, stepView)}
    ${renderSignals(layout, stepView)}
  `;
  attachDragHandlers();
}

function renderRuntimeLog() {
  if (!runtimeLog) {
    return;
  }

  const lines = currentLogLines();

  if (lines.length === 0) {
    runtimeLog.innerHTML = `<p class="runtime-log-empty">No runtime events yet.</p>`;
    return;
  }

  runtimeLog.innerHTML = lines.map((line) => `
    <div class="runtime-log-line">${escapeXml(line)}</div>
  `).join("");
}

function renderScenarioList() {
  if (!scenarioList) {
    return;
  }

  scenarioList.innerHTML = state.scenarios.map((scenario) => `
    <button
      type="button"
      class="scenario-link ${scenario.id === state.activeScenarioId ? "active" : ""}"
      data-scenario-id="${scenario.id}">
      <span class="scenario-link-order">${escapeXml(formatScenarioOrder(scenario))}</span>
      <span class="scenario-link-title">${escapeXml(scenario.title)}</span>
    </button>
  `).join("");

  scenarioList.querySelectorAll("[data-scenario-id]").forEach((element) => {
    element.addEventListener("click", () => {
      selectScenario(element.dataset.scenarioId);
    });
  });
}

function selectScenario(nextScenarioId) {
  if (!nextScenarioId || nextScenarioId === state.activeScenarioId) {
    return;
  }

  scenarioId = nextScenarioId;
  fetchJson(`/api/scenarios/${nextScenarioId}`)
    .then((scenario) => Promise.all([
      Promise.resolve(scenario),
      fetchJson(`/api/scenarios/${nextScenarioId}/runtime`)
    ]))
    .then(([scenario, runtime]) => {
      state.activeRuntime = null;
      state.stepIndex = runtime.currentStepIndex;
      applyScenario(scenario);
      render();
      updateUrl(nextScenarioId);
    });
}

function buildCurrentStepView() {
  if (hasLiveRuntimeForScenario()) {
    return buildLiveView(state.activeRuntime);
  }

  const step = state.scenario.steps[state.stepIndex];
  return buildStepView(step, state.scenario.events);
}

function currentTitle() {
  if (hasLiveRuntimeForScenario()) {
    return state.activeRuntime.lastEventLabel || "Live Runtime";
  }

  return state.scenario.steps[state.stepIndex].title;
}

function currentDescription() {
  if (hasLiveRuntimeForScenario()) {
    return describeLiveRuntime(state.activeRuntime);
  }

  return state.scenario.steps[state.stepIndex].description;
}

function currentLogLines() {
  return scenarioLog(state.activeScenarioId).slice().reverse();
}

function ensurePlaybackStepLogged() {
  if (hasLiveRuntimeForScenario() || !state.scenario) {
    return;
  }

  const step = state.scenario.steps[state.stepIndex];
  if (!step) {
    return;
  }

  const stepKey = `${state.stepIndex}`;
  if (state.lastPlaybackStepKeyByScenario[state.activeScenarioId] === stepKey) {
    return;
  }

  appendScenarioLog(`Step ${state.stepIndex}: ${step.title}`);
  if (step.description) {
    appendScenarioLog(step.description);
  }
  state.lastPlaybackStepKeyByScenario[state.activeScenarioId] = stepKey;
}

function syncRuntimeLogToScenarioHistory() {
  if (!state.activeRuntime || !state.activeRuntime.scenarioId) {
    return;
  }

  const scenarioRuntimeId = state.activeRuntime.scenarioId;
  const nextSnapshot = Array.isArray(state.activeRuntime.eventLog) ? state.activeRuntime.eventLog : [];
  const previousSnapshot = state.lastRuntimeSnapshotByScenario[scenarioRuntimeId] || [];

  if (sameLines(previousSnapshot, nextSnapshot)) {
    return;
  }

  const commonPrefixLength = sharedPrefixLength(previousSnapshot, nextSnapshot);
  if (previousSnapshot.length > 0 && commonPrefixLength < previousSnapshot.length) {
    appendScenarioLog("Backend session restarted", scenarioRuntimeId);
  }

  nextSnapshot.slice(commonPrefixLength).forEach((line) => {
    appendScenarioLog(line, scenarioRuntimeId);
  });
  state.lastRuntimeSnapshotByScenario[scenarioRuntimeId] = nextSnapshot.slice();
}

function appendScenarioLog(line, targetScenarioId = state.activeScenarioId) {
  if (!line || !targetScenarioId) {
    return;
  }

  const lines = scenarioLog(targetScenarioId);
  lines.push(line);
  if (lines.length > 80) {
    lines.shift();
  }
}

function clearClientRuntimeState(targetScenarioId) {
  if (!targetScenarioId) {
    return;
  }

  if (state.activeScenarioId === targetScenarioId) {
    state.activeRuntime = null;
    state.stepIndex = 0;
  }

  state.lastPlaybackStepKeyByScenario[targetScenarioId] = null;
  state.lastRuntimeSnapshotByScenario[targetScenarioId] = [];
}

function scenarioLog(targetScenarioId) {
  if (!targetScenarioId) {
    return [];
  }

  if (!state.logsByScenario[targetScenarioId]) {
    state.logsByScenario[targetScenarioId] = [];
  }

  return state.logsByScenario[targetScenarioId];
}

function sameLines(left, right) {
  if (left.length !== right.length) {
    return false;
  }

  for (let index = 0; index < left.length; index += 1) {
    if (left[index] !== right[index]) {
      return false;
    }
  }

  return true;
}

function sharedPrefixLength(left, right) {
  const maxLength = Math.min(left.length, right.length);
  let index = 0;

  while (index < maxLength && left[index] === right[index]) {
    index += 1;
  }

  return index;
}

function buildStepView(step, events) {
  const selectedEvents = step.eventIds
    .map((eventId) => events.find((event) => event.id === eventId))
    .filter(Boolean);
  const readyNodeIds = uniqueIds(selectedEvents
    .filter((event) => event.type === "readiness" || event.type === "status")
    .flatMap((event) => event.activeNodeIds || []));
  const readyEdgeIds = uniqueIds(selectedEvents
    .filter((event) => event.type === "readiness" || event.type === "status")
    .flatMap((event) => event.activeEdgeIds || []));
  const activeNodeIds = uniqueIds(selectedEvents
    .flatMap((event) => event.activeNodeIds || [])
    .filter((nodeId) => !readyNodeIds.includes(nodeId)));
  const activeEdgeIds = uniqueIds(selectedEvents
    .flatMap((event) => event.activeEdgeIds || [])
    .filter((edgeId) => !readyEdgeIds.includes(edgeId)));
  const primaryEvent = selectedEvents[selectedEvents.length - 1] || null;

  return {
    activeNodeIds,
    activeEdgeIds,
    readyNodeIds,
    readyEdgeIds,
    failedNodeIds: [],
    busyNodeIds: [],
    signals: primaryEvent && primaryEvent.signalFromId && primaryEvent.signalToId
      ? [{
        label: primaryEvent.label,
        fromNodeId: primaryEvent.signalFromId,
        toNodeId: primaryEvent.signalToId
      }]
      : []
  };
}

function buildLiveView(runtime) {
  const nodeStatuses = runtime.nodeStatuses || {};
  const edgeStatuses = runtime.edgeStatuses || {};

  return {
    activeNodeIds: collectIdsByStatus(nodeStatuses, ["BUSY", "WAITING"]),
    activeEdgeIds: collectIdsByStatus(edgeStatuses, ["ACTIVE", "BUSY"]),
    readyNodeIds: collectIdsByStatus(nodeStatuses, ["READY"]),
    readyEdgeIds: collectIdsByStatus(edgeStatuses, ["READY"]),
    failedNodeIds: collectIdsByStatus(nodeStatuses, ["FAILED"]),
    busyNodeIds: collectIdsByStatus(nodeStatuses, ["BUSY", "WAITING"]),
    signals: runtime.activeSignals || []
  };
}

function hasLiveRuntimeForScenario() {
  return state.activeRuntime
    && state.activeRuntime.scenarioId === state.activeScenarioId
    && (Object.keys(state.activeRuntime.nodeStatuses || {}).length > 0
      || Object.keys(state.activeRuntime.edgeStatuses || {}).length > 0
      || (state.activeRuntime.activeSignals || []).length > 0
      || state.activeRuntime.completed);
}

function describeLiveRuntime(runtime) {
  const labels = [];
  const nodeStatuses = runtime.nodeStatuses || {};
  const edgeStatuses = runtime.edgeStatuses || {};
  const ready = collectIdsByStatus(nodeStatuses, ["READY"]);
  const busy = collectIdsByStatus(nodeStatuses, ["BUSY", "WAITING"]);
  const failed = collectIdsByStatus(nodeStatuses, ["FAILED"]);
  const activeEdges = collectIdsByStatus(edgeStatuses, ["ACTIVE", "BUSY"]);

  if (ready.length > 0) {
    labels.push(`Ready: ${ready.join(", ")}`);
  }
  if (busy.length > 0) {
    labels.push(`Busy: ${busy.join(", ")}`);
  }
  if (failed.length > 0) {
    labels.push(`Failed: ${failed.join(", ")}`);
  }
  if (activeEdges.length > 0) {
    labels.push(`Flow: ${activeEdges.join(", ")}`);
  }
  if (runtime.completed) {
    labels.push("Session completed");
  }
  if (labels.length === 0) {
    return "The visualizer is waiting for runtime events.";
  }

  return labels.join(" | ");
}

function renderEdges(edges, layout, stepView) {
  return edges.map((edge) => {
    const from = layout.get(edge.from);
    const to = layout.get(edge.to);
    const edgeState = edgeCssState(edge.id, stepView);
    const labelX = (from.centerX + to.centerX) / 2;
    const labelY = (from.centerY + to.centerY) / 2 - 16;
    const labelWidth = Math.max(estimateTextWidth(edge.label, 12) + 18, 44);

    return `
      <g>
        <line class="edge ${edgeState}" x1="${from.centerX}" y1="${from.centerY}" x2="${to.centerX}" y2="${to.centerY}" marker-end="url(#arrow)"></line>
        <rect class="edge-label-pill" x="${labelX - labelWidth / 2}" y="${labelY - 14}" width="${labelWidth}" height="22" rx="11"></rect>
        <text class="edge-label" x="${labelX}" y="${labelY}" text-anchor="middle">${escapeXml(edge.label)}</text>
      </g>
    `;
  }).join("");
}

function renderNodes(layout, stepView) {
  return Array.from(layout.values())
    .sort((left, right) => depth(left.node, layout) - depth(right.node, layout))
    .map((entry) => renderNode(entry, stepView))
    .join("");
}

function renderNode(entry, stepView) {
  const { node, absoluteX, absoluteY } = entry;
  const nodeState = nodeCssState(node.id, stepView);
  const stroke = colorForType(node.type);
  const typeClass = node.type === "container" ? "node-container" : "";
  const ringInset = node.type === "container" ? 10 : 8;
  const titleLines = wrapText(node.label, 18, node.width - 36);
  const blockHeight = titleLines.length * 22;
  const titleStartY = node.type === "container"
    ? 34
    : Math.round((node.height - blockHeight) / 2) + 16;
  const titleMarkup = renderTextLines(titleLines, node.width / 2, titleStartY, 22, "node-title", "middle");
  const resizeHandle = node.type === "container" ? `
    <g class="resize-handle" data-resize-node-id="${node.id}" aria-label="Resize ${escapeXml(node.label)}">
      <rect x="${node.width - 30}" y="${node.height - 30}" width="30" height="30" rx="8"></rect>
      <path d="M${node.width - 20},${node.height - 8} L${node.width - 8},${node.height - 20} M${node.width - 12},${node.height - 8} L${node.width - 8},${node.height - 12}"></path>
    </g>
  ` : "";

  return `
    <g class="node ${typeClass} ${nodeState}" data-node-id="${node.id}" transform="translate(${absoluteX}, ${absoluteY})">
      <rect class="node-ready-ring" x="${ringInset}" y="${ringInset}" width="${node.width - ringInset * 2}" height="${node.height - ringInset * 2}" rx="${node.type === "container" ? 22 : 14}"></rect>
      <rect class="node-card" width="${node.width}" height="${node.height}" rx="${node.type === "container" ? 28 : 20}" stroke="${stroke}"></rect>
      ${titleMarkup}
      ${resizeHandle}
    </g>
  `;
}

function renderSignals(layout, stepView) {
  const signals = stepView.signals || [];
  return signals.map((signal, index) => renderSignal(layout, signal, index)).join("");
}

function renderSignal(layout, signal, index) {
  if (!signal.fromNodeId || !signal.toNodeId) {
    return "";
  }

  const from = layout.get(signal.fromNodeId);
  const to = layout.get(signal.toNodeId);
  if (!from || !to) {
    return "";
  }

  const label = signal.label || "message";
  const offset = index * 18;
  const midX = (from.centerX + to.centerX) / 2;
  const midY = (from.centerY + to.centerY) / 2 - 18 - offset;

  return `
    <g class="signal">
      <text class="signal-label" x="${midX}" y="${midY}" text-anchor="middle">${label}</text>
      <circle class="signal-dot" r="8">
        <animate attributeName="cx" from="${from.centerX}" to="${to.centerX}" dur="1.4s" repeatCount="indefinite"></animate>
        <animate attributeName="cy" from="${from.centerY}" to="${to.centerY}" dur="1.4s" repeatCount="indefinite"></animate>
      </circle>
    </g>
  `;
}

function buildLayout(nodes) {
  const layout = new Map();
  nodes.forEach((node) => {
    resolveNode(node, nodes, layout);
  });
  return layout;
}

function resolveNode(node, nodes, layout) {
  if (layout.has(node.id)) {
    return layout.get(node.id);
  }

  let absoluteX = node.x;
  let absoluteY = node.y;
  if (node.parentId) {
    const parent = nodes.find((candidate) => candidate.id === node.parentId);
    if (parent) {
      const parentLayout = resolveNode(parent, nodes, layout);
      absoluteX += parentLayout.absoluteX;
      absoluteY += parentLayout.absoluteY;
    }
  }

  const entry = {
    node,
    absoluteX,
    absoluteY,
    centerX: absoluteX + node.width / 2,
    centerY: absoluteY + node.height / 2
  };
  layout.set(node.id, entry);
  return entry;
}

function depth(node, layout) {
  let count = 0;
  let current = node;

  while (current.parentId) {
    const parentEntry = layout.get(current.parentId);
    if (!parentEntry) {
      break;
    }

    count += 1;
    current = parentEntry.node;
  }

  return count;
}

function bindDragEvents() {
  graph.addEventListener("pointermove", (event) => {
    if (!state.drag || state.drag.pointerId !== event.pointerId) {
      return;
    }

    const node = state.scenario.nodes.find((candidate) => candidate.id === state.drag.nodeId);
    if (!node) {
      return;
    }

    const currentPoint = toSvgPoint(event);

    if (state.drag.mode === "resize") {
      resizeContainer(node, currentPoint);
    } else {
      moveNode(node, currentPoint);
    }

    state.drag.lastPoint = currentPoint;
    render();
  });

  graph.addEventListener("pointerup", (event) => {
    if (state.drag && state.drag.pointerId === event.pointerId) {
      finishDrag();
    }
  });

  graph.addEventListener("pointercancel", (event) => {
    if (state.drag && state.drag.pointerId === event.pointerId) {
      finishDrag();
    }
  });
}

function attachDragHandlers() {
  graph.querySelectorAll(".resize-handle").forEach((handleElement) => {
    handleElement.addEventListener("pointerdown", (event) => {
      event.stopPropagation();
      const node = state.scenario.nodes.find((candidate) => candidate.id === handleElement.dataset.resizeNodeId);
      if (!node) {
        return;
      }

      graph.setPointerCapture(event.pointerId);
      state.drag = {
        mode: "resize",
        nodeId: node.id,
        pointerId: event.pointerId,
        lastPoint: toSvgPoint(event),
        startPoint: toSvgPoint(event),
        startWidth: node.width,
        startHeight: node.height
      };
    });
  });

  graph.querySelectorAll(".node").forEach((nodeElement) => {
    nodeElement.addEventListener("pointerdown", (event) => {
      const nodeId = nodeElement.dataset.nodeId;
      const node = state.scenario.nodes.find((candidate) => candidate.id === nodeId);
      if (!node) {
        return;
      }

      graph.setPointerCapture(event.pointerId);
      nodeElement.classList.add("dragging");
      state.drag = {
        mode: "move",
        nodeId,
        pointerId: event.pointerId,
        lastPoint: toSvgPoint(event)
      };
    });
  });
}

function moveNode(node, currentPoint) {
  const deltaX = Math.round(currentPoint.x - state.drag.lastPoint.x);
  const deltaY = Math.round(currentPoint.y - state.drag.lastPoint.y);
  let nextX = node.x + deltaX;
  let nextY = node.y + deltaY;

  if (node.parentId) {
    const parent = state.scenario.nodes.find((candidate) => candidate.id === node.parentId);
    if (parent) {
      const maxX = Math.max(CONTAINER_PADDING, parent.width - node.width - CONTAINER_PADDING);
      const maxY = Math.max(CONTAINER_PADDING, parent.height - node.height - CONTAINER_PADDING);
      nextX = clamp(nextX, CONTAINER_PADDING, maxX);
      nextY = clamp(nextY, CONTAINER_PADDING, maxY);
    }
  }

  node.x = nextX;
  node.y = nextY;
}

function resizeContainer(node, currentPoint) {
  const children = state.scenario.nodes.filter((candidate) => candidate.parentId === node.id);
  const childrenWidth = children.reduce(
    (requiredWidth, child) => Math.max(requiredWidth, child.x + child.width + CONTAINER_PADDING),
    0
  );
  const childrenHeight = children.reduce(
    (requiredHeight, child) => Math.max(requiredHeight, child.y + child.height + CONTAINER_PADDING),
    0
  );
  const minWidth = Math.max(MIN_CONTAINER_WIDTH, childrenWidth);
  const minHeight = Math.max(MIN_CONTAINER_HEIGHT, childrenHeight);
  const deltaX = currentPoint.x - state.drag.startPoint.x;
  const deltaY = currentPoint.y - state.drag.startPoint.y;

  node.width = Math.max(minWidth, Math.round(state.drag.startWidth + deltaX));
  node.height = Math.max(minHeight, Math.round(state.drag.startHeight + deltaY));
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function finishDrag() {
  graph.querySelectorAll(".node.dragging").forEach((nodeElement) => {
    nodeElement.classList.remove("dragging");
  });
  state.drag = null;
}

function toSvgPoint(event) {
  const point = graph.createSVGPoint();
  point.x = event.clientX;
  point.y = event.clientY;
  return point.matrixTransform(graph.getScreenCTM().inverse());
}

function nodeCssState(nodeId, stepView) {
  const classes = [];

  if (stepView.activeNodeIds.includes(nodeId)) {
    classes.push("active");
  }
  if (stepView.readyNodeIds.includes(nodeId)) {
    classes.push("ready");
  }
  if (stepView.failedNodeIds.includes(nodeId)) {
    classes.push("failed");
  }
  if (stepView.busyNodeIds.includes(nodeId)) {
    classes.push("busy");
  }

  return classes.join(" ");
}

function edgeCssState(edgeId, stepView) {
  if (stepView.readyEdgeIds.includes(edgeId)) {
    return "ready";
  }
  if (stepView.activeEdgeIds.includes(edgeId)) {
    return "active";
  }
  return "";
}

function collectIdsByStatus(statusMap, acceptedStatuses) {
  return Object.entries(statusMap)
    .filter(([, status]) => acceptedStatuses.includes(status))
    .map(([id]) => id);
}

function uniqueIds(ids) {
  return [...new Set(ids)];
}

function formatScenarioOrder(scenario) {
  return typeof scenario.order === "number"
    ? String(scenario.order).padStart(3, "0")
    : "---";
}


function updateUrl(nextScenarioId) {
  const url = new URL(window.location.href);
  url.searchParams.set("scenario", nextScenarioId);
  window.history.replaceState({}, "", url);
}

function colorForType(type) {
  switch (type) {
    case "external":
      return "#2563eb";
    case "container":
      return "#0f172a";
    case "service":
      return "#0f766e";
    case "broker":
      return "#b45309";
    case "consumer":
      return "#7c3aed";
    case "monitor":
      return "#be185d";
    default:
      return "#475569";
  }
}

function renderTextLines(lines, x, startY, lineHeight, cssClass, textAnchor = "start") {
  return `
    <text class="${cssClass}" x="${x}" y="${startY}" text-anchor="${textAnchor}">
      ${lines.map((line, index) => `<tspan x="${x}" dy="${index === 0 ? 0 : lineHeight}">${escapeXml(line)}</tspan>`).join("")}
    </text>
  `;
}

function wrapText(text, fontSize, maxWidth) {
  if (!text) {
    return [""];
  }

  const words = text.split(/\s+/).filter(Boolean);
  const lines = [];
  let currentLine = "";

  words.forEach((word) => {
    const candidate = currentLine ? `${currentLine} ${word}` : word;
    if (estimateTextWidth(candidate, fontSize) <= maxWidth) {
      currentLine = candidate;
      return;
    }

    if (currentLine) {
      lines.push(currentLine);
    }

    if (estimateTextWidth(word, fontSize) <= maxWidth) {
      currentLine = word;
      return;
    }

    const chunks = breakLongWord(word, fontSize, maxWidth);
    lines.push(...chunks.slice(0, -1));
    currentLine = chunks[chunks.length - 1];
  });

  if (currentLine) {
    lines.push(currentLine);
  }

  return lines;
}

function breakLongWord(word, fontSize, maxWidth) {
  const chunks = [];
  let currentChunk = "";

  for (const char of word) {
    const candidate = currentChunk + char;
    if (currentChunk && estimateTextWidth(candidate, fontSize) > maxWidth) {
      chunks.push(currentChunk);
      currentChunk = char;
      continue;
    }

    currentChunk = candidate;
  }

  if (currentChunk) {
    chunks.push(currentChunk);
  }

  return chunks;
}

function estimateTextWidth(text, fontSize) {
  return text.length * fontSize * 0.58;
}

function escapeXml(text) {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&apos;");
}
