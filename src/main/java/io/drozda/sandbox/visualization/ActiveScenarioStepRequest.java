package io.drozda.sandbox.visualization;

public record ActiveScenarioStepRequest(
        String scenarioId,
        int stepIndex
) {
}
