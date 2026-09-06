package io.drozda.sandbox.visualization;

public record ScenarioRuntimeUpdate(
        long revision,
        ActiveScenarioRuntimeState runtime
) {
}
