package io.drozda.sandbox.visualization;

import java.util.List;
import java.util.Map;

public record ActiveScenarioRuntimeState(
        String scenarioId,
        int currentStepIndex,
        boolean active,
        boolean completed,
        String testName,
        Map<String, String> nodeStatuses,
        Map<String, String> edgeStatuses,
        List<RuntimeSignal> activeSignals,
        String lastEventType,
        String lastEventLabel
) {
}
