# Code Review v5: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-05  
**База:** v4 (dev после merge PR20)  
**Дельта:** 40 изменённых файлов, +6 новых, -1 удалён (`WorldListener.java`)  
**Ключевые изменения:** `PhaseScheduler`, `ParallelDispatcher`, `CellProcessor`-пул, immutable `EventBus` в `SimulationWorld`, volatile ECS-компоненты, type hierarchy в `EventBus`, виртуальные потоки

---

## Сводная таблица: прогресс относительно v4

| Проблема из v4 | Статус | Детали |
|---|---|---|
| `setEventBus()` — изменяемое состояние в `SimulationWorld` | ✅ **ИСПРАВЛЕНО** | `createWorld(EventBus)` — immutable с рождения |
| `Organism.components` — `HashMap` не thread-safe | ✅ **ИСПРАВЛЕНО** | `ConcurrentHashMap` |
| `DefaultEventBus.publish()` — исключение рушит цепочку | ✅ **ИСПРАВЛЕНО** | `try/catch` на каждого подписчика + `log.error` |
| `SimulationWorld<T,C>` — `getConfiguration()` в движке | ✅ **ИСПРАВЛЕНО** | Параметр `C` убран, метод перешёл в `NatureWorld` |
| `CityMap.getConfiguration()` — костыльный `Object`/`Void` | ✅ **ИСПРАВЛЕНО** | `SimulationWorld<T>` без `C` → CityMap чистый |
| `HealthComponent`/`AgeComponent` — не volatile | ✅ **ИСПРАВЛЕНО** | Все поля `volatile` |
| `GameLoop` — God Class (scheduling + dispatching + threading) | ✅ **ИСПРАВЛЕНО** | Выделены `PhaseScheduler` и `ParallelDispatcher` |
| `EventBus` — нет `unsubscribe` | ✅ **ИСПРАВЛЕНО** | Добавлен `unsubscribe()` |
| Нет теста на exception isolation в `EventBus` | ✅ **ИСПРАВЛЕНО** | `EventBusTest.shouldHandleSubscribersThrowingExceptions` |
| Нет теста thread-safety компонентов | ✅ **ИСПРАВЛЕНО** | `ComponentThreadSafetyTest` |
| `LifecycleService` — двойной publish `EntityDiedEvent` | ✅ **ИСПРАВЛЕНО** | Publish удалён |
| Двойная отчётность смертей — `FeedingService` (EATEN_BY_PACK) | ❌ **НЕ ИСПРАВЛЕНО** | Два события на одну смерть |
| Двойная отчётность — `MovementService` (MOVEMENT_EXHAUSTION) | ❌ **НЕ ИСПРАВЛЕНО** | Два события на одну смерть |
| Двойная отчётность — `ReproductionService` (REPRODUCTION_EXHAUSTION) | ❌ **НЕ ИСПРАВЛЕНО** | Два события на одну смерть |
| `SpeciesKey` — глобальный синглтон | ❌ **НЕ ИСПРАВЛЕНО** | Остался с v1 |

**Итог: 11 из 14 активных проблем v4 закрыты.**

---

## ✅ Детальный разбор значимых улучшений

### 1. PhaseScheduler + ParallelDispatcher — правильное разделение ответственностей

`GameLoop` в v4 был God Class: управлял потоком выполнения, группировал задачи по фазам, диспетчеризировал параллельные чанки. В v5 ответственности разделены:

```
GameLoop          — координатор: тикает мир, дренирует очередь задач, делегирует выполнение
PhaseScheduler    — группирует задачи по Phase, сортирует по priority, формирует parallelGroup
ParallelDispatcher — параллельное выполнение одной группы CellService через ExecutorService
```

`PhaseScheduler.execute()` — чистый алгоритм без состояния между тиками (кроме `phasedTasks`/`parallelGroup`, которые очищаются в начале каждого execute). `ParallelDispatcher.dispatch()` — изолированная единица параллелизма с `CountDownLatch`.

**Нюанс:** `PhaseScheduler.parallelGroup` — это `List`, объявленный как поле класса и очищаемый в `execute()`. При текущей однопоточной `runTick()` это корректно. Но если в будущем `execute()` будет вызываться из нескольких потоков — `parallelGroup.clear()` + последующий `add()` создадут race condition. Рекомендуется держать `parallelGroup` как локальную переменную внутри `execute()`.

