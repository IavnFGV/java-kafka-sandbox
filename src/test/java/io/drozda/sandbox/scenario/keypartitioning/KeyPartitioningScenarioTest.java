package io.drozda.sandbox.scenario.keypartitioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.keypartitioning.app.KeyPartitioningScenarioStatus;
import io.drozda.sandbox.scenario.keypartitioning.app.KeyStrategy;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=key-partition-host-test-${random.uuid}")
class KeyPartitioningScenarioTest {
    @Autowired KeyPartitioningEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldRouteRecordsWithTheSameKeyToOnePartition() {
        try {
            KeyPartitioningScenarioStatus result = environment.runExperiment("key-routing-test", KeyStrategy.ORDER_ID);
            long order42Partitions = result.observations().stream()
                    .filter(record -> "order-42".equals(record.orderId()))
                    .map(record -> record.partition())
                    .distinct()
                    .count();

            assertTrue(result.published());
            assertTrue(result.received());
            assertTrue(result.learningGoalMet());
            assertEquals(10, result.observations().stream()
                    .filter(record -> "order-42".equals(record.orderId())).count());
            assertEquals(1, order42Partitions);
            assertEquals(3, result.consumerAssignments().size());
            assertTrue(result.consumerAssignments().values().stream()
                    .allMatch(partitions -> partitions.size() == 1));

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "key-partitioning", KeyPartitioningScenarioStarter.ROUTE_BY_KEY,
                    "visual-key-routing-test", java.util.Map.of("keyStrategy", "ORDER_ID"));
            assertEquals("READY", runtime.nodeStatuses().get("consumer-a"));
            assertTrue(runtime.nodeDetails().get("consumer-a").contains("assigned partitions"));
            assertEquals(3, runtime.activeSignals().stream()
                    .filter(signal -> "READY".equals(signal.state()))
                    .count());
            assertTrue(runtime.nodeDetails().values().stream()
                    .filter(detail -> detail.contains("records | offsets"))
                    .allMatch(detail -> detail.length() < 80));
            assertTrue(runtime.eventLog().stream()
                    .anyMatch(line -> line.contains("consumed order-42 SHIPPED")));
            assertTrue(runtime.completed());
        } finally {
            environment.stop();
        }
    }

    @Test
    void shouldKeepTechnicallySuccessfulNoKeyRunInLearningState() {
        try {
            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "key-partitioning", KeyPartitioningScenarioStarter.ROUTE_BY_KEY,
                    "no-key-test", java.util.Map.of("keyStrategy", "NO_KEY"));

            assertEquals("WAITING", runtime.nodeStatuses().get("consumer-a"));
            assertTrue(runtime.nodeDetails().get("consumer-a").contains("assigned partitions"));
            assertTrue(runtime.completed());
        } finally {
            environment.stop();
        }
    }
}
