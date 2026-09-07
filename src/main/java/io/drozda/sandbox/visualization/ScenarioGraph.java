package io.drozda.sandbox.visualization;

import java.util.List;

public record ScenarioGraph(
        String id,
        int order,
        String title,
        String summary,
        String practicalPurpose,
        List<Integer> backlogItems,
        int viewportWidth,
        int viewportHeight,
        List<ScenarioNode> nodes,
        List<ScenarioEdge> edges,
        List<ScenarioStep> steps,
        List<VisualizationEvent> events,
        String sourceRoot
) {
    public ScenarioGraph(
            String id, int order, String title, String summary, String practicalPurpose,
            List<Integer> backlogItems, int viewportWidth, int viewportHeight,
            List<ScenarioNode> nodes, List<ScenarioEdge> edges,
            List<ScenarioStep> steps, List<VisualizationEvent> events) {
        this(id, order, title, summary, practicalPurpose, backlogItems, viewportWidth,
                viewportHeight, nodes, edges, steps, events, null);
    }

    public ScenarioGraph withSourceRoot(String path) {
        return new ScenarioGraph(id, order, title, summary, practicalPurpose, backlogItems,
                viewportWidth, viewportHeight, nodes.stream()
                        .map(node -> node.withSourceReferences(ScenarioSourceCatalog.references(id, node.id())))
                        .toList(), edges, steps, events, path);
    }
}
