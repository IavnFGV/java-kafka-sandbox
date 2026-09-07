package io.drozda.sandbox.scenario.earliestvslatest.model;

public record OffsetResetEvent(String eventId, String phase, int sequence, long createdAt) {
}
