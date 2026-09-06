package io.drozda.sandbox.visualization;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ScenarioCatalog {

    public List<ScenarioGraph> scenarios() {
        return List.of(
                topicPartitionOffsetsScenario(),
                consumerGroupSinglePartitionScenario(),
                consumerGroupTwoPartitionsScenario(),
                consumerGroupRebalanceOnJoinScenario(),
                consumerGroupConsumerFailureScenario(),
                tradeFlowScenario(),
                systemReadyScenario()
        ).stream()
                .sorted((left, right) -> Integer.compare(left.order(), right.order()))
                .toList();
    }

    public ScenarioGraph scenarioById(String scenarioId) {
        return scenarios().stream()
                .filter(scenario -> scenario.id().equals(scenarioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scenario id: " + scenarioId));
    }

    public ScenarioGraph tradeFlowScenario() {
        return new ScenarioGraph(
                "trade-flow",
                2,
                "Trade Event Flow",
                "A visual walkthrough of how a trade event moves through the sandbox application. Drag nodes to refine the layout.",
                1200,
                720,
                List.of(
                        new ScenarioNode("client", "Client", "external", 70, 300, 170, 96, null,
                                "A caller triggers a trade event through the application boundary."),
                        new ScenarioNode("spring-app", "Spring Boot Application", "container", 280, 150, 420, 360, null,
                                "The application boundary groups the publisher and listener components."),
                        new ScenarioNode("publisher", "TradeEventPublisher", "service", 36, 86, 170, 96, "spring-app",
                                "The publisher prepares an event and sends it to Kafka."),
                        new ScenarioNode("listener", "TradeEventListener", "consumer", 212, 212, 170, 96, "spring-app",
                                "The listener receives the event and starts downstream handling."),
                        new ScenarioNode("kafka", "Kafka Topic", "broker", 820, 300, 190, 96, null,
                                "The trade-events topic buffers and distributes the event."),
                        new ScenarioNode("observer", "System Observer", "monitor", 820, 500, 190, 96, null,
                                "A future monitoring layer can track delivery, timing, and failures.")
                ),
                List.of(
                        new ScenarioEdge("request", "client", "publisher", "invoke"),
                        new ScenarioEdge("publish", "publisher", "kafka", "send trade event"),
                        new ScenarioEdge("consume", "kafka", "listener", "deliver event"),
                        new ScenarioEdge("inspect", "kafka", "observer", "emit telemetry"),
                        new ScenarioEdge("inspect-listener", "listener", "observer", "listener metrics")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "The client, application components, Kafka topic, and observer already exist. We start by seeing the system shape before any message moves.",
                                List.of()),
                        new ScenarioStep("step-2", "Client triggers action",
                                "An external caller starts a business action that creates a trade event.",
                                List.of("event-client-request")),
                        new ScenarioStep("step-3", "Publisher sends event",
                                "TradeEventPublisher serializes the payload and sends it to the trade-events topic.",
                                List.of("event-publish")),
                        new ScenarioStep("step-4", "Kafka stores and routes",
                                "Kafka persists the message to the topic partition and makes it available to consumers.",
                                List.of("event-kafka-route", "event-kafka-telemetry")),
                        new ScenarioStep("step-5", "Listener receives event",
                                "TradeEventListener consumes the event and begins application-side processing.",
                                List.of("event-listener-receive")),
                        new ScenarioStep("step-6", "Monitoring layer observes flow",
                                "The visualization reminds us where metrics, tracing, retries, and DLT monitoring will live.",
                                List.of("event-kafka-telemetry", "event-listener-telemetry"))
                ),
                List.of(
                        new VisualizationEvent(
                                "event-client-request",
                                "command",
                                "TradeEvent",
                                "A client-side action enters the Spring Boot application boundary.",
                                List.of("client", "spring-app", "publisher"),
                                List.of("request"),
                                "client",
                                "publisher"
                        ),
                        new VisualizationEvent(
                                "event-publish",
                                "publish",
                                "TradeEvent",
                                "TradeEventPublisher emits a trade event to Kafka.",
                                List.of("spring-app", "publisher", "kafka"),
                                List.of("publish"),
                                "publisher",
                                "kafka"
                        ),
                        new VisualizationEvent(
                                "event-kafka-route",
                                "routing",
                                "TradeEvent",
                                "Kafka routes the event toward subscribed consumers.",
                                List.of("kafka", "listener", "spring-app"),
                                List.of("consume"),
                                "kafka",
                                "listener"
                        ),
                        new VisualizationEvent(
                                "event-kafka-telemetry",
                                "telemetry",
                                "Telemetry",
                                "Kafka emits delivery telemetry that can feed monitoring.",
                                List.of("kafka", "observer"),
                                List.of("inspect"),
                                "kafka",
                                "observer"
                        ),
                        new VisualizationEvent(
                                "event-listener-receive",
                                "consume",
                                "TradeEvent",
                                "TradeEventListener receives the event inside the application container.",
                                List.of("spring-app", "listener"),
                                List.of("consume"),
                                "kafka",
                                "listener"
                        ),
                        new VisualizationEvent(
                                "event-listener-telemetry",
                                "telemetry",
                                "Telemetry",
                                "The listener can emit metrics and traces to the observer layer.",
                                List.of("listener", "observer", "spring-app"),
                                List.of("inspect-listener"),
                                "listener",
                                "observer"
                        )
                )
        );
    }

    public ScenarioGraph topicPartitionOffsetsScenario() {
        return new ScenarioGraph(
                "topic-partition-offsets",
                3,
                "Topic, Partition, Offset Basics",
                "A first Kafka mental model: a topic is split into partitions, and offsets are positions inside each partition rather than global numbers.",
                1320,
                760,
                List.of(
                        new ScenarioNode("producer", "Producer", "service", 60, 310, 170, 96, null,
                                "A producer sends records into the topic."),
                        new ScenarioNode("orders-topic-box", "orders Topic", "container", 320, 120, 470, 500, null,
                                "A topic is a logical name. Kafka actually stores records inside partitions."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 36, 64, 180, 120, "orders-topic-box",
                                "Offsets inside partition 0 start at 0 and increase as records are appended."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 250, 64, 180, 120, "orders-topic-box",
                                "Partition 1 is a separate ordered log with its own offsets."),
                        new ScenarioNode("consumer", "Consumer", "consumer", 940, 310, 180, 96, null,
                                "A consumer reads records by partition and offset."),
                        new ScenarioNode("observer", "Offset View", "monitor", 940, 520, 200, 96, null,
                                "A learner-facing observer calls out which offsets were appended where.")
                ),
                List.of(
                        new ScenarioEdge("producer-p0", "producer", "partition-0", "append record A"),
                        new ScenarioEdge("producer-p1", "producer", "partition-1", "append record B"),
                        new ScenarioEdge("p0-consumer", "partition-0", "consumer", "read p0 offsets"),
                        new ScenarioEdge("p1-consumer", "partition-1", "consumer", "read p1 offsets"),
                        new ScenarioEdge("offset-observer", "consumer", "observer", "inspect positions")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "We begin with one producer, one consumer, and a topic that already has two partitions. Nothing is random yet: partitions are the real storage units.",
                                List.of()),
                        new ScenarioStep("step-2", "Producer appends to partition 0",
                                "The first record lands in partition 0 and receives offset 0 there. That offset says nothing about partition 1.",
                                List.of("event-p0-offset-0")),
                        new ScenarioStep("step-3", "Producer appends to partition 1",
                                "A second record lands in partition 1 and also receives offset 0, because offsets are local to each partition.",
                                List.of("event-p1-offset-0")),
                        new ScenarioStep("step-4", "Partition 0 gets another record",
                                "Appending again to partition 0 creates offset 1 in that partition only.",
                                List.of("event-p0-offset-1")),
                        new ScenarioStep("step-5", "Consumer reads per partition",
                                "A consumer does not read by a single global topic counter. It advances independently through each assigned partition.",
                                List.of("event-consumer-read"))
                ),
                List.of(
                        new VisualizationEvent("event-p0-offset-0", "append", "p0 offset 0",
                                "The producer appends the first record to partition 0.", List.of("producer", "partition-0", "observer"),
                                List.of("producer-p0"), "producer", "partition-0"),
                        new VisualizationEvent("event-p1-offset-0", "append", "p1 offset 0",
                                "Partition 1 starts its own offset sequence at 0.", List.of("producer", "partition-1", "observer"),
                                List.of("producer-p1"), "producer", "partition-1"),
                        new VisualizationEvent("event-p0-offset-1", "append", "p0 offset 1",
                                "Partition 0 now contains a second appended record at offset 1.", List.of("producer", "partition-0", "observer"),
                                List.of("producer-p0"), "producer", "partition-0"),
                        new VisualizationEvent("event-consumer-read", "consume", "read by partition",
                                "The consumer tracks progress partition by partition rather than using one global topic offset.",
                                List.of("partition-0", "partition-1", "consumer", "observer"),
                                List.of("p0-consumer", "p1-consumer", "offset-observer"), "partition-0", "consumer")
                )
        );
    }

    public ScenarioGraph consumerGroupSinglePartitionScenario() {
        return new ScenarioGraph(
                "consumer-group-single-partition",
                4,
                "One Partition, Two Consumers, One Group",
                "A classic interview trap: with only one partition, one consumer works and the second consumer in the same group stays idle.",
                1360,
                780,
                List.of(
                        new ScenarioNode("producer", "Producer", "service", 70, 330, 170, 96, null,
                                "The producer keeps sending records to the topic."),
                        new ScenarioNode("topic-box", "payments Topic", "container", 320, 150, 320, 360, null,
                                "This topic has only one partition, so there is only one unit of parallel work."),
                        new ScenarioNode("single-partition", "Partition 0", "broker", 70, 110, 180, 120, "topic-box",
                                "All records for this topic end up in the single partition."),
                        new ScenarioNode("group-box", "Consumer Group billing-group", "container", 760, 110, 470, 500, null,
                                "Consumers in the same group cooperate and split partitions, not individual messages."),
                        new ScenarioNode("consumer-a", "Consumer A", "consumer", 42, 92, 170, 96, "group-box",
                                "The first consumer joins and can own the partition."),
                        new ScenarioNode("consumer-b", "Consumer B", "consumer", 248, 300, 170, 96, "group-box",
                                "The second consumer is healthy but has nothing to do while there is only one partition."),
                        new ScenarioNode("observer", "Assignment View", "monitor", 1060, 660, 190, 90, null,
                                "We use the observer to narrate which consumer owns the partition.")
                ),
                List.of(
                        new ScenarioEdge("producer-to-partition", "producer", "single-partition", "append"),
                        new ScenarioEdge("partition-to-consumer-a", "single-partition", "consumer-a", "assigned"),
                        new ScenarioEdge("partition-to-consumer-b", "single-partition", "consumer-b", "would like work"),
                        new ScenarioEdge("consumer-a-to-observer", "consumer-a", "observer", "active"),
                        new ScenarioEdge("consumer-b-to-observer", "consumer-b", "observer", "idle")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "We start with one topic partition and two consumers inside the same group. The key question is not who is faster, but how many partitions exist.",
                                List.of()),
                        new ScenarioStep("step-2", "Consumer A joins first",
                                "The group coordinator can assign the only partition to Consumer A, so work begins there.",
                                List.of("event-consumer-a-assigned")),
                        new ScenarioStep("step-3", "Consumer B joins the same group",
                                "Consumer B is part of the group, but there is still only one partition to assign.",
                                List.of("event-consumer-b-joins")),
                        new ScenarioStep("step-4", "Only one consumer gets records",
                                "Kafka does not alternate records between consumers in the same group when there is only one partition. Consumer A keeps the entire partition.",
                                List.of("event-only-a-consumes"))
                ),
                List.of(
                        new VisualizationEvent("event-consumer-a-assigned", "assignment", "p0 -> Consumer A",
                                "The single partition is assigned to Consumer A.", List.of("single-partition", "consumer-a", "observer"),
                                List.of("partition-to-consumer-a", "consumer-a-to-observer"), "single-partition", "consumer-a"),
                        new VisualizationEvent("event-consumer-b-joins", "membership", "Consumer B joined",
                                "Consumer B joins the same group, but Kafka cannot split one partition across two active consumers in the same group.",
                                List.of("consumer-a", "consumer-b", "group-box", "observer"),
                                List.of("consumer-b-to-observer"), null, null),
                        new VisualizationEvent("event-only-a-consumes", "consume", "Consumer B is idle",
                                "Records continue to flow only through Consumer A because partition ownership is whole, not message by message.",
                                List.of("producer", "single-partition", "consumer-a", "consumer-b", "observer"),
                                List.of("producer-to-partition", "partition-to-consumer-a", "consumer-a-to-observer"), "single-partition", "consumer-a")
                )
        );
    }

    public ScenarioGraph consumerGroupTwoPartitionsScenario() {
        return new ScenarioGraph(
                "consumer-group-two-partitions",
                5,
                "Two Partitions, Two Consumers, One Group",
                "The happy path for scaling: when partitions exist, consumers in the same group can split the work.",
                1400,
                820,
                List.of(
                        new ScenarioNode("producer", "Producer", "service", 60, 350, 170, 96, null,
                                "The producer sends records that Kafka routes into partitions."),
                        new ScenarioNode("topic-box", "orders Topic", "container", 300, 150, 360, 430, null,
                                "This topic now has two partitions, so Kafka has two parallel units of consumption."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 48, 90, 180, 120, "topic-box",
                                "Partition 0 is one independent ordered log."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 48, 250, 180, 120, "topic-box",
                                "Partition 1 is another independent ordered log."),
                        new ScenarioNode("group-box", "Consumer Group order-group", "container", 760, 110, 500, 540, null,
                                "Kafka assigns whole partitions across the consumers in this group."),
                        new ScenarioNode("consumer-a", "Consumer A", "consumer", 48, 110, 170, 96, "group-box",
                                "Consumer A can own one or more partitions."),
                        new ScenarioNode("consumer-b", "Consumer B", "consumer", 286, 290, 170, 96, "group-box",
                                "Consumer B can own the remaining partition."),
                        new ScenarioNode("observer", "Lag / Assignment View", "monitor", 1070, 700, 220, 90, null,
                                "The observer summarizes which consumer owns which partition.")
                ),
                List.of(
                        new ScenarioEdge("producer-p0", "producer", "partition-0", "append to p0"),
                        new ScenarioEdge("producer-p1", "producer", "partition-1", "append to p1"),
                        new ScenarioEdge("p0-consumer-a", "partition-0", "consumer-a", "assigned"),
                        new ScenarioEdge("p1-consumer-b", "partition-1", "consumer-b", "assigned"),
                        new ScenarioEdge("a-observer", "consumer-a", "observer", "p0 owner"),
                        new ScenarioEdge("b-observer", "consumer-b", "observer", "p1 owner")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "The topology is the same idea as before, but now the topic has two partitions and the group has enough work to share.",
                                List.of()),
                        new ScenarioStep("step-2", "Both consumers join the group",
                                "The coordinator sees two consumers and two partitions, which is enough for real parallel work.",
                                List.of("event-both-join")),
                        new ScenarioStep("step-3", "Kafka distributes ownership",
                                "Partition 0 goes to Consumer A while partition 1 goes to Consumer B.",
                                List.of("event-split-assignment")),
                        new ScenarioStep("step-4", "Consumers process independently",
                                "Now both consumers are active at the same time, each advancing offsets only within its own partition.",
                                List.of("event-parallel-consume"))
                ),
                List.of(
                        new VisualizationEvent("event-both-join", "membership", "Two consumers in group",
                                "The group has enough partitions to keep both consumers busy.", List.of("group-box", "consumer-a", "consumer-b", "observer"),
                                List.of("a-observer", "b-observer"), null, null),
                        new VisualizationEvent("event-split-assignment", "assignment", "p0 -> A, p1 -> B",
                                "Kafka assigns whole partitions, not alternating records, so each consumer gets a full partition.",
                                List.of("partition-0", "partition-1", "consumer-a", "consumer-b", "observer"),
                                List.of("p0-consumer-a", "p1-consumer-b", "a-observer", "b-observer"), "partition-0", "consumer-a"),
                        new VisualizationEvent("event-parallel-consume", "consume", "Parallel consumption",
                                "Both consumers are active, but each keeps order only within its own partition.",
                                List.of("producer", "partition-0", "partition-1", "consumer-a", "consumer-b"),
                                List.of("producer-p0", "producer-p1", "p0-consumer-a", "p1-consumer-b"), "partition-1", "consumer-b")
                )
        );
    }

    public ScenarioGraph consumerGroupRebalanceOnJoinScenario() {
        return new ScenarioGraph(
                "consumer-group-rebalance-join",
                6,
                "Rebalance When a Second Consumer Joins",
                "A second consumer can trigger rebalance so partition ownership changes while the group is alive.",
                1420,
                820,
                List.of(
                        new ScenarioNode("producer", "Producer", "service", 60, 350, 170, 96, null,
                                "Records continue to arrive while the group membership changes."),
                        new ScenarioNode("topic-box", "trades Topic", "container", 300, 150, 360, 430, null,
                                "Two partitions give Kafka something to rebalance across consumers."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 48, 90, 180, 120, "topic-box",
                                "Before rebalance, this partition may already be owned by Consumer A."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 48, 250, 180, 120, "topic-box",
                                "This partition can move during rebalance as group membership changes."),
                        new ScenarioNode("group-box", "Consumer Group trade-group", "container", 760, 110, 500, 540, null,
                                "A rebalance recalculates partition assignment for the whole group."),
                        new ScenarioNode("consumer-a", "Consumer A", "consumer", 48, 110, 170, 96, "group-box",
                                "Initially the only active consumer in the group."),
                        new ScenarioNode("consumer-b", "Consumer B", "consumer", 286, 290, 170, 96, "group-box",
                                "A new consumer joins later and forces ownership to be recalculated."),
                        new ScenarioNode("coordinator", "Group Coordinator", "monitor", 1070, 700, 220, 90, null,
                                "The coordinator notices joins and starts the rebalance cycle.")
                ),
                List.of(
                        new ScenarioEdge("producer-p0", "producer", "partition-0", "append"),
                        new ScenarioEdge("producer-p1", "producer", "partition-1", "append"),
                        new ScenarioEdge("p0-a", "partition-0", "consumer-a", "owned by A"),
                        new ScenarioEdge("p1-a", "partition-1", "consumer-a", "owned by A"),
                        new ScenarioEdge("p1-b", "partition-1", "consumer-b", "moves to B"),
                        new ScenarioEdge("coordinator-group", "coordinator", "group-box", "rebalance")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "We start with two partitions and a group that currently has only Consumer A.",
                                List.of()),
                        new ScenarioStep("step-2", "Consumer A owns everything",
                                "While alone in the group, Consumer A can own both partitions.",
                                List.of("event-a-owns-both")),
                        new ScenarioStep("step-3", "Consumer B joins",
                                "A new consumer enters the group, so the coordinator must trigger rebalance.",
                                List.of("event-b-joins")),
                        new ScenarioStep("step-4", "Rebalance redistributes partitions",
                                "After rebalance, Kafka can move one partition away from Consumer A and give it to Consumer B.",
                                List.of("event-rebalanced")),
                        new ScenarioStep("step-5", "Both consumers continue",
                                "The group resumes with a new stable assignment and both consumers can work.",
                                List.of("event-post-rebalance"))
                ),
                List.of(
                        new VisualizationEvent("event-a-owns-both", "assignment", "A owns p0 and p1",
                                "With only one consumer present, both partitions belong to Consumer A.", List.of("partition-0", "partition-1", "consumer-a"),
                                List.of("p0-a", "p1-a"), "partition-0", "consumer-a"),
                        new VisualizationEvent("event-b-joins", "rebalance", "Consumer B joined",
                                "The group coordinator detects a membership change and triggers rebalance.", List.of("consumer-a", "consumer-b", "coordinator", "group-box"),
                                List.of("coordinator-group"), "coordinator", "group-box"),
                        new VisualizationEvent("event-rebalanced", "assignment", "p1 moved to B",
                                "Partition 1 is reassigned to Consumer B, which is the core visual of rebalance.", List.of("partition-0", "partition-1", "consumer-a", "consumer-b", "coordinator"),
                                List.of("p0-a", "p1-b", "coordinator-group"), "partition-1", "consumer-b"),
                        new VisualizationEvent("event-post-rebalance", "consume", "Stable after rebalance",
                                "The new assignment is now stable and both consumers can continue independently.", List.of("producer", "partition-0", "partition-1", "consumer-a", "consumer-b"),
                                List.of("producer-p0", "producer-p1", "p0-a", "p1-b"), "partition-0", "consumer-a")
                )
        );
    }

    public ScenarioGraph consumerGroupConsumerFailureScenario() {
        return new ScenarioGraph(
                "consumer-group-consumer-failure",
                7,
                "Consumer Failure and Partition Takeover",
                "When one consumer dies, its partition is not lost forever. The group rebalances and a surviving consumer takes over.",
                1450,
                840,
                List.of(
                        new ScenarioNode("producer", "Producer", "service", 60, 360, 170, 96, null,
                                "Records continue to arrive even while one consumer disappears."),
                        new ScenarioNode("topic-box", "shipments Topic", "container", 300, 150, 360, 430, null,
                                "Two partitions allow us to see takeover after failure."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 48, 90, 180, 120, "topic-box",
                                "Partition 0 starts assigned to Consumer A."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 48, 250, 180, 120, "topic-box",
                                "Partition 1 starts assigned to Consumer B."),
                        new ScenarioNode("group-box", "Consumer Group shipment-group", "container", 760, 110, 520, 560, null,
                                "A consumer failure causes the group to pause, rebalance, and then continue with a new owner."),
                        new ScenarioNode("consumer-a", "Consumer A", "consumer", 48, 110, 170, 96, "group-box",
                                "The surviving consumer will eventually own both partitions."),
                        new ScenarioNode("consumer-b", "Consumer B", "consumer", 286, 290, 170, 96, "group-box",
                                "This consumer fails after owning partition 1."),
                        new ScenarioNode("coordinator", "Group Coordinator", "monitor", 1080, 720, 220, 90, null,
                                "The coordinator notices missed heartbeats and starts reassignment."),
                        new ScenarioNode("lag-view", "Temporary Lag", "monitor", 1180, 300, 180, 96, null,
                                "While partition 1 has no owner, records can accumulate there briefly.")
                ),
                List.of(
                        new ScenarioEdge("producer-p0", "producer", "partition-0", "append"),
                        new ScenarioEdge("producer-p1", "producer", "partition-1", "append"),
                        new ScenarioEdge("p0-a", "partition-0", "consumer-a", "owned by A"),
                        new ScenarioEdge("p1-b", "partition-1", "consumer-b", "owned by B"),
                        new ScenarioEdge("p1-lag", "partition-1", "lag-view", "temporarily waiting"),
                        new ScenarioEdge("p1-a", "partition-1", "consumer-a", "taken over by A"),
                        new ScenarioEdge("coordinator-group", "coordinator", "group-box", "detect failure")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "The group begins in a balanced state: two partitions and two active consumers.",
                                List.of()),
                        new ScenarioStep("step-2", "Each consumer owns one partition",
                                "Consumer A owns partition 0 while Consumer B owns partition 1.",
                                List.of("event-balanced-start")),
                        new ScenarioStep("step-3", "Consumer B fails",
                                "Consumer B stops heartbeating. For a short time, partition 1 is left without an active owner.",
                                List.of("event-b-failed")),
                        new ScenarioStep("step-4", "Coordinator triggers rebalance",
                                "The group coordinator detects the failure and begins reassignment.",
                                List.of("event-failure-detected")),
                        new ScenarioStep("step-5", "Consumer A takes over partition 1",
                                "After rebalance, Consumer A owns both partitions and processing resumes.",
                                List.of("event-a-takes-over"))
                ),
                List.of(
                        new VisualizationEvent("event-balanced-start", "assignment", "p0 -> A, p1 -> B",
                                "This is the normal balanced state before any failure.", List.of("partition-0", "partition-1", "consumer-a", "consumer-b"),
                                List.of("p0-a", "p1-b"), "partition-1", "consumer-b"),
                        new VisualizationEvent("event-b-failed", "failure", "Consumer B failed",
                                "Consumer B disappears, and partition 1 is now temporarily unserved.", List.of("consumer-b", "partition-1", "lag-view"),
                                List.of("p1-lag"), "partition-1", "lag-view"),
                        new VisualizationEvent("event-failure-detected", "rebalance", "Coordinator detected failure",
                                "Missed heartbeats cause the coordinator to trigger rebalance for the group.", List.of("coordinator", "group-box", "consumer-a", "partition-1"),
                                List.of("coordinator-group", "p1-lag"), "coordinator", "group-box"),
                        new VisualizationEvent("event-a-takes-over", "assignment", "A now owns p0 and p1",
                                "The surviving consumer takes over partition 1, so records there are processed again.", List.of("consumer-a", "partition-0", "partition-1", "lag-view"),
                                List.of("p0-a", "p1-a"), "partition-1", "consumer-a")
                )
        );
    }

    public ScenarioGraph systemReadyScenario() {
        return new ScenarioGraph(
                "system-ready",
                1,
                "System Ready",
                "A Spring wiring check: the application starts and creates its Kafka-facing beans before any real message is sent.",
                1180,
                640,
                List.of(
                        new ScenarioNode("spring-app", "Spring Boot Application", "container", 170, 120, 560, 320, null,
                                "The application context starts and wires the sandbox components."),
                        new ScenarioNode("publisher", "TradeEventPublisher", "service", 44, 92, 246, 88, "spring-app",
                                "The publisher bean exists and can use KafkaTemplate; broker delivery is not tested here."),
                        new ScenarioNode("listener", "TradeEventListener", "consumer", 44, 204, 232, 88, "spring-app",
                                "The listener bean exists; broker connection and partition assignment are not tested here."),
                        new ScenarioNode("kafka", "Kafka Broker", "broker", 860, 246, 220, 96, null,
                                "The external dependency required by later scenarios. Its health is not checked in scenario 001.")
                ),
                List.of(
                        new ScenarioEdge("publisher-kafka", "publisher", "kafka", "configured publish path"),
                        new ScenarioEdge("listener-kafka", "listener", "kafka", "configured consume path")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "The application, Kafka broker, publisher, and listener are visible before readiness signals start appearing.",
                                List.of()),
                        new ScenarioStep("step-2", "Spring context started",
                                "The application context boots and the Spring Boot container becomes available.",
                                List.of("event-spring-started")),
                        new ScenarioStep("step-3", "Publisher bean ready",
                                "TradeEventPublisher and KafkaTemplate are injected. No broker acknowledgement has been requested yet.",
                                List.of("event-publisher-ready")),
                        new ScenarioStep("step-4", "Listener bean ready",
                                "TradeEventListener is injected. This does not yet prove a broker connection or partition assignment.",
                                List.of("event-listener-ready")),
                        new ScenarioStep("step-5", "Spring wiring ready",
                                "The application context and Kafka-facing beans are ready for a real Kafka experiment.",
                                List.of("event-system-ready"))
                ),
                List.of(
                        new VisualizationEvent(
                                "event-spring-started",
                                "startup",
                                "Context Up",
                                "The Spring Boot application context has started.",
                                List.of("spring-app"),
                                List.of(),
                                null,
                                null
                        ),
                        new VisualizationEvent(
                                "event-publisher-ready",
                                "readiness",
                                "Publisher Ready",
                                "TradeEventPublisher has been injected; message delivery is not tested in this scenario.",
                                List.of("spring-app", "publisher"),
                                List.of(),
                                null,
                                null
                        ),
                        new VisualizationEvent(
                                "event-listener-ready",
                                "readiness",
                                "Listener Ready",
                                "TradeEventListener has been injected; subscription and assignment are not tested here.",
                                List.of("spring-app", "listener"),
                                List.of(),
                                null,
                                null
                        ),
                        new VisualizationEvent(
                                "event-system-ready",
                                "status",
                                "Spring Wiring Ready",
                                "The application context contains the Kafka-facing beans needed for the next experiment.",
                                List.of("spring-app", "publisher", "listener"),
                                List.of(),
                                null,
                                null
                        )
                )
        );
    }
}
