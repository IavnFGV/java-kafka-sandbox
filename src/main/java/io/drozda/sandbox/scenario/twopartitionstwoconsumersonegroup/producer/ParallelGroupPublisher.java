package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent;

public class ParallelGroupPublisher {
    private final KafkaTemplate<String, ParallelGroupEvent> template;

    public ParallelGroupPublisher(KafkaTemplate<String, ParallelGroupEvent> template) { this.template = template; }

    public CompletableFuture<SendResult<String, ParallelGroupEvent>> publish(String topic, ParallelGroupEvent event) {
        return template.send(topic, event.targetPartition(), "partition-" + event.targetPartition(), event);
    }
}
