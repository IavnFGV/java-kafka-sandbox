package io.drozda.sandbox.visualization.junit;

import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualScenarioExtension implements BeforeEachCallback, AfterEachCallback {

    private static final Logger log = LoggerFactory.getLogger(VisualScenarioExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        Optional<VisualScenario> onMethod = context.getTestMethod()
                .map(method -> method.getAnnotation(VisualScenario.class));

        Optional<VisualScenario> onClass = context.getTestClass()
                .map(clazz -> clazz.getAnnotation(VisualScenario.class));

        VisualScenario scenario = onMethod.orElse(onClass.orElse(null));

        if (scenario == null) {
            log.info("VISUAL JUNIT: no scenario for test {}", context.getDisplayName());
            return;
        }

        VisualScenarioTestClient.start(scenario.value(), context.getDisplayName());
        log.info("VISUAL JUNIT: test={}, scenario={}",
                context.getDisplayName(),
                scenario.value());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Optional<VisualScenario> onMethod = context.getTestMethod()
                .map(method -> method.getAnnotation(VisualScenario.class));

        Optional<VisualScenario> onClass = context.getTestClass()
                .map(clazz -> clazz.getAnnotation(VisualScenario.class));

        VisualScenario scenario = onMethod.orElse(onClass.orElse(null));
        if (scenario == null) {
            return;
        }

        VisualScenarioTestClient.complete(scenario.value(), context.getDisplayName());
    }
}
