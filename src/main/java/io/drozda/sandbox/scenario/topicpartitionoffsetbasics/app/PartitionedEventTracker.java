package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.model.PartitionedEvent;

public class PartitionedEventTracker {

    private final Map<String, CompletableFuture<ConsumerRecord<String, PartitionedEvent>>> expectations =
            new ConcurrentHashMap<>();

    public CompletableFuture<ConsumerRecord<String, PartitionedEvent>> expect(String eventId) {
        CompletableFuture<ConsumerRecord<String, PartitionedEvent>> expectation = new CompletableFuture<>();
        expectations.put(eventId, expectation);
        return expectation;
    }

    public void received(ConsumerRecord<String, PartitionedEvent> record) {
        CompletableFuture<ConsumerRecord<String, PartitionedEvent>> expectation =
                expectations.remove(record.value().eventId());
        if (expectation != null) {
            expectation.complete(record);
        }
    }

    public void discard(String eventId) {
        expectations.remove(eventId);
    }

    public void reset() {
        expectations.clear();
    }
}
