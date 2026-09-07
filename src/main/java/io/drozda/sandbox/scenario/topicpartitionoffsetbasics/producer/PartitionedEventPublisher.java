package io.drozda.sandbox.scenario.topicpartitionoffsetbasics.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.topicpartitionoffsetbasics.model.PartitionedEvent;

public class PartitionedEventPublisher {

    private final KafkaTemplate<String, PartitionedEvent> kafkaTemplate;
    private final String topic;

    public PartitionedEventPublisher(KafkaTemplate<String, PartitionedEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, PartitionedEvent>> publish(
            int partition,
            PartitionedEvent event
    ) {
        return kafkaTemplate.send(topic, partition, event.eventId(), event);
    }
}
