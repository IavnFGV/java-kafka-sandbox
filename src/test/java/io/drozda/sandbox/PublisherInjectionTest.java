package io.drozda.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.model.TradeEvent;
import io.drozda.sandbox.visualization.junit.VisualScenario;
import io.drozda.sandbox.visualization.junit.VisualScenarioExtension;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=publisher-injection-test-${random.uuid}")
@ExtendWith(VisualScenarioExtension.class)
class PublisherInjectionTest {

    private static final Logger log = LoggerFactory.getLogger(PublisherInjectionTest.class);

    @Autowired
    TradeEventPublisher tradeEventPublisher;

    @Autowired
    TradeEventListener tradeEventListener;

    @Test
    @VisualScenario("trade-flow")
    public void shouldInjectTradeEventPublisher() {
        assertNotNull(tradeEventPublisher);
    }

    @Test
    public void shouldSendEvent() throws InterruptedException {
        tradeEventListener.latch = new CountDownLatch(1);
        TradeEvent tradeEvent = new TradeEvent("eventId", "tradeId", "symbol", "type");
        tradeEventPublisher.publishTradeEvent(tradeEvent);

        boolean received = tradeEventListener.latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(received);
        assertEquals(tradeEvent, tradeEventListener.tradeEvent);
        log.info("Test observed received trade event: {}", tradeEventListener.tradeEvent);

    }

}
