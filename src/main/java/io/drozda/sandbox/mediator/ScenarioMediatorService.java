package io.drozda.sandbox.mediator;

import java.util.List;

import org.springframework.stereotype.Service;

import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@Service
public class ScenarioMediatorService {

    private final List<ScenarioStarter> scenarioStarters;
    private final List<ScenarioEnvironment> scenarioEnvironments;

    public ScenarioMediatorService(List<ScenarioStarter> scenarioStarters, List<ScenarioEnvironment> scenarioEnvironments) {
        this.scenarioStarters = scenarioStarters;
        this.scenarioEnvironments = scenarioEnvironments;
    }

    public List<ScenarioCommand> commandsFor(String scenarioId) {
        return starterFor(scenarioId).commands();
    }

    public ScenarioEnvironmentStatus environmentStatus(String scenarioId) {
        return environmentFor(scenarioId).status();
    }

    public ScenarioEnvironmentStatus startEnvironment(String scenarioId) {
        return environmentFor(scenarioId).start();
    }

    public ScenarioEnvironmentStatus stopEnvironment(String scenarioId) {
        return environmentFor(scenarioId).stop();
    }

    public ScenarioEnvironmentStatus resetEnvironment(String scenarioId) {
        return environmentFor(scenarioId).reset();
    }

    public ActiveScenarioRuntimeState execute(String scenarioId, String commandId, String invocationName) {
        startEnvironment(scenarioId);
        return starterFor(scenarioId).execute(commandId, invocationName);
    }

    private ScenarioStarter starterFor(String scenarioId) {
        return scenarioStarters.stream()
                .filter(starter -> starter.scenarioId().equals(scenarioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No scenario starter registered for scenario: " + scenarioId));
    }

    private ScenarioEnvironment environmentFor(String scenarioId) {
        return scenarioEnvironments.stream()
                .filter(environment -> environment.scenarioId().equals(scenarioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No scenario environment registered for scenario: " + scenarioId));
    }
}
