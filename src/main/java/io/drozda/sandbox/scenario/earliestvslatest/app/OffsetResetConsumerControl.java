package io.drozda.sandbox.scenario.earliestvslatest.app;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import io.drozda.sandbox.scenario.earliestvslatest.consumer.EarliestConsumer;
import io.drozda.sandbox.scenario.earliestvslatest.consumer.LatestConsumer;

public class OffsetResetConsumerControl {
    private final KafkaListenerEndpointRegistry registry;

    public OffsetResetConsumerControl(KafkaListenerEndpointRegistry registry) { this.registry = registry; }

    public void startBoth() {
        container(EarliestConsumer.ID).start();
        container(LatestConsumer.ID).start();
    }

    private MessageListenerContainer container(String id) {
        MessageListenerContainer container = registry.getListenerContainer(id);
        if (container == null) throw new IllegalStateException("Listener container not found: " + id);
        return container;
    }
}
