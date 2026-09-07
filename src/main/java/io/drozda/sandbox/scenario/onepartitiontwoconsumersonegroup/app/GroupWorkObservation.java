package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app;

public record GroupWorkObservation(
        String phase,
        int sequence,
        int partition,
        long offset,
        String consumerId
) {
}
