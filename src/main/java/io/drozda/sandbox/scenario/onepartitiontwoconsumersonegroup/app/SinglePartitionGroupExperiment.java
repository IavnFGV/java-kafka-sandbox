package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.consumer.ConsumerGroupMemberA;
import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.model.GroupWorkEvent;
import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.producer.GroupWorkPublisher;

public class SinglePartitionGroupExperiment {
    private static final int TIMEOUT_SECONDS = 15;

    private final GroupWorkPublisher publisher;
    private final SinglePartitionGroupTracker tracker;
    private final ConsumerMemberControl memberControl;
    private final String topic;

    public SinglePartitionGroupExperiment(
            GroupWorkPublisher publisher,
            SinglePartitionGroupTracker tracker,
            ConsumerMemberControl memberControl,
            String topic
    ) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.memberControl = memberControl;
        this.topic = topic;
    }

    public SinglePartitionGroupScenarioStatus run(String invocationName) {
        List<GroupWorkObservation> observations = new ArrayList<>();
        String initialOwner = null;
        String takeoverOwner = null;
        boolean published = false;
        try {
            tracker.resetAssignments();
            memberControl.startBoth();
            initialOwner = tracker.awaitSingleOwner(topic, null, Duration.ofSeconds(TIMEOUT_SECONDS));
            String idleConsumer = other(initialOwner);

            observations.addAll(publishBatch("BEFORE FAILURE", 0));
            published = true;
            verifyConsumer(observations, initialOwner, "BEFORE FAILURE");

            memberControl.stop(initialOwner);
            takeoverOwner = tracker.awaitSingleOwner(
                    topic, initialOwner, Duration.ofSeconds(TIMEOUT_SECONDS));

            observations.addAll(publishBatch("AFTER TAKEOVER", 3));
            verifyConsumer(observations, takeoverOwner, "AFTER TAKEOVER");
            return status(invocationName, true, true, initialOwner, idleConsumer,
                    takeoverOwner, observations, null);
        } catch (Exception exception) {
            return status(invocationName, published, false, initialOwner,
                    initialOwner != null ? other(initialOwner) : null,
                    takeoverOwner, observations, rootMessage(exception));
        } finally {
            tracker.resetEvents();
        }
    }

    private List<GroupWorkObservation> publishBatch(String phase, int firstSequence) throws Exception {
        List<GroupWorkEvent> events = IntStream.range(firstSequence, firstSequence + 3)
                .mapToObj(sequence -> new GroupWorkEvent(
                        UUID.randomUUID().toString(), phase, sequence, System.currentTimeMillis()))
                .toList();
        var received = tracker.expect(events.stream().map(GroupWorkEvent::eventId).toList());
        List<SendResult<String, GroupWorkEvent>> sends = new ArrayList<>();
        for (GroupWorkEvent event : events) {
            sends.add(publisher.publish(topic, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
        List<TrackedGroupWorkRecord> records = received.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        List<GroupWorkObservation> result = records.stream().map(tracked -> new GroupWorkObservation(
                tracked.record().value().phase(), tracked.record().value().sequence(),
                tracked.record().partition(), tracked.record().offset(), tracked.consumerId()
        )).toList();
        verifyCoordinates(sends, records);
        return result;
    }

    private void verifyCoordinates(
            List<SendResult<String, GroupWorkEvent>> sends,
            List<TrackedGroupWorkRecord> records
    ) {
        for (TrackedGroupWorkRecord tracked : records) {
            SendResult<String, GroupWorkEvent> send = sends.stream()
                    .filter(candidate -> candidate.getProducerRecord().value().eventId()
                            .equals(tracked.record().value().eventId()))
                    .findFirst().orElseThrow();
            if (send.getRecordMetadata().partition() != tracked.record().partition()
                    || send.getRecordMetadata().offset() != tracked.record().offset()) {
                throw new IllegalStateException("Producer and consumer Kafka coordinates differ");
            }
        }
    }

    private void verifyConsumer(
            List<GroupWorkObservation> observations,
            String expectedConsumer,
            String phase
    ) {
        boolean matches = observations.stream()
                .filter(observation -> observation.phase().equals(phase))
                .allMatch(observation -> observation.partition() == 0
                        && observation.consumerId().equals(expectedConsumer));
        if (!matches) throw new IllegalStateException("Records were not handled by " + expectedConsumer);
    }

    private String other(String consumer) {
        return ConsumerGroupMemberA.LABEL.equals(consumer) ? "Consumer B" : ConsumerGroupMemberA.LABEL;
    }

    private SinglePartitionGroupScenarioStatus status(
            String invocationName,
            boolean published,
            boolean received,
            String initialOwner,
            String idleConsumer,
            String takeoverOwner,
            List<GroupWorkObservation> observations,
            String error
    ) {
        return new SinglePartitionGroupScenarioStatus(
                "consumer-group-single-partition", invocationName, true, true, true,
                published, received, initialOwner, idleConsumer, takeoverOwner,
                List.copyOf(observations), topic, error
        );
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
