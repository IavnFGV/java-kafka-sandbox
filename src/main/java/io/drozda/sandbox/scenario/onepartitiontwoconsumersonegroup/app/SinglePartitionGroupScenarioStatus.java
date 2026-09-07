package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app;

import java.util.List;

public record SinglePartitionGroupScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady,
        boolean published,
        boolean received,
        String initialOwner,
        String initialIdleConsumer,
        String takeoverOwner,
        List<GroupWorkObservation> observations,
        String topic,
        String error
) {
}
