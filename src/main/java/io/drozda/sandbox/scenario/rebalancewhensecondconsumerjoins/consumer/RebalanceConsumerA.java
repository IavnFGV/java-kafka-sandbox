package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.consumer;

import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app.RebalanceTracker;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent;

public class RebalanceConsumerA implements ConsumerSeekAware {
    public static final String ID = "scenario-011-consumer-a";
    public static final String LABEL = "Consumer A";
    private final RebalanceTracker tracker;
    public RebalanceConsumerA(RebalanceTracker tracker) { this.tracker = tracker; }
    @KafkaListener(id = ID, autoStartup = "false", topics = "${app.kafka.topics.rebalance-join}", groupId = "${app.kafka.groups.rebalance-join}")
    public void onEvent(ConsumerRecord<String, RebalanceEvent> record) { tracker.received(record, LABEL); }
    @Override public void onPartitionsAssigned(Map<TopicPartition, Long> values, ConsumerSeekCallback callback) { tracker.assigned(LABEL, values.keySet()); }
    @Override public void onPartitionsRevoked(Collection<TopicPartition> values) { tracker.revoked(LABEL, values); }
}
