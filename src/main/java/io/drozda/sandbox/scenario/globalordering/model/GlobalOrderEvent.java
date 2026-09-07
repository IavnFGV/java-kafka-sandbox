package io.drozda.sandbox.scenario.globalordering.model;

public record GlobalOrderEvent(
        String eventId,
        String orderId,
        int sequence,
        String status,
        long processingDelayMs,
        long createdAt
) {
}
