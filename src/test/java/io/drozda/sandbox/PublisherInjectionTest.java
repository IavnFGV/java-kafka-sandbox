package io.drozda.sandbox;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.model.TradeEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PublisherInjectionTest {

    private static final Logger log = LoggerFactory.getLogger(PublisherInjectionTest.class);

    @Autowired
    TradeEventPublisher tradeEventPublisher;

    @Autowired
    TradeEventListener tradeEventListener;

    @Test
    public void shouldInjectTradeEventPublisher() {
        assertNotNull(tradeEventPublisher);
    }

    @Test
    public void shouldSendEvent() throws InterruptedException {
        TradeEvent tradeEvent = new TradeEvent("eventId", "tradeId", "symbol", "type");
        tradeEventPublisher.publishTradeEvent(tradeEvent);

        boolean received = tradeEventListener.latch.await(1, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(received);
        assertEquals(tradeEvent, tradeEventListener.tradeEvent);
        log.info("Test observed received trade event: {}", tradeEventListener.tradeEvent);
        
    }

}
