package io.drozda.sandbox.scenario.multipleconsumergroups.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;

public class SharedOrderPublisher {
    private final KafkaTemplate<String, SharedOrderEvent> template;

    public SharedOrderPublisher(KafkaTemplate<String, SharedOrderEvent> template) {
        this.template = template;
    }

    public CompletableFuture<SendResult<String, SharedOrderEvent>> publish(String topic, SharedOrderEvent event) {
        return template.send(topic, event.eventId(), event);
    }
}
