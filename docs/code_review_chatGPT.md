# CODE REVIEW — `alex.kov.island` (`dev`)

Контекст ревью: оцениваю **ядро симуляционного движка**, а не конкретные доменные плагины `nature` / `simcity`. Для меня здесь главный вопрос: можно ли на этом основании строить переиспользуемый движок для разных симуляций и игр, не переписывая основу через пару итераций.

## Краткий вывод

У проекта уже есть полезный каркас для simulation engine:

- есть абстракции `SimulationWorld`, `SimulationNode`, `SimulationPlugin`, `ScheduledTask`, `WorldSnapshot`;
- есть единый `GameLoop` с фазами, приоритетами и параллельной обработкой work units;
- есть попытка изоляции домена через плагины;
- есть тесты на воспроизводимость и устойчивость.

Но в текущем виде это всё ещё **доменное приложение с engine-shaped core**, а не полноценный универсальный движок. Главные причины:

1. core-слой всё ещё протекает доменными типами и ожиданиями;
2. scheduler знает слишком много о runtime-типах задач;
3. контракт мира и ноды недостаточно строгий;
4. в нескольких местах есть реальные race / data integrity риски;
5. документация местами обещает то, чего код не делает.

---

## 1. Общий обзор проекта

### Что моделируется

По коду видно, что базовая модель — это **сеточная симуляция**: мир имеет ширину/высоту, разбит на узлы (`SimulationNode`), в узлах живут сущности (`Mortal`), а логика исполняется покадрово через `GameLoop`.

Для `nature` это островная экосистема, для `simcity` — городская симуляция. Это хороший знак: сама идея core-движка уже не привязана только к одному сценарию.

### Насколько доменная модель привязана к “острову”

Привязка всё ещё высокая:

- `Main.main()` напрямую вызывает `NatureLauncher.main(args)`.
- `SimulationContext` хранит `SimulationRenderer`, который на практике приходит из домена.
- `SimulationWorld.getConfiguration()` возвращает `Object`, а в `Cell` это тут же приводится к `Configuration`.
- В `Cell` и `Island` уже сидят доменные правила природы, биомассы, защиты, сезонности и статистики.

Это означает, что движок пока не полностью отделён от конкретной игры. Он может запускать и другое доменное приложение, но не выглядит как чистое reusable ядро.

### Можно ли переиспользовать текущие абстракции

Да, но частично. Наиболее полезные абстракции уже есть:

- `SimulationPlugin`
- `SimulationWorld`
- `SimulationNode`
- `WorldSnapshot` / `NodeSnapshot`
- `ScheduledTask` / `CellService`
- `RandomProvider`

Однако сейчас эти абстракции слишком мягкие. Они помогают вынести часть логики наружу, но не гарантируют независимость от домена.

### Есть ли “протекание” домена в технические детали

Да, и это ключевая проблема. Примеры:

- `SimulationContext` тащит renderer как часть engine-контракта, хотя визуализация — это отдельный адаптерный слой.
- `Cell` знает о `Island`, `Animal`, `Plant`, `Configuration`, `SpeciesKey`, `SizeClass`.
- `CityMap.getConfiguration()` возвращает `null`, то есть контракт формально есть, но по факту не используется.
- `GameLoop` содержит оптимизированный path только для задач, которые одновременно являются `CellService`.

Это уже не “ядро без домена”, а ядро с доменными допущениями.

---

## 2. Архитектура и дизайн

### Архитектурный стиль

Архитектура ближе всего к **plugin-based simulation framework** с:

- центральным scheduler-циклом (`GameLoop`);
- world abstraction (`SimulationWorld`);
- node abstraction (`SimulationNode`);
- domain services (`CellService`);
- domain plugins (`SimulationPlugin`).

Это не чистый ECS и не чистый DDD. Это гибрид orchestration layer + domain services + grid world model.

### Это engine или одноразовое приложение?

