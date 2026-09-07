package io.drozda.sandbox.scenario.topicpartitionoffsetbasics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.app.PartitionedEventTracker;
import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.model.PartitionedEvent;

class PartitionedEventTrackerTest {

    @Test
    void shouldCompleteOnlyMatchingExpectationWithConsumerCoordinates() {
        PartitionedEventTracker tracker = new PartitionedEventTracker();
        PartitionedEvent expected = new PartitionedEvent("expected", "A", 1L);
        PartitionedEvent unrelated = new PartitionedEvent("unrelated", "B", 2L);
        CompletableFuture<ConsumerRecord<String, PartitionedEvent>> expectation = tracker.expect(expected.eventId());

        tracker.received(new ConsumerRecord<>("topic", 1, 7L, unrelated.eventId(), unrelated));
        tracker.received(new ConsumerRecord<>("topic", 0, 12L, expected.eventId(), expected));

        ConsumerRecord<String, PartitionedEvent> received = expectation.join();
        assertEquals(0, received.partition());
        assertEquals(12L, received.offset());
        assertEquals(expected, received.value());
    }
}
