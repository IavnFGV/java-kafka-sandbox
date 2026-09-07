package io.drozda.sandbox.scenario.earliestvslatest.consumer;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;

import io.drozda.sandbox.scenario.earliestvslatest.app.OffsetResetTracker;
import io.drozda.sandbox.scenario.earliestvslatest.model.OffsetResetEvent;

public class EarliestConsumer implements ConsumerSeekAware {
    public static final String ID = "scenario-010-earliest";
    private final OffsetResetTracker tracker;

    public EarliestConsumer(OffsetResetTracker tracker) { this.tracker = tracker; }

    @KafkaListener(id = ID, autoStartup = "false", topics = "${app.kafka.topics.offset-reset}",
            groupId = "${app.kafka.groups.earliest}",
            properties = ConsumerConfig.AUTO_OFFSET_RESET_CONFIG + ":earliest")
    public void onEvent(ConsumerRecord<String, OffsetResetEvent> record) { tracker.received(record, "earliest"); }

    @Override public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        tracker.assigned("earliest");
    }
}
