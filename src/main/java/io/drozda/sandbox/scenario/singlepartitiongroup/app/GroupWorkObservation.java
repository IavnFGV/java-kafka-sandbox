package io.drozda.sandbox.scenario.singlepartitiongroup.app;

public record GroupWorkObservation(
        String phase,
        int sequence,
        int partition,
        long offset,
        String consumerId
) {
}
