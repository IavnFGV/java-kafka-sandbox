package io.drozda.sandbox.visualization;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ScenarioCatalog {

    public ScenarioGraph scenarioById(String scenarioId) {
        return switch (scenarioId) {
            case "trade-flow" -> tradeFlowScenario();
            case "system-ready" -> systemReadyScenario();
            default -> throw new IllegalArgumentException("Unknown scenario id: " + scenarioId);
        };
    }

    public ScenarioGraph tradeFlowScenario() {
        return new ScenarioGraph(
                "trade-flow",
                "Trade Event Flow",
                "A visual walkthrough of how a trade event moves through the sandbox application. Drag nodes to refine the layout.",
                1200,
                720,
                List.of(
                        new ScenarioNode("client", "Client", "external", 70, 300, 170, 96, null,
                                "A caller triggers a trade event through the application boundary."),
                        new ScenarioNode("spring-app", "Spring Boot Application", "container", 280, 150, 420, 360, null,
                                "The application boundary groups the publisher and listener components."),
                        new ScenarioNode("publisher", "TradeEventPublisher", "service", 36, 86, 170, 96, "spring-app",
                                "The publisher prepares an event and sends it to Kafka."),
                        new ScenarioNode("listener", "TradeEventListener", "consumer", 212, 212, 170, 96, "spring-app",
                                "The listener receives the event and starts downstream handling."),
                        new ScenarioNode("kafka", "Kafka Topic", "broker", 820, 300, 190, 96, null,
                                "The trade-events topic buffers and distributes the event."),
                        new ScenarioNode("observer", "System Observer", "monitor", 820, 500, 190, 96, null,
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
                                List.of("event-client-request")),
                        new ScenarioStep("step-2", "Publisher sends event",
                                "TradeEventPublisher serializes the payload and sends it to the trade-events topic.",
                                List.of("event-publish")),
                        new ScenarioStep("step-3", "Kafka stores and routes",
                                "Kafka persists the message to the topic partition and makes it available to consumers.",
                                List.of("event-kafka-route", "event-kafka-telemetry")),
                        new ScenarioStep("step-4", "Listener receives event",
                                "TradeEventListener consumes the event and begins application-side processing.",
                                List.of("event-listener-receive")),
                        new ScenarioStep("step-5", "Monitoring layer observes flow",
                                "The visualization reminds us where metrics, tracing, retries, and DLT monitoring will live.",
                                List.of("event-kafka-telemetry", "event-listener-telemetry"))
                ),
                List.of(
                        new VisualizationEvent(
                                "event-client-request",
                                "command",
                                "TradeEvent",
                                "A client-side action enters the Spring Boot application boundary.",
                                List.of("client", "spring-app", "publisher"),
                                List.of("request"),
                                "client",
                                "publisher"
                        ),
                        new VisualizationEvent(
                                "event-publish",
                                "publish",
                                "TradeEvent",
                                "TradeEventPublisher emits a trade event to Kafka.",
                                List.of("spring-app", "publisher", "kafka"),
                                List.of("publish"),
                                "publisher",
                                "kafka"
                        ),
                        new VisualizationEvent(
                                "event-kafka-route",
                                "routing",
                                "TradeEvent",
                                "Kafka routes the event toward subscribed consumers.",
                                List.of("kafka", "listener", "spring-app"),
                                List.of("consume"),
                                "kafka",
                                "listener"
                        ),
                        new VisualizationEvent(
                                "event-kafka-telemetry",
                                "telemetry",
                                "Telemetry",
                                "Kafka emits delivery telemetry that can feed monitoring.",
                                List.of("kafka", "observer"),
                                List.of("inspect"),
                                "kafka",
                                "observer"
                        ),
                        new VisualizationEvent(
                                "event-listener-receive",
                                "consume",
                                "TradeEvent",
                                "TradeEventListener receives the event inside the application container.",
                                List.of("spring-app", "listener"),
                                List.of("consume"),
                                "kafka",
                                "listener"
                        ),
                        new VisualizationEvent(
                                "event-listener-telemetry",
                                "telemetry",
                                "Telemetry",
                                "The listener can emit metrics and traces to the observer layer.",
                                List.of("listener", "observer", "spring-app"),
                                List.of("inspect-listener"),
                                "listener",
                                "observer"
                        )
                )
        );
    }

    public ScenarioGraph systemReadyScenario() {
        return new ScenarioGraph(
                "system-ready",
                "System Ready",
                "A basic readiness walkthrough: the Spring Boot app starts, Kafka-facing components are injected, and the sandbox is ready.",
                1100,
                640,
                List.of(
                        new ScenarioNode("spring-app", "Spring Boot Application", "container", 250, 120, 420, 320, null,
                                "The application context starts and wires the sandbox components."),
                        new ScenarioNode("publisher", "TradeEventPublisher", "service", 36, 86, 170, 96, "spring-app",
                                "The publisher bean is created and ready to send events."),
                        new ScenarioNode("listener", "TradeEventListener", "consumer", 212, 86, 170, 96, "spring-app",
                                "The Kafka listener bean is created and ready to receive events."),
                        new ScenarioNode("kafka", "Kafka Broker", "broker", 760, 250, 190, 96, null,
                                "Kafka is reachable from the sandbox application.")
                ),
                List.of(
                        new ScenarioEdge("publisher-kafka", "publisher", "kafka", "publisher ready"),
                        new ScenarioEdge("listener-kafka", "listener", "kafka", "listener subscribed")
                ),
                List.of(
                        new ScenarioStep("step-1", "Spring context started",
                                "The application context boots and the Spring Boot container becomes available.",
                                List.of("event-spring-started")),
                        new ScenarioStep("step-2", "Publisher bean ready",
                                "TradeEventPublisher is injected and ready to send messages to Kafka.",
                                List.of("event-publisher-ready")),
                        new ScenarioStep("step-3", "Listener bean ready",
                                "TradeEventListener is injected and can subscribe to Kafka records.",
                                List.of("event-listener-ready")),
                        new ScenarioStep("step-4", "System ready",
                                "All key components are alive, wired, and ready for scenario execution.",
                                List.of("event-system-ready"))
                ),
                List.of(
                        new VisualizationEvent(
                                "event-spring-started",
                                "startup",
                                "Context Up",
                                "The Spring Boot application context has started.",
                                List.of("spring-app"),
                                List.of(),
                                null,
                                null
                        ),
                        new VisualizationEvent(
                                "event-publisher-ready",
                                "readiness",
                                "Publisher Ready",
                                "TradeEventPublisher has been injected and is ready to publish.",
                                List.of("spring-app", "publisher", "kafka"),
                                List.of("publisher-kafka"),
                                "publisher",
                                "kafka"
                        ),
                        new VisualizationEvent(
                                "event-listener-ready",
                                "readiness",
                                "Listener Ready",
                                "TradeEventListener has been injected and is ready to consume from Kafka.",
                                List.of("spring-app", "listener", "kafka"),
                                List.of("listener-kafka"),
                                "listener",
                                "kafka"
                        ),
                        new VisualizationEvent(
                                "event-system-ready",
                                "status",
                                "System Ready",
                                "The sandbox is healthy and can start scenario execution.",
                                List.of("spring-app", "publisher", "listener", "kafka"),
                                List.of("publisher-kafka", "listener-kafka"),
                                null,
                                null
                        )
                )
        );
    }
}
