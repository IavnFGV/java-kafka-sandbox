# 012. Consumer Failure and Partition Takeover

[English](012-consumer-failure-partition-takeover.en.md)

## Что проверяем

Сценарий покрывает вопросы #13, #41 и #97: что происходит с partition после исчезновения ее consumer, почему сообщения не теряются и откуда возникает временный lag.

В начале два consumer одной группы делят две partition. Backend не предполагает конкретное распределение, а получает его из `onPartitionsAssigned`. Затем выбирается фактический владелец Partition 1 и останавливается его listener-контейнер.

После остановки owner producer отправляет новую запись в Partition 1. Backend
ждёт, пока оставшийся участник получит обе partitions и прочитает запись. Код не
проверяет, что в момент отправки partition ещё не имела owner: rebalance может
успеть закончиться до публикации. Поэтому `Temporary Lag` — учебное представление
перехода, а не измеренное окно отсутствия consumer или broker lag.

## Как устроен опыт

[`TakeoverTracker`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverTracker.java#L7) хранит реальные assignment/revoke callbacks и ожидает записи по уникальным `eventId`. [`TakeoverExperiment`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverExperiment.java#L6) выполняет последовательность:

1. Запускает оба управляемых consumer.
2. Ждет наблюдаемого распределения с двумя владельцами.
3. Проверяет обработку по одной записи из каждой partition.
4. Останавливает владельца P1 и сразу публикует туда запись.
5. Ждет, пока survivor получит обе partition.
6. Проверяет доставку ожидавшей записи и продолжение обработки.

UI строит ownership только из результата Kafka. Во время перехода `Temporary Lag` подсвечивается как ожидание, затем запись движется к survivor, а его node показывает владение P0 и P1.

## Stop и настоящий crash

В playground используется управляемый `MessageListenerContainer.stop()`. Такой consumer корректно покидает группу, поэтому coordinator узнает об изменении быстро. При аварийном завершении процесса graceful leave отсутствует: Kafka обычно ждет прекращения heartbeat до `session.timeout.ms`, и окно недоступности partition может быть длиннее.

Следовательно, этот опыт точно демонстрирует механизм reassignment и сохранность записи, но не измеряет crash-detection latency. Реальный kill контейнера остается отдельной точкой роста инфраструктуры mediator.

## Практический вывод

Kafka хранит записи в partition независимо от текущего consumer. Потеря consumer не означает потерю данных: после изменения membership partition назначается выжившему участнику. Однако во время обнаружения и rebalance растет lag, а запись после последнего committed offset может быть обработана повторно. Поэтому обработчик должен учитывать idempotency и корректную стратегию commit.

## Было и стало

До реализации: `d977767`.

После реализации: `6d5d278`.

## Основная логика в коде

- [`TakeoverConsumerControl`](../src/main/java/io/drozda/sandbox/scenario/consumerfailureandpartitiontakeover/app/TakeoverConsumerControl.java#L3) — Адресная остановка listener-контейнера.
