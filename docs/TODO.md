# TODO: Архитектурные направления развития

> Документ описывает приоритетные задачи и стратегические направления развития
> симуляционного движка. Организован по горизонтам планирования.
> Текущая версия движка: **1.16.0**

---

## 🔴 HOTFIX — Блокирующие баги (до следующего релиза)

### HF-1: Устранить двойную отчётность смертей [COMPLETED]
**Файлы:** `FeedingService.java`, `MovementService.java`, `ReproductionService.java`

Три сервиса публиковали `EntityDiedEvent` напрямую, тогда как `Island.onEntityRemoved()`
публиковал второй раз при удалении из ячейки. Все счётчики смертей были завышены в 2 раза.

- [x] `FeedingService`: заменено на `a.die(EATEN_BY_PACK)` — прямой `publish()` удалён
- [x] `MovementService`: заменено на `animal.die(MOVEMENT_EXHAUSTION)` — прямой `publish()` удалён
- [x] `ReproductionService`: заменено на `parent.die(REPRODUCTION_EXHAUSTION)` — прямой `publish()` удалён
- [x] Добавлен тест `StatisticsDeathCountingTest`: убедились, что одна смерть = ровно одно событие в `StatisticsService`


---

## 🟡 SPRINT 1 — Техдолг и стабилизация ядра

### S1-1: Устранить `SpeciesKey` как глобальный синглтон
**Файлы:** `SpeciesKey.java`, `SpeciesLoader.java`, `SpeciesRegistry.java`

`private static final Map<String, SpeciesKey> REGISTRY` делает невозможным параллельный
запуск нескольких изолированных симуляций в одной JVM (например, параллельные тесты).

- [ ] Убрать `static final` поля `WOLF`, `BEAR` и т.д. из `SpeciesKey` — оставить только data-класс с `code` и `isPredator`
- [ ] Перенести реестр видов в `SpeciesRegistry` как instance-состояние
- [ ] `SpeciesLoader.load()` возвращает `SpeciesRegistry` с полным набором `SpeciesKey`-экземпляров
- [ ] Обновить все ссылки вида `SpeciesKey.WOLF` → получение через инжектированный `SpeciesRegistry`
- [ ] Убедиться, что два параллельных теста с разными `SpeciesRegistry` не влияют друг на друга

### S1-2: Перевести `PhaseScheduler.parallelGroup` в локальную переменную
**Файл:** `PhaseScheduler.java`

Instance-field `parallelGroup` — рабочий буфер, не состояние объекта.
Скрытая зависимость от порядка вызовов; потенциальный источник багов при рефакторинге.

- [ ] Объявить `parallelGroup` как `List<CellService>` внутри метода `execute()`
- [ ] Убедиться, что тесты проходят без изменений

### S1-3: Добавить `shouldStop()` как полноценный механизм завершения
**Файл:** `SimulationPlugin.java`, `SimulationEngine.java`

Хук `shouldStop()` объявлен, но не вызывается в `GameLoop.run()`.
Симуляция не может завершиться по доменному условию (полное вымирание, достижение цели).

- [ ] В `GameLoop.run()` добавить вызов `plugin.shouldStop(context)` после каждого `runTick()`
- [ ] Реализовать `NaturePlugin.shouldStop()`: останавливать при полном вымирании всех животных
- [ ] Добавить тест `SimulationStopConditionTest`: мир с одним животным → оно умирает → симуляция останавливается

### S1-4: Документация EventBus — type hierarchy как явный контракт
**Файл:** `EventBus.java`

Подписка на суперкласс (`Object.class`) работает как wildcard и получает все события.
Это не очевидно и не задокументировано.

- [x] Добавить Javadoc к `EventBus.subscribe()` с описанием type hierarchy
- [x] Добавить тест `EventBusTest.subscribingToObjectClassReceivesAllEvents()`
- [x] Рассмотреть добавление `publishAsync()` для неблокирующей публикации

---

## 🟢 SPRINT 2 — ECS: от задатков к полноценному слою

### S2-1: Ввести `ComponentStore<C>` — типизированное хранилище компонентов
**Новый файл:** `engine/ecs/ComponentStore.java`