Пока ближе к **framework prototype**, чем к production-grade engine.

Плюс в том, что уже есть опорные слои для повторного использования. Минус в том, что engine всё ещё знает слишком много о том, как именно устроены текущие миры. Если завтра появится другой тип симуляции — например, без сетки, с графом дорог, с event-driven экономикой или с непрерывным временем — часть этого core придётся перепроектировать.

### SOLID

#### S — Single Responsibility

`Cell` перегружен ответственностями:

- хранение сущностей;
- синхронизация;
- ограничения на размещение;
- доступ к соседям;
- выборка по типам;
- sampling;
- обновление listener-ов мира;
- доменные проверки по terrain/type/species.

То же касается `Island`, который одновременно:

- хранит карту;
- считает chunk-ы;
- управляет перемещением;
- хранит статистику;
- обновляет сезон;
- уведомляет listener-ов;
- содержит правила движения и частично доменную физику.

Это не просто “большой класс”; это смешение модели, инфраструктуры и доменной политики.

#### O — Open/Closed

`GameLoop.runTick()` делает branching по `instanceof CellService`. То есть scheduler расширяется через изменение центрального кода, а не через регистрацию стратегии исполнения.

Это плохой знак для универсального движка: появление новой категории задач потребует корректировки ядра.

#### L — Liskov Substitution

Формально `SimulationWorld` и `SimulationNode` выглядят универсально, но по факту контракт подразумевает:

- сеточную топологию;
- координаты `x,y`;
- соседей;
- move semantics;
- work units как наборы ячеек.

То есть это “универсальность под квадратную решётку”, а не под произвольную симуляционную модель.

#### I — Interface Segregation

`SimulationWorld` слишком широкий:

- конфигурация;
- размеры;
- getNode;
- moveEntity;
- snapshot;
- initialize;
- listener-ы;
- parallel work units.

Разные потребители используют разные части, а часть методов вообще нужна только конкретным доменам.

Отдельная проблема — `getConfiguration(): Object`. Это уже слабый контракт, а не интерфейс. Именно поэтому `Cell` и другие классы вынуждены делать downcast.

#### D — Dependency Inversion

Слабое место: core и domain всё ещё связаны не только через абстракции, но и через знание о конкретных реализациях.

Примеры:

- `Main` → `NatureLauncher`
- `SimulationContext` → renderer конкретной визуализации
- `Cell` → `Island`
- `CityMap.getConfiguration()` возвращает `null`, но world interface формально требует configuration
- `AbstractService` знает о `NatureWorld`, `NatureEnvironment`, `Season`

Вместо “ядро → абстракции → домены” получается “ядро + местами доменные типы, замешанные в core”.

### Coupling / Cohesion

Связность внутри отдельных классов высокая, но часто это **вынужденная cohesion из-за склеивания обязанностей**, а не удачная архитектурная декомпозиция.

Особенно слабый coupling между слоями:

- `engine` и `nature` связаны теснее, чем хотелось бы;
- `engine` и `simcity` пока тоже не полностью автономны;
- `model`-слой в доменах содержит и структуру данных, и поведение, и часть concurrency policy.

---

## 3. Алгоритмы и логика симуляции

### Основной цикл

`GameLoop.run()`/`runTick()` — это fixed-step loop. Он:

1. увеличивает `tickCount`;
2. подмешивает pending tasks;
3. тикает world;
4. раскладывает recurring tasks по фазам;
5. сортирует задачи по приоритету;
6. для параллелизуемых `CellService` запускает пакетную обработку по work units.

Это нормальная заготовка, но сейчас она ещё не уровень “движка”, а уровень “central coordinator”.

### Что хорошо

- Есть разделение на фазы через `Phase`.
- Есть приоритеты.
- Есть параллельный путь только для задач, которые его поддерживают.
- Есть work units у мира.

Это хорошая база.

### Что плохо

#### 1) scheduler знает про конкретный runtime type

