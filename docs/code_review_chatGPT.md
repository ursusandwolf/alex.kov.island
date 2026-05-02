# CODE REVIEW — `alex.kov.island` (`dev`)

Контекст ревью: ядро симуляционного движка, а не плагины доменов `nature` / `simcity`. Ниже — оценка именно engine-слоя, его переиспользуемости, расширяемости, надёжности и производительности. Использую только видимые в ветке `dev` core-файлы и документацию репозитория. citeturn738542view0turn886060view1turn570609view0turn561785view0turn583806view0

---

## 1. Общий обзор проекта

Проект моделирует **сеточную (grid-based) симуляцию острова**: есть мир с шириной/высотой, узлы/ячейки, сущности-«живые» (`Mortal`), покадровый цикл (`GameLoop`) и параллельные сервисы обработки узлов (`CellService`). Это уже не одноразовый скрипт: у ядра есть отдельные интерфейсы для мира, узла, snapshot-ов и tickable-задач. citeturn992963view1turn185085view2turn185085view3turn185085view5turn505580view3turn459519view0

Однако доменная модель **сильно привязана к островному сценарию**. Это видно по тому, что `Cell` хранит `Island`, отдельно знает про `Animal` и `Plant`, а `Main` напрямую стартует `NatureLauncher`, то есть entrypoint не является универсальным bootstrap-ом движка. Для будущего reusable engine это серьёзный сигнал о протекании домена в технический слой. citeturn944891view0turn323932view2

Отдельно отмечу несоответствие между заявлением в README и фактическим кодом: в README говорится о `VirtualThreadPerTaskExecutor`, `ReentrantReadWriteLock`, `EnumMap`, O(1) indexing и swap-to-remove, но `GameLoop` создаёт `Executors.newFixedThreadPool(...)`, а `Cell` использует `ArrayList` + `ReentrantLock` и линейные проходы по спискам. Это не косметика, а прямой индикатор того, что документация и реализация уже разошлись. citeturn886060view1turn459519view0turn944891view0

---

## 2. Архитектура и дизайн

Архитектурно это **гибрид orchestration-слоя, grid world abstraction и domain-heavy model**, но не полноценный engine в смысле чётко отделённого ядра от предметной области. Ближе всего это выглядит как «симулятор с попыткой вынести scheduler и world API», а не как зрелая engine-библиотека, где домен подключается адаптером. `SimulationWorld`, `SimulationNode`, `WorldSnapshot` и `Tickable` — полезная база, но рядом с ними есть жёсткие ссылки на конкретный островной домен. citeturn992963view1turn185085view2turn185085view3turn185085view5turn944891view0

### SOLID

**S — Single Responsibility**  
`Cell` делает слишком много: хранит состояние, обеспечивает синхронизацию, считает лимиты по видам, обновляет статистику острова, очищает мёртвых существ и ещё формирует строку статистики. Это уже не «ячейка», а мини-агрегат с доменным правилами и выводом в UI/лог. Такое склеивание затрудняет повторное использование и тестирование. citeturn944891view0

**O — Open/Closed**  
`GameLoop` группирует задачи по `instanceof CellService`. Это означает, что добавление нового типа фаз/задач потребует правок scheduler-а. То есть модель расширяется через изменение существующего кода, а не через регистрацию новых стратегий/планировщиков. citeturn459519view0

**L — Liskov Substitution**  
`SimulationContext` заявлен как универсальный контекст, но жёстко тащит `SimulationView` из `com.island.nature.view`. Аналогично, `SimulationWorld` и `SimulationNode` по контракту вроде бы абстрактны, но фактически предполагают grid и конкретный способ движения/координат. Для другого домена такая подстановка будет ломать ожидания. citeturn992963view3turn992963view1turn185085view2

**I — Interface Segregation**  
`SimulationWorld` совмещает конфигурацию, размерности, поиск соседей, перемещение сущностей, snapshot и инициализацию. Это слишком широкий интерфейс: разные потребители будут использовать только 20–40% методов, а остальное станет лишней зависимостью. `getConfiguration()` возвращает `Object`, что особенно плохо: это не контракт, а «любой объект, лишь бы что-то было». citeturn992963view1