`Organism` хранит компоненты в `ConcurrentHashMap<Class, Component>`. При тысячах
организмов это дорого: boxing, cache miss, lock-free overhead. `ComponentStore` изолирует
хранилище и позволяет менять реализацию без изменения `Organism`.

```java
public interface ComponentStore {
    <C extends Component> void add(Class<C> type, C component);
    <C extends Component> C get(Class<C> type);
    <C extends Component> boolean has(Class<C> type);
    <C extends Component> void remove(Class<C> type);
}
```

- [ ] Создать `DefaultComponentStore` (текущая ConcurrentHashMap-реализация)
- [ ] Создать `ArrayComponentStore` для фиксированного набора компонентов (без boxing, O(1) по индексу)
- [ ] `Organism` делегирует к `ComponentStore` вместо собственного `Map`
- [ ] Сравнить throughput двух реализаций через `SimulationOptimizationTest`

### S2-2: Ввести `System`-слой в ECS-архитектуру
**Новая директория:** `engine/ecs/`

Сервисы (`FeedingService`, `LifecycleService`) работают с `Animal`/`Organism` напрямую.
Настоящий ECS предполагает `System`, которая итерирует по компонентам, а не по сущностям.
Это даёт cache-friendly доступ и возможность переиспользования систем между доменами.

```java
// engine/ecs/EntitySystem.java
public interface EntitySystem<T extends Mortal> extends ScheduledTask {
    /** Компоненты, которые должны присутствовать у сущности для обработки */
    List<Class<? extends Component>> requiredComponents();
    void process(T entity, int tickCount);
}
```

- [ ] Создать `EntitySystem<T>` интерфейс в `engine/ecs/`
- [ ] Создать `EntityQuery<T>` — выборка сущностей по набору компонентов из `SimulationWorld`
- [ ] Мигрировать `LifecycleService` как `HealthSystem` (требует `HealthComponent`, `AgeComponent`)
- [ ] Мигрировать `MovementService` как `MovementSystem` (требует `MovementComponent`)
- [ ] Убедиться, что `EntitySystem` реализует `CellService` — остаётся в pipeline `GameLoop`

### S2-3: Добавить `ComponentFactory` для создания стандартных наборов
**Новый файл:** `engine/ecs/ComponentFactory.java`

Сейчас `Animal`, `Organism`, `GenericBiomass` создают компоненты в конструкторе.
При расширении набора компонентов каждый конструктор нужно менять вручную.

- [ ] `ComponentFactory.createAnimalComponents(AnimalType)` — возвращает список компонентов
- [ ] `ComponentFactory.createBiomassComponents(BiomassConfig)` — аналогично
- [ ] Поддержка `ComponentBundle` — именованный preset: `PREDATOR_BUNDLE`, `HERBIVORE_BUNDLE`

---

## 🔵 SPRINT 3 — Масштабирование и производительность

### S3-1: Пространственный индекс для поиска соседей
**Новый файл:** `engine/spatial/SpatialIndex.java`

При N > 10 000 сущностей `GridUtils.getNeighbors()` итерирует по 8 соседям-ячейкам,
а `findActualPrey()` в `FeedingService` перебирает содержимое ячейки. Для разреженных
миров с большими клетками нужен пространственный индекс.

```java
public interface SpatialIndex<T extends Mortal> {
    void insert(T entity, int x, int y);
    void remove(T entity);
    List<T> queryRadius(int x, int y, int radius);
    List<T> queryRect(int x1, int y1, int x2, int y2);
}
```

- [ ] Реализовать `GridSpatialIndex` — хэш-таблица по (x/bucketSize, y/bucketSize)
- [ ] Реализовать `QuadTreeSpatialIndex` для разреженных миров с крупными кластерами
- [ ] Интегрировать в `SimulationWorld` как опциональный компонент
- [ ] `MovementService` использует индекс для поиска цели без перебора всех ячеек

### S3-2: Адаптивный LOD на основе плотности популяции
**Файлы:** `Configuration.java`, `AbstractService.java`, `SamplingContext.java`