---

### 2. CellProcessor пул — устранение GC-давления

```java
// ParallelDispatcher — пул объектов вместо new Callable<>() на каждый тик
private final List<CellProcessor<T>> processorPool = new ArrayList<>();

// Каждый тик: переиспользуем существующие процессоры
CellProcessor<T> processor = processorPool.get(i++);
processor.update(unit, services, tickCount, latch); // обновляем состояние
taskExecutor.execute(processor);                    // отправляем в пул
```

В v4 `GameLoop` создавал `new Callable<>()` для каждого чанка на каждом тике — при 100 тиках в секунду и 20 чанках это 2000 объектов/сек в GC. Пул устраняет эти аллокации.

**Анализ корректности `volatile` полей в `CellProcessor`:**

```java
private volatile Collection<? extends SimulationNode<T>> unit;
private volatile List<CellService<T, SimulationNode<T>>> services;
private volatile int tickCount;
private volatile CountDownLatch latch;
private volatile Throwable error;
```

`update()` устанавливает 5 полей последовательно — не атомарно как группа. Это могло бы быть проблемой при конкурентном вызове `update()` + `run()`, но жизненный цикл корректен: `dispatch()` вызывает `update()`, затем `execute()`, затем ждёт `latch.await()` — следующий `update()` происходит только после завершения `latch`. Видимость обеспечивается `volatile`. Корректно.

**Замечание:** `volatile` на `List<CellService>` обеспечивает видимость ссылки на список, но не его содержимого. Если `services` список мутируется между тиками — нужна дополнительная синхронизация. Сейчас список не мутируется после передачи в `CellProcessor`, поэтому это не проблема. Но это неочевидный инвариант — стоит задокументировать.

---

### 3. EventBus с type hierarchy — неожиданно мощная функция

```java
// DefaultEventBus — subscribe(Object.class) получает ВСЕ события
private Set<Class<?>> getTypeHierarchy(Class<?> type) {
    // BFS по суперклассам и интерфейсам
    // EntityDiedEvent → [EntityDiedEvent, Object]
    // String → [String, CharSequence, Comparable, Serializable, Object]
}
```

Подписка на `Object.class` теперь работает как wildcard-подписчик — получает все события. `AlertService` использует именно такой паттерн (подписывается на конкретный тип). Функция полезна для будущих `MetricsCollector`, `AuditLog` и т.д.

**Риск:** Каждый `publish()` итерирует по иерархии типов события. Для `EntityDiedEvent` иерархия невелика (~2 типа). Но если кто-то опубликует `ArrayList` или другой сложный тип — иерархия будет большой. Кеш `typeHierarchyCache` решает это для повторных вызовов. ✅

**Тест `shouldPublishToHierarchicalSubscribers` проверяет эту функцию** — хорошо. ✅

---

### 4. Virtual Thread поддержка

```java
// SimulationEngine.build()
ExecutorService executor = (threads > 0)
    ? Executors.newFixedThreadPool(threads)
    : Executors.newVirtualThreadPerTaskExecutor(); // Java 21+
```

Для CPU-bound задач симуляции виртуальные потоки не дают преимущества (нет I/O-блокировок). Но для тестов и future use (сетевые симуляции, плагины с внешними вызовами) — правильный extension point. ✅

---

### 5. SimulationWorld<T> без параметра C — упрощение интерфейса

В v3 `SimulationWorld<T, C>` был введён для типобезопасного `getConfiguration()`. В v5 параметр `C` убран — `getConfiguration()` перенесён в `NatureWorld` как доменный метод. `SimulationWorld` больше не знает о конфигурации.

Это **правильное упрощение**: движку не нужна конфигурация плагина. `CityMap` больше не обязан реализовывать `getConfiguration()` с заглушкой. ✅

---

## 🔴 Остающийся критический баг: двойная отчётность смертей (3 случая)

Из v4 исправлен только `LifecycleService`. Три сервиса продолжают публиковать `EntityDiedEvent` напрямую, при этом `Island.onEntityRemoved()` тоже публикует событие при удалении из ячейки.

### Случай 1: FeedingService — EATEN vs EATEN_BY_PACK

