package io.drozda.sandbox.visualization;

public record ScenarioNode(
        String id,
        String label,
        String type,
        int x,
        int y,
        String description
) {
}
