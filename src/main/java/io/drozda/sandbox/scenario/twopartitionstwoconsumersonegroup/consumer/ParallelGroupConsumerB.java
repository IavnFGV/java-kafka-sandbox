package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.consumer;

import java.util.Collection;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupTracker;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent;

public class ParallelGroupConsumerB implements ConsumerSeekAware {
    public static final String LABEL = "Consumer B";
    private final ParallelGroupTracker tracker;

    public ParallelGroupConsumerB(ParallelGroupTracker tracker) { this.tracker = tracker; }

    @KafkaListener(id = "scenario-008-consumer-b", topics = "${app.kafka.topics.parallel-group}",
            groupId = "${app.kafka.groups.parallel-group}")
    public void onEvent(ConsumerRecord<String, ParallelGroupEvent> record) { tracker.received(record, LABEL); }

    @Override public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        tracker.assigned(LABEL, assignments.keySet());
    }

    @Override public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        tracker.revoked(LABEL, partitions);
    }
}
