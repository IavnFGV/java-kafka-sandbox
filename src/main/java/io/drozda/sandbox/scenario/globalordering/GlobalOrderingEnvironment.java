package io.drozda.sandbox.scenario.globalordering;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.globalordering.app.GlobalOrderingCommandRequest;
import io.drozda.sandbox.scenario.globalordering.app.GlobalOrderingMode;
import io.drozda.sandbox.scenario.globalordering.app.GlobalOrderingScenarioApplication;
import io.drozda.sandbox.scenario.globalordering.app.GlobalOrderingScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class GlobalOrderingEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public GlobalOrderingEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override public String scenarioId() { return "global-ordering"; }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            String runId = UUID.randomUUID().toString();
            applicationContext = new SpringApplicationBuilder(GlobalOrderingScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.global-ordering.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=global-ordering-scenario-app",
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.globalordering.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.globalordering.model.GlobalOrderEvent",
                            "--app.kafka.topics.global-ordering-single=scenario-006-single-" + runId,
                            "--app.kafka.topics.global-ordering-parallel=scenario-006-parallel-" + runId,
                            "--app.kafka.groups.global-ordering-single=scenario-006-single-group-" + runId,
                            "--app.kafka.groups.global-ordering-parallel=scenario-006-parallel-group-" + runId
                    );
            baseUrl = "http://localhost:"
                    + ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
        }
        return environmentStatus("STARTED", "Started isolated global-ordering app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }
        return stopped("Stopped isolated global-ordering app.");
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) return start();
        GlobalOrderingScenarioStatus status = client().post().uri("/internal/global-ordering/reset")
                .retrieve().body(GlobalOrderingScenarioStatus.class);
        return environmentStatus("RESET", "Reset global-ordering state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        return applicationContext == null
                ? stopped("Global-ordering app is not running.")
                : environmentStatus("STARTED", "Global-ordering app is running.", fetchStatus());
    }

    public synchronized GlobalOrderingScenarioStatus compare(
            String invocationName,
            GlobalOrderingMode mode
    ) {
        if (applicationContext == null) start();
        return client().post()
                .uri("/internal/global-ordering/commands/{id}", GlobalOrderingScenarioStarter.COMPARE_TOPOLOGY)
                .body(new GlobalOrderingCommandRequest(invocationName, mode))
                .retrieve().body(GlobalOrderingScenarioStatus.class);
    }

    private GlobalOrderingScenarioStatus fetchStatus() {
        return client().get().uri("/internal/global-ordering/status")
                .retrieve().body(GlobalOrderingScenarioStatus.class);
    }

    private RestClient client() { return restClientBuilder.baseUrl(baseUrl).build(); }

    private ScenarioEnvironmentStatus environmentStatus(
            String lifecycle,
            String detail,
            GlobalOrderingScenarioStatus status
    ) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady());
    }

    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
