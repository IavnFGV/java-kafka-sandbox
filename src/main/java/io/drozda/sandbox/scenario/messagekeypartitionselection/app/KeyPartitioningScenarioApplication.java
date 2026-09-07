package io.drozda.sandbox.scenario.messagekeypartitionselection.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.messagekeypartitionselection.consumer.KeyedOrderEventListener;
import io.drozda.sandbox.scenario.messagekeypartitionselection.model.KeyedOrderEvent;
import io.drozda.sandbox.scenario.messagekeypartitionselection.producer.KeyedOrderEventPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.key-partitioning.enabled", havingValue = "true")
@Import(KeyPartitioningScenarioController.class)
public class KeyPartitioningScenarioApplication {
    @Bean
    NewTopic keyPartitioningTopic(@Value("${app.kafka.topics.key-partitioning}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean KeyedEventTracker keyedEventTracker() { return new KeyedEventTracker(); }

    @Bean
    KeyedOrderEventPublisher keyedOrderEventPublisher(
            KafkaTemplate<String, KeyedOrderEvent> template,
            @Value("${app.kafka.topics.key-partitioning}") String topic
    ) { return new KeyedOrderEventPublisher(template, topic); }

    @Bean KeyedOrderEventListener keyedOrderEventListener(KeyedEventTracker tracker) {
        return new KeyedOrderEventListener(tracker);
    }

    @Bean
    KeyPartitioningExperiment keyPartitioningExperiment(
            KeyedOrderEventPublisher publisher, KeyedEventTracker tracker,
            @Value("${app.kafka.topics.key-partitioning}") String topic
    ) { return new KeyPartitioningExperiment(publisher, tracker, topic); }
}
