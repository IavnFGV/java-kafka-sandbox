package io.drozda.sandbox.scenario.spi;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;

public interface ScenarioEnvironment {

    String scenarioId();

    ScenarioEnvironmentStatus start();

    ScenarioEnvironmentStatus stop();

    ScenarioEnvironmentStatus reset();

    ScenarioEnvironmentStatus status();
}
