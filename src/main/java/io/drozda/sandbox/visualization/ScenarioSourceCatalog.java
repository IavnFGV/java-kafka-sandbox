package io.drozda.sandbox.visualization;

import java.util.List;

/** Explicit node-to-implementation links. Keep line anchors in sync when source files change. */
final class ScenarioSourceCatalog {
    private ScenarioSourceCatalog() {}

    static List<ScenarioSourceReference> references(String scenarioId, String nodeId) {
        return switch (scenarioId) {
            case "trade-flow" -> switch (nodeId) {
                case "client" -> List.of(
                        new ScenarioSourceReference("TradeFlowScenarioController",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioController.java", 43,
                                "Internal HTTP command that triggers the trade-flow experiment."),
                        new ScenarioSourceReference("TradeFlowExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowExperiment.java", 27,
                                "Run the experiment and verify its observations.")
                );
                case "spring-app", "topic" -> List.of(
                        new ScenarioSourceReference("TradeFlowScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioApplication.java", 24,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("TradeFlowEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowEnvironment.java", 35,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "publisher" -> List.of(
                        new ScenarioSourceReference("TradeFlowPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/producer/TradeFlowPublisher.java", 20,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("TradeFlowExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowExperiment.java", 27,
                                "Run the experiment and verify its observations.")
                );
                case "listener" -> List.of(
                        new ScenarioSourceReference("TradeFlowListener",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/consumer/TradeFlowListener.java", 16,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("TradeFlowEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowEventTracker.java", 19,
                                "Correlate received records with the current experiment.")
                );
                case "kafka-broker" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("application.yml",
                                "src/main/resources/application.yml", 7,
                                "Spring Kafka bootstrap servers and serialization defaults.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "topic-partition-offsets" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("PartitionedEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/producer/PartitionedEventPublisher.java", 20,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("TopicPartitionOffsetsExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/TopicPartitionOffsetsExperiment.java", 33,
                                "Run the experiment and verify its observations.")
                );
                case "kafka-broker" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("application.yml",
                                "src/main/resources/application.yml", 7,
                                "Spring Kafka bootstrap servers and serialization defaults.")
                );
                case "orders-topic-box" -> List.of(
                        new ScenarioSourceReference("TopicPartitionOffsetsScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/TopicPartitionOffsetsScenarioApplication.java", 24,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("TopicPartitionOffsetsEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/TopicPartitionOffsetsEnvironment.java", 35,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1" -> List.of(
                        new ScenarioSourceReference("TopicPartitionOffsetsScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/TopicPartitionOffsetsScenarioApplication.java", 24,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("PartitionedEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/producer/PartitionedEventPublisher.java", 20,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("PartitionedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/PartitionedEventTracker.java", 22,
                                "Correlate received records with the current experiment.")
                );
                case "consumer" -> List.of(
                        new ScenarioSourceReference("PartitionedEventListener",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/consumer/PartitionedEventListener.java", 17,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("PartitionedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/topicpartitionoffsetbasics/app/PartitionedEventTracker.java", 22,
                                "Correlate received records with the current experiment.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "key-partitioning" -> switch (nodeId) {
                case "publisher" -> List.of(
                        new ScenarioSourceReference("KeyedOrderEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/producer/KeyedOrderEventPublisher.java", 20,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("KeyPartitioningExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningExperiment.java", 29,
                                "Run the experiment and verify its observations.")
                );
                case "kafka-broker" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("application.yml",
                                "src/main/resources/application.yml", 7,
                                "Spring Kafka bootstrap servers and serialization defaults.")
                );
                case "topic" -> List.of(
                        new ScenarioSourceReference("KeyPartitioningScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("KeyPartitioningEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/KeyPartitioningEnvironment.java", 32,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1", "partition-2" -> List.of(
                        new ScenarioSourceReference("KeyPartitioningScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("KeyedOrderEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/producer/KeyedOrderEventPublisher.java", 20,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("KeyedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyedEventTracker.java", 31,
                                "Correlate received records with the current experiment.")
                );
                case "consumer-a", "consumer-b", "consumer-c" -> List.of(
                        new ScenarioSourceReference("KeyedOrderEventListener",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/consumer/KeyedOrderEventListener.java", 17,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("KeyedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyedEventTracker.java", 31,
                                "Correlate received records with the current experiment.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "partition-ordering" -> switch (nodeId) {
                case "publisher" -> List.of(
                        new ScenarioSourceReference("OrderedEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/producer/OrderedEventPublisher.java", 19,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("PartitionOrderingExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/PartitionOrderingExperiment.java", 33,
                                "Run the experiment and verify its observations.")
                );
                case "kafka-broker" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("application.yml",
                                "src/main/resources/application.yml", 7,
                                "Spring Kafka bootstrap servers and serialization defaults.")
                );
                case "topic" -> List.of(
                        new ScenarioSourceReference("PartitionOrderingScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/PartitionOrderingScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("PartitionOrderingEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/PartitionOrderingEnvironment.java", 31,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1", "partition-2" -> List.of(
                        new ScenarioSourceReference("PartitionOrderingScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/PartitionOrderingScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("OrderedEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/producer/OrderedEventPublisher.java", 19,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("OrderedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/OrderedEventTracker.java", 37,
                                "Correlate received records with the current experiment.")
                );
                case "consumer-a", "consumer-b", "consumer-c" -> List.of(
                        new ScenarioSourceReference("OrderedEventListener",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/consumer/OrderedEventListener.java", 21,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("OrderedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/OrderedEventTracker.java", 37,
                                "Correlate received records with the current experiment.")
                );
                case "order-check" -> List.of(
                        new ScenarioSourceReference("PartitionOrderingExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/PartitionOrderingExperiment.java", 33,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("OrderedEventTracker",
                                "src/main/java/io/drozda/sandbox/scenario/orderingwithinonepartition/app/OrderedEventTracker.java", 37,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("PartitionOrderingScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/orderingwithinonepartition/PartitionOrderingScenarioTest.java", 22,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "global-ordering" -> switch (nodeId) {
                case "publisher" -> List.of(
                        new ScenarioSourceReference("GlobalOrderPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/producer/GlobalOrderPublisher.java", 17,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("GlobalOrderingExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingExperiment.java", 41,
                                "Run the experiment and verify its observations.")
                );
                case "kafka-broker" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("application.yml",
                                "src/main/resources/application.yml", 7,
                                "Spring Kafka bootstrap servers and serialization defaults.")
                );
                case "topic" -> List.of(
                        new ScenarioSourceReference("GlobalOrderingScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("GlobalOrderingEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/GlobalOrderingEnvironment.java", 32,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1" -> List.of(
                        new ScenarioSourceReference("GlobalOrderingScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("GlobalOrderPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/producer/GlobalOrderPublisher.java", 17,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("GlobalOrderTracker",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderTracker.java", 35,
                                "Correlate received records with the current experiment.")
                );
                case "single-consumer" -> List.of(
                        new ScenarioSourceReference("GlobalOrderListener",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/consumer/GlobalOrderListener.java", 26,
                                "Listener for this topology; parallel consumers share this method."),
                        new ScenarioSourceReference("GlobalOrderTracker",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderTracker.java", 35,
                                "Correlate received records with the current experiment.")
                );
                case "fast-consumer", "slow-consumer" -> List.of(
                        new ScenarioSourceReference("GlobalOrderListener",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/consumer/GlobalOrderListener.java", 35,
                                "Listener for this topology; parallel consumers share this method."),
                        new ScenarioSourceReference("GlobalOrderTracker",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderTracker.java", 35,
                                "Correlate received records with the current experiment.")
                );
                case "result" -> List.of(
                        new ScenarioSourceReference("GlobalOrderingExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderingExperiment.java", 41,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("GlobalOrderTracker",
                                "src/main/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/app/GlobalOrderTracker.java", 35,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("GlobalOrderingScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/parallelorderswithoutglobalordering/GlobalOrderingScenarioTest.java", 24,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "consumer-group-single-partition" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("GroupWorkPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/producer/GroupWorkPublisher.java", 17,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("SinglePartitionGroupExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupExperiment.java", 36,
                                "Run the experiment and verify its observations.")
                );
                case "topic-box" -> List.of(
                        new ScenarioSourceReference("SinglePartitionGroupScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupScenarioApplication.java", 25,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("SinglePartitionGroupEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupEnvironment.java", 31,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "single-partition" -> List.of(
                        new ScenarioSourceReference("SinglePartitionGroupScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupScenarioApplication.java", 25,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("GroupWorkPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/producer/GroupWorkPublisher.java", 17,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("SinglePartitionGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupTracker.java", 32,
                                "Correlate received records with the current experiment.")
                );
                case "group-box" -> List.of(
                        new ScenarioSourceReference("SinglePartitionGroupEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupEnvironment.java", 31,
                                "Configure the isolated application, topic names, and consumer groups."),
                        new ScenarioSourceReference("ConsumerGroupMemberA",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberA.java", 24,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("ConsumerGroupMemberB",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberB.java", 24,
                                "Kafka subscription and record handling.")
                );
                case "consumer-a" -> List.of(
                        new ScenarioSourceReference("ConsumerGroupMemberA",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberA.java", 24,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("SinglePartitionGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupTracker.java", 32,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("ConsumerMemberControl",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/ConsumerMemberControl.java", 9,
                                "Start or stop individual listener containers.")
                );
                case "consumer-b" -> List.of(
                        new ScenarioSourceReference("ConsumerGroupMemberB",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberB.java", 24,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("SinglePartitionGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupTracker.java", 32,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("ConsumerMemberControl",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/ConsumerMemberControl.java", 9,
                                "Start or stop individual listener containers.")
                );
                case "observer" -> List.of(
                        new ScenarioSourceReference("SinglePartitionGroupExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupExperiment.java", 36,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("SinglePartitionGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupTracker.java", 32,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("SinglePartitionGroupScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupScenarioTest.java", 21,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "consumer-group-two-partitions" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("ParallelGroupPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/producer/ParallelGroupPublisher.java", 15,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("ParallelGroupExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupExperiment.java", 28,
                                "Run the experiment and verify its observations.")
                );
                case "topic-box" -> List.of(
                        new ScenarioSourceReference("ParallelGroupScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("ParallelGroupEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/ParallelGroupEnvironment.java", 27,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1" -> List.of(
                        new ScenarioSourceReference("ParallelGroupScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("ParallelGroupPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/producer/ParallelGroupPublisher.java", 15,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("ParallelGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupTracker.java", 34,
                                "Correlate received records with the current experiment.")
                );
                case "group-box" -> List.of(
                        new ScenarioSourceReference("ParallelGroupEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/ParallelGroupEnvironment.java", 27,
                                "Configure the isolated application, topic names, and consumer groups."),
                        new ScenarioSourceReference("ParallelGroupConsumerA",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/consumer/ParallelGroupConsumerA.java", 20,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("ParallelGroupConsumerB",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/consumer/ParallelGroupConsumerB.java", 20,
                                "Kafka subscription and record handling.")
                );
                case "consumer-a" -> List.of(
                        new ScenarioSourceReference("ParallelGroupConsumerA",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/consumer/ParallelGroupConsumerA.java", 20,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("ParallelGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupTracker.java", 34,
                                "Correlate received records with the current experiment.")
                );
                case "consumer-b" -> List.of(
                        new ScenarioSourceReference("ParallelGroupConsumerB",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/consumer/ParallelGroupConsumerB.java", 20,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("ParallelGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupTracker.java", 34,
                                "Correlate received records with the current experiment.")
                );
                case "observer" -> List.of(
                        new ScenarioSourceReference("ParallelGroupExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupExperiment.java", 28,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("ParallelGroupTracker",
                                "src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupTracker.java", 34,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("ParallelGroupScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/ParallelGroupScenarioTest.java", 21,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "earliest-vs-latest" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("OffsetResetPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/producer/OffsetResetPublisher.java", 15,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("EarliestVsLatestExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/EarliestVsLatestExperiment.java", 26,
                                "Run the experiment and verify its observations.")
                );
                case "topic-box" -> List.of(
                        new ScenarioSourceReference("EarliestVsLatestScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/EarliestVsLatestScenarioApplication.java", 24,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("EarliestVsLatestEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/EarliestVsLatestEnvironment.java", 25,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0" -> List.of(
                        new ScenarioSourceReference("EarliestVsLatestScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/EarliestVsLatestScenarioApplication.java", 24,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("OffsetResetPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/producer/OffsetResetPublisher.java", 15,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("OffsetResetTracker",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/OffsetResetTracker.java", 30,
                                "Correlate received records with the current experiment.")
                );
                case "earliest-group", "earliest-consumer" -> List.of(
                        new ScenarioSourceReference("EarliestConsumer",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/consumer/EarliestConsumer.java", 20,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("OffsetResetConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/OffsetResetConsumerControl.java", 9,
                                "Start or stop individual listener containers."),
                        new ScenarioSourceReference("EarliestVsLatestEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/EarliestVsLatestEnvironment.java", 25,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "latest-group", "latest-consumer" -> List.of(
                        new ScenarioSourceReference("LatestConsumer",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/consumer/LatestConsumer.java", 20,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("OffsetResetConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/OffsetResetConsumerControl.java", 9,
                                "Start or stop individual listener containers."),
                        new ScenarioSourceReference("EarliestVsLatestEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/EarliestVsLatestEnvironment.java", 25,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "observer" -> List.of(
                        new ScenarioSourceReference("EarliestVsLatestExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/EarliestVsLatestExperiment.java", 26,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("OffsetResetTracker",
                                "src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/OffsetResetTracker.java", 30,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("EarliestVsLatestScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/earliestvslatest/EarliestVsLatestScenarioTest.java", 23,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "consumer-group-rebalance-join" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("RebalancePublisher",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/producer/RebalancePublisher.java", 11,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("RebalanceExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceExperiment.java", 20,
                                "Run the experiment and verify its observations.")
                );
                case "topic-box" -> List.of(
                        new ScenarioSourceReference("RebalanceScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceScenarioApplication.java", 22,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("RebalanceOnJoinEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/RebalanceOnJoinEnvironment.java", 19,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1" -> List.of(
                        new ScenarioSourceReference("RebalanceScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceScenarioApplication.java", 22,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("RebalancePublisher",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/producer/RebalancePublisher.java", 11,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("RebalanceTracker",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceTracker.java", 27,
                                "Correlate received records with the current experiment.")
                );
                case "group-box" -> List.of(
                        new ScenarioSourceReference("RebalanceOnJoinEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/RebalanceOnJoinEnvironment.java", 19,
                                "Configure the isolated application, topic names, and consumer groups."),
                        new ScenarioSourceReference("RebalanceConsumerA",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/consumer/RebalanceConsumerA.java", 17,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("RebalanceConsumerB",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/consumer/RebalanceConsumerB.java", 17,
                                "Kafka subscription and record handling.")
                );
                case "consumer-a" -> List.of(
                        new ScenarioSourceReference("RebalanceConsumerA",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/consumer/RebalanceConsumerA.java", 17,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("RebalanceTracker",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceTracker.java", 27,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("RebalanceConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceConsumerControl.java", 8,
                                "Start or stop individual listener containers.")
                );
                case "consumer-b" -> List.of(
                        new ScenarioSourceReference("RebalanceConsumerB",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/consumer/RebalanceConsumerB.java", 17,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("RebalanceTracker",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceTracker.java", 27,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("RebalanceConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceConsumerControl.java", 8,
                                "Start or stop individual listener containers.")
                );
                case "coordinator" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("RebalanceTracker",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceTracker.java", 34,
                                "Track partition assignment and revocation callbacks."),
                        new ScenarioSourceReference("RebalanceConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceConsumerControl.java", 8,
                                "Start or stop individual listener containers.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "multiple-consumer-groups" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("SharedOrderPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/producer/SharedOrderPublisher.java", 17,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("MultipleConsumerGroupsExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsExperiment.java", 31,
                                "Run the experiment and verify its observations.")
                );
                case "topic-box" -> List.of(
                        new ScenarioSourceReference("MultipleConsumerGroupsScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("MultipleConsumerGroupsEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/MultipleConsumerGroupsEnvironment.java", 30,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0" -> List.of(
                        new ScenarioSourceReference("MultipleConsumerGroupsScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsScenarioApplication.java", 23,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("SharedOrderPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/producer/SharedOrderPublisher.java", 17,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("MultipleConsumerGroupsTracker",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsTracker.java", 27,
                                "Correlate received records with the current experiment.")
                );
                case "audit-group", "audit-consumer" -> List.of(
                        new ScenarioSourceReference("AuditGroupConsumer",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/consumer/AuditGroupConsumer.java", 15,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("MultipleConsumerGroupsTracker",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsTracker.java", 27,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("MultipleConsumerGroupsEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/MultipleConsumerGroupsEnvironment.java", 30,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "notification-group", "notification-consumer" -> List.of(
                        new ScenarioSourceReference("NotificationGroupConsumer",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/consumer/NotificationGroupConsumer.java", 15,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("MultipleConsumerGroupsTracker",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsTracker.java", 27,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("MultipleConsumerGroupsEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/MultipleConsumerGroupsEnvironment.java", 30,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "observer" -> List.of(
                        new ScenarioSourceReference("MultipleConsumerGroupsExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsExperiment.java", 31,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("MultipleConsumerGroupsTracker",
                                "src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsTracker.java", 27,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("MultipleConsumerGroupsScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/multipleconsumergroups/MultipleConsumerGroupsScenarioTest.java", 22,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "consumer-group-consumer-failure" -> switch (nodeId) {
                case "producer" -> List.of(
                        new ScenarioSourceReference("TakeoverPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/producer/TakeoverPublisher.java", 3,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("TakeoverExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverExperiment.java", 6,
                                "Run the experiment and verify its observations.")
                );
                case "topic-box" -> List.of(
                        new ScenarioSourceReference("TakeoverScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverScenarioApplication.java", 4,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("ConsumerFailureEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/ConsumerFailureEnvironment.java", 3,
                                "Configure the isolated application, topic names, and consumer groups.")
                );
                case "partition-0", "partition-1" -> List.of(
                        new ScenarioSourceReference("TakeoverScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverScenarioApplication.java", 4,
                                "Declare the topic topology and scenario beans."),
                        new ScenarioSourceReference("TakeoverPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/producer/TakeoverPublisher.java", 3,
                                "Send records to Kafka and choose the record key or partition."),
                        new ScenarioSourceReference("TakeoverTracker",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java", 6,
                                "Correlate received records with the current experiment.")
                );
                case "group-box" -> List.of(
                        new ScenarioSourceReference("ConsumerFailureEnvironment",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/ConsumerFailureEnvironment.java", 3,
                                "Configure the isolated application, topic names, and consumer groups."),
                        new ScenarioSourceReference("TakeoverConsumerA",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/consumer/TakeoverConsumerA.java", 3,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("TakeoverConsumerB",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/consumer/TakeoverConsumerB.java", 3,
                                "Kafka subscription and record handling.")
                );
                case "consumer-a" -> List.of(
                        new ScenarioSourceReference("TakeoverConsumerA",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/consumer/TakeoverConsumerA.java", 3,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("TakeoverTracker",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java", 6,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("TakeoverConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverConsumerControl.java", 3,
                                "Start or stop individual listener containers.")
                );
                case "consumer-b" -> List.of(
                        new ScenarioSourceReference("TakeoverConsumerB",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/consumer/TakeoverConsumerB.java", 3,
                                "Kafka subscription and record handling."),
                        new ScenarioSourceReference("TakeoverTracker",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java", 6,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("TakeoverConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverConsumerControl.java", 3,
                                "Start or stop individual listener containers.")
                );
                case "coordinator" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("TakeoverTracker",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java", 7,
                                "Track partition assignment and revocation callbacks."),
                        new ScenarioSourceReference("TakeoverConsumerControl",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverConsumerControl.java", 3,
                                "Start or stop individual listener containers.")
                );
                case "lag-view" -> List.of(
                        new ScenarioSourceReference("TakeoverExperiment",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverExperiment.java", 6,
                                "Run the experiment and verify its observations."),
                        new ScenarioSourceReference("TakeoverTracker",
                                "src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java", 6,
                                "Correlate received records with the current experiment."),
                        new ScenarioSourceReference("ConsumerFailureScenarioTest",
                                "src/test/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/ConsumerFailureScenarioTest.java", 3,
                                "Integration assertions for this scenario.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            case "system-ready" -> switch (nodeId) {
                case "spring-app" -> List.of(
                        new ScenarioSourceReference("SystemReadyScenarioApplication",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/app/SystemReadyScenarioApplication.java", 12,
                                "Import the baseline components into the readiness application."),
                        new ScenarioSourceReference("SystemReadyProbe",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java", 25,
                                "Check injected beans; this does not check broker health.")
                );
                case "publisher" -> List.of(
                        new ScenarioSourceReference("TradeEventPublisher",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/producer/TradeEventPublisher.java", 18,
                                "Baseline Kafka publisher."),
                        new ScenarioSourceReference("SystemReadyProbe",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java", 25,
                                "Check publisher bean presence.")
                );
                case "listener" -> List.of(
                        new ScenarioSourceReference("TradeEventListener",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/consumer/TradeEventListener.java", 13,
                                "Baseline Kafka subscription."),
                        new ScenarioSourceReference("SystemReadyProbe",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java", 29,
                                "Check listener bean presence.")
                );
                case "kafka" -> List.of(
                        new ScenarioSourceReference("docker-compose.yml",
                                "docker-compose.yml", 4,
                                "External Kafka broker: image, listeners, and single-broker settings."),
                        new ScenarioSourceReference("application.yml",
                                "src/main/resources/application.yml", 7,
                                "Spring Kafka bootstrap servers and serialization defaults."),
                        new ScenarioSourceReference("SystemReadyProbe",
                                "src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java", 33,
                                "Only client wiring is checked; the external broker is not probed.")
                );
                default -> throw new IllegalArgumentException("No source references for " + scenarioId + "/" + nodeId);
            };
            default -> throw new IllegalArgumentException("No source references for scenario: " + scenarioId);
        };
    }
}
