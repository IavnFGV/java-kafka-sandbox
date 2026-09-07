# Scenario documentation

Scenario articles use an explicit language suffix:

```text
<scenario-number>-<slug>.<language>.md
```

For example:

- `004-message-key-partition-selection.ru.md` — Russian
- `004-message-key-partition-selection.en.md` — English

Keep the scenario number and slug identical across translations so links can select a language
without maintaining a separate scenario-to-document mapping.

The `*-a-*` documents are supplementary articles for the corresponding numbered scenario and
follow the same language convention.

All 14 articles are available in Russian and English. The articles include historical
before/after notes; completed changes in those sections are not a current task list. Each version links to the relevant
implementation using repository-relative paths and `#L` line anchors. Update these anchors
when the referenced code moves. `README.en.md` is the explicitly suffixed English copy of this index.

## Platform walkthrough

[How the platform works (RU)](platform-architecture.ru.md) /
[English](platform-architecture.en.md): follow Play through the mediator, create and
close a scenario Spring context inside the main JVM, and trace events into the UI.
These platform guides are separate from the 14 numbered scenario articles.

## Articles

| Article | Russian | English |
| --- | --- | --- |
| 001-a. Why the visualizer kept polling the backend | [RU](001-a-from-polling-to-long-polling.ru.md) | [EN](001-a-from-polling-to-long-polling.en.md) |
| 001. What it means for a Spring Boot application to be ready for Kafka | [RU](001-system-ready.ru.md) | [EN](001-system-ready.en.md) |
| 002. The first real message through Kafka | [RU](002-trade-event-flow.ru.md) | [EN](002-trade-event-flow.en.md) |
| 003. Topic, partition, and offset | [RU](003-topic-partition-offset.ru.md) | [EN](003-topic-partition-offset.en.md) |
| 004. Message keys and partition selection | [RU](004-message-key-partition-selection.ru.md) | [EN](004-message-key-partition-selection.en.md) |
| 005-a. How to avoid losing fast events between Kafka and the visualizer | [RU](005-a-lossless-scenario-timeline.ru.md) | [EN](005-a-lossless-scenario-timeline.en.md) |
| 005. Ordering within one partition | [RU](005-ordering-within-one-partition.ru.md) | [EN](005-ordering-within-one-partition.en.md) |
| 006. Parallel orders without global ordering | [RU](006-no-global-ordering-across-partitions.ru.md) | [EN](006-no-global-ordering-across-partitions.en.md) |
| 007. One partition, two consumers, one group | [RU](007-one-partition-two-consumers.ru.md) | [EN](007-one-partition-two-consumers.en.md) |
| 008. Two partitions, two consumers, one group | [RU](008-two-partitions-two-consumers.ru.md) | [EN](008-two-partitions-two-consumers.en.md) |
| 009. Multiple Consumer Groups | [RU](009-multiple-consumer-groups.ru.md) | [EN](009-multiple-consumer-groups.en.md) |
| 010. Earliest vs Latest | [RU](010-earliest-vs-latest.ru.md) | [EN](010-earliest-vs-latest.en.md) |
| 011. Rebalance When a Second Consumer Joins | [RU](011-rebalance-when-second-consumer-joins.ru.md) | [EN](011-rebalance-when-second-consumer-joins.en.md) |
| 012. Consumer Failure and Partition Takeover | [RU](012-consumer-failure-partition-takeover.ru.md) | [EN](012-consumer-failure-partition-takeover.en.md) |

## Implementation entry points

- [`ScenarioCatalog.sourceRoot()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioCatalog.java#L30) maps scenario IDs to their implementation packages.
- [`TradeFlowExperiment.run()`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowExperiment.java#L27) demonstrates the basic publish, acknowledgement, and matching-consumer-event flow.
- [`ScenarioRuntimeService.runtimeUpdateAfter()`](../src/main/java/io/drozda/sandbox/visualization/ScenarioRuntimeService.java#L303) supplies retained transitions to the visualizer.
