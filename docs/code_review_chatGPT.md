# CODE_REVIEW.md

## 1. Общий обзор проекта

Проект моделирует островную экосистему: животные, биомассу/растения, клетки карты, сезонность, питание, движение, размножение, смертность и статистику. По текущему состоянию код уже вышел за рамки «учебного OOP-примера» и пытается эмулировать движок симуляции: есть `GameLoop`, `SimulationWorld`, `Tickable`, `CellService`, снапшоты мира, сервисы по фазам и отдельная конфигурация домена.

Проблема в том, что доменная модель остаётся жёстко привязанной именно к «острову» и именно к зоосистеме:

- `Island` совмещает роль world runtime, хранилища карты, агрегатора статистики, сезонного календаря и координатора защиты видов.
- `SpeciesConfig`, `SpeciesRegistry`, `AnimalFactory`, `DefaultProtectionService`, `StatisticsService` и `ConsoleView` содержат доменные допущения, которые трудно перенести в другую симуляцию без переписывания.
- `GameLoop` и сервисы предполагают именно клеточный мир с соседями, охотой, возрастом, голодом и биомассой.

Абстракции частично переиспользуемы, но пока они сформированы вокруг островного сценария, а не вокруг универсального simulation engine.

### Вывод по домену

- Домен моделируется как экосистема острова, а не как нейтральное ядро симуляции.
- Переиспользование текущих абстракций в другой игре возможно только на уровне идеи, но не на уровне API и границ ответственности.
- Есть сильное «протекание» домена в технические детали: в движок уже встроены сезонность, endangered protection, пакетная охота, LOD-сэмплинг, console rendering.

## 2. Архитектура и дизайн

### Архитектурный стиль

Фактически это гибрид:

- композиционный `bootstrap`-style wiring в `SimulationBootstrap`;
- service-oriented orchestration через `TaskRegistry`;
- stateful world object (`Island`) с большим числом обязанностей;
- phase-based simulation pipeline (`FeedingService`, `MovementService`, `ReproductionService`, `LifecycleService`, `CleanupService`).

Это не чистая layered architecture и не чистый hexagonal/clean architecture. Скорее это procedural engine с попыткой выделить интерфейсы под расширение.

### Насколько это «движок», а не одноразовое приложение

Сейчас ближе к одноразовому приложению с потенциальным движковым каркасом, чем к настоящему reusable engine.

Ключевые признаки:

- ядро знает слишком много про конкретный предметный мир;
- механики зарегистрированы вручную в `TaskRegistry.registerAll()`;
- добавление новой механики требует изменения не только нового класса, но и центральной композиции;
- большая часть абстракций не защищает от coupling, а только оформляет его.

### SOLID

#### S — Single Responsibility Principle

Нарушение в `Island`.

`Island` одновременно:

- хранит grid и chunk partitioning,
- реализует `SimulationWorld`,
- обновляет season,
- держит `ProtectionService`,
- прокидывает статистику,
- создаёт snapshots,
- реализует movement logic,
- управляет bounds-checking.

Это слишком много причин для изменения.

Нарушение в `GameLoop`.

`GameLoop` одновременно:

- владеет scheduling;
- группирует задачи;
- определяет execution mode для `CellService`;
- запускает parallel workload;
- собирает `SimulationMetrics`;
- делает error handling;
- управляет lifecycle executor-а.

Это уже не loop, а мини-runtime.

Нарушение в `StatisticsService`.

Он одновременно хранит population counters, biomass counters, death counters, metrics snapshot и расчётные функции для сатурации/голода. Это не один сервис, а минимум три: event aggregator, metrics store, analytics facade.

#### O — Open/Closed Principle

Слабое место: `TaskRegistry.registerAll()`.

Чтобы добавить новую фазу или механику, нужно менять registry и фактически пересобирать pipeline. Абстракция `CellService` выглядит расширяемой, но порядок и координация остаются жёстко запрограммированными.

