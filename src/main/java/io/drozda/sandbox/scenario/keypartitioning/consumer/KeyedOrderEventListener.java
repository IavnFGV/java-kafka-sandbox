package io.drozda.sandbox.scenario.keypartitioning.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.keypartitioning.app.KeyedEventTracker;
import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;

public class KeyedOrderEventListener {
    private final KeyedEventTracker tracker;

    public KeyedOrderEventListener(KeyedEventTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(topics = "${app.kafka.topics.key-partitioning}")
    public void onEvent(ConsumerRecord<String, KeyedOrderEvent> record) {
        tracker.received(record);
    }
}
