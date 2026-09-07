package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupObservation;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class ParallelGroupScenarioStarter implements ScenarioStarter {
    public static final String OBSERVE_PARALLEL_ASSIGNMENT = "observe-parallel-assignment";
    private static final long STEP_DELAY_MS = 300;
    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final ParallelGroupEnvironment environment;

    public ParallelGroupScenarioStarter(ScenarioCatalog catalog, ScenarioRuntimeService runtime,
            ParallelGroupEnvironment environment) {
        this.catalog = catalog; this.runtime = runtime; this.environment = environment;
    }
    @Override public String scenarioId() { return "consumer-group-two-partitions"; }
    @Override public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(OBSERVE_PARALLEL_ASSIGNMENT, "Observe Parallel Assignment",
                "Assign two partitions to two consumers and process both shards."));
    }

    @Override public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!OBSERVE_PARALLEL_ASSIGNMENT.equals(commandId)) throw new IllegalArgumentException("Unknown command: " + commandId);
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        runtime.startActiveSession(scenario, invocationName);
        ParallelGroupScenarioStatus result = environment.observe(invocationName);
        if (!result.received()) { event(scenario, "component-failed", "observer", result.error(), null, null, "FAILED"); return runtime.completeActiveSession(scenario); }

        runtime.updateActiveStep(scenario, 1);
        result.partitionOwners().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
            String partition = "partition-" + entry.getKey();
            String consumer = node(entry.getValue());
            event(scenario, "signal-started", null, "p" + entry.getKey() + " assigned", partition, consumer, "READY");
            runtime.updateNodeDetail(scenario, consumer, "owns Partition " + entry.getKey());
            event(scenario, "component-ready", consumer, "Active partition owner", null, null, "READY");
        });

        runtime.updateActiveStep(scenario, 2);
        result.observations().stream().sorted(Comparator.comparingInt(ParallelGroupObservation::sequence)).forEach(observation -> {
            String partition = "partition-" + observation.partition();
            String consumer = node(observation.consumerId());
            signal(scenario, "producer", partition, "record " + observation.sequence() + " @" + observation.offset());
            signal(scenario, partition, consumer, "record " + observation.sequence() + " consumed");
            runtime.updateNodeDetail(scenario, partition,
                    "owner=" + observation.consumerId() + " | last offset=" + observation.offset());
        });

        runtime.updateActiveStep(scenario, 3);
        runtime.updateNodeDetail(scenario, "observer", "2 partitions | 2 active consumers | 1 consumer per partition");
        event(scenario, "component-ready", "observer", "Both consumers processed independent shards", null, null, "READY");
        return runtime.completeActiveSession(scenario);
    }

    private String node(String consumer) { return "Consumer A".equals(consumer) ? "consumer-a" : "consumer-b"; }
    private void signal(ScenarioGraph scenario, String from, String to, String label) {
        event(scenario, "signal-started", null, label, from, to, "ACTIVE"); pause();
        event(scenario, "signal-delivered", null, label, from, to, "READY");
    }
    private void event(ScenarioGraph scenario, String type, String node, String label,
            String from, String to, String status) {
        runtime.applyRuntimeEvent(scenario, new RuntimeEventRequest(scenario.id(), type, node, null, label, from, to, status));
    }
    private void pause() {
        try { Thread.sleep(STEP_DELAY_MS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("Playback interrupted", exception); }
    }
}
