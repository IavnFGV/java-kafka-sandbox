package io.drozda.sandbox.scenario.multipleconsumergroups;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.multipleconsumergroups.app.ConsumerGroupObservation;
import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class MultipleConsumerGroupsScenarioStarter implements ScenarioStarter {
    public static final String OBSERVE_INDEPENDENT_GROUPS = "observe-independent-groups";
    private static final long STEP_DELAY_MS = 300;
    private final ScenarioCatalog catalog;
    private final ScenarioRuntimeService runtime;
    private final MultipleConsumerGroupsEnvironment environment;

    public MultipleConsumerGroupsScenarioStarter(
            ScenarioCatalog catalog, ScenarioRuntimeService runtime,
            MultipleConsumerGroupsEnvironment environment) {
        this.catalog = catalog;
        this.runtime = runtime;
        this.environment = environment;
    }

    @Override public String scenarioId() { return "multiple-consumer-groups"; }
    @Override public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(OBSERVE_INDEPENDENT_GROUPS, "Observe Independent Groups",
                "Publish one stream and verify that both consumer groups receive all records."));
    }

    @Override public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!OBSERVE_INDEPENDENT_GROUPS.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command: " + commandId);
        }
        ScenarioGraph scenario = catalog.scenarioById(scenarioId());
        runtime.startActiveSession(scenario, invocationName);
        MultipleConsumerGroupsScenarioStatus result = environment.observe(invocationName);
        if (!result.receivedByBothGroups()) {
            event(scenario, "component-failed", "observer", result.error(), null, null, "FAILED");
            return runtime.completeActiveSession(scenario);
        }

        runtime.updateActiveStep(scenario, 1);
        event(scenario, "component-ready", "audit-consumer", "Member of audit-group", null, null, "READY");
        event(scenario, "component-ready", "notification-consumer", "Member of notification-group", null, null, "READY");

        runtime.updateActiveStep(scenario, 2);
        result.observations().stream()
                .filter(observation -> "audit-group".equals(observation.groupId()))
                .sorted(Comparator.comparingInt(ConsumerGroupObservation::sequence))
                .forEach(observation -> signal(scenario, "producer", "partition-0",
                        "append record " + observation.sequence() + " @" + observation.offset()));

        runtime.updateActiveStep(scenario, 3);
        result.observations().stream().map(ConsumerGroupObservation::sequence).distinct().sorted()
                .forEach(sequence -> parallelDelivery(scenario, result, sequence));

        runtime.updateActiveStep(scenario, 4);
        runtime.updateNodeDetail(scenario, "observer", "3 published | 3 audit | 3 notification | independent offsets");
        event(scenario, "component-ready", "observer", "Both groups received the complete stream", null, null, "READY");
        return runtime.completeActiveSession(scenario);
    }

    private String consumerNode(String groupId) {
        return "audit-group".equals(groupId) ? "audit-consumer" : "notification-consumer";
    }
    private void parallelDelivery(
            ScenarioGraph scenario, MultipleConsumerGroupsScenarioStatus result, int sequence) {
        List<ConsumerGroupObservation> deliveries = result.observations().stream()
                .filter(observation -> observation.sequence() == sequence)
                .sorted(Comparator.comparing(ConsumerGroupObservation::groupId))
                .toList();
        String playbackGroup = "record-" + sequence + "-fan-out";
        deliveries.forEach(observation -> event(
                scenario, "signal-started", null,
                "fan out record " + observation.sequence() + " @" + observation.offset() + " to 2 groups",
                "partition-0", consumerNode(observation.groupId()), "ACTIVE", playbackGroup));
        pause();
        deliveries.forEach(observation -> event(
                scenario, "signal-delivered", null,
                "fan out record " + observation.sequence() + " @" + observation.offset() + " to 2 groups",
                "partition-0", consumerNode(observation.groupId()), "READY", playbackGroup));
    }
    private void signal(ScenarioGraph scenario, String from, String to, String label) {
        event(scenario, "signal-started", null, label, from, to, "ACTIVE");
        pause();
        event(scenario, "signal-delivered", null, label, from, to, "READY");
    }
    private void event(ScenarioGraph scenario, String type, String node, String label,
            String from, String to, String status) {
        event(scenario, type, node, label, from, to, status, null);
    }
    private void event(ScenarioGraph scenario, String type, String node, String label,
            String from, String to, String status, String playbackGroup) {
        runtime.applyRuntimeEvent(scenario,
                new RuntimeEventRequest(
                        scenario.id(), type, node, null, label, from, to, status, playbackGroup));
    }
    private void pause() {
        try { Thread.sleep(STEP_DELAY_MS); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Playback interrupted", exception);
        }
    }
}
