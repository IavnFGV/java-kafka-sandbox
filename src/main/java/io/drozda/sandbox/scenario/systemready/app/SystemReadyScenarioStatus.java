package io.drozda.sandbox.scenario.systemready.app;

public record SystemReadyScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady
) {
}
