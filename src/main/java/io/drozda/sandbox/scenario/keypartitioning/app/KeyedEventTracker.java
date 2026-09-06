package io.drozda.sandbox.scenario.keypartitioning.app;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;

public class KeyedEventTracker {
    private final Map<String, CompletableFuture<ConsumerRecord<String, KeyedOrderEvent>>> expectations =
            new ConcurrentHashMap<>();

    public CompletableFuture<ConsumerRecord<String, KeyedOrderEvent>> expect(String eventId) {
        CompletableFuture<ConsumerRecord<String, KeyedOrderEvent>> future = new CompletableFuture<>();
        expectations.put(eventId, future);
        return future;
    }

    public void received(ConsumerRecord<String, KeyedOrderEvent> record) {
        CompletableFuture<ConsumerRecord<String, KeyedOrderEvent>> future = expectations.remove(record.value().eventId());
        if (future != null) {
            future.complete(record);
        }
    }

    public void discard(String eventId) {
        expectations.remove(eventId);
    }

    public void reset() {
        expectations.clear();
    }
}
