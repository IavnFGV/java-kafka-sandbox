package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.model.GlobalOrderEvent;

public class GlobalOrderTracker {
    private final Object monitor = new Object();
    private final Map<String, Set<TopicPartition>> assignmentsByThread = new ConcurrentHashMap<>();
    private Set<String> expectedEventIds = Set.of();
    private List<TrackedGlobalOrderRecord> completedRecords = List.of();
    private long startedAt;
    private CompletableFuture<List<TrackedGlobalOrderRecord>> completion = new CompletableFuture<>();

    public CompletableFuture<List<TrackedGlobalOrderRecord>> expect(List<String> eventIds) {
        synchronized (monitor) {
            expectedEventIds = Set.copyOf(eventIds);
            completedRecords = new ArrayList<>();
            startedAt = System.nanoTime();
            completion = new CompletableFuture<>();
            return completion;
        }
    }

    public void process(ConsumerRecord<String, GlobalOrderEvent> record, String consumerId) {
        synchronized (monitor) {
            if (!expectedEventIds.contains(record.value().eventId())) {
                return;
            }
        }

        sleep(record.value().processingDelayMs());

        synchronized (monitor) {
            completedRecords.add(new TrackedGlobalOrderRecord(
                    record,
                    consumerId,
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
            ));
            if (completedRecords.size() == expectedEventIds.size()) {
                completion.complete(List.copyOf(completedRecords));
            }
        }
    }

    public void assigned(String threadName, Set<TopicPartition> assignments) {
        assignmentsByThread.put(threadName, Set.copyOf(assignments));
    }

    public void revoked(String threadName, Collection<TopicPartition> revoked) {
        assignmentsByThread.computeIfPresent(threadName, (ignored, current) -> {
            Set<TopicPartition> remaining = ConcurrentHashMap.newKeySet();
            remaining.addAll(current);
            remaining.removeAll(revoked);
            return Set.copyOf(remaining);
        });
    }

    public void awaitAssignments(String topic, int expectedPartitions, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            long assigned = assignmentsByThread.values().stream()
                    .flatMap(Set::stream)
                    .filter(partition -> partition.topic().equals(topic))
                    .map(TopicPartition::partition)
                    .distinct()
                    .count();
            if (assigned == expectedPartitions) {
                return;
            }
            sleep(100);
        }
        throw new IllegalStateException("Assignments did not stabilize for " + topic);
    }

    public void reset() {
        synchronized (monitor) {
            expectedEventIds = Set.of();
            completedRecords = List.of();
            completion = new CompletableFuture<>();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Global ordering processing interrupted", exception);
        }
    }
}
