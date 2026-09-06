package io.drozda.sandbox.scenario.keypartitioning.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;

public class KeyedOrderEventPublisher {
    private final KafkaTemplate<String, KeyedOrderEvent> kafkaTemplate;
    private final String topic;

    public KeyedOrderEventPublisher(KafkaTemplate<String, KeyedOrderEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, KeyedOrderEvent>> publish(KeyedOrderEvent event) {
        return kafkaTemplate.send(topic, event.orderId(), event);
    }
}
