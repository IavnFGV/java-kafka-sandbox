package io.drozda.sandbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.model.TradeEvent;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class PublisherInjectionTest {

    @Autowired
    TradeEventPublisher tradeEventPublisher;

    @Test
    public void shouldInjectTradeEventPublisher() {
        assertNotNull(tradeEventPublisher);
    }

    @Test
    public void shouldSendEvent() {
        TradeEvent tradeEvent = new TradeEvent("eventId", "tradeId", "symbol", "type");
        tradeEventPublisher.publishTradeEvent(tradeEvent);
    }

}
