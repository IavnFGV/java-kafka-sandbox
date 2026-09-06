package io.drozda.sandbox.scenario.keypartitioning;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Map<Integer, List<KeyedRecordObservation>> byPartition = result.observations().stream()
                .collect(Collectors.groupingBy(KeyedRecordObservation::partition));
        for (int partition = 0; partition < 3; partition++) {
            String nodeId = "partition-" + partition;
            List<KeyedRecordObservation> records = byPartition.getOrDefault(partition, List.of());
            String detail = records.isEmpty() ? "no records in this run" : records.stream()
                    .map(record -> record.orderId() + " " + record.status() + " @" + record.offset())
                    .collect(Collectors.joining(" | "));
            runtime.updateNodeDetail(scenario, nodeId, detail);
            if (!records.isEmpty()) ready(scenario, nodeId, "Partition " + partition + " received keyed records");
        }
        int orderPartition = result.observations().get(0).partition();
        runtime.updateActiveStep(scenario, 1);
        signal(scenario, "publish", "publisher", "partition-" + orderPartition,
                strategy + " → observed partition " + orderPartition);
        runtime.updateActiveStep(scenario, 2);
        if (result.learningGoalMet()) {
            ready(scenario, "publisher", "orderId selected as stable business key");
            ready(scenario, "consumer", "Consumer verified one ordered shard for order-42");
            runtime.updateNodeDetail(scenario, "consumer", "GUARANTEED: order-42 → partition " + orderPartition);
            ready(scenario, "kafka-broker", "Kafka partitioner routed all records");
            ready(scenario, "topic", "Related events share one partition log");
        } else {
            waiting(scenario, "publisher", "Strategy does not preserve order-42 affinity");
            waiting(scenario, "consumer", "Observed distribution is not a routing guarantee");
            runtime.updateNodeDetail(scenario, "consumer", "LEARNING GOAL NOT MET: choose ORDER_ID");
            waiting(scenario, "kafka-broker", "Kafka worked, but the business key is unsafe");
            waiting(scenario, "topic", "Technical run completed without an ordering guarantee");
        }
        pause();
        return runtime.completeActiveSession(scenario);
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
