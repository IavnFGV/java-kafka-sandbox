package io.drozda.sandbox.visualization;

public record ActiveScenarioRuntimeState(
        String scenarioId,
        int currentStepIndex,
        boolean active,
        boolean completed,
        String testName
) {
}
