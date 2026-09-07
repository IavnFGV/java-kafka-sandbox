package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.consumer;

import java.util.Collection;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app.GlobalOrderTracker;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.model.GlobalOrderEvent;

public class GlobalOrderListener implements ConsumerSeekAware {
    private final GlobalOrderTracker tracker;

    public GlobalOrderListener(GlobalOrderTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.global-ordering-single}",
            groupId = "${app.kafka.groups.global-ordering-single}",
            concurrency = "1"
    )
    public void onSingle(ConsumerRecord<String, GlobalOrderEvent> record) {
        tracker.process(record, "Single Consumer");
    }

    @KafkaListener(
            topics = "${app.kafka.topics.global-ordering-parallel}",
            groupId = "${app.kafka.groups.global-ordering-parallel}",
            concurrency = "2"
    )
    public void onParallel(ConsumerRecord<String, GlobalOrderEvent> record) {
        String consumerId = record.partition() == 0 ? "Fast Consumer" : "Slow Consumer";
        tracker.process(record, consumerId);
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
