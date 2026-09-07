package io.drozda.sandbox.visualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.async.DeferredResult;

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
        assertTrue(afterReady.eventLog().stream().anyMatch(line -> line.contains("Component ready: Publisher Ready")));

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
        assertEquals("Session completed", completed.eventLog().get(completed.eventLog().size() - 1));
    }

    @Test
    void shouldClearActiveSessionAndResetToBaselineStep() {
        ScenarioGraph scenario = scenarioCatalog.systemReadyScenario();
        runtimeService.startActiveSession(scenario, "runtime-test");
        runtimeService.updateActiveStep(scenario, 3);

        ActiveScenarioRuntimeState cleared = runtimeService.clearActiveSession(scenario);

        assertEquals(0, runtimeService.getState(scenario).currentStepIndex());
        assertFalse(cleared.active());
        assertNull(cleared.scenarioId());
    }

    @Test
    void shouldCompleteLongPollWhenRuntimeChanges() {
        ScenarioGraph scenario = scenarioCatalog.systemReadyScenario();
        long currentRevision = runtimeService.currentRuntimeUpdate().revision();
        DeferredResult<ScenarioRuntimeUpdate> pending = runtimeService.awaitRuntimeUpdate(currentRevision, 5_000);

        assertFalse(pending.hasResult());

        runtimeService.startActiveSession(scenario, "long-poll-test");

        assertTrue(pending.hasResult());
        ScenarioRuntimeUpdate update = (ScenarioRuntimeUpdate) pending.getResult();
        assertEquals(currentRevision + 1, update.revision());
        assertEquals(1, update.events().size());
        assertEquals(currentRevision + 1, update.events().get(0).sequence());
        assertFalse(update.events().get(0).visibleInTimeline());
        assertFalse(update.events().get(0).animated());
        assertNull(update.events().get(0).before().scenarioId());
        assertEquals("system-ready", update.events().get(0).after().scenarioId());
        assertEquals("system-ready", update.runtime().scenarioId());
    }

    @Test
    void shouldClassifyAnimatedAndTechnicalTimelineEventsOnBackend() {
        ScenarioGraph scenario = scenarioCatalog.systemReadyScenario();
        runtimeService.startActiveSession(scenario, "classification-test");
        long cursor = runtimeService.currentRuntimeUpdate().revision();

        runtimeService.applyRuntimeEvent(scenario, new RuntimeEventRequest(
                scenario.id(), "signal-started", null, "publisher-kafka", "TradeEvent",
                "publisher", "kafka", null));
        runtimeService.applyRuntimeEvent(scenario, new RuntimeEventRequest(
                scenario.id(), "signal-delivered", null, "publisher-kafka", "TradeEvent",
                "publisher", "kafka", null));
        runtimeService.updateNodeDetail(scenario, "publisher", "sent");

        ScenarioRuntimeUpdate update = (ScenarioRuntimeUpdate) runtimeService
                .awaitRuntimeUpdate(cursor, 5_000).getResult();

        assertEquals(3, update.events().size());
        assertTrue(update.events().get(0).visibleInTimeline());
        assertTrue(update.events().get(0).animated());
        assertFalse(update.events().get(1).visibleInTimeline());
        assertFalse(update.events().get(1).animated());
        assertFalse(update.events().get(2).visibleInTimeline());
    }

    @Test
    void shouldKeepReadyAssignmentVisibleButNotAnimated() {
        ScenarioGraph scenario = scenarioCatalog.systemReadyScenario();
        runtimeService.startActiveSession(scenario, "assignment-test");
        long cursor = runtimeService.currentRuntimeUpdate().revision();

        runtimeService.applyRuntimeEvent(scenario, new RuntimeEventRequest(
                scenario.id(), "signal-started", null, null, "partition assigned",
                "kafka", "listener", "READY"));

        ScenarioRuntimeUpdate update = (ScenarioRuntimeUpdate) runtimeService
                .awaitRuntimeUpdate(cursor, 5_000).getResult();

        assertTrue(update.events().get(0).visibleInTimeline());
        assertFalse(update.events().get(0).animated());
    }

    @Test
    void shouldReturnEveryRuntimeTransitionAfterSequenceCursor() {
        ScenarioGraph scenario = scenarioCatalog.systemReadyScenario();
        long beforeRun = runtimeService.currentRuntimeUpdate().revision();

        runtimeService.startActiveSession(scenario, "timeline-test");
        runtimeService.updateActiveStep(scenario, 1);
        runtimeService.completeActiveSession(scenario);

        DeferredResult<ScenarioRuntimeUpdate> result = runtimeService.awaitRuntimeUpdate(beforeRun, 5_000);
        ScenarioRuntimeUpdate update = (ScenarioRuntimeUpdate) result.getResult();

        assertEquals(3, update.events().size());
        assertEquals(beforeRun + 1, update.events().get(0).sequence());
        assertEquals(beforeRun + 3, update.events().get(2).sequence());
        assertEquals("session-started", update.events().get(0).after().lastEventType());
        assertEquals("session-completed", update.events().get(2).after().lastEventType());
    }

    @Test
    void shouldExposeRuntimeDetailsForIndividualNodes() {
        ScenarioGraph scenario = scenarioCatalog.topicPartitionOffsetsScenario();
        runtimeService.startActiveSession(scenario, "node-detail-test");

        ActiveScenarioRuntimeState updated = runtimeService.updateNodeDetail(
                scenario,
                "partition-0",
                "A @ offset 12 | C @ offset 13"
        );

        assertEquals("A @ offset 12 | C @ offset 13", updated.nodeDetails().get("partition-0"));
    }
}
