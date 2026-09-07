package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.model.GroupWorkEvent;

record TrackedGroupWorkRecord(
        ConsumerRecord<String, GroupWorkEvent> record,
        String consumerId
) {
}
