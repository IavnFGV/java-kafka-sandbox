package io.drozda.sandbox.scenario.multipleconsumergroups.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import io.drozda.sandbox.scenario.multipleconsumergroups.consumer.AuditGroupConsumer;
import io.drozda.sandbox.scenario.multipleconsumergroups.consumer.NotificationGroupConsumer;
import io.drozda.sandbox.scenario.multipleconsumergroups.model.SharedOrderEvent;
import io.drozda.sandbox.scenario.multipleconsumergroups.producer.SharedOrderPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.multiple-consumer-groups.enabled", havingValue = "true")
@Import(MultipleConsumerGroupsScenarioController.class)
public class MultipleConsumerGroupsScenarioApplication {
    @Bean NewTopic multipleGroupsTopic(@Value("${app.kafka.topics.multiple-groups}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
    @Bean MultipleConsumerGroupsTracker multipleConsumerGroupsTracker() {
        return new MultipleConsumerGroupsTracker();
    }
    @Bean SharedOrderPublisher sharedOrderPublisher(KafkaTemplate<String, SharedOrderEvent> template) {
        return new SharedOrderPublisher(template);
    }
    @Bean AuditGroupConsumer auditGroupConsumer(MultipleConsumerGroupsTracker tracker) {
        return new AuditGroupConsumer(tracker);
    }
    @Bean NotificationGroupConsumer notificationGroupConsumer(MultipleConsumerGroupsTracker tracker) {
        return new NotificationGroupConsumer(tracker);
    }
    @Bean MultipleConsumerGroupsExperiment multipleConsumerGroupsExperiment(
            SharedOrderPublisher publisher, MultipleConsumerGroupsTracker tracker,
            @Value("${app.kafka.topics.multiple-groups}") String topic) {
        return new MultipleConsumerGroupsExperiment(publisher, tracker, topic);
    }
}
