package io.drozda.sandbox.scenario.topicpartitionoffsets.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.topicpartitionoffsets.consumer.PartitionedEventListener;
import io.drozda.sandbox.scenario.topicpartitionoffsets.model.PartitionedEvent;
import io.drozda.sandbox.scenario.topicpartitionoffsets.producer.PartitionedEventPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.topic-partition-offsets.enabled", havingValue = "true")
@Import(TopicPartitionOffsetsScenarioController.class)
public class TopicPartitionOffsetsScenarioApplication {

    @Bean
    NewTopic partitionOffsetsTopic(@Value("${app.kafka.topics.partition-offsets}") String topic) {
        return TopicBuilder.name(topic).partitions(2).replicas(1).build();
    }

    @Bean
    PartitionedEventTracker partitionedEventTracker() {
        return new PartitionedEventTracker();
    }

    @Bean
    PartitionedEventPublisher partitionedEventPublisher(
            KafkaTemplate<String, PartitionedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.partition-offsets}") String topic
    ) {
        return new PartitionedEventPublisher(kafkaTemplate, topic);
    }

    @Bean
    PartitionedEventListener partitionedEventListener(PartitionedEventTracker tracker) {
        return new PartitionedEventListener(tracker);
    }

    @Bean
    TopicPartitionOffsetsExperiment topicPartitionOffsetsExperiment(
            PartitionedEventPublisher publisher,
            PartitionedEventTracker tracker,
            @Value("${app.kafka.topics.partition-offsets}") String topic
    ) {
        return new TopicPartitionOffsetsExperiment(publisher, tracker, topic);
    }
}
