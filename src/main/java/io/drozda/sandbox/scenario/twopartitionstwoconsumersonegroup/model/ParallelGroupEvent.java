package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model;

public record ParallelGroupEvent(String eventId, int targetPartition, int sequence, long createdAt) {
}
