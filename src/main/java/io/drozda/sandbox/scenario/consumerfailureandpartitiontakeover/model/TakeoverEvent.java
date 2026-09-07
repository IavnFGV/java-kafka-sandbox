package io.drozda.sandbox.scenario.consumerfailureandpartitiontakeover.model;
public record TakeoverEvent(String eventId,String phase,int partition,int sequence,long createdAt) { }
