package io.drozda.sandbox.scenario.keypartitioning.app;

public record KeyedRecordObservation(
        String orderId,
        String status,
        int partition,
        long offset,
        String consumerId
) {
}
