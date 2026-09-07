package io.drozda.sandbox.mediator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

class ScenarioMediatorServiceTest {

    private final TestScenarioEnvironment environment = new TestScenarioEnvironment();
    private final TestScenarioStarter starter = new TestScenarioStarter();
    private final ScenarioMediatorService mediatorService =
            new ScenarioMediatorService(List.of(starter), List.of(environment));

    @Test
    void shouldExposeScenarioCommands() {
        assertEquals(1, mediatorService.commandsFor("system-ready").size());
        assertEquals("baseline-readiness", mediatorService.commandsFor("system-ready").get(0).id());
    }

    @Test
    void shouldStartEnvironmentBeforeExecutingCommand() {
        mediatorService.execute("system-ready", "baseline-readiness", "mediator-test");

        assertEquals(1, environment.startCalls);
        assertEquals("baseline-readiness", starter.lastCommandId);
        assertEquals("mediator-test", starter.lastInvocationName);
    }

    @Test
    void shouldRunDefaultCommandForScenario() {
        mediatorService.runDefault("system-ready", "default-run");

        assertEquals(1, environment.startCalls);
        assertEquals("baseline-readiness", starter.lastCommandId);
        assertEquals("default-run", starter.lastInvocationName);
    }

    @Test
    void shouldManageEnvironmentLifecycle() {
        assertEquals("STOPPED", mediatorService.environmentStatus("system-ready").lifecycleState());
        assertEquals("STARTED", mediatorService.startEnvironment("system-ready").lifecycleState());
        assertEquals("RESET", mediatorService.resetEnvironment("system-ready").lifecycleState());
        assertEquals("STOPPED", mediatorService.stopEnvironment("system-ready").lifecycleState());
    }

    @Test
    void shouldRejectUnknownScenario() {
        assertThrows(IllegalArgumentException.class, () -> mediatorService.commandsFor("missing-scenario"));
        assertThrows(IllegalArgumentException.class, () -> mediatorService.startEnvironment("missing-scenario"));
    }

    private static final class TestScenarioStarter implements ScenarioStarter {

        private String lastCommandId;
        private String lastInvocationName;

        @Override
        public String scenarioId() {
            return "system-ready";
        }

        @Override
        public List<ScenarioCommand> commands() {
            return List.of(new ScenarioCommand("baseline-readiness", "Run", "Run test command"));
        }

        @Override
        public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
            this.lastCommandId = commandId;
            this.lastInvocationName = invocationName;
            return new ActiveScenarioRuntimeState(
                    "system-ready",
                    0,
                    false,
                    true,
                    invocationName,
                    java.util.Map.of(),
                    java.util.Map.of(),
                    java.util.Map.of(),
                    java.util.List.of(),
                    java.util.List.of("done"),
                    "done",
                    invocationName
            );
        }
    }

    private static final class TestScenarioEnvironment implements ScenarioEnvironment {

        private int startCalls;
        private String state = "STOPPED";

        @Override
        public String scenarioId() {
            return "system-ready";
        }

        @Override
        public ScenarioEnvironmentStatus start() {
            startCalls += 1;
            state = "STARTED";
            return status();
        }

        @Override
        public ScenarioEnvironmentStatus stop() {
            state = "STOPPED";
            return status();
        }

        @Override
        public ScenarioEnvironmentStatus reset() {
            state = "RESET";
            return status();
        }

        @Override
        public ScenarioEnvironmentStatus status() {
            return new ScenarioEnvironmentStatus(
                    "system-ready",
                    state,
                    "test-environment",
                    "STARTED".equals(state) || "RESET".equals(state),
                    "STARTED".equals(state) || "RESET".equals(state),
                    "STARTED".equals(state) || "RESET".equals(state)
            );
        }
    }
}
