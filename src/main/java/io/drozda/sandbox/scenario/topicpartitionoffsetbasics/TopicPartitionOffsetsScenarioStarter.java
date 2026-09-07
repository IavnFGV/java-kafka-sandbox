package io.drozda.sandbox.scenario.topicpartitionoffsetbasics;

import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app.TopicPartitionOffsetsScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class TopicPartitionOffsetsScenarioStarter implements ScenarioStarter {

    public static final String APPEND_AND_READ = "append-and-read";
    private static final long STEP_DELAY_MS = 650L;

    private final ScenarioCatalog scenarioCatalog;
    private final ScenarioRuntimeService runtimeService;
    private final TopicPartitionOffsetsEnvironment environment;

    public TopicPartitionOffsetsScenarioStarter(
            ScenarioCatalog scenarioCatalog,
            ScenarioRuntimeService runtimeService,
            TopicPartitionOffsetsEnvironment environment
    ) {
        this.scenarioCatalog = scenarioCatalog;
        this.runtimeService = runtimeService;
        this.environment = environment;
    }

    @Override
    public String scenarioId() {
        return "topic-partition-offsets";
    }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(
                APPEND_AND_READ,
                "Append Records and Inspect Offsets",
                "Append records to two partitions and verify their coordinates on the consumer side."
        ));
    }

    @Override
    public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!APPEND_AND_READ.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command for topic-partition-offsets: " + commandId);
        }

        ScenarioGraph scenario = scenarioCatalog.scenarioById(scenarioId());
        String runName = invocationName == null || invocationName.isBlank()
                ? scenario.title() + " Run"
                : invocationName.trim();
        runtimeService.startActiveSession(scenario, runName);
        ready(scenario, "producer", "Producer is ready");
        runtimeService.updateNodeDetail(scenario, "producer", "explicit route: p0 → p1 → p0");
        ready(scenario, "kafka-broker", "Kafka broker and two-partition topic are ready");
        ready(scenario, "orders-topic-box", "Topic has partitions 0 and 1");

        TopicPartitionOffsetsScenarioStatus result = environment.appendAndRead(runName);
        if (!result.published()) {
            failed(scenario, "producer", result.error());
            return finish(scenario);
        }

        append(scenario, 1, "producer-p0", "producer", "partition-0",
                "A → p" + result.firstPartition() + " offset " + result.firstOffset());
        runtimeService.updateNodeDetail(scenario, "partition-0", "A @ offset " + result.firstOffset());
        ready(scenario, "partition-0", "Partition 0 stored A at offset " + result.firstOffset());

        append(scenario, 2, "producer-p1", "producer", "partition-1",
                "B → p" + result.secondPartition() + " offset " + result.secondOffset());
        runtimeService.updateNodeDetail(scenario, "partition-1", "B @ offset " + result.secondOffset());
        ready(scenario, "partition-1", "Partition 1 stored B at offset " + result.secondOffset());

        append(scenario, 3, "producer-p0", "producer", "partition-0",
                "C → p" + result.thirdPartition() + " offset " + result.thirdOffset());
        runtimeService.updateNodeDetail(scenario, "partition-0",
                "A @ offset " + result.firstOffset() + " | C @ offset " + result.thirdOffset());
        ready(scenario, "partition-0", "Partition 0 advanced to offset " + result.thirdOffset());

        runtimeService.updateActiveStep(scenario, 4);
        if (result.received()) {
            signal(scenario, "p0-consumer", "partition-0", "consumer", "Read partition 0 records");
            signal(scenario, "p1-consumer", "partition-1", "consumer", "Read partition 1 record");
            runtimeService.updateNodeDetail(scenario, "consumer", "assigned partitions: 0, 1");
            ready(scenario, "consumer", "Consumer verified topic + partition + offset coordinates");
        } else {
            failed(scenario, "consumer", result.error());
        }
        return finish(scenario);
    }

    private void append(ScenarioGraph scenario, int step, String edgeId, String from, String to, String label) {
        runtimeService.updateActiveStep(scenario, step);
        signal(scenario, edgeId, from, to, label);
    }

    private void signal(ScenarioGraph scenario, String edgeId, String from, String to, String label) {
        event(scenario, "signal-started", null, edgeId, label, from, to, "ACTIVE");
        pause();
        event(scenario, "signal-delivered", null, edgeId, label, from, to, "READY");
    }

    private void ready(ScenarioGraph scenario, String nodeId, String label) {
        event(scenario, "component-ready", nodeId, null, label, null, null, "READY");
    }

    private void failed(ScenarioGraph scenario, String nodeId, String label) {
        event(scenario, "component-failed", nodeId, null, label, null, null, "FAILED");
    }

    private ActiveScenarioRuntimeState finish(ScenarioGraph scenario) {
        pause();
        return runtimeService.completeActiveSession(scenario);
    }

    private void event(
            ScenarioGraph scenario,
            String type,
            String nodeId,
            String edgeId,
            String label,
            String from,
            String to,
            String status
    ) {
        runtimeService.applyRuntimeEvent(
                scenario,
                new RuntimeEventRequest(scenario.id(), type, nodeId, edgeId, label, from, to, status)
        );
    }

    private void pause() {
        try {
            Thread.sleep(STEP_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Topic-partition-offsets playback was interrupted", exception);
        }
    }
}
