package io.drozda.sandbox.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ScenarioRuntimeService {

    private static final String STATUS_READY = "READY";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BUSY = "BUSY";
    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_FAILED = "FAILED";
    private static final String EVENT_SIGNAL_STARTED = "SIGNAL-STARTED";
    private static final String EVENT_SIGNAL_FINISHED = "SIGNAL-FINISHED";
    private static final String EVENT_SIGNAL_DELIVERED = "SIGNAL-DELIVERED";
    private static final String EVENT_COMPONENT_READY = "COMPONENT-READY";
    private static final String EVENT_COMPONENT_BUSY = "COMPONENT-BUSY";
    private static final String EVENT_COMPONENT_WAITING = "COMPONENT-WAITING";
    private static final String EVENT_COMPONENT_FAILED = "COMPONENT-FAILED";
    private static final String EVENT_RUNTIME_RESET = "RUNTIME-RESET";
    private static final String EVENT_SESSION_COMPLETED = "SESSION-COMPLETED";

    private final Map<String, Integer> currentStepByScenario = new ConcurrentHashMap<>();
    private volatile ActiveScenarioRuntimeState activeRuntime = emptyRuntime();

    public ScenarioRuntimeState getState(ScenarioGraph scenario) {
        int stepIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        return new ScenarioRuntimeState(scenario.id(), clamp(stepIndex, scenario));
    }

    public ScenarioRuntimeState next(ScenarioGraph scenario) {
        int currentIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        int nextIndex = (currentIndex + 1) % scenario.steps().size();
        currentStepByScenario.put(scenario.id(), nextIndex);
        return new ScenarioRuntimeState(scenario.id(), nextIndex);
    }

    public ScenarioRuntimeState previous(ScenarioGraph scenario) {
        int currentIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        int previousIndex = (currentIndex - 1 + scenario.steps().size()) % scenario.steps().size();
        currentStepByScenario.put(scenario.id(), previousIndex);
        return new ScenarioRuntimeState(scenario.id(), previousIndex);
    }

    public ScenarioRuntimeState reset(ScenarioGraph scenario) {
        currentStepByScenario.put(scenario.id(), 0);
        return new ScenarioRuntimeState(scenario.id(), 0);
    }

    public ActiveScenarioRuntimeState activeRuntime() {
        return activeRuntime;
    }

    public ActiveScenarioRuntimeState startActiveSession(ScenarioGraph scenario, String testName) {
        currentStepByScenario.put(scenario.id(), 0);
        activeRuntime = new ActiveScenarioRuntimeState(
                scenario.id(),
                0,
                true,
                false,
                testName,
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new ArrayList<>(),
                "session-started",
                testName
        );
        return activeRuntime;
    }

    public ActiveScenarioRuntimeState updateActiveStep(ScenarioGraph scenario, int stepIndex) {
        int clamped = clamp(stepIndex, scenario);
        currentStepByScenario.put(scenario.id(), clamped);
        activeRuntime = new ActiveScenarioRuntimeState(
                scenario.id(),
                clamped,
                true,
                false,
                activeRuntime.testName(),
                copy(activeRuntime.nodeStatuses()),
                copy(activeRuntime.edgeStatuses()),
                copySignals(activeRuntime.activeSignals()),
                "step-changed",
                "Step " + clamped
        );
        return activeRuntime;
    }

    public ActiveScenarioRuntimeState completeActiveSession(ScenarioGraph scenario) {
        int currentIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        activeRuntime = new ActiveScenarioRuntimeState(
                scenario.id(),
                clamp(currentIndex, scenario),
                false,
                true,
                activeRuntime.testName(),
                copy(activeRuntime.nodeStatuses()),
                copy(activeRuntime.edgeStatuses()),
                copySignals(activeRuntime.activeSignals()),
                "session-completed",
                activeRuntime.testName()
        );
        return activeRuntime;
    }

    public ActiveScenarioRuntimeState applyRuntimeEvent(ScenarioGraph scenario, RuntimeEventRequest request) {
        ActiveScenarioRuntimeState baseRuntime = runtimeForScenario(scenario);
        Map<String, String> nodeStatuses = copy(baseRuntime.nodeStatuses());
        Map<String, String> edgeStatuses = copy(baseRuntime.edgeStatuses());
        List<RuntimeSignal> activeSignals = copySignals(baseRuntime.activeSignals());
        String eventType = normalizeType(request.type());
        String status = normalizeStatus(request.status());
        boolean active = true;
        boolean completed = false;

        if (EVENT_RUNTIME_RESET.equals(eventType)) {
            nodeStatuses.clear();
            edgeStatuses.clear();
            activeSignals.clear();
        }

        switch (eventType) {
            case EVENT_COMPONENT_READY -> status = defaultStatus(status, STATUS_READY);
            case EVENT_COMPONENT_BUSY -> status = defaultStatus(status, STATUS_BUSY);
            case EVENT_COMPONENT_WAITING -> status = defaultStatus(status, STATUS_WAITING);
            case EVENT_COMPONENT_FAILED -> status = defaultStatus(status, STATUS_FAILED);
            case EVENT_SIGNAL_STARTED -> {
                String signalState = defaultStatus(status, STATUS_ACTIVE);
                updateSignal(activeSignals, request, true, signalState);
                if (request.edgeId() != null) {
                    edgeStatuses.put(request.edgeId(), signalState);
                }
            }
            case EVENT_SIGNAL_FINISHED, EVENT_SIGNAL_DELIVERED -> {
                updateSignal(activeSignals, request, false, null);
                if (request.edgeId() != null) {
                    edgeStatuses.put(request.edgeId(), defaultStatus(status, STATUS_READY));
                }
            }
            case EVENT_SESSION_COMPLETED -> {
                active = false;
                completed = true;
            }
            default -> {
                // Generic event: rely on explicit status fields below.
            }
        }

        if (request.nodeId() != null && status != null) {
            nodeStatuses.put(request.nodeId(), status);
        }

        if (request.edgeId() != null && status != null) {
            edgeStatuses.put(request.edgeId(), status);
        }

        activeRuntime = new ActiveScenarioRuntimeState(
                scenario.id(),
                currentStepByScenario.getOrDefault(scenario.id(), 0),
                active,
                completed,
                baseRuntime.testName(),
                nodeStatuses,
                edgeStatuses,
                activeSignals,
                request.type(),
                request.label()
        );
        return activeRuntime;
    }

    private String signalId(RuntimeEventRequest request) {
        return request.fromNodeId() + "->" + request.toNodeId() + ":" + (request.label() != null ? request.label() : "message");
    }

    private void updateSignal(List<RuntimeSignal> activeSignals, RuntimeEventRequest request, boolean addSignal, String state) {
        if (request.fromNodeId() == null || request.toNodeId() == null) {
            return;
        }

        String signalId = signalId(request);
        activeSignals.removeIf(signal -> signal.id().equals(signalId));

        if (addSignal) {
            activeSignals.add(new RuntimeSignal(
                    signalId,
                    request.label() != null ? request.label() : "message",
                    request.fromNodeId(),
                    request.toNodeId(),
                    state
            ));
        }
    }

    private ActiveScenarioRuntimeState runtimeForScenario(ScenarioGraph scenario) {
        if (scenario.id().equals(activeRuntime.scenarioId())) {
            return activeRuntime;
        }

        return new ActiveScenarioRuntimeState(
                scenario.id(),
                currentStepByScenario.getOrDefault(scenario.id(), 0),
                true,
                false,
                activeRuntime.testName(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new ArrayList<>(),
                "session-started",
                scenario.title()
        );
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase().replace('_', '-');
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase();
    }

    private String defaultStatus(String status, String fallback) {
        return status != null ? status : fallback;
    }

    private Map<String, String> copy(Map<String, String> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private List<RuntimeSignal> copySignals(List<RuntimeSignal> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private ActiveScenarioRuntimeState emptyRuntime() {
        return new ActiveScenarioRuntimeState(null, 0, false, false, null,
                new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(), null, null);
    }

    private int clamp(int stepIndex, ScenarioGraph scenario) {
        if (scenario.steps().isEmpty()) {
            return 0;
        }

        if (stepIndex < 0 || stepIndex >= scenario.steps().size()) {
            return 0;
        }

        return stepIndex;
    }
}
