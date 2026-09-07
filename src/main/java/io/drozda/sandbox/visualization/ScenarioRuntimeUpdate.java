package io.drozda.sandbox.visualization;

import java.util.List;

public record ScenarioRuntimeUpdate(
        long revision,
        List<ScenarioTimelineEvent> events,
        ActiveScenarioRuntimeState runtime
) {
}