Текущий LOD — глобальные константы (`feedingLodLimit=500`). При плотной популяции
в малом кластере limit=500 не защищает от O(N²). При разреженном мире — избыточно мал.

- [ ] Добавить `DensityMonitor` — отслеживает max/avg/p95 плотность по чанкам
- [ ] `SamplingContext` принимает не константу, а функцию от плотности: `limit = f(density)`
- [ ] Добавить `lod.adaptive=true` в `Configuration` — включает/выключает адаптивный LOD
- [ ] Метрики LOD выводить в `WorldSnapshot.getMetrics()` для мониторинга

### S3-3: Профилировщик тиков и bottleneck-детектор
**Новый файл:** `engine/metrics/TickProfiler.java`

Нет инструмента для измерения, сколько времени занимает каждый сервис на каждом тике.
При деградации производительности причина неизвестна без внешнего профайлера.

```java
public interface TickProfiler {
    void recordServiceDuration(String serviceName, long nanos);
    Map<String, TickStats> getStats(); // avg, p99, max
    void reset();
}
```

- [ ] `ParallelDispatcher` измеряет время `processCell` per service per tick
- [ ] `PhaseScheduler` измеряет время каждой фазы
- [ ] `SimulationContext` предоставляет доступ к `TickProfiler`
- [ ] `AlertService` логирует предупреждение, если сервис занимает > X% tick budget
- [ ] Добавить `GameLoopProfilingTest` — запускает 1000 тиков, проверяет P99 < threshold

### S3-4: Кэш взаимодействий в `InteractionMatrix`
**Файл:** `util/InteractionMatrix.java`

`FeedingService` вызывает `matrix.getChance(predator, prey)` в горячем пути.
Матрица пересчитывается раз при старте, но поиск по двум ключам — это два HashMap.get().

- [ ] Представить матрицу как `int[][]` с индексацией по `speciesOrdinal`
- [ ] Добавить `speciesOrdinal()` в `SpeciesKey` (стабильный int, назначаемый `SpeciesRegistry`)
- [ ] Benchmark: сравнить HashMap.get() vs array access в `InteractionMatrixBenchmark`

---

## ⚙️ SPRINT 4 — Движок: изоляция и расширяемость

### S4-1: `WorldFactory` и декларативное описание мира
**Новый файл:** `engine/WorldFactory.java`

`NaturePlugin.createWorld()` содержит императивный код инициализации: `new Island(...)`,
`new WorldInitializer()`, `initExecutor`. При смене стратегии инициализации — меняется плагин.

```java
public interface WorldFactory<T extends Mortal, W extends SimulationWorld<T>> {
    W create(EventBus eventBus, WorldConfig config);
}
```

- [ ] Ввести `WorldConfig` — декларативное описание размеров, чанков, стратегий
- [ ] `WorldFactory` принимает `WorldConfig` и `EventBus`, возвращает готовый `SimulationWorld`
- [ ] `NaturePlugin` делегирует к `NatureWorldFactory`
- [ ] Это открывает путь к генерации мира из JSON/YAML без изменения кода плагина

### S4-2: Детерминированный replay — сериализация состояния
**Новый файл:** `engine/replay/SimulationRecorder.java`

Нет возможности воспроизвести точный ход симуляции. `DefaultRandomProvider` не сериализуем.
Без этого невозможна точная отладка «почему вымерли волки на тике 437».

- [ ] `RandomProvider` расширить методом `getSeed(): long` и `fromSeed(long): RandomProvider`
- [ ] `SimulationRecorder` пишет на каждом тике: `tickCount`, seed RNG, ключевые события
- [ ] `SimulationReplayer` восстанавливает состояние из записи и воспроизводит тики
- [ ] Формат: бинарный (compact) или JSON (human-readable) — управляется флагом
- [ ] Тест `ReplayDeterminismTest`: запустить симуляцию, записать, воспроизвести, сравнить состояние

### S4-3: `SimulationPlugin` Registry — динамическая загрузка плагинов
**Новый файл:** `engine/PluginRegistry.java`

`NatureLauncher` и `SimCityLauncher` хардкодят конкретные плагины. Для сменных модулей
нужен реестр, который загружает плагин по имени/классу.

