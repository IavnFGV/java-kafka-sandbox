package io.drozda.sandbox.scenario.orderingwithinonepartition;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.orderingwithinonepartition.app.PartitionOrderingCommandRequest;
import io.drozda.sandbox.scenario.orderingwithinonepartition.app.PartitionOrderingScenarioApplication;
import io.drozda.sandbox.scenario.orderingwithinonepartition.app.PartitionOrderingScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class PartitionOrderingEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public PartitionOrderingEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override public String scenarioId() { return "partition-ordering"; }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            String runId = UUID.randomUUID().toString();
            applicationContext = new SpringApplicationBuilder(PartitionOrderingScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.partition-ordering.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=partition-ordering-scenario-app",
                            "--spring.kafka.consumer.group-id=scenario-005-ordering-" + runId,
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.orderingwithinonepartition.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.orderingwithinonepartition.model.OrderedOrderEvent",
                            "--app.kafka.topics.partition-ordering=scenario-005-partition-ordering-" + runId
                    );
            baseUrl = "http://localhost:"
                    + ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
        }
        return environmentStatus("STARTED", "Started isolated partition-ordering app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }
        return stopped("Stopped isolated partition-ordering app.");
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) return start();
        PartitionOrderingScenarioStatus status = client().post().uri("/internal/partition-ordering/reset")
                .retrieve().body(PartitionOrderingScenarioStatus.class);
        return environmentStatus("RESET", "Reset partition-ordering state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        return applicationContext == null
                ? stopped("Partition-ordering app is not running.")
                : environmentStatus("STARTED", "Partition-ordering app is running.", fetchStatus());
    }

    public synchronized PartitionOrderingScenarioStatus verifyOrder(String invocationName) {
        if (applicationContext == null) start();
        return client().post()
                .uri("/internal/partition-ordering/commands/{id}", PartitionOrderingScenarioStarter.VERIFY_ORDER)
                .body(new PartitionOrderingCommandRequest(invocationName))
                .retrieve().body(PartitionOrderingScenarioStatus.class);
    }

    private PartitionOrderingScenarioStatus fetchStatus() {
        return client().get().uri("/internal/partition-ordering/status")
                .retrieve().body(PartitionOrderingScenarioStatus.class);
    }

    private RestClient client() { return restClientBuilder.baseUrl(baseUrl).build(); }

    private ScenarioEnvironmentStatus environmentStatus(
            String lifecycle, String detail, PartitionOrderingScenarioStatus status
    ) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady());
    }

    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
