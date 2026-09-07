package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app;

public record GlobalOrderObservation(
        String orderId,
        int sequence,
        String status,
        int partition,
        long offset,
        String consumerId,
        long completedAfterMs
) {
}
