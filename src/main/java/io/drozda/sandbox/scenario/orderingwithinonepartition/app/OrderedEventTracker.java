package io.drozda.sandbox.scenario.orderingwithinonepartition.app;

import java.time.Duration;
import java.util.ArrayList;
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

import io.drozda.sandbox.scenario.orderingwithinonepartition.model.OrderedOrderEvent;

public class OrderedEventTracker {
    private final Map<String, String> consumerNamesByThread = new ConcurrentHashMap<>();
    private final Map<String, Set<TopicPartition>> assignmentsByConsumer = new ConcurrentHashMap<>();
    private final AtomicInteger consumerSequence = new AtomicInteger();
    private final Object monitor = new Object();
    private Set<String> expectedEventIds = Set.of();
    private List<TrackedOrderedRecord> receivedInCallbackOrder = List.of();
    private CompletableFuture<List<TrackedOrderedRecord>> completion = new CompletableFuture<>();

    public CompletableFuture<List<TrackedOrderedRecord>> expect(List<String> eventIds) {
        synchronized (monitor) {
            expectedEventIds = Set.copyOf(eventIds);
            receivedInCallbackOrder = new ArrayList<>();
            completion = new CompletableFuture<>();
            return completion;
        }
    }

    public void received(ConsumerRecord<String, OrderedOrderEvent> record) {
        synchronized (monitor) {
            if (!expectedEventIds.contains(record.value().eventId())) {
                return;
            }
            receivedInCallbackOrder.add(new TrackedOrderedRecord(
                    record, consumerId(Thread.currentThread().getName())));
            if (receivedInCallbackOrder.size() == expectedEventIds.size()) {
                completion.complete(List.copyOf(receivedInCallbackOrder));
            }
        }
    }

    public void assigned(String consumerThread, Set<TopicPartition> assignments) {
        assignmentsByConsumer.put(consumerId(consumerThread), Set.copyOf(assignments));
    }

    public void revoked(String consumerThread, Collection<TopicPartition> revoked) {
        assignmentsByConsumer.computeIfPresent(consumerId(consumerThread), (ignored, current) -> {
            Set<TopicPartition> remaining = ConcurrentHashMap.newKeySet();
            remaining.addAll(current);
            remaining.removeAll(revoked);
            return Set.copyOf(remaining);
        });
    }

    public void awaitStableAssignments(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        int stableChecks = 0;
        while (System.nanoTime() < deadline) {
            Map<String, List<Integer>> current = assignments();
            boolean complete = current.size() == 3
                    && current.values().stream().allMatch(partitions -> partitions.size() == 1)
                    && current.values().stream().flatMap(List::stream).distinct().count() == 3;
            stableChecks = complete ? stableChecks + 1 : 0;
            if (stableChecks >= 3) return;
            sleepBriefly();
        }
        throw new IllegalStateException("Consumer assignments did not stabilize: " + assignments());
    }

    public Map<String, List<Integer>> assignments() {
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        assignmentsByConsumer.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), entry.getValue().stream()
                        .map(TopicPartition::partition).sorted().toList()));
        return result;
    }

    public void reset() {
        synchronized (monitor) {
            expectedEventIds = Set.of();
            receivedInCallbackOrder = List.of();
            completion = new CompletableFuture<>();
        }
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
            throw new IllegalStateException("Interrupted while waiting for assignments", exception);
        }
    }
}
