package io.drozda.sandbox.scenario.multipleconsumergroups.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;
import io.drozda.sandbox.scenario.multipleconsumergroups.producer.SharedOrderPublisher;

public class MultipleConsumerGroupsExperiment {
    private static final int EVENT_COUNT = 3;
    private static final int TIMEOUT_SECONDS = 15;
    private final SharedOrderPublisher publisher;
    private final MultipleConsumerGroupsTracker tracker;
    private final String topic;

    public MultipleConsumerGroupsExperiment(
            SharedOrderPublisher publisher, MultipleConsumerGroupsTracker tracker, String topic) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.topic = topic;
    }

    public MultipleConsumerGroupsScenarioStatus run(String invocationName) {
        List<SendResult<String, SharedOrderEvent>> sends = new ArrayList<>();
        try {
            List<SharedOrderEvent> events = IntStream.range(0, EVENT_COUNT)
                    .mapToObj(sequence -> new SharedOrderEvent(
                            UUID.randomUUID().toString(), sequence, System.currentTimeMillis()))
                    .toList();
            var received = tracker.expect(events.stream().map(SharedOrderEvent::eventId).toList());
            for (SharedOrderEvent event : events) {
                sends.add(publisher.publish(topic, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            List<TrackedGroupRecord> records = received.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            verify(sends, records);
            List<ConsumerGroupObservation> observations = records.stream()
                    .map(record -> new ConsumerGroupObservation(
                            record.record().value().sequence(), record.record().partition(),
                            record.record().offset(), record.groupId()))
                    .sorted(Comparator.comparingInt(ConsumerGroupObservation::sequence)
                            .thenComparing(ConsumerGroupObservation::groupId))
                    .toList();
            return status(invocationName, true, true, observations, null);
        } catch (Exception exception) {
            return status(invocationName, sends.size() == EVENT_COUNT, false, List.of(), rootMessage(exception));
        } finally {
            tracker.reset();
        }
    }

    private void verify(List<SendResult<String, SharedOrderEvent>> sends, List<TrackedGroupRecord> records) {
        Map<String, List<TrackedGroupRecord>> byGroup = records.stream()
                .collect(Collectors.groupingBy(TrackedGroupRecord::groupId));
        if (byGroup.size() != 2 || byGroup.values().stream().anyMatch(group -> group.size() != EVENT_COUNT)) {
            throw new IllegalStateException("Each consumer group must receive all three records");
        }
        for (SendResult<String, SharedOrderEvent> send : sends) {
            String eventId = send.getProducerRecord().value().eventId();
            List<TrackedGroupRecord> copies = records.stream()
                    .filter(record -> record.record().value().eventId().equals(eventId)).toList();
            if (copies.size() != 2 || copies.stream().anyMatch(record ->
                    record.record().partition() != send.getRecordMetadata().partition()
                            || record.record().offset() != send.getRecordMetadata().offset())) {
                throw new IllegalStateException("Groups observed different Kafka coordinates for " + eventId);
            }
        }
    }

    private MultipleConsumerGroupsScenarioStatus status(
            String invocationName, boolean published, boolean received,
            List<ConsumerGroupObservation> observations, String error) {
        return new MultipleConsumerGroupsScenarioStatus(
                "multiple-consumer-groups", invocationName, true, true, true,
                published, received, observations, topic, error);
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
