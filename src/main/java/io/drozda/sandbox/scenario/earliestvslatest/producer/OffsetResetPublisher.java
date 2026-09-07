package io.drozda.sandbox.scenario.earliestvslatest.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.earliestvslatest.model.OffsetResetEvent;

public class OffsetResetPublisher {
    private final KafkaTemplate<String, OffsetResetEvent> template;

    public OffsetResetPublisher(KafkaTemplate<String, OffsetResetEvent> template) { this.template = template; }

    public CompletableFuture<SendResult<String, OffsetResetEvent>> publish(String topic, OffsetResetEvent event) {
        return template.send(topic, event.eventId(), event);
    }
}
