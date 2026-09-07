# 004. Message key и выбор partition

[English](004-message-key-partition-selection.en.md)

## Было / стало

- Было: `7516328` — key-based partitioning существовал только в учебном плане.
- Стало: `6618e4c` — отдельное приложение проверяет маршрутизацию реальными Kafka records.
- Интерактивность: `b4120bd` — пользователь выбирает key strategy, а backend выполняет соответствующий Kafka-эксперимент.
- Consumer assignment: `1da60af` — три реальных consumers показывают назначения partitions и полный путь каждой записи.
- Routing confidence: `52cbacf` — десять событий снижают вероятность случайного совпадения, а assignment-связи перестраиваются из runtime.
- Compact topology: `198c903` — partition-ноды показывают сводку, а полная последовательность остаётся в runtime log.
- Stable assignment: `f7a4c55` — experiment учитывает revoke и ждёт завершения стартового rebalance.
- Learning boundary: `6023ab9` - описание отделяет routing из `004` от ordering из будущего `005` и показывает создание трёх consumers.

## Зачем это нужно

В event-driven системе несколько событий относятся к одной сущности. Например,
заказ `order-42` проходит состояния `CREATED`, `PAID`, `SHIPPED`. Если события
попадут в разные partitions, они смогут обрабатываться параллельно и общей
гарантии порядка между ними не будет.

Kafka message key позволяет связать маршрутизацию с бизнес-идентификатором:

```java
kafkaTemplate.send(topic, event.orderId(), event);
```

Producer не передаёт номер partition: стандартный partitioner вычисляет его по
сериализованному key. Пока число partitions неизменно, одинаковый key
маршрутизируется в одну partition.

Главное не постоянный физический consumer, а один упорядоченный log для сущности.
Параллельная обработка разных partitions может записать в общую базу `SHIPPED`,
затем запоздавший `PAID` и оставить неверное состояние.

## Чем `004` отличается от `005`

`004` отвечает на вопрос **куда попадут связанные records**. Мы выбираем key и
доказываем, что события одного заказа находятся в одной partition. Этот сценарий
создаёт необходимое условие для порядка.

`005` ответит на вопрос **в каком порядке records будут прочитаны** после
попадания в одну partition. Там мы сопоставим порядок отправки, возрастающие
offsets и порядок получения listener. Коротко: `004` проверяет маршрутизацию и
совместное размещение, а `005` проверит порядок внутри partition log.

В UI доступны `No key`, `Unique eventId` и `Order ID`. Все варианты реально
отправляются в Kafka, но только `Order ID` даёт зелёную гарантию совместной
маршрутизации событий заказа.

## Эксперимент

Topic имеет три partitions. Приложение отправляет десять последовательных
состояний `order-42` и одну запись другого заказа. Сокращённо поток выглядит так:

```text
order-42 CREATED
order-42 VALIDATED
...
order-42 PAID
...
order-42 DELIVERED
order-73 CREATED
```

