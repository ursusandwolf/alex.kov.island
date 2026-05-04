# Code Review v3: alex.kov.island (branch: feature/pr20)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-04  
**База:** ветка `dev` (v2 ревью)  
**PR:** feature/pr20  
**Дельта:** 37 изменённых файлов, 2 новых (`ExecutionMode.java`, `Mushroom.java`), 2 удалённых (`SimulationBootstrap.java`, `Cabbage.java`)

---

## Сводная таблица: прогресс PR относительно v2

| Проблема из v2 | Статус | Подробности |
|---|---|---|
| Семантическая регрессия параллелизма (все сервисы priority=50) | ✅ **ИСПРАВЛЕНО** | Lifecycle=90, Feed=80, Move=70, Repro=60, Cleanup=10 |
| `isParallelizable()` — слабая boolean-семантика | ✅ **ИСПРАВЛЕНО** | Заменён `ExecutionMode` enum |
| Executor leak в `NaturePlugin.createWorld()` | ✅ **ИСПРАВЛЕНО** | `try/finally { initExecutor.shutdown() }` |
| `Configuration.load()` грузит только 3 поля из ~50 | ✅ **ИСПРАВЛЕНО** | Reflection по всем полям |
| `int seasonDuration = 50` — magic number | ✅ **ИСПРАВЛЕНО** | `config.getSeasonDuration()` |
| `WorldListener<Object>` — слабая типизация | ✅ **ИСПРАВЛЕНО** | `WorldListener<T>` параметризован |
| `SimulationWorld.getConfiguration()` возвращает `Object` | ✅ **ИСПРАВЛЕНО** | `SimulationWorld<T, C>` + `C getConfiguration()` |
| `AbstractService.processCell(Cell)` — конкретный тип в базе | ✅ **ИСПРАВЛЕНО** | `processCell(SimulationNode<Organism>)` |
| `AbstractService.tick()` — мёртвый `world == null` | ✅ **ИСПРАВЛЕНО** | Удалён |
| `SimulationBootstrap` — мёртвый код | ✅ **ИСПРАВЛЕНО** | Удалён |
| Два независимых RNG в `SimulationEngine` | ✅ **ИСПРАВЛЕНО** | `renderer` убран из `SimulationContext`, API упрощён |
| `SpeciesKey` — глобальный синглтон | ❌ **НЕ ИСПРАВЛЕНО** | Остался `static final REGISTRY` |
| `CityMap.getConfiguration()` возвращает `null` | ❌ **НЕ ИСПРАВЛЕНО** | Возвращает `Object`, игнорирует `<T, C>` |
| Downcast `(Island)` в `NatureLauncher` | ⚠️ **ДОПУСТИМО** | Принято как ограничение launcher-слоя |

**Итог: 11 из 13 активных проблем v2 закрыты. Все HIGH-приоритетные исправлены.**

---

## ✅ Детальный разбор исправлений

### 1. ExecutionMode + приоритеты сервисов — закрытие главной проблемы v2

**Было (v2):** все сервисы с `isParallelizable()=true` и `priority=50` → один параллельный проход, Feed и Lifecycle на одной ячейке одновременно.

**Стало (v3):**
```java
// engine/ExecutionMode.java — явная семантика вместо boolean
public enum ExecutionMode { SEQUENTIAL, PARALLEL }

// CellService defaults
default ExecutionMode executionMode() { return ExecutionMode.PARALLEL; }

// Сервисы — явная цепочка приоритетов
LifecycleService:  priority() = 90  // 1-я волна: метаболизм
FeedingService:    priority() = 80  // 2-я волна: кормление
MovementService:   priority() = 70  // 3-я волна: движение
ReproductionService: priority() = 60 // 4-я волна: размножение
CleanupService:    priority() = 10  // 5-я волна: очистка
```

`GameLoop.runTick()` при разных приоритетах сбрасывает `parallelGroup` и запускает волну. Семантика v1 восстановлена, но теперь **явно выражена в коде**, а не является случайным следствием порядка добавления задач. Это правильная архитектура.

