package io.drozda.sandbox.scenario.messagekeypartitionselection;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.messagekeypartitionselection.app.KeyPartitioningCommandRequest;
import io.drozda.sandbox.scenario.messagekeypartitionselection.app.KeyStrategy;
import io.drozda.sandbox.scenario.messagekeypartitionselection.app.KeyPartitioningScenarioApplication;
import io.drozda.sandbox.scenario.messagekeypartitionselection.app.KeyPartitioningScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class KeyPartitioningEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public KeyPartitioningEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override public String scenarioId() { return "key-partitioning"; }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            String runId = UUID.randomUUID().toString();
            applicationContext = new SpringApplicationBuilder(KeyPartitioningScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.key-partitioning.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=key-partitioning-scenario-app",
                            "--spring.kafka.consumer.group-id=scenario-004-key-" + runId,
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.messagekeypartitionselection.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.messagekeypartitionselection.model.KeyedOrderEvent",
                            "--app.kafka.topics.key-partitioning=scenario-004-key-partitioning-" + runId
                    );
            baseUrl = "http://localhost:" + ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
        }
        return environmentStatus("STARTED", "Started isolated key-partitioning app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }
        return stopped("Stopped isolated key-partitioning app.");
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) return start();
        KeyPartitioningScenarioStatus status = client().post().uri("/internal/key-partitioning/reset")
                .retrieve().body(KeyPartitioningScenarioStatus.class);
        return environmentStatus("RESET", "Reset key-partitioning state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        return applicationContext == null
                ? stopped("Key-partitioning app is not running.")
                : environmentStatus("STARTED", "Key-partitioning app is running.", fetchStatus());
    }

    public synchronized KeyPartitioningScenarioStatus runExperiment(String invocationName, KeyStrategy strategy) {
        if (applicationContext == null) start();
        return client().post()
                .uri("/internal/key-partitioning/commands/{id}", KeyPartitioningScenarioStarter.ROUTE_BY_KEY)
                .body(new KeyPartitioningCommandRequest(invocationName, strategy))
                .retrieve().body(KeyPartitioningScenarioStatus.class);
    }

    private KeyPartitioningScenarioStatus fetchStatus() {
        return client().get().uri("/internal/key-partitioning/status")
                .retrieve().body(KeyPartitioningScenarioStatus.class);
    }

    private RestClient client() { return restClientBuilder.baseUrl(baseUrl).build(); }

    private ScenarioEnvironmentStatus environmentStatus(String lifecycle, String detail,
            KeyPartitioningScenarioStatus status) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady());
    }

    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
