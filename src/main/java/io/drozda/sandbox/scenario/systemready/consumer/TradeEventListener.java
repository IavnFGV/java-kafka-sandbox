package io.drozda.sandbox.scenario.systemready.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.systemready.model.TradeEvent;

public class TradeEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradeEventListener.class);

    @KafkaListener(topics = "${app.kafka.topics.system-ready}", autoStartup = "false")
    public void onTradeEvent(TradeEvent tradeEvent) {
        log.info("Received trade event from Kafka: {}", tradeEvent);
    }
}
