package io.drozda.sandbox.scenario.tradeflow.app;

public record TradeFlowScenarioStatus(
        String scenarioId,
        String invocationName,
        boolean publisherReady,
        boolean listenerReady,
        boolean kafkaTemplateReady,
        boolean published,
        boolean received,
        String eventId,
        String topic,
        Integer partition,
        Long offset,
        String error
) {
}