`runTick()` определяет parallel path через:

- `task.isParallelizable()`
- `task instanceof CellService`

То есть новый тип параллельной задачи без `CellService` движок не увидит.

#### 2) логика исполнения создаёт лишнюю работу каждый тик

На каждом тике происходит:

- построение `EnumMap`;
- раскладка задач по фазам;
- сортировка по приоритету;
- сбор `Callable`-ов;
- ожидание futures.

Для раннего прототипа это нормально, но для long-run симуляции это лишние аллокации и лишний overhead scheduler-а.

#### 3) ошибки в parallel tasks могут стать тихими

`runCellServicesParallel()` ловит `ExecutionException`, логирует и идёт дальше. Это опасно: один failed worker может оставить world в частично обновлённом состоянии, а следующий этап уже будет работать поверх повреждённого snapshot-а.

Для симуляции это хуже обычного падения: тихая порча состояния гораздо опаснее явного crash-а.

#### 4) нет строгой модели барьеров

Сейчас `beforeTick()` вызывается перед parallel processing, `afterTick()` — после. Но контракт не гарантирует, что:

- сервисы не разделяют mutable state;
- сервисы не зависят друг от друга;
- внешний код не меняет world во время обработки.

Это означает, что безопасность исполнения держится на дисциплине автора, а не на контракте движка.

#### 5) фиксированный timestep есть, но детерминизм не формализован

Сам по себе fixed-step loop не делает симуляцию детерминированной. Нужны ещё:

- стабильный порядок обхода;
- стабильный RNG;
- отсутствие зависимостей от wall-clock;
- отсутствие race conditions;
- одинаковая политика обработки ошибок.

Часть этого уже есть, часть — нет.

### Big-O и узкие места

На росте количества сущностей узкими местами будут:

- линейные проходы по спискам внутри `Cell`/`CityTile`;
- повторные копии коллекций;
- сортировка задач каждый тик;
- повторный пересчёт соседей/шардов, если это будет расширяться;
- `countBySpecies()` в `EntityContainer`, который проходит по всем `AnimalType` и ещё обращается к biomass.

То есть основная проблема не в одном “медленном алгоритме”, а в сумме постоянных линейных операций в hot path.

---

## 4. Коллекции и структуры данных

### `Cell`

Здесь уже видно попытку оптимизации через `EntityContainer` с индексами по типу, размеру и виду. Это правильное направление.

Но реализация ещё не дотягивает до заявлений из README:

- не везде O(1);
- не везде swap-to-remove;
- не везде immutable access;
- не везде доказана безопасность concurrent read/write.

### `EntityContainer`

Плюсы:

- `animalsByType`
- `animalsBySize`
- `predators`
- `herbivores`
- `biomassBySpecies`
- `allAnimals`
- `allBiomass`

Это уже похоже на data-oriented storage, а не на naive object graph.

Минусы:

- `countBySpecies()` всё равно делает проход по всем `AnimalType`;
- `removeDeadAnimals()` проходит по `allAnimals` целиком;
- `getByType()` / `getBySize()` возвращают wrapper view, но внутренняя структура всё равно mutable;
- `getAllAnimals()` и `getAllBiomass()` отдают view на живые коллекции, а не снимок.

### `Cell` vs `CityTile`

Тут видна архитектурная несогласованность.

`Cell`:

- старается скрывать внутренности;
- возвращает копии списков;
- использует `ReadWriteLock`;
- даёт iteration APIs.

`CityTile`:

- через Lombok `@Getter` отдаёт `entities` как сырой mutable list;
- не переопределяет `getEntities()`;
- не прячет внутренние списки за snapshot/view;
- синхронизируется только через `ReentrantLock`, но потребитель может обойти это через getter.

Это важный разрыв: два “node”-типа в разных доменах ведут себя по-разному, а engine-контракт из-за этого перестаёт быть надёжным.

### Лишние аллокации и копирования

