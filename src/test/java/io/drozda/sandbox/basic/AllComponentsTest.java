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
        VisualScenarioTestClient.step("system-ready", 0);
        Thread.sleep(400);
        assertNotNull(tradeEventPublisher);

        VisualScenarioTestClient.step("system-ready", 1);
        Thread.sleep(400);
        assertNotNull(tradeEventListener);

        VisualScenarioTestClient.step("system-ready", 2);
        Thread.sleep(400);
        assertNotNull(kafkaTemplate);

        VisualScenarioTestClient.step("system-ready", 3);
        Thread.sleep(400);
    }
}
