package io.drozda.sandbox.scenario.consumerfailureandpartitiontakeover.app;
import org.apache.kafka.clients.consumer.ConsumerRecord; import io.drozda.sandbox.scenario.consumerfailureandpartitiontakeover.model.TakeoverEvent;
public record TrackedTakeoverRecord(ConsumerRecord<String,TakeoverEvent> record,String consumer) { }
