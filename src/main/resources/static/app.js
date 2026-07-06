const state = {
  scenario: null,
  stepIndex: 0,
  timerId: null,
  drag: null,
  activeScenarioId: null,
  activeRuntime: null
};

const graph = document.getElementById("graph");
const title = document.getElementById("scenario-title");
const summary = document.getElementById("scenario-summary");
const stepTitle = document.getElementById("step-title");
const stepDescription = document.getElementById("step-description");
const prevBtn = document.getElementById("prev-btn");
const playBtn = document.getElementById("play-btn");
const nextBtn = document.getElementById("next-btn");
const resetBtn = document.getElementById("reset-btn");
const scenarioId = new URLSearchParams(window.location.search).get("scenario") || "system-ready";

Promise.all([
  fetchJson(`/api/scenarios/${scenarioId}`),
  fetchJson(`/api/scenarios/${scenarioId}/runtime`)
]).then(([scenario, runtime]) => {
  state.scenario = scenario;
  state.stepIndex = runtime.currentStepIndex;
  state.activeScenarioId = scenario.id;
  state.activeRuntime = null;
  title.textContent = scenario.title;
  summary.textContent = scenario.summary;
  graph.setAttribute("viewBox", `0 0 ${scenario.viewportWidth} ${scenario.viewportHeight}`);
  render();
});

window.setInterval(syncActiveRuntime, 500);

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

playBtn.addEventListener("click", () => {
  if (state.timerId) {
    stopPlayback();
    return;
  }

  playBtn.textContent = "Pause";
  state.timerId = window.setInterval(() => {
    moveRuntime(`/api/scenarios/${scenarioId}/runtime/next`, false);
  }, 1800);
});

function stopPlayback() {
  if (!state.timerId) {
    return;
  }

  window.clearInterval(state.timerId);
  state.timerId = null;
  playBtn.textContent = "Play";
}

