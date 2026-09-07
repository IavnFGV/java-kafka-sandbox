# 009. Multiple Consumer Groups

[English](009-multiple-consumer-groups.en.md)

## Зачем это нужно

В сценарии 008 два consumer находились в одной группе. Они сотрудничали: Kafka разделила две partition между ними, поэтому каждая запись была обработана группой только один раз.

Сценарий 009 отвечает на другой практический вопрос: как позволить нескольким независимым сервисам реагировать на один поток событий? Например, после создания заказа аудит должен сохранить историю, а сервис уведомлений должен отправить письмо. Ни один из них не должен «украсть» сообщение у другого.

Для этого сервисы используют разные `group.id`. Kafka хранит позицию чтения отдельно для каждой consumer group. В результате `audit-group` и `notification-group` независимо получают все три записи из одного topic. Физически Kafka не создает вторую копию записи: обе группы читают одну и ту же координату `topic-partition-offset`, но имеют собственный прогресс обработки.

## Что делает эксперимент

Изолированное Spring Boot приложение создает topic с одной partition. [`SharedOrderPublisher`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/producer/SharedOrderPublisher.java#L17) публикует три уникальных события только один раз. Два `@KafkaListener` подписаны на этот topic, но используют разные группы:

- [`AuditGroupConsumer`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/consumer/AuditGroupConsumer.java#L15) работает в `audit-group`;
- [`NotificationGroupConsumer`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/consumer/NotificationGroupConsumer.java#L15) работает в `notification-group`.

[`MultipleConsumerGroupsTracker`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsTracker.java#L27) ожидает шесть callback: по три от каждой группы. Затем эксперимент проверяет, что для каждого `eventId` обе группы увидели одинаковые partition и offset. В UI одна анимация показывает append в Kafka, после чего запись расходится в два независимых consumer group.

Эти две доставки объединены общим `playbackGroup`. Timeline сохраняет все backend-события, но сигналы с одинаковой группой собирает в один визуальный кадр. Поэтому UI может честно показывать несколько одновременных действий, не теряя их порядок в техническом журнале. Активный сигнал теперь рисует не только движущуюся точку, но и полный пунктирный путь со стрелкой между границами node.

## Практический вывод

Consumer group определяет не только масштабирование, но и логическую подписку:

- одинаковый `group.id` означает совместную обработку и разделение partition;
- разные `group.id` означают независимую доставку полного потока каждой группе;
- offsets одной группы не влияют на offsets другой.

Это позволяет подключать к одному event stream аудит, уведомления, аналитику и другие сервисы без изменения producer.

## Было и стало

До сценария: `9244dba`.

После сценария: `e6ec185`.

Улучшение визуализации: `4533a72` добавил стрелки и поддержку одновременных runtime-сигналов; `5a1bc3a` объединил такие сигналы в одну карточку `PARALLEL` на timeline и отделил `append` от последующего fan-out.

## Основная логика в коде

- [`MultipleConsumerGroupsExperiment`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsExperiment.java#L59) — Проверка доставки в обе группы и совпадения координат.

## Дополнительные ссылки на реализацию

- [`MultipleConsumerGroupsExperiment`](../src/main/java/io/drozda/sandbox/scenario/multipleconsumergroups/app/MultipleConsumerGroupsExperiment.java#L31)
