package io.drozda.sandbox.scenario.globalordering.app;

public enum GlobalOrderingMode {
    SINGLE,
    PARALLEL;

    public static GlobalOrderingMode from(String value) {
        if (value == null || value.isBlank()) {
            return SINGLE;
        }
        return valueOf(value.trim().toUpperCase());
    }
}
