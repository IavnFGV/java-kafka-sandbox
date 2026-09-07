package io.drozda.sandbox.mediator;

public record ScenarioEnvironmentStatus(
        String scenarioId,
        String lifecycleState,
        String detail,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady
) {
}
