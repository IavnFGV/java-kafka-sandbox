package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupCommandRequest;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupScenarioApplication;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app.ParallelGroupScenarioStatus;

@Component
public class ParallelGroupEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext context;
    private volatile String baseUrl;

    public ParallelGroupEnvironment(RestClient.Builder restClientBuilder) { this.restClientBuilder = restClientBuilder; }
    @Override public String scenarioId() { return "consumer-group-two-partitions"; }

    @Override public synchronized ScenarioEnvironmentStatus start() {
        if (context == null) {
            String runId = UUID.randomUUID().toString();
            context = new SpringApplicationBuilder(ParallelGroupScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.parallel-group.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=parallel-group-scenario-app",
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent",
                            "--app.kafka.topics.parallel-group=scenario-008-parallel-group-" + runId,
                            "--app.kafka.groups.parallel-group=scenario-008-group-" + runId
                    );
            baseUrl = "http://localhost:" + ((WebServerApplicationContext) context).getWebServer().getPort();
        }
        return toStatus("STARTED", "Started isolated two-partition group app.", fetchStatus());
    }

    @Override public synchronized ScenarioEnvironmentStatus stop() {
        if (context != null) { context.close(); context = null; baseUrl = null; }
        return stopped("Stopped isolated two-partition group app.");
    }

    @Override public synchronized ScenarioEnvironmentStatus reset() {
        if (context == null) return start();
        ParallelGroupScenarioStatus status = client().post().uri("/internal/parallel-group/reset")
                .retrieve().body(ParallelGroupScenarioStatus.class);
        return toStatus("RESET", "Reset two-partition group state.", status);
    }

    @Override public synchronized ScenarioEnvironmentStatus status() {
        return context == null ? stopped("Two-partition group app is not running.")
                : toStatus("STARTED", "Two-partition group app is running.", fetchStatus());
    }

    public synchronized ParallelGroupScenarioStatus observe(String invocationName) {
        if (context == null) start();
        return client().post().uri("/internal/parallel-group/commands/{id}",
                        ParallelGroupScenarioStarter.OBSERVE_PARALLEL_ASSIGNMENT)
                .body(new ParallelGroupCommandRequest(invocationName))
                .retrieve().body(ParallelGroupScenarioStatus.class);
    }

    private ParallelGroupScenarioStatus fetchStatus() {
        return client().get().uri("/internal/parallel-group/status")
                .retrieve().body(ParallelGroupScenarioStatus.class);
    }
    private RestClient client() { return restClientBuilder.baseUrl(baseUrl).build(); }
    private ScenarioEnvironmentStatus toStatus(String lifecycle, String detail, ParallelGroupScenarioStatus status) {
        return new ScenarioEnvironmentStatus(scenarioId(), lifecycle, detail,
                status.publisherReady(), status.listenerReady(), status.kafkaTemplateReady());
    }
    private ScenarioEnvironmentStatus stopped(String detail) {
        return new ScenarioEnvironmentStatus(scenarioId(), "STOPPED", detail, false, false, false);
    }
}