function render() {
  if (!state.scenario) {
    return;
  }

  const liveMode = hasLiveRuntimeForScenario();
  const step = state.scenario.steps[state.stepIndex];
  const stepView = liveMode
    ? buildLiveView(state.activeRuntime)
    : buildStepView(step, state.scenario.events);

  stepTitle.textContent = liveMode
    ? (state.activeRuntime.lastEventLabel || "Live Runtime")
    : step.title;
  stepDescription.textContent = liveMode
    ? describeLiveRuntime(state.activeRuntime)
    : step.description;
  const layout = buildLayout(state.scenario.nodes);

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

function syncActiveRuntime() {
  fetchJson("/api/scenarios/runtime/active")
    .then((runtime) => {
      if (!runtime || !runtime.scenarioId) {
        return;
      }

      state.activeRuntime = runtime;

      if (runtime.scenarioId !== state.activeScenarioId) {
        return fetchJson(`/api/scenarios/${runtime.scenarioId}`).then((scenario) => {
          state.scenario = scenario;
          state.activeScenarioId = scenario.id;
          title.textContent = scenario.title;
          summary.textContent = scenario.summary;
          graph.setAttribute("viewBox", `0 0 ${scenario.viewportWidth} ${scenario.viewportHeight}`);
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

function renderEdges(edges, layout, stepView) {
  return edges.map((edge) => {
    const from = layout.get(edge.from);
    const to = layout.get(edge.to);
    const edgeState = stepView.readyEdgeIds.includes(edge.id)
      ? "ready"
      : stepView.activeEdgeIds.includes(edge.id)
        ? "active"
        : "";
    const x1 = from.centerX;
    const y1 = from.centerY;
    const x2 = to.centerX;
    const y2 = to.centerY;
    const labelX = (x1 + x2) / 2;
    const labelY = (y1 + y2) / 2 - 12;

    return `
      <g>
        <line class="edge ${edgeState}" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" marker-end="url(#arrow)"></line>
        <text class="edge-label" x="${labelX}" y="${labelY}" text-anchor="middle">${edge.label}</text>
      </g>
    `;
  }).join("");
}

function renderNodes(layout, stepView) {
  return Array.from(layout.values())
    .sort((left, right) => depth(left.node, layout) - depth(right.node, layout))
    .map((entry) => {
    const { node, absoluteX, absoluteY } = entry;
    const active = stepView.activeNodeIds.includes(node.id) ? "active" : "";
    const ready = stepView.readyNodeIds.includes(node.id) ? "ready" : "";
    const failed = stepView.failedNodeIds.includes(node.id) ? "failed" : "";
    const busy = stepView.busyNodeIds.includes(node.id) ? "busy" : "";
    const stroke = colorForType(node.type);
    const typeClass = node.type === "container" ? "node-container" : "";
    const ringInset = node.type === "container" ? 10 : 8;
    const titleLines = wrapText(node.label, 18, node.width - 36);
    const copyLines = wrapText(node.description, 12, node.width - 36);
    const titleMarkup = renderTextLines(titleLines, 18, 30, 22, "node-title");
    const copyStartY = 30 + Math.max(titleLines.length - 1, 0) * 22 + 24;
    const copyMarkup = renderTextLines(copyLines, 18, copyStartY, 17, "node-copy");

    return `
      <g class="node ${typeClass} ${active} ${ready} ${failed} ${busy}" data-node-id="${node.id}" transform="translate(${absoluteX}, ${absoluteY})">
        <rect class="node-ready-ring" x="${ringInset}" y="${ringInset}" width="${node.width - ringInset * 2}" height="${node.height - ringInset * 2}" rx="${node.type === "container" ? 22 : 14}"></rect>
        <rect class="node-card" width="${node.width}" height="${node.height}" rx="${node.type === "container" ? 28 : 20}" stroke="${stroke}"></rect>
        ${titleMarkup}
        ${copyMarkup}
      </g>
    `;
  }).join("");
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
  const startX = from.centerX;
  const startY = from.centerY;
  const endX = to.centerX;
  const endY = to.centerY;
  const offset = index * 18;
  const midX = (startX + endX) / 2;
  const midY = (startY + endY) / 2 - 18 - offset;

  return `
    <g class="signal">
      <text class="signal-label" x="${midX}" y="${midY}" text-anchor="middle">${label}</text>
      <circle class="signal-dot" r="8">
        <animate attributeName="cx" from="${startX}" to="${endX}" dur="1.4s" repeatCount="indefinite"></animate>
        <animate attributeName="cy" from="${startY}" to="${endY}" dur="1.4s" repeatCount="indefinite"></animate>
      </circle>
    </g>
  `;
}

function buildStepView(step, events) {
  const selectedEvents = step.eventIds
    .map((eventId) => events.find((event) => event.id === eventId))
    .filter(Boolean);

  const readyNodeIds = [...new Set(selectedEvents
    .filter((event) => event.type === "readiness" || event.type === "status")
    .flatMap((event) => event.activeNodeIds || []))];
  const readyEdgeIds = [...new Set(selectedEvents
    .filter((event) => event.type === "readiness" || event.type === "status")
    .flatMap((event) => event.activeEdgeIds || []))];
  const activeNodeIds = [...new Set(selectedEvents
    .flatMap((event) => event.activeNodeIds || [])
    .filter((nodeId) => !readyNodeIds.includes(nodeId)))];
  const activeEdgeIds = [...new Set(selectedEvents
    .flatMap((event) => event.activeEdgeIds || [])
    .filter((edgeId) => !readyEdgeIds.includes(edgeId)))];
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
  const readyNodeIds = [];
  const failedNodeIds = [];
  const busyNodeIds = [];
  const readyEdgeIds = [];
  const activeEdgeIds = [];

  Object.entries(nodeStatuses).forEach(([nodeId, status]) => {
    if (status === "READY") {
      readyNodeIds.push(nodeId);
    } else if (status === "FAILED") {
      failedNodeIds.push(nodeId);
    } else if (status === "BUSY" || status === "WAITING") {
      busyNodeIds.push(nodeId);
    }
  });

  Object.entries(edgeStatuses).forEach(([edgeId, status]) => {
    if (status === "READY") {
      readyEdgeIds.push(edgeId);
    } else if (status === "ACTIVE" || status === "BUSY") {
      activeEdgeIds.push(edgeId);
    }
  });

  return {
    activeNodeIds: busyNodeIds,
    activeEdgeIds,
    readyNodeIds,
    readyEdgeIds,
    failedNodeIds,
    busyNodeIds,
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
  const ready = Object.entries(nodeStatuses).filter(([, status]) => status === "READY").map(([nodeId]) => nodeId);
  const failed = Object.entries(nodeStatuses).filter(([, status]) => status === "FAILED").map(([nodeId]) => nodeId);
  const busy = Object.entries(nodeStatuses).filter(([, status]) => status === "BUSY" || status === "WAITING").map(([nodeId]) => nodeId);
  const activeEdges = Object.entries(edgeStatuses).filter(([, status]) => status === "ACTIVE" || status === "BUSY").map(([edgeId]) => edgeId);

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
