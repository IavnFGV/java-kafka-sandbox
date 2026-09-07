package io.drozda.sandbox.visualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ScenarioCatalogTest {

    private final ScenarioCatalog catalog = new ScenarioCatalog();

    @Test
    void shouldExplainPurposeAndBacklogCoverageForLearningScenarios() {
        List<ScenarioGraph> scenarios = catalog.scenarios();

        scenarios.forEach(scenario -> {
            assertFalse(scenario.practicalPurpose().isBlank(), scenario.id() + " must explain why it matters");
            assertTrue(
                    scenario.backlogItems().stream().allMatch(item -> item >= 1 && item <= 100),
                    scenario.id() + " contains an invalid backlog item"
            );
            if (scenario.order() > 1) {
                assertFalse(scenario.backlogItems().isEmpty(), scenario.id() + " must reference the Kafka backlog");
            }
        });

        assertEquals(
                scenarios.size(),
                scenarios.stream().map(ScenarioGraph::order).distinct().count(),
                "Scenario order values must be unique"
        );
    }
}
