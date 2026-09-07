package io.drozda.sandbox.scenario.globalordering.app;

public record GlobalOrderingCommandRequest(
        String invocationName,
        GlobalOrderingMode mode
) {
}
