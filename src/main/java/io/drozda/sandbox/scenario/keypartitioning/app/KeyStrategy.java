package io.drozda.sandbox.scenario.keypartitioning.app;

public enum KeyStrategy {
    NO_KEY,
    EVENT_ID,
    ORDER_ID;

    public static KeyStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return NO_KEY;
        }
        return KeyStrategy.valueOf(value.trim().toUpperCase());
    }
}