**D — Dependency Inversion**  
`Main` зависит от `NatureLauncher`, `SimulationContext` зависит от `SimulationView` из доменного пакета, а `Cell` зависит от `Island`. На уровне core это показывает обратную зависимость: не домен подключается к движку, а движок тянет домен. Для reusable engine это главный архитектурный дефект. citeturn323932view2turn992963view3turn944891view0

### Coupling / Cohesion

Связность внутри `Cell` высокая, но это **ложная cohesion**: объект склеен вокруг одного сценария и плохо переиспользуется. Связность между core и domain тоже высокая, и это уже плохой coupling. В идеале engine должен знать только о generic node/entity contracts, а не о `Island`, `Animal`, `Plant`, `SimulationView` из конкретного домена. citeturn944891view0turn992963view3

---

## 3. Алгоритмы и логика симуляции

Главный цикл симуляции устроен как fixed-step loop: `GameLoop.run()` считает elapsed time через `System.currentTimeMillis()`, вызывает `runTick()`, потом спит остаток `tickDurationMs`. Это рабочая база, но она чувствительна к дрейфу, квази-реальному времени и паузам GC; для детерминированного simulation engine это слабее, чем accumulator-based loop на `nanoTime()` с явной политикой catch-up/drop-frame. citeturn459519view0

`runTick()` сначала увеличивает `tickCount`, затем тикает world, а потом обходит recurring tasks, группируя их по типу `CellService`. Такое решение не универсально: scheduler знает слишком много о конкретной реализации задач и строит порядок исполнения через `instanceof`, а не через явные фазы/стадии/приоритеты. Это будет мешать, когда появятся новые механики вроде экономики, погоды, миграций, pathfinding, AI-планирования или deferred events. citeturn459519view0

Параллельная часть тоже хрупкая. `runCellServicesParallel()` создаёт задачи по work unit-ам и вызывает `taskExecutor.invokeAll(tasks)`, но возвращённые `Future` не анализируются. Если внутренняя задача упадёт, ошибка легко потеряется на уровне `Future`, а после этого `afterTick()` всё равно выполнится. Это уже риск тихих повреждений состояния и трудноуловимых симуляционных багов. citeturn459519view0

Ещё один риск — модель синхронизации. `GameLoop` исходит из того, что один и тот же `CellService`-экземпляр безопасно использовать параллельно на нескольких work unit-ах. Но контракт `CellService` этого не гарантирует, а stateful-сервисы будут легко ловить гонки. То есть безопасность тут основана на дисциплине автора, а не на контракте движка. citeturn505580view3turn459519view0

---

## 4. Коллекции и структуры данных

Здесь самый явный технический долг — `Cell`. `animals` и `plants` хранятся в `ArrayList`, а операции вроде `addAnimal()`, `getAnimalsBySpecies()`, `countAnimalsBySpecies()` и `removeAnimal()` выполняют линейные проходы. Для небольших сцен это допустимо, но для SimCity-like или large-sim workload это начнёт доминировать по CPU. При росте плотности сущностей такие методы станут hot path. citeturn944891view0

Особенно плохо, что `getAnimals()` и `getPlants()` возвращают внутренние mutable lists напрямую. Это ломает инварианты, обходит lock discipline и делает невозможным надёжно контролировать потокобезопасность и lifetime сущностей. Если потребитель модифицирует список снаружи, `Cell` теряет контроль над своим состоянием. citeturn944891view0

`getPlantCount()` суммирует biomass в `double` и потом приводит к `int`. Это выглядит как скрытая потеря точности и дополнительный сигнал, что численная модель не централизована. Для движка лучше иметь один canonical numeric model на уровне ядра, а не разрозненные типы и конверсии в каждой domain-ячейке. citeturn944891view0turn886060view1

---

## 5. Качество кода

Нейминг в core местами приемлемый, но есть признаки размытости ответственности. Например, `SimulationContext` звучит как engine object, но фактически содержит domain view. `getConsoleView()` помечен deprecated, что означает миграционный хвост, который ещё не был вычищен. `getConfiguration()` в `SimulationWorld` слишком расплывчат для сильного API. citeturn992963view3turn992963view1

Есть и стилистические smell-ы: `Cell.getStatistics()` возвращает пользовательскую строку с русскими подписями прямо из model-layer. Это уже не model concern, а presentation concern. Подобные вещи лучше вынести в formatter/renderer/diagnostics layer. citeturn944891view0

