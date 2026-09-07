package io.drozda.sandbox.scenario.orderingwithinonepartition.consumer;

import java.util.Collection;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.orderingwithinonepartition.app.OrderedEventTracker;
import io.drozda.sandbox.scenario.orderingwithinonepartition.model.OrderedOrderEvent;

public class OrderedEventListener implements ConsumerSeekAware {
    private final OrderedEventTracker tracker;

    public OrderedEventListener(OrderedEventTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(topics = "${app.kafka.topics.partition-ordering}", concurrency = "3")
    public void onEvent(ConsumerRecord<String, OrderedOrderEvent> record) {
        tracker.received(record);
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        tracker.assigned(Thread.currentThread().getName(), assignments.keySet());
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        tracker.revoked(Thread.currentThread().getName(), partitions);
    }
}
