package io.drozda.sandbox.scenario.orderingwithinonepartition.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.orderingwithinonepartition.model.OrderedOrderEvent;

public record TrackedOrderedRecord(
        ConsumerRecord<String, OrderedOrderEvent> record,
        String consumerId
) {
}
