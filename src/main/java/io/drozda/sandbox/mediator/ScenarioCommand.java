package io.drozda.sandbox.mediator;

public record ScenarioCommand(
        String id,
        String label,
        String description
) {
}
