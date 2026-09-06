package io.drozda.sandbox.scenario.topicpartitionoffsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.drozda.sandbox.mediator.ScenarioMediatorService;
import io.drozda.sandbox.visualization.ActiveScenarioRuntimeState;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=partition-offset-host-test-${random.uuid}")
class TopicPartitionOffsetsScenarioTest {

    @Autowired
    ScenarioMediatorService mediator;

    @Test
    void shouldAppendAndConsumeRecordsUsingPartitionLocalOffsets() {
        try {
            ActiveScenarioRuntimeState runtime = mediator.execute(
                    "topic-partition-offsets",
                    TopicPartitionOffsetsScenarioStarter.APPEND_AND_READ,
                    "TopicPartitionOffsetsScenarioTest"
            );

            assertEquals("READY", runtime.nodeStatuses().get("producer"));
            assertEquals("READY", runtime.nodeStatuses().get("kafka-broker"));
            assertEquals("READY", runtime.nodeStatuses().get("orders-topic-box"));
            assertEquals("READY", runtime.nodeStatuses().get("partition-0"));
            assertEquals("READY", runtime.nodeStatuses().get("partition-1"));
            assertEquals("READY", runtime.nodeStatuses().get("consumer"));
            assertEquals("READY", runtime.edgeStatuses().get("producer-p0"));
            assertEquals("READY", runtime.edgeStatuses().get("producer-p1"));
            assertEquals("READY", runtime.edgeStatuses().get("p0-consumer"));
            assertEquals("READY", runtime.edgeStatuses().get("p1-consumer"));
            assertTrue(runtime.eventLog().stream().anyMatch(line -> line.contains("Partition 0 advanced")));
            assertTrue(runtime.completed());
            assertFalse(runtime.active());
        } finally {
            mediator.stopEnvironment("topic-partition-offsets");
        }
    }
}
