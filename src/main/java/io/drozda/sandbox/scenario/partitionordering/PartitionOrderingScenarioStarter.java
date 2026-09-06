package io.drozda.sandbox.scenario.partitionordering;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.partitionordering.app.OrderedRecordObservation;
import io.drozda.sandbox.scenario.partitionordering.app.PartitionOrderingScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class PartitionOrderingScenarioStarter implements ScenarioStarter {
    public static final String VERIFY_ORDER = "verify-order";
    private static final long STEP_DELAY_MS = 400L;

    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final PartitionOrderingEnvironment environment;

    public PartitionOrderingScenarioStarter(
            ScenarioCatalog catalog,
            ScenarioRuntimeService runtime,
            PartitionOrderingEnvironment environment
    ) {
        this.catalog = catalog;
        this.runtime = runtime;
        this.environment = environment;
    }

    @Override public String scenarioId() { return "partition-ordering"; }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(VERIFY_ORDER, "Verify Partition Order",
                "Compare business sequence, Kafka offsets, and listener callback order."));
    }

    @Override
    public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!VERIFY_ORDER.equals(commandId)) throw new IllegalArgumentException("Unknown command: " + commandId);
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        runtime.startActiveSession(scenario, invocationName);
        PartitionOrderingScenarioStatus result = environment.verifyOrder(invocationName);
        if (!result.published() || !result.received()) {
            failed(scenario, "publisher", result.error());
            return runtime.completeActiveSession(scenario);
        }

        result.consumerAssignments().forEach((consumerId, partitions) -> {
            runtime.updateNodeDetail(scenario, nodeId(consumerId), "assigned partitions: " + partitions);
            partitions.forEach(partition -> assignment(
                    scenario, "partition-" + partition, nodeId(consumerId), "p" + partition + " assigned"));
        });

        runtime.updateActiveStep(scenario, 1);
        for (OrderedRecordObservation record : result.observations()) {
            String partitionNode = "partition-" + record.partition();
            String label = "seq " + record.sequence() + " " + record.status() + " @" + record.offset();
            signal(scenario, "publisher", partitionNode, label);
            runtime.updateNodeDetail(scenario, partitionNode,
                    "last appended: seq " + record.sequence() + " @" + record.offset());
            signal(scenario, partitionNode, nodeId(record.consumerId()),
                    record.consumerId() + " read seq " + record.sequence());
        }

        runtime.updateActiveStep(scenario, 2);
        boolean verified = result.onePartition()
                && result.increasingOffsets()
                && result.receiveOrderPreserved();
        String summary = "one partition=" + result.onePartition()
                + ", offsets increasing=" + result.increasingOffsets()
                + ", callback order=" + result.receiveOrderPreserved();
        runtime.updateNodeDetail(scenario, "order-check", summary);
        if (verified) {
            ready(scenario, "publisher", "Producer sent sequence 0..5 with one orderId key");
            ready(scenario, "order-check", "Partition ordering verified");
            ready(scenario, "kafka-broker", "Kafka preserved append order inside the partition");
            result.consumerAssignments().keySet().forEach(consumer ->
                    ready(scenario, nodeId(consumer), consumer + " assignment active"));
        } else {
            failed(scenario, "order-check", "Ordering verification failed: " + summary);
        }
        pause();
        return runtime.completeActiveSession(scenario);
    }

    private String nodeId(String consumerId) { return consumerId.toLowerCase().replace(' ', '-'); }

    private void signal(ScenarioGraph scenario, String from, String to, String label) {
        event(scenario, "signal-started", null, label, from, to, "ACTIVE");
        pause();
        event(scenario, "signal-delivered", null, label, from, to, "READY");
    }

    private void assignment(ScenarioGraph scenario, String from, String to, String label) {
        event(scenario, "signal-started", null, label, from, to, "READY");
    }

    private void ready(ScenarioGraph scenario, String node, String label) {
        event(scenario, "component-ready", node, label, null, null, "READY");
    }

    private void failed(ScenarioGraph scenario, String node, String label) {
        event(scenario, "component-failed", node, label, null, null, "FAILED");
    }

    private void event(
            ScenarioGraph scenario, String type, String node, String label,
            String from, String to, String status
    ) {
        runtime.applyRuntimeEvent(scenario,
                new RuntimeEventRequest(scenario.id(), type, node, null, label, from, to, status));
    }

    private void pause() {
        try {
            Thread.sleep(STEP_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Partition ordering playback interrupted", exception);
        }
    }
}
