const state = {
  scenario: null,
  stepIndex: 0,
  timerId: null
};

const graph = document.getElementById("graph");
const title = document.getElementById("scenario-title");
const summary = document.getElementById("scenario-summary");
const stepTitle = document.getElementById("step-title");
const stepDescription = document.getElementById("step-description");
const prevBtn = document.getElementById("prev-btn");
const playBtn = document.getElementById("play-btn");
const nextBtn = document.getElementById("next-btn");

fetch("/api/scenarios/trade-flow")
  .then((response) => response.json())
  .then((scenario) => {
    state.scenario = scenario;
    title.textContent = scenario.title;
    summary.textContent = scenario.summary;
    render();
  });

prevBtn.addEventListener("click", () => {
  stopPlayback();
  state.stepIndex = (state.stepIndex - 1 + state.scenario.steps.length) % state.scenario.steps.length;
  render();
});

nextBtn.addEventListener("click", () => {
  stopPlayback();
  state.stepIndex = (state.stepIndex + 1) % state.scenario.steps.length;
  render();
});

playBtn.addEventListener("click", () => {
  if (state.timerId) {
    stopPlayback();
    return;
  }

  playBtn.textContent = "Pause";
  state.timerId = window.setInterval(() => {
    state.stepIndex = (state.stepIndex + 1) % state.scenario.steps.length;
    render();
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
  stepTitle.textContent = step.title;
  stepDescription.textContent = step.description;

  graph.innerHTML = `
    <defs>
      <marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L0,6 L9,3 z" fill="rgba(31, 41, 55, 0.35)"></path>
      </marker>
    </defs>
    ${renderEdges(state.scenario.edges, state.scenario.nodes, step)}
    ${renderNodes(state.scenario.nodes, step)}
  `;
}

function renderEdges(edges, nodes, step) {
  return edges.map((edge) => {
    const from = nodes.find((node) => node.id === edge.from);
    const to = nodes.find((node) => node.id === edge.to);
    const active = edge.id === step.activeEdgeId ? "active" : "";
    const x1 = from.x + 80;
    const y1 = from.y + 34;
    const x2 = to.x;
    const y2 = to.y + 34;
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

function renderNodes(nodes, step) {
  return nodes.map((node) => {
    const active = node.id === step.activeNodeId ? "active" : "";
    const stroke = colorForType(node.type);

    return `
      <g class="node ${active}" transform="translate(${node.x}, ${node.y})">
        <rect class="node-card" width="160" height="88" rx="20" stroke="${stroke}"></rect>
        <text class="node-title" x="18" y="30">${node.label}</text>
        <text class="node-copy" x="18" y="54">${node.description}</text>
      </g>
    `;
  }).join("");
}

function colorForType(type) {
  switch (type) {
    case "external":
      return "#2563eb";
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
