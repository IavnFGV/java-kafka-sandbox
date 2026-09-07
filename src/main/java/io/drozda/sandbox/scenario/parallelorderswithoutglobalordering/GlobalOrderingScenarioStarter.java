package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app.GlobalOrderObservation;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app.GlobalOrderingMode;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app.GlobalOrderingScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class GlobalOrderingScenarioStarter implements ScenarioStarter {
    public static final String COMPARE_TOPOLOGY = "compare-topology";
    private static final long STEP_DELAY_MS = 250L;

    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final GlobalOrderingEnvironment environment;

    public GlobalOrderingScenarioStarter(
            ScenarioCatalog catalog,
            ScenarioRuntimeService runtime,
            GlobalOrderingEnvironment environment
    ) {
        this.catalog = catalog;
        this.runtime = runtime;
        this.environment = environment;
    }

    @Override public String scenarioId() { return "global-ordering"; }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(
                COMPARE_TOPOLOGY,
                "Compare Ordered Shards",
                "Compare head-of-line blocking in one partition with independent order processing in two partitions."
        ));
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
        if (!COMPARE_TOPOLOGY.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command: " + commandId);
        }
        GlobalOrderingMode mode = GlobalOrderingMode.from(parameters.get("topology"));
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        runtime.startActiveSession(scenario, invocationName);
        GlobalOrderingScenarioStatus result = environment.compare(invocationName, mode);
        if (!result.published() || !result.received()) {
            failed(scenario, "result", result.error());
            return runtime.completeActiveSession(scenario);
        }

        runtime.updateActiveStep(scenario, 1);
        configureTopology(scenario, result);
        runtime.updateActiveStep(scenario, 2);
        for (GlobalOrderObservation observation : result.observations()) {
            String partitionNode = "partition-" + observation.partition();
            String consumerNode = consumerNode(result.mode(), observation);
            String label = shortOrder(observation) + " " + observation.sequence()
                    + " " + observation.status() + " @" + observation.offset();
            signal(scenario, "publisher", partitionNode, label);
            runtime.updateNodeDetail(scenario, partitionNode,
                    "last: " + shortOrder(observation) + " " + observation.status()
                            + " @" + observation.offset());
            signal(scenario, partitionNode, consumerNode,
                    shortOrder(observation) + " completed at " + observation.completedAfterMs() + " ms");
            runtime.updateNodeDetail(scenario, consumerNode,
                    shortOrder(observation) + " " + observation.status()
                            + " | " + observation.completedAfterMs() + " ms");
        }

        runtime.updateActiveStep(scenario, 3);
        String summary = "mode=" + result.mode()
                + " | fast=" + result.fastOrderCompletedMs() + " ms"
                + " | slow=" + result.slowOrderCompletedMs() + " ms"
                + " | per-order sequence=" + result.perOrderSequencePreserved()
                + " | global completion order=" + result.globalCompletionMatchesPublishOrder();
        runtime.updateNodeDetail(scenario, "result", summary);
        if (result.perOrderSequencePreserved()) {
            ready(scenario, "result", "Per-order ordering preserved");
            ready(scenario, "kafka-broker", "Kafka topology processed both orders");
            ready(scenario, "publisher", "Same interleaved event stream published");
        } else {
            failed(scenario, "result", "Per-order sequence was not preserved: " + summary);
        }
        pause();
        return runtime.completeActiveSession(scenario);
    }

    private void configureTopology(ScenarioGraph scenario, GlobalOrderingScenarioStatus result) {
        runtime.updateNodeDetail(scenario, "topic", result.mode() == GlobalOrderingMode.SINGLE
                ? "1 partition topic | one ordered queue"
                : "2 partition topic | one ordered shard per order");
        assignment(scenario, "partition-0",
                result.mode() == GlobalOrderingMode.SINGLE ? "single-consumer" : "fast-consumer",
                "p0 assigned");
        if (result.mode() == GlobalOrderingMode.PARALLEL) {
            assignment(scenario, "partition-1", "slow-consumer", "p1 assigned");
        }
    }

    private String consumerNode(GlobalOrderingMode mode, GlobalOrderObservation observation) {
        if (mode == GlobalOrderingMode.SINGLE) return "single-consumer";
        return observation.orderId().equals("Fast Order") ? "fast-consumer" : "slow-consumer";
    }

    private String shortOrder(GlobalOrderObservation observation) {
        return observation.orderId().equals("Fast Order") ? "fast" : "slow";
    }

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
        event(scenario, "component-failed", node,
                label != null ? label : "Scenario failed", null, null, "FAILED");
    }

    private void event(
            ScenarioGraph scenario,
            String type,
            String node,
            String label,
            String from,
            String to,
            String status
    ) {
        runtime.applyRuntimeEvent(scenario,
                new RuntimeEventRequest(scenario.id(), type, node, null, label, from, to, status));
    }

    private void pause() {
        try {
            Thread.sleep(STEP_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Global ordering playback interrupted", exception);
        }
    }
}
