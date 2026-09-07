package io.drozda.sandbox.scenario.messagekeypartitionselection.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.messagekeypartitionselection.model.KeyedOrderEvent;
import io.drozda.sandbox.scenario.messagekeypartitionselection.app.KeyStrategy;

public class KeyedOrderEventPublisher {
    private final KafkaTemplate<String, KeyedOrderEvent> kafkaTemplate;
    private final String topic;

    public KeyedOrderEventPublisher(KafkaTemplate<String, KeyedOrderEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, KeyedOrderEvent>> publish(
            KeyStrategy strategy,
            KeyedOrderEvent event
    ) {
        return switch (strategy) {
            case NO_KEY -> kafkaTemplate.send(topic, event);
            case EVENT_ID -> kafkaTemplate.send(topic, event.eventId(), event);
            case ORDER_ID -> kafkaTemplate.send(topic, event.orderId(), event);
        };
    }
}
