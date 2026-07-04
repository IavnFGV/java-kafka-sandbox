package io.drozda.sandbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import io.drozda.sandbox.model.TradeEvent;

@Service
public class TradeEventPublisher {

    @Autowired
    KafkaTemplate<String, TradeEvent> kafkaTemplate;

    @Value("${app.kafka.topics.trade-events}")
    private String topic;

    public void publishTradeEvent(TradeEvent tradeEvent) {
        kafkaTemplate.send(topic, tradeEvent.tradeId(), tradeEvent);
    }

}
