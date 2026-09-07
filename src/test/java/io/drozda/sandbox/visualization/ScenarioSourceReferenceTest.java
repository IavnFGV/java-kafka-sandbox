package io.drozda.sandbox.visualization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void everyNodeShouldLinkToExistingFilesAndNonemptySourceLines() throws Exception {
        for (ScenarioGraph scenario : catalog.scenarios()) {
            for (ScenarioNode node : scenario.nodes()) {
                String context = scenario.id() + "/" + node.id();
                assertFalse(node.sourceReferences().isEmpty(), context);
                for (ScenarioSourceReference reference : node.sourceReferences()) {
                    Path path = Path.of(reference.path());
                    assertFalse(path.isAbsolute(), context);
                    assertFalse(reference.path().contains(".."), context);
                    assertTrue(Files.isRegularFile(path), context + ": " + path);
                    assertFalse(reference.label().isBlank(), context);
                    assertFalse(reference.description().isBlank(), context);
                    var lines = Files.readAllLines(path);
                    assertTrue(reference.line() > 0 && reference.line() <= lines.size(),
                            context + ": " + reference);
                    assertFalse(lines.get(reference.line() - 1).isBlank(), context + ": " + reference);
                }
            }
        }
    }

    @Test
    void shouldDistinguishConsumerImplementationsAndLinkExternalBrokerConfiguration() {
        ScenarioGraph scenario = catalog.scenarioById("consumer-group-consumer-failure");
        assertTrue(node(scenario, "consumer-a").sourceReferences().get(0).path().endsWith("TakeoverConsumerA.java"));
        assertTrue(node(scenario, "consumer-b").sourceReferences().get(0).path().endsWith("TakeoverConsumerB.java"));
        assertEquals("docker-compose.yml", node(catalog.scenarioById("system-ready"), "kafka")
                .sourceReferences().get(0).path());
    }

    @Test
    void shouldExposeReferencesInTheApiPayload() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var json = mapper.readTree(mapper.writeValueAsString(catalog.scenarioById("trade-flow")));
        assertTrue(json.get("nodes").get(0).get("sourceReferences").isArray());
        assertTrue(json.get("nodes").get(0).get("sourceReferences").get(0).get("line").asInt() > 0);
    }

    private ScenarioNode node(ScenarioGraph scenario, String id) {
        return scenario.nodes().stream().filter(node -> node.id().equals(id)).findFirst().orElseThrow();
    }
}