Примеры:

- `Cell.getEntities()` создаёт новый `ArrayList` на каждый вызов;
- `Cell.forEachAnimal()` делает копию списка перед обходом;
- `Cell.forEachPredator()` / `forEachHerbivoreSampled()` тоже работают через копирование;
- `GameLoop` каждый тик создаёт новые коллекции для фаз и задач.

Для маленькой симуляции это не критично. Для большого мира — это станет заметным источником pressure на GC.

---

## 5. Качество кода

### Нейминг

В целом имена достаточно понятные, но есть проблемы на уровне слоя:

- `SimulationContext` — по имени это engine-object, но фактически он тащит внешние вещи;
- `getConfiguration()` — слишком размыто;
- `CellService` — названию не хватает семантики, потому что это и система, и task, и phase participant;
- `AbstractService` — слишком общий класс для конкретного доменного base class.

### Дублирование

Есть несколько повторяющихся паттернов:

- lock/unlock try/finally в `Cell`, `CityTile`, `Island`, `CityMap`;
- `if (from instanceof X f && to instanceof X t)` во всех перемещениях;
- сбор work units и проход по ним;
- подсчёт сущностей/метрик отдельными сервісами.

### Магические числа

Магические значения разбросаны по доменам:

- `priority = 50`;
- `tickDurationMs`;
- `BANKRUPTCY_THRESHOLD = 5`;
- `taxRate > 30`;
- `resDemand > 50`;
- `cellPopulation < 5`;
- `tickCount % 2 == 0`.

В прототипе это допустимо, но для engine-кода лучше выносить такие числа в policy/config objects.

### Слишком большие классы и методы

Кандидаты:

- `GameLoop`
- `Cell`
- `Island`
- `CityMap`
- `AbstractService`
- `PopulationService`
- `FeedingService` и `ReproductionService` тоже стоит пересмотреть отдельно

Особенно важно, что большие методы в этих классах не просто длинные — они часто совмещают orchestration, business rules и mutation.

---

## 6. Тестируемость

### Что уже хорошо

В проекте есть тесты на:

- воспроизводимость;
- устойчивость;
- loop concurrency;
- world initialization;
- domain mechanics.

Это уже лучше, чем у большинства ранних симуляционных проектов.

`RandomProvider` — тоже правильный шаг: он открывает путь к deterministic replay.

### Где код трудно тестировать

- `SimulationContext` связан с доменной visual layer.
- `Main` жёстко запускает `NatureLauncher`.
- `AbstractService` завязан на `NatureWorld` и `NatureEnvironment`.
- `Cell` и `Island` используют конкретные доменные типы и побочные эффекты через listener-ов.
- `GameLoop` создаёт executor внутри себя и не даёт нормального hook для подмены политики исполнения.

### Что мешает изолированному тестированию ядра

1. нет отдельного pure scheduler abstraction;
2. нет лёгкой подмены clock/executor;
3. world/node contracts слишком конкретны;
4. часть логики скрыта в domain services;
5. нет жёстко заданного boundary между engine state и domain state.

---

## 7. Масштабируемость и расширяемость

### Добавить новый тип сущности

В grid-based мире это возможно. Но если новая сущность имеет:

- другую топологию;
- иной lifecycle;
- non-local interaction;
- AI, зависящий не от клеток, а от графа/рынка/зон;

тогда текущий core начнёт сопротивляться.

### Изменить правила взаимодействия

Изменить можно, но часто через доработку существующих сервисов и моделей, а не через подключение независимого модуля. Это уже признак того, что core пока не является по-настоящему композиционным.

### Добавить экономику, погоду, ресурсы, строительство

Возможность есть, но текущий runtime плохо приспособлен для богатого набора систем:

- нет явного event bus;
- нет dependency graph между системами;
- нет state store / component store как отдельной сущности;
- нет формального rollback/replay слоя;
- нет чёткой политики синхронизации для shared global state.

