package io.drozda.sandbox.scenario.tradeflow.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.tradeflow.TradeFlowScenarioStarter;

@RestController
@RequestMapping("/internal/trade-flow")
@ConditionalOnProperty(name = "scenario.trade-flow.enabled", havingValue = "true")
public class TradeFlowScenarioController {

    private final TradeFlowExperiment experiment;
    private final TradeFlowEventTracker tracker;
    private final String topic;

    public TradeFlowScenarioController(
            TradeFlowExperiment experiment,
            TradeFlowEventTracker tracker,
            @org.springframework.beans.factory.annotation.Value("${app.kafka.topics.trade-flow}") String topic
    ) {
        this.experiment = experiment;
        this.tracker = tracker;
        this.topic = topic;
    }

    @GetMapping("/status")
    public TradeFlowScenarioStatus status() {
        return readyStatus("status");
    }

    @PostMapping("/reset")
    public TradeFlowScenarioStatus reset() {
        tracker.reset();
        return readyStatus("reset");
    }

    @PostMapping("/commands/{commandId}")
    public TradeFlowScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) TradeFlowScenarioCommandRequest request
    ) {
        if (!TradeFlowScenarioStarter.SEND_AND_RECEIVE.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command for trade-flow scenario app: " + commandId);
        }

        String invocationName = request != null && request.invocationName() != null && !request.invocationName().isBlank()
                ? request.invocationName().trim()
                : TradeFlowScenarioStarter.SEND_AND_RECEIVE;
        return experiment.run(invocationName);
    }

    private TradeFlowScenarioStatus readyStatus(String invocationName) {
        return new TradeFlowScenarioStatus(
                "trade-flow", invocationName, true, true, true,
                false, false, null, topic, null, null, null
        );
    }
}
