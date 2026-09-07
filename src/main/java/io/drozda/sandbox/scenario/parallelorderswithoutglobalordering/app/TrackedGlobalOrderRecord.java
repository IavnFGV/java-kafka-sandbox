package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.model.GlobalOrderEvent;

record TrackedGlobalOrderRecord(
        ConsumerRecord<String, GlobalOrderEvent> record,
        String consumerId,
        long completedAfterMs
) {
}
