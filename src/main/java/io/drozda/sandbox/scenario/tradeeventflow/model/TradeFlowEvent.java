package io.drozda.sandbox.scenario.tradeeventflow.model;

public record TradeFlowEvent(
        String eventId,
        String tradeId,
        String symbol,
        String type,
        long createdAtEpochMillis
) {
}
