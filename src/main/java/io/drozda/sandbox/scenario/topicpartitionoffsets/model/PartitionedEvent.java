package io.drozda.sandbox.scenario.topicpartitionoffsets.model;

public record PartitionedEvent(
        String eventId,
        String label,
        long createdAtEpochMillis
) {
}
