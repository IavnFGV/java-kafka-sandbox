package io.drozda.sandbox.scenario.orderingwithinonepartition.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.orderingwithinonepartition.model.OrderedOrderEvent;
import io.drozda.sandbox.scenario.orderingwithinonepartition.producer.OrderedEventPublisher;

public class PartitionOrderingExperiment {
    private static final long TIMEOUT_SECONDS = 10;
    private static final List<String> STATUSES = List.of(
            "CREATED", "VALIDATED", "RESERVED", "PAID", "PACKED", "SHIPPED"
    );

    private final OrderedEventPublisher publisher;
    private final OrderedEventTracker tracker;
    private final String topic;

    public PartitionOrderingExperiment(OrderedEventPublisher publisher, OrderedEventTracker tracker, String topic) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.topic = topic;
    }

    public PartitionOrderingScenarioStatus run(String invocationName) {
        String orderId = "order-005-" + UUID.randomUUID();
        List<OrderedOrderEvent> events = IntStream.range(0, STATUSES.size())
                .mapToObj(sequence -> new OrderedOrderEvent(
                        UUID.randomUUID().toString(), orderId, sequence,
                        STATUSES.get(sequence), System.currentTimeMillis()))
                .toList();
        CompletableFuture<List<TrackedOrderedRecord>> received = tracker.expect(
                events.stream().map(OrderedOrderEvent::eventId).toList());
        List<SendResult<String, OrderedOrderEvent>> sends = new ArrayList<>();

        try {
            tracker.awaitStableAssignments(Duration.ofSeconds(TIMEOUT_SECONDS));
            for (OrderedOrderEvent event : events) {
                sends.add(publisher.publish(event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            List<TrackedOrderedRecord> consumed = received.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<OrderedRecordObservation> observations = observations(consumed);
            boolean onePartition = observations.stream().map(OrderedRecordObservation::partition).distinct().count() == 1;
            boolean increasingOffsets = adjacentOffsetsIncrease(observations);
            boolean receiveOrderPreserved = IntStream.range(0, observations.size())
                    .allMatch(index -> observations.get(index).sequence() == index);
            verifyProducerCoordinates(sends, consumed);
            return status(invocationName, true, true, onePartition, increasingOffsets,
                    receiveOrderPreserved, observations, null);
        } catch (Exception exception) {
            return status(invocationName, sends.size() == events.size(), false,
                    false, false, false, List.of(), rootMessage(exception));
        } finally {
            tracker.reset();
        }
    }

    private List<OrderedRecordObservation> observations(List<TrackedOrderedRecord> consumed) {
        return consumed.stream().map(tracked -> new OrderedRecordObservation(
                tracked.record().value().sequence(), tracked.record().value().status(),
                tracked.record().partition(), tracked.record().offset(), tracked.consumerId()
        )).toList();
    }

    private boolean adjacentOffsetsIncrease(List<OrderedRecordObservation> observations) {
        return IntStream.range(1, observations.size())
                .allMatch(index -> observations.get(index).offset() > observations.get(index - 1).offset());
    }

    private void verifyProducerCoordinates(
            List<SendResult<String, OrderedOrderEvent>> sends,
            List<TrackedOrderedRecord> consumed
    ) {
        Map<String, SendResult<String, OrderedOrderEvent>> sendsByEventId = sends.stream()
                .collect(java.util.stream.Collectors.toMap(
                        send -> send.getProducerRecord().value().eventId(), send -> send));
        for (TrackedOrderedRecord tracked : consumed) {
            var record = tracked.record();
            var metadata = sendsByEventId.get(record.value().eventId()).getRecordMetadata();
            if (metadata.partition() != record.partition() || metadata.offset() != record.offset()) {
                throw new IllegalStateException(
                        "Producer and listener coordinates differ at sequence " + record.value().sequence());
            }
        }
    }

    private PartitionOrderingScenarioStatus status(
            String invocationName, boolean published, boolean received,
            boolean onePartition, boolean increasingOffsets, boolean receiveOrderPreserved,
            List<OrderedRecordObservation> observations, String error
    ) {
        return new PartitionOrderingScenarioStatus(
                "partition-ordering", invocationName, true, true, true,
                published, received, onePartition, increasingOffsets, receiveOrderPreserved,
                topic, observations, tracker.assignments(), error
        );
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
