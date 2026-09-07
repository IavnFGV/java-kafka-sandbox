package io.drozda.sandbox.visualization;

import java.util.List;

public record VisualizationEvent(
        String id,
        String type,
        String label,
        String description,
        List<String> activeNodeIds,
        List<String> activeEdgeIds,
        String signalFromId,
        String signalToId
) {
}
