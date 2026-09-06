package io.drozda.sandbox.scenario.keypartitioning.app;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;

public class KeyedEventTracker {
    private final Map<String, CompletableFuture<TrackedKeyedRecord>> expectations =
            new ConcurrentHashMap<>();
    private final Map<String, String> consumerNamesByThread = new ConcurrentHashMap<>();
    private final Map<String, Set<TopicPartition>> assignmentsByConsumer = new ConcurrentHashMap<>();
    private final AtomicInteger consumerSequence = new AtomicInteger();

    public CompletableFuture<TrackedKeyedRecord> expect(String eventId) {
        CompletableFuture<TrackedKeyedRecord> future = new CompletableFuture<>();
        expectations.put(eventId, future);
        return future;
    }

    public void received(ConsumerRecord<String, KeyedOrderEvent> record) {
        String consumerId = consumerId(Thread.currentThread().getName());
        CompletableFuture<TrackedKeyedRecord> future = expectations.remove(record.value().eventId());
        if (future != null) {
            future.complete(new TrackedKeyedRecord(record, consumerId));
        }
    }

    public void assigned(String consumerThread, Set<TopicPartition> assignments) {
        assignmentsByConsumer.put(consumerId(consumerThread), Set.copyOf(assignments));
    }

    public void revoked(String consumerThread, Collection<TopicPartition> revoked) {
        String consumerId = consumerId(consumerThread);
        assignmentsByConsumer.computeIfPresent(consumerId, (ignored, current) -> {
            Set<TopicPartition> remaining = ConcurrentHashMap.newKeySet();
            remaining.addAll(current);
            remaining.removeAll(revoked);
            return Set.copyOf(remaining);
        });
    }

    public void awaitStableAssignments(int consumers, int partitions, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        int stableChecks = 0;
        while (System.nanoTime() < deadline) {
            Map<String, List<Integer>> current = assignments();
            boolean complete = current.size() == consumers
                    && current.values().stream().allMatch(value -> value.size() == 1)
                    && current.values().stream().flatMap(List::stream).distinct().count() == partitions;
            stableChecks = complete ? stableChecks + 1 : 0;
            if (stableChecks >= 3) {
                return;
            }
            sleepBriefly();
        }
        throw new IllegalStateException("Consumer assignments did not stabilize: " + assignments());
    }

    public Map<String, List<Integer>> assignments() {
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        assignmentsByConsumer.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), entry.getValue().stream()
                        .map(TopicPartition::partition)
                        .sorted()
                        .toList()));
        return result;
    }

    public void discard(String eventId) {
        expectations.remove(eventId);
    }

    public void reset() {
        expectations.clear();
    }

    private String consumerId(String threadName) {
        return consumerNamesByThread.computeIfAbsent(threadName,
                ignored -> "Consumer " + (char) ('A' + consumerSequence.getAndIncrement()));
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for consumer assignments", exception);
        }
    }
}
