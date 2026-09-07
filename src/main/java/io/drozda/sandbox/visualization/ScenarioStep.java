package io.drozda.sandbox.visualization;

import java.util.List;

public record ScenarioStep(
        String id,
        String title,
        String description,
        List<String> eventIds
) {
}
