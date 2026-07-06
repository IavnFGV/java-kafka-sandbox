const state = {
  scenario: null,
  stepIndex: 0,
  timerId: null,
  drag: null
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

Promise.all([
  fetchJson("/api/scenarios/trade-flow"),
  fetchJson("/api/scenarios/trade-flow/runtime")
]).then(([scenario, runtime]) => {
  state.scenario = scenario;
  state.stepIndex = runtime.currentStepIndex;
  title.textContent = scenario.title;
  summary.textContent = scenario.summary;
  graph.setAttribute("viewBox", `0 0 ${scenario.viewportWidth} ${scenario.viewportHeight}`);
  render();
});

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
  moveRuntime("/api/scenarios/trade-flow/runtime/previous");
});

nextBtn.addEventListener("click", () => {
  stopPlayback();
  moveRuntime("/api/scenarios/trade-flow/runtime/next");
});

resetBtn.addEventListener("click", () => {
  stopPlayback();
  moveRuntime("/api/scenarios/trade-flow/runtime/reset");
});

playBtn.addEventListener("click", () => {
  if (state.timerId) {
    stopPlayback();
    return;
  }

  playBtn.textContent = "Pause";
  state.timerId = window.setInterval(() => {
    moveRuntime("/api/scenarios/trade-flow/runtime/next", false);
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

  const step = state.scenario.steps[state.stepIndex];
  const stepView = buildStepView(step, state.scenario.events);
  stepTitle.textContent = step.title;
  stepDescription.textContent = step.description;
  const layout = buildLayout(state.scenario.nodes);

  graph.innerHTML = `
    <defs>
      <marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L0,6 L9,3 z" fill="rgba(31, 41, 55, 0.35)"></path>
      </marker>
    </defs>
    ${renderEdges(state.scenario.edges, layout, stepView)}
    ${renderNodes(layout, stepView)}
    ${renderSignal(layout, stepView)}
  `;
  attachDragHandlers();
}

function moveRuntime(url, shouldRender = true) {
  fetchJson(url, { method: "POST" }).then((runtime) => {
    state.stepIndex = runtime.currentStepIndex;
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
    const active = stepView.activeEdgeIds.includes(edge.id) ? "active" : "";
    const x1 = from.centerX;
    const y1 = from.centerY;
    const x2 = to.centerX;
    const y2 = to.centerY;
    const labelX = (x1 + x2) / 2;
    const labelY = (y1 + y2) / 2 - 12;

    return `
      <g>
        <line class="edge ${active}" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" marker-end="url(#arrow)"></line>
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
    const stroke = colorForType(node.type);
    const typeClass = node.type === "container" ? "node-container" : "";

    return `
      <g class="node ${typeClass} ${active}" data-node-id="${node.id}" transform="translate(${absoluteX}, ${absoluteY})">
        <rect class="node-card" width="${node.width}" height="${node.height}" rx="${node.type === "container" ? 28 : 20}" stroke="${stroke}"></rect>
        <text class="node-title" x="18" y="30">${node.label}</text>
        <text class="node-copy" x="18" y="54">${node.description}</text>
      </g>
    `;
  }).join("");
}

function renderSignal(layout, stepView) {
  if (!stepView.signalFromId || !stepView.signalToId) {
    return "";
  }

  const from = layout.get(stepView.signalFromId);
  const to = layout.get(stepView.signalToId);
  if (!from || !to) {
    return "";
  }

  const label = stepView.signalLabel || "message";
  const startX = from.centerX;
  const startY = from.centerY;
  const endX = to.centerX;
  const endY = to.centerY;
  const midX = (startX + endX) / 2;
  const midY = (startY + endY) / 2 - 18;

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

  const activeNodeIds = [...new Set(selectedEvents.flatMap((event) => event.activeNodeIds || []))];
  const activeEdgeIds = [...new Set(selectedEvents.flatMap((event) => event.activeEdgeIds || []))];
  const primaryEvent = selectedEvents[selectedEvents.length - 1] || null;

  return {
    activeNodeIds,
    activeEdgeIds,
    signalLabel: primaryEvent ? primaryEvent.label : null,
    signalFromId: primaryEvent ? primaryEvent.signalFromId : null,
    signalToId: primaryEvent ? primaryEvent.signalToId : null
  };
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
