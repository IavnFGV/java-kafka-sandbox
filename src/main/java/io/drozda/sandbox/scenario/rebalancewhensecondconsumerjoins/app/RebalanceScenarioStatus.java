package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

import java.util.List;
import java.util.Map;

public record RebalanceScenarioStatus(String scenarioId, String invocationName,
        boolean publisherReady, boolean listenerReady, boolean kafkaTemplateReady,
        boolean published, boolean rebalanced, Map<Integer, String> initialOwners,
        Map<Integer, String> finalOwners, List<RebalanceObservation> observations,
        List<String> membershipEvents, String topic, String error) {
}
