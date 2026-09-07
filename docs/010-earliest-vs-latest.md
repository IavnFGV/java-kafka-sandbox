# 010. Earliest vs Latest

## Практическая задача

Новый сервис подключается к существующему Kafka topic, в котором уже накопились события. Должен ли он восстановить состояние из всей истории или обрабатывать только новые события? Это решение задает `auto.offset.reset`, но только если у consumer group еще нет валидного committed offset.

Сценарий 010 покрывает вопрос #16 из Kafka backlog и сравнивает две новые независимые группы на одном потоке.

## Эксперимент

Topic содержит одну partition, чтобы Kafka-координаты были очевидны. Оба listener создаются с `autoStartup=false`. Их политики объявлены непосредственно в [`EarliestConsumer`](../src/main/java/io/drozda/sandbox/scenario/earliestvslatest/consumer/EarliestConsumer.java#L20) и [`LatestConsumer`](../src/main/java/io/drozda/sandbox/scenario/earliestvslatest/consumer/LatestConsumer.java#L20).

Эксперимент выполняет строгую последовательность в [`EarliestVsLatestExperiment`](../src/main/java/io/drozda/sandbox/scenario/earliestvslatest/app/EarliestVsLatestExperiment.java#L26):

1. Producer публикует три исторические записи и получает acknowledgements Kafka.
2. Стартуют две новые группы: `earliest` и `latest`.
3. Backend ожидает assignment обеих групп, не полагаясь на случайный `sleep`.
4. Producer публикует еще две live-записи.
5. Проверка сравнивает реально полученные offsets.

Результат:

- `earliest` читает offsets `0, 1, 2, 3, 4`;
- `latest` читает только offsets `3, 4`.

На timeline сначала видна история, затем ее replay только в `earliest`. Доставка каждой live-записи обеим группам объединена в один кадр `PARALLEL`.

## Важное ограничение

`auto.offset.reset` не является командой «читать заново». Если группа уже сохранила offset, Kafka продолжит с него независимо от значения `earliest` или `latest`. Политика используется, когда offset отсутствует или больше недоступен из-за retention.

Поэтому каждый повторный `Play` пересоздает изолированное приложение сценария с уникальными topic и group-id. Это не production-рекомендация, а способ воспроизводимо демонстрировать именно поведение новой группы.

## Выбор политики

`earliest` подходит для построения проекций, миграций, аудита и сервисов, которым нужна накопленная история. `latest` подходит потребителям, которым важны только будущие сигналы, но требует осознанного принятия потери старых событий.

## Было и стало

До сценария: `59935db`.

После реализации: `280b5f8`.
