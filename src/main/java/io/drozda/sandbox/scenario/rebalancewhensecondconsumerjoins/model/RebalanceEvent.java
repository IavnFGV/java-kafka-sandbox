package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model;

public record RebalanceEvent(String eventId, String phase, int partition, int sequence, long createdAt) {
}
