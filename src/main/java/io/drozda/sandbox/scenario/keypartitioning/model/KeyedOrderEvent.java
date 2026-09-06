package io.drozda.sandbox.scenario.keypartitioning.model;

public record KeyedOrderEvent(String eventId, String orderId, String status, long createdAtEpochMillis) {
}
