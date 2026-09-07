package io.drozda.sandbox.scenario.messagekeypartitionselection.model;

public record KeyedOrderEvent(String eventId, String orderId, String status, long createdAtEpochMillis) {
}
