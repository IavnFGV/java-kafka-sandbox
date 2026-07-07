package io.drozda.sandbox.scenario.systemready.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.drozda.sandbox.scenario.systemready.SystemReadyProbe;
import io.drozda.sandbox.scenario.systemready.SystemReadyScenarioStarter;

@RestController
@RequestMapping("/internal/system-ready")
public class SystemReadyScenarioController {

    private final SystemReadyProbe systemReadyProbe;

    public SystemReadyScenarioController(SystemReadyProbe systemReadyProbe) {
        this.systemReadyProbe = systemReadyProbe;
    }

    @GetMapping("/status")
    public SystemReadyScenarioStatus status() {
        return baselineStatus("status");
    }

    @PostMapping("/reset")
    public SystemReadyScenarioStatus reset() {
        return baselineStatus("reset");
    }

    @PostMapping("/commands/{commandId}")
    public SystemReadyScenarioStatus execute(
            @PathVariable String commandId,
            @RequestBody(required = false) SystemReadyScenarioCommandRequest request
    ) {
        if (!SystemReadyScenarioStarter.BASELINE_READINESS.equals(commandId)) {
            throw new IllegalArgumentException("Unknown command for system-ready scenario app: " + commandId);
        }

        String invocationName = request != null && request.invocationName() != null && !request.invocationName().isBlank()
                ? request.invocationName().trim()
                : "baseline-readiness";
        return baselineStatus(invocationName);
    }

    private SystemReadyScenarioStatus baselineStatus(String invocationName) {
        return new SystemReadyScenarioStatus(
                "system-ready",
                invocationName,
                systemReadyProbe.publisherReady(),
                systemReadyProbe.listenerReady(),
                systemReadyProbe.kafkaTemplateReady()
        );
    }
}
