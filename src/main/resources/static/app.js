const POLL_INTERVAL_MS = 500;
const PLAYBACK_INTERVAL_MS = 1800;
const DEFAULT_SCENARIO_ID = "system-ready";

const state = {
  scenarios: [],
  scenario: null,
  stepIndex: 0,
  timerId: null,
  drag: null,
  activeScenarioId: null,
  activeRuntime: null
};

const graph = document.getElementById("graph");
const scenarioList = document.getElementById("scenario-list");
const title = document.getElementById("scenario-title");
const summary = document.getElementById("scenario-summary");
const stepTitle = document.getElementById("step-title");
const stepDescription = document.getElementById("step-description");
const prevBtn = document.getElementById("prev-btn");
const playBtn = document.getElementById("play-btn");
const nextBtn = document.getElementById("next-btn");
const resetBtn = document.getElementById("reset-btn");
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
  prevBtn.addEventListener("click", () => {
    stopPlayback();
    moveRuntime(`/api/scenarios/${scenarioId}/runtime/previous`);
  });

  nextBtn.addEventListener("click", () => {
    stopPlayback();
    moveRuntime(`/api/scenarios/${scenarioId}/runtime/next`);
  });

  resetBtn.addEventListener("click", () => {
    stopPlayback();
    moveRuntime(`/api/scenarios/${scenarioId}/runtime/reset`);
  });

  playBtn.addEventListener("click", togglePlayback);
}

function togglePlayback() {
  if (state.timerId) {
    stopPlayback();
    return;
  }

  playBtn.textContent = "Pause";
  state.timerId = window.setInterval(() => {
    moveRuntime(`/api/scenarios/${scenarioId}/runtime/next`, false);
  }, PLAYBACK_INTERVAL_MS);
}

function stopPlayback() {
  if (!state.timerId) {
    return;
  }

  window.clearInterval(state.timerId);
  state.timerId = null;
  playBtn.textContent = "Play";
}

function syncActiveRuntime() {
  fetchJson("/api/scenarios/runtime/active")
    .then((runtime) => {
      if (!runtime || !runtime.scenarioId) {
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

function moveRuntime(url, shouldRender = true) {
  fetchJson(url, { method: "POST" }).then((runtime) => {
    state.stepIndex = runtime.currentStepIndex;
    state.activeRuntime = runtime;

    if (shouldRender) {
      render();
      return;
    }

    render();
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

  const stepView = buildCurrentStepView();
  const layout = buildLayout(state.scenario.nodes);

  stepTitle.textContent = currentTitle();
  stepDescription.textContent = currentDescription();
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

function renderScenarioList() {
  if (!scenarioList) {
    return;
  }

  scenarioList.innerHTML = state.scenarios.map((scenario) => `
    <button
      type="button"
      class="scenario-link ${scenario.id === state.activeScenarioId ? "active" : ""}"
      data-scenario-id="${scenario.id}">
      <span class="scenario-link-title">${escapeXml(scenario.title)}</span>
      <span class="scenario-link-copy">${escapeXml(shortScenarioSummary(scenario.summary))}</span>
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

  stopPlayback();
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
  const titleMarkup = renderTextLines(titleLines, 18, 30, 22, "node-title");

  return `
    <g class="node ${typeClass} ${nodeState}" data-node-id="${node.id}" transform="translate(${absoluteX}, ${absoluteY})">
      <rect class="node-ready-ring" x="${ringInset}" y="${ringInset}" width="${node.width - ringInset * 2}" height="${node.height - ringInset * 2}" rx="${node.type === "container" ? 22 : 14}"></rect>
      <rect class="node-card" width="${node.width}" height="${node.height}" rx="${node.type === "container" ? 28 : 20}" stroke="${stroke}"></rect>
      ${titleMarkup}
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
    const deltaX = currentPoint.x - state.drag.lastPoint.x;
    const deltaY = currentPoint.y - state.drag.lastPoint.y;

    node.x += Math.round(deltaX);
    node.y += Math.round(deltaY);
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
  graph.querySelectorAll(".node").forEach((nodeElement) => {
    nodeElement.addEventListener("pointerdown", (event) => {
      const nodeId = nodeElement.dataset.nodeId;
      const node = state.scenario.nodes.find((candidate) => candidate.id === nodeId);
      if (!node) {
        return;
      }

      nodeElement.setPointerCapture(event.pointerId);
      nodeElement.classList.add("dragging");
      state.drag = {
        nodeId,
        pointerId: event.pointerId,
        lastPoint: toSvgPoint(event)
      };
    });
  });
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

function shortScenarioSummary(summaryText) {
  if (!summaryText) {
    return "";
  }

  if (summaryText.length <= 72) {
    return summaryText;
  }

  return `${summaryText.slice(0, 69).trimEnd()}...`;
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

function renderTextLines(lines, x, startY, lineHeight, cssClass) {
  return `
    <text class="${cssClass}" x="${x}" y="${startY}">
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
