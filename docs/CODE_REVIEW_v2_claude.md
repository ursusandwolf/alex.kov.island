# Code Review v2: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-02  
**Предыдущее ревью:** 2026-05-02 (v1)  
**Новых файлов:** +13 (127 vs 114)  
**Формат:** Diff-ревью: что исправлено, что осталось, что появилось нового

---

## Сводная таблица: прогресс относительно v1

| Проблема из v1 | Статус | Комментарий |
|---|---|---|
| `SimulationContext` импортирует `nature.view` | ✅ **ИСПРАВЛЕНО** | Введён `SimulationRenderer` в `engine` |
| Нет `SimulationPlugin` интерфейса | ✅ **ИСПРАВЛЕНО** | `NaturePlugin`, `SimCityPlugin` реализуют |
| `LifecycleService` `super(null, ...)` | ✅ **ИСПРАВЛЕНО** | Один конструктор, без null |
| Дублирование sampling-логики | ✅ **ИСПРАВЛЕНО** | `SamplingUtils` выделен |
| `CityMap.getParallelWorkUnits()` — аллокация на каждом тике | ✅ **ИСПРАВЛЕНО** | Lazy-init кеш |
| `CityTile` использует `CopyOnWriteArrayList` | ✅ **ИСПРАВЛЕНО** | `ArrayList` + `ReentrantLock` |
| `DefaultBiomassManager` встроен в `Island` | ✅ **ИСПРАВЛЕНО** | Выделен отдельный класс |
| `NatureDomainContext` отсутствует | ✅ **ИСПРАВЛЕНО** | Введён `@Builder`-класс |
| `Cell` делает `instanceof Island` | ✅ **ИСПРАВЛЕНО** | `WorldListener` callback pattern |
| `System.err` в `GameLoop` | ✅ **ИСПРАВЛЕНО** | SLF4J `log.error` |
| `Thread.sleep` в `NatureLauncher` | ✅ **ИСПРАВЛЕНО** | `ScheduledExecutorService` |
| Нет unit-теста для `GameLoop` | ✅ **ИСПРАВЛЕНО** | `GameLoopConcurrencyTest` добавлен |
| `SimulationBootstrap` — мёртвый код | ❌ **НЕ ИСПРАВЛЕНО** | Файл всё ещё существует |
| `Configuration.load()` грузит только 3 поля из ~50 | ❌ **НЕ ИСПРАВЛЕНО** | Критический дефект остался |
| `SpeciesKey` — глобальный синглтон | ❌ **НЕ ИСПРАВЛЕНО** | Блокирует multi-tenant |
| `Island.updateSeason()` — `int seasonDuration = 50` | ❌ **НЕ ИСПРАВЛЕНО** | Magic number |
| `SimulationWorld.getConfiguration()` возвращает `Object` | ❌ **НЕ ИСПРАВЛЕНО** | Не типобезопасно |
| `AbstractService.tick()` `instanceof Cell` fallback | ❌ **НЕ ИСПРАВЛЕНО** | Остался downcast |

**Итог прогресса: 12 из 18 проблем v1 устранены. Хороший спринт.**

---

## ✅ Что реально улучшилось — детальный разбор

### 1. SimulationPlugin + SimulationEngine — Архитектурный прорыв

```java
// engine/SimulationPlugin.java — чистый интерфейс без доменных зависимостей
public interface SimulationPlugin<T extends Mortal> {
    SimulationWorld<T> createWorld();
    void registerTasks(GameLoop<T> gameLoop, SimulationWorld<T> world);
    default void onSimulationStarted(SimulationContext<T> context) { }
    default void onSimulationStopped(SimulationContext<T> context) { }
}
```

Это прямое закрытие главного архитектурного gap'а предыдущей версии. `SimulationEngine` теперь полностью domain-free. Запуск новой симуляции сводится к реализации двух методов. Отлично.

**Нюанс:** `SimulationEngine.build()` создаёт собственный `new DefaultRandomProvider()` независимо от того, что плагин использует внутри. Результат — две несвязанные RNG-цепочки в одной симуляции. Для deterministic replay это будет проблемой.

