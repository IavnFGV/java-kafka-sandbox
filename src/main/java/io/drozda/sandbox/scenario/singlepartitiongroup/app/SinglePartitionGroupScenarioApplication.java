package io.drozda.sandbox.scenario.singlepartitiongroup.app;

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

import io.drozda.sandbox.scenario.singlepartitiongroup.consumer.ConsumerGroupMemberA;
import io.drozda.sandbox.scenario.singlepartitiongroup.consumer.ConsumerGroupMemberB;
import io.drozda.sandbox.scenario.singlepartitiongroup.model.GroupWorkEvent;
import io.drozda.sandbox.scenario.singlepartitiongroup.producer.GroupWorkPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.single-partition-group.enabled", havingValue = "true")
@Import(SinglePartitionGroupScenarioController.class)
public class SinglePartitionGroupScenarioApplication {
    @Bean
    NewTopic singlePartitionGroupTopic(
            @Value("${app.kafka.topics.single-partition-group}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean SinglePartitionGroupTracker singlePartitionGroupTracker() {
        return new SinglePartitionGroupTracker();
    }

    @Bean GroupWorkPublisher groupWorkPublisher(KafkaTemplate<String, GroupWorkEvent> template) {
        return new GroupWorkPublisher(template);
    }

    @Bean ConsumerGroupMemberA consumerGroupMemberA(SinglePartitionGroupTracker tracker) {
        return new ConsumerGroupMemberA(tracker);
    }

    @Bean ConsumerGroupMemberB consumerGroupMemberB(SinglePartitionGroupTracker tracker) {
        return new ConsumerGroupMemberB(tracker);
    }

    @Bean ConsumerMemberControl consumerMemberControl(KafkaListenerEndpointRegistry registry) {
        return new ConsumerMemberControl(registry);
    }

    @Bean
    SinglePartitionGroupExperiment singlePartitionGroupExperiment(
            GroupWorkPublisher publisher,
            SinglePartitionGroupTracker tracker,
            ConsumerMemberControl memberControl,
            @Value("${app.kafka.topics.single-partition-group}") String topic
    ) {
        return new SinglePartitionGroupExperiment(publisher, tracker, memberControl, topic);
    }
}
