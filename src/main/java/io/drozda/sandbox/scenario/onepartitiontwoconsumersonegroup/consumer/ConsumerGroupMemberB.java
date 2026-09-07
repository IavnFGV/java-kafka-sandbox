package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.consumer;

import java.util.Collection;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app.SinglePartitionGroupTracker;
import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.model.GroupWorkEvent;

public class ConsumerGroupMemberB implements ConsumerSeekAware {
    public static final String ID = "scenario-007-consumer-b";
    public static final String LABEL = "Consumer B";

    private final SinglePartitionGroupTracker tracker;

    public ConsumerGroupMemberB(SinglePartitionGroupTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(id = ID, topics = "${app.kafka.topics.single-partition-group}",
            groupId = "${app.kafka.groups.single-partition-group}")
    public void onEvent(ConsumerRecord<String, GroupWorkEvent> record) {
        tracker.received(record, LABEL);
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        tracker.assigned(LABEL, assignments.keySet());
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        tracker.revoked(LABEL);
    }
}