Ещё один пример — `AnimalFactory`: species registry захардкожен. Новая сущность требует правки фабрики, а не регистрации через конфигурацию или plugin-style registry.

#### L — Liskov Substitution Principle

Проблема в том, что некоторые интерфейсы формально подменяемы, но семантически нет.

Например, `SimulationWorld` заявляет универсальность, но большинство реализаций и сервисов ожидают именно `Cell` в качестве `SimulationNode`, конкретную структуру соседей, конкретные свойства `SpeciesRegistry` и конкретные `DeathCause`. Замена world implementation на иной тип симуляции будет ломать поведение на уровне ожиданий, а не компиляции.

#### I — Interface Segregation Principle

`SimulationNode` перегружен:

- lock,
- coordinates,
- neighbors,
- living entities,
- animal iteration,
- biomass iteration,
- predator/herbivore sampling,
- counts,
- capacity checks,
- add/remove entity.

Это слишком широкий контракт. Для части сервисов нужен только read-only доступ к агрегации, для части — только movement, для view — только snapshot data. Сейчас клиенты завязаны на жирный интерфейс.

`SimulationWorld` аналогично содержит и spatial API, и statistics, и protection, и snapshot, и season. Это не один интерфейс, а набор разных портов.

#### D — Dependency Inversion Principle

Есть попытка: `RandomProvider`, `SimulationView`, `ProtectionService`, `InteractionProvider`, `HuntingStrategy`.

Но инверсия неполная:

- `Island` создаёт `DefaultProtectionService` напрямую.
- `SimulationBootstrap` жёстко конструирует `ConsoleView`.
- `TaskRegistry` жёстко создаёт `DefaultHuntingStrategy`.
- Сервисы часто делают `instanceof Cell` и лезут в concrete model layer.

То есть зависимости формально инвертированы, но инфраструктурно всё ещё concrete-first.

### Анти-паттерны

- **God Object**: `Island`, частично `GameLoop`, частично `StatisticsService`.
- **Service Locator in disguise**: `SimulationContext` становится контейнером, из которого потом тянут всё подряд.
- **Feature Envy / domain leakage**: сервисы из engine знают слишком много о конкретных доменных типах.
- **Primitive obsession**: basis points, percentages, `SCALE_10K`, `SCALE_1M` захардкожены в константах без единой value-object модели.
- **Temporal coupling**: сначала надо вызвать `init()`, потом `registerAll()`, потом `start()`. Порядок важен, но не зафиксирован типами.

### Coupling / Cohesion

#### Низкая cohesion

- `Island` — низкая cohesion из-за смешения world state, lifecycle, statistics, season, movement, snapshot.
- `GameLoop` — низкая cohesion из-за смешения scheduling и execution policy.

#### Высокая coupling

- `FeedingService` зависит от `AnimalFactory`, `InteractionProvider`, `SpeciesRegistry`, `HuntingStrategy`, `SimulationWorld`, `Cell`, `RandomProvider` и внутренней логики обхода узлов.
- `CleanupService` зависит от `Cell.getContainer()` и callback-а на удаление.
- `ConsoleView` зависит от структуры snapshot и от набора species codes.

### Где архитектура мешает переиспользованию

- Видимая архитектура уже «знает» про хищников/травоядных/растения/биомассу.
- `TaskRegistry` фиксирует функциональные фазы как обязательный пайплайн для этого домена.
- `StatisticsService` хранит не абстрактные метрики, а именно метрики этой экосистемы.
- `ConsoleView` рендерит именно эту карту и именно эти виды.

## 3. Алгоритмы и логика симуляции

### Основной цикл

Симуляция выглядит как phase-based tick loop:

1. `GameLoop.runTick()` проходит по списку задач.
2. Задачи типа `CellService` группируются и запускаются через parallel path.
3. Для каждого work unit выполняются `processCell()` всех сервисов.
4. После прохода обновляются `SimulationMetrics`.
5. Отдельная задача рендерит snapshot через `SimulationView`.