```java
// FeedingService.java:128-129
a.die(DeathCause.EATEN);                    // lastDeathCause = EATEN
if (node.removeEntity(a)) {
    // → Cell.removeAnimal() → Island.onEntityRemoved()
    // → publish(EntityDiedEvent(a, "EATEN"))          ← СОБЫТИЕ 1
    
    eventBus.publish(EntityDiedEvent(a, "EATEN_BY_PACK")); // ← СОБЫТИЕ 2
}
```

`StatisticsService` регистрирует: +1 к смертям EATEN, +1 к смертям EATEN_BY_PACK. Одна смерть — две записи.

### Случай 2: MovementService — MOVEMENT_EXHAUSTION vs HUNGER

```java
// MovementService.java:71
animal.consumeEnergy(moveCost); // внутри: die(DeathCause.HUNGER) если energy=0
if (!animal.isAlive()) {
    eventBus.publish(EntityDiedEvent(animal, "MOVEMENT_EXHAUSTION")); // ← СОБЫТИЕ 1
    // animal ещё В ячейке — CleanupService следующего тика:
    // cell.cleanupDeadEntities() → removeEntity() → Island.onEntityRemoved()
    // → publish(EntityDiedEvent(animal, "HUNGER"))                   ← СОБЫТИЕ 2
}
```

Одна смерть → MOVEMENT_EXHAUSTION + HUNGER. Разные причины, но один организм.

### Случай 3: ReproductionService — REPRODUCTION_EXHAUSTION vs HUNGER

Аналогичен MovementService: `consumeEnergy()` устанавливает `lastDeathCause = HUNGER`, сервис публикует `REPRODUCTION_EXHAUSTION`, CleanupService позже публикует `HUNGER`.

**Итог:** Все счётчики смертей по причинам (`StatisticsService.getTotalDeaths()`) содержат дубли. `SurvivalCalibrationTest` работает с некорректными данными.

**Единственное правильное решение** — один источник истины:

```java
// Шаг 1: убрать прямые publish() из всех сервисов для смертей
// (оставить только в Island.onEntityRemoved)

// Шаг 2: для смертей без немедленного removeEntity (MovementService, ReproductionService)
// установить правильную DeathCause перед consume:
animal.die(DeathCause.MOVEMENT_EXHAUSTION); // явно, не через tryConsumeEnergy

// Шаг 3: CleanupService при удалении тригернёт onEntityRemoved → единственный publish
```

---

## 🟡 Новые замечания в v5

### 1. [MEDIUM] EventBus.publish() — wildcard через Object.class не задокументирован

```java
// Недокументированная ловушка: подписка на Object.class
bus.subscribe(Object.class, e -> log.info("any event: {}", e)); // получает ВСЕ события
```

Это мощная функция, но её отсутствие в Javadoc к `EventBus` означает, что разработчик может обнаружить это случайно. Добавить в `EventBus.subscribe()`:

```java
/**
 * Registers a subscriber for a specific event type.
 * Supports type hierarchy: subscribing to a parent class/interface will
 * receive events of all subtypes. E.g., subscribe(Object.class, ...) 
 * receives ALL events.
 */
```

---

### 2. [MEDIUM] `PhaseScheduler.parallelGroup` — поле класса вместо локальной переменной

```java
// PhaseScheduler — parallelGroup как instance field
private final List<CellService<T, SimulationNode<T>>> parallelGroup = new ArrayList<>();

// execute() очищает в начале каждой фазы:
parallelGroup.clear();
```

Нет проблемы при текущей однопоточной `runTick()`. Но `parallelGroup` — это рабочий буфер для одного вызова `execute()`, а не долгоживущее состояние `PhaseScheduler`. Семантически правильнее — локальная переменная. Это также устранит скрытую зависимость от порядка вызовов:

```java
// Рекомендуется: локальная переменная
public void execute(SimulationWorld<T> world, List<ScheduledTask> tasks, int tickCount) {
    // ...
    List<CellService<T, SimulationNode<T>>> parallelGroup = new ArrayList<>();
    // ...
}
```

---

### 3. [LOW] `WorldListener` удалён — но `SimulationNode` получил `default void onEntityAdded/Removed`

```java
// SimulationNode.java — новые default-методы
default void onEntityAdded(T entity) { }
default void onEntityRemoved(T entity) { }
```

`WorldListener` был отдельным интерфейсом (паттерн Observer). Теперь колбэки переехали в `SimulationWorld` как `onEntityAdded/onEntityRemoved` и в `SimulationNode` как `default`-методы. Это концептуально другой паттерн — не Observer, а Template Method.

