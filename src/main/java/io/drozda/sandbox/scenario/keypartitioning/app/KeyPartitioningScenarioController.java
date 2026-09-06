package io.drozda.sandbox.scenario.keypartitioning.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.keypartitioning.KeyPartitioningScenarioStarter;

@RestController
@RequestMapping("/internal/key-partitioning")
@ConditionalOnProperty(name = "scenario.key-partitioning.enabled", havingValue = "true")
public class KeyPartitioningScenarioController {
    private final KeyPartitioningExperiment experiment;
    private final KeyedEventTracker tracker;
    private final String topic;

    public KeyPartitioningScenarioController(KeyPartitioningExperiment experiment, KeyedEventTracker tracker,
            @Value("${app.kafka.topics.key-partitioning}") String topic) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status")
    public KeyPartitioningScenarioStatus status() { return ready("status"); }

    @PostMapping("/reset")
    public KeyPartitioningScenarioStatus reset() { tracker.reset(); return ready("reset"); }

    @PostMapping("/commands/{commandId}")
    public KeyPartitioningScenarioStatus execute(@PathVariable String commandId,
            @RequestBody(required = false) KeyPartitioningCommandRequest request) {
        if (!KeyPartitioningScenarioStarter.ROUTE_BY_KEY.equals(commandId)) {
            throw new IllegalArgumentException("Unknown key-partitioning command: " + commandId);
        }
        String name = request != null && request.invocationName() != null ? request.invocationName() : commandId;
        return experiment.run(name);
    }

    private KeyPartitioningScenarioStatus ready(String name) {
        return new KeyPartitioningScenarioStatus(
                "key-partitioning", name, true, true, true, false, false, topic, List.of(), null
        );
    }
}
