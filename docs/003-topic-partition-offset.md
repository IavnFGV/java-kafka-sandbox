# 003. Topic, partition и offset

## Было / стало

- Было: `0fef6ba` — схема объясняла partitions заранее заданной анимацией.
- Стало: `50e363a` — отдельное приложение отправляет и читает реальные Kafka records.

## Ментальная модель

Kafka topic — логическое имя потока, но записи физически находятся в partitions.
Каждая partition является отдельным упорядоченным append-only log и имеет
собственную последовательность offsets.

Полный адрес записи состоит из трёх частей:

`topic + partition + offset`.

Поэтому offset без partition ничего однозначно не определяет. В двух partitions
могут одновременно существовать записи с одинаковым offset. Глобального порядка
между partitions Kafka не обещает.

## Что делает эксперимент

Сценарное приложение создаёт topic `scenario-003-partition-offsets` с двумя
partitions. `PartitionedEventPublisher` выполняет три явные отправки:

1. Событие A в partition `0`.
2. Событие B в partition `1`.
3. Событие C снова в partition `0`.

Мы выбираем partitions явно, чтобы эксперимент был детерминированным. Выбор по
key и работа стандартного partitioner-а станут отдельной темой.

Первый offset partition `0` обозначим `N`, offset partition `1` — `M`. Третья
запись должна получить в partition `0` значение больше `N`. Мы намеренно не
проверяем абсолютные `0, 0, 1`: topic сохраняется между запусками, поэтому
реальные значения могут быть, например, `12, 7, 13`.

## Как подписан consumer

`PartitionedEventListener` подписывается на весь topic через `@KafkaListener`, а
не на конкретную partition. Поскольку consumer один, Kafka consumer group
назначает ему обе partitions:

```text
Partition 0 ─┐
             ├→ PartitionedEventListener
Partition 1 ─┘
```

Listener принимает `ConsumerRecord`, а не только payload. Поэтому доступны
`topic()`, `partition()` и `offset()` реально прочитанной записи.

## Как подтверждается результат

Перед отправкой `TopicPartitionOffsetsExperiment` регистрирует в
`PartitionedEventTracker` ожидание каждого уникального `eventId`. Это делается
заранее, чтобы быстрый listener не успел доставить запись раньше регистрации
ожидания.

Producer возвращает `SendResult` с координатами сохранённой записи. Listener
получает `ConsumerRecord` с координатами прочитанной записи. Experiment сравнивает
их попарно. Успех означает, что:

- Kafka подтвердила все три append;
- consumer получил именно три события текущего запуска;
- producer и consumer увидели одинаковые partition/offset;
- offset второй записи partition `0` оказался больше первого.

`TopicPartitionOffsetsScenarioStarter` переводит этот результат в события UI.
В runtime log появляются фактические значения, а broker, topic, partitions,
producer и consumer подсвечиваются только после подтверждения.

## Архитектурная граница

Как и в `002`, `TopicPartitionOffsetsEnvironment` поднимает отдельный Spring
context на случайном HTTP-порту. Повторный Play использует тот же context, но
создаёт новые events; Stop закрывает context. Внутреннее HTTP API запускает
эксперимент, но сами сообщения отправляются в Kafka через `KafkaTemplate`.

Сценарии `001` и `002` не переиспользуют Kafka-бины `003`. Так следующий
эксперимент можно менять или ломать, не затрагивая уже пройденные примеры.

## Практический вывод

Consumer обычно подписывается на topic, а Kafka назначает ему partitions. Offset
является локальной позицией внутри назначенной partition. Именно поэтому
consumer progress, ordering и дальнейшее масштабирование всегда рассматриваются
по partitions, а не только по имени topic.