Последствие: `SimulationWorld<T>` теперь обязан реализовывать `onEntityAdded/onEntityRemoved`. `CityMap` также обязан. Сейчас `CityMap` не реализует эти методы — они наследуют пустые дефолты. Это корректно для текущего состояния SimCity, но разработчик может добавить статистику в будущем и не заметить, что нужно переопределить методы в `CityMap`.

---

### 4. [LOW] `CellProcessor` пул никогда не сжимается

```java
// ParallelDispatcher — пул растёт, но не уменьшается
while (processorPool.size() < unitCount) {
    processorPool.add(new CellProcessor<>());
}
```

При смене размера мира (или если `getParallelWorkUnits()` когда-либо вернёт меньше чанков) — избыточные `CellProcessor` останутся в пуле до конца жизни `ParallelDispatcher`. Для текущих фиксированных размеров мира — не проблема. При динамических мирах — утечка памяти.

---

### 5. [LOW] `GameLoopOptimizationTest` — слабое утверждение для пула

```java
// GameLoopOptimizationTest
assertTrue(gameLoop.getTickCount() >= 5); // не проверяет реюз процессоров
```

Тест называется `shouldReuseCellProcessorsAndReduceAllocations`, но не проверяет ни реюз (`processorPool` приватный), ни аллокации. Фактически тест только проверяет, что `runTick()` работает 5 раз без исключений. Переименовать или добавить настоящую проверку через рефлексию или счётчик конструктора `CellProcessor`.

---

## 📊 Комплексная оценка прогресса проекта (v1 → v5)

### Динамика по версиям

| Критерий | v1 | v2 | v3 (PR20) | v4 | v5 | Всего Δ |
|---|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 7.5 | 8.0 | 8.0 | **8.5** | +2.0 |
| **Код** | 7.0 | 7.5 | 8.0 | 7.5 | **8.0** | +1.0 |
| **Переиспользуемость** | 6.0 | 7.0 | 7.5 | 8.0 | **8.5** | +2.5 |
| **Тестируемость** | 5.0 | 6.0 | 7.0 | 7.5 | **8.0** | +3.0 |
| **Общая** | 6.5 | 7.5 | 8.0 | 7.5 | **8.5** | +2.0 |

### Что изменилось за 5 версий — архитектурная карта

#### Engine пакет: был монолит → стал framework

```
v1:  GameLoop (всё в одном)
v5:  GameLoop (координатор)
      ├── PhaseScheduler (порядок фаз и приоритетов)
      └── ParallelDispatcher (параллельное выполнение чанков)
           └── CellProcessor[] (пул переиспользуемых воркеров)

v1:  SimulationContext (импортировал nature.view!)
v5:  SimulationContext (zero domain imports, несёт EventBus)

v1:  нет plugin mechanism
v5:  SimulationPlugin<T> — createWorld(EventBus), registerTasks(), lifecycle hooks

v1:  нет event system
v5:  EventBus с type hierarchy, unsubscribe, exception isolation
```

#### Domain isolation: был нарушен → стал чистым

```
v1:  engine → imports nature.view              ❌
v5:  engine → zero domain imports              ✅

v1:  Cell instanceof Island                    ❌
v5:  world.onEntityAdded/onEntityRemoved()     ✅

v1:  AbstractService → NatureWorld (конкретный тип)
v5:  AbstractService → NatureWorld (domain interface, не engine)  ✅

v1:  SimulationWorld<T,C> → getConfiguration() в движке  ❌
v5:  getConfiguration() только в NatureWorld             ✅
```

#### Параллелизм: был хрупким → стал надёжным

```
v1:  Thread.sleep + ArrayList для задач (race condition)
v5:  ScheduledExecutorService + ConcurrentLinkedQueue   ✅

v2:  все сервисы priority=50 → один параллельный проход (семантическая регрессия)
v5:  PRIORITY_LIFECYCLE=90 → PRIORITY_CLEANUP=10 → волновой порядок  ✅

v4:  Organism.components — HashMap (not thread-safe)
v5:  ConcurrentHashMap + volatile component fields      ✅

v4:  GameLoop создавал new Callable<>() per chunk per tick
v5:  CellProcessor пул — zero allocation per tick      ✅
```

