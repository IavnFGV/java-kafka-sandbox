package io.drozda.sandbox.visualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScenarioRuntimeServiceTest {

    private final ScenarioCatalog scenarioCatalog = new ScenarioCatalog();
    private final ScenarioRuntimeService runtimeService = new ScenarioRuntimeService();

    @Test
    void shouldTrackIndependentRuntimeEvents() {
        ScenarioGraph scenario = scenarioCatalog.systemReadyScenario();
        runtimeService.startActiveSession(scenario, "runtime-test");

        ActiveScenarioRuntimeState afterReady = runtimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(
                        scenario.id(),
                        "component-ready",
                        "publisher",
                        "publisher-kafka",
                        "Publisher Ready",
                        null,
                        null,
                        null
                )
        );

        assertEquals("READY", afterReady.nodeStatuses().get("publisher"));
        assertEquals("READY", afterReady.edgeStatuses().get("publisher-kafka"));

        ActiveScenarioRuntimeState afterSignalStart = runtimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(
                        scenario.id(),
                        "signal-started",
                        null,
                        "publisher-kafka",
                        "TradeEvent",
                        "publisher",
                        "kafka",
                        null
                )
        );

        assertEquals("ACTIVE", afterSignalStart.edgeStatuses().get("publisher-kafka"));
        assertEquals(1, afterSignalStart.activeSignals().size());

        ActiveScenarioRuntimeState afterFailure = runtimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(
                        scenario.id(),
                        "component-failed",
                        "kafka",
                        null,
                        "Kafka Down",
                        null,
                        null,
                        null
                )
        );

        assertEquals("FAILED", afterFailure.nodeStatuses().get("kafka"));

        ActiveScenarioRuntimeState afterSignalFinish = runtimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(
                        scenario.id(),
                        "signal-finished",
                        null,
                        "publisher-kafka",
                        "TradeEvent",
                        "publisher",
                        "kafka",
                        null
                )
        );

        assertEquals("READY", afterSignalFinish.edgeStatuses().get("publisher-kafka"));
        assertTrue(afterSignalFinish.activeSignals().isEmpty());

        ActiveScenarioRuntimeState completed = runtimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(
                        scenario.id(),
                        "session-completed",
                        null,
                        null,
                        "Done",
                        null,
                        null,
                        null
                )
        );

        assertTrue(completed.completed());
        assertFalse(completed.active());
    }
}
