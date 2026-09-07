# 008. Two partitions, two consumers, one group

[English](008-two-partitions-two-consumers.en.md)

## Было / стало

- До сценария: `6c04ea9` - 007 доказал, что второй consumer простаивает при одной partition.
- Реализация: `4b9f5bc` - добавлены реальный two-partition topic, два group members, наблюдение assignment и проверка обеих веток обработки.

## Зачем это нужно

Сценарий 007 показал ограничение: Kafka назначает partition целиком одному
consumer в group, поэтому одна partition не способна занять работой два
экземпляра сервиса. 008 меняет только один параметр: partitions становится две.

Теперь у group есть две независимые единицы работы. После rebalance Kafka может
назначить по одной partition каждому consumer. Это и есть базовый механизм
горизонтального масштабирования Kafka consumer:

```text
Partition 0 -> Consumer A
Partition 1 -> Consumer B
```

Фактическое назначение может быть обратным. Поэтому визуализатор не предполагает,
что A всегда получит P0. Он ждёт реальный assignment и строит линии по ответу
listener callbacks.

## Реальный эксперимент

[ParallelGroupScenarioApplication](../src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupScenarioApplication.java#L23)
создаёт topic с двумя partitions и два listener beans с одинаковым group id.
Отдельные beans нужны, чтобы в telemetry сохранялась понятная идентичность
`Consumer A` и `Consumer B`.

Оба listener реализуют `ConsumerSeekAware`. Например,
[ParallelGroupConsumerA](../src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/consumer/ParallelGroupConsumerA.java#L14)
передаёт tracker как assignment, так и revocation. Tracker ждёт устойчивую карту,
в которой P0 и P1 имеют разных owners.

[ParallelGroupExperiment](../src/main/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/app/ParallelGroupExperiment.java#L28)
после assignment создаёт шесть records. Чётные sequence явно отправляются в P0,
нечётные в P1. Явная partition здесь нужна для детерминированного учебного
опыта. В production partition обычно выбирается из message key.

Ожидание assignment подтверждает разных owners для P0 и P1. Последовательность
отправки создаёт по три records на partition. Затем `verify()` проверяет:

- каждый record обработан owner своей partition;
- producer metadata совпадает с `ConsumerRecord.partition()` и `offset()`;
- всего получено шесть callbacks для зарегистрированных event IDs.

Tracker фильтрует чужие event IDs, но не проверяет уникальность каждого callback.
Эти проверки не доказывают отсутствие повторных доставок.

Визуализация сначала строит реальные assignment-связи, затем показывает путь
каждого record `Producer -> Partition -> Consumer`. Нода `Parallel Assignment`
становится зелёной только после успешной проверки обеих веток.

## Отличие от сценария 006

В 006 две partitions решали бизнес-проблему head-of-line blocking между быстрым
и медленным заказами. Главным результатом было время завершения и отсутствие
глобального порядка.

В 008 payload намеренно нейтрален. Здесь изучается инфраструктурный контракт
consumer group: partitions являются слотами параллелизма, а consumers получают
эти слоты целиком. Один механизм Kafka участвует в двух сценариях, но отвечает
на разные инженерные вопросы.

## Практические границы

Равное число partitions и consumers не гарантирует одинаковую нагрузку. Если в
P0 приходит намного больше данных или её records обрабатываются дольше, Consumer
A станет bottleneck, пока Consumer B простаивает после обработки P1. Kafka
балансирует ownership partitions, а не стоимость каждого record. Это приводит к
темам hot key, hot partition и consumer lag.

Если добавить третий consumer, он останется без assignment, как второй consumer
в сценарии 007. Если один из двух consumers завершится, оставшийся после
rebalance сможет владеть обеими partitions. Следующие сценарии рассмотрят
изменение membership отдельно.

Интеграционный контракт находится в
[ParallelGroupScenarioTest](../src/test/java/io/drozda/sandbox/scenario/twopartitionstwoconsumersonegroup/ParallelGroupScenarioTest.java#L20).

Сценарий закрепляет backlog `#13 Consumer group mechanics` и
`#14 One partition assigned to one consumer in a group`.