```java
public class PluginRegistry {
    public void register(String name, Supplier<SimulationPlugin<?>> factory);
    public SimulationPlugin<?> get(String name);
    public Set<String> listAvailable();
}
```

- [ ] `PluginRegistry` — singleton или DI-managed компонент в `SimulationEngine`
- [ ] Поддержка загрузки через `ServiceLoader` (стандартный Java plugin SPI)
- [ ] `SimulationEngine.start(String pluginName, ...)` — запуск по имени
- [ ] Пример: `engine.start("nature", 100, 4)` вместо `engine.start(new NaturePlugin(), 100, 4)`

### S4-4: Многомировая симуляция — несколько миров в одном движке
**Файл:** `engine/SimulationEngine.java`

Движок может управлять только одной симуляцией. Для A/B-тестирования параметров,
сравнения доменов, клиент-серверных сценариев нужна поддержка нескольких миров.

- [ ] `SimulationEngine` возвращает `SimulationContext` — один контекст = один мир
- [ ] Создать `MultiSimulationManager` — хранит Map<String, SimulationContext>
- [ ] Изолированный `EventBus` per context (уже реализован — `new DefaultEventBus()` per engine.build())
- [ ] После S1-1 (SpeciesKey): изолированный `SpeciesRegistry` per context
- [ ] Тест: два `NaturePlugin` с разными параметрами запускаются параллельно без взаимовлияния

---

## 🌐 SPRINT 5 — Новые домены и игровые механики

### S5-1: Расширить SimCity-плагин до рабочего прототипа
**Директория:** `com/island/simcity/`

SimCity существует как proof-of-concept, но без реальных игровых механик:
нет дорог, нет балансировки зонирования, нет графа связности.

- [ ] `ZoningService`: автоматическое расширение жилых/коммерческих зон
- [ ] `TrafficService`: симуляция потоков между зонами по дорожной сети
- [ ] `ConnectivityService`: граф связности — `Resident` доволен только если дом соединён с работой
- [ ] `CityConfig` POJO + `CityWorldFactory` — параллель с `Configuration` в nature
- [ ] `SimCitySurvivalTest`: город с 10 жителями > 100 тиков → happiness > 50

### S5-2: Система ресурсов — универсальная механика для всех доменов
**Новый файл:** `engine/resource/ResourceSystem.java`

Ресурсы (еда, деньги, энергия, вода) — общая абстракция для SimCity и природы.
Сейчас «энергия» зашита в `HealthComponent`. Ресурсная система выносит это наружу.

```java
public interface ResourceSystem<T extends Mortal> {
    void addResource(T entity, String resourceType, long amount);
    long getResource(T entity, String resourceType);
    boolean consumeResource(T entity, String resourceType, long amount);
}
```

- [ ] Реализовать `FixedResourceSystem` — до N типов ресурсов в `EnumMap`
- [ ] `HealthComponent.currentEnergy` мигрирует в ресурс типа `"energy"`
- [ ] SimCity `Resident.happiness` — ресурс типа `"happiness"`
- [ ] Тест: один организм с двумя ресурсами, потребление одного не влияет на другой

### S5-3: Погодная система как движковый компонент
**Новый файл:** `engine/environment/WeatherSystem.java`

`Season` реализован в домене `nature` как enum. Погода — универсальная механика
(температура, осадки) применима в SimCity, природе, стратегиях.

```java
public interface EnvironmentSystem<T extends Mortal> extends ScheduledTask {
    float getTemperature(int x, int y);
    float getPrecipitation(int x, int y);
    void addWeatherModifier(WeatherModifier modifier);
}
```

- [ ] Перенести `Season` из `nature.entities` в `engine.environment`
- [ ] `DefaultWeatherSystem`: глобальная температура + градиент по координатам
- [ ] `NaturePlugin` подписывает `LifecycleService` на погодные события вместо прямого `getSeason()`
- [ ] SimCity `EconomyService` реагирует на температуру (зимой затраты выше)

### S5-4: Процедурная генерация мира
**Новый файл:** `engine/worldgen/WorldGenerator.java`

