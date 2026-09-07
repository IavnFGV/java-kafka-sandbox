package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.consumer.RebalanceConsumerA;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.consumer.RebalanceConsumerB;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.producer.RebalancePublisher;

@Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
@ConditionalOnProperty(name="scenario.rebalance-join.enabled", havingValue="true")
@Import(RebalanceScenarioController.class)
public class RebalanceScenarioApplication {
    @Bean NewTopic rebalanceTopic(@Value("${app.kafka.topics.rebalance-join}") String topic) { return TopicBuilder.name(topic).partitions(2).replicas(1).build(); }
    @Bean RebalanceTracker rebalanceTracker() { return new RebalanceTracker(); }
    @Bean RebalancePublisher rebalancePublisher(KafkaTemplate<String, RebalanceEvent> template) { return new RebalancePublisher(template); }
    @Bean RebalanceConsumerA rebalanceConsumerA(RebalanceTracker tracker) { return new RebalanceConsumerA(tracker); }
    @Bean RebalanceConsumerB rebalanceConsumerB(RebalanceTracker tracker) { return new RebalanceConsumerB(tracker); }
    @Bean RebalanceConsumerControl rebalanceControl(KafkaListenerEndpointRegistry registry) { return new RebalanceConsumerControl(registry); }
    @Bean RebalanceExperiment rebalanceExperiment(RebalancePublisher publisher, RebalanceTracker tracker,
            RebalanceConsumerControl control, @Value("${app.kafka.topics.rebalance-join}") String topic) {
        return new RebalanceExperiment(publisher, tracker, control, topic);
    }
}
