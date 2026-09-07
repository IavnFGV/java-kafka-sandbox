package io.drozda.sandbox.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.TradeEventListener;
import io.drozda.sandbox.TradeEventPublisher;
import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.model.TradeEvent;
import io.drozda.sandbox.scenario.systemready.SystemReadyScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=all-components-test-${random.uuid}")
class AllComponentsTest {

    @Autowired
    TradeEventPublisher tradeEventPublisher;

    @Autowired
    TradeEventListener tradeEventListener;

    @Autowired
    KafkaTemplate<String, TradeEvent> kafkaTemplate;

    @Autowired
    ScenarioMediatorService scenarioMediatorService;

    @Test
    void shouldLoadAllCoreComponentsAndRunBaselineReadinessScenario() {
        assertNotNull(tradeEventPublisher);
        assertNotNull(tradeEventListener);
        assertNotNull(kafkaTemplate);

        ScenarioEnvironmentStatus environmentStatus = scenarioMediatorService.startEnvironment("system-ready");
        assertTrue(environmentStatus.publisherReady());
        assertTrue(environmentStatus.listenerReady());
        assertTrue(environmentStatus.kafkaTemplateReady());

        ActiveScenarioRuntimeState runtime = scenarioMediatorService.execute(
                "system-ready",
                SystemReadyScenarioStarter.BASELINE_READINESS,
                "AllComponentsTest"
        );

        assertEquals("READY", runtime.nodeStatuses().get("spring-app"));
        assertEquals("READY", runtime.nodeStatuses().get("publisher"));
        assertEquals("READY", runtime.nodeStatuses().get("listener"));
        assertFalse(runtime.nodeStatuses().containsKey("kafka"));
        assertFalse(runtime.edgeStatuses().containsKey("publisher-kafka"));
        assertFalse(runtime.edgeStatuses().containsKey("listener-kafka"));
        assertTrue(runtime.completed());
        assertFalse(runtime.active());
    }
}
