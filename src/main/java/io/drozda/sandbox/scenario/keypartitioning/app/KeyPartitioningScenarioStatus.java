package io.drozda.sandbox.scenario.keypartitioning.app;

import java.util.List;
import java.util.Map;

public record KeyPartitioningScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady,
        boolean published,
        boolean received,
        KeyStrategy keyStrategy,
        boolean learningGoalMet,
        String topic,
        List<KeyedRecordObservation> observations,
        Map<String, List<Integer>> consumerAssignments,
        String error
) {
}
