# 007. One partition, two consumers, one group

## Было / стало

- До сценария: `1f8d3dd` - сценарий 006 показал параллелизм независимых partitions.
- Реализация: `f04011c` - статическая схема 007 заменена реальным assignment, остановкой owner и takeover после rebalance.

## Зачем это нужно

После сценария 006 возникает естественная идея: если consumers дают параллельную
обработку, можно просто запускать их больше. Но Kafka масштабирует consumer group
не по количеству records, а по количеству partitions.

Внутри одной group partition в каждый момент назначена не более чем одному
consumer. Поэтому для topic с одной partition два consumers дают такую картину:

```text
Partition 0 -> Consumer A
               Consumer B: idle
```

Kafka не раздаёт чётные records Consumer A, а нечётные Consumer B. Partition
является неделимой единицей assignment. Иначе два обработчика могли бы читать
один ordered log одновременно и разрушать его порядок.

Практический вывод: throughput одной group ограничен количеством partitions.
Если сервис развернул десять экземпляров, а topic содержит три partitions,
работать с этим topic смогут максимум три экземпляра. Остальные останутся
участниками group без assignment.

## Почему idle consumer всё-таки полезен

Лишний consumer не добавляет параллелизма, но может дать резерв мощности. Если
текущий owner завершится, изменится membership group. Group coordinator запустит
rebalance, и свободный consumer сможет получить Partition 0:

```text
до остановки:    Partition 0 -> Consumer A; Consumer B idle
после rebalance: Partition 0 -> Consumer B; Consumer A stopped
```

Это не active-active обработка одной partition. Это смена единственного owner.
Между остановкой и новым assignment возникает пауза, поэтому rebalance влияет на
latency. Следующие сценарии отдельно углубят механику rebalance и failure
detection; здесь мы фиксируем базовый контракт ownership.

## Два настоящих consumers

В сценарии нет `concurrency = 2` на одном методе, потому что нам нужно отдельно
управлять каждым участником. Созданы два listener beans. Например,
[ConsumerGroupMemberA](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberA.java#L14)
имеет стабильные `ID` и `LABEL`, а его `@KafkaListener` подписан на тот же topic и
тот же group id, что и Consumer B.

Оба реализуют `ConsumerSeekAware`. Callback
[`onPartitionsAssigned`](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/consumer/ConsumerGroupMemberA.java#L30)
сообщает tracker фактический assignment, а `onPartitionsRevoked` убирает старое
владение. Эти callbacks приходят от Spring Kafka listener container в ответ на
реальный consumer-group protocol.

Нельзя заранее утверждать, что Partition 0 получит именно Consumer A. Результат
зависит от момента join, выбранного assignor и текущего состояния group. Поэтому
[SinglePartitionGroupTracker](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupTracker.java#L50)
ждёт состояние, в котором у partition ровно один owner, и возвращает его label.
Второй участник вычисляется как idle consumer только после наблюдения assignment.

## Как останавливается один listener

Остановка всего scenario application не подходит: вместе с owner исчез бы и
standby consumer. Для адресного управления используется
[ConsumerMemberControl](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/ConsumerMemberControl.java#L9).

Spring Kafka регистрирует containers по значениям `@KafkaListener(id=...)` в
`KafkaListenerEndpointRegistry`. Control находит container текущего owner и
вызывает `stop()`. Broker видит изменение состава group, оставшийся consumer
проходит rebalance и получает Partition 0.

Перед каждым повторным Play метод `startBoth()` запускает оба containers. Это
важно для playground: остановленный в прошлом прогоне listener не должен менять
начальные условия следующего эксперимента.

## Последовательность эксперимента

Основная оркестрация находится в
[SinglePartitionGroupExperiment](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/app/SinglePartitionGroupExperiment.java#L36):

1. Запускаются оба listener containers.
2. Tracker ждёт единственного owner Partition 0.
3. В topic публикуются три records фазы `BEFORE FAILURE`.
4. Проверяется, что всю первую пачку получил только owner.
5. Container owner останавливается.
6. Tracker ждёт нового owner, отличного от предыдущего.
7. Публикуются ещё три records фазы `AFTER TAKEOVER`.
8. Проверяется, что всю вторую пачку получил бывший idle consumer.

Каждый batch регистрирует уникальные event IDs до публикации и ждёт все
соответствующие callbacks. Producer metadata сопоставляется с фактическим
`ConsumerRecord` по event ID, partition и offset. Поэтому успешный send нельзя
ошибочно принять за успешную обработку.

Сценарий использует уникальные topic и group names при каждом старте отдельного
Spring context в
[SinglePartitionGroupEnvironment](../src/main/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupEnvironment.java#L30).
Старые records и committed offsets не вмешиваются в новый запуск.

## Что показывает UI

Сначала оба consumers видимы как здоровые group members. После реального
assignment только owner получает связь от Partition 0 и зелёный статус. Второй
помечается `WAITING`, а не `FAILED`: он исправен, ему просто нечего назначить.

Первая пачка анимируется только через owner. Затем owner становится красным после
адресного stop, assignment-связь перестраивается к оставшемуся consumer, и вторая
пачка идёт уже через него. Нода `Assignment and Takeover` показывает фактические
имена initial owner, idle member и takeover owner.

## Что проверяет тест

[SinglePartitionGroupScenarioTest](../src/test/java/io/drozda/sandbox/scenario/onepartitiontwoconsumersonegroup/SinglePartitionGroupScenarioTest.java#L20)
закрепляет наблюдаемые свойства:

- initial owner и idle consumer различаются;
- takeover owner равен прежнему idle consumer;
- первая пачка обработана только initial owner;
- вторая пачка обработана только takeover owner;
- все шесть records принадлежат Partition 0;
- mediator завершает timeline, а observer получает статус `READY`.

Тест также запускает experiment второй раз через mediator. Тем самым проверяется
важное для интерактивного playground поведение повторного Play.

## Граница вывода

Этот сценарий не доказывает exactly-once обработку при failure. Если consumer
успел выполнить внешний side effect, но не успел зафиксировать offset, новый
owner может получить record повторно. Это отдельные темы offset commits,
at-least-once и idempotent consumer.

Также standby consumer не делает failover мгновенным. Скорость зависит от
heartbeat/session timeout, способа штатного выхода и rebalance protocol.

Главный вывод сценария: **число активных consumers одной group не может быть
больше числа partitions, но лишний здоровый consumer способен принять partition
после смены membership**.

Сценарий покрывает backlog `#13 Consumer group mechanics` и
`#14 One partition assigned to one consumer in a group`.
