package io.drozda.sandbox;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import io.drozda.sandbox.model.TradeEvent;

@Service
public class TradeEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradeEventListener.class);

    public CountDownLatch latch = new CountDownLatch(1);
    public TradeEvent tradeEvent;
    

    @KafkaListener(topics = "${app.kafka.topics.trade-events}")
    public void onTradeEvent(TradeEvent tradeEvent) {
        this.tradeEvent = tradeEvent;
        log.info("Received trade event from Kafka: {}", this.tradeEvent);
        latch.countDown();
    }

}
