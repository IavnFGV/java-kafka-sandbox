package io.drozda.sandbox.scenario.multipleconsumergroups.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsTracker;
import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;

public class AuditGroupConsumer {
    public static final String GROUP = "audit-group";
    private final MultipleConsumerGroupsTracker tracker;

    public AuditGroupConsumer(MultipleConsumerGroupsTracker tracker) { this.tracker = tracker; }

    @KafkaListener(id = "scenario-009-audit", topics = "${app.kafka.topics.multiple-groups}",
            groupId = "${app.kafka.groups.audit}")
    public void onEvent(ConsumerRecord<String, SharedOrderEvent> record) {
        tracker.received(record, GROUP);
    }
}
