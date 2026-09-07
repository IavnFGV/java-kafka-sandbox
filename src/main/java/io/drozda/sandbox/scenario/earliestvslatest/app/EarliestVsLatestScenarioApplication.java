package io.drozda.sandbox.scenario.earliestvslatest.app;

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

import io.drozda.sandbox.scenario.earliestvslatest.consumer.EarliestConsumer;
import io.drozda.sandbox.scenario.earliestvslatest.consumer.LatestConsumer;
import io.drozda.sandbox.scenario.earliestvslatest.model.OffsetResetEvent;
import io.drozda.sandbox.scenario.earliestvslatest.producer.OffsetResetPublisher;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConditionalOnProperty(name = "scenario.earliest-vs-latest.enabled", havingValue = "true")
@Import(EarliestVsLatestScenarioController.class)
public class EarliestVsLatestScenarioApplication {
    @Bean NewTopic offsetResetTopic(@Value("${app.kafka.topics.offset-reset}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
    @Bean OffsetResetTracker offsetResetTracker() { return new OffsetResetTracker(); }
    @Bean OffsetResetPublisher offsetResetPublisher(KafkaTemplate<String, OffsetResetEvent> template) {
        return new OffsetResetPublisher(template);
    }
    @Bean EarliestConsumer earliestConsumer(OffsetResetTracker tracker) { return new EarliestConsumer(tracker); }
    @Bean LatestConsumer latestConsumer(OffsetResetTracker tracker) { return new LatestConsumer(tracker); }
    @Bean OffsetResetConsumerControl offsetResetConsumerControl(KafkaListenerEndpointRegistry registry) {
        return new OffsetResetConsumerControl(registry);
    }
    @Bean EarliestVsLatestExperiment earliestVsLatestExperiment(OffsetResetPublisher publisher,
            OffsetResetTracker tracker, OffsetResetConsumerControl control,
            @Value("${app.kafka.topics.offset-reset}") String topic) {
        return new EarliestVsLatestExperiment(publisher, tracker, control, topic);
    }
}
