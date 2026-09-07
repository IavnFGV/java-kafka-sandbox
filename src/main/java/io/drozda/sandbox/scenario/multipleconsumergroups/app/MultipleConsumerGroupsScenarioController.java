package io.drozda.sandbox.scenario.multipleconsumergroups.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.multipleconsumergroups.MultipleConsumerGroupsScenarioStarter;

@RestController
@RequestMapping("/internal/multiple-consumer-groups")
@ConditionalOnProperty(name = "scenario.multiple-consumer-groups.enabled", havingValue = "true")
public class MultipleConsumerGroupsScenarioController {
    private final MultipleConsumerGroupsExperiment experiment;
    private final MultipleConsumerGroupsTracker tracker;
    private final String topic;

    public MultipleConsumerGroupsScenarioController(
            MultipleConsumerGroupsExperiment experiment, MultipleConsumerGroupsTracker tracker,
            @Value("${app.kafka.topics.multiple-groups}") String topic) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status") public MultipleConsumerGroupsScenarioStatus status() { return ready("status"); }
    @PostMapping("/reset") public MultipleConsumerGroupsScenarioStatus reset() { tracker.reset(); return ready("reset"); }

    @PostMapping("/commands/{commandId}")
    public MultipleConsumerGroupsScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) MultipleConsumerGroupsCommandRequest request) {
        if (!MultipleConsumerGroupsScenarioStarter.OBSERVE_INDEPENDENT_GROUPS.equals(commandId)) {
            throw new IllegalArgumentException("Unknown multiple-consumer-groups command: " + commandId);
        }
        return experiment.run(request != null && request.invocationName() != null
                ? request.invocationName() : commandId);
    }

    private MultipleConsumerGroupsScenarioStatus ready(String name) {
        return new MultipleConsumerGroupsScenarioStatus(
                "multiple-consumer-groups", name, true, true, true,
                false, false, List.of(), topic, null);
    }
}
