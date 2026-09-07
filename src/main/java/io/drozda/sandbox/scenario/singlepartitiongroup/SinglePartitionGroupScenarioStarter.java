package io.drozda.sandbox.scenario.singlepartitiongroup;

import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.singlepartitiongroup.app.GroupWorkObservation;
import io.drozda.sandbox.scenario.singlepartitiongroup.app.SinglePartitionGroupScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class SinglePartitionGroupScenarioStarter implements ScenarioStarter {
    public static final String OBSERVE_TAKEOVER = "observe-takeover";
    private static final long STEP_DELAY_MS = 350L;

    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final SinglePartitionGroupEnvironment environment;

    public SinglePartitionGroupScenarioStarter(
            ScenarioCatalog catalog,
            ScenarioRuntimeService runtime,
            SinglePartitionGroupEnvironment environment
    ) {
        this.catalog = catalog;
        this.runtime = runtime;
        this.environment = environment;
    }

    @Override public String scenarioId() { return "consumer-group-single-partition"; }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(
                OBSERVE_TAKEOVER,
                "Observe Assignment and Takeover",
                "Show one active owner, one idle group member, then stop the owner and observe rebalance."
        ));
    }

    @Override
    public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!OBSERVE_TAKEOVER.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command: " + commandId);
        }
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        runtime.startActiveSession(scenario, invocationName);
        SinglePartitionGroupScenarioStatus result = environment.observeTakeover(invocationName);
        if (!result.received()) {
            failed(scenario, "observer", result.error());
            return runtime.completeActiveSession(scenario);
        }

        String initialOwnerNode = node(result.initialOwner());
        String idleNode = node(result.initialIdleConsumer());
        String takeoverNode = node(result.takeoverOwner());

        runtime.updateActiveStep(scenario, 1);
        runtime.updateNodeDetail(scenario, initialOwnerNode, "owns Partition 0");
        runtime.updateNodeDetail(scenario, idleNode, "healthy group member | no assignment");
        event(scenario, "component-ready", initialOwnerNode, "Partition 0 owner", null, null, "READY");
        event(scenario, "component-waiting", idleNode, "Idle: no partition available", null, null, "WAITING");
        assignment(scenario, initialOwnerNode, "initial assignment");

        runtime.updateActiveStep(scenario, 2);
        animatePhase(scenario, result.observations(), "BEFORE FAILURE", initialOwnerNode);

        runtime.updateActiveStep(scenario, 3);
        event(scenario, "component-failed", initialOwnerNode, "Consumer stopped", null, null, "FAILED");
        pause();
        runtime.updateNodeDetail(scenario, takeoverNode, "owns Partition 0 after rebalance");
        assignment(scenario, takeoverNode, "partition reassigned");
        event(scenario, "component-ready", takeoverNode, "Took over Partition 0", null, null, "READY");

        runtime.updateActiveStep(scenario, 4);
        animatePhase(scenario, result.observations(), "AFTER TAKEOVER", takeoverNode);
        runtime.updateNodeDetail(scenario, "observer",
                "initial owner=" + result.initialOwner()
                        + " | idle=" + result.initialIdleConsumer()
                        + " | takeover=" + result.takeoverOwner());
        event(scenario, "component-ready", "observer", "No records were assigned to two consumers at once",
                null, null, "READY");
        return runtime.completeActiveSession(scenario);
    }

    private void animatePhase(
            ScenarioGraph scenario,
            List<GroupWorkObservation> observations,
            String phase,
            String consumerNode
    ) {
        observations.stream().filter(observation -> observation.phase().equals(phase)).forEach(observation -> {
            signal(scenario, "producer", "single-partition",
                    "record " + observation.sequence() + " appended @" + observation.offset());
            signal(scenario, "single-partition", consumerNode,
                    "record " + observation.sequence() + " -> " + observation.consumerId());
            runtime.updateNodeDetail(scenario, "single-partition",
                    "last offset=" + observation.offset() + " | owner=" + observation.consumerId());
        });
    }

    private String node(String consumerId) {
        return "Consumer A".equals(consumerId) ? "consumer-a" : "consumer-b";
    }

    private void assignment(ScenarioGraph scenario, String consumerNode, String label) {
        event(scenario, "signal-started", null, label,
                "single-partition", consumerNode, "READY");
    }

    private void signal(ScenarioGraph scenario, String from, String to, String label) {
        event(scenario, "signal-started", null, label, from, to, "ACTIVE");
        pause();
        event(scenario, "signal-delivered", null, label, from, to, "READY");
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
            throw new IllegalStateException("Single-partition group playback interrupted", exception);
        }
    }
}
