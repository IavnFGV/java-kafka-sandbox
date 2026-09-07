package io.drozda.sandbox.scenario.earliestvslatest.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.drozda.sandbox.scenario.earliestvslatest.model.OffsetResetEvent;
import io.drozda.sandbox.scenario.earliestvslatest.producer.OffsetResetPublisher;

public class EarliestVsLatestExperiment {
    private static final int TIMEOUT_SECONDS = 15;
    private final OffsetResetPublisher publisher;
    private final OffsetResetTracker tracker;
    private final OffsetResetConsumerControl consumers;
    private final String topic;

    public EarliestVsLatestExperiment(OffsetResetPublisher publisher, OffsetResetTracker tracker,
            OffsetResetConsumerControl consumers, String topic) {
        this.publisher = publisher; this.tracker = tracker; this.consumers = consumers; this.topic = topic;
    }

    public EarliestVsLatestScenarioStatus run(String invocationName) {
        boolean published = false;
        try {
            List<OffsetResetEvent> history = events("HISTORY", 0, 3);
            List<OffsetResetEvent> live = events("LIVE", 3, 5);
            List<OffsetResetEvent> all = new ArrayList<>(history);
            all.addAll(live);
            var received = tracker.expect(all.stream().map(OffsetResetEvent::eventId).toList());

            publish(history);
            consumers.startBoth();
            tracker.awaitBothAssigned(Duration.ofSeconds(TIMEOUT_SECONDS));
            publish(live);
            published = true;

            List<OffsetResetObservation> observations = received.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .stream().sorted(Comparator.comparing(OffsetResetObservation::group)
                            .thenComparingLong(OffsetResetObservation::offset)).toList();
            verify(observations);
            return status(invocationName, true, true, observations, null);
        } catch (Exception exception) {
            return status(invocationName, published, false, List.of(), rootMessage(exception));
        } finally {
            tracker.reset();
        }
    }

    private List<OffsetResetEvent> events(String phase, int start, int end) {
        return IntStream.range(start, end).mapToObj(sequence -> new OffsetResetEvent(
                UUID.randomUUID().toString(), phase, sequence, System.currentTimeMillis())).toList();
    }
    private void publish(List<OffsetResetEvent> events) throws Exception {
        for (OffsetResetEvent event : events) publisher.publish(topic, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
    private void verify(List<OffsetResetObservation> observations) {
        List<Long> earliest = offsets(observations, "earliest");
        List<Long> latest = offsets(observations, "latest");
        if (!earliest.equals(List.of(0L, 1L, 2L, 3L, 4L)) || !latest.equals(List.of(3L, 4L))) {
            throw new IllegalStateException("Unexpected reset positions: earliest=" + earliest + ", latest=" + latest);
        }
    }
    private List<Long> offsets(List<OffsetResetObservation> values, String group) {
        return values.stream().filter(value -> group.equals(value.group()))
                .map(OffsetResetObservation::offset).sorted().toList();
    }
    private EarliestVsLatestScenarioStatus status(String name, boolean published, boolean verified,
            List<OffsetResetObservation> observations, String error) {
        return new EarliestVsLatestScenarioStatus("earliest-vs-latest", name, true, true, true,
                published, verified, observations, topic, error);
    }
    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
