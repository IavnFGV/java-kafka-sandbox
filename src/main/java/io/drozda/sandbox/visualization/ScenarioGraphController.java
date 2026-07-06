package io.drozda.sandbox.visualization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/trade-flow")
    public ScenarioGraph tradeFlowScenario() {
        return scenarioCatalog.tradeFlowScenario();
    }

    @GetMapping("/trade-flow/runtime")
    public ScenarioRuntimeState tradeFlowRuntime() {
        return scenarioRuntimeService.getState(scenarioCatalog.tradeFlowScenario());
    }

    @PostMapping("/trade-flow/runtime/next")
    public ScenarioRuntimeState nextTradeFlowStep() {
        return scenarioRuntimeService.next(scenarioCatalog.tradeFlowScenario());
    }

    @PostMapping("/trade-flow/runtime/previous")
    public ScenarioRuntimeState previousTradeFlowStep() {
        return scenarioRuntimeService.previous(scenarioCatalog.tradeFlowScenario());
    }

    @PostMapping("/trade-flow/runtime/reset")
    public ScenarioRuntimeState resetTradeFlowStep() {
        return scenarioRuntimeService.reset(scenarioCatalog.tradeFlowScenario());
    }
}
