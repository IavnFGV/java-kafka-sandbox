package io.drozda.sandbox.basic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.TradeEventListener;
import io.drozda.sandbox.TradeEventPublisher;
import io.drozda.sandbox.model.TradeEvent;
import io.drozda.sandbox.visualization.junit.VisualScenario;
import io.drozda.sandbox.visualization.junit.VisualScenarioExtension;
import io.drozda.sandbox.visualization.junit.VisualScenarioTestClient;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=all-components-test-${random.uuid}")
@ExtendWith(VisualScenarioExtension.class)
@VisualScenario("system-ready")
class AllComponentsTest {

    @Autowired
    TradeEventPublisher tradeEventPublisher;

    @Autowired
    TradeEventListener tradeEventListener;

    @Autowired
    KafkaTemplate<String, TradeEvent> kafkaTemplate;

    @Test
    void shouldLoadAllCoreComponents() throws InterruptedException {
        VisualScenarioTestClient.event("system-ready", "component-ready", "spring-app", null,
                "Spring Context Ready", null, null, "READY");
        Thread.sleep(400);
        assertNotNull(tradeEventPublisher);

        VisualScenarioTestClient.event("system-ready", "component-ready", "publisher", "publisher-kafka",
                "Publisher Ready", null, null, "READY");
        Thread.sleep(400);
        assertNotNull(tradeEventListener);

        VisualScenarioTestClient.event("system-ready", "component-ready", "listener", "listener-kafka",
                "Listener Ready", null, null, "READY");
        Thread.sleep(400);
        assertNotNull(kafkaTemplate);

        VisualScenarioTestClient.event("system-ready", "component-ready", "kafka", null,
                "Kafka Reachable", null, null, "READY");
        Thread.sleep(400);
    }
}
