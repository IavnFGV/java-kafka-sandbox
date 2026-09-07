package io.drozda.sandbox.scenario.orderingwithinonepartition.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.orderingwithinonepartition.consumer.OrderedEventListener;
import io.drozda.sandbox.scenario.orderingwithinonepartition.model.OrderedOrderEvent;
import io.drozda.sandbox.scenario.orderingwithinonepartition.producer.OrderedEventPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.partition-ordering.enabled", havingValue = "true")
@Import(PartitionOrderingScenarioController.class)
public class PartitionOrderingScenarioApplication {
    @Bean
    NewTopic partitionOrderingTopic(@Value("${app.kafka.topics.partition-ordering}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean OrderedEventTracker orderedEventTracker() { return new OrderedEventTracker(); }

    @Bean
    OrderedEventPublisher orderedEventPublisher(
            KafkaTemplate<String, OrderedOrderEvent> template,
            @Value("${app.kafka.topics.partition-ordering}") String topic
    ) { return new OrderedEventPublisher(template, topic); }

    @Bean OrderedEventListener orderedEventListener(OrderedEventTracker tracker) {
        return new OrderedEventListener(tracker);
    }

    @Bean
    PartitionOrderingExperiment partitionOrderingExperiment(
            OrderedEventPublisher publisher, OrderedEventTracker tracker,
            @Value("${app.kafka.topics.partition-ordering}") String topic
    ) { return new PartitionOrderingExperiment(publisher, tracker, topic); }
}
