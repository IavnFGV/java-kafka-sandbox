package io.drozda.sandbox.scenario.keypartitioning.app;

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
}
