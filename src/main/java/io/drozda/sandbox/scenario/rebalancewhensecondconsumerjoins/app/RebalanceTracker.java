package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

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
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent;

public class RebalanceTracker {
    private final Object monitor = new Object();
    private final Map<String, Set<TopicPartition>> assignments = new ConcurrentHashMap<>();
    private final List<String> membershipEvents = java.util.Collections.synchronizedList(new ArrayList<>());
    private Set<String> expectedIds = Set.of();
    private List<TrackedRebalanceRecord> records = List.of();
    private CompletableFuture<List<TrackedRebalanceRecord>> completion = new CompletableFuture<>();

    public CompletableFuture<List<TrackedRebalanceRecord>> expect(List<String> ids) {
        synchronized (monitor) { expectedIds = Set.copyOf(ids); records = new ArrayList<>(); completion = new CompletableFuture<>(); return completion; }
    }
    public void received(ConsumerRecord<String, RebalanceEvent> record, String consumer) {
        synchronized (monitor) {
            if (!expectedIds.contains(record.value().eventId())) return;
            records.add(new TrackedRebalanceRecord(record, consumer));
            if (records.size() == expectedIds.size()) completion.complete(List.copyOf(records));
        }
    }
    public void assigned(String consumer, Set<TopicPartition> partitions) {
        assignments.put(consumer, Set.copyOf(partitions));
        membershipEvents.add(consumer + " assigned " + partitionNumbers(partitions));
    }
    public void revoked(String consumer, Collection<TopicPartition> partitions) {
        assignments.put(consumer, Set.of());
        membershipEvents.add(consumer + " revoked " + partitionNumbers(partitions));
    }
    public Map<Integer, String> awaitOwners(String topic, int distinctOwners, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Map<Integer, String> owners = owners(topic);
            if (owners.size() == 2 && owners.values().stream().distinct().count() == distinctOwners) return Map.copyOf(owners);
            sleep();
        }
        throw new IllegalStateException("Assignment did not stabilize with " + distinctOwners + " owner(s)");
    }
    public List<String> membershipEvents() { return List.copyOf(membershipEvents); }
    public void reset() { synchronized (monitor) { expectedIds = Set.of(); records = List.of(); completion = new CompletableFuture<>(); assignments.clear(); membershipEvents.clear(); } }
    private Map<Integer, String> owners(String topic) {
        Map<Integer, String> result = new LinkedHashMap<>();
        assignments.forEach((consumer, partitions) -> partitions.stream().filter(p -> p.topic().equals(topic))
                .forEach(p -> result.put(p.partition(), consumer)));
        return result;
    }
    private List<Integer> partitionNumbers(Collection<TopicPartition> values) { return values.stream().map(TopicPartition::partition).sorted().toList(); }
    private void sleep() { try { Thread.sleep(75); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); } }
}
