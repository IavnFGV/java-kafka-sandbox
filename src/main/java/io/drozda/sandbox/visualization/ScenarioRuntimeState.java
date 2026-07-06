package io.drozda.sandbox.visualization;

public record ScenarioRuntimeState(
        String scenarioId,
        int currentStepIndex
) {
}
