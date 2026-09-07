package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.model.PartitionedEvent;
import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.producer.PartitionedEventPublisher;

public class TopicPartitionOffsetsExperiment {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final PartitionedEventPublisher publisher;
    private final PartitionedEventTracker tracker;
    private final String topic;

    public TopicPartitionOffsetsExperiment(
            PartitionedEventPublisher publisher,
            PartitionedEventTracker tracker,
            String topic
    ) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.topic = topic;
    }

    public TopicPartitionOffsetsScenarioStatus run(String invocationName) {
        PartitionedEvent first = event("A");
        PartitionedEvent second = event("B");
        PartitionedEvent third = event("C");
        List<PartitionedEvent> events = List.of(first, second, third);

        CompletableFuture<ConsumerRecord<String, PartitionedEvent>> firstReceived = tracker.expect(first.eventId());
        CompletableFuture<ConsumerRecord<String, PartitionedEvent>> secondReceived = tracker.expect(second.eventId());
        CompletableFuture<ConsumerRecord<String, PartitionedEvent>> thirdReceived = tracker.expect(third.eventId());

        SendResult<String, PartitionedEvent> firstSent = null;
        SendResult<String, PartitionedEvent> secondSent = null;
        SendResult<String, PartitionedEvent> thirdSent = null;
        try {
            firstSent = await(publisher.publish(0, first));
            secondSent = await(publisher.publish(1, second));
            thirdSent = await(publisher.publish(0, third));

            ConsumerRecord<String, PartitionedEvent> firstConsumed = await(firstReceived);
            ConsumerRecord<String, PartitionedEvent> secondConsumed = await(secondReceived);
            ConsumerRecord<String, PartitionedEvent> thirdConsumed = await(thirdReceived);
            verifyCoordinates(firstSent, firstConsumed);
            verifyCoordinates(secondSent, secondConsumed);
            verifyCoordinates(thirdSent, thirdConsumed);

            if (thirdSent.getRecordMetadata().offset() <= firstSent.getRecordMetadata().offset()) {
                throw new IllegalStateException("Partition 0 offset did not advance");
            }

            return result(invocationName, true, true, firstSent, secondSent, thirdSent, null);
        } catch (Exception exception) {
            return result(invocationName, allPublished(firstSent, secondSent, thirdSent), false,
                    firstSent, secondSent, thirdSent, rootMessage(exception));
        } finally {
            events.forEach(event -> tracker.discard(event.eventId()));
        }
    }

    private PartitionedEvent event(String label) {
        return new PartitionedEvent(UUID.randomUUID().toString(), label, System.currentTimeMillis());
    }

    private <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void verifyCoordinates(
            SendResult<String, PartitionedEvent> sent,
            ConsumerRecord<String, PartitionedEvent> consumed
    ) {
        if (sent.getRecordMetadata().partition() != consumed.partition()
                || sent.getRecordMetadata().offset() != consumed.offset()) {
            throw new IllegalStateException("Producer and consumer record coordinates differ");
        }
    }

    private boolean allPublished(
            SendResult<String, PartitionedEvent> first,
            SendResult<String, PartitionedEvent> second,
            SendResult<String, PartitionedEvent> third
    ) {
        return first != null && second != null && third != null;
    }

    private TopicPartitionOffsetsScenarioStatus result(
            String invocationName,
            boolean published,
            boolean received,
            SendResult<String, PartitionedEvent> first,
            SendResult<String, PartitionedEvent> second,
            SendResult<String, PartitionedEvent> third,
            String error
    ) {
        return new TopicPartitionOffsetsScenarioStatus(
                "topic-partition-offsets", invocationName, true, true, true,
                published, received, topic,
                partition(first), offset(first), partition(second), offset(second), partition(third), offset(third), error
        );
    }

    private Integer partition(SendResult<String, PartitionedEvent> result) {
        return result == null ? null : result.getRecordMetadata().partition();
    }

    private Long offset(SendResult<String, PartitionedEvent> result) {
        return result == null ? null : result.getRecordMetadata().offset();
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
