package io.drozda.sandbox.scenario.multipleconsumergroups.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsTracker;
import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;

public class NotificationGroupConsumer {
    public static final String GROUP = "notification-group";
    private final MultipleConsumerGroupsTracker tracker;

    public NotificationGroupConsumer(MultipleConsumerGroupsTracker tracker) { this.tracker = tracker; }

    @KafkaListener(id = "scenario-009-notification", topics = "${app.kafka.topics.multiple-groups}",
            groupId = "${app.kafka.groups.notification}")
    public void onEvent(ConsumerRecord<String, SharedOrderEvent> record) {
        tracker.received(record, GROUP);
    }
}
