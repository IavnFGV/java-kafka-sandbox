package io.drozda.sandbox.scenario.tradeflow.model;

public record TradeFlowEvent(
        String eventId,
        String tradeId,
        String symbol,
        String type,
        long createdAtEpochMillis
) {
}
