package io.drozda.sandbox.scenario.globalordering.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.globalordering.model.GlobalOrderEvent;

record TrackedGlobalOrderRecord(
        ConsumerRecord<String, GlobalOrderEvent> record,
        String consumerId,
        long completedAfterMs
) {
}
