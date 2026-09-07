package io.drozda.sandbox.visualization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ScenarioSourceReferenceTest {
    private final ScenarioCatalog catalog = new ScenarioCatalog();

    @Test
    void shouldExposeAnExistingSourceDirectoryForEveryScenario() {
        assertFalse(catalog.scenarios().isEmpty());
        catalog.scenarios().forEach(scenario -> {
            assertFalse(scenario.sourceRoot().isBlank(), scenario.id());
            assertTrue(Files.isDirectory(Path.of(scenario.sourceRoot())),
                    () -> "Missing source root for " + scenario.id() + ": " + scenario.sourceRoot());
        });
    }
}
