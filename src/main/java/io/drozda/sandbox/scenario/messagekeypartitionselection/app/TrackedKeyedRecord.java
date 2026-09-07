package io.drozda.sandbox.scenario.messagekeypartitionselection.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.messagekeypartitionselection.model.KeyedOrderEvent;

public record TrackedKeyedRecord(
        ConsumerRecord<String, KeyedOrderEvent> record,
        String consumerId
) {
}
