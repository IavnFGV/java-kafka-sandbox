package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

public record RebalanceObservation(String phase, int sequence, int partition, long offset, String consumer) {
}
