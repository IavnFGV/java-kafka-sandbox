package io.drozda.sandbox.scenario.multipleconsumergroups.app;

import java.util.List;

public record MultipleConsumerGroupsScenarioStatus(
        String scenarioId, String invocationName,
        boolean publisherReady, boolean listenerReady, boolean kafkaTemplateReady,
        boolean published, boolean receivedByBothGroups,
        List<ConsumerGroupObservation> observations,
        String topic, String error
) {
}
