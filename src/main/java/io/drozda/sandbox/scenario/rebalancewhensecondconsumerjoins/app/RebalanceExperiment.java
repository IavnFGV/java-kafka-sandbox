package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.producer.RebalancePublisher;

public class RebalanceExperiment {
    private static final int TIMEOUT_SECONDS = 20;
    private final RebalancePublisher publisher; private final RebalanceTracker tracker;
    private final RebalanceConsumerControl control; private final String topic;
    public RebalanceExperiment(RebalancePublisher publisher, RebalanceTracker tracker, RebalanceConsumerControl control, String topic) {
        this.publisher = publisher; this.tracker = tracker; this.control = control; this.topic = topic;
    }
    public RebalanceScenarioStatus run(String name) {
        Map<Integer, String> initial = Map.of(); Map<Integer, String> after = Map.of();
        List<RebalanceObservation> observations = new ArrayList<>(); boolean published = false;
        try {
            control.startA();
            initial = tracker.awaitOwners(topic, 1, Duration.ofSeconds(TIMEOUT_SECONDS));
            observations.addAll(publishBatch("BEFORE JOIN", 0)); published = true;
            verifyOwners(observations, "BEFORE JOIN", initial);
            control.startB();
            after = tracker.awaitOwners(topic, 2, Duration.ofSeconds(TIMEOUT_SECONDS));
            observations.addAll(publishBatch("AFTER JOIN", 2));
            verifyOwners(observations, "AFTER JOIN", after);
            return status(name, true, true, initial, after, observations, null);
        } catch (Exception exception) {
            return status(name, published, false, initial, after, observations, rootMessage(exception));
        }
    }
    private List<RebalanceObservation> publishBatch(String phase, int first) throws Exception {
        List<RebalanceEvent> events = IntStream.range(0, 2).mapToObj(partition -> new RebalanceEvent(
                UUID.randomUUID().toString(), phase, partition, first + partition, System.currentTimeMillis())).toList();
        var completion = tracker.expect(events.stream().map(RebalanceEvent::eventId).toList());
        for (RebalanceEvent event : events) publisher.publish(topic, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return completion.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).stream().map(value -> new RebalanceObservation(
                value.record().value().phase(), value.record().value().sequence(), value.record().partition(),
                value.record().offset(), value.consumer())).toList();
    }
    private void verifyOwners(List<RebalanceObservation> values, String phase, Map<Integer, String> owners) {
        if (!values.stream().filter(v -> phase.equals(v.phase())).allMatch(v -> v.consumer().equals(owners.get(v.partition()))))
            throw new IllegalStateException("Records did not follow partition ownership during " + phase);
    }
    private RebalanceScenarioStatus status(String name, boolean published, boolean rebalanced,
            Map<Integer, String> initial, Map<Integer, String> after, List<RebalanceObservation> values, String error) {
        return new RebalanceScenarioStatus("consumer-group-rebalance-join", name, true, true, true,
                published, rebalanced, initial, after, List.copyOf(values), tracker.membershipEvents(), topic, error);
    }
    private String rootMessage(Exception e) { Throwable c=e; while(c.getCause()!=null)c=c.getCause(); return c.getMessage()!=null?c.getMessage():c.getClass().getSimpleName(); }
}