### Универсальность цикла

Идея фазового цикла универсальна, но конкретная реализация — нет.

Почему:

- порядок задач не описан как декларативный pipeline, а выводится из порядка регистрации;
- оптимизация заточена под `CellService` и `SimulationNode`;
- внутри `runCellServicesParallel()` сервисы не независимы, а исполняются как coupled batch.

### Критический architectural hotspot

`GameLoop.runCellServicesParallel()`.

Сейчас для каждого work unit выполняются **все** cell services подряд в рамках одного task. Это означает, что внутри одного chunk-а state progression идёт сервис за сервисом, а не как общий world-wide barrier между фазами. Для симуляции это опасно: результат начинает зависеть от chunk partitioning и текущего порядка сервисов.

Это особенно нежелательно для движка, который должен быть переносимым между играми. У разных симуляций разные требования к синхронизации фаз.

### Лишние проходы и вычисления

- `Cell.addAnimal()` каждый раз пересчитывает количество животных конкретного вида линейным проходом по списку.
- `Cell.countAnimalsBySpecies()` и `getAnimalsBySpecies()` повторяют линейный фильтр по той же коллекции.
- `CellSnapshot` строит biomass map заново для каждого снапшота, проходя по всем животным и biomass containers.
- `StatisticsService.getSpeciesCountsMap()` каждый вызов собирает новый `HashMap`.
- `ConsoleView.display()` делает полный traversal по всему snapshot каждый кадр.

### Big-O и bottlenecks

Основные узкие места при росте количества сущностей:

- `Cell.addAnimal()` — O(n) на добавление, что при плотной клетке превращается в квадратичную деградацию при массовом спауне.
- `Cell.cleanupDeadOrganisms()` — O(n), но вызывается регулярно; на больших мирах это станет дорогим.
- `StatisticsService.getTotalPopulation()` и `getSpeciesCountsMap()` — каждое вычисление идёт по всем counters.
- `ConsoleView.display()` — O(width * height + species) на tick; для больших карт console rendering быстро станет доминирующей стоимостью.

### Риск при росте мира

Сейчас параллелизм завязан на chunk partitioning, но не видно глобального планировщика, который гарантировал бы отсутствие write contention между соседними клетками/чанками. При росте плотности сущностей contention на lock-ах клеток будет основной проблемой.

## 4. Коллекции и структуры данных

### Где структуры не оптимальны

#### `Cell`

- `animals` и `plants` — обычные списки.
- При этом часто нужно: count by species, remove dead, query by species, iterate predators/herbivores.

Это означает, что текущая структура не соответствует паттерну доступа.

#### `EntityContainer`

Там уже есть более полезная декомпозиция:

- `animalsByType`,
- `animalsBySize`,
- `predators`,
- `herbivores`,
- `allAnimals`,
- `allBiomass`.

Но `Cell` либо не использует эту модель полноценно, либо держит parallel state. Это признак дублирования хранения и риска рассинхронизации.

### Лишние аллокации и копирования

- `Island.getParallelWorkUnits()` создаёт новый список через stream/toList().
- `StatisticsService.getSpeciesCountsMap()`, `getTickDeaths()`, `getTotalDeaths()` создают новые map-ы на каждом вызове.
- `CellSnapshot` и `IslandSnapshot` создаются на каждый кадр и повторно собирают агрегаты.
- `ConsoleView` создаёт много временных строк и коллекций на render tick.

### Универсальность структур

Сейчас структуры данных не универсальны: они отражают именно сценарий острова, а не абстрактные пространственные сущности.

Особенно это видно в `EntityContainer`, где есть `predators`/`herbivores` как first-class категории. Для универсального движка это слишком предметно; лучше хранить capability flags или policies, а не биологические ярлыки.

## 5. Качество кода

