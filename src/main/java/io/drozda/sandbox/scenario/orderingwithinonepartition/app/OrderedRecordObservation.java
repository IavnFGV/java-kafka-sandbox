package io.drozda.sandbox.scenario.orderingwithinonepartition.app;

public record OrderedRecordObservation(
        int sequence,
        String status,
        int partition,
        long offset,
        String consumerId
) {
}
