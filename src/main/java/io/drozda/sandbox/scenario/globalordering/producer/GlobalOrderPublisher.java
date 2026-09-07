package io.drozda.sandbox.scenario.globalordering.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.globalordering.model.GlobalOrderEvent;

public class GlobalOrderPublisher {
    private final KafkaTemplate<String, GlobalOrderEvent> kafkaTemplate;

    public GlobalOrderPublisher(KafkaTemplate<String, GlobalOrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, GlobalOrderEvent>> publish(
            String topic,
            int partition,
            GlobalOrderEvent event
    ) {
        return kafkaTemplate.send(topic, partition, event.orderId(), event);
    }
}