### Нейминг

Проблемные места:

- `EntityContainer` — слишком общее имя для сущности, но реализация очень предметная.
- `SimulationWorld`, `SimulationNode` — хорошие названия, но контракт слишком широкий.
- `SpeciesConfig` и `SpeciesRegistry` — одновременно config, registry и data source.
- `GameLoop` — фактически runtime/scheduler, не просто loop.
- `ConsoleView` — визуализация, но по сути это renderer + dashboard.

### Дублирование

- Повторяющиеся проверки `instanceof Cell` во многих сервисах.
- Повторяющиеся циклы по всем животным для подсчёта и фильтрации.
- Похожие механики `getAnimalsBySpecies`, `countAnimalsBySpecies`, `countBySpecies` в разных местах.
- Логика получения protection map присутствует и в `Island`, и в `AbstractService`.

### Магические значения

Очень много бизнес-магии вынесено в `SimulationConstants`, но это не решает проблему полностью, потому что сами значения всё ещё «магические» по смыслу:

- `SCALE_10K`, `SCALE_1M`;
- `WOLF_PACK_MIN_SIZE`;
- `FEEDING_LOD_LIMIT`, `MOVEMENT_LOD_LIMIT`, `REPRODUCTION_LOD_LIMIT`;
- `ENDANGERED_*`;
- `HIBERNATION_METABOLISM_MODIFIER_BP`.

Константы помогают, но без value objects и документированных policy-объектов это остаётся набором чисел, а не моделью поведения.

### Слишком большие классы/методы

- `Island` — однозначно слишком большой класс.
- `GameLoop` — слишком много обязанностей и слишком много скрытых соглашений.
- `FeedingService.processPredators()` и особенно `tryEat()` — метод перегружен логикой выбора prey, проверки protection, вероятностных бросков, применения penalties, отчётов о смерти и освобождения животных.
- `StatisticsService` — агрегирует слишком много статистики в одном объекте.

## 6. Тестируемость

### Где код трудно тестировать

- `Island` создаёт `DefaultProtectionService` внутри себя и потому плохо мокается.
- `SimulationBootstrap` жёстко собирает runtime graph, а не принимает фабрики/интерфейсы.
- `GameLoop` создаёт собственный thread и executor; трудно изолировать timing.
- `ConsoleView` напрямую пишет в stdout.
- `DefaultRandomProvider` и `ThreadLocalRandom` усложняют детерминированные tests, хотя `RandomProvider` уже помогает.

### Можно ли тестировать ядро изолированно от домена

Пока только частично.

Есть хорошие признаки:

- `RandomProvider`;
- `SimulationView`;
- `InteractionProvider`;
- `HuntingStrategy`;
- `WorldSnapshot`/`NodeSnapshot`.

Но domain core по-прежнему не отделён от island-specific объектов. Например, `FeedingService` и `LifecycleService` работают не на абстрактных capability interfaces, а на `Animal`, `Biomass`, `Cell`, `SpeciesRegistry`.

### Где не хватает абстракций

- Вместо прямой работы с `Cell` сервисам нужен `MutableNode` / `SpatialCell` / `EntityStore` интерфейс.
- Вместо прямого `Island` world лучше иметь `SimulationRuntime` / `WorldState` с портами для read/write.
- Вместо `TaskRegistry` на ручных `addRecurringTask()` лучше иметь declarative pipeline descriptor.

## 7. Масштабируемость и расширяемость

### Добавить новый тип сущности

Сейчас это дорого:

- придётся менять `SpeciesRegistry`,
- возможно `AnimalFactory`,
- snapshot/renderer,
- статистику,
- вероятностные матрицы,
- возможно `EntityContainer` и `Cell`.

То есть сущность не является plug-in элементом.

### Изменить правила взаимодействия

