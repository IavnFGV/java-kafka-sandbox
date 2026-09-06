package io.drozda.sandbox.mediator;

import java.util.Map;

public record ScenarioCommandRequest(
        String invocationName,
        Map<String, String> parameters
) {
}
