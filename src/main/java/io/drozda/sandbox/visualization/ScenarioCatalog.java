package io.drozda.sandbox.visualization;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ScenarioCatalog {

    public List<ScenarioGraph> scenarios() {
        return List.of(
                topicPartitionOffsetsScenario(),
                keyPartitioningScenario(),
                partitionOrderingScenario(),
                globalOrderingScenario(),
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
                "A real end-to-end Kafka experiment: publish one unique trade event and receive the same event in an isolated listener.",
                "Use an event log when producers and consumers must be decoupled: the producer finishes after Kafka accepts the record, while consumers process it independently.",
                List.of(1),
                1200,
                650,
                List.of(
                        new ScenarioNode("client", "Client", "external", 40, 275, 170, 96, null,
                                "The Play action asks the isolated scenario application to run one experiment."),
                        new ScenarioNode("spring-app", "Trade Flow Spring Boot Application", "container", 260, 100, 410, 440, null,
                                "A dedicated Spring context owns only the components needed by scenario 002."),
                        new ScenarioNode("publisher", "TradeFlowPublisher", "service", 70, 105, 250, 96, "spring-app",
                                "Creates a unique event and waits for Kafka's send acknowledgement."),
                        new ScenarioNode("listener", "TradeFlowListener", "consumer", 70, 285, 250, 96, "spring-app",
                                "Receives the record and confirms that its eventId matches the published event."),
                        new ScenarioNode("kafka-broker", "Kafka Broker", "broker-container", 760, 100, 360, 440, null,
                                "The external Kafka process accepts the record and returns partition and offset metadata."),
                        new ScenarioNode("topic", "scenario-002-trade-events Topic", "broker", 50, 160, 260, 120, "kafka-broker",
                                "A scenario-owned topic with one partition stores the trade event.")
                ),
                List.of(
                        new ScenarioEdge("request", "client", "publisher", "invoke"),
                        new ScenarioEdge("publish", "publisher", "topic", "send + acknowledgement"),
                        new ScenarioEdge("consume", "topic", "listener", "poll and deliver")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "The isolated application and its scenario-owned Kafka topic exist before a message moves.",
                                List.of()),
                        new ScenarioStep("step-2", "Client triggers action",
                                "The mediator asks the scenario application to publish and verify one unique event.",
                                List.of("event-client-request")),
                        new ScenarioStep("step-3", "Kafka acknowledges the record",
                                "The send future returns real partition and offset metadata from Kafka.",
                                List.of("event-publish")),
                        new ScenarioStep("step-4", "Listener receives the event",
                                "The scenario listener consumes a record with the same unique eventId.",
                                List.of("event-listener-receive")),
                        new ScenarioStep("step-5", "End-to-end flow confirmed",
                                "Both the producer acknowledgement and matching consumer receipt have been observed.",
                                List.of("event-flow-confirmed"))
                ),
                List.of(
                        new VisualizationEvent(
                                "event-client-request",
                                "command",
                                "Run trade flow",
                                "The command enters the isolated scenario application.",
                                List.of("client", "spring-app", "publisher"),
                                List.of("request"),
                                "client",
                                "publisher"
                        ),
                        new VisualizationEvent(
                                "event-publish",
                                "readiness",
                                "Kafka acknowledged",
                                "Kafka returned partition and offset metadata for the produced record.",
                                List.of("spring-app", "publisher", "kafka-broker", "topic"),
                                List.of("publish"),
                                "publisher",
                                "topic"
                        ),
                        new VisualizationEvent(
                                "event-listener-receive",
                                "readiness",
                                "Matching event received",
                                "TradeFlowListener received the eventId created by this run.",
                                List.of("spring-app", "topic", "listener"),
                                List.of("consume"),
                                "topic",
                                "listener"
                        ),
                        new VisualizationEvent(
                                "event-flow-confirmed",
                                "status",
                                "End-to-end confirmed",
                                "A real record completed the publisher, Kafka, and listener path.",
                                List.of("spring-app", "publisher", "kafka-broker", "topic", "listener"),
                                List.of("publish", "consume"),
                                null,
                                null
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
                "Partition a busy stream for parallel processing, preserve order within each shard, and identify exactly where every consumer is reading.",
                List.of(2, 11, 12),
                1320,
                760,
                List.of(
                        new ScenarioNode("producer", "PartitionedEventPublisher", "service", 35, 330, 230, 96, null,
                                "The producer explicitly selects a partition for each record in this experiment."),
                        new ScenarioNode("kafka-broker", "Kafka Broker", "broker-container", 410, 80, 500, 600, null,
                                "Kafka stores the topic as separate partition logs."),
                        new ScenarioNode("orders-topic-box", "scenario-003-partition-offsets Topic", "topic-container",
                                40, 80, 420, 440, "kafka-broker",
                                "The topic contains two independent ordered partitions."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 75, 110, 270, 120, "orders-topic-box",
                                "Records A and C are appended here, receiving increasing local offsets."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 75, 270, 270, 120, "orders-topic-box",
                                "Record B is appended here and uses this partition's own offset sequence."),
                        new ScenarioNode("consumer", "PartitionedEventListener", "consumer", 1040, 330, 240, 96, null,
                                "One consumer subscribes to the topic and receives records from both partitions.")
                ),
                List.of(
                        new ScenarioEdge("producer-p0", "producer", "partition-0", "append record A"),
                        new ScenarioEdge("producer-p1", "producer", "partition-1", "append record B"),
                        new ScenarioEdge("p0-consumer", "partition-0", "consumer", "read p0 offsets"),
                        new ScenarioEdge("p1-consumer", "partition-1", "consumer", "read p1 offsets")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "We begin with one producer, one consumer, and a topic that already has two partitions. Nothing is random yet: partitions are the real storage units.",
                                List.of()),
                        new ScenarioStep("step-2", "Producer appends to partition 0",
                                "Record A lands in partition 0 and receives its next local offset N. That offset says nothing about partition 1.",
                                List.of("event-p0-offset-0")),
                        new ScenarioStep("step-3", "Producer appends to partition 1",
                                "Record B lands in partition 1 and receives that partition's next offset M, independent of N.",
                                List.of("event-p1-offset-0")),
                        new ScenarioStep("step-4", "Partition 0 gets another record",
                                "Appending C to partition 0 creates a greater offset in partition 0 without advancing partition 1.",
                                List.of("event-p0-offset-1")),
                        new ScenarioStep("step-5", "Consumer reads per partition",
                                "A consumer does not read by a single global topic counter. It advances independently through each assigned partition.",
                                List.of("event-consumer-read"))
                ),
                List.of(
                        new VisualizationEvent("event-p0-offset-0", "append", "p0 offset N",
                                "The producer appends the first record to partition 0.", List.of("producer", "partition-0"),
                                List.of("producer-p0"), "producer", "partition-0"),
                        new VisualizationEvent("event-p1-offset-0", "append", "p1 offset M",
                                "Partition 1 has its own independent offset sequence.", List.of("producer", "partition-1"),
                                List.of("producer-p1"), "producer", "partition-1"),
                        new VisualizationEvent("event-p0-offset-1", "append", "p0 offset N' > N",
                                "Partition 0 now contains another record at a greater offset.", List.of("producer", "partition-0"),
                                List.of("producer-p0"), "producer", "partition-0"),
                        new VisualizationEvent("event-consumer-read", "consume", "read by partition",
                                "The consumer tracks progress partition by partition rather than using one global topic offset.",
                                List.of("partition-0", "partition-1", "consumer"),
                                List.of("p0-consumer", "p1-consumer"), "partition-0", "consumer")
                )
        );
    }

    public ScenarioGraph keyPartitioningScenario() {
        return new ScenarioGraph(
                "key-partitioning",
                4,
                "Message Key and Partition Selection",
                "Scenario 004 answers where related records go: a stable business key routes them to one partition. Scenario 005 will separately verify their read order inside that partition.",
                "First place one entity's events in the same ordered shard; only then can partition-level ordering provide a useful guarantee.",
                List.of(10),
                1380,
                780,
                List.of(
                        new ScenarioNode("publisher", "KeyedOrderEventPublisher", "service", 35, 340, 240, 100, null,
                                "Sends orderId as the Kafka record key and does not choose a partition."),
                        new ScenarioNode("kafka-broker", "Kafka Broker", "broker-container", 455, 70, 470, 640, null,
                                "Kafka applies its partitioner to the serialized record key."),
                        new ScenarioNode("topic", "scenario-004-key-partitioning Topic", "topic-container",
                                40, 80, 390, 480, "kafka-broker", "A topic with three partitions."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 75, 90, 240, 100, "topic", "One ordered shard."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 75, 215, 240, 100, "topic", "One ordered shard."),
                        new ScenarioNode("partition-2", "Partition 2", "broker", 75, 340, 240, 100, "topic", "One ordered shard."),
                        new ScenarioNode("consumer-a", "Consumer A", "consumer", 1090, 185, 220, 90, null,
                                "One of three concurrent consumers in the same group."),
                        new ScenarioNode("consumer-b", "Consumer B", "consumer", 1090, 345, 220, 90, null,
                                "One of three concurrent consumers in the same group."),
                        new ScenarioNode("consumer-c", "Consumer C", "consumer", 1090, 505, 220, 90, null,
                                "One of three concurrent consumers in the same group.")
                ),
                List.of(
                        new ScenarioEdge("publish", "publisher", "topic", "send with selected key")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "The producer knows only the topic and orderId key; the topic already has three partitions.", List.of()),
                        new ScenarioStep("step-2", "Kafka selects a partition",
                                "CREATED, PAID, and SHIPPED for order-42 all receive the same partition number.", List.of()),
                        new ScenarioStep("step-3", "Consumer verifies the route",
                                "Three listener instances share the partitions; each record is animated toward the consumer that actually received it.", List.of())
                ),
                List.of()
        );
    }

    public ScenarioGraph partitionOrderingScenario() {
        return new ScenarioGraph(
                "partition-ordering",
                5,
                "Ordering Within One Partition",
                "Scenario 005 verifies what happens after scenario 004 routed related records together: Kafka offsets and listener callbacks preserve their append order inside one partition.",
                "Protect state transitions for one entity from being consumed out of order, while recognizing that the guarantee ends at the partition and listener boundary.",
                List.of(11),
                1400,
                800,
                List.of(
                        new ScenarioNode("publisher", "OrderedEventPublisher", "service", 35, 335, 245, 100, null,
                                "Sends sequence 0..5 using one orderId key."),
                        new ScenarioNode("kafka-broker", "Kafka Broker", "broker-container", 430, 65, 470, 650, null,
                                "Each partition is an independent ordered append-only log."),
                        new ScenarioNode("topic", "scenario-005-partition-ordering Topic", "topic-container",
                                40, 80, 390, 490, "kafka-broker", "Three partitions; one receives this order's complete sequence."),
                        new ScenarioNode("partition-0", "Partition 0", "broker", 75, 90, 240, 100, "topic", "Ordered shard 0."),
                        new ScenarioNode("partition-1", "Partition 1", "broker", 75, 220, 240, 100, "topic", "Ordered shard 1."),
                        new ScenarioNode("partition-2", "Partition 2", "broker", 75, 350, 240, 100, "topic", "Ordered shard 2."),
                        new ScenarioNode("consumer-a", "Consumer A", "consumer", 1060, 145, 220, 90, null,
                                "One concurrent consumer in the scenario group."),
                        new ScenarioNode("consumer-b", "Consumer B", "consumer", 1060, 285, 220, 90, null,
                                "One concurrent consumer in the scenario group."),
                        new ScenarioNode("consumer-c", "Consumer C", "consumer", 1060, 425, 220, 90, null,
                                "One concurrent consumer in the scenario group."),
                        new ScenarioNode("order-check", "Order Verification", "monitor", 1035, 610, 270, 110, null,
                                "Compares business sequence, Kafka offsets, and listener callback order.")
                ),
                List.of(new ScenarioEdge("publish", "publisher", "topic", "send with orderId key")),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "Three consumers own three partitions before the ordered sequence is published.", List.of()),
                        new ScenarioStep("step-2", "Append and consume in order",
                                "Records sequence 0..5 receive increasing offsets in one partition and reach its assigned consumer.", List.of()),
                        new ScenarioStep("step-3", "Compare the three orders",
                                "The experiment compares producer sequence, partition offsets, and actual listener callback order.", List.of())
                ),
                List.of()
        );
    }

    public ScenarioGraph globalOrderingScenario() {
        return new ScenarioGraph(
                "global-ordering",
                6,
                "Parallel Orders Without Global Ordering",
                "Scenario 006 compares one ordered queue with two ordered shards: a short order can finish without waiting for an unrelated slow order.",
                "Remove head-of-line blocking between independent entities while preserving the event sequence inside each order, accepting that the topic has no single global processing order.",
                List.of(12),
                1420,
                820,
                List.of(
                        new ScenarioNode("publisher", "Interleaved Order Publisher", "service",
                                30, 340, 250, 105, null,
                                "Publishes the same interleaved Fast and Slow order events in both modes."),
                        new ScenarioNode("kafka-broker", "Kafka Broker", "broker-container",
                                390, 65, 500, 680, null,
                                "The selected topic has either one ordered queue or two ordered shards."),
                        new ScenarioNode("topic", "Selected Orders Topic", "topic-container",
                                40, 80, 420, 510, "kafka-broker",
                                "SINGLE uses a real one-partition topic; PARALLEL uses a real two-partition topic."),
                        new ScenarioNode("partition-0", "Partition 0", "broker",
                                90, 125, 240, 115, "topic", "Fast Order uses P0 in parallel mode."),
                        new ScenarioNode("partition-1", "Partition 1", "broker",
                                90, 300, 240, 115, "topic", "Slow Order uses P1 in parallel mode."),
                        new ScenarioNode("single-consumer", "Single Consumer", "consumer",
                                1030, 120, 245, 100, null,
                                "Processes every event serially in SINGLE mode."),
                        new ScenarioNode("fast-consumer", "Fast Consumer", "consumer",
                                1030, 300, 245, 100, null,
                                "Owns the Fast Order partition in PARALLEL mode."),
                        new ScenarioNode("slow-consumer", "Slow Consumer", "consumer",
                                1030, 475, 245, 100, null,
                                "Owns the Slow Order partition in PARALLEL mode."),
                        new ScenarioNode("result", "Completion Comparison", "monitor",
                                1000, 650, 305, 115, null,
                                "Shows measured completion time and ordering boundaries.")
                ),
                List.of(new ScenarioEdge("publish", "publisher", "topic", "same event stream")),
                List.of(
                        new ScenarioStep("step-1", "Choose the topology",
                                "Run one real partition and consumer, or two real partitions and consumers.", List.of()),
                        new ScenarioStep("step-2", "Assign ordered work",
                                "SINGLE assigns one queue; PARALLEL assigns one order stream to each partition.", List.of()),
                        new ScenarioStep("step-3", "Process both orders",
                                "The short and long order preserve their own status sequence while completion timing changes.", List.of()),
                        new ScenarioStep("step-4", "Compare completion",
                                "Parallel partitions remove unrelated waiting, but no global completion order remains.", List.of())
                ),
                List.of()
        );
    }

    public ScenarioGraph consumerGroupSinglePartitionScenario() {
        return new ScenarioGraph(
                "consumer-group-single-partition",
                7,
                "One Partition, Two Consumers, One Group",
                "A classic interview trap: with only one partition, one consumer works and the second consumer in the same group stays idle.",
                "Avoid paying for consumer instances that cannot increase throughput because the topic has fewer partitions than consumers in the group.",
                List.of(13, 14),
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
                        new ScenarioNode("observer", "Assignment and Takeover", "monitor", 1020, 660, 270, 90, null,
                                "Shows the real initial owner, idle member, and owner after rebalance.")
                ),
                List.of(
                        new ScenarioEdge("producer-to-partition", "producer", "single-partition", "append")
                ),
                List.of(
                        new ScenarioStep("step-1", "Initial topology",
                                "A real one-partition topic and two separately controlled consumers start in one group.",
                                List.of()),
                        new ScenarioStep("step-2", "Observe the assignment",
                                "Kafka assigns Partition 0 to one real group member; the other remains healthy but idle.", List.of()),
                        new ScenarioStep("step-3", "Consume through one owner",
                                "The first record batch is delivered only to the current partition owner.", List.of()),
                        new ScenarioStep("step-4", "Stop the owner",
                                "Stopping the active listener changes group membership and triggers a rebalance.", List.of()),
                        new ScenarioStep("step-5", "Standby takes over",
                                "The formerly idle consumer receives Partition 0 and processes the next records from their Kafka offsets.", List.of())
                ),
                List.of()
        );
    }

    public ScenarioGraph consumerGroupTwoPartitionsScenario() {
        return new ScenarioGraph(
                "consumer-group-two-partitions",
                8,
                "Two Partitions, Two Consumers, One Group",
                "The happy path for scaling: when partitions exist, consumers in the same group can split the work.",
                "Scale one logical service horizontally by giving its instances separate partitions while processing every record only once per group.",
                List.of(13, 14),
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
                11,
                "Rebalance When a Second Consumer Joins",
                "A second consumer can trigger rebalance so partition ownership changes while the group is alive.",
                "Understand temporary processing pauses and ownership changes that happen during deployments, autoscaling, and rolling restarts.",
                List.of(41, 94),
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
                12,
                "Consumer Failure and Partition Takeover",
                "When one consumer dies, its partition is not lost forever. The group rebalances and a surviving consumer takes over.",
                "Keep processing after an instance crashes by reassigning its partitions, while accounting for delay and possible repeated processing.",
                List.of(13, 41, 97),
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
                "Separate application wiring failures from broker and message-flow failures before investigating Kafka behavior.",
                List.of(),
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
