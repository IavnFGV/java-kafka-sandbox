package io.drozda.sandbox.scenario.partitionordering.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.partitionordering.model.OrderedOrderEvent;

public record TrackedOrderedRecord(
        ConsumerRecord<String, OrderedOrderEvent> record,
        String consumerId
) {
}
