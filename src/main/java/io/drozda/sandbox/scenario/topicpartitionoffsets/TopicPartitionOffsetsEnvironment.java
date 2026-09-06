package io.drozda.sandbox.scenario.topicpartitionoffsets;

import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;
import io.drozda.sandbox.scenario.topicpartitionoffsets.app.TopicPartitionOffsetsCommandRequest;
import io.drozda.sandbox.scenario.topicpartitionoffsets.app.TopicPartitionOffsetsScenarioApplication;
import io.drozda.sandbox.scenario.topicpartitionoffsets.app.TopicPartitionOffsetsScenarioStatus;

@Component
public class TopicPartitionOffsetsEnvironment implements ScenarioEnvironment {

    private static final String TOPIC = "scenario-003-partition-offsets";

    private final RestClient.Builder restClientBuilder;
    private volatile ConfigurableApplicationContext applicationContext;
    private volatile String baseUrl;

    public TopicPartitionOffsetsEnvironment(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public String scenarioId() {
        return "topic-partition-offsets";
    }

    @Override
    public synchronized ScenarioEnvironmentStatus start() {
        if (applicationContext == null) {
            String groupId = "scenario-003-partition-offsets-" + UUID.randomUUID();
            applicationContext = new SpringApplicationBuilder(TopicPartitionOffsetsScenarioApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--scenario.topic-partition-offsets.enabled=true",
                            "--server.port=0",
                            "--spring.application.name=topic-partition-offsets-scenario-app",
                            "--spring.kafka.consumer.group-id=" + groupId,
                            "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.topicpartitionoffsets.model",
                            "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.topicpartitionoffsets.model.PartitionedEvent",
                            "--app.kafka.topics.partition-offsets=" + TOPIC
                    );
            WebServerApplicationContext webContext = (WebServerApplicationContext) applicationContext;
            baseUrl = "http://localhost:" + webContext.getWebServer().getPort();
        }

        return toEnvironmentStatus("STARTED", "Started isolated topic-partition-offsets app.", fetchStatus());
    }

    @Override
    public synchronized ScenarioEnvironmentStatus stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
            baseUrl = null;
        }
        return stoppedStatus("Stopped isolated topic-partition-offsets app.");
    }

    @Override
    public synchronized ScenarioEnvironmentStatus reset() {
        if (applicationContext == null) {
            return start();
        }
        TopicPartitionOffsetsScenarioStatus status = client().post()
                .uri("/internal/topic-partition-offsets/reset")
                .retrieve()
                .body(TopicPartitionOffsetsScenarioStatus.class);
        return toEnvironmentStatus("RESET", "Reset topic-partition-offsets state.", status);
    }

    @Override
    public synchronized ScenarioEnvironmentStatus status() {
        if (applicationContext == null) {
            return stoppedStatus("Topic-partition-offsets app is not running.");
        }
        return toEnvironmentStatus("STARTED", "Topic-partition-offsets app is running.", fetchStatus());
    }

    public synchronized TopicPartitionOffsetsScenarioStatus appendAndRead(String invocationName) {
        if (applicationContext == null) {
            start();
        }
        return client().post()
                .uri("/internal/topic-partition-offsets/commands/{commandId}",
                        TopicPartitionOffsetsScenarioStarter.APPEND_AND_READ)
                .body(new TopicPartitionOffsetsCommandRequest(invocationName))
                .retrieve()
                .body(TopicPartitionOffsetsScenarioStatus.class);
    }

    private TopicPartitionOffsetsScenarioStatus fetchStatus() {
        return client().get()
                .uri("/internal/topic-partition-offsets/status")
                .retrieve()
                .body(TopicPartitionOffsetsScenarioStatus.class);
    }

    private RestClient client() {
        return restClientBuilder.baseUrl(baseUrl).build();
    }

    private ScenarioEnvironmentStatus toEnvironmentStatus(
            String lifecycleState,
            String detail,
            TopicPartitionOffsetsScenarioStatus status
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