### Использование в другой игре

Для другого островоподобного или клеточного симулятора код можно взять как старт.

Для:

- city-builder,
- traffic simulation,
- economic sim,
- colony sim,

придётся как минимум переписать core contracts мира, node-абстракции и scheduler policy. То есть reusable сейчас — только частично.

---

## 8. Потенциальные баги и риски

### 1) `CityMap.moveEntity()`: риск потери сущности при перемещении

```java
if (t.canAccept(entity) && f.removeEntity(entity)) {
    return t.addEntity(entity);
}
```

Проблема: если `removeEntity()` прошёл, а `addEntity()` неожиданно не прошёл, сущность потеряется.

Даже если сейчас `canAccept()` почти всегда true, сам паттерн unsafe: перемещение должно быть атомарным или компенсируемым.

### 2) `Island.moveEntity()` для biomass: риск дублирования

Там порядок обратный:

```java
if (t.addEntity(b)) {
    return f.removeEntity(b);
}
```

Если `removeEntity()` не сработает, biomass может оказаться и в source, и в target.

### 3) `CityTile` отдаёт сырой mutable list

Через Lombok-generated getter наружу уходит внутренний `entities`-лист. Это прямой обход lock discipline. Для thread-safe engine это серьёзная проблема.

### 4) `CityMap.cachedChunks` и lazy init

`cachedChunks` инициализируется лениво. Сам `initializeChunks()` synchronized, но поле не volatile, а `getParallelWorkUnits()` читает его без синхронизации. Это создаёт риск тонкой гонки видимости.

### 5) `SimulationWorld.getConfiguration()` как `Object`

Слабый контракт провоцирует downcast-ы и `null`-возвраты. В одном домене это configuration object, в другом — `null`. Это архитектурный риск и источник NPE/invalid cast.

### 6) `GameLoop.stop()`

`join(2000)` + `shutdownNow()` — грубый останов. Если worker-ы зависли или делают долгую работу, остановка может оставить систему в промежуточном состоянии.

### 7) Скрытые race conditions в service state

`CellService` контракт позволяет реализовать stateful service. Но `GameLoop` может выполнять такой сервис параллельно на множестве work units. Если сервис хранит mutable fields, это почти гарантированный источник гонок.

---

## 9. Приоритетные улучшения

- **[HIGH]** Развязать engine и domain: убрать доменные типы из core-контрактов, особенно из `SimulationContext`, `SimulationWorld.getConfiguration()`, `Main`, `AbstractService`.
- **[HIGH]** Переделать scheduler: уйти от `instanceof CellService` к явной модели систем/фаз/барьеров.
- **[HIGH]** Исправить атомарность перемещений в `CityMap.moveEntity()` и `Island.moveEntity()`.
- **[HIGH]** Убрать утечки mutable collections: `CityTile` не должен отдавать `entities` напрямую.
- **[MEDIUM]** Стабилизировать snapshot/state contract: world/node должны возвращать либо immutable views, либо специально оформленные read models.
- **[MEDIUM]** Свести документацию и реализацию: сейчас README и код по нескольким пунктам расходятся.
- **[MEDIUM]** Упростить и формализовать concurrency policy: кто владеет lock-ами, кто может читать, кто может писать, где барьер.
- **[LOW]** Уменьшить аллокации в hot path и вынести магические числа в config/policy-объекты.

---

## 10. Рефакторинг (с примерами)

### Пример 1. Отделить engine-ядро от доменного слоя

**До**

```java
public class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T> world;
    private final GameLoop<T> gameLoop;
    private final SimulationRenderer view;
    private final RandomProvider random;
}
```

Проблема: engine-контекст хранит renderer как часть core-модели. Это вынуждает ядро знать о конкретной визуализации.

**После**

```java
public interface SimulationObserver {
    void onTick(WorldSnapshot snapshot);
}

public final class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T> world;
    private final GameLoop<T> gameLoop;
    private final RandomProvider random;
    private final List<SimulationObserver> observers;
}
```

