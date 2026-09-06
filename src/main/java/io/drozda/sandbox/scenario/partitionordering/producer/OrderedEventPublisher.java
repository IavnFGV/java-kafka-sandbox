package io.drozda.sandbox.scenario.partitionordering.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.partitionordering.model.OrderedOrderEvent;

public class OrderedEventPublisher {
    private final KafkaTemplate<String, OrderedOrderEvent> kafkaTemplate;
    private final String topic;

    public OrderedEventPublisher(KafkaTemplate<String, OrderedOrderEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, OrderedOrderEvent>> publish(OrderedOrderEvent event) {
        return kafkaTemplate.send(topic, event.orderId(), event);
    }
}
