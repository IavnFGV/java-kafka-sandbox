package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent;

public class ParallelGroupTracker {
    private final Object monitor = new Object();
    private final Map<String, Set<TopicPartition>> assignments = new ConcurrentHashMap<>();
    private Set<String> expectedIds = Set.of();
    private List<TrackedParallelGroupRecord> received = List.of();
    private CompletableFuture<List<TrackedParallelGroupRecord>> completion = new CompletableFuture<>();

    public CompletableFuture<List<TrackedParallelGroupRecord>> expect(List<String> eventIds) {
        synchronized (monitor) {
            expectedIds = Set.copyOf(eventIds);
            received = new ArrayList<>();
            completion = new CompletableFuture<>();
            return completion;
        }
    }

    public void received(ConsumerRecord<String, ParallelGroupEvent> record, String consumerId) {
        synchronized (monitor) {
            if (!expectedIds.contains(record.value().eventId())) return;
            received.add(new TrackedParallelGroupRecord(record, consumerId));
            if (received.size() == expectedIds.size()) completion.complete(List.copyOf(received));
        }
    }

    public void assigned(String consumerId, Set<TopicPartition> partitions) {
        assignments.put(consumerId, Set.copyOf(partitions));
    }

    public void revoked(String consumerId, Collection<TopicPartition> partitions) {
        assignments.computeIfPresent(consumerId, (ignored, current) -> current.stream()
                .filter(partition -> !partitions.contains(partition)).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    public Map<Integer, String> awaitSplitAssignment(String topic, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Map<Integer, String> owners = new LinkedHashMap<>();
            assignments.forEach((consumer, partitions) -> partitions.stream()
                    .filter(partition -> partition.topic().equals(topic))
                    .forEach(partition -> owners.put(partition.partition(), consumer)));
            if (owners.size() == 2 && owners.containsKey(0) && owners.containsKey(1)
                    && !owners.get(0).equals(owners.get(1))) return Map.copyOf(owners);
            sleep(100);
        }
        throw new IllegalStateException("Two-partition assignment did not stabilize for " + topic);
    }

    public void reset() {
        synchronized (monitor) {
            expectedIds = Set.of();
            received = List.of();
            completion = new CompletableFuture<>();
        }
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
