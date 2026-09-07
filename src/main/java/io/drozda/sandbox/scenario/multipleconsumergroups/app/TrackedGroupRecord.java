package io.drozda.sandbox.scenario.multipleconsumergroups.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;

public record TrackedGroupRecord(ConsumerRecord<String, SharedOrderEvent> record, String groupId) {
}
