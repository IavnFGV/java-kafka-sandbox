package io.drozda.sandbox.scenario.systemready.model;

public record TradeEvent(
    String eventId,
    String tradeId,
    String symbol,
    String type
) {
}