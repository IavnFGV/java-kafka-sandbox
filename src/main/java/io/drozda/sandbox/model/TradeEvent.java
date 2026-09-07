package io.drozda.sandbox.model;

public record TradeEvent(
    String eventId,
    String tradeId,
    String symbol,
    String type
) {
}