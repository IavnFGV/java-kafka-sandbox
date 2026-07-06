package io.drozda.sandbox.visualization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioGraphController {

    private final ScenarioCatalog scenarioCatalog;
    private final ScenarioRuntimeService scenarioRuntimeService;

    public ScenarioGraphController(ScenarioCatalog scenarioCatalog, ScenarioRuntimeService scenarioRuntimeService) {
        this.scenarioCatalog = scenarioCatalog;
        this.scenarioRuntimeService = scenarioRuntimeService;
    }

    @GetMapping("/{scenarioId}")
    public ScenarioGraph scenario(@PathVariable String scenarioId) {
        return scenarioCatalog.scenarioById(scenarioId);
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
}
