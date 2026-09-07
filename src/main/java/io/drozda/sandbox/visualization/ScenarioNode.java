package io.drozda.sandbox.visualization;

public record ScenarioNode(
        String id,
        String label,
        String type,
        int x,
        int y,
        int width,
        int height,
        String parentId,
        String description
) {
}
