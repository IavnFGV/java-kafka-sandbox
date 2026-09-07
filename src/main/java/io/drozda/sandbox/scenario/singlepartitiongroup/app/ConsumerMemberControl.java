package io.drozda.sandbox.scenario.singlepartitiongroup.app;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import io.drozda.sandbox.scenario.singlepartitiongroup.consumer.ConsumerGroupMemberA;
import io.drozda.sandbox.scenario.singlepartitiongroup.consumer.ConsumerGroupMemberB;

public class ConsumerMemberControl {
    private final KafkaListenerEndpointRegistry registry;

    public ConsumerMemberControl(KafkaListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    public void startBoth() {
        container(ConsumerGroupMemberA.ID).start();
        container(ConsumerGroupMemberB.ID).start();
    }

    public void stop(String consumerId) {
        container(ConsumerGroupMemberA.LABEL.equals(consumerId)
                ? ConsumerGroupMemberA.ID : ConsumerGroupMemberB.ID).stop();
    }

    private MessageListenerContainer container(String id) {
        MessageListenerContainer container = registry.getListenerContainer(id);
        if (container == null) throw new IllegalStateException("Listener container not found: " + id);
        return container;
    }
}