`WorldInitializer` заполняет мир случайно с равномерным распределением. Никакой топологии:
ни рек, ни биомов, ни высот. Это ограничивает реализм и разнообразие симуляций.

```java
public interface WorldGenerator<T extends Mortal> {
    void generate(SimulationWorld<T> world, WorldGenConfig config);
}
```

- [ ] `NoiseWorldGenerator`: heightmap через Perlin/Simplex noise → биомы
- [ ] `BiomeClassifier`: высота + влажность → тип биома (`FOREST`, `DESERT`, `WETLAND`)
- [ ] `TerrainType` расширить: `RIVER`, `MOUNTAIN`, `PLAINS`
- [ ] `Animal` имеет `Set<TerrainType> preferredTerrains` — влияет на маршруты движения
- [ ] Тест: сгенерировать мир 50x50, убедиться что биомы связны и покрывают >80% карты

---

## 🏗 SPRINT 6 — Инфраструктура разработки

### S6-1: Benchmark-сюита как CI-gate
**Новая директория:** `src/benchmark/`

Нет автоматических проверок производительности. Рефакторинг может незаметно
деградировать throughput на 30% без срабатывания тестов.

- [ ] Подключить JMH (Java Microbenchmark Harness) как `test` scope зависимость
- [ ] `EntityContainerBenchmark`: add/remove/countByType при 1K, 10K, 100K сущностях
- [ ] `InteractionMatrixBenchmark`: getChance() при HashMap vs array backing
- [ ] `ParallelDispatcherBenchmark`: throughput при 4/8/16 чанках, 2/4/8 потоках
- [ ] CI (GitHub Actions): запускать benchmarks на PR, падать при деградации > 15%

### S6-2: Архитектурные тесты через ArchUnit
**Новый файл:** `src/test/java/com/island/ArchitectureTest.java`

Нарушения слоёв (engine → nature) обнаруживались вручную при ревью. Нужны автоматические
проверки, которые падают в CI при появлении нового нарушения.

```java
@ArchTest
static ArchRule engineHasNoDomainDependencies =
    noClasses().that().resideInAPackage("..engine..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..nature..", "..simcity..");
```

- [ ] Подключить ArchUnit как тестовую зависимость
- [ ] Проверка: `engine` не импортирует `nature` или `simcity`
- [ ] Проверка: `util` не импортирует `nature` или `simcity`
- [ ] Проверка: все `CellService`-реализации находятся в `*.service.*` пакете
- [ ] Проверка: `Component`-реализации — data-only (нет методов с побочными эффектами)

### S6-3: Contract-тесты для `SimulationPlugin`
**Новый файл:** `engine/SimulationPluginContractTest.java`

Любой новый плагин должен удовлетворять базовым контрактам движка.
Сейчас это проверяется только через интеграционные тесты конкретного плагина.

```java
// Абстрактный тест — наследуется каждым плагином
public abstract class SimulationPluginContractTest<T extends Mortal> {
    protected abstract SimulationPlugin<T> createPlugin();

    @Test void worldCreationShouldBeIdempotent() { ... }
    @Test void registerTasksShouldNotThrow() { ... }
    @Test void tenTicksShouldNotThrow() { ... }
    @Test void stopShouldReleaseResources() { ... }
}
```

- [ ] Создать `SimulationPluginContractTest<T>` в пакете `engine`
- [ ] `NaturePluginContractTest extends SimulationPluginContractTest<Organism>`
- [ ] `SimCityPluginContractTest extends SimulationPluginContractTest<SimEntity>`
- [ ] Любой новый плагин автоматически покрывается набором контрактных проверок

### S6-4: Mutation testing — оценка качества тестов
**Конфигурация:** `pom.xml`

Текущие тесты могут проходить при наличии логических ошибок. Mutation testing выявляет
«мёртвые» тесты, которые проходят даже при изменении условий в коде.

- [ ] Подключить PITest (`pitest-maven-plugin`) в `pom.xml`
- [ ] Запустить mutation coverage для пакетов `engine` и `nature.service`
- [ ] Целевой показатель: mutation score > 70% для `engine`, > 60% для `nature.service`
- [ ] Добавить PITest в CI как информационный (не blocking) шаг

