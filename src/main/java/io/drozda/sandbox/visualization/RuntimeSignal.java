package io.drozda.sandbox.visualization;

public record RuntimeSignal(
        String id,
        String label,
        String fromNodeId,
        String toNodeId,
        String state
) {
}
