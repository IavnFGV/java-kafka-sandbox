package io.drozda.sandbox.scenario.earliestvslatest.app;

public record OffsetResetObservation(String group, String phase, int sequence, int partition, long offset) {
}
