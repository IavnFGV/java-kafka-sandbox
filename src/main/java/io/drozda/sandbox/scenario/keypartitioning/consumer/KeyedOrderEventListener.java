package io.drozda.sandbox.scenario.keypartitioning.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.keypartitioning.app.KeyedEventTracker;
import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;

public class KeyedOrderEventListener implements ConsumerSeekAware {
    private final KeyedEventTracker tracker;

    public KeyedOrderEventListener(KeyedEventTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(topics = "${app.kafka.topics.key-partitioning}", concurrency = "3")
    public void onEvent(ConsumerRecord<String, KeyedOrderEvent> record) {
        tracker.received(record);
    }

    @Override
    public void onPartitionsAssigned(
            java.util.Map<org.apache.kafka.common.TopicPartition, Long> assignments,
            ConsumerSeekCallback callback
    ) {
        tracker.assigned(Thread.currentThread().getName(), assignments.keySet());
    }
}