```java
// SimulationEngine.java:38
RandomProvider random = new DefaultRandomProvider(); // ← игнорирует RNG плагина
SimulationContext<T> context = new SimulationContext<>(world, gameLoop, renderer, random);
```

**Рекомендация:** либо `SimulationPlugin.createRandomProvider()` как extension point, либо передавать `random` как параметр в `build()`.

---

### 2. WorldListener — корректное устранение instanceof Island

```java
// До (v1):
if (world instanceof Island island) {
    island.onOrganismAdded(animal.getSpeciesKey());
}

// После (v2):
for (com.island.engine.WorldListener l : world.getListeners()) {
    l.onEntityAdded(animal.getSpeciesKey());
}
```

Паттерн правильный. `Island` регистрирует себя слушателем, Cell не знает о конкретной реализации мира. LSP соблюдён.

**Однако — нерешённая проблема с типизацией:**

```java
// engine/WorldListener.java
public interface WorldListener {
    void onEntityAdded(Object key);   // ← Object — слишком слабый тип
    void onEntityRemoved(Object key);
}

// Island.onEntityAdded — защищается instanceof
public void onEntityAdded(Object key) {
    if (key instanceof SpeciesKey sk) { ... }  // ← молчаливо игнорирует неверный тип
}
```

`WorldListener` с `Object key` — это потеря типобезопасности на уровне движка. Если другой разработчик вызовет `l.onEntityAdded("wrong-type")`, код скомпилируется, выполнится без исключений и ничего не сделает. Ошибка обнаружится только в runtime, если вообще обнаружится.

**Правильное решение:**
```java
// Параметризовать по типу ключа
public interface WorldListener<K> {
    void onEntityAdded(K key);
    void onEntityRemoved(K key);
}

// SimulationWorld использует конкретный тип
List<WorldListener<T>> getListeners(); // где T extends Mortal — или отдельный параметр ключа
```

---

### 3. Phase / ScheduledTask / priority — правильное направление, неполная реализация

Новая система `Phase` (PREPARE → SIMULATION → POSTPROCESS) и `ScheduledTask` с `priority()` и `isParallelizable()` — это правильная идея для управления порядком задач. `CellService` теперь расширяет `ScheduledTask` с умными defaults:

```java
// CellService.java
default Phase phase() { return Phase.SIMULATION; }
default int priority() { return 50; }
default boolean isParallelizable() { return true; }
```

**Критическая проблема: семантическая регрессия параллелизма.**

В v1 сервисы выполнялись **последовательными волнами**: Feed для ВСЕГО мира → Move для ВСЕГО мира → Repro → Lifecycle → Cleanup. Это давало гарантию: к моменту Move все хищники уже поели.

В v2 все 5 сервисов имеют `priority=50` и `isParallelizable=true`. В `GameLoop.runTick()` они попадают в **один** `parallelGroup` и обрабатываются за **один** вызов `runCellServicesParallel()`:

```java
// GameLoop.java — все сервисы в одной группе
for (Collection<? extends SimulationNode<T>> unit : workUnits) {
    tasks.add(() -> {
        for (SimulationNode<T> node : unit) {
            for (CellService<T, SimulationNode<T>> service : services) {
                // Feed, Move, Repro, Lifecycle, Cleanup — всё за один проход по чанку
                service.processCell(node, tickCount);
            }
        }
        return null;
    });
}
```

Теперь для **каждой ячейки** в чанке выполняется `Feed → Move → Repro → Lifecycle → Cleanup`. Животное может быть накормлено `FeedingService`, затем немедленно перемещено `MovementService` — в рамках того же прохода по ячейке. Сосед из другого чанка обрабатывается параллельно, и может видеть животное одновременно в двух местах на разных стадиях обработки.

Это **не то же самое поведение**, что было в v1. Это изменение игровой механики, не задокументированное и не тестируемое.

**Как должно быть:** разные сервисы должны иметь разные приоритеты, отражающие порядок зависимостей:

```java
// Правильная расстановка приоритетов
public class LifecycleService extends AbstractService {
    @Override public int priority() { return 90; } // Первым: обновить метаболизм

public class FeedingService extends AbstractService {
    @Override public int priority() { return 80; } // Второй: кормить

public class MovementService extends AbstractService {
    @Override public int priority() { return 70; } // Третий: двигать

public class ReproductionService extends AbstractService {
    @Override public int priority() { return 60; }

public class CleanupService extends AbstractService {
    @Override public int priority() { return 10; } // Последний: чистка
```

При разных приоритетах `parallelGroup` будет сбрасываться при смене приоритета, восстанавливая волновой порядок. **Без этого система Phase/Priority — инфраструктура без реальной функции.**

---

### 4. NatureDomainContext — хорошее DI-решение

```java
@Getter
@Builder
public class NatureDomainContext {
    private final Configuration config;
    private final SpeciesRegistry speciesRegistry;
    private final InteractionProvider interactionProvider;
    // ...
}
```

Чистая агрегация зависимостей домена. `Island` теперь принимает `NatureDomainContext` вместо 5+ параметров. Конструктор `Island` стал читаемым.

**Нюанс:** `ProtectionService` инициализируется в `NaturePlugin` с `config.getIslandWidth() * config.getIslandHeight()` как `totalCells`. Это значение вычисляется до создания `Island`. Если в будущем `Island` получит динамический размер — `ProtectionService` будет инициализирован с устаревшим значением.

---

## 🔴 Новые проблемы, введённые в v2

### 1. [HIGH] ExecutorService leak в NaturePlugin.createWorld()

```java
// NaturePlugin.java:71
initializer.initialize(island, domainContext.getSpeciesRegistry(), 
    domainContext.getAnimalFactory(),
    java.util.concurrent.Executors.newSingleThreadExecutor(), // ← УТЕЧКА!
    domainContext.getRandomProvider());
```

`WorldInitializer.initialize()` принимает `ExecutorService` и использует его для параллельного заполнения мира. Этот executor **никогда не завершается**. При каждом вызове `createWorld()` (например, в тестах) создаётся новый поток, который живёт до завершения JVM.

**Fix:**
```java
ExecutorService initExecutor = Executors.newSingleThreadExecutor();
try {
    initializer.initialize(island, ..., initExecutor, ...);
} finally {
    initExecutor.shutdown(); // или shutdownNow()
}
```

---

### 2. [MEDIUM] AbstractService.tick() — мёртвая ветка с null-guard

После исправления `LifecycleService` (убран `super(null, ...)`), конструктор `AbstractService` теперь **всегда** получает ненулевой `NatureWorld`. Но мёртвый null-guard остался:

```java
// AbstractService.tick() — строка 53
if (world == null) {
    return; // ← никогда не выполнится при текущем конструкторе
}
```

Это не дефект, но вводит в заблуждение: разработчик читает код и думает «бывает случай, когда world == null». Убрать.

---

### 3. [MEDIUM] AbstractService.tick() — instanceof Cell всё ещё там

```java
// AbstractService.java:59 — fallback для тестов
for (SimulationNode<Organism> node : unit) {
    if (node instanceof Cell cell) {  // ← downcast в базовом классе сервисов
        processCell(cell, tickCount);
    }
}
```

`AbstractService` — базовый класс. Он знает о `Cell` (конкретной реализации `SimulationNode`) из домена `nature`. Это нарушение DIP. Тест, который запускает сервис с кастомной реализацией `SimulationNode` (не `Cell`), молчаливо пропустит все ноды.

**Корень проблемы:** `processCell(Cell node, ...)` — сигнатура принимает конкретный тип `Cell`, а не `SimulationNode<Organism>`. Это делает тест-изоляцию невозможной без использования реального `Cell`. Метод должен принимать `SimulationNode<Organism>`, или параметр типа в `CellService<T, N>` должен использоваться последовательно.

---

### 4. [LOW] SimulationBootstrap — мёртвый код

```java
// Файл всё ещё существует, никем не вызывается
public class SimulationBootstrap { ... }
```

`NaturePlugin` делает всё то же самое. `SimulationBootstrap` не используется ни в `NatureLauncher`, ни в тестах. Удалить.