`GameLoop` печатает ошибки напрямую в `System.err`. Для production-ready engine это слабое место: нужен структурированный logging/tracing contract, иначе observability будет фрагментированной и неуправляемой. citeturn459519view0

---

## 6. Тестируемость

Положительный момент: наличие `RandomProvider` в `SimulationContext` — хороший шаг к детерминированности и reproducibility. В README прямо заявлена работа с фиксированными seed-ами, и это правильное направление для симуляции. citeturn992963view3turn886060view1

Но изоляция ядра от домена пока слабая. `SimulationContext` зависит от `SimulationView` из nature-пакета, `Main` стартует `NatureLauncher`, а `Cell` жёстко привязан к `Island`. Это значит, что unit-testing ядра без загрузки доменного мира будет затруднён. Хороший engine должен позволять тестировать scheduler, world contract и snapshot contract без конкретной игры. citeturn992963view3turn323932view2turn944891view0

По дереву тестов видно, что есть как минимум `ReproducibilityTest`, `SimulationOptimizationTest`, `StressStabilityTest` и `ExtinctionBalanceTest`, то есть автор уже мыслит в сторону стабильности и воспроизводимости. Это плюс, но сами названия показывают, что тестовый слой пока ещё очень сильно завязан на доменные сценарии. citeturn438368view0

---

## 7. Масштабируемость и расширяемость

### Добавить новый тип сущности
Сейчас это возможно, но не дёшево. Если сущность должна жить в grid world и подчиняться `Mortal`/`SimulationNode`-контрактам, добавить её можно. Но как только сущность требует иной топологии, иной модели взаимодействия или иной жизненной цикла, придётся менять core API. citeturn185085view2turn992963view1turn185085view5

### Изменить правила взаимодействия
Правила уже частично залиты в `Cell` и, вероятно, в доменные сервисы. Из-за этого механики вроде торговли, экономики, сезонности, транспорта, строительства и политики придётся встраивать в существующие объекты, а не подключать как независимые модули. Это увеличит coupling и риск rewrite-ов. citeturn944891view0turn459519view0

### Добавить новую механику
Для ресурсов, экономики, погоды и транспорта ядру не хватает явного event bus, phase scheduler, component storage и domain-agnostic state model. Сейчас у вас есть tick-loop и node-processing, но нет полноценного механизма описания зависимостей между системами. citeturn459519view0turn992963view1turn185085view3

### Использовать код в другой игре
В другой grid-based игре часть кода переиспользуется, но в другом типе симуляции — например, agent-based city-builder с дорогами, зонированием и экономикой — core потребуется существенно перепроектировать. Сильнее всего придётся переписать world/node contracts и отделить model-layer от конкретного островного домена. citeturn992963view1turn944891view0turn323932view2

---

## 8. Потенциальные баги и риски

`Cell.getAnimalCount()`, `getAnimals()`, `getPlants()` и часть других методов читают состояние без lock-а, хотя writer methods используют `ReentrantLock`. Это создаёт риск гонок и чтения несогласованного состояния. Документация в README обещает более сильную синхронизацию, чем код реально обеспечивает. citeturn944891view0turn886060view1

`GameLoop.stop()` вызывает `join(2000)` и затем `shutdownNow()`. Если worker tasks ещё выполняются или зависли, shutdown будет грубым. Нет полноценной проверки завершения executor-а и нет жёсткого lifecycle contract для in-flight work. citeturn459519view0

`GameLoop.runCellServicesParallel()` использует общий `tickCount` и общие service instances в параллельных задачах. Если любой сервис хранит mutable state, возможны race conditions. Это особенно опасно в долгоживущих сервисах с кешами, статистикой или аккумуляторами. citeturn459519view0turn505580view3

---

## 9. Приоритетные улучшения

- **[HIGH]** Развязать engine и domain: убрать `SimulationView` из `SimulationContext`, убрать `Island`/`Animal`/`Plant` из core-ядра, сделать адаптеры на стороне доменов. citeturn992963view3turn944891view0
- **[HIGH]** Заменить type-based scheduling в `GameLoop` на явные фазы/системы/приоритеты и перестать группировать задачи через `instanceof CellService`. citeturn459519view0
- **[HIGH]** Привести README в соответствие с кодом: сейчас заявлены virtual threads, `ReentrantReadWriteLock` и O(1) buckets, но фактическая реализация этого не подтверждает. citeturn886060view1turn459519view0turn944891view0
- **[MEDIUM]** Устранить утечки mutable state: `getAnimals()/getPlants()` должны возвращать immutable snapshots/views, а не внутренние списки. citeturn944891view0
- **[MEDIUM]** Увести heavy string formatting и diagnostics из model-layer. citeturn944891view0
- **[LOW]** Улучшить observability: structured logs, tick metrics, execution timings per phase. citeturn459519view0turn886060view0

