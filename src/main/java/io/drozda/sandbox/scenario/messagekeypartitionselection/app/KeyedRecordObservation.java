package io.drozda.sandbox.scenario.messagekeypartitionselection.app;

public record KeyedRecordObservation(
        String orderId,
        String status,
        int partition,
        long offset,
        String consumerId
) {
}