---

### 5. [LOW] Два downcast в точках входа

```java
// NatureLauncher.java:58
Island island = (Island) context.getWorld(); // ← знает о конкретном типе

// SimCityPlugin.java:33
CityMap map = (CityMap) world; // ← знает о конкретном типе
```

`NatureLauncher` обращается к `island.getSpeciesCounts()` — метод, которого нет в `SimulationWorld`. `SimCityPlugin` кастует `world` для передачи в сервисы. Это **неизбежно** пока `SimulationWorld` не имеет достаточно широкого интерфейса. Это допустимо в точке входа домена, но нужно документировать как сознательное ограничение.

Более глубокая проблема: эти касты означают, что `SimulationEngine` не изолирует движок полностью — плагин и лаунчер всё равно работают с конкретными типами. `SimulationContext.getWorld()` возвращает `SimulationWorld<T>`, а не плагин-специфичный тип — для этого нужен либо дополнительный accessor в `SimulationContext`, либо плагин должен хранить ссылку на свой `world` самостоятельно.

---

## ❌ Оставшиеся критические проблемы из v1

### Configuration.load() — конфигурируемость сломана

Состояние **не изменилось** с v1. Из ~50 параметров класса только 3 читаются из файла:

```java
// Configuration.java — только это работает из properties
config.islandWidth = getIntProperty(props, "island.width", ...);
config.islandHeight = getIntProperty(props, "island.height", ...);
config.tickDurationMs = getIntProperty(props, "island.tickDurationMs", ...);
// feedingLodLimit, wolfPackMinSize, plantGrowthRateBP... — ВСЕГДА из hardcoded defaults
```

Это HIGH-priority дефект: изменение `species.properties` не имеет эффекта на 95% параметров симуляции.

---

### SpeciesKey — глобальный статический реестр

```java
private static final Map<String, SpeciesKey> REGISTRY = new ConcurrentHashMap<>();
public static final SpeciesKey WOLF = register("wolf", true);
```

Неизменно. Невозможно запустить две независимые симуляции с разными наборами видов. Блокирует тестирование изолированных миров.

---

### SimulationWorld.getConfiguration() возвращает Object

```java
Object getConfiguration(); // ← типобезопасность нулевая
```

```java
// CityMap.java
public Object getConfiguration() { return null; } // SimCity прямо возвращает null
```

`AbstractService` делает `world.getConfiguration()` и приводит к `Configuration`. Если добавить второй домен с другим объектом конфигурации — ClassCastException в runtime.

---

### Island и Cell — оставшиеся instanceof

В доменном коде остались обоснованные instanceof (например, `node instanceof Cell cell` внутри `nature` пакета — это нормально). Но:

```java
// Island.java:175 — в интерфейсном методе SimulationWorld
public Optional<SimulationNode<Organism>> getNode(SimulationNode<Organism> current, int dx, int dy) {
    if (current instanceof Cell cell) { // ← Island знает только о Cell
```

Этот паттерн приемлем, поскольку `Island` — конкретная реализация `SimulationWorld<Organism>` и ожидает `Cell` как свой тип ноды. Это **не нарушение**, но следует задокументировать как архитектурное ограничение: `Island` работает только с `Cell`, не с произвольными `SimulationNode`.

---

## 🔬 Новый тест GameLoopConcurrencyTest — оценка

```java
void shouldNotThrowExceptionWhenAddingTaskFromAnotherThread()
```

Тест проверяет потокобезопасность `addRecurringTask` при конкурентных вызовах. Это **правильно** — `ConcurrentLinkedQueue` для `pendingTasks` решает race condition, которая была в `ArrayList` v1. Тест это подтверждает.

**Что тест НЕ проверяет:**
- Правильный порядок выполнения задач по `priority`
- Что `CellService` с `isParallelizable=true` действительно выполняется параллельно
- Что исключение в параллельном сервисе не обрушивает весь тик
- Что `stop()` корректно завершает все in-flight задачи

