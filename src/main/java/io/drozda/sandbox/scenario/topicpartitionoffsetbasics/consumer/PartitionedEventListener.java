package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app.PartitionedEventTracker;
import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.model.PartitionedEvent;

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