---

## 10. Рефакторинг (с примерами)

### Пример 1: отделить engine-ядро от доменной модели

**До** — core знает о природе и view: `SimulationContext` хранит `SimulationView`, а `Main` запускает `NatureLauncher`. citeturn992963view3turn323932view2

```java
// core
public class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T> world;
    private final GameLoop<T> gameLoop;
    private final SimulationView view;   // domain leak
    private final RandomProvider random;
}
```

**После** — core зависит только от абстракций, а домен подключает адаптер:

```java
public interface SimulationRenderer {
    void render(WorldSnapshot snapshot);
}

public final class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T> world;
    private final GameLoop<T> gameLoop;
    private final RandomProvider random;
    private final SimulationRenderer renderer;
}
```

Это убирает зависимость core от `nature`-пакета и делает движок подключаемым для других игр. citeturn992963view3turn185085view3

### Пример 2: заменить `instanceof`-scheduler на фазовый планировщик

**До** — `GameLoop` группирует recurring tasks по `instanceof CellService`. citeturn459519view0

```java
for (Tickable task : recurringTasks) {
    boolean isCellService = task instanceof CellService;
    ...
}
```

**После** — фазы объявляются явно:

```java
public enum Phase { PREPARE, SIMULATION, POSTPROCESS }

public interface ScheduledTask extends Tickable {
    Phase phase();
    int priority();
}

public final class Scheduler {
    void runTick(int tick) {
        runPhase(Phase.PREPARE, tick);
        runPhase(Phase.SIMULATION, tick);
        runPhase(Phase.POSTPROCESS, tick);
    }
}
```

Такой вариант лучше масштабируется под экономику, AI, погоду, строительство и другие подсистемы. Для реальных движков и ECS-подобных решений это ближе к индустриальному паттерну, чем ручная группировка по runtime type. citeturn942085search15turn942085search0turn942085search17

---

## 11. С чем сравнивать и что уже существует

Если цель — **широкий reusable simulation core**, наиболее релевантные аналоги — не столько игровые движки, сколько agent-based / multi-agent simulation toolkits. MASON прямо описывает себя как Java toolkit для больших количеств агентов без domain-specific features; Repast Simphony — open-source Java-based modeling toolkit для гибких моделей взаимодействующих агентов. Это очень близкие ориентиры для вашего случая. citeturn942085search13turn942085search6

Если нужен именно **ECS-подход для игровой логики**, Bevy показывает современную модель: все game logic построено на ECS, компоненты — простые structs, а scheduler умеет параллелить non-conflicting systems. Ashley — компактный Java ECS, вдохновлённый Ash/Artemis, с прозрачным API для entity-system подхода. citeturn942085search15turn942085search0

### Практический вывод по сравнению
Ваш проект сейчас ближе всего к **MASON/Repast по цели** и к **легковесному ECS/scheduler-ядру по архитектурным приёмам**, но пока не дотягивает до зрелой reusable platform из-за доменных утечек, слабого контракта синхронизации и того, что scheduler знает слишком много о конкретных task types. citeturn992963view1turn459519view0turn944891view0turn992963view3

---

## 12. Итоговая оценка

- **Архитектура:** 4/10  
- **Код:** 5/10  
- **Переиспользуемость:** 3/10  
- **Общая оценка:** 4/10  

**Короткий вердикт:** как основа для универсального симуляционного движка проект пока **не готов**. У него уже есть полезные кирпичи — tick-loop, world/node/snapshot contracts, deterministic random abstraction, попытка параллелизма — но core слишком тесно связан с конкретным островным доменом, а документация и реализация по ключевым пунктам расходятся. До reusable engine нужно сначала вычистить domain leakage, формализовать scheduler, нормализовать data structures и привести concurrency contract к уровню, который можно безопасно расширять без переписывания ядра. citeturn459519view0turn992963view1turn992963view3turn944891view0turn886060view1
