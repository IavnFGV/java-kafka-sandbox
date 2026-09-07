package io.drozda.sandbox.scenario.multipleconsumergroups.app;

public record ConsumerGroupObservation(int sequence, int partition, long offset, String groupId) {
}
