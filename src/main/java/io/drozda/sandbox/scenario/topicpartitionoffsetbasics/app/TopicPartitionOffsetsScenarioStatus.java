package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app;

public record TopicPartitionOffsetsScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady,
        boolean published,
        boolean received,
        String topic,
        Integer firstPartition,
        Long firstOffset,
        Integer secondPartition,
        Long secondOffset,
        Integer thirdPartition,
        Long thirdOffset,
        String error
) {
}
