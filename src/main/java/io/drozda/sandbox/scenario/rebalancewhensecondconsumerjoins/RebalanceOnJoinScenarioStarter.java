package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins;

import java.util.Comparator; import java.util.List; import java.util.Map;
import org.springframework.stereotype.Component;
import io.drozda.sandbox.mediator.ScenarioCommand;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app.*;
import io.drozda.sandbox.scenario.spi.ScenarioStarter;
import io.drozda.sandbox.visualization.*;

@Component
public class RebalanceOnJoinScenarioStarter implements ScenarioStarter {
    public static final String OBSERVE_REBALANCE="observe-rebalance"; private static final long DELAY=350;
    private final ScenarioCatalog catalog; private final ScenarioRuntimeService runtime; private final RebalanceOnJoinEnvironment environment;
    public RebalanceOnJoinScenarioStarter(ScenarioCatalog catalog,ScenarioRuntimeService runtime,RebalanceOnJoinEnvironment environment){this.catalog=catalog;this.runtime=runtime;this.environment=environment;}
    @Override public String scenarioId(){return "consumer-group-rebalance-join";}
    @Override public List<ScenarioCommand> commands(){return List.of(new ScenarioCommand(OBSERVE_REBALANCE,"Observe Rebalance","Start Consumer A, then join Consumer B and observe partition movement."));}
    @Override public ActiveScenarioRuntimeState execute(String commandId,String name){
        if(!OBSERVE_REBALANCE.equals(commandId))throw new IllegalArgumentException("Unknown command: "+commandId);
        ScenarioGraph scenario=catalog.scenarioById(scenarioId());runtime.startActiveSession(scenario,name);
        RebalanceScenarioStatus result=environment.observe(name);
        if(!result.rebalanced()){event(scenario,"component-failed","coordinator",result.error(),null,null,"FAILED",null);return runtime.completeActiveSession(scenario);}
        runtime.updateActiveStep(scenario,1);event(scenario,"component-ready","consumer-a","Consumer A joined",null,null,"READY",null);
        result.initialOwners().forEach((partition,owner)->assignment(scenario,partition,owner,"initial-p"+partition));
        runtime.updateActiveStep(scenario,2);animate(scenario,result,"BEFORE JOIN");
        runtime.updateActiveStep(scenario,3);event(scenario,"component-busy","coordinator","Consumer B joined: rebalance",null,null,"BUSY",null);pause();event(scenario,"component-ready","consumer-b","Consumer B joined",null,null,"READY",null);
        runtime.updateActiveStep(scenario,4);result.finalOwners().forEach((partition,owner)->assignment(scenario,partition,owner,"final-p"+partition));
        runtime.updateNodeDetail(scenario,"consumer-a","owns "+owned(result.finalOwners(),"Consumer A"));runtime.updateNodeDetail(scenario,"consumer-b","owns "+owned(result.finalOwners(),"Consumer B"));
        runtime.updateActiveStep(scenario,5);animate(scenario,result,"AFTER JOIN");runtime.updateNodeDetail(scenario,"coordinator",String.join(" | ",result.membershipEvents()));event(scenario,"component-ready","coordinator","Stable assignment after rebalance",null,null,"READY",null);
        return runtime.completeActiveSession(scenario);
    }
    private void animate(ScenarioGraph s,RebalanceScenarioStatus r,String phase){r.observations().stream().filter(v->phase.equals(v.phase())).sorted(Comparator.comparingInt(RebalanceObservation::partition)).forEach(v->{signal(s,"producer","partition-"+v.partition(),"append @"+v.offset());signal(s,"partition-"+v.partition(),node(v.consumer()),v.consumer()+" consumed @"+v.offset());});}
    private void assignment(ScenarioGraph s,int partition,String owner,String group){event(s,"signal-started",null,"p"+partition+" → "+owner,"partition-"+partition,node(owner),"READY",group);}
    private String owned(Map<Integer,String> owners,String consumer){return owners.entrySet().stream().filter(e->consumer.equals(e.getValue())).map(e->"P"+e.getKey()).sorted().toList().toString();}
    private String node(String owner){return "Consumer A".equals(owner)?"consumer-a":"consumer-b";}
    private void signal(ScenarioGraph s,String from,String to,String label){event(s,"signal-started",null,label,from,to,"ACTIVE",null);pause();event(s,"signal-delivered",null,label,from,to,"READY",null);}
    private void event(ScenarioGraph s,String type,String node,String label,String from,String to,String status,String group){runtime.applyRuntimeEvent(s,new RuntimeEventRequest(s.id(),type,node,null,label,from,to,status,group));}
    private void pause(){try{Thread.sleep(DELAY);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}
