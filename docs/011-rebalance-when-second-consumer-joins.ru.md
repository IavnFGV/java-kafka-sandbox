# 011. Rebalance When a Second Consumer Joins

## Что проверяем

Сценарий отвечает на вопросы #41 и #94: что происходит с consumer group во время масштабирования и почему подключение нового consumer может временно остановить обработку.

Topic содержит две partition. Сначала запускается только Consumer A, поэтому Kafka назначает ему обе. После контрольной публикации стартует Consumer B. Изменение состава группы запускает rebalance: прежние назначения отзываются, группа согласует новое распределение, затем каждый consumer получает отдельную partition.

Важно, что UI не предполагает, кому достанется P0 или P1. [`RebalanceTracker`](../src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceTracker.java) получает реальные `onPartitionsRevoked` и `onPartitionsAssigned` callbacks и строит карты ownership из ответа Kafka.

## Последовательность

1. Управляемый listener-контейнер Consumer A запускается отдельно.
2. Backend ожидает фактическое назначение обеих partition одному владельцу.
3. В каждую partition отправляется запись, и tracker проверяет фактического обработчика.
4. Запускается Consumer B.
5. Backend ожидает стабильную карту с двумя разными владельцами.
6. Новая пара записей подтверждает работу нового распределения.

Listener-контейнеры объявлены с `autoStartup=false`, а запускает их [`RebalanceConsumerControl`](../src/main/java/io/drozda/sandbox/scenario/rebalancewhensecondconsumerjoins/app/RebalanceConsumerControl.java). Благодаря этому сценарий управляет моментом join, не имитируя rebalance вручную.

## Практический смысл

Rebalance нужен для масштабирования и восстановления, но это координационная операция. Во время изменения ownership обработка может приостановиться. Долгие callback, частые рестарты, нестабильные consumer и неудачные настройки таймаутов превращают полезное перераспределение в rebalance storm.

После стабилизации группа снова обеспечивает правило: одна partition принадлежит только одному consumer внутри группы. При этом один consumer до масштабирования вполне может владеть несколькими partition.

Каждый `Play` создает новые topic и group-id, поэтому демонстрация начинается с чистого состава группы.

## Было и стало

До реализации: `f55bd4a`.

После реализации: `ba04c13`.
