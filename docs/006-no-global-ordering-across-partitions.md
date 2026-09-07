# 006. Parallel orders without global ordering

## Было / стало

- До сценария: `601fbc3` - завершена общая timeline-визуализация предыдущих экспериментов.
- Реализация: `50cc164` - добавлено отдельное Spring Boot приложение, две Kafka-топологии, UI-переключатель и интеграционный тест.

## Какой практический вопрос мы решаем

Сценарий `004` показал, как стабильный business key направляет события одной
сущности в одну partition. Сценарий `005` подтвердил, что Kafka сохраняет порядок
records внутри этой partition. Но отсюда легко сделать неверный вывод: будто
упорядочен весь topic.

Сценарий `006` показывает одновременно две стороны partitioning:

1. Независимые partitions позволяют обрабатывать разные заказы параллельно.
2. Между partitions нет общего порядка обработки или завершения.

Практическая проблема называется **head-of-line blocking**. Если быстрый заказ и
длинный заказ попали в одну partition, один consumer получает общий
последовательный поток. События быстрого заказа могут ждать, пока завершится
обработка промежуточных событий длинного заказа. Это корректно, но увеличивает
latency независимой работы.

## Два заказа

Experiment создаёт одинаковый interleaved поток для обеих топологий:

```text
Fast: CREATED ---------------- PAID ---------------- COMPLETED
Slow:         CREATED -> VALIDATING -> RESERVED -> PACKING -> SHIPPED -> COMPLETED
```

У Fast Order три состояния и короткая имитация обработки. У Slow Order шесть
состояний и более долгая обработка. События создаются в
[GlobalOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/globalordering/app/GlobalOrderingExperiment.java#L78),
а их единый порядок публикации явно задан на строках 85-88. Задержка находится в
payload только ради наблюдаемого учебного эксперимента. В production обработчик
не должен доверять клиентскому полю, задающему время работы.

## Режим 1: одна partition, один consumer

UI по умолчанию выбирает `1 partition / 1 consumer`. Все девять records явно
отправляются в partition 0. Единственный listener обрабатывает их строго один за
другим. Поэтому:

- порядок публикации и завершения совпадает;
- последовательность каждого заказа сохраняется;
- Fast Order ждёт оказавшиеся перед ним стадии Slow Order;
- throughput ограничен одним consumer loop.

Это не ошибка Kafka. Мы попросили одну ordered queue и получили именно её.

## Режим 2: две partitions, два consumers

В режиме `2 partitions / 2 consumers` Fast Order направляется в partition 0, а
Slow Order в partition 1. Выбор сделан явно в
[GlobalOrderingExperiment](../src/main/java/io/drozda/sandbox/scenario/globalordering/app/GlobalOrderingExperiment.java#L52),
чтобы эксперимент был детерминированным. В обычном producer стабильный
`orderId` можно передать как key и позволить partitioner выбрать shard. Важно не
конкретное число partition, а контракт: один order всегда использует один key.

[GlobalOrderListener](../src/main/java/io/drozda/sandbox/scenario/globalordering/consumer/GlobalOrderListener.java#L21)
содержит две учебные подписки. Первая имеет `concurrency = 1` для single-topic.
Вторая имеет `concurrency = 2` для parallel-topic. Spring Kafka создаёт два child
containers, то есть два реальных Kafka consumers одной group. После assignment
один владеет partition 0, второй partition 1.

Теперь consumer loops работают независимо. Fast Order способен завершиться,
пока Slow Order ещё проходит свои стадии. Общий completion order больше не обязан
совпадать с publish order. При этом `CREATED -> PAID -> COMPLETED` внутри Fast
Order и вся цепочка Slow Order остаются упорядоченными.

Именно это означает формулировка **Kafka не даёт глобального порядка между
partitions**. Она не означает случайный порядок внутри каждой partition.

## Что измеряет приложение

[GlobalOrderTracker](../src/main/java/io/drozda/sandbox/scenario/globalordering/app/GlobalOrderTracker.java#L25)
регистрирует ожидаемые event IDs и время начала запуска. Listener имитирует
работу, затем tracker сохраняет record в фактический момент завершения вместе с
consumer id и elapsed time. Поэтому результирующий список отражает completion
order, а не заранее известный publish order.

Experiment проверяет:

- все события опубликованы и прочитаны;
- producer и consumer видят одинаковые `partition + offset`;
- sequence каждого заказа равна `0..N`;
- совпадает ли глобальный completion order с publish order;
- через сколько миллисекунд завершились Fast и Slow orders.

В parallel-режиме ожидается, что Fast завершится раньше Slow и раньше, чем Fast
завершался в single-режиме. Эти свойства закреплены интеграционным тестом
[GlobalOrderingScenarioTest](../src/test/java/io/drozda/sandbox/scenario/globalordering/GlobalOrderingScenarioTest.java#L23).

## Почему здесь отдельное Spring Boot приложение

[GlobalOrderingEnvironment](../src/main/java/io/drozda/sandbox/scenario/globalordering/GlobalOrderingEnvironment.java#L31)
поднимает изолированный Spring context сценария. Для каждого старта создаются
уникальные topic и group names. Это не даёт старым offsets и records изменить
результат нового учебного запуска. Кнопка Stop закрывает context и все listener
containers вместе с ним.

Медиатор не воспроизводит придуманную Kafka-анимацию. Он получает observations
из сценарного приложения, а
[GlobalOrderingScenarioStarter](../src/main/java/io/drozda/sandbox/scenario/globalordering/GlobalOrderingScenarioStarter.java#L54)
переводит их в timeline: publisher, фактическая partition, назначенный consumer,
status, offset и время завершения.

## Что сценарий не доказывает

Он не сравнивает максимальный throughput и не является benchmark: искусственные
задержки, локальная Kafka и маленькая выборка для этого не подходят. Он также не
обещает, что два business keys обязательно попадут в разные partitions при
обычном hashing. Разные keys могут столкнуться в одном shard.

Вывод уже и полезнее: partition является единицей порядка и параллелизма. Нужно
сохранять порядок там, где он имеет бизнес-смысл, и не создавать глобальную
очередь для независимых сущностей без необходимости. Если системе всё-таки нужен
единый total order, придётся использовать одну partition или строить отдельный
механизм координации, принимая потерю масштабируемости и рост сложности.

Сценарий покрывает backlog `#12 Why global ordering does not exist across partitions`.
