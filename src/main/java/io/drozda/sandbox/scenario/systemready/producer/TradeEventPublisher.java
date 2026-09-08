package io.drozda.sandbox.scenario.systemready.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.systemready.model.TradeEvent;

public class TradeEventPublisher {
    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final String topic;

    public TradeEventPublisher(KafkaTemplate<String, TradeEvent> kafkaTemplate,
            @Value("${app.kafka.topics.system-ready}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishTradeEvent(TradeEvent tradeEvent) {
        kafkaTemplate.send(topic, tradeEvent.tradeId(), tradeEvent);
    }
}
