package io.drozda.sandbox.scenario.orderingwithinonepartition.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.orderingwithinonepartition.PartitionOrderingScenarioStarter;

@RestController
@RequestMapping("/internal/partition-ordering")
@ConditionalOnProperty(name = "scenario.partition-ordering.enabled", havingValue = "true")
public class PartitionOrderingScenarioController {
    private final PartitionOrderingExperiment experiment;
    private final OrderedEventTracker tracker;
    private final String topic;

    public PartitionOrderingScenarioController(
            PartitionOrderingExperiment experiment,
            OrderedEventTracker tracker,
            @Value("${app.kafka.topics.partition-ordering}") String topic
    ) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status")
    public PartitionOrderingScenarioStatus status() { return ready("status"); }

    @PostMapping("/reset")
    public PartitionOrderingScenarioStatus reset() { tracker.reset(); return ready("reset"); }

    @PostMapping("/commands/{commandId}")
    public PartitionOrderingScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) PartitionOrderingCommandRequest request
    ) {
        if (!PartitionOrderingScenarioStarter.VERIFY_ORDER.equals(commandId)) {
            throw new IllegalArgumentException("Unknown partition-ordering command: " + commandId);
        }
        String name = request != null && request.invocationName() != null
                ? request.invocationName() : commandId;
        return experiment.run(name);
    }

    private PartitionOrderingScenarioStatus ready(String name) {
        return new PartitionOrderingScenarioStatus(
                "partition-ordering", name, true, true, true,
                false, false, false, false, false,
                topic, List.of(), tracker.assignments(), null
        );
    }
}
