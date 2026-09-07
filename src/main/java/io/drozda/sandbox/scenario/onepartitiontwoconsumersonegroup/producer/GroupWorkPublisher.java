package io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.onepartitiontwoconsumersonegroup.model.GroupWorkEvent;

public class GroupWorkPublisher {
    private final KafkaTemplate<String, GroupWorkEvent> kafkaTemplate;

    public GroupWorkPublisher(KafkaTemplate<String, GroupWorkEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, GroupWorkEvent>> publish(String topic, GroupWorkEvent event) {
        return kafkaTemplate.send(topic, "shared-work", event);
    }
}
