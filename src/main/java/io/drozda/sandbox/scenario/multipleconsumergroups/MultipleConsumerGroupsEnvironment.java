package io.drozda.sandbox.scenario.multipleconsumergroups;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsCommandRequest;
import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsScenarioApplication;
import io.drozda.sandbox.scenario.multipleconsumergroups.app.MultipleConsumerGroupsScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class MultipleConsumerGroupsEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext context;
    private volatile String baseUrl;

    public MultipleConsumerGroupsEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override public String scenarioId() { return "multiple-consumer-groups"; }

    @Override public synchronized ScenarioEnvironmentStatus start() {
        if (context == null) {
            String runId = UUID.randomUUID().toString();
            context = new SpringApplicationBuilder(MultipleConsumerGroupsScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.multiple-consumer-groups.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=multiple-consumer-groups-scenario-app",
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.multipleconsumergroups.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent",
                            "--app.kafka.topics.multiple-groups=scenario-009-multiple-groups-" + runId,
                            "--app.kafka.groups.audit=scenario-009-audit-" + runId,
                            "--app.kafka.groups.notification=scenario-009-notification-" + runId
                    );
            baseUrl = "http://localhost:" + ((WebServerApplicationContext) context).getWebServer().getPort();
        }
        return toStatus("STARTED", "Started isolated multiple-consumer-groups app.", fetchStatus());
    }

    @Override public synchronized ScenarioEnvironmentStatus stop() {
        if (context != null) { context.close(); context = null; baseUrl = null; }
        return stopped("Stopped isolated multiple-consumer-groups app.");
    }

    @Override public synchronized ScenarioEnvironmentStatus reset() {
        if (context == null) return start();
        MultipleConsumerGroupsScenarioStatus status = client().post()
                .uri("/internal/multiple-consumer-groups/reset")
                .retrieve().body(MultipleConsumerGroupsScenarioStatus.class);
        return toStatus("RESET", "Reset multiple-consumer-groups state.", status);
    }

    @Override public synchronized ScenarioEnvironmentStatus status() {
        return context == null ? stopped("Multiple-consumer-groups app is not running.")
                : toStatus("STARTED", "Multiple-consumer-groups app is running.", fetchStatus());
    }

    public synchronized MultipleConsumerGroupsScenarioStatus observe(String invocationName) {
        if (context == null) start();
        return client().post().uri("/internal/multiple-consumer-groups/commands/{id}",
                        MultipleConsumerGroupsScenarioStarter.OBSERVE_INDEPENDENT_GROUPS)
                .body(new MultipleConsumerGroupsCommandRequest(invocationName))
                .retrieve().body(MultipleConsumerGroupsScenarioStatus.class);
    }

    private MultipleConsumerGroupsScenarioStatus fetchStatus() {
        return client().get().uri("/internal/multiple-consumer-groups/status")
                .retrieve().body(MultipleConsumerGroupsScenarioStatus.class);
    }
    private RestClient client() { return restClientBuilder.baseUrl(baseUrl).build(); }
    private ScenarioEnvironmentStatus toStatus(
            String lifecycle, String detail, MultipleConsumerGroupsScenarioStatus status) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady());
    }
    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
