package io.drozda.sandbox.mediator;

import java.util.List;

import org.springframework.stereotype.Service;

import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@Service
public class ScenarioMediatorService {

    private final List<ScenarioStarter> scenarioStarters;

    public ScenarioMediatorService(List<ScenarioStarter> scenarioStarters) {
        this.scenarioStarters = scenarioStarters;
    }

    public List<ScenarioCommand> commandsFor(String scenarioId) {
        return starterFor(scenarioId).commands();
    }

    public ActiveScenarioRuntimeState execute(String scenarioId, String commandId, String invocationName) {
        return starterFor(scenarioId).execute(commandId, invocationName);
    }

    private ScenarioStarter starterFor(String scenarioId) {
        return scenarioStarters.stream()
                .filter(starter -> starter.scenarioId().equals(scenarioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No scenario starter registered for scenario: " + scenarioId));
    }
}
