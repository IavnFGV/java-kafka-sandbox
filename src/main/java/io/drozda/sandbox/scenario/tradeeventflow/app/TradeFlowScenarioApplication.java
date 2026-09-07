package io.drozda.sandbox.scenario.tradeeventflow.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.tradeeventflow.consumer.TradeFlowListener;
import io.drozda.sandbox.scenario.tradeeventflow.model.TradeFlowEvent;
import io.drozda.sandbox.scenario.tradeeventflow.producer.TradeFlowPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.trade-flow.enabled", havingValue = "true")
@Import(TradeFlowScenarioController.class)
public class TradeFlowScenarioApplication {

    @Bean
    NewTopic tradeFlowTopic(@Value("${app.kafka.topics.trade-flow}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    TradeFlowEventTracker tradeFlowEventTracker() {
        return new TradeFlowEventTracker();
    }

    @Bean
    TradeFlowPublisher tradeFlowPublisher(
            KafkaTemplate<String, TradeFlowEvent> kafkaTemplate,
            @Value("${app.kafka.topics.trade-flow}") String topic
    ) {
        return new TradeFlowPublisher(kafkaTemplate, topic);
    }

    @Bean
    TradeFlowListener tradeFlowListener(TradeFlowEventTracker tracker) {
        return new TradeFlowListener(tracker);
    }

    @Bean
    TradeFlowExperiment tradeFlowExperiment(
            TradeFlowPublisher publisher,
            TradeFlowEventTracker tracker,
            @Value("${app.kafka.topics.trade-flow}") String topic
    ) {
        return new TradeFlowExperiment(publisher, tracker, topic);
    }
}
