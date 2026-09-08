package io.drozda.sandbox.basic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.scenario.systemready.SystemReadyProbe;
import io.drozda.sandbox.scenario.systemready.SystemReadyScenarioStarter;
import io.drozda.sandbox.scenario.systemready.app.SystemReadyScenarioApplication;
import io.drozda.sandbox.scenario.systemready.app.SystemReadyScenarioController;
import io.drozda.sandbox.scenario.systemready.consumer.TradeEventListener;
import io.drozda.sandbox.scenario.systemready.producer.TradeEventPublisher;

@SpringBootTest
class AllComponentsTest {
    @Autowired ApplicationContext context;
    @Autowired ScenarioMediatorService mediator;

    @Test
    void shouldKeepScenarioBeansOutOfPlatformAndSupportReadinessRestart() {
        assertTrue(context.getBeansOfType(TradeEventPublisher.class).isEmpty());
        assertTrue(context.getBeansOfType(TradeEventListener.class).isEmpty());
        assertTrue(context.getBeansOfType(SystemReadyProbe.class).isEmpty());
        assertTrue(context.getBeansOfType(SystemReadyScenarioController.class).isEmpty());
        assertTrue(context.getBeansOfType(SystemReadyScenarioApplication.class).isEmpty());
        assertTrue(context.getBean(KafkaListenerEndpointRegistry.class).getListenerContainers().isEmpty());
        try {
            for (int run = 0; run < 2; run++) {
                var status = mediator.startEnvironment("system-ready");
                assertTrue(status.publisherReady());
                assertTrue(status.listenerReady());
                assertTrue(status.kafkaTemplateReady());
                var runtime = mediator.execute("system-ready",
                        SystemReadyScenarioStarter.BASELINE_READINESS, "Readiness isolation test");
                assertEquals("READY", runtime.nodeStatuses().get("publisher"));
                assertEquals("READY", runtime.nodeStatuses().get("listener"));
                assertFalse(runtime.nodeStatuses().containsKey("kafka"));
                assertTrue(runtime.completed());
                assertFalse(runtime.active());
                assertEquals("STOPPED", mediator.stopEnvironment("system-ready").lifecycleState());
            }
        } finally {
            mediator.stopEnvironment("system-ready");
        }
    }

    @Test
    void readinessContextShouldCreateButNotStartItsKafkaListener() {
        try (var scenario = new org.springframework.boot.builder.SpringApplicationBuilder(SystemReadyScenarioApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .run("--scenario.system-ready.enabled=true",
                        "--app.kafka.topics.system-ready=readiness-wiring-test",
                        "--spring.kafka.consumer.group-id=readiness-wiring-test",
                        "--spring.kafka.bootstrap-servers=localhost:1")) {
            assertNotNull(scenario.getBean(TradeEventPublisher.class));
            assertNotNull(scenario.getBean(TradeEventListener.class));
            assertNotNull(scenario.getBean(SystemReadyProbe.class));
            var containers = scenario.getBean(KafkaListenerEndpointRegistry.class).getListenerContainers();
            assertEquals(1, containers.size());
            assertTrue(containers.stream().noneMatch(container -> container.isRunning()));
            assertNull(scenario.getParent());
        }
    }

}
