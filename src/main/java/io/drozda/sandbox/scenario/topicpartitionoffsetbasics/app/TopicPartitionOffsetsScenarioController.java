package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.TopicPartitionOffsetsScenarioStarter;

@RestController
@RequestMapping("/internal/topic-partition-offsets")
@ConditionalOnProperty(name = "scenario.topic-partition-offsets.enabled", havingValue = "true")
public class TopicPartitionOffsetsScenarioController {

    private final TopicPartitionOffsetsExperiment experiment;
    private final PartitionedEventTracker tracker;
    private final String topic;

    public TopicPartitionOffsetsScenarioController(
            TopicPartitionOffsetsExperiment experiment,
            PartitionedEventTracker tracker,
            @Value("${app.kafka.topics.partition-offsets}") String topic
    ) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status")
    public TopicPartitionOffsetsScenarioStatus status() {
        return readyStatus("status");
    }

    @PostMapping("/reset")
    public TopicPartitionOffsetsScenarioStatus reset() {
        tracker.reset();
        return readyStatus("reset");
    }

    @PostMapping("/commands/{commandId}")
    public TopicPartitionOffsetsScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) TopicPartitionOffsetsCommandRequest request
    ) {
        if (!TopicPartitionOffsetsScenarioStarter.APPEND_AND_READ.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command for topic-partition-offsets: " + commandId);
        }

        String invocationName = request != null && request.invocationName() != null
                && !request.invocationName().isBlank()
                ? request.invocationName().trim()
                : TopicPartitionOffsetsScenarioStarter.APPEND_AND_READ;
        return experiment.run(invocationName);
    }

    private TopicPartitionOffsetsScenarioStatus readyStatus(String invocationName) {
        return new TopicPartitionOffsetsScenarioStatus(
                "topic-partition-offsets", invocationName, true, true, true,
                false, false, topic, null, null, null, null, null, null, null
        );
    }
}
