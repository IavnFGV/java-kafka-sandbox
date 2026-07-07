package io.drozda.sandbox.scenario.spi;

import java.util.List;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

public interface ScenarioStarter {

    String scenarioId();

    List<ScenarioCommand> commands();

    ActiveScenarioRuntimeState execute(String commandId, String invocationName);

    default boolean supports(String commandId) {
        return commands().stream().anyMatch(command -> command.id().equals(commandId));
    }
}
