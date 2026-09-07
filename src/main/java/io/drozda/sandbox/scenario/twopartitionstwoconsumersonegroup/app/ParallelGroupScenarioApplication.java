package io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.consumer.ParallelGroupConsumerA;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.consumer.ParallelGroupConsumerB;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.model.ParallelGroupEvent;
import io.drozda.sandbox.scenario.twopartitionstwoconsumersonegroup.producer.ParallelGroupPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.parallel-group.enabled", havingValue = "true")
@Import(ParallelGroupScenarioController.class)
public class ParallelGroupScenarioApplication {
    @Bean NewTopic parallelGroupTopic(@Value("${app.kafka.topics.parallel-group}") String topic) {
        return TopicBuilder.name(topic).partitions(2).replicas(1).build();
    }
    @Bean ParallelGroupTracker parallelGroupTracker() { return new ParallelGroupTracker(); }
    @Bean ParallelGroupPublisher parallelGroupPublisher(KafkaTemplate<String, ParallelGroupEvent> template) {
        return new ParallelGroupPublisher(template);
    }
    @Bean ParallelGroupConsumerA parallelGroupConsumerA(ParallelGroupTracker tracker) {
        return new ParallelGroupConsumerA(tracker);
    }
    @Bean ParallelGroupConsumerB parallelGroupConsumerB(ParallelGroupTracker tracker) {
        return new ParallelGroupConsumerB(tracker);
    }
    @Bean ParallelGroupExperiment parallelGroupExperiment(
            ParallelGroupPublisher publisher, ParallelGroupTracker tracker,
            @Value("${app.kafka.topics.parallel-group}") String topic) {
        return new ParallelGroupExperiment(publisher, tracker, topic);
    }
}
