package io.drozda.sandbox.scenario.messagekeypartitionselection.app;

public record KeyPartitioningCommandRequest(String invocationName, KeyStrategy keyStrategy) {
}
