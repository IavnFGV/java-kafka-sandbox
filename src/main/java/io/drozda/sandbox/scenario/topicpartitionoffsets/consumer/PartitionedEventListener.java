package io.drozda.sandbox.scenario.topicpartitionoffsets.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.topicpartitionoffsets.app.PartitionedEventTracker;
import io.drozda.sandbox.scenario.topicpartitionoffsets.model.PartitionedEvent;

public class PartitionedEventListener {

    private final PartitionedEventTracker tracker;

    public PartitionedEventListener(PartitionedEventTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(topics = "${app.kafka.topics.partition-offsets}")
    public void onEvent(ConsumerRecord<String, PartitionedEvent> record) {
        tracker.received(record);
    }
}
