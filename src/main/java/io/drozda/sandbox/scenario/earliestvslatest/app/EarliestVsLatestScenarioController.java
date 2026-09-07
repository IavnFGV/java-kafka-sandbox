package io.drozda.sandbox.scenario.earliestvslatest.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.earliestvslatest.EarliestVsLatestScenarioStarter;

@RestController
@RequestMapping("/internal/earliest-vs-latest")
@ConditionalOnProperty(name = "scenario.earliest-vs-latest.enabled", havingValue = "true")
public class EarliestVsLatestScenarioController {
    private final EarliestVsLatestExperiment experiment;
    private final OffsetResetTracker tracker;
    private final String topic;
    public EarliestVsLatestScenarioController(EarliestVsLatestExperiment experiment, OffsetResetTracker tracker,
            @Value("${app.kafka.topics.offset-reset}") String topic) {
        this.experiment = experiment; this.tracker = tracker; this.topic = topic;
    }
    @GetMapping("/status") public EarliestVsLatestScenarioStatus status() { return ready("status"); }
    @PostMapping("/reset") public EarliestVsLatestScenarioStatus reset() { tracker.reset(); return ready("reset"); }
    @PostMapping("/commands/{commandId}")
    public EarliestVsLatestScenarioStatus execute(@PathVariable String commandId,
            @RequestBody(required = false) EarliestVsLatestCommandRequest request) {
        if (!EarliestVsLatestScenarioStarter.COMPARE_RESET_POLICIES.equals(commandId))
            throw new IllegalArgumentException("Unknown earliest-vs-latest command: " + commandId);
        return experiment.run(request != null && request.invocationName() != null ? request.invocationName() : commandId);
    }
    private EarliestVsLatestScenarioStatus ready(String name) {
        return new EarliestVsLatestScenarioStatus("earliest-vs-latest", name, true, true, true,
                false, false, List.of(), topic, null);
    }
}
