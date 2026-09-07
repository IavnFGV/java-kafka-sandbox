package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.model.GroupWorkEvent;

public class SinglePartitionGroupTracker {
    private final Object monitor = new Object();
    private final Map<String, Set<TopicPartition>> assignments = new ConcurrentHashMap<>();
    private Set<String> expectedEventIds = Set.of();
    private List<TrackedGroupWorkRecord> received = List.of();
    private CompletableFuture<List<TrackedGroupWorkRecord>> completion = new CompletableFuture<>();

    public CompletableFuture<List<TrackedGroupWorkRecord>> expect(List<String> eventIds) {
        synchronized (monitor) {
            expectedEventIds = Set.copyOf(eventIds);
            received = new ArrayList<>();
            completion = new CompletableFuture<>();
            return completion;
        }
    }

    public void received(ConsumerRecord<String, GroupWorkEvent> record, String consumerId) {
        synchronized (monitor) {
            if (!expectedEventIds.contains(record.value().eventId())) return;
            received.add(new TrackedGroupWorkRecord(record, consumerId));
            if (received.size() == expectedEventIds.size()) {
                completion.complete(List.copyOf(received));
            }
        }
    }

    public void assigned(String consumerId, Set<TopicPartition> partitions) {
        assignments.put(consumerId, Set.copyOf(partitions));
    }

    public void revoked(String consumerId) {
        assignments.put(consumerId, Set.of());
    }

    public String awaitSingleOwner(String topic, String previousOwner, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            List<String> owners = assignments.entrySet().stream()
                    .filter(entry -> entry.getValue().stream()
                            .anyMatch(partition -> partition.topic().equals(topic) && partition.partition() == 0))
                    .map(Map.Entry::getKey)
                    .toList();
            if (owners.size() == 1 && (previousOwner == null || !owners.get(0).equals(previousOwner))) {
                return owners.get(0);
            }
            sleep(100);
        }
        throw new IllegalStateException("One stable partition owner was not observed for " + topic);
    }

    public void resetEvents() {
        synchronized (monitor) {
            expectedEventIds = Set.of();
            received = List.of();
            completion = new CompletableFuture<>();
        }
    }

    public void resetAssignments() {
        assignments.clear();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Assignment observation interrupted", exception);
        }
    }
}
