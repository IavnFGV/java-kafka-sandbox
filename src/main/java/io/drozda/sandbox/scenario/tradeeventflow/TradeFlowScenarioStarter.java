package io.drozda.sandbox.scenario.tradeeventflow;

import java.util.List;

import org.springframework.stereotype.Component;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.scenario.tradeeventflow.app.TradeFlowScenarioStatus;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;
import io.drozda.sandbox.visualization.RuntimeEventRequest;
import io.drozda.sandbox.visualization.ScenarioCatalog;
import io.drozda.sandbox.visualization.ScenarioGraph;
import io.drozda.sandbox.visualization.ScenarioRuntimeService;

@Component
public class TradeFlowScenarioStarter implements ScenarioStarter {

    public static final String SEND_AND_RECEIVE = "send-and-receive";
    private static final long STEP_DELAY_MS = 650L;

    private final ScenarioCatalog scenarioCatalog;
    private final ScenarioRuntimeService runtimeService;
    private final TradeFlowEnvironment environment;

    public TradeFlowScenarioStarter(
            ScenarioCatalog scenarioCatalog,
            ScenarioRuntimeService runtimeService,
            TradeFlowEnvironment environment
    ) {
        this.scenarioCatalog = scenarioCatalog;
        this.runtimeService = runtimeService;
        this.environment = environment;
    }

    @Override
    public String scenarioId() {
        return "trade-flow";
    }

    @Override
    public List<ScenarioCommand> commands() {
        return List.of(new ScenarioCommand(
                SEND_AND_RECEIVE,
                "Send and Receive Trade Event",
                "Publish a unique event to Kafka and verify that the scenario listener receives it."
        ));
    }

    @Override
    public ActiveScenarioRuntimeState execute(String commandId, String invocationName) {
        if (!SEND_AND_RECEIVE.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command for trade-flow: " + commandId);
        }

        ScenarioGraph scenario = scenarioCatalog.scenarioById(scenarioId());
        String runName = invocationName == null || invocationName.isBlank() ? scenario.title() + " Run" : invocationName.trim();
        runtimeService.startActiveSession(scenario, runName);
        runtimeService.updateActiveStep(scenario, 1);
        event(scenario, "component-busy", "client", null, "Command accepted", null, null, "BUSY");
        event(scenario, "signal-started", null, "request", "Run trade flow", "client", "publisher", "ACTIVE");

        TradeFlowScenarioStatus result = environment.sendAndReceive(runName);
        event(scenario, "signal-delivered", null, "request", "Run trade flow", "client", "publisher", "READY");
        event(scenario, "component-ready", "client", null, "Command delivered", null, null, "READY");

        if (result.published()) {
            showDeliveredSignal(scenario, 2, "publish", "publisher", "topic", "Kafka acknowledged event " + result.eventId());
            event(scenario, "component-ready", "publisher", null, "Publisher acknowledged", null, null, "READY");
            event(scenario, "component-ready", "kafka-broker", null, "Kafka broker responded", null, null, "READY");
            event(scenario, "component-ready", "topic", null,
                    "Stored in partition " + result.partition() + " at offset " + result.offset(), null, null, "READY");
        } else {
            runtimeService.updateActiveStep(scenario, 2);
            event(scenario, "component-failed", "publisher", "publish", result.error(), null, null, "FAILED");
        }

        if (result.received()) {
            showDeliveredSignal(scenario, 3, "consume", "topic", "listener", "Listener received matching event");
            event(scenario, "component-ready", "listener", null, "Matching event received", null, null, "READY");
        } else {
            runtimeService.updateActiveStep(scenario, 3);
            event(scenario, "component-failed", "listener", "consume", result.error(), null, null, "FAILED");
        }

        runtimeService.updateActiveStep(scenario, 4);
        pause();
        return runtimeService.completeActiveSession(scenario);
    }

    private void showDeliveredSignal(
            ScenarioGraph scenario,
            int step,
            String edgeId,
            String from,
            String to,
            String label
    ) {
        runtimeService.updateActiveStep(scenario, step);
        event(scenario, "signal-started", null, edgeId, label, from, to, "ACTIVE");
        pause();
        event(scenario, "signal-delivered", null, edgeId, label, from, to, "READY");
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
            throw new IllegalStateException("Trade-flow playback was interrupted", exception);
        }
    }
}
