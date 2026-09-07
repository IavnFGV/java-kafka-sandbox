package io.drozda.sandbox.scenario.messagekeypartitionselection.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.messagekeypartitionselection.model.KeyedOrderEvent;
import io.drozda.sandbox.scenario.messagekeypartitionselection.producer.KeyedOrderEventPublisher;

public class KeyPartitioningExperiment {
    private static final long TIMEOUT_SECONDS = 10;

    private final KeyedOrderEventPublisher publisher;
    private final KeyedEventTracker tracker;
    private final String topic;

    public KeyPartitioningExperiment(KeyedOrderEventPublisher publisher, KeyedEventTracker tracker, String topic) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.topic = topic;
    }

    public KeyPartitioningScenarioStatus run(String invocationName, KeyStrategy strategy) {
        List<KeyedOrderEvent> events = List.of(
                event("order-42", "CREATED"),
                event("order-42", "VALIDATED"),
                event("order-42", "RESERVED"),
                event("order-42", "PAID"),
                event("order-42", "PACKING"),
                event("order-42", "PACKED"),
                event("order-42", "SHIPPING"),
                event("order-42", "SHIPPED"),
                event("order-42", "DELIVERING"),
                event("order-42", "DELIVERED"),
                event("order-73", "CREATED")
        );
        List<CompletableFuture<TrackedKeyedRecord>> receives =
                events.stream().map(event -> tracker.expect(event.eventId())).toList();
        List<SendResult<String, KeyedOrderEvent>> sends = new ArrayList<>();

        try {
            tracker.awaitStableAssignments(3, 3, Duration.ofSeconds(TIMEOUT_SECONDS));
            for (KeyedOrderEvent event : events) {
                sends.add(publisher.publish(strategy, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            List<TrackedKeyedRecord> consumed = new ArrayList<>();
            for (CompletableFuture<TrackedKeyedRecord> receive : receives) {
                consumed.add(receive.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            verify(events, sends, consumed, strategy);
            return status(invocationName, true, true, strategy, observations(consumed), null);
        } catch (Exception exception) {
            return status(invocationName, sends.size() == events.size(), false, strategy,
                    List.of(), rootMessage(exception));
        } finally {
            events.forEach(event -> tracker.discard(event.eventId()));
        }
    }

    private void verify(
            List<KeyedOrderEvent> events,
            List<SendResult<String, KeyedOrderEvent>> sends,
            List<TrackedKeyedRecord> consumed,
            KeyStrategy strategy
    ) {
        for (int index = 0; index < events.size(); index++) {
            String expectedKey = keyFor(strategy, events.get(index));
            var consumedRecord = consumed.get(index).record();
            if (!Objects.equals(expectedKey, consumedRecord.key())
                    || sends.get(index).getRecordMetadata().partition() != consumedRecord.partition()
                    || sends.get(index).getRecordMetadata().offset() != consumedRecord.offset()) {
                throw new IllegalStateException("Producer and consumer coordinates or key differ");
            }
        }
    }

    private String keyFor(KeyStrategy strategy, KeyedOrderEvent event) {
        return switch (strategy) {
            case NO_KEY -> null;
            case EVENT_ID -> event.eventId();
            case ORDER_ID -> event.orderId();
        };
    }

    private List<KeyedRecordObservation> observations(List<TrackedKeyedRecord> consumed) {
        return consumed.stream().map(tracked -> new KeyedRecordObservation(
                tracked.record().value().orderId(), tracked.record().value().status(),
                tracked.record().partition(), tracked.record().offset(), tracked.consumerId()
        )).toList();
    }

    private KeyedOrderEvent event(String orderId, String status) {
        return new KeyedOrderEvent(UUID.randomUUID().toString(), orderId, status, System.currentTimeMillis());
    }

    private KeyPartitioningScenarioStatus status(
            String invocationName, boolean published, boolean received, KeyStrategy strategy,
            List<KeyedRecordObservation> observations, String error
    ) {
        return new KeyPartitioningScenarioStatus(
                "key-partitioning", invocationName, true, true, true,
                published, received, strategy, strategy == KeyStrategy.ORDER_ID,
                topic, observations, tracker.assignments(), error
        );
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
