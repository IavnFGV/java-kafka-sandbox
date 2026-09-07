package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app;

import java.util.List; import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.RebalanceOnJoinScenarioStarter;

@RestController @RequestMapping("/internal/rebalance-join")
@ConditionalOnProperty(name="scenario.rebalance-join.enabled", havingValue="true")
public class RebalanceScenarioController {
    private final RebalanceExperiment experiment; private final RebalanceTracker tracker; private final String topic;
    public RebalanceScenarioController(RebalanceExperiment experiment, RebalanceTracker tracker,
            @Value("${app.kafka.topics.rebalance-join}") String topic) { this.experiment=experiment;this.tracker=tracker;this.topic=topic; }
    @GetMapping("/status") public RebalanceScenarioStatus status(){return ready("status");}
    @PostMapping("/reset") public RebalanceScenarioStatus reset(){tracker.reset();return ready("reset");}
    @PostMapping("/commands/{id}") public RebalanceScenarioStatus execute(@PathVariable String id,
            @RequestBody(required=false) RebalanceCommandRequest request) {
        if(!RebalanceOnJoinScenarioStarter.OBSERVE_REBALANCE.equals(id)) throw new IllegalArgumentException("Unknown command: "+id);
        return experiment.run(request!=null&&request.invocationName()!=null?request.invocationName():id);
    }
    private RebalanceScenarioStatus ready(String name){return new RebalanceScenarioStatus("consumer-group-rebalance-join",name,
            true,true,true,false,false,Map.of(),Map.of(),List.of(),List.of(),topic,null);}
}
