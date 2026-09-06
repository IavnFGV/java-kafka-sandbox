package io.drozda.sandbox.scenario.keypartitioning.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;

public record TrackedKeyedRecord(
        ConsumerRecord<String, KeyedOrderEvent> record,
        String consumerId
) {
}
