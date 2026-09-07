package io.drozda.sandbox.scenario.consumerfailureandpartitiontakeover.app;
public record TakeoverObservation(String phase,int sequence,int partition,long offset,String consumer) { }