**Нюанс для документации:** порядок волн зависит от того, что сервисы имеют разные приоритеты, и этот порядок нигде не задокументирован явно как инвариант. При добавлении нового сервиса разработчик должен понять систему приоритетов — рекомендуется добавить комментарий в `TaskRegistry.registerAll()` или константы:

```java
// TaskRegistry.java — рекомендация
private static final int PRIORITY_LIFECYCLE   = 90;
private static final int PRIORITY_FEEDING     = 80;
private static final int PRIORITY_MOVEMENT    = 70;
private static final int PRIORITY_REPRODUCTION = 60;
private static final int PRIORITY_CLEANUP     = 10;
```

---

### 2. SimulationWorld<T, C> — типобезопасная конфигурация

```java
// engine/SimulationWorld.java
public interface SimulationWorld<T extends Mortal, C> extends Tickable {
    C getConfiguration(); // теперь типизировано
    ...
}

// nature/NatureWorld.java
public interface NatureWorld extends SimulationWorld<Organism, Configuration> { ... }

// nature/model/Island.java
public Configuration getConfiguration() { return config; } // возвращает конкретный тип
```

`AbstractService` делает `world.getConfiguration()` без каста — теперь это типобезопасно. Хорошее решение.

---

### 3. WorldListener<T> — полная типобезопасность

```java
// engine/WorldListener.java
public interface WorldListener<T> {
    void onEntityAdded(T entity);
    void onEntityRemoved(T entity);
}

// nature/model/Island.java
public class Island implements NatureWorld, WorldListener<Organism> {
    public void onEntityAdded(Organism entity) {
        if (entity instanceof Animal a) { onOrganismAdded(a.getSpeciesKey()); }
    }
}

// nature/model/Cell.java
for (WorldListener<Organism> l : world.getListeners()) {
    l.onEntityAdded(animal); // передаёт Organism, не Object
}
```

Цепочка полностью типобезопасна. `instanceof SpeciesKey` из v2 заменён на `instanceof Animal` — это доменная логика (не все Organism являются Animal, биомасса не имеет SpeciesKey-статистики). Корректно.

---

### 4. Configuration.load() — reflection-подход

```java
for (java.lang.reflect.Field field : Configuration.class.getDeclaredFields()) {
    String propertyKey = "island." + field.getName();
    String value = System.getProperty(propertyKey, props.getProperty(propertyKey));
    if (value != null) {
        field.setAccessible(true);
        if (field.getType() == int.class) field.setInt(config, Integer.parseInt(value));
        else if (field.getType() == long.class) field.setLong(config, Long.parseLong(value));
    }
}
```

Принцип верный: загружаются все поля без ручного перечисления. Поддержка system properties (`-Disland.islandWidth=200`) как override — хороший паттерн для ops-конфигурирования.

**Три оставшихся дефекта этого подхода:**

**Дефект 1 [MEDIUM]: Не поддерживаются `boolean`, `double`, `float` поля.**  
Все поведенческие флаги типа `boolean someFlag` и `double someRate` будут молча проигнорированы при загрузке из файла. Сейчас `Configuration` содержит только `int`/`long` поля (проверено), поэтому это не блокирует. Но при расширении конфигурации автор может добавить `boolean` или `double` и не заметить, что файл его не читает.

```java
// Рекомендация: добавить поддержку + предупреждение для неподдерживаемых типов
} else if (field.getType() == boolean.class) {
    field.setBoolean(config, Boolean.parseBoolean(value));
} else if (field.getType() == double.class) {
    field.setDouble(config, Double.parseDouble(value));
} else {
    log.warn("Unsupported config field type {} for field {}", field.getType(), field.getName());
}
```

**Дефект 2 [MEDIUM]: Все ошибки молча поглощаются.**

```java
} catch (Exception e) {
    // Ignore errors for individual fields, fallback to default
}
```

Если `species.properties` содержит `island.islandWidth=abc` — симуляция стартует с дефолтом без единого предупреждения в логах. В production это приведёт к неожиданному поведению, которое невозможно отладить.

```java
// Рекомендация
} catch (NumberFormatException e) {
    log.warn("Invalid value '{}' for config property '{}', using default", value, propertyKey);
} catch (IllegalAccessException e) {
    log.error("Cannot set config field '{}' via reflection", field.getName(), e);
}
```

