package io.drozda.sandbox.scenario.multipleconsumergroups.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;

public class MultipleConsumerGroupsTracker {
    private final Object monitor = new Object();
    private Set<String> expectedIds = Set.of();
    private List<TrackedGroupRecord> records = List.of();
    private CompletableFuture<List<TrackedGroupRecord>> completion = new CompletableFuture<>();

    public CompletableFuture<List<TrackedGroupRecord>> expect(List<String> eventIds) {
        synchronized (monitor) {
            expectedIds = Set.copyOf(eventIds);
            records = new ArrayList<>();
            completion = new CompletableFuture<>();
            return completion;
        }
    }

    public void received(ConsumerRecord<String, SharedOrderEvent> record, String groupId) {
        synchronized (monitor) {
            if (!expectedIds.contains(record.value().eventId())) return;
            records.add(new TrackedGroupRecord(record, groupId));
            if (records.size() == expectedIds.size() * 2) completion.complete(List.copyOf(records));
        }
    }

    public void reset() {
        synchronized (monitor) {
            expectedIds = Set.of();
            records = List.of();
            completion = new CompletableFuture<>();
        }
    }
}
