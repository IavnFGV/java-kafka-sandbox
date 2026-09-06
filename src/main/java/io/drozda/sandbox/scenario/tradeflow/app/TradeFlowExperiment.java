package io.drozda.sandbox.scenario.tradeflow.app;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.tradeflow.model.TradeFlowEvent;
import io.drozda.sandbox.scenario.tradeflow.producer.TradeFlowPublisher;

public class TradeFlowExperiment {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final TradeFlowPublisher publisher;
    private final TradeFlowEventTracker tracker;
    private final String topic;

    public TradeFlowExperiment(TradeFlowPublisher publisher, TradeFlowEventTracker tracker, String topic) {
        this.publisher = publisher;
        this.tracker = tracker;
        this.topic = topic;
    }

    public TradeFlowScenarioStatus run(String invocationName) {
        TradeFlowEvent event = new TradeFlowEvent(
                UUID.randomUUID().toString(),
                "trade-" + UUID.randomUUID(),
                "AAPL",
                "CREATED",
                System.currentTimeMillis()
        );
        CompletableFuture<TradeFlowEvent> receivedEvent = tracker.expect(event.eventId());

        SendResult<String, TradeFlowEvent> sendResult;
        try {
            sendResult = publisher.publish(event).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            tracker.discard(event.eventId());
            return result(invocationName, false, false, event.eventId(), null, null, rootMessage(exception));
        }

        try {
            TradeFlowEvent consumed = receivedEvent.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            return result(invocationName, true, event.eventId().equals(consumed.eventId()), event.eventId(),
                    sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset(), null);
        } catch (Exception exception) {
            tracker.discard(event.eventId());
            return result(invocationName, true, false, event.eventId(),
                    sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset(), rootMessage(exception));
        }
    }

    private TradeFlowScenarioStatus result(
            String invocationName,
            boolean published,
            boolean received,
            String eventId,
            Integer partition,
            Long offset,
            String error
    ) {
        return new TradeFlowScenarioStatus(
                "trade-flow", invocationName, true, true, true,
                published, received, eventId, topic, partition, offset, error
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
