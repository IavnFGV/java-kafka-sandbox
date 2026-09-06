package io.drozda.sandbox.scenario.tradeflow.app;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.drozda.sandbox.scenario.tradeflow.model.TradeFlowEvent;

public class TradeFlowEventTracker {

    private final Map<String, CompletableFuture<TradeFlowEvent>> expectations = new ConcurrentHashMap<>();

    public CompletableFuture<TradeFlowEvent> expect(String eventId) {
        CompletableFuture<TradeFlowEvent> expectation = new CompletableFuture<>();
        expectations.put(eventId, expectation);
        return expectation;
    }

    public void received(TradeFlowEvent event) {
        CompletableFuture<TradeFlowEvent> expectation = expectations.remove(event.eventId());
        if (expectation != null) {
            expectation.complete(event);
        }
    }

    public void discard(String eventId) {
        expectations.remove(eventId);
    }

    public void reset() {
        expectations.clear();
    }
}
