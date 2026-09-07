package io.drozda.sandbox.scenario.systemready;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import io.drozda.sandbox.TradeEventListener;
import io.drozda.sandbox.TradeEventPublisher;
import io.drozda.sandbox.model.TradeEvent;

@Component
public class SystemReadyProbe {

    private final TradeEventPublisher tradeEventPublisher;
    private final TradeEventListener tradeEventListener;
    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;

    public SystemReadyProbe(
            TradeEventPublisher tradeEventPublisher,
            TradeEventListener tradeEventListener,
            KafkaTemplate<String, TradeEvent> kafkaTemplate
    ) {
        this.tradeEventPublisher = tradeEventPublisher;
        this.tradeEventListener = tradeEventListener;
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean publisherReady() {
        return tradeEventPublisher != null;
    }

    public boolean listenerReady() {
        return tradeEventListener != null;
    }

    public boolean kafkaTemplateReady() {
        return kafkaTemplate != null;
    }
}
