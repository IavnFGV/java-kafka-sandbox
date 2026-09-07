package io.drozda.sandbox.visualization;

public record ScenarioTimelineEvent(
        long sequence,
        ActiveScenarioRuntimeState before,
        ActiveScenarioRuntimeState after,
        boolean visibleInTimeline,
        boolean animated
) {
}
