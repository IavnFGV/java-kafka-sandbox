package io.drozda.sandbox.scenario.tradeflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=trade-flow-host-test-${random.uuid}")
class TradeFlowScenarioTest {

    @Autowired
    ScenarioMediatorService mediator;

    @Test
    void shouldPublishAndReceiveMatchingEventThroughKafka() {
        try {
            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "trade-flow",
                    TradeFlowScenarioStarter.SEND_AND_RECEIVE,
                    "TradeFlowScenarioTest"
            );

            assertEquals("READY", runtime.nodeStatuses().get("publisher"));
            assertEquals("READY", runtime.nodeStatuses().get("kafka-broker"));
            assertEquals("READY", runtime.nodeStatuses().get("topic"));
            assertEquals("READY", runtime.nodeStatuses().get("listener"));
            assertEquals("READY", runtime.edgeStatuses().get("publish"));
            assertEquals("READY", runtime.edgeStatuses().get("consume"));
            assertTrue(runtime.eventLog().stream().anyMatch(line -> line.contains("partition")));
            assertTrue(runtime.completed());
            assertFalse(runtime.active());
        } finally {
            mediator.stopEnvironment("trade-flow");
        }
    }
}