Experiment получает producer metadata, а listener — полные `ConsumerRecord`.
Для каждой записи сравниваются key, partition и offset. Количество distinct partitions у записей `order-42` вычисляет
[`KeyPartitioningScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/KeyPartitioningScenarioStarter.java#L100)
для отображения результата. Флаг гарантии задаётся стратегией `ORDER_ID`; отдельного
assert на единственную partition в experiment нет.

Почему записей десять? Если каждый уникальный `eventId` независимо и равномерно
хешируется в одну из трёх partitions, вероятность случайно увидеть все десять в
одной partition равна `(1/3)^9 ≈ 0,0051%`. Для трёх событий она была бы `1/9 ≈
11,1%`, поэтому картинка слишком часто выглядела бы правильной при неправильном
key. Даже редкое совпадение не окрашивает сценарий зелёным: гарантия определяется
стратегией `Order ID`, а не одним удачным прогоном.

К `No key` этот расчёт напрямую применять нельзя: default producer может
использовать sticky partitioning и некоторое время отправлять batch без ключа в
одну partition. Это ещё один пример того, почему наблюдаемое совпадение не равно
контракту маршрутизации.

Разные keys могут столкнуться в одной partition: key создаёт стабильную группу
маршрутизации, а не персональную partition.

В визуализаторе каждая partition показывает сводку записей текущего запуска.
Пустая partition тоже отображается: это помогает увидеть, что
одиннадцать записей не обязаны равномерно заполнить три partitions.
Чтобы длинный поток не выходил за границы ноды, partition показывает компактную
сводку: количество records, диапазон offsets и последний status. Полная
последовательность остаётся в runtime log внизу страницы.

Анимация воспроизводит каждую запись отдельно: `publisher → partition N`, затем
`partition N → consumer`. Поэтому видно, куда Kafka направила конкретные
`CREATED`, `PAID` и `SHIPPED`, и какой listener их прочитал. В одной consumer
group работают три реальных consumer instance. После rebalance каждый получает
одну из трёх partitions; визуализатор читает фактическое назначение из listener
container, а не предполагает его заранее. Поэтому связанные события с правильным
key идут не только в одну partition, но в текущем assignment попадают одному
consumer. Важно: гарантию порядка создаёт общая partition, а конкретный consumer
может смениться после rebalance.

Три consumers создаются не тремя listener-классами, а параметром Spring Kafka:

```java
@KafkaListener(
    topics = "${app.kafka.topics.key-partitioning}",
    concurrency = "3"
)
public void onEvent(ConsumerRecord<String, KeyedOrderEvent> record) {
    tracker.received(record);
}
```

Spring создаёт три listener container и, соответственно, три Kafka consumer в
одной group. Точнее, внешний `ConcurrentMessageListenerContainer` управляет
тремя дочерними `KafkaMessageListenerContainer`. У каждого дочернего container
есть собственный `KafkaConsumer` и долгоживущий poll-thread:

```text
ConcurrentMessageListenerContainer
├── container-0 → KafkaConsumer → poll-thread-0
├── container-1 → KafkaConsumer → poll-thread-1
└── container-2 → KafkaConsumer → poll-thread-2
```

Все три потока вызывают один метод `onEvent`, поэтому по сигнатуре callback не
видно, какой consumer доставил запись. Для учебной телеметрии tracker читает имя
текущего listener thread:

```java
String consumerId = consumerId(Thread.currentThread().getName());
```

Один и тот же thread участвует и в callback назначения, и в обработке records.
Так [`KeyedEventTracker`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyedEventTracker.java#L53) связывает условный `Consumer A` с его partitions, а затем
помечает им полученный `ConsumerRecord`. Это позволяет визуализатору провести
record к фактическому consumer, хотя все consumers используют один Java-метод.

Класс [`KeyedOrderEventListener`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/consumer/KeyedOrderEventListener.java#L17) реализует `ConsumerSeekAware`:
`onPartitionsAssigned` передаёт назначения в `KeyedEventTracker`, а
`onPartitionsRevoked` удаляет отозванные partitions. Затем
[`KeyPartitioningScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/KeyPartitioningScenarioStarter.java#L44) преобразует карту assignments в динамические
связи `partition → consumer` на экране.

Имя thread здесь является инструментом наблюдения учебного стенда, а не частью
бизнес-контракта. Production-обработчику обычно достаточно `topic`, `partition`,
`offset`, key и payload; после restart или rebalance имя consumer/thread может
измениться.

При старте consumer group первый успевший подключиться consumer временно может
получить все partitions. Это промежуточная фаза, а не итоговый assignment.
Experiment ждёт стабильного состояния `3 consumers × 1 partition`, учитывает
callbacks назначения и отзыва partitions и только затем публикует records.
Пунктирные связи `partition → consumer` строятся из этого runtime assignment, а
не зашиты в статический граф. Новый запуск очищает старые связи и после возможного
rebalance рисует актуальные; движущиеся records отображаются поверх них.

## Практический вывод

Выбирайте key из идентификатора сущности, порядок событий которой важен:
`orderId`, `accountId`, `customerId`. Плохой или слишком популярный key может
создать hot partition, а увеличение количества partitions способно изменить
соответствие key → partition. Саму гарантию порядка мы проверим в сценарии `005`.

Сценарий покрывает backlog `#10 Message key and partition selection`.

## Основная логика в коде

- [`KeyedOrderEventPublisher`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/producer/KeyedOrderEventPublisher.java#L20) — Выбор key и отправка в Kafka.
- [`KeyPartitioningExperiment`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningExperiment.java#L66) — Сравнение key и координат producer/consumer.

## Дополнительные ссылки на реализацию

- [`KeyPartitioningExperiment`](../src/main/java/io/drozda/sandbox/scenario/messagekeypartitionselection/app/KeyPartitioningExperiment.java#L29)
