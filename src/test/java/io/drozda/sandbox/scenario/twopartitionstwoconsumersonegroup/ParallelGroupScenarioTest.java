package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=parallel-group-host-test-${random.uuid}")
class ParallelGroupScenarioTest {
    @Autowired ParallelGroupEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldAssignTwoPartitionsToTwoDifferentConsumers() {
        try {
            ParallelGroupScenarioStatus result = environment.observe("parallel-group-test");
            assertTrue(result.published());
            assertTrue(result.received());
            assertEquals(2, result.partitionOwners().size());
            assertNotEquals(result.partitionOwners().get(0), result.partitionOwners().get(1));
            assertEquals(6, result.observations().size());
            assertTrue(result.observations().stream().allMatch(observation ->
                    observation.consumerId().equals(result.partitionOwners().get(observation.partition()))));
            assertTrue(result.observations().stream().filter(o -> o.partition() == 0).count() == 3);
            assertTrue(result.observations().stream().filter(o -> o.partition() == 1).count() == 3);

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "consumer-group-two-partitions", ParallelGroupScenarioStarter.OBSERVE_PARALLEL_ASSIGNMENT,
                    "visual-parallel-group-test");
            assertTrue(runtime.completed());
            assertEquals("READY", runtime.nodeStatuses().get("observer"));
        } finally {
            environment.stop();
        }
    }
}
