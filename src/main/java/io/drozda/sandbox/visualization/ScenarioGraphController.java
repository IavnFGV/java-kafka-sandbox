package io.drozda.sandbox.visualization;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioGraphController {

    @GetMapping("/trade-flow")
    public ScenarioGraph tradeFlowScenario() {
        return new ScenarioGraph(
                "trade-flow",
                "Trade Event Flow",
                "A visual walkthrough of how a trade event moves through the sandbox application.",
                List.of(
                        new ScenarioNode("client", "Client", "external", 90, 220,
                                "A caller triggers a trade event through the application boundary."),
                        new ScenarioNode("publisher", "TradeEventPublisher", "service", 310, 220,
                                "The publisher prepares an event and sends it to Kafka."),
                        new ScenarioNode("kafka", "Kafka Topic", "broker", 530, 220,
                                "The trade-events topic buffers and distributes the event."),
                        new ScenarioNode("listener", "TradeEventListener", "consumer", 750, 220,
                                "The listener receives the event and starts downstream handling."),
                        new ScenarioNode("observer", "System Observer", "monitor", 530, 420,
                                "A future monitoring layer can track delivery, timing, and failures.")
                ),
                List.of(
                        new ScenarioEdge("request", "client", "publisher", "invoke"),
                        new ScenarioEdge("publish", "publisher", "kafka", "send trade event"),
                        new ScenarioEdge("consume", "kafka", "listener", "deliver event"),
                        new ScenarioEdge("inspect", "kafka", "observer", "emit telemetry"),
                        new ScenarioEdge("inspect-listener", "listener", "observer", "listener metrics")
                ),
                List.of(
                        new ScenarioStep("step-1", "Client triggers action",
                                "An external caller starts a business action that creates a trade event.",
                                "client", "request"),
                        new ScenarioStep("step-2", "Publisher sends event",
                                "TradeEventPublisher serializes the payload and sends it to the trade-events topic.",
                                "publisher", "publish"),
                        new ScenarioStep("step-3", "Kafka stores and routes",
                                "Kafka persists the message to the topic partition and makes it available to consumers.",
                                "kafka", "consume"),
                        new ScenarioStep("step-4", "Listener receives event",
                                "TradeEventListener consumes the event and begins application-side processing.",
                                "listener", "consume"),
                        new ScenarioStep("step-5", "Monitoring layer observes flow",
                                "The visualization reminds us where metrics, tracing, retries, and DLT monitoring will live.",
                                "observer", "inspect-listener")
                )
        );
    }
}
