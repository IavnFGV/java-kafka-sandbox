package io.drozda.sandbox.scenario.keypartitioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.keypartitioning.app.KeyPartitioningScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=key-partition-host-test-${random.uuid}")
class KeyPartitioningScenarioTest {
    @Autowired KeyPartitioningEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldRouteRecordsWithTheSameKeyToOnePartition() {
        try {
            KeyPartitioningScenarioStatus result = environment.routeByKey("key-routing-test");
            long order42Partitions = result.observations().stream()
                    .filter(record -> "order-42".equals(record.orderId()))
                    .map(record -> record.partition())
                    .distinct()
                    .count();

            assertTrue(result.published());
            assertTrue(result.received());
            assertEquals(3, result.observations().stream()
                    .filter(record -> "order-42".equals(record.orderId())).count());
            assertEquals(1, order42Partitions);

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "key-partitioning", KeyPartitioningScenarioStarter.ROUTE_BY_KEY, "visual-key-routing-test");
            assertEquals("READY", runtime.nodeStatuses().get("consumer"));
            assertTrue(runtime.nodeDetails().get("consumer").contains("order-42 stayed in partition"));
            assertTrue(runtime.completed());
        } finally {
            environment.stop();
        }
    }
}
