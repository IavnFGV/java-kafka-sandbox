package io.drozda.sandbox.scenario.keypartitioning.app;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.keypartitioning.model.KeyedOrderEvent;
import io.drozda.sandbox.scenario.keypartitioning.producer.KeyedOrderEventPublisher;

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

    public KeyPartitioningScenarioStatus run(String invocationName) {
        List<KeyedOrderEvent> events = List.of(
                event("order-42", "CREATED"),
                event("order-42", "PAID"),
                event("order-42", "SHIPPED"),
                event("order-73", "CREATED")
        );
        List<CompletableFuture<ConsumerRecord<String, KeyedOrderEvent>>> receives =
                events.stream().map(event -> tracker.expect(event.eventId())).toList();
        List<SendResult<String, KeyedOrderEvent>> sends = new ArrayList<>();

        try {
            for (KeyedOrderEvent event : events) {
                sends.add(publisher.publish(event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            List<ConsumerRecord<String, KeyedOrderEvent>> consumed = new ArrayList<>();
            for (CompletableFuture<ConsumerRecord<String, KeyedOrderEvent>> receive : receives) {
                consumed.add(receive.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            verify(events, sends, consumed);
            return status(invocationName, true, true, observations(events, sends), null);
        } catch (Exception exception) {
            return status(invocationName, sends.size() == events.size(), false, observations(events, sends), rootMessage(exception));
        } finally {
            events.forEach(event -> tracker.discard(event.eventId()));
        }
    }

    private void verify(
            List<KeyedOrderEvent> events,
            List<SendResult<String, KeyedOrderEvent>> sends,
            List<ConsumerRecord<String, KeyedOrderEvent>> consumed
    ) {
        int order42Partition = sends.get(0).getRecordMetadata().partition();
        for (int index = 0; index < events.size(); index++) {
            if (!events.get(index).orderId().equals(consumed.get(index).key())
                    || sends.get(index).getRecordMetadata().partition() != consumed.get(index).partition()
                    || sends.get(index).getRecordMetadata().offset() != consumed.get(index).offset()) {
                throw new IllegalStateException("Producer and consumer coordinates or key differ");
            }
            if ("order-42".equals(events.get(index).orderId())
                    && sends.get(index).getRecordMetadata().partition() != order42Partition) {
                throw new IllegalStateException("Records sharing order-42 key reached different partitions");
            }
        }
    }

    private List<KeyedRecordObservation> observations(
            List<KeyedOrderEvent> events,
            List<SendResult<String, KeyedOrderEvent>> sends
    ) {
        List<KeyedRecordObservation> result = new ArrayList<>();
        for (int index = 0; index < sends.size(); index++) {
            result.add(new KeyedRecordObservation(
                    events.get(index).orderId(), events.get(index).status(),
                    sends.get(index).getRecordMetadata().partition(), sends.get(index).getRecordMetadata().offset()
            ));
        }
        return result;
    }

    private KeyedOrderEvent event(String orderId, String status) {
        return new KeyedOrderEvent(UUID.randomUUID().toString(), orderId, status, System.currentTimeMillis());
    }

    private KeyPartitioningScenarioStatus status(
            String invocationName, boolean published, boolean received,
            List<KeyedRecordObservation> observations, String error
    ) {
        return new KeyPartitioningScenarioStatus(
                "key-partitioning", invocationName, true, true, true,
                published, received, topic, observations, error
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
