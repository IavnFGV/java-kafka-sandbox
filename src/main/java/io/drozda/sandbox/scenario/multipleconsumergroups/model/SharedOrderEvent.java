package io.drozda.sandbox.scenario.multipleconsumergroups.model;

public record SharedOrderEvent(String eventId, int sequence, long createdAt) {
}
