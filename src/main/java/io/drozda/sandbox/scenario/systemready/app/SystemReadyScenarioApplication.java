package io.drozda.sandbox.scenario.systemready.app;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import io.drozda.sandbox.TradeEventListener;
import io.drozda.sandbox.TradeEventPublisher;
import io.drozda.sandbox.scenario.systemready.SystemReadyProbe;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
        TradeEventPublisher.class,
        TradeEventListener.class,
        SystemReadyProbe.class,
        SystemReadyScenarioController.class
})
public class SystemReadyScenarioApplication {
}
