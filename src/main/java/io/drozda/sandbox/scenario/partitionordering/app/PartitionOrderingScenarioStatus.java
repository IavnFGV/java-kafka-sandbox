package io.drozda.sandbox.scenario.partitionordering.app;

import java.util.List;
import java.util.Map;

public record PartitionOrderingScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady,
        boolean published,
        boolean received,
        boolean onePartition,
        boolean increasingOffsets,
        boolean receiveOrderPreserved,
        String topic,
        List<OrderedRecordObservation> observations,
        Map<String, List<Integer>> consumerAssignments,
        String error
) {
}
