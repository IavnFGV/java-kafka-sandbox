package io.drozda.sandbox.visualization;

public record ActiveScenarioSessionRequest(
        String scenarioId,
        String testName
) {
}
