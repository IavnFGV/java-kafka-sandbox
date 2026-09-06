package io.drozda.sandbox.scenario.tradeflow.consumer;

import org.springframework.kafka.annotation.KafkaListener;

import io.drozda.sandbox.scenario.tradeflow.app.TradeFlowEventTracker;
import io.drozda.sandbox.scenario.tradeflow.model.TradeFlowEvent;

public class TradeFlowListener {

    private final TradeFlowEventTracker tracker;

    public TradeFlowListener(TradeFlowEventTracker tracker) {
        this.tracker = tracker;
    }

    @KafkaListener(topics = "${app.kafka.topics.trade-flow}")
    public void onEvent(TradeFlowEvent event) {
        tracker.received(event);
    }
}
