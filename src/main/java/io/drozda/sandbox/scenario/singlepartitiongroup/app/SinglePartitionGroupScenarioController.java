package io.drozda.sandbox.scenario.singlepartitiongroup.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.singlepartitiongroup.SinglePartitionGroupScenarioStarter;

@RestController
@RequestMapping("/internal/single-partition-group")
@ConditionalOnProperty(name = "scenario.single-partition-group.enabled", havingValue = "true")
public class SinglePartitionGroupScenarioController {
    private final SinglePartitionGroupExperiment experiment;
    private final SinglePartitionGroupTracker tracker;
    private final String topic;

    public SinglePartitionGroupScenarioController(
            SinglePartitionGroupExperiment experiment,
            SinglePartitionGroupTracker tracker,
            @Value("${app.kafka.topics.single-partition-group}") String topic
    ) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status")
    public SinglePartitionGroupScenarioStatus status() {
        return ready("status");
    }

    @PostMapping("/reset")
    public SinglePartitionGroupScenarioStatus reset() {
        tracker.resetEvents();
        return ready("reset");
    }

    @PostMapping("/commands/{commandId}")
    public SinglePartitionGroupScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) SinglePartitionGroupCommandRequest request
    ) {
        if (!SinglePartitionGroupScenarioStarter.OBSERVE_TAKEOVER.equals(commandId)) {
            throw new IllegalArgumentException("Unknown single-partition-group command: " + commandId);
        }
        String name = request != null && request.invocationName() != null
                ? request.invocationName() : commandId;
        return experiment.run(name);
    }

    private SinglePartitionGroupScenarioStatus ready(String name) {
        return new SinglePartitionGroupScenarioStatus(
                "consumer-group-single-partition", name, true, true, true,
                false, false, null, null, null, List.of(), topic, null
        );
    }
}