Частично можно через `HuntingStrategy` и `InteractionProvider`, но реальная логика по-прежнему размазана по сервисам. Например, в `FeedingService` присутствуют и selection, и strike resolution, и penalties, и overpopulation bonus.

### Добавить новую механику (ресурсы, экономика, погода и т.д.)

Это возможно только ценой расширения центрального pipeline и world/model contracts.

Если добавить экономику или weather, придётся:

- либо вставлять новые `CellService` в `TaskRegistry`,
- либо перегружать `Island` дополнительным состоянием,
- либо менять snapshot/view слой.

Архитектура пока не даёт действительно независимых механик.

### Использование в другой игре с другим доменом

Это потребует переписывания ядра в тех местах, где сейчас зашит островной domain vocabulary:

- `AnimalType` и `SpeciesRegistry`;
- `ProtectionService`;
- `DefaultHuntingStrategy`;
- `ConsoleView`;
- `LifecycleService`, `FeedingService`, `MovementService`, `ReproductionService`;
- `Island`.

То есть не просто адаптации, а extraction core + rewrite of domain layer.

## 8. Потенциальные баги и риски

### Подозрительные места

#### `Cell.getAnimals()` / `Cell.getPlants()`

Возвращают внутренние mutable lists без копии и без lock-guard. Это прямой риск race condition и `ConcurrentModificationException`.

#### `Cell.getPlantCount()`

Считает массу растений без lock. Если параллельно идут модификации, результат неконсистентен.

#### `StatisticsService.registerRemoval()`

Использует `AtomicInteger.decrementAndGet()` без нижней границы. При рассинхронизации событий можно уйти в отрицательные значения, а потом `getSpeciesCount()` их обрежет, скрывая проблему.

#### `StatisticsService.getSpeciesCount()`

Смешивает count животных и mass биомассы в одно число. Это очень рискованно: consumer-ы могут интерпретировать это как population count, хотя там уже «count + масса».

#### `DefaultProtectionService.update()`

Зависит от `statisticsService.getSpeciesCount(key)`. Если там смешаны animal count и biomass mass, endangered logic может стать некорректным.

#### `Island.getProtectionMap(SpeciesRegistry passedRegistry)`

Параметр `passedRegistry` не используется. Это признак либо сломанного контракта, либо следа от рефакторинга. Легко пропустить ошибку при дальнейшем развитии.

#### `GameLoop` lifecycle

`start()` создаёт новый `Thread`, но не видно полноценного join/termination management. `stop()` делает shutdown executor, но background thread может жить дольше ожидаемого.

### Нарушения инвариантов

- В `Cell` ограничения по maxPerCell проверяются через линейный count при добавлении, но `getAnimalCount()` просто возвращает `animals.size()` без учета species constraints.
- В `EntityContainer` и `Cell` есть риск рассинхронизации разных индексов, если одна операция не обновит все коллекции.
- В `SimulationWorld`/`Island` world state и stats state потенциально могут расходиться, если событие произошло в одной ветке, а статистика обновилась в другой.

## 9. Приоритетные улучшения

- [HIGH] Вынести из `Island` отдельный `WorldState`/`SpatialGrid` и оставить в `Island` только orchestration + delegation.
- [HIGH] Разделить `GameLoop` на scheduler + phase executor + metrics collector. Убрать неявную зависимость от порядка регистрации задач.
- [HIGH] Убрать direct `Cell` dependency из сервисов: ввести интерфейсы уровня `MutableNode`, `ReadonlyNode`, `EntityStore`.
- [HIGH] Перестать смешивать population count и biomass mass в `StatisticsService`.
- [MEDIUM] Перепроектировать `TaskRegistry` в declarative pipeline configuration.
- [MEDIUM] Перевести `AnimalFactory` на registry, которая не требует ручного изменения при каждом новом species.
- [MEDIUM] Перестать отдавать наружу mutable collections из `Cell`.
- [LOW] Упростить `ConsoleView` и отделить dashboard rendering от snapshot traversal.
- [LOW] Ввести явные value objects для probability/weight/energy policy, чтобы сократить primitive obsession.

