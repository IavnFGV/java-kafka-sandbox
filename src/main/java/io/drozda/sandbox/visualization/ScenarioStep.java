package io.drozda.sandbox.visualization;

public record ScenarioStep(
        String id,
        String title,
        String description,
        String activeNodeId,
        String activeEdgeId
) {
}
