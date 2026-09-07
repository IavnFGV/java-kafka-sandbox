package io.drozda.sandbox.scenario.parallelorderswithoutglobalordering.app;

public record GlobalOrderingCommandRequest(
        String invocationName,
        GlobalOrderingMode mode
) {
}
