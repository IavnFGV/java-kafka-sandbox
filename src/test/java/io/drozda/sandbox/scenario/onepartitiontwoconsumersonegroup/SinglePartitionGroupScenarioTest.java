package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app.SinglePartitionGroupScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=single-partition-group-host-test-${random.uuid}")
class SinglePartitionGroupScenarioTest {
    @Autowired SinglePartitionGroupEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldKeepOneConsumerIdleThenReassignPartitionAfterOwnerStops() {
        try {
            SinglePartitionGroupScenarioStatus result = environment.observeTakeover("single-partition-test");

            assertTrue(result.published());
            assertTrue(result.received());
            assertNotEquals(result.initialOwner(), result.initialIdleConsumer());
            assertEquals(result.initialIdleConsumer(), result.takeoverOwner());
            assertEquals(6, result.observations().size());
            assertTrue(result.observations().stream()
                    .filter(observation -> observation.phase().equals("BEFORE FAILURE"))
                    .allMatch(observation -> observation.consumerId().equals(result.initialOwner())));
            assertTrue(result.observations().stream()
                    .filter(observation -> observation.phase().equals("AFTER TAKEOVER"))
                    .allMatch(observation -> observation.consumerId().equals(result.takeoverOwner())));
            assertTrue(result.observations().stream().allMatch(observation -> observation.partition() == 0));

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "consumer-group-single-partition",
                    SinglePartitionGroupScenarioStarter.OBSERVE_TAKEOVER,
                    "visual-single-partition-test"
            );
            assertTrue(runtime.completed());
            assertEquals("READY", runtime.nodeStatuses().get("observer"));
            assertTrue(runtime.nodeDetails().get("observer").contains("takeover="));
        } finally {
            environment.stop();
        }
    }
}
