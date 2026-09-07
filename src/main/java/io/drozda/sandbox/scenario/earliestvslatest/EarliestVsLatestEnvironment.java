package io.drozda.sandbox.scenario.earliestvslatest;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.earliestvslatest.app.EarliestVsLatestCommandRequest;
import io.drozda.sandbox.scenario.earliestvslatest.app.EarliestVsLatestScenarioApplication;
import io.drozda.sandbox.scenario.earliestvslatest.app.EarliestVsLatestScenarioStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class EarliestVsLatestEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder builder;
    private volatile ConfigurableApplicationContext context;
    private volatile String baseUrl;
    public EarliestVsLatestEnvironment(RestClient.Builder builder) { this.builder = builder; }
    @Override public String scenarioId() { return "earliest-vs-latest"; }
    @Override public synchronized ScenarioEnvironmentStatus start() {
        if (context == null) {
            String id = UUID.randomUUID().toString();
            context = new SpringApplicationBuilder(EarliestVsLatestScenarioApplication.class)
                    .web(WebApplicationType.SERVLET).run(
                            "--scenario.earliest-vs-latest.enabled=true", "--server.port=0",
                            "--spring.application.name=earliest-vs-latest-scenario-app",
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.earliestvslatest.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.earliestvslatest.model.OffsetResetEvent",
                            "--app.kafka.topics.offset-reset=scenario-010-offset-reset-" + id,
                            "--app.kafka.groups.earliest=scenario-010-earliest-" + id,
                            "--app.kafka.groups.latest=scenario-010-latest-" + id);
            baseUrl = "http://localhost:" + ((WebServerApplicationContext) context).getWebServer().getPort();
        }
        return status("STARTED", "Started isolated earliest-vs-latest app.", fetch());
    }
    @Override public synchronized ScenarioEnvironmentStatus stop() {
        if (context != null) { context.close(); context = null; baseUrl = null; }
        return stopped("Stopped isolated earliest-vs-latest app.");
    }
    @Override public synchronized ScenarioEnvironmentStatus reset() {
        if (context == null) return start();
        return status("RESET", "Reset offset observations.", client().post()
                .uri("/internal/earliest-vs-latest/reset").retrieve().body(EarliestVsLatestScenarioStatus.class));
    }
    @Override public synchronized ScenarioEnvironmentStatus status() {
        return context == null ? stopped("Earliest-vs-latest app is not running.")
                : status("STARTED", "Earliest-vs-latest app is running.", fetch());
    }
    public synchronized EarliestVsLatestScenarioStatus compare(String name) {
        if (context != null) stop();
        start();
        return client().post().uri("/internal/earliest-vs-latest/commands/{id}",
                        EarliestVsLatestScenarioStarter.COMPARE_RESET_POLICIES)
                .body(new EarliestVsLatestCommandRequest(name)).retrieve().body(EarliestVsLatestScenarioStatus.class);
    }
    private EarliestVsLatestScenarioStatus fetch() { return client().get().uri("/internal/earliest-vs-latest/status")
            .retrieve().body(EarliestVsLatestScenarioStatus.class); }
    private RestClient client() { return builder.baseUrl(baseUrl).build(); }
    private ScenarioEnvironmentStatus status(String lifecycle, String detail, EarliestVsLatestScenarioStatus value) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                value.publisherReady(), value.listenerReady(), value.kafkaTemplateReady());
    }
    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
