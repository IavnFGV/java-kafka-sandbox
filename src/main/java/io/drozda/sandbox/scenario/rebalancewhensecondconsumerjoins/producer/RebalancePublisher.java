package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.producer;

import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent;

public class RebalancePublisher {
    private final KafkaTemplate<String, RebalanceEvent> template;
    public RebalancePublisher(KafkaTemplate<String, RebalanceEvent> template) { this.template = template; }
    public CompletableFuture<SendResult<String, RebalanceEvent>> publish(String topic, RebalanceEvent event) {
        return template.send(topic, event.partition(), "p" + event.partition(), event);
    }
}
