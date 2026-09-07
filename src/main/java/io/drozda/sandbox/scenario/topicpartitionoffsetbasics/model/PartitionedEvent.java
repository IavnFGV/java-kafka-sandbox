package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.model;

public record PartitionedEvent(
        String eventId,
        String label,
        long createdAtEpochMillis
) {
}
