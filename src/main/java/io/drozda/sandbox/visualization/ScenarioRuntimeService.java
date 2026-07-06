package io.drozda.sandbox.visualization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ScenarioRuntimeService {

    private final Map<String, Integer> currentStepByScenario = new ConcurrentHashMap<>();

    public ScenarioRuntimeState getState(ScenarioGraph scenario) {
        int stepIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        return new ScenarioRuntimeState(scenario.id(), clamp(stepIndex, scenario));
    }

    public ScenarioRuntimeState next(ScenarioGraph scenario) {
        int currentIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        int nextIndex = (currentIndex + 1) % scenario.steps().size();
        currentStepByScenario.put(scenario.id(), nextIndex);
        return new ScenarioRuntimeState(scenario.id(), nextIndex);
    }

    public ScenarioRuntimeState previous(ScenarioGraph scenario) {
        int currentIndex = currentStepByScenario.getOrDefault(scenario.id(), 0);
        int previousIndex = (currentIndex - 1 + scenario.steps().size()) % scenario.steps().size();
        currentStepByScenario.put(scenario.id(), previousIndex);
        return new ScenarioRuntimeState(scenario.id(), previousIndex);
    }

    public ScenarioRuntimeState reset(ScenarioGraph scenario) {
        currentStepByScenario.put(scenario.id(), 0);
        return new ScenarioRuntimeState(scenario.id(), 0);
    }

    private int clamp(int stepIndex, ScenarioGraph scenario) {
        if (scenario.steps().isEmpty()) {
            return 0;
        }

        if (stepIndex < 0 || stepIndex >= scenario.steps().size()) {
            return 0;
        }

        return stepIndex;
    }
}