## 10. Рефакторинг с примерами

### Пример 1. Выделение engine-ядра из доменной логики

#### До

```java
public class Island implements SimulationWorld {
    private final StatisticsService statisticsService;
    private final ProtectionService protectionService;
    private Season currentSeason = Season.SPRING;

    @Override
    public void tick(int tickCount) {
        this.tickCount = tickCount;
        updateSeason();
        statisticsService.onTickStarted();
        protectionService.update(tickCount);
    }

    @Override
    public boolean moveAnimal(Animal animal, SimulationNode from, SimulationNode to) {
        if (from instanceof Cell f && to instanceof Cell t) {
            return moveOrganism(animal, f, t);
        }
        return false;
    }
}
```

#### После

```java
public interface WorldState {
    int width();
    int height();
    Optional<MutableNode> nodeAt(int x, int y);
    Snapshot snapshot();
}

public interface SimulationRuntime {
    void tick(int tickCount);
    void registerPhase(SimulationPhase phase);
}

public final class IslandWorld implements WorldState {
    private final SpatialGrid grid;
    private final PopulationIndex populationIndex;
    private final SeasonModel seasonModel;

    // только доступ к данным и правилам локальной пространственной модели
}

public final class DefaultSimulationRuntime implements SimulationRuntime {
    private final PhasePipeline pipeline;
    private final MetricsCollector metrics;
    private final ExecutorService executor;
}
```

Что меняется:

- `Island` перестаёт быть god object.
- Season/statistics/protection уезжают в отдельные policy/engine services.
- Мир становится переносимым: другая игра может заменить `IslandWorld` своим `WorldState`.

### Пример 2. Введение абстракций для cell-level механик

#### До

```java
public class FeedingService extends AbstractService {
    @Override
    public void processCell(SimulationNode node, int tickCount) {
        processPredators(node, tickCount);
        processHerbivores(node, tickCount);
    }

    private void tryEat(Animal consumer, SimulationNode node) {
        PreyProvider preyProvider = new PreyProvider(node, interactionMatrix, 0, protectionMap, getRandom());
        // selection + strike + penalties + death reporting
    }
}
```

#### После

```java
public interface HungerResolver {
    HuntResult resolve(HuntContext context);
}

public interface FoodSourceSelector {
    Optional<Target> select(FoodSearchContext context);
}

public interface MutableNodeView {
    Stream<AnimalView> predators();
    Stream<AnimalView> herbivores();
    Stream<BiomassView> biomass();
    boolean remove(EntityId id);
}

public final class FeedingPhase implements SimulationPhase {
    private final FoodSourceSelector selector;
    private final HungerResolver resolver;

    @Override
    public void apply(WorldState world) {
        // orchestration only
    }
}
```

Что меняется:

- выбор prey отделён от исполнения ханта;
- penalization вынесен в resolver;
- node API становится минимальным и читаемым;
- новый домен может подставить свой `FoodSourceSelector` и `HungerResolver`.

## 11. Итоговая оценка

- Архитектура: 5/10
- Код: 5/10
- Переиспользуемость: 3/10
- Общая оценка: 4/10

### Короткий вердикт

Проект **ещё не готов** быть основой для универсального симуляционного движка.

Почему:

- ядро слишком тесно связано с островным зоопейзажем;
- `Island` и `GameLoop` перегружены ответственностями;
- фазовая архитектура есть, но она не отделена от конкретных механик;
- статистика, защита видов, сезонность и визуализация смешаны с runtime-ядром;
- при добавлении нового домена придётся переписывать не только контент, но и часть engine-core.

Хорошая новость: база уже достаточно близка к движку, чтобы из неё сделать reusable engine. Но для этого нужен не косметический рефакторинг, а жёсткое разделение engine/kernel, world model и island-specific rules.