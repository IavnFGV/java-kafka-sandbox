package io.drozda.sandbox.visualization;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.mediator.ScenarioCommandRequest;
import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.mediator.ScenarioMediatorService;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioGraphController {

    private static final long DEFAULT_RUNTIME_UPDATE_TIMEOUT_MS = 120_000L;
    private static final long MIN_RUNTIME_UPDATE_TIMEOUT_MS = 1_000L;
    private static final long MAX_RUNTIME_UPDATE_TIMEOUT_MS = 120_000L;

    private final ScenarioCatalog scenarioCatalog;
    private final ScenarioRuntimeService scenarioRuntimeService;
    private final ScenarioMediatorService scenarioMediatorService;

    public ScenarioGraphController(
            ScenarioCatalog scenarioCatalog,
            ScenarioRuntimeService scenarioRuntimeService,
            ScenarioMediatorService scenarioMediatorService
    ) {
        this.scenarioCatalog = scenarioCatalog;
        this.scenarioRuntimeService = scenarioRuntimeService;
        this.scenarioMediatorService = scenarioMediatorService;
    }

    @GetMapping
    public List<ScenarioGraph> scenarios() {
        return scenarioCatalog.scenarios();
    }

    @GetMapping("/{scenarioId}")
    public ScenarioGraph scenario(@PathVariable String scenarioId) {
        return scenarioCatalog.scenarioById(scenarioId);
    }

    @GetMapping("/{scenarioId}/commands")
    public List<ScenarioCommand> scenarioCommands(@PathVariable String scenarioId) {
        return scenarioMediatorService.commandsFor(scenarioId);
    }

    @GetMapping("/{scenarioId}/environment")
    public ScenarioEnvironmentStatus scenarioEnvironment(@PathVariable String scenarioId) {
        return scenarioMediatorService.environmentStatus(scenarioId);
    }

    @PostMapping("/{scenarioId}/environment/start")
    public ScenarioEnvironmentStatus startScenarioEnvironment(@PathVariable String scenarioId) {
        return scenarioMediatorService.startEnvironment(scenarioId);
    }

    @PostMapping("/{scenarioId}/environment/stop")
    public ScenarioEnvironmentStatus stopScenarioEnvironment(@PathVariable String scenarioId) {
        ScenarioGraph scenario = scenarioCatalog.scenarioById(scenarioId);
        ScenarioEnvironmentStatus status = scenarioMediatorService.stopEnvironment(scenarioId);
        scenarioRuntimeService.clearActiveSession(scenario);
        scenarioRuntimeService.reset(scenario);
        return status;
    }

    @PostMapping("/{scenarioId}/environment/reset")
    public ScenarioEnvironmentStatus resetScenarioEnvironment(@PathVariable String scenarioId) {
        return scenarioMediatorService.resetEnvironment(scenarioId);
    }

    @GetMapping("/{scenarioId}/runtime")
    public ScenarioRuntimeState scenarioRuntime(@PathVariable String scenarioId) {
        return scenarioRuntimeService.getState(scenarioCatalog.scenarioById(scenarioId));
    }

    @PostMapping("/{scenarioId}/runtime/next")
    public ScenarioRuntimeState nextStep(@PathVariable String scenarioId) {
        return scenarioRuntimeService.next(scenarioCatalog.scenarioById(scenarioId));
    }

    @PostMapping("/{scenarioId}/runtime/previous")
    public ScenarioRuntimeState previousStep(@PathVariable String scenarioId) {
        return scenarioRuntimeService.previous(scenarioCatalog.scenarioById(scenarioId));
    }

    @PostMapping("/{scenarioId}/runtime/reset")
    public ScenarioRuntimeState resetStep(@PathVariable String scenarioId) {
        return scenarioRuntimeService.reset(scenarioCatalog.scenarioById(scenarioId));
    }

    @GetMapping("/runtime/active")
    public ActiveScenarioRuntimeState activeRuntime() {
        return scenarioRuntimeService.activeRuntime();
    }

    @GetMapping("/runtime/updates")
    public DeferredResult<ScenarioRuntimeUpdate> runtimeUpdates(
            @RequestParam(defaultValue = "-1") long after,
            @RequestParam(defaultValue = "120000") long timeoutMs
    ) {
        long requestedTimeout = timeoutMs > 0 ? timeoutMs : DEFAULT_RUNTIME_UPDATE_TIMEOUT_MS;
        long boundedTimeout = Math.max(
                MIN_RUNTIME_UPDATE_TIMEOUT_MS,
                Math.min(requestedTimeout, MAX_RUNTIME_UPDATE_TIMEOUT_MS)
        );
        return scenarioRuntimeService.awaitRuntimeUpdate(after, boundedTimeout);
    }

    @PostMapping("/runtime/session/start")
    public ActiveScenarioRuntimeState startRuntimeSession(@RequestBody ActiveScenarioSessionRequest request) {
        return scenarioRuntimeService.startActiveSession(
                scenarioCatalog.scenarioById(request.scenarioId()),
                request.testName()
        );
    }

    @PostMapping("/runtime/session/step")
    public ActiveScenarioRuntimeState updateRuntimeStep(@RequestBody ActiveScenarioStepRequest request) {
        return scenarioRuntimeService.updateActiveStep(
                scenarioCatalog.scenarioById(request.scenarioId()),
                request.stepIndex()
        );
    }

    @PostMapping("/runtime/session/complete")
    public ActiveScenarioRuntimeState completeRuntimeSession(@RequestBody ActiveScenarioSessionRequest request) {
        return scenarioRuntimeService.completeActiveSession(scenarioCatalog.scenarioById(request.scenarioId()));
    }

    @PostMapping("/runtime/event")
    public ActiveScenarioRuntimeState applyRuntimeEvent(@RequestBody RuntimeEventRequest request) {
        return scenarioRuntimeService.applyRuntimeEvent(
                scenarioCatalog.scenarioById(request.scenarioId()),
                request
        );
    }

    @PostMapping("/{scenarioId}/commands/{commandId}")
    public ActiveScenarioRuntimeState executeScenarioCommand(
            @PathVariable String scenarioId,
            @PathVariable String commandId,
            @RequestBody(required = false) ScenarioCommandRequest request
    ) {
        return scenarioMediatorService.execute(
                scenarioId,
                commandId,
                request != null ? request.invocationName() : null
        );
    }

    @PostMapping("/{scenarioId}/run")
    public ActiveScenarioRuntimeState runScenario(
            @PathVariable String scenarioId,
            @RequestBody(required = false) ScenarioCommandRequest request
    ) {
        return scenarioMediatorService.runDefault(
                scenarioId,
                request != null ? request.invocationName() : null
        );
    }
}
