package io.drozda.sandbox.scenario.earliestvslatest.app;

import java.util.List;

public record EarliestVsLatestScenarioStatus(
        String scenarioId, String invocationName,
        boolean publisherReady, boolean listenerReady, boolean kafkaTemplateReady,
        boolean published, boolean verified,
        List<OffsetResetObservation> observations, String topic, String error) {
}
