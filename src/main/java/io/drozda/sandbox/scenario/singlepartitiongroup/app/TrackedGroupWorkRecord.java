package io.drozda.sandbox.scenario.singlepartitiongroup.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.singlepartitiongroup.model.GroupWorkEvent;

record TrackedGroupWorkRecord(
        ConsumerRecord<String, GroupWorkEvent> record,
        String consumerId
) {
}
