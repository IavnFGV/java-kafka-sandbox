package io.drozda.sandbox.scenario.singlepartitiongroup.consumer;

import java.util.Collection;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.singlepartitiongroup.app.SinglePartitionGroupTracker;
import io.drozda.sandbox.scenario.singlepartitiongroup.model.GroupWorkEvent;

public class ConsumerGroupMemberA implements ConsumerSeekAware {
    public static final String ID = "scenario-007-consumer-a";
    public static final String LABEL = "Consumer A";

    private final SinglePartitionGroupTracker tracker;

    public ConsumerGroupMemberA(SinglePartitionGroupTracker tracker) {
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
