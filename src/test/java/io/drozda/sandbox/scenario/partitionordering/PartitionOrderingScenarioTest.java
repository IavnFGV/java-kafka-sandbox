package io.drozda.sandbox.scenario.partitionordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.partitionordering.app.PartitionOrderingScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=partition-ordering-host-test-${random.uuid}")
class PartitionOrderingScenarioTest {
    @Autowired PartitionOrderingEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldPreserveSequenceWithinOnePartition() {
        try {
            PartitionOrderingScenarioStatus result = environment.verifyOrder("partition-order-test");

            assertTrue(result.published());
            assertTrue(result.received());
            assertTrue(result.onePartition());
            assertTrue(result.increasingOffsets());
            assertTrue(result.receiveOrderPreserved());
            assertEquals(6, result.observations().size());
            assertTrue(IntStream.range(0, result.observations().size())
                    .allMatch(index -> result.observations().get(index).sequence() == index));
            assertEquals(1, result.observations().stream()
                    .map(observation -> observation.consumerId()).distinct().count());
            assertEquals(3, result.consumerAssignments().size());

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "partition-ordering", PartitionOrderingScenarioStarter.VERIFY_ORDER,
                    "visual-partition-order-test");
            assertTrue(runtime.completed());
            assertEquals("READY", runtime.nodeStatuses().get("order-check"));
            assertTrue(runtime.nodeDetails().get("order-check").contains("callback order=true"));
            assertEquals(3, runtime.activeSignals().stream()
                    .filter(signal -> "READY".equals(signal.state())).count());
        } finally {
            environment.stop();
        }
    }
}
