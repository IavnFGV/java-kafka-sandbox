package io.drozda.sandbox.scenario.earliestvslatest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.earliestvslatest.app.EarliestVsLatestScenarioStatus;
import io.drozda.sandbox.scenario.earliestvslatest.app.OffsetResetObservation;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=offset-reset-host-test-${random.uuid}")
class EarliestVsLatestScenarioTest {
    @Autowired EarliestVsLatestEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldApplyResetPolicyOnlyWhenNewGroupsHaveNoCommittedOffset() {
        try {
            EarliestVsLatestScenarioStatus result = environment.compare("offset-reset-test");
            assertTrue(result.published());
            assertTrue(result.verified());
            assertEquals(List.of(0L, 1L, 2L, 3L, 4L), offsets(result, "earliest"));
            assertEquals(List.of(3L, 4L), offsets(result, "latest"));

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "earliest-vs-latest", EarliestVsLatestScenarioStarter.COMPARE_RESET_POLICIES,
                    "visual-offset-reset-test");
            assertTrue(runtime.completed());
            assertEquals("READY", runtime.nodeStatuses().get("observer"));
        } finally {
            environment.stop();
        }
    }

    private List<Long> offsets(EarliestVsLatestScenarioStatus result, String group) {
        return result.observations().stream().filter(value -> group.equals(value.group()))
                .map(OffsetResetObservation::offset).sorted().toList();
    }
}