#### Тестируемость: была почти нулевой → стала приемлемой

```
v1:  только интеграционные тесты через полный Island
v5:  + ArchitectureEvolutionTest (ECS, EventBus)
     + ConfigurationReflectionTest (config loading)
     + EventBusTest (exception isolation, unsubscribe, hierarchy)
     + ComponentThreadSafetyTest (volatile visibility)
     + GameLoopOptimizationTest (parallel error handling)
     + RefactoringVerificationTest (GridUtils, SamplingContext, double-lock)
     + SurvivalCalibrationTest (balance instrument)
```

### Что НЕ изменилось за 5 версий

| Проблема | Версия появления | Статус |
|---|---|---|
| `SpeciesKey` — глобальный статический реестр | v1 | ❌ Не исправлен |
| Двойная отчётность смертей (3 сервиса) | v4 | ❌ Частично исправлен |
| `AbstractService.processCell` — `instanceof Cell` | v2 | ⚠️ Принят как domain fact |
| Нет System-слоя в ECS (Component без System) | v4 | ⚠️ Первый шаг сделан |

---

## 💡 Приоритеты для следующей итерации

### [HIGH] Закрыть двойную отчётность смертей (3 сервиса)

Один паттерн на все три случая:

```java
// MovementService — вместо: consumeEnergy() + publish(MOVEMENT_EXHAUSTION)
if (!animal.isAlive()) {
    // Переопределить причину смерти на доменно-корректную
    animal.setLastDeathCause(DeathCause.MOVEMENT_EXHAUSTION);
    // Не публиковать — CleanupService уберёт тело → onEntityRemoved → единственный publish
}

// ReproductionService — аналогично
animal.setLastDeathCause(DeathCause.REPRODUCTION_EXHAUSTION); // без publish

// FeedingService — для EATEN_BY_PACK: убрать прямой publish
// a.die(EATEN_BY_PACK) вместо die(EATEN) + publish(EATEN_BY_PACK)
// Тогда Island.onEntityRemoved опубликует правильную причину
```

Изменение в 3 файлах, ~6 строк. Устраняет всю двойную статистику.

### [MEDIUM] Задокументировать type hierarchy в EventBus

```java
/**
 * Subscribes to events of the given type AND all its subtypes.
 * Subscribing to {@code Object.class} acts as a wildcard for all events.
 */
<E> void subscribe(Class<E> eventType, Consumer<E> subscriber);
```

### [MEDIUM] Перевести `PhaseScheduler.parallelGroup` в локальную переменную

```java
public void execute(...) {
    List<CellService<T, SimulationNode<T>>> parallelGroup = new ArrayList<>();
    // ...
}
```

### [LOW] Решить судьбу `SpeciesKey`-синглтона

Технический долг существует с v1. Для запуска нескольких изолированных симуляций в одной JVM (например, параллельных тестов) — блокирует. Решение: `SpeciesRegistry` как объект, создаваемый `SpeciesLoader`, без статического реестра в `SpeciesKey`.

### [LOW] Улучшить `GameLoopOptimizationTest`

Переименовать в `shouldHandleParallelExecutionCorrectly` или добавить реальную проверку пула через рефлексию.

---

## Вердикт

**Проект готов к роли универсального симуляционного движка на ~85%.**

За 5 итераций проект прошёл путь от «учебной симуляции острова» до **архитектурно обоснованного game engine** с:
- Чистым `engine`-пакетом без доменных зависимостей
- Рабочей plugin-системой (два независимых домена: `nature` и `simcity`)
- Безопасным параллелизмом (`PhaseScheduler` + `ParallelDispatcher` + `CellProcessor` пул)
- Типизированным event-driven взаимодействием
- ECS-задатками (`Component`, `ConcurrentHashMap`-хранилище, `volatile`-поля)
- Приемлемым тестовым покрытием движка

**Одно блокирующее для следующего PR:** двойная отчётность смертей в 3 сервисах — это 6 строк кода, но делает `StatisticsService` и `SurvivalCalibrationTest` недостоверными.

**После этого фикса** проект можно считать production-ready foundation для симуляторов уровня «Весёлая Ферма» / упрощённый SimCity. До уровня полноценного SimCity остаётся: `SpeciesKey`-синглтон, полноценный ECS `System`-слой, и пространственный индекс для >10K сущностей.
