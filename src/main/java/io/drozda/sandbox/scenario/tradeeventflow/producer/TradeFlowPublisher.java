package io.drozda.sandbox.scenario.tradeeventflow.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.tradeeventflow.model.TradeFlowEvent;

public class TradeFlowPublisher {

    private final KafkaTemplate<String, TradeFlowEvent> kafkaTemplate;
    private final String topic;

    public TradeFlowPublisher(KafkaTemplate<String, TradeFlowEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, TradeFlowEvent>> publish(TradeFlowEvent event) {
        return kafkaTemplate.send(topic, event.tradeId(), event);
    }
}
