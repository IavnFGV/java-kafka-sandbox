# 001. Что значит «Spring Boot приложение готово к работе с Kafka»

[English](001-system-ready.en.md)

## Было / стало

- Было: `11d56e5` — broker подсвечивался как доступный по факту создания `KafkaTemplate`.
- Стало: `8dc0d2b` — сценарий честно проверяет только Spring wiring, а broker остаётся внешней непроверенной зависимостью.

Перед отправкой первого сообщения полезно проверить базовую конструкцию приложения.
Внутри отдельного Spring context сценария находятся два компонента: [`TradeEventPublisher`](../src/main/java/io/drozda/sandbox/scenario/systemready/producer/TradeEventPublisher.java#L18) и
[`TradeEventListener`](../src/main/java/io/drozda/sandbox/scenario/systemready/consumer/TradeEventListener.java#L13). Publisher использует созданный Spring объект `KafkaTemplate`,
а listener объявляет метод с `@KafkaListener`.

Снаружи расположен Kafka broker. Он хранит записи в partitions и отдаёт их
consumer-группам. Важно не смешивать две разные проверки: создание Kafka-компонентов
в Spring и реальное подключение к broker.

В сценарии `001 System Ready` медиатор запускает отдельный Spring Boot context.
Специальный probe получает через dependency injection publisher, listener и
`KafkaTemplate`. Если context успешно создан и все зависимости присутствуют,
визуализатор последовательно подсвечивает внутренние компоненты приложения.

При этом Kafka broker намеренно не становится зелёным. Наличие `KafkaTemplate`
означает только, что producer client сконфигурирован в Spring. Оно не доказывает,
что broker доступен, topic существует, сообщение получило acknowledgement или
listener получил partition assignment.

Зачем нужен настолько простой сценарий? Он разделяет два класса проблем. Если
Spring context не стартует, мы ищем ошибку в конфигурации и создании бинов. Если
бины созданы, но сообщение не проходит, следующая зона поиска — сеть, broker,
topic, сериализация, consumer group и offsets.

Короткий вывод для собеседования: готовый `KafkaTemplate` — это готовность клиента,
а не подтверждение работоспособности Kafka. Настоящую end-to-end готовность можно
доказать только реальной отправкой и получением сообщения. Этим займётся сценарий
`002 Trade Event Flow`.

## Основная логика в коде

- [`SystemReadyProbe`](../src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyProbe.java#L25) — Проверка наличия внедрённых компонентов.
- [`SystemReadyScenarioStarter`](../src/main/java/io/drozda/sandbox/scenario/systemready/SystemReadyScenarioStarter.java#L51) — Отображение результата проверки Spring wiring.

## Изоляция компонентов

Publisher, listener и модель `TradeEvent` находятся в `scenario.systemready`
(подпакеты `producer`, `consumer`, `model`). Они не создаются в основном context
платформы. Их импортирует только условная `SystemReadyScenarioApplication`, которую
environment включает аргументом `scenario.system-ready.enabled=true`.
Topic/group и настройки JSON-модели также задаёт environment, а не общий YAML.

Listener имеет `autoStartup=false`: существует его бин и listener container, но
чтение Kafka не стартует. Поэтому 001 работает без доступного broker. Реальную
отправку и получение проверяет 002. Устаревший `PublisherInjectionTest` удалён;
`AllComponentsTest` проверяет изоляцию, готовность, Stop и повторный запуск.

`@VisualAction` и его аспект удалены: это был эксперимент с логированием вызовов,
не связанный с текущей timeline. События UI формирует `SystemReadyScenarioStarter`.
