package io.drozda.sandbox.scenario.orderingwithinonepartition.model;

public record OrderedOrderEvent(
        String eventId,
        String orderId,
        int sequence,
        String status,
        long createdAt
) {
}
