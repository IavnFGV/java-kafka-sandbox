package io.drozda.sandbox.scenario.earliestvslatest;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.earliestvslatest.app.EarliestVsLatestScenarioStatus;
import io.drozda.sandbox.scenario.earliestvslatest.app.OffsetResetObservation;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class EarliestVsLatestScenarioStarter implements ScenarioStarter {
    public static final String COMPARE_RESET_POLICIES = "compare-reset-policies";
    private static final long DELAY_MS = 300;
    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final EarliestVsLatestEnvironment environment;
    public EarliestVsLatestScenarioStarter(ScenarioCatalog catalog, ScenarioRuntimeService runtime,
            EarliestVsLatestEnvironment environment) {
        this.catalog = catalog; this.runtime = runtime; this.environment = environment;
    }
    @Override public String scenarioId() { return "earliest-vs-latest"; }
    @Override public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(COMPARE_RESET_POLICIES, "Compare Reset Policies",
                "Create history, start two new groups, then compare the offsets they receive."));
    }
    @Override public ActiveScenarioRuntimeState execute(String commandId, String name) {
        if (!COMPARE_RESET_POLICIES.equals(commandId)) throw new IllegalArgumentException("Unknown command: " + commandId);
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        runtime.startActiveSession(scenario, name);
        EarliestVsLatestScenarioStatus result = environment.compare(name);
        if (!result.verified()) {
            event(scenario, "component-failed", "observer", result.error(), null, null, "FAILED", null);
            return runtime.completeActiveSession(scenario);
        }
        List<OffsetResetObservation> earliest = group(result, "earliest");
        List<OffsetResetObservation> latest = group(result, "latest");
        runtime.updateActiveStep(scenario, 1);
        earliest.stream().filter(value -> "HISTORY".equals(value.phase())).forEach(value ->
                signal(scenario, "producer", "partition-0", "append history @" + value.offset()));
        runtime.updateActiveStep(scenario, 2);
        event(scenario, "component-ready", "earliest-consumer", "assigned with earliest", null, null, "READY", null);
        event(scenario, "component-ready", "latest-consumer", "assigned with latest", null, null, "READY", null);
        earliest.stream().filter(value -> "HISTORY".equals(value.phase())).forEach(value ->
                signal(scenario, "partition-0", "earliest-consumer", "replay history @" + value.offset()));
        runtime.updateActiveStep(scenario, 3);
        earliest.stream().filter(value -> "LIVE".equals(value.phase())).forEach(value -> {
            signal(scenario, "producer", "partition-0", "append live @" + value.offset());
            parallelLive(scenario, value.offset());
        });
        runtime.updateActiveStep(scenario, 4);
        runtime.updateNodeDetail(scenario, "earliest-consumer", "received offsets " + offsets(earliest));
        runtime.updateNodeDetail(scenario, "latest-consumer", "received offsets " + offsets(latest));
        runtime.updateNodeDetail(scenario, "observer", "earliest: 0..4 | latest: 3..4");
        event(scenario, "component-ready", "observer", "Reset policies produced different starting positions",
                null, null, "READY", null);
        return runtime.completeActiveSession(scenario);
    }
    private List<OffsetResetObservation> group(EarliestVsLatestScenarioStatus result, String group) {
        return result.observations().stream().filter(value -> group.equals(value.group()))
                .sorted(Comparator.comparingLong(OffsetResetObservation::offset)).toList();
    }
    private List<Long> offsets(List<OffsetResetObservation> values) {
        return values.stream().map(OffsetResetObservation::offset).toList();
    }
    private void parallelLive(ScenarioGraph scenario, long offset) {
        String group = "live-" + offset;
        event(scenario, "signal-started", null, "deliver live @" + offset,
                "partition-0", "earliest-consumer", "ACTIVE", group);
        event(scenario, "signal-started", null, "deliver live @" + offset,
                "partition-0", "latest-consumer", "ACTIVE", group);
        pause();
        event(scenario, "signal-delivered", null, "deliver live @" + offset,
                "partition-0", "earliest-consumer", "READY", group);
        event(scenario, "signal-delivered", null, "deliver live @" + offset,
                "partition-0", "latest-consumer", "READY", group);
    }
    private void signal(ScenarioGraph scenario, String from, String to, String label) {
        event(scenario, "signal-started", null, label, from, to, "ACTIVE", null); pause();
        event(scenario, "signal-delivered", null, label, from, to, "READY", null);
    }
    private void event(ScenarioGraph scenario, String type, String node, String label,
            String from, String to, String status, String playbackGroup) {
        runtime.applyRuntimeEvent(scenario, new RuntimeEventRequest(
                scenario.id(), type, node, null, label, from, to, status, playbackGroup));
    }
    private void pause() {
        try { Thread.sleep(DELAY_MS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
    }
}
