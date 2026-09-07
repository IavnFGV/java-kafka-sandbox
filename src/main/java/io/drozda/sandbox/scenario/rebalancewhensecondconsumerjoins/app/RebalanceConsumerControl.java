package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.consumer.RebalanceConsumerA;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.consumer.RebalanceConsumerB;

public class RebalanceConsumerControl {
    private final KafkaListenerEndpointRegistry registry;
    public RebalanceConsumerControl(KafkaListenerEndpointRegistry registry) { this.registry = registry; }
    public void startA() { container(RebalanceConsumerA.ID).start(); }
    public void startB() { container(RebalanceConsumerB.ID).start(); }
    private MessageListenerContainer container(String id) {
        MessageListenerContainer value = registry.getListenerContainer(id);
        if (value == null) throw new IllegalStateException("Listener container not found: " + id);
        return value;
    }
}