**Дефект 3 [LOW]: `field.setAccessible(true)` без проверки финальности.**  
Если кто-то добавит `private final int someField` — `setAccessible(true)` + `setInt()` сработает (Java reflection позволяет) но нарушит ожидаемую immutability. Решение: проверять `Modifier.isFinal(field.getModifiers())` и пропускать.

---

### 5. Executor leak — исправлен корректно

```java
// NaturePlugin.java
ExecutorService initExecutor = Executors.newSingleThreadExecutor();
try {
    initializer.initialize(island, ..., initExecutor, ...);
} finally {
    initExecutor.shutdown();
}
```

Паттерн правильный. `shutdown()` (не `shutdownNow()`) — позволяет инициализации завершиться, затем корректно завершает поток. ✅

---

### 6. Новая сущность Mushroom — удаление Cabbage

```java
public class Mushroom extends Biomass {
    public Mushroom(Configuration config, long maxBiomass, int speed) {
        super(config, "Mushroom", SpeciesKey.MUSHROOM, maxBiomass, speed);
    }
}
```

`GenericAnimal` обновлён: `canEat(SpeciesKey.MUSHROOM)`. Замена данных в `species.properties` предполагается. Минимальный, чистый changeset. ✅

---

### 7. TerrainType и Season — замена @RequiredArgsConstructor на явные конструкторы

Замена `@RequiredArgsConstructor` на явный конструктор в enum — правильное решение. Lombok-аннотации на enum могут давать неожиданное поведение, явный конструктор читается без знания Lombok. ✅

---

## 🔴 Новые проблемы, введённые в PR

### 1. [LOW] Мусорный import в GameLoop.java

```java
// GameLoop.java — строки 6-7
import lombok.extern.slf4j.Slf4j;  // ← НЕ используется: @Slf4j не стоит на классе
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ...
private static final Logger log = LoggerFactory.getLogger(GameLoop.class); // ← ручная декларация
```

В v2 был `@Slf4j` (Lombok). В v3 он заменён ручной декларацией, но `import lombok.extern.slf4j.Slf4j` остался. Не влияет на компиляцию, но захламляет.

```java
// Fix: убрать строку
import lombok.extern.slf4j.Slf4j;
```

---

### 2. [MEDIUM] CityMap.getConfiguration() не использует типизацию SimulationWorld<T, C>

```java
// simcity/model/CityMap.java
public Object getConfiguration() { return null; } // ← Object, не C
// CityMap должен реализовывать SimulationWorld<SimEntity, Void> или SimulationWorld<SimEntity, CityConfig>
```

`SimulationWorld<T, C>` введён именно для того, чтобы избавиться от `Object`. Но SimCity не воспользовался этим. `CityMap` реализует `SimulationWorld<SimEntity, ?>` неявно через `Object`, что обнуляет типобезопасность для SimCity-домена.

```java
// Правильно: объявить конкретный тип или использовать Void
public class CityMap implements SimulationWorld<SimEntity, Void> {
    @Override public Void getConfiguration() { return null; }
}
// или создать CityConfig POJO и SimulationWorld<SimEntity, CityConfig>
```

---

### 3. [LOW] SimulationRenderer — сирота в engine

```java
// engine/SimulationRenderer.java — существует, но нигде не используется движком
public interface SimulationRenderer {
    void display(WorldSnapshot snapshot);
    void setSilent(boolean silent);
}
```

В v2 `SimulationRenderer` передавался в `SimulationEngine.start(plugin, tick, threads, renderer)` и хранился в `SimulationContext`. В v3 `renderer` убран из `SimulationEngine` и `SimulationContext`. Рендеринг теперь регистрируется через `TaskRegistry` как лямбда-задача.

Это **регрессия в архитектуре рендеринга**: движок потерял осведомлённость о рендерере, нельзя из `SimulationContext` получить доступ к renderer и, например, включить silent-режим. `SimulationRenderer` interface висит в пакете `engine` без связи с остальным кодом.

