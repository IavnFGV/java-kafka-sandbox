package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app;

import java.util.List;

public record GlobalOrderingScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady,
        boolean published,
        boolean received,
        GlobalOrderingMode mode,
        boolean perOrderSequencePreserved,
        boolean globalCompletionMatchesPublishOrder,
        long fastOrderCompletedMs,
        long slowOrderCompletedMs,
        String topic,
        List<GlobalOrderObservation> observations,
        String error
) {
}
