package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest;
import io.drozda.sandbox.mediator.ScenarioMediatorService; import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app.RebalanceScenarioStatus; import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties="spring.kafka.consumer.group-id=rebalance-host-test-${random.uuid}")
class RebalanceOnJoinScenarioTest {
    @Autowired RebalanceOnJoinEnvironment environment; @Autowired ScenarioMediatorService mediator;
    @Test void shouldMoveFromOneOwnerToTwoOwnersWhenSecondConsumerJoins(){try{
        RebalanceScenarioStatus result=environment.observe("rebalance-test");assertTrue(result.rebalanced());assertEquals(2,result.initialOwners().size());assertEquals(1,result.initialOwners().values().stream().distinct().count());assertEquals(2,result.finalOwners().values().stream().distinct().count());assertTrue(result.membershipEvents().stream().anyMatch(v->v.contains("revoked")));
        ActiveScenarioRuntimeState runtime=mediator.execute("consumer-group-rebalance-join",RebalanceOnJoinScenarioStarter.OBSERVE_REBALANCE,"visual-rebalance-test");assertTrue(runtime.completed());assertEquals("READY",runtime.nodeStatuses().get("coordinator"));
    }finally{environment.stop();}}
}
