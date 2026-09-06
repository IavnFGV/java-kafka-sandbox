# 005. Ordering within one partition

## Было / стало

- Базовая модель: `553a11f` - событие получило явный бизнес-номер `sequence`.
- Реализация: `c18e577` - отдельное Spring Boot приложение проверяет порядок на реальной Kafka.

## Зачем это нужно

Сценарий `004` отвечал, **куда** Kafka направит связанные records. Стабильный
`orderId` key поместил события одного заказа в одну partition. Сценарий `005`
проверяет следующий контракт: **в каком порядке** эти records будут записаны и
переданы listener.

Partition является append-only log. Broker назначает records возрастающие
offsets, а consumer читает log последовательно:

```text
sequence 0 CREATED   -> offset N
sequence 1 VALIDATED -> offset N+1
sequence 2 RESERVED  -> offset N+2
sequence 3 PAID      -> offset N+3
sequence 4 PACKED    -> offset N+4
sequence 5 SHIPPED   -> offset N+5
```

`sequence` находится в payload и выражает ожидаемый бизнес-порядок. `offset`
назначается Kafka и выражает фактическую позицию record внутри partition. Это
разные координаты, которые experiment намеренно сравнивает.

## Реальный эксперимент

[OrderedEventPublisher](../src/main/java/io/drozda/sandbox/scenario/partitionordering/producer/OrderedEventPublisher.java#L19)
отправляет каждый event с `orderId` как key. Отправки выполняются последовательно
с ожиданием broker acknowledgement в
[PartitionOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/partitionordering/app/PartitionOrderingExperiment.java#L44).

Topic имеет три partitions, а listener запускает три consumers одной group через
[`concurrency = "3"`](../src/main/java/io/drozda/sandbox/scenario/partitionordering/consumer/OrderedEventListener.java#L21).
После rebalance каждому consumer назначается одна partition. Все шесть событий
одного key попадают в одну partition и обрабатываются её текущим владельцем.

Experiment проверяет три свойства:

1. Все observations имеют один `partition`.
2. Каждый следующий `offset` больше предыдущего.
3. Порядок callback-ов содержит `sequence 0..5` без перестановок.

Проверки находятся в
[PartitionOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/partitionordering/app/PartitionOrderingExperiment.java#L49).
Producer metadata дополнительно сопоставляется с полученными `ConsumerRecord` по
`eventId`, а не по позиции списка. Поэтому проверка координат не может случайно
скрыть нарушение callback-order.

## Как фиксируется порядок callback-ов

В `004` tracker создавал отдельный `CompletableFuture` для каждого `eventId`.
Такой подход удобен для проверки доставки, но исходный список futures способен
вернуть результаты в заранее заданном порядке независимо от реальной очередности
callback-ов.

В `005` [OrderedEventTracker](../src/main/java/io/drozda/sandbox/scenario/partitionordering/app/OrderedEventTracker.java#L28)
сначала регистрирует ожидаемый набор IDs. Каждый реальный вызов listener добавляет
record в общий `receivedInCallbackOrder` именно в момент получения:

```java
receivedInCallbackOrder.add(new TrackedOrderedRecord(
    record, consumerId(Thread.currentThread().getName())
));
```

Когда собраны все records, tracker завершает один future неизменяемой копией
списка. Таким образом, последующая проверка видит фактическую последовательность
listener callback-ов.

Tracker также ждёт стабильного assignment `3 consumers x 1 partition` в
[awaitStableAssignments](../src/main/java/io/drozda/sandbox/scenario/partitionordering/app/OrderedEventTracker.java#L63),
чтобы не принять промежуточную фазу стартового rebalance за итоговую topology.

## Визуализация

[PartitionOrderingScenarioStarter](../src/main/java/io/drozda/sandbox/scenario/partitionordering/PartitionOrderingScenarioStarter.java#L56)
строит реальные assignment-связи. Затем каждый record анимируется по пути
`publisher -> partition -> consumer`, а partition показывает последний sequence
и offset. Нода `Order Verification` зеленеет только при выполнении всех трёх
проверок.

## Граница гарантии

Kafka гарантирует порядок records внутри partition и последовательную передачу
одному consumer этой group. Гарантия не распространяется на разные partitions.
Она также не гарантирует порядок **завершения** бизнес-обработки, если listener
передаст records в параллельный executor, reactive pipeline или другой
асинхронный механизм. Тогда приложение должно самостоятельно сохранить порядок
или обеспечить идемпотентность и контроль версий состояния.

Сценарий покрывает backlog `#11 Ordering guarantees within a partition`.
