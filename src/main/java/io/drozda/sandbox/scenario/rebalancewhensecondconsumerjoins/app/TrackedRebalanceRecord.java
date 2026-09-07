package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent;

public record TrackedRebalanceRecord(ConsumerRecord<String, RebalanceEvent> record, String consumer) {
}
