package io.drozda.sandbox.scenario.systemready;

import java.util.Map;
import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;
import io.drozda.sandbox.scenario.systemready.app.SystemReadyScenarioApplication;
import io.drozda.sandbox.scenario.systemready.app.SystemReadyScenarioCommandRequest;
import io.drozda.sandbox.scenario.systemready.app.SystemReadyScenarioStatus;

@Component
public class SystemReadyEnvironment implements ScenarioEnvironment {

    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public SystemReadyEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public String scenarioId() {
        return "system-ready";
    }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            applicationContext = new SpringApplicationBuilder(SystemReadyScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .properties(Map.of(
                            "server.port", "0",
                            "spring.application.name", "system-ready-scenario-app",
                            "spring.kafka.consumer.group-id", "system-ready-scenario-" + UUID.randomUUID()
                    ))
                    .run();
            WebServerApplicationContext webContext = (WebServerApplicationContext) applicationContext;
            baseUrl = "http://localhost:" + webContext.getWebServer().getPort();
        }

        return toEnvironmentStatus("STARTED", "Started system-ready scenario app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }

        return new ScenarioEnvironmentStatus(
                scenarioId(),
                "STOPPED",
                "Stopped system-ready scenario app.",
                false,
                false,
                false
        );
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) {
            return start();
        }

        SystemReadyScenarioStatus status = client().post()
                .uri("/internal/system-ready/reset")
                .retrieve()
                .body(SystemReadyScenarioStatus.class);

        return toEnvironmentStatus("RESET", "Reset system-ready scenario app state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        if (applicationContext == null) {
            return new ScenarioEnvironmentStatus(
                    scenarioId(),
                    "STOPPED",
                    "System-ready scenario app is not running.",
                    false,
                    false,
                    false
            );
        }

        return toEnvironmentStatus("STARTED", "System-ready scenario app is running.", fetchStatus());
    }

    public synchronized SystemReadyScenarioStatus baselineReadiness(String invocationName) {
        if (applicationContext == null) {
            start();
        }

        return client().post()
                .uri("/internal/system-ready/commands/{commandId}", SystemReadyScenarioStarter.BASELINE_READINESS)
                .body(new SystemReadyScenarioCommandRequest(invocationName))
                .retrieve()
                .body(SystemReadyScenarioStatus.class);
    }

    private SystemReadyScenarioStatus fetchStatus() {
        return client().get()
                .uri("/internal/system-ready/status")
                .retrieve()
                .body(SystemReadyScenarioStatus.class);
    }

    private RestClient client() {
        return restClientBuilder.baseUrl(baseUrl).build();
    }

    private ScenarioEnvironmentStatus toEnvironmentStatus(
            String lifecycleState,
            String detail,
            SystemReadyScenarioStatus status
    ) {
        return new ScenarioEnvironmentStatus(
                scenarioId(),
                lifecycleState,
                detail,
                status.publisherReady(),
                status.listenerReady(),
                status.kafkaTemplateReady()
        );
    }
}