**Варианты:**
- **(A)** Вернуть `renderer` в `SimulationContext` как `Optional<SimulationRenderer>` — не обязательный, но доступный
- **(B)** Удалить `SimulationRenderer` из `engine`, рендеринг полностью в плагине — тогда честная изоляция
- **(C)** Оставить как есть, задокументировать что рендеринг — responsibility плагина

---

### 4. [LOW] AbstractService.tick() — избыточный `afterTick` вызов

```java
// AbstractService.tick() — fallback для тестов
public void tick(int tickCount) {
    beforeTick(tickCount);
    for (...) { processCell(node, tickCount); }
    afterTick(tickCount);  // ← вызывается, но afterTick() — no-op в базе
}
```

При нормальном выполнении через `GameLoop`, `beforeTick()` и `afterTick()` вызываются явно в `runCellServicesParallel()`. В fallback-пути `tick()` они тоже вызываются. Это не баг, но `afterTick()` никогда не переопределяется ни одним сервисом — dead code extension point.

---

## ❌ Оставшиеся проблемы (не затронуты PR)

### SpeciesKey — глобальный синглтон

```java
private static final Map<String, SpeciesKey> REGISTRY = new ConcurrentHashMap<>();
public static final SpeciesKey WOLF = register("wolf", true);
// ...
public static final SpeciesKey MUSHROOM = register("mushroom", false); // ← Mushroom добавлен
```

Не исправлено и не планировалось в этом PR. Блокирует запуск нескольких изолированных симуляций (в том числе параллельных тестов). Для текущего масштаба — не критично, но ограничение остаётся.

### Services processCell — instanceof Cell внутри domain-сервисов

```java
// FeedingService, MovementService и др.
public void processCell(SimulationNode<Organism> node, int tickCount) {
    if (node instanceof Cell cell) { ... }
}
```

После того как сигнатура `processCell` поднята до `SimulationNode<Organism>`, внутренний `instanceof Cell` стал **доменной реальностью**, а не архитектурным нарушением. `nature`-сервисы знают о `Cell` — это нормально. Однако это означает, что `AbstractService`-сервисы непригодны для реализации с другим типом ноды без переопределения паттерна. Для текущих целей — приемлемо.

---

## 🔬 Оценка новых тестов

### shouldExecuteTasksInPriorityOrder() — отличный тест

```java
// GameLoopConcurrencyTest.java
@Test
void shouldExecuteTasksInPriorityOrder() {
    List<Integer> executionOrder = new CopyOnWriteArrayList<>();
    // Добавляет задачи с priority 50, 100, 10 (намеренно не в порядке)
    gameLoop.runTick();
    assertEquals(List.of(100, 50, 10), executionOrder);
}
```

Этот тест закрывает главный риск, выявленный в v2: проверяет что `ScheduledTask.priority()` реально влияет на порядок выполнения, а не только декларируется. Тест написан через `runTick()` (не через `start()`), что правильно для детерминированного unit-тестирования порядка.

**Что ещё стоит добавить:**

```java
// Тест 1: CellService с ExecutionMode.PARALLEL действительно идёт в parallelGroup
// Тест 2: Задача в Phase.PREPARE выполняется до Phase.SIMULATION
@Test
void shouldExecutePreparePhaseBeforeSimulation() {
    List<String> phases = new CopyOnWriteArrayList<>();
    gameLoop.addRecurringTask(new ScheduledTask() {
        @Override public Phase phase() { return Phase.PREPARE; }
        @Override public int priority() { return 50; }
        @Override public void tick(int tc) { phases.add("PREPARE"); }
    });
    gameLoop.addRecurringTask(new ScheduledTask() {
        @Override public Phase phase() { return Phase.SIMULATION; }
        @Override public int priority() { return 50; }
        @Override public void tick(int tc) { phases.add("SIMULATION"); }
    });
    gameLoop.runTick();
    assertEquals(List.of("PREPARE", "SIMULATION"), phases);
}

// Тест 3: исключение в одном сервисе не останавливает остальные
@Test
void shouldContinueAfterServiceException() {
    AtomicBoolean secondRan = new AtomicBoolean(false);
    gameLoop.addRecurringTask((Tickable) tc -> { throw new RuntimeException("boom"); });
    gameLoop.addRecurringTask((Tickable) tc -> secondRan.set(true));
    gameLoop.runTick();
    assertTrue(secondRan.get());
}
```

