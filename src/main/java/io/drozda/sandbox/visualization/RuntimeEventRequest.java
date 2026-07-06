package io.drozda.sandbox.visualization;

public record RuntimeEventRequest(
        String scenarioId,
        String type,
        String nodeId,
        String edgeId,
        String label,
        String fromNodeId,
        String toNodeId,
        String status
) {
}
