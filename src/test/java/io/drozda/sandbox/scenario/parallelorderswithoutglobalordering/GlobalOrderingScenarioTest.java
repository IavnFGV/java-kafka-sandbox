package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app.GlobalOrderingMode;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app.GlobalOrderingScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=global-ordering-host-test-${random.uuid}")
class GlobalOrderingScenarioTest {
    @Autowired GlobalOrderingEnvironment environment;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldShowParallelOrderedShardsRemovingHeadOfLineBlocking() {
        try {
            GlobalOrderingScenarioStatus single = environment.compare("single-ordering-test", GlobalOrderingMode.SINGLE);
            GlobalOrderingScenarioStatus parallel = environment.compare("parallel-ordering-test", GlobalOrderingMode.PARALLEL);

            assertTrue(single.published());
            assertTrue(single.received());
            assertTrue(single.perOrderSequencePreserved());
            assertTrue(single.globalCompletionMatchesPublishOrder());
            assertEquals(1, single.observations().stream()
                    .map(observation -> observation.partition()).distinct().count());
            assertEquals(1, single.observations().stream()
                    .map(observation -> observation.consumerId()).distinct().count());

            assertTrue(parallel.published());
            assertTrue(parallel.received());
            assertTrue(parallel.perOrderSequencePreserved());
            assertFalse(parallel.globalCompletionMatchesPublishOrder());
            assertEquals(2, parallel.observations().stream()
                    .map(observation -> observation.partition()).distinct().count());
            assertEquals(2, parallel.observations().stream()
                    .map(observation -> observation.consumerId()).distinct().count());
            assertTrue(parallel.fastOrderCompletedMs() < parallel.slowOrderCompletedMs());
            assertTrue(parallel.fastOrderCompletedMs() < single.fastOrderCompletedMs());

            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "global-ordering",
                    GlobalOrderingScenarioStarter.COMPARE_TOPOLOGY,
                    "visual-global-ordering-test",
                    Map.of("topology", "PARALLEL")
            );
            assertTrue(runtime.completed());
            assertEquals("READY", runtime.nodeStatuses().get("result"));
            assertTrue(runtime.nodeDetails().get("result").contains("per-order sequence=true"));
        } finally {
            environment.stop();
        }
    }
}
