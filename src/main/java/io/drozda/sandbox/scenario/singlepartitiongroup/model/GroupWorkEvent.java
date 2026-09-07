package io.drozda.sandbox.scenario.singlepartitiongroup.model;

public record GroupWorkEvent(
        String eventId,
        String phase,
        int sequence,
        long createdAt
) {
}
