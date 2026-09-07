package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.consumer.GlobalOrderListener;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.model.GlobalOrderEvent;
import io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.producer.GlobalOrderPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.global-ordering.enabled", havingValue = "true")
@Import(GlobalOrderingScenarioController.class)
public class GlobalOrderingScenarioApplication {
    @Bean
    NewTopic globalOrderingSingleTopic(
            @Value("${app.kafka.topics.global-ordering-single}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic globalOrderingParallelTopic(
            @Value("${app.kafka.topics.global-ordering-parallel}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(2).replicas(1).build();
    }

    @Bean GlobalOrderTracker globalOrderTracker() { return new GlobalOrderTracker(); }

    @Bean GlobalOrderPublisher globalOrderPublisher(KafkaTemplate<String, GlobalOrderEvent> template) {
        return new GlobalOrderPublisher(template);
    }

    @Bean GlobalOrderListener globalOrderListener(GlobalOrderTracker tracker) {
        return new GlobalOrderListener(tracker);
    }

    @Bean
    GlobalOrderingExperiment globalOrderingExperiment(
            GlobalOrderPublisher publisher,
            GlobalOrderTracker tracker,
            @Value("${app.kafka.topics.global-ordering-single}") String singleTopic,
            @Value("${app.kafka.topics.global-ordering-parallel}") String parallelTopic
    ) {
        return new GlobalOrderingExperiment(publisher, tracker, singleTopic, parallelTopic);
    }
}
