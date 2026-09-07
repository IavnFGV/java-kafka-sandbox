package io.drozda.sandbox.scenario.globalordering.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.globalordering.model.GlobalOrderEvent;
import io.drozda.sandbox.scenario.globalordering.producer.GlobalOrderPublisher;

public class GlobalOrderingExperiment {
    private static final long TIMEOUT_SECONDS = 15;
    private static final long FAST_DELAY_MS = 80;
    private static final long SLOW_DELAY_MS = 280;

    private final GlobalOrderPublisher publisher;
    private final GlobalOrderTracker tracker;
    private final String singleTopic;
    private final String parallelTopic;

    public GlobalOrderingExperiment(
            GlobalOrderPublisher publisher,
            GlobalOrderTracker tracker,
            String singleTopic,
            String parallelTopic
    ) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.singleTopic = singleTopic;
        this.parallelTopic = parallelTopic;
    }

    public GlobalOrderingScenarioStatus run(String invocationName, GlobalOrderingMode mode) {
        String suffix = UUID.randomUUID().toString();
        String fastOrderId = "fast-" + suffix;
        String slowOrderId = "slow-" + suffix;
        List<GlobalOrderEvent> events = events(fastOrderId, slowOrderId);
        String topic = mode == GlobalOrderingMode.SINGLE ? singleTopic : parallelTopic;
        int partitionCount = mode == GlobalOrderingMode.SINGLE ? 1 : 2;
        CompletableFuture<List<TrackedGlobalOrderRecord>> received = tracker.expect(
                events.stream().map(GlobalOrderEvent::eventId).toList());
        List<SendResult<String, GlobalOrderEvent>> sends = new ArrayList<>();

        try {
            tracker.awaitAssignments(topic, partitionCount, Duration.ofSeconds(TIMEOUT_SECONDS));
            for (GlobalOrderEvent event : events) {
                int partition = mode == GlobalOrderingMode.SINGLE || event.orderId().equals(fastOrderId) ? 0 : 1;
                sends.add(publisher.publish(topic, partition, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            List<TrackedGlobalOrderRecord> completed = received.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            verifyProducerCoordinates(sends, completed);
            List<GlobalOrderObservation> observations = observations(completed);
            boolean perOrderSequencePreserved = orderSequencePreserved(observations, fastOrderId)
                    && orderSequencePreserved(observations, slowOrderId);
            boolean globalCompletionMatchesPublishOrder = IntStream.range(0, events.size())
                    .allMatch(index -> events.get(index).eventId()
                            .equals(completed.get(index).record().value().eventId()));
            return status(invocationName, mode, true, true, perOrderSequencePreserved,
                    globalCompletionMatchesPublishOrder,
                    completionTime(observations, fastOrderId), completionTime(observations, slowOrderId),
                    topic, observations, null);
        } catch (Exception exception) {
            return status(invocationName, mode, sends.size() == events.size(), false,
                    false, false, 0, 0, topic, List.of(), rootMessage(exception));
        } finally {
            tracker.reset();
        }
    }

    private List<GlobalOrderEvent> events(String fastOrderId, String slowOrderId) {
        List<GlobalOrderEvent> fast = orderEvents(
                fastOrderId, List.of("CREATED", "PAID", "COMPLETED"), FAST_DELAY_MS);
        List<GlobalOrderEvent> slow = orderEvents(
                slowOrderId,
                List.of("CREATED", "VALIDATING", "RESERVED", "PACKING", "SHIPPED", "COMPLETED"),
                SLOW_DELAY_MS);
        return List.of(
                fast.get(0), slow.get(0), slow.get(1), fast.get(1), slow.get(2),
                slow.get(3), fast.get(2), slow.get(4), slow.get(5)
        );
    }

    private List<GlobalOrderEvent> orderEvents(String orderId, List<String> statuses, long delayMs) {
        return IntStream.range(0, statuses.size())
                .mapToObj(sequence -> new GlobalOrderEvent(
                        UUID.randomUUID().toString(), orderId, sequence, statuses.get(sequence),
                        delayMs, System.currentTimeMillis()))
                .toList();
    }

    private List<GlobalOrderObservation> observations(List<TrackedGlobalOrderRecord> records) {
        return records.stream().map(tracked -> new GlobalOrderObservation(
                tracked.record().value().orderId().startsWith("fast-") ? "Fast Order" : "Slow Order",
                tracked.record().value().sequence(), tracked.record().value().status(),
                tracked.record().partition(), tracked.record().offset(), tracked.consumerId(),
                tracked.completedAfterMs()
        )).toList();
    }

    private boolean orderSequencePreserved(List<GlobalOrderObservation> observations, String rawOrderId) {
        String orderId = rawOrderId.startsWith("fast-") ? "Fast Order" : "Slow Order";
        List<Integer> sequences = observations.stream()
                .filter(observation -> observation.orderId().equals(orderId))
                .map(GlobalOrderObservation::sequence)
                .toList();
        return IntStream.range(0, sequences.size()).allMatch(index -> sequences.get(index) == index);
    }

    private long completionTime(List<GlobalOrderObservation> observations, String rawOrderId) {
        String orderId = rawOrderId.startsWith("fast-") ? "Fast Order" : "Slow Order";
        return observations.stream().filter(observation -> observation.orderId().equals(orderId))
                .map(GlobalOrderObservation::completedAfterMs).max(Comparator.naturalOrder()).orElse(0L);
    }

    private void verifyProducerCoordinates(
            List<SendResult<String, GlobalOrderEvent>> sends,
            List<TrackedGlobalOrderRecord> completed
    ) {
        Map<String, SendResult<String, GlobalOrderEvent>> byEventId = new HashMap<>();
        sends.forEach(send -> byEventId.put(send.getProducerRecord().value().eventId(), send));
        for (TrackedGlobalOrderRecord tracked : completed) {
            var metadata = byEventId.get(tracked.record().value().eventId()).getRecordMetadata();
            if (metadata.partition() != tracked.record().partition()
                    || metadata.offset() != tracked.record().offset()) {
                throw new IllegalStateException("Producer and consumer Kafka coordinates differ");
            }
        }
    }

    private GlobalOrderingScenarioStatus status(
            String invocationName,
            GlobalOrderingMode mode,
            boolean published,
            boolean received,
            boolean perOrderSequencePreserved,
            boolean globalCompletionMatchesPublishOrder,
            long fastOrderCompletedMs,
            long slowOrderCompletedMs,
            String topic,
            List<GlobalOrderObservation> observations,
            String error
    ) {
        return new GlobalOrderingScenarioStatus(
                "global-ordering", invocationName, true, true, true, published, received, mode,
                perOrderSequencePreserved, globalCompletionMatchesPublishOrder,
                fastOrderCompletedMs, slowOrderCompletedMs, topic, observations, error
        );
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
