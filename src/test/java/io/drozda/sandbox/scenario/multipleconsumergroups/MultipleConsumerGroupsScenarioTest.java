package io.drozda.sandbox.scenario.multipleconsumergroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=multiple-groups-host-test-${random.uuid}")
class MultipleConsumerGroupsScenarioTest {
    @Autowired MultipleConsumerGroupsEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldDeliverEveryRecordToEachConsumerGroup() {
        try {
            MultipleConsumerGroupsScenarioStatus result = environment.observe("multiple-groups-test");
            assertTrue(result.published());
            assertTrue(result.receivedByBothGroups());
            assertEquals(6, result.observations().size());
            assertEquals(3, result.observations().stream()
                    .filter(observation -> "audit-group".equals(observation.groupId())).count());
            assertEquals(3, result.observations().stream()
                    .filter(observation -> "notification-group".equals(observation.groupId())).count());
            assertEquals(3, result.observations().stream().collect(Collectors.groupingBy(
                    observation -> observation.sequence() + ":" + observation.partition() + ":" + observation.offset()))
                    .values().stream().filter(copies -> copies.size() == 2).count());

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "multiple-consumer-groups",
                    MultipleConsumerGroupsScenarioStarter.OBSERVE_INDEPENDENT_GROUPS,
                    "visual-multiple-groups-test");
            assertTrue(runtime.completed());
            assertEquals("READY", runtime.nodeStatuses().get("observer"));
        } finally {
            environment.stop();
        }
    }
}
