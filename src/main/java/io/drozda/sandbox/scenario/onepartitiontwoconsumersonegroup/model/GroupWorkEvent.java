package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.model;

public record GroupWorkEvent(
        String eventId,
        String phase,
        int sequence,
        long createdAt
) {
}
