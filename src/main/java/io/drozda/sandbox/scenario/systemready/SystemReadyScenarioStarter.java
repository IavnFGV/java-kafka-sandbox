package io.drozda.sandbox.scenario.systemready;

import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;
import io.drozda.sandbox.scenario.systemready.app.SystemReadyScenarioStatus;

@Component
public class SystemReadyScenarioStarter implements ScenarioStarter {

    public static final String BASELINE_READINESS = "baseline-readiness";

    private final ScenarioCatalog scenarioCatalog;
    private final ScenarioRuntimeService scenarioRuntimeService;
    private final SystemReadyEnvironment systemReadyEnvironment;

    public SystemReadyScenarioStarter(
            ScenarioCatalog scenarioCatalog,
            ScenarioRuntimeService scenarioRuntimeService,
            SystemReadyEnvironment systemReadyEnvironment
    ) {
        this.scenarioCatalog = scenarioCatalog;
        this.scenarioRuntimeService = scenarioRuntimeService;
        this.systemReadyEnvironment = systemReadyEnvironment;
    }

    @Override
    public String scenarioId() {
        return "system-ready";
    }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(
                BASELINE_READINESS,
                "Run Baseline Readiness",
                "Confirm that the Spring app, publisher, listener, and Kafka-facing wiring are ready."
        ));
    }

    @Override
    public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!BASELINE_READINESS.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command for system-ready: " + commandId);
        }

        ScenarioGraph scenario = scenarioCatalog.scenarioById(scenarioId());
        String effectiveInvocationName = invocationName == null || invocationName.isBlank()
                ? scenario.title() + " Command"
                : invocationName.trim();
        SystemReadyScenarioStatus status = systemReadyEnvironment.baselineReadiness(effectiveInvocationName);

        scenarioRuntimeService.startActiveSession(scenario, effectiveInvocationName);
        scenarioRuntimeService.updateActiveStep(scenario, 1);
        event(scenario, "component-ready", "spring-app", null, "Spring Context Ready", "READY");

        if (status.publisherReady()) {
            scenarioRuntimeService.updateActiveStep(scenario, 2);
            event(scenario, "component-ready", "publisher", "publisher-kafka", "Publisher Ready", "READY");
        }

        if (status.listenerReady()) {
            scenarioRuntimeService.updateActiveStep(scenario, 3);
            event(scenario, "component-ready", "listener", "listener-kafka", "Listener Ready", "READY");
        }

        if (status.kafkaTemplateReady()) {
            scenarioRuntimeService.updateActiveStep(scenario, 4);
            event(scenario, "component-ready", "kafka", null, "Kafka Reachable", "READY");
        }

        return scenarioRuntimeService.completeActiveSession(scenario);
    }

    private void event(
            ScenarioGraph scenario,
            String type,
            String nodeId,
            String edgeId,
            String label,
            String status
    ) {
        scenarioRuntimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(
                        scenario.id(),
                        type,
                        nodeId,
                        edgeId,
                        label,
                        null,
                        null,
                        status
                )
        );
    }
}
