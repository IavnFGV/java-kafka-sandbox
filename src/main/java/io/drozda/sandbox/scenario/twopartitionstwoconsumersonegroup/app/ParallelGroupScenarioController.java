package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.ParallelGroupScenarioStarter;

@RestController
@RequestMapping("/internal/parallel-group")
@ConditionalOnProperty(name = "scenario.parallel-group.enabled", havingValue = "true")
public class ParallelGroupScenarioController {
    private final ParallelGroupExperiment experiment;
    private final ParallelGroupTracker tracker;
    private final String topic;

    public ParallelGroupScenarioController(ParallelGroupExperiment experiment, ParallelGroupTracker tracker,
            @Value("${app.kafka.topics.parallel-group}") String topic) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status") public ParallelGroupScenarioStatus status() { return ready("status"); }
    @PostMapping("/reset") public ParallelGroupScenarioStatus reset() { tracker.reset(); return ready("reset"); }

    @PostMapping("/commands/{commandId}")
    public ParallelGroupScenarioStatus execute(@PathVariable String commandId,
            @RequestBody(required = false) ParallelGroupCommandRequest request) {
        if (!ParallelGroupScenarioStarter.OBSERVE_PARALLEL_ASSIGNMENT.equals(commandId)) {
            throw new IllegalArgumentException("Unknown parallel-group command: " + commandId);
        }
        return experiment.run(request != null && request.invocationName() != null
                ? request.invocationName() : commandId);
    }

    private ParallelGroupScenarioStatus ready(String name) {
        return new ParallelGroupScenarioStatus("consumer-group-two-partitions", name,
                true, true, true, false, false, Map.of(), List.of(), topic, null);
    }
}