**Рекомендуемые дополнения:**
```java
// Тест: задачи с разным priority исполняются в правильном порядке
@Test
void shouldExecuteHighPriorityTaskBeforeLow() {
    List<Integer> executionOrder = new CopyOnWriteArrayList<>();
    // Add low-priority task first
    // Add high-priority task second
    // After runTick(), verify high runs before low
}

// Тест: исключение в одном CellService не останавливает другие
@Test
void shouldContinueAfterCellServiceException() {
    AtomicBoolean secondServiceRan = new AtomicBoolean(false);
    // Add throwing CellService
    // Add normal CellService that sets flag
    // After runTick(), assert secondServiceRan == true
}
```

---

## 🏗 Архитектурный анализ: что изменилось в дизайне

### Слоёвая чистота engine пакета

**v1:** `engine/SimulationContext.java` → импортировал `nature.view.SimulationView`  
**v2:** `engine/SimulationContext.java` → использует `engine.SimulationRenderer`

```
engine пакет (v2):
├── GameLoop, CellService, Mortal, Tickable     ← инфраструктура
├── SimulationPlugin, SimulationEngine           ← новый plugin mechanism
├── Phase, ScheduledTask                         ← управление порядком  
├── SimulationRenderer                           ← абстракция рендеринга
├── WorldListener                                ← event callback
└── SimulationContext, SimulationWorld, SimulationNode, WorldSnapshot ← модель
```

**Нет импортов из `nature` или `simcity`.** Это значительный прогресс.

### Зависимости между пакетами (v2)

```
engine  ←  nature.service (AbstractService)
engine  ←  nature.model   (Cell, Island)
engine  ←  simcity.*      (CityMap, CityTile, SimCityPlugin)
util    ←  nature.entities (InteractionMatrix знает о SpeciesKey, AnimalType)
```

`util/InteractionMatrix` всё ещё зависит от `nature.entities` — это граница, которую стоит устранить в следующей итерации: `InteractionProvider` должен работать с generic `Object` ключами или параметрически.

---

## 💡 Приоритетные улучшения для следующей итерации

### [HIGH] Расставить приоритеты у сервисов

```java
// FeedingService
@Override public int priority() { return 80; }

// MovementService  
@Override public int priority() { return 70; }

// ReproductionService
@Override public int priority() { return 60; }

// LifecycleService
@Override public int priority() { return 90; } // первым

// CleanupService
@Override public int priority() { return 10; } // последним
```

Без этого `Phase`/`ScheduledTask` — мёртвая архитектура. Это изменение восстанавливает семантику v1 (волновой порядок) и одновременно использует новую систему приоритетов.

### [HIGH] Исправить executor leak в NaturePlugin

```java
public SimulationWorld<Organism> createWorld() {
    Island island = new Island(domainContext, config.getIslandWidth(), config.getIslandHeight());
    ExecutorService initExecutor = Executors.newSingleThreadExecutor();
    try {
        new WorldInitializer().initialize(island, domainContext.getSpeciesRegistry(),
            domainContext.getAnimalFactory(), initExecutor, domainContext.getRandomProvider());
    } finally {
        initExecutor.shutdown();
    }
    island.init();
    return island;
}
```

### [HIGH] Починить Configuration.load()

Нужно либо полностью загружать все параметры из файла, либо убрать `load()` и принять, что конфигурация задаётся только через код. Полумера хуже, чем ни одна из крайностей.

```java
// Пример полной загрузки:
public static Configuration load() {
    Configuration c = new Configuration();
    Properties p = loadProperties();
    
    c.islandWidth = getIntProperty(p, "island.width", c.islandWidth);
    c.islandHeight = getIntProperty(p, "island.height", c.islandHeight);
    c.tickDurationMs = getIntProperty(p, "tick.durationMs", c.tickDurationMs);
    c.feedingLodLimit = getIntProperty(p, "lod.feeding", c.feedingLodLimit);
    c.reproductionLodLimit = getIntProperty(p, "lod.reproduction", c.reproductionLodLimit);
    c.movementLodLimit = getIntProperty(p, "lod.movement", c.movementLodLimit);
    c.wolfPackMinSize = getIntProperty(p, "wolf.packMinSize", c.wolfPackMinSize);
    c.plantGrowthRateBP = getIntProperty(p, "plant.growthRateBP", c.plantGrowthRateBP);
    // ... все ~50 полей
    return c;
}
```

