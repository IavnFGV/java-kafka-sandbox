package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent;

record TrackedParallelGroupRecord(ConsumerRecord<String, ParallelGroupEvent> record, String consumerId) {
}
