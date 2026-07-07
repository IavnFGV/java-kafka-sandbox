package io.drozda.sandbox.visualization;

import java.util.List;

public record ScenarioGraph(
        String id,
        int order,
        String title,
        String summary,
        int viewportWidth,
        int viewportHeight,
        List<ScenarioNode> nodes,
        List<ScenarioEdge> edges,
        List<ScenarioStep> steps,
        List<VisualizationEvent> events
) {
}
