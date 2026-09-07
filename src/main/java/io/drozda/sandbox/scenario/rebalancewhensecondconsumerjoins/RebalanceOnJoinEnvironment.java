package io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins;

import java.util.UUID;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.drozda.sandbox.mediator.ScenarioEnvironmentStatus;
import io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.app.*;
import io.drozda.sandbox.scenario.spi.ScenarioEnvironment;

@Component
public class RebalanceOnJoinEnvironment implements ScenarioEnvironment {
    private final RestClient.Builder builder; private volatile ConfigurableApplicationContext context; private volatile String baseUrl;
    public RebalanceOnJoinEnvironment(RestClient.Builder builder){this.builder=builder;}
    @Override public String scenarioId(){return "consumer-group-rebalance-join";}
    @Override public synchronized ScenarioEnvironmentStatus start(){
        if(context==null){String id=UUID.randomUUID().toString();context=new SpringApplicationBuilder(RebalanceScenarioApplication.class)
                .web(WebApplicationType.SERVLET).run("--scenario.rebalance-join.enabled=true","--server.port=0",
                        "--spring.application.name=rebalance-join-scenario-app",
                        "--spring.kafka.consumer.properties.spring.json.trusted.packages=io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model",
                        "--spring.kafka.consumer.properties.spring.json.value.default.type=io.drozda.sandbox.scenario.rebalancewhensecondconsumerjoins.model.RebalanceEvent",
                        "--app.kafka.topics.rebalance-join=scenario-011-rebalance-"+id,
                        "--app.kafka.groups.rebalance-join=scenario-011-group-"+id);
            baseUrl="http://localhost:"+((WebServerApplicationContext)context).getWebServer().getPort();}
        return convert("STARTED","Started isolated rebalance app.",fetch());}
    @Override public synchronized ScenarioEnvironmentStatus stop(){if(context!=null){context.close();context=null;baseUrl=null;}return stopped("Stopped isolated rebalance app.");}
    @Override public synchronized ScenarioEnvironmentStatus reset(){if(context==null)return start();return convert("RESET","Reset rebalance state.",client().post().uri("/internal/rebalance-join/reset").retrieve().body(RebalanceScenarioStatus.class));}
    @Override public synchronized ScenarioEnvironmentStatus status(){return context==null?stopped("Rebalance app is not running."):convert("STARTED","Rebalance app is running.",fetch());}
    public synchronized RebalanceScenarioStatus observe(String name){if(context!=null)stop();start();return client().post().uri("/internal/rebalance-join/commands/{id}",RebalanceOnJoinScenarioStarter.OBSERVE_REBALANCE).body(new RebalanceCommandRequest(name)).retrieve().body(RebalanceScenarioStatus.class);}
    private RebalanceScenarioStatus fetch(){return client().get().uri("/internal/rebalance-join/status").retrieve().body(RebalanceScenarioStatus.class);}
    private RestClient client(){return builder.baseUrl(baseUrl).build();}
    private ScenarioEnvironmentStatus convert(String lifecycle,String detail,RebalanceScenarioStatus value){return new ScenarioEnvironmentStatus(scenarioId(),lifecycle,detail,value.publisherReady(),value.listenerReady(),value.kafkaTemplateReady());}
    private ScenarioEnvironmentStatus stopped(String detail){return new ScenarioEnvironmentStatus(scenarioId(),"STOPPED",detail,false,false,false);}
}