Тогда renderer, console view, metrics collector и UI dashboard становятся внешними адаптерами.

### Пример 2. Сделать scheduler явным, а не type-driven

**До**

```java
for (ScheduledTask task : recurringTasks) {
    if (task.isParallelizable() && task instanceof CellService) {
        // special path
    } else {
        task.tick(tickCount);
    }
}
```

**После**

```java
public enum Phase { PREPARE, SIMULATION, POSTPROCESS }

public interface ScheduledTask extends Tickable {
    Phase phase();
    int priority();
    ExecutionMode executionMode(); // SEQUENTIAL / PARALLEL
}
```

```java
scheduler.runPhase(Phase.PREPARE, tick);
scheduler.runPhase(Phase.SIMULATION, tick);
scheduler.runPhase(Phase.POSTPROCESS, tick);
```

Такой подход лучше масштабируется под экономику, AI, путь, строительство, погоду и deferred events.

### Пример 3. Убрать unsafe move semantics

**До**

```java
if (t.canAccept(entity) && f.removeEntity(entity)) {
    return t.addEntity(entity);
}
```

**После**

```java
public boolean moveEntity(E entity, Node from, Node to) {
    lockBoth(from, to);
    try {
        if (!to.canAccept(entity)) return false;
        if (!from.contains(entity)) return false;
        if (!to.addEntity(entity)) return false;
        return from.removeEntity(entity);
    } finally {
        unlockBoth(from, to);
    }
}
```

Или ещё лучше — сделать отдельную transactional mutation API, чтобы перемещение было атомарным и инварианты не разрушались промежуточным состоянием.

---

## 11. С чем сравнивать и нужно ли “изобретать колесо”

Если цель — **универсальный симуляционный движок**, то ближе всего вам не классический game engine, а **agent-based / multi-agent simulation toolkit**.

Практические аналоги по духу:

- **MASON** — Java toolkit для многоагентных симуляций; хорош как ориентир для model/world separation и simulation core.
- **Repast Simphony** — ещё один зрелый Java-based toolkit для агентного моделирования и симуляции.
- **Ashley** — лёгкий Java ECS для игровых систем, если хочется data-oriented runtime и понятный system scheduler.
- **Bevy ECS** — хороший ориентир по data-driven world/systems/scheduling, если интересует современная модель execution and parallelism.

### Что у них полезно взять

- явные system boundaries;
- разделение model vs visualization;
- стабильный scheduler;
- чёткий contract для state access;
- reproducible execution;
- минимизация runtime type checks.

### Практический вывод

Да, колесо здесь частично уже изобретено заново, особенно в scheduler/world/node слоях. Это не значит, что код плохой — но значит, что перед дальнейшим ростом надо решить: вы строите **свой niche simulation framework** или адаптируете уже существующий open-source base.

Если цель — быстрее выйти в production-quality, я бы рассматривал заимствование архитектурных идей из MASON/Repast для simulation core и из ECS-фреймворков для execution model, а не полную самодельную реализацию всего runtime.

---

## 12. Итоговая оценка

- **Архитектура:** 4/10
- **Код:** 5/10
- **Переиспользуемость:** 3/10
- **Общая оценка:** 4/10

### Короткий вердикт

Проект **ещё не готов** быть основой для универсального симуляционного движка без существенной переработки core-слоя.

Положительная сторона: уже есть правильные строительные блоки — world/node/snapshot contracts, plugin-based запуск, tick loop, work units, попытка parallel execution, тесты на детерминизм и устойчивость.

Главная проблема: ядро ещё слишком тесно связано с конкретными доменными моделями и слишком сильно полагается на договорённости, а не на строгие контракты. Пока это не исправлено, добавление новых механик будет расширять технический долг быстрее, чем ядро будет становиться универсальнее.
