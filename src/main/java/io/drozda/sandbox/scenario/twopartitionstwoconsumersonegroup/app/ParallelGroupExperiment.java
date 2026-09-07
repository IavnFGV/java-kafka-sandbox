package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.producer.ParallelGroupPublisher;

public class ParallelGroupExperiment {
    private static final int TIMEOUT_SECONDS = 15;
    private final ParallelGroupPublisher publisher;
    private final ParallelGroupTracker tracker;
    private final String topic;

    public ParallelGroupExperiment(ParallelGroupPublisher publisher, ParallelGroupTracker tracker, String topic) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.topic = topic;
    }

    public ParallelGroupScenarioStatus run(String invocationName) {
        List<SendResult<String, ParallelGroupEvent>> sends = new ArrayList<>();
        try {
            Map<Integer, String> owners = tracker.awaitSplitAssignment(
                    topic, Duration.ofSeconds(TIMEOUT_SECONDS));
            List<ParallelGroupEvent> events = IntStream.range(0, 6)
                    .mapToObj(sequence -> new ParallelGroupEvent(
                            UUID.randomUUID().toString(), sequence % 2, sequence, System.currentTimeMillis()))
                    .toList();
            var received = tracker.expect(events.stream().map(ParallelGroupEvent::eventId).toList());
            for (ParallelGroupEvent event : events) {
                sends.add(publisher.publish(topic, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            List<TrackedParallelGroupRecord> records = received.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            verify(sends, records, owners);
            List<ParallelGroupObservation> observations = records.stream()
                    .map(record -> new ParallelGroupObservation(
                            record.record().value().sequence(), record.record().partition(),
                            record.record().offset(), record.consumerId()))
                    .toList();
            return status(invocationName, true, true, owners, observations, null);
        } catch (Exception exception) {
            return status(invocationName, sends.size() == 6, false, Map.of(), List.of(), rootMessage(exception));
        } finally {
            tracker.reset();
        }
    }

    private void verify(
            List<SendResult<String, ParallelGroupEvent>> sends,
            List<TrackedParallelGroupRecord> records,
            Map<Integer, String> owners
    ) {
        if (records.size() != 6) throw new IllegalStateException("Expected six consumed records");
        for (TrackedParallelGroupRecord record : records) {
            if (!record.consumerId().equals(owners.get(record.record().partition()))) {
                throw new IllegalStateException("Record was handled outside its assigned partition owner");
            }
            SendResult<String, ParallelGroupEvent> send = sends.stream()
                    .filter(candidate -> candidate.getProducerRecord().value().eventId()
                            .equals(record.record().value().eventId()))
                    .findFirst().orElseThrow();
            if (send.getRecordMetadata().partition() != record.record().partition()
                    || send.getRecordMetadata().offset() != record.record().offset()) {
                throw new IllegalStateException("Producer and consumer coordinates differ");
            }
        }
    }

    private ParallelGroupScenarioStatus status(
            String invocationName, boolean published, boolean received,
            Map<Integer, String> owners, List<ParallelGroupObservation> observations, String error
    ) {
        return new ParallelGroupScenarioStatus(
                "consumer-group-two-partitions", invocationName, true, true, true,
                published, received, owners, observations, topic, error);
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