### Tests обновлены: SimulationBootstrap → NaturePlugin/SimulationEngine

`ExtinctionBalanceTest` и `StressStabilityTest` обновлены — теперь используют `NaturePlugin` и `SimulationEngine` вместо удалённого `SimulationBootstrap`. Это **интеграционные тесты через публичный API движка** — правильный уровень абстракции.

---

## 💡 Рекомендации для следующего PR

### [HIGH] Починить Configuration.load() — обработка ошибок

```java
// Заменить тихое поглощение
} catch (Exception e) {
    // Ignore
}

// На информативное логирование
} catch (NumberFormatException e) {
    log.warn("Config: invalid value '{}' for '{}', using default {}", 
             value, propertyKey, getCurrentValue(field, config));
} catch (IllegalAccessException e) {
    log.error("Config: cannot access field '{}' via reflection", field.getName(), e);
}
```

### [MEDIUM] Добавить поддержку `boolean`/`double` в Configuration.load()

```java
} else if (field.getType() == boolean.class) {
    field.setBoolean(config, Boolean.parseBoolean(value));
} else if (field.getType() == double.class) {
    field.setDouble(config, Double.parseDouble(value));
}
```

### [MEDIUM] Определиться с SimulationRenderer — убрать или использовать

Либо:
```java
// Вариант A: вернуть в SimulationContext
public class SimulationContext<T extends Mortal> {
    private final Optional<SimulationRenderer> renderer;
    // ...
    public void render() { renderer.ifPresent(r -> r.display(world.createSnapshot())); }
}
```
Либо удалить `SimulationRenderer` из `engine` и полностью отдать рендеринг плагину.

### [MEDIUM] CityMap — использовать типизацию SimulationWorld<SimEntity, Void>

```java
public class CityMap implements SimulationWorld<SimEntity, Void> {
    @Override
    public Void getConfiguration() { return null; }
}
```

### [LOW] Убрать мусорный import в GameLoop.java

```java
// Удалить:
import lombok.extern.slf4j.Slf4j;
```

### [LOW] Добавить константы приоритетов в TaskRegistry

```java
// TaskRegistry.java
private static final int PRI_LIFECYCLE    = 90;
private static final int PRI_FEEDING      = 80;
private static final int PRI_MOVEMENT     = 70;
private static final int PRI_REPRODUCTION = 60;
private static final int PRI_CLEANUP      = 10;
```

---

## 📊 Итоговая оценка v3

| Критерий | v1 | v2 | v3 (PR) | Δ |
|---|---|---|---|---|
| **Архитектура** | 6.5 | 7.5 | **8.0** | ↑ ExecutionMode, typed config, typed listener |
| **Код** | 7.0 | 7.5 | **8.0** | ↑ executor leak fixed, reflection config, приоритеты |
| **Переиспользуемость** | 6.0 | 7.0 | **7.5** | ↑ engine абсолютно domain-free |
| **Тестируемость** | 5.0 | 6.0 | **7.0** | ↑ priority test, SimulationBootstrap удалён |
| **Общая** | 6.5 | 7.5 | **8.0** | ↑ |

---

## Вердикт

**PR готов к merge с незначительными замечаниями.**

Все три блокирующих проблемы из v2-ревью закрыты:
- Приоритеты сервисов расставлены → семантика симуляции корректна
- Executor leak устранён → тесты не оставляют hanging threads
- `Configuration.load()` охватывает все поля → конфигурируемость работает

Единственное замечание уровня MEDIUM для обсуждения перед merge: `Configuration.load()` молча поглощает ошибки парсинга — в production это источник трудно отлаживаемых проблем. Добавить `log.warn` для malformed values — 5 минут работы, но существенно улучшает операционную надёжность.

**Готовность движка к роли универсальной платформы: 80%.** Оставшиеся 20% — `SpeciesKey`-синглтон (multi-tenant), `CityMap` конфигурация, и покрытие тестами Phase/ExecutionMode поведения `GameLoop`.
