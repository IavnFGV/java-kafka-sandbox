package io.drozda.sandbox.mediator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import io.drozda.sandbox.scenario.systemready.SystemReadyProbe;
import io.drozda.sandbox.scenario.systemready.SystemReadyScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

class ScenarioMediatorServiceTest {

    private final ScenarioCatalog scenarioCatalog = new ScenarioCatalog();
    private final ScenarioRuntimeService runtimeService = new ScenarioRuntimeService();
    private final SystemReadyProbe probe = new SystemReadyProbe(
            mock(io.drozda.sandbox.TradeEventPublisher.class),
            mock(io.drozda.sandbox.TradeEventListener.class),
            mock(org.springframework.kafka.core.KafkaTemplate.class)
    );
    private final SystemReadyScenarioStarter systemReadyStarter =
            new SystemReadyScenarioStarter(scenarioCatalog, runtimeService, probe);
    private final ScenarioMediatorService mediatorService = new ScenarioMediatorService(java.util.List.of(systemReadyStarter));

    @Test
    void shouldExposeBaselineCommandForSystemReadyScenario() {
        assertEquals(1, mediatorService.commandsFor("system-ready").size());
        assertEquals(SystemReadyScenarioStarter.BASELINE_READINESS,
                mediatorService.commandsFor("system-ready").get(0).id());
    }

    @Test
    void shouldExecuteSystemReadyBaselineCommand() {
        ActiveScenarioRuntimeState runtime = mediatorService.execute(
                "system-ready",
                SystemReadyScenarioStarter.BASELINE_READINESS,
                "mediator-test"
        );

        assertEquals("READY", runtime.nodeStatuses().get("spring-app"));
        assertEquals("READY", runtime.nodeStatuses().get("publisher"));
        assertEquals("READY", runtime.nodeStatuses().get("listener"));
        assertEquals("READY", runtime.nodeStatuses().get("kafka"));
        assertEquals("READY", runtime.edgeStatuses().get("publisher-kafka"));
        assertEquals("READY", runtime.edgeStatuses().get("listener-kafka"));
        assertEquals(4, runtime.currentStepIndex());
        assertTrue(runtime.completed());
        assertFalse(runtime.active());
    }

    @Test
    void shouldRejectUnknownCommandForSystemReadyScenario() {
        assertThrows(IllegalArgumentException.class, () ->
                mediatorService.execute("system-ready", "unknown-command", "mediator-test"));
    }
}
