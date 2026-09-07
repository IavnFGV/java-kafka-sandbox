package io.drozda.sandbox.scenario.globalordering.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.globalordering.GlobalOrderingScenarioStarter;

@RestController
@RequestMapping("/internal/global-ordering")
@ConditionalOnProperty(name = "scenario.global-ordering.enabled", havingValue = "true")
public class GlobalOrderingScenarioController {
    private final GlobalOrderingExperiment experiment;
    private final GlobalOrderTracker tracker;
    private final String singleTopic;

    public GlobalOrderingScenarioController(
            GlobalOrderingExperiment experiment,
            GlobalOrderTracker tracker,
            @Value("${app.kafka.topics.global-ordering-single}") String singleTopic
    ) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.singleTopic = singleTopic;
    }

    @GetMapping("/status")
    public GlobalOrderingScenarioStatus status() {
        return ready("status", GlobalOrderingMode.SINGLE);
    }

    @PostMapping("/reset")
    public GlobalOrderingScenarioStatus reset() {
        tracker.reset();
        return ready("reset", GlobalOrderingMode.SINGLE);
    }

    @PostMapping("/commands/{commandId}")
    public GlobalOrderingScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) GlobalOrderingCommandRequest request
    ) {
        if (!GlobalOrderingScenarioStarter.COMPARE_TOPOLOGY.equals(commandId)) {
            throw new IllegalArgumentException("Unknown global-ordering command: " + commandId);
        }
        String name = request != null && request.invocationName() != null
                ? request.invocationName() : commandId;
        GlobalOrderingMode mode = request != null && request.mode() != null
                ? request.mode() : GlobalOrderingMode.SINGLE;
        return experiment.run(name, mode);
    }

    private GlobalOrderingScenarioStatus ready(String name, GlobalOrderingMode mode) {
        return new GlobalOrderingScenarioStatus(
                "global-ordering", name, true, true, true, false, false, mode,
                false, false, 0, 0, singleTopic, List.of(), null
        );
    }
}