### [MEDIUM] Параметризовать WorldListener

```java
// engine/WorldListener.java — убрать Object
public interface WorldListener<K> {
    void onEntityAdded(K key);
    void onEntityRemoved(K key);
}

// SimulationWorld<T extends Mortal>
void addListener(WorldListener<?> listener); // или WorldListener<T> если ключ = сущность
```

### [MEDIUM] Удалить SimulationBootstrap

Мёртвый код. Путает картину.

### [MEDIUM] Унифицировать RNG

```java
// SimulationPlugin — добавить extension point
default RandomProvider createRandomProvider() {
    return new DefaultRandomProvider();
}

// SimulationEngine.build() — использовать плагин
RandomProvider random = plugin.createRandomProvider();
```

### [LOW] seasonDuration = 50 → константа

```java
// Island.java
private static final int SEASON_DURATION_TICKS = 50; // или в Configuration
```

### [LOW] AbstractService.tick() — убрать мёртвые ветки

Убрать `if (world == null)` (не может быть null после исправления конструктора). Убрать `instanceof Cell` — или принять как явное документированное допущение о типе ноды.

---

## 🔧 Рефакторинг: как использовать Priority для восстановления семантики

### До (v2 — семантическая регрессия):
```
Тик:
  ┌─ runCellServicesParallel([Feed, Move, Repro, Lifecycle, Cleanup]) ─┐
  │  Chunk 1: cell1→[Feed,Move,Repro,LC,CU], cell2→[...]              │  ← всё вместе
  │  Chunk 2: (параллельно) cell3→[Feed,Move,Repro,LC,CU], ...        │
  └───────────────────────────────────────────────────────────────────┘
```

### После (с правильными приоритетами):
```
Тик:
  ┌─ priority=90: runCellServicesParallel([Lifecycle]) ─┐  Wave 1
  └─────────────────────────────────────────────────────┘
  ┌─ priority=80: runCellServicesParallel([Feeding]) ───┐  Wave 2
  └─────────────────────────────────────────────────────┘
  ┌─ priority=70: runCellServicesParallel([Movement]) ──┐  Wave 3
  └─────────────────────────────────────────────────────┘
  ... и т.д.
```

Код изменений минимальный — только переопределить `priority()` в каждом сервисе. Инфраструктура уже готова.

---

## 📊 Итоговая оценка v2

| Критерий | v1 | v2 | Динамика |
|---|---|---|---|
| **Архитектура** | 6.5/10 | **7.5/10** | ↑ Plugin mechanism, WorldListener, SimulationEngine |
| **Код** | 7.0/10 | **7.5/10** | ↑ SLF4J, SamplingUtils, NatureDomainContext |
| **Переиспользуемость** | 6.0/10 | **7.0/10** | ↑ engine пакет чист, два рабочих плагина |
| **Общая** | 6.5/10 | **7.5/10** | ↑ Заметный прогресс |

---

## Вердикт

**Автор продуктивно отработал ревью v1.** За один спринт устранены 12 из 18 замечаний, включая самые архитектурно значимые. Движок явно движется к правильному виду.

**Три вещи, требующие внимания до следующего feature-разработки:**

1. **Расставить `priority()` у сервисов** — иначе `Phase`/`ScheduledTask` — инфраструктура без смысла, и симуляция работает с изменённой (непроверенной) семантикой.
2. **Починить executor leak в `NaturePlugin`** — каждый тест, создающий мир, оставляет висящий поток.
3. **Починить `Configuration.load()`** — или честно убрать иллюзию конфигурируемости.

**Готовность к роли универсального движка: 75%.** От 65% в v1. Оставшиеся 25% — это `SpeciesKey`-синглтон, типизация `WorldListener`, `Configuration.load()`, и тестовое покрытие именно engine-логики (priority ordering, phase execution, service isolation).
