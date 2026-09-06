package io.drozda.sandbox.scenario.keypartitioning;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.keypartitioning.app.KeyPartitioningScenarioStatus;
import io.drozda.sandbox.scenario.keypartitioning.app.KeyStrategy;
import io.drozda.sandbox.scenario.keypartitioning.app.KeyedRecordObservation;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class KeyPartitioningScenarioStarter implements ScenarioStarter {
    public static final String ROUTE_BY_KEY = "route-by-key";
    private static final long STEP_DELAY_MS = 550L;

    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final KeyPartitioningEnvironment environment;

    public KeyPartitioningScenarioStarter(ScenarioCatalog catalog, ScenarioRuntimeService runtime,
            KeyPartitioningEnvironment environment) {
        this.catalog = catalog;
        this.runtime = runtime;
        this.environment = environment;
    }

    @Override public String scenarioId() { return "key-partitioning"; }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(ROUTE_BY_KEY, "Route Events by Key",
                "Publish related order events and observe key-based partition selection."));
    }

    @Override
    public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        return execute(commandId, invocationName, Map.of());
    }

    @Override
    public ActiveScenarioRuntimeState execute(
            String commandId,
            String invocationName,
            Map<String, String> parameters
    ) {
        if (!ROUTE_BY_KEY.equals(commandId)) throw new IllegalArgumentException("Unknown command: " + commandId);
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        KeyStrategy strategy = KeyStrategy.from(parameters.get("keyStrategy"));
        runtime.startActiveSession(scenario, invocationName);
        runtime.updateNodeDetail(scenario, "publisher", "selected strategy: " + strategy);
        KeyPartitioningScenarioStatus result = environment.runExperiment(invocationName, strategy);
        if (!result.published() || !result.received()) {
            event(scenario, "component-failed", "publisher", null, result.error(), null, null, "FAILED");
            return runtime.completeActiveSession(scenario);
        }

        result.consumerAssignments().forEach((consumerId, partitions) ->
                runtime.updateNodeDetail(scenario, nodeId(consumerId), "assigned partitions: " + partitions));

        runtime.updateActiveStep(scenario, 1);
        Map<Integer, String> partitionDetails = new java.util.LinkedHashMap<>();
        for (KeyedRecordObservation record : result.observations()) {
            String partitionNode = "partition-" + record.partition();
            String recordLabel = record.orderId() + " " + record.status();
            signal(scenario, null, "publisher", partitionNode,
                    recordLabel + " → p" + record.partition());
            String detail = appendDetail(partitionDetails.get(record.partition()),
                    recordLabel + " @" + record.offset());
            partitionDetails.put(record.partition(), detail);
            runtime.updateNodeDetail(scenario, partitionNode, detail);
            ready(scenario, partitionNode, recordLabel + " appended at offset " + record.offset());
            String consumerNode = nodeId(record.consumerId());
            signal(scenario, null, partitionNode, consumerNode,
                    record.consumerId() + " consumed " + recordLabel + " from p" + record.partition());
        }
        int orderPartition = result.observations().get(0).partition();
        runtime.updateActiveStep(scenario, 2);
        if (result.learningGoalMet()) {
            ready(scenario, "publisher", "orderId selected as stable business key");
            result.consumerAssignments().keySet().forEach(consumerId ->
                    ready(scenario, nodeId(consumerId), consumerId + " assignment active"));
            ready(scenario, "kafka-broker", "Kafka partitioner routed all records");
            ready(scenario, "topic", "Related events share one partition log");
        } else {
            waiting(scenario, "publisher", "Strategy does not preserve order-42 affinity");
            result.consumerAssignments().keySet().forEach(consumerId ->
                    waiting(scenario, nodeId(consumerId), consumerId + " cannot restore cross-partition order"));
            waiting(scenario, "kafka-broker", "Kafka worked, but the business key is unsafe");
            waiting(scenario, "topic", "Technical run completed without an ordering guarantee");
        }
        pause();
        return runtime.completeActiveSession(scenario);
    }

    private String appendDetail(String existing, String record) {
        return existing == null || existing.isBlank() ? record : existing + " | " + record;
    }

    private String nodeId(String consumerId) {
        return consumerId.toLowerCase().replace(' ', '-');
    }

    private void signal(ScenarioGraph scenario, String edge, String from, String to, String label) {
        event(scenario, "signal-started", null, edge, label, from, to, "ACTIVE");
        pause();
        event(scenario, "signal-delivered", null, edge, label, from, to, "READY");
    }

    private void ready(ScenarioGraph scenario, String node, String label) {
        event(scenario, "component-ready", node, null, label, null, null, "READY");
    }

    private void waiting(ScenarioGraph scenario, String node, String label) {
        event(scenario, "component-waiting", node, null, label, null, null, "WAITING");
    }

    private void event(ScenarioGraph scenario, String type, String node, String edge, String label,
            String from, String to, String status) {
        runtime.applyRuntimeEvent(scenario,
                new RuntimeEventRequest(scenario.id(), type, node, edge, label, from, to, status));
    }

    private void pause() {
        try { Thread.sleep(STEP_DELAY_MS); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Key partitioning playback interrupted", exception);
        }
    }
}