---

## 🔮 Долгосрочные направления (backlog)

### L-1: Сетевая симуляция — распределённый движок
Распределить чанки мира по нескольким JVM/машинам. Граничные ячейки — зоны обмена.

- [ ] Исследование: Akka Cluster vs. custom Netty vs. gRPC для обмена состоянием граничных чанков
- [ ] Протокол `ChunkHandoffMessage`: передача сущностей между нодами
- [ ] `DistributedSimulationWorld` — каждая нода владеет подмножеством чанков
- [ ] Консистентность: eventual consistency для статистики, strong consistency для перемещений

### L-2: Полноценный ECS с Archetype-based хранилищем
Заменить `ConcurrentHashMap<Class, Component>` на Archetype-storage (как в Unity DOTS, bevy).
Гарантирует cache-friendly итерацию: все `HealthComponent` подряд в памяти.

- [ ] Исследование архетипов: `Archetype` = уникальная комбинация компонентов
- [ ] `ComponentChunk`: массив компонентов одного типа для одного архетипа
- [ ] `EntityQuery` итерирует по чанкам, не по сущностям
- [ ] Профилировать: насколько улучшается cache hit rate при 50K сущностях

### L-3: Скриптовый движок для игровых правил
Правила симуляции (вероятности охоты, метаболизм) сейчас в `species.properties` + Java.
Для non-developer game designers нужен DSL или скриптовый язык.

- [ ] Исследование: Groovy DSL vs. Lua (LuaJ) vs. JavaScript (GraalVM) vs. YAML-rules
- [ ] `RuleEngine` интерфейс: принимает контекст события, возвращает решение
- [ ] `ScriptedHuntingStrategy`: `HuntingStrategy` загружается из файла правил
- [ ] Hot-reload: изменение правил без перезапуска симуляции

### L-4: Визуализация — WebSocket/REST API для frontend
Консольный вывод (`ConsoleView`) не масштабируется для реального продукта.
Нужен канал для передачи состояния мира внешнему клиенту.

- [ ] `SimulationApiServer`: embedded HTTP-сервер (Undertow/Netty)
- [ ] `GET /api/world/snapshot` — текущий `WorldSnapshot` в JSON
- [ ] `WebSocket /api/world/stream` — `WorldSnapshot` на каждый N-й тик
- [ ] `POST /api/simulation/start|stop|pause` — управление жизненным циклом
- [ ] Пример React-клиента с Canvas-рендерингом сетки

### L-5: Сохранение и загрузка состояния (Save/Load)
Симуляция не может быть сохранена и продолжена. Критично для долгоживущих симуляций.

- [ ] `WorldSerializer`: сериализует полное состояние `SimulationWorld` в байты
- [ ] Формат: Protocol Buffers (скорость) или JSON (читаемость) — флагом
- [ ] `WorldDeserializer`: восстанавливает мир, включая состояние всех `Organism`
- [ ] `ObjectPool` должен поддерживать pre-warming из десериализованных объектов
- [ ] Тест: сохранить на тике 100, загрузить, запустить ещё 100 тиков — результат воспроизводим

---

## 📐 Принципы при реализации

> Следующие принципы применяются ко всем задачам этого документа.

1. **Engine остаётся domain-free.** Любой новый класс в `com.island.engine` не должен импортировать `nature` или `simcity`. Проверяется через ArchUnit (S6-2).

2. **Один источник истины для событий.** Смерть/рождение публикуются ровно в одном месте — `Island.onEntityRemoved/Added`. Сервисы только меняют состояние сущностей.

3. **Новая механика — новый плагин или сервис, не изменение ядра.** Добавление погоды, ресурсов, новых видов не должно требовать изменения `GameLoop`, `PhaseScheduler`, `SimulationEngine`.

4. **Тест до реализации для всех HF и S1-S4 задач.** Каждый hotfix сопровождается тестом, который падал до и проходит после.

5. **Benchmark для всего, что касается горячего пути.** `processCell`, `getComponent`, `publish`, `getChance` — измеряются до и после изменений.
