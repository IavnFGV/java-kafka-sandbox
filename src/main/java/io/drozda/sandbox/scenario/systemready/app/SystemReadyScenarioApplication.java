package io.drozda.sandbox.scenario.systemready.app;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import io.drozda.sandbox.scenario.systemready.consumer.TradeEventListener;
import io.drozda.sandbox.scenario.systemready.producer.TradeEventPublisher;
import io.drozda.sandbox.scenario.systemready.SystemReadyProbe;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "scenario.system-ready.enabled", havingValue = "true")
@EnableAutoConfiguration
@Import({
        TradeEventPublisher.class,
        TradeEventListener.class,
        SystemReadyProbe.class,
        SystemReadyScenarioController.class
})
public class SystemReadyScenarioApplication {
}
