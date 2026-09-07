package io.drozda.sandbox.scenario.consumerfailureandpartitiontakeover.app;
import java.util.*;
public record TakeoverScenarioStatus(String scenarioId,String invocationName,boolean publisherReady,boolean listenerReady,boolean kafkaTemplateReady,boolean published,boolean takenOver,String failedConsumer,String survivor,int orphanedPartition,Map<Integer,String> initialOwners,Map<Integer,String> finalOwners,List<TakeoverObservation> observations,List<String> membershipEvents,String topic,String error) { }
