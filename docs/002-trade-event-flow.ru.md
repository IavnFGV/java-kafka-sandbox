# 002. Первое настоящее сообщение через Kafka

[English](002-trade-event-flow.en.md)

## Было / стало

- Было: `8132885` — Trade Event Flow существовал только как заранее описанная анимация.
- Стало: `18fec16` — отдельное Spring Boot-приложение публикует реальную запись и ждёт её получения через Kafka.

Сценарий `001 System Ready` проверял только устройство Spring-контекста: нужные
бины созданы, но реального сообщения ещё не было. В `002 Trade Event Flow` мы
впервые проходим полный маршрут:

`Client → Publisher → Kafka topic → Listener`.

Зелёный узел здесь означает не предположение визуализатора, а подтверждённый
факт. Publisher становится зелёным после acknowledgement от Kafka, topic — после
получения `partition` и `offset`, listener — только после получения именно того
события, которое было создано текущим запуском.

## Зачем столько классов

Сценарий разделён на три части: управление окружением, Kafka-эксперимент и
визуализация результата. Благодаря этому Kafka-код не знает о квадратиках на
экране, а визуализатор не изображает успех до получения реального результата.

### Жизненный цикл сценария

[`TradeFlowEnvironment`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowEnvironment.java#L19) управляет отдельным Spring Boot-контекстом. При первом
нажатии Play он запускает [`TradeFlowScenarioApplication`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioApplication.java#L21) на свободном HTTP-порту
и сохраняет ссылку на созданный context. Повторный Play **не создаёт ещё один
context**: существующее приложение переиспользуется. Stop вызывает `close()`, и
только следующий Play создаст новый context.

Environment также задаёт уникальный `group.id`, имя topic и настройки JSON,
после чего общается со сценарным приложением через внутренний HTTP API. Это
делает границу явной: медиатор не достаёт Kafka-бины напрямую из чужого context,
а посылает ему команду и получает структурированный ответ.

`TradeFlowScenarioApplication` — конфигурация этого маленького приложения. Она
создаёт `NewTopic` с одной partition и одной replica, а также регистрирует
publisher, listener, tracker и experiment. Свойство
`scenario.trade-flow.enabled=true` не позволяет этим бинам случайно попасть в
основное приложение или другой сценарий.

[`TradeFlowScenarioController`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioController.java#L16) предоставляет внутренние endpoints:

- `GET /status` сообщает, что компоненты созданы;
- `POST /reset` очищает незавершённые ожидания;
- `POST /commands/send-and-receive` запускает реальный эксперимент.

[`TradeFlowScenarioCommandRequest`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioCommandRequest.java#L3) содержит имя конкретного запуска, а
[`TradeFlowScenarioStatus`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowScenarioStatus.java#L3) является снимком результата: readiness компонентов,
`eventId`, topic, partition, offset и возможная ошибка.

### Kafka-эксперимент

[`TradeFlowEvent`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/model/TradeFlowEvent.java#L3) — отдельная модель сообщения сценария. У неё есть уникальный
`eventId`, бизнес-ключ `tradeId`, символ, тип события и время создания. В Kafka
ключом записи становится `tradeId`; позднее на этом можно показать выбор
partition и сохранение порядка для одинакового ключа.

[`TradeFlowPublisher`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/producer/TradeFlowPublisher.java#L20) — тонкая обёртка над
`KafkaTemplate<String, TradeFlowEvent>`. Он отправляет событие и возвращает
`CompletableFuture<SendResult<...>>`. Завершение future подтверждает, что broker
принял запись, и даёт metadata с partition и offset. Это ещё не означает, что
consumer обработал сообщение.

[`TradeFlowListener`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/consumer/TradeFlowListener.java#L16) подписывается через `@KafkaListener`. Полученное сообщение он
передаёт tracker-у. Listener намеренно ничего не знает ни об HTTP, ни о
визуализации.

[`TradeFlowEventTracker`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowEventTracker.java#L13) связывает два асинхронных момента. Перед отправкой
experiment регистрирует ожидание по `eventId` и получает `CompletableFuture`.
Когда listener принимает сообщение, tracker ищет ожидание с тем же `eventId` и
завершает future. `ConcurrentHashMap` нужен потому, что experiment и Kafka
listener работают в разных потоках. Старое сообщение из topic не сможет ложно
завершить новый запуск.

[`TradeFlowExperiment`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/app/TradeFlowExperiment.java#L27) координирует один прогон:

1. Создаёт событие с новым `eventId`.
2. Регистрирует ожидание до отправки, чтобы не пропустить быстрый ответ.
3. Ждёт acknowledgement от Kafka не более десяти секунд.
4. Отдельно ждёт matching event от listener ещё не более десяти секунд.
5. Возвращает `TradeFlowScenarioStatus` с подтверждёнными фактами или ошибкой.

### От результата к экрану

[`TradeFlowScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowScenarioStarter.java#L51) реализует общий [`ScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/spi/ScenarioStarter.java#L15). Медиатор вызывает
его команду `send-and-receive`. Starter просит environment выполнить experiment,
а затем переводит результат в события визуализатора: подсвечивает request,
publisher, broker, topic, consume edge и listener. Небольшие задержки нужны
только для читаемой анимации; Kafka-проверка выполняется по настоящим ответам, а
не по таймеру.

[`TradeFlowScenarioTest`](../src/test/java/io/drozda/sandbox/scenario/tradeeventflow/TradeFlowScenarioTest.java#L21) проходит тот же публичный путь через медиатор и настоящую
Kafka. Он проверяет READY-статусы узлов и связей, наличие partition в runtime log
и завершение сессии. В `finally` окружение всегда останавливается, чтобы тест не
оставлял вложенный Spring context.

## Что важно запомнить

Успешный `KafkaTemplate.send()` подтверждает запись в Kafka, но не end-to-end
обработку consumer-ом. Это два разных наблюдаемых события. Поэтому сценарий
отдельно ждёт broker acknowledgement и отдельно — сообщение из listener.

Kafka показана контейнером, а topic — внутренним узлом. В следующих сценариях
topic сможет содержать partitions, а Kafka cluster — несколько brokers и
replicas, не ломая уже понятную визуальную модель.
