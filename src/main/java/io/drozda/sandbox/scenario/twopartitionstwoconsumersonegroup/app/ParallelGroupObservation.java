package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

public record ParallelGroupObservation(int sequence, int partition, long offset, String consumerId) {
}
