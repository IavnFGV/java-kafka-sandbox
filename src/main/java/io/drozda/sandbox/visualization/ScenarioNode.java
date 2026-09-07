package io.drozda.sandbox.visualization;

import java.util.List;

public record ScenarioNode(
        String id,
        String label,
        String type,
        int x,
        int y,
        int width,
        int height,
        String parentId,
        String description,
        List<ScenarioSourceReference> sourceReferences
) {
    public ScenarioNode(String id, String label, String type, int x, int y, int width,
            int height, String parentId, String description) {
        this(id, label, type, x, y, width, height, parentId, description, List.of());
    }

    public ScenarioNode {
        sourceReferences = List.copyOf(sourceReferences);
    }

    public ScenarioNode withSourceReferences(List<ScenarioSourceReference> references) {
        return new ScenarioNode(id, label, type, x, y, width, height, parentId, description, references);
    }
}
