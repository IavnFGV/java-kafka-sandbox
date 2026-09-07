package io.drozda.sandbox.scenario.singlepartitiongroup;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.singlepartitiongroup.app.SinglePartitionGroupCommandRequest;
import io.drozda.sandbox.scenario.singlepartitiongroup.app.SinglePartitionGroupScenarioApplication;
import io.drozda.sandbox.scenario.singlepartitiongroup.app.SinglePartitionGroupScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class SinglePartitionGroupEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public SinglePartitionGroupEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override public String scenarioId() { return "consumer-group-single-partition"; }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            String runId = UUID.randomUUID().toString();
            applicationContext = new SpringApplicationBuilder(SinglePartitionGroupScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.single-partition-group.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=single-partition-group-scenario-app",
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.singlepartitiongroup.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.singlepartitiongroup.model.GroupWorkEvent",
                            "--app.kafka.topics.single-partition-group=scenario-007-single-partition-" + runId,
                            "--app.kafka.groups.single-partition-group=scenario-007-group-" + runId
                    );
            baseUrl = "http://localhost:"
                    + ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
        }
        return environmentStatus("STARTED", "Started isolated single-partition group app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }
        return stopped("Stopped isolated single-partition group app.");
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) return start();
        SinglePartitionGroupScenarioStatus status = client().post()
                .uri("/internal/single-partition-group/reset")
                .retrieve().body(SinglePartitionGroupScenarioStatus.class);
        return environmentStatus("RESET", "Reset single-partition group state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        return applicationContext == null
                ? stopped("Single-partition group app is not running.")
                : environmentStatus("STARTED", "Single-partition group app is running.", fetchStatus());
    }

    public synchronized SinglePartitionGroupScenarioStatus observeTakeover(String invocationName) {
        if (applicationContext == null) start();
        return client().post()
                .uri("/internal/single-partition-group/commands/{id}",
                        SinglePartitionGroupScenarioStarter.OBSERVE_TAKEOVER)
                .body(new SinglePartitionGroupCommandRequest(invocationName))
                .retrieve().body(SinglePartitionGroupScenarioStatus.class);
    }

    private SinglePartitionGroupScenarioStatus fetchStatus() {
        return client().get().uri("/internal/single-partition-group/status")
                .retrieve().body(SinglePartitionGroupScenarioStatus.class);
    }

    private RestClient client() { return restClientBuilder.baseUrl(baseUrl).build(); }

    private ScenarioEnvironmentStatus environmentStatus(
            String lifecycle,
            String detail,
            SinglePartitionGroupScenarioStatus status
    ) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady());
    }

    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
