package io.drozda.sandbox.visualization;

public record RuntimeEventRequest(
        String scenarioId,
        String type,
        String nodeId,
        String edgeId,
        String label,
        String fromNodeId,
        String toNodeId,
        String status,
        String playbackGroup
) {
    public RuntimeEventRequest(
            String scenarioId, String type, String nodeId, String edgeId,
            String label, String fromNodeId, String toNodeId, String status) {
        this(scenarioId, type, nodeId, edgeId, label, fromNodeId, toNodeId, status, null);
    }
}
