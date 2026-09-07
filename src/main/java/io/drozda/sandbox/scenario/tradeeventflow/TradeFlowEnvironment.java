package io.drozda.sandbox.scenario.tradeeventflow;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;
import io.drozda.sandbox.scenario.tradeeventflow.app.TradeFlowScenarioApplication;
import io.drozda.sandbox.scenario.tradeeventflow.app.TradeFlowScenarioCommandRequest;
import io.drozda.sandbox.scenario.tradeeventflow.app.TradeFlowScenarioStatus;

@Component
public class TradeFlowEnvironment implements ScenarioEnvironment {

    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public TradeFlowEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public String scenarioId() {
        return "trade-flow";
    }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            String runId = UUID.randomUUID().toString();
            String groupId = "scenario-002-trade-flow-" + runId;
            applicationContext = new SpringApplicationBuilder(TradeFlowScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.trade-flow.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=trade-flow-scenario-app",
                            "--spring.kafka.consumer.group-id=" + groupId,
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.tradeeventflow.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.tradeeventflow.model.TradeFlowEvent",
                            "--app.kafka.topics.trade-flow=scenario-002-trade-events-" + runId
                    );
            WebServerApplicationContext webContext = (WebServerApplicationContext) applicationContext;
            baseUrl = "http://localhost:" + webContext.getWebServer().getPort();
        }

        return toEnvironmentStatus("STARTED", "Started isolated trade-flow scenario app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }

        return stoppedStatus("Stopped isolated trade-flow scenario app.");
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) {
            return start();
        }

        TradeFlowScenarioStatus status = client().post()
                .uri("/internal/trade-flow/reset")
                .retrieve()
                .body(TradeFlowScenarioStatus.class);
        return toEnvironmentStatus("RESET", "Reset trade-flow scenario state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        if (applicationContext == null) {
            return stoppedStatus("Trade-flow scenario app is not running.");
        }
        return toEnvironmentStatus("STARTED", "Trade-flow scenario app is running.", fetchStatus());
    }

    public synchronized TradeFlowScenarioStatus sendAndReceive(String invocationName) {
        if (applicationContext == null) {
            start();
        }

        return client().post()
                .uri("/internal/trade-flow/commands/{commandId}", TradeFlowScenarioStarter.SEND_AND_RECEIVE)
                .body(new TradeFlowScenarioCommandRequest(invocationName))
                .retrieve()
                .body(TradeFlowScenarioStatus.class);
    }

    private TradeFlowScenarioStatus fetchStatus() {
        return client().get()
                .uri("/internal/trade-flow/status")
                .retrieve()
                .body(TradeFlowScenarioStatus.class);
    }

    private RestClient client() {
        return restClientBuilder.baseUrl(baseUrl).build();
    }

    private ScenarioEnvironmentStatus toEnvironmentStatus(
            String lifecycleState,
            String detail,
            TradeFlowScenarioStatus status
    ) {
        return new ScenarioEnvironmentStatus(
                scenarioId(), lifecycleState, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady()
        );
    }

    private ScenarioEnvironmentStatus stoppedStatus(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
