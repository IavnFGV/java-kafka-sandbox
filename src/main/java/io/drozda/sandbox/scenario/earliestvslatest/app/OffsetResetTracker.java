package io.drozda.sandbox.scenario.earliestvslatest.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.drozda.sandbox.scenario.earliestvslatest.model.OffsetResetEvent;

public class OffsetResetTracker {
    private final Object monitor = new Object();
    private final Set<String> assignedGroups = ConcurrentHashMap.newKeySet();
    private Set<String> expectedIds = Set.of();
    private List<OffsetResetObservation> observations = List.of();
    private CompletableFuture<List<OffsetResetObservation>> completion = new CompletableFuture<>();

    public CompletableFuture<List<OffsetResetObservation>> expect(List<String> eventIds) {
        synchronized (monitor) {
            expectedIds = Set.copyOf(eventIds);
            observations = new ArrayList<>();
            completion = new CompletableFuture<>();
            return completion;
        }
    }

    public void received(ConsumerRecord<String, OffsetResetEvent> record, String group) {
        synchronized (monitor) {
            if (!expectedIds.contains(record.value().eventId())) return;
            observations.add(new OffsetResetObservation(group, record.value().phase(),
                    record.value().sequence(), record.partition(), record.offset()));
            long earliest = observations.stream().filter(value -> "earliest".equals(value.group())).count();
            long latest = observations.stream().filter(value -> "latest".equals(value.group())).count();
            if (earliest == 5 && latest == 2) completion.complete(List.copyOf(observations));
        }
    }

    public void assigned(String group) { assignedGroups.add(group); }

    public void awaitBothAssigned(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (assignedGroups.containsAll(Set.of("earliest", "latest"))) return;
            sleep();
        }
        throw new IllegalStateException("Offset reset consumers were not assigned");
    }

    public void reset() {
        synchronized (monitor) {
            expectedIds = Set.of();
            observations = List.of();
            completion = new CompletableFuture<>();
            assignedGroups.clear();
        }
    }

    private void sleep() {
        try { Thread.sleep(50); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Assignment wait interrupted", exception);
        }
    }
}
