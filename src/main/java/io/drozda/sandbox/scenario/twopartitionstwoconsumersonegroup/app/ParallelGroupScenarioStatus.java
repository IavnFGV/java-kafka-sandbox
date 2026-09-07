package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

import java.util.List;
import java.util.Map;

public record ParallelGroupScenarioStatus(
        String scenarioId, String invocationName,
        boolean publisherReady, boolean listenerReady, boolean kafkaTemplateReady,
        boolean published, boolean received,
        Map<Integer, String> partitionOwners,
        List<ParallelGroupObservation> observations,
        String topic, String error
) {
}
