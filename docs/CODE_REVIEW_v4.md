# Code Review v4: alex.kov.island (branch: dev, после merge PR20)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-04  
**База:** PR20 (v3 ревью)  
**Дельта:** 48 изменённых файлов, +14 новых, -1 удалён (`SimulationRenderer.java`)  
**Ключевые изменения:** EventBus, ECS-компоненты, GridUtils, SamplingContext, константы приоритетов, ConfigurationReflectionTest

---

## Сводная таблица: прогресс относительно PR20

| Проблема из v3 | Статус | Детали |
|---|---|---|
| Мёртвый import `@Slf4j` в `GameLoop` | ✅ **ИСПРАВЛЕНО** | Удалён |
| `CityMap.getConfiguration()` возвращает `Object` | ✅ **ИСПРАВЛЕНО** | `SimulationWorld<SimEntity, Void>` |
| `SimulationRenderer` — сирота в `engine` без использования | ✅ **ИСПРАВЛЕНО** | Удалён |
| `Configuration.load()` — silent swallow ошибок | ✅ **ИСПРАВЛЕНО** | `log.error` вместо `// Ignore` |
| `Configuration.load()` — `boolean`/`double` не поддерживались | ✅ **ИСПРАВЛЕНО** | Добавлены ветки `boolean`/`double` |
| Константы приоритетов сервисов не задокументированы | ✅ **ИСПРАВЛЕНО** | `TaskRegistry.PRIORITY_*` константы |
| Нет теста для `Phase`-порядка | ✅ **ЧАСТИЧНО** | Есть `ArchitectureEvolutionTest`, Phase-тест не добавлен |
| `SpeciesKey` — глобальный синглтон | ❌ **НЕ ИСПРАВЛЕНО** | Остался |
| **Двойная отчётность смертей** | 🔴 **НОВЫЙ БАГ** | EventBus + WorldListener = двойной счёт |
| **`HashMap` для ECS компонентов не thread-safe** | 🔴 **НОВЫЙ БАГ** | `Organism.components` без синхронизации |
| **`EventBus.publish()` не защищён от исключений** | 🟡 **НОВЫЙ РИСК** | Одна сломанная подписка роняет всех |
| **`setEventBus()` — изменяемое состояние в `SimulationWorld`** | 🟡 **НОВЫЙ РИСК** | Нарушает immutability интерфейса |

---

## ✅ Что хорошо сделано в этой версии

### 1. EventBus — правильная идея, чистая реализация интерфейса

```java
// engine/event/EventBus.java — domain-free, generic
public interface EventBus {
    void publish(Object event);
    <E> void subscribe(Class<E> eventType, Consumer<E> subscriber);
}
```

`DefaultEventBus` на `ConcurrentHashMap` + `CopyOnWriteArrayList` — правильный выбор для read-heavy подписчиков. `SimulationEngine` создаёт `EventBus` и передаёт через `registerTasks()` — шина находится в движке, не в домене. Архитектурно корректно.

`StatisticsService.subscribe(EventBus)` — хороший паттерн: сервис сам регистрирует свои интересы, а не заставляет внешний код это делать. ✅

---

### 2. ECS-компоненты — задел на будущее

```java
// engine/ecs/Component.java — маркерный интерфейс в engine
public interface Component { }

// nature/entities/Organism.java — компонентный контейнер
private final Map<Class<? extends Component>, Component> components = new HashMap<>();

public <C extends Component> void addComponent(C component) {
    components.put(component.getClass(), component);
}
public <C extends Component> C getComponent(Class<C> type) {
    return type.cast(components.get(type));
}
```

`HealthComponent`, `AgeComponent`, `MovementComponent` — данные вынесены из `Organism` в компоненты. Это правильный путь к полноценному ECS. `Animal` уже получает `MovementComponent` через `addComponent()`. Потенциал очевиден.

---

### 3. GridUtils — устранение дублирования lock-логики

```java
// util/GridUtils.java — вынесен deadlock-safe double-lock
public static void executeWithDoubleLock(SimulationNode<?> n1, SimulationNode<?> n2,
        int x1, int y1, int x2, int y2, Runnable action) {
    SimulationNode<?> first = (x1 < x2 || (x1 == x2 && y1 < y2)) ? n1 : n2;
    // ... consistent lock ordering
}
```

В v3 этот паттерн дублировался в `Island.moveEntity()` и `Island.moveBiomassPartially()`. Теперь — единая утилита, используемая в трёх местах `Island`. Устранено дублирование deadlock-sensitive кода. ✅

---

### 4. SamplingContext — правильная инкапсуляция параметров

```java
// util/SamplingContext.java
@Value public class SamplingContext {
    int limit;
    RandomProvider random;
}

// Cell.java — новая сигнатура
public void forEachAnimalSampled(SamplingContext context, Consumer<Animal> action)
```

Вместо двух параметров `(int limit, RandomProvider random)` — один объект-контекст. При добавлении нового параметра семплирования (например, `bias` или `seed`) не нужно менять все сигнатуры. Обратная совместимость через перегрузку в `SamplingUtils`. ✅

---

### 5. TaskRegistry.PRIORITY_* константы

```java
public static final int PRIORITY_LIFECYCLE   = 90;
public static final int PRIORITY_FEEDING     = 80;
public static final int PRIORITY_MOVEMENT    = 70;
public static final int PRIORITY_REPRODUCTION = 60;
public static final int PRIORITY_CLEANUP     = 10;
public static final int PRIORITY_VIEW        = 0;
```

Приоритеты теперь именованы и централизованы. Новый сервис берёт константу, а не вписывает магическое число. ✅

---

### 6. ConfigurationReflectionTest — новый тест

Покрывает: дефолтные значения, override через system properties, malformed значения (graceful fallback), `islandHeight`. Это именно то, чего не хватало в v3. Особенно ценен тест на malformed input — он подтверждает, что `log.error` и fallback к default работают корректно.

---

### 7. SurvivalCalibrationTest — инструмент балансировки

```
=== STARTING SURVIVAL CALIBRATION TEST ===
Species    | Avg Pop      | Extinct Rate | Hunger %     | Age %
wolf       | 3.40         | 0.20         | 87.5%        | ...
```

Не unit-тест — **инструмент калибровки**. Запускает 5 итераций по 100 тиков, собирает статистику вымирания и причин смерти, выдаёт рекомендации. Ценный артефакт для настройки баланса симуляции.

---

## 🔴 Критические новые баги

### Баг 1 [CRITICAL]: Двойная отчётность смертей

Это самый серьёзный баг версии. Каждая смерть существа регистрируется в `StatisticsService` **дважды** через два независимых пути.

**Анализ потока для смерти от поедания:**

```
FeedingService:
  1. a.die(DeathCause.EATEN)          → animal.lastDeathCause = EATEN
  2. node.removeEntity(a)             → Cell.removeAnimal()
                                         → WorldListener.onEntityRemoved(a)
                                         → Island.onEntityRemoved(a)
                                         → eventBus.publish(EntityDiedEvent(a, "EATEN"))  ← ПЕРВЫЙ ПУТЬ
                                         → StatisticsService.registerDeath(EATEN)
  3. eventBus.publish(EntityDiedEvent(a, "EATEN"))  ← ВТОРОЙ ПУТЬ (тот же FeedingService!)
                                         → StatisticsService.registerDeath(EATEN)
```

**Анализ потока для смерти от голода/старости:**

```
LifecycleService:
  1. a.tryConsumeEnergy() → die(HUNGER) → lastDeathCause = HUNGER
  2. eventBus.publish(EntityDiedEvent(a, "HUNGER"))  ← ПЕРВЫЙ ПУТЬ
     → StatisticsService.registerDeath(HUNGER)
  [следующий тик:]
  CleanupService:
  3. cell.cleanupDeadEntities()
     → Cell.removeAnimal(a)              ← ВТОРОЙ ПУТЬ
     → WorldListener.onEntityRemoved(a)
     → Island.onEntityRemoved(a)
     → eventBus.publish(EntityDiedEvent(a, "HUNGER"))  [lastDeathCause всё ещё HUNGER]
     → StatisticsService.registerDeath(HUNGER)
```

**Результат:** Все счётчики смертей завышены в 2 раза. `SurvivalCalibrationTest` измеряет ложные данные. `ExtinctionBalanceTest` может проходить на некорректных числах.

**Корневая причина:** Архитектурная неопределённость — кто отвечает за регистрацию смерти: сервис, убивший существо, или `WorldListener`, реагирующий на удаление из ячейки? В v3 был один путь (прямые вызовы `statistics.reportDeath`). В v4 добавили EventBus-путь через `Island.onEntityRemoved`, но **не убрали** прямые вызовы из сервисов.

**Правильное решение — выбрать один путь:**

```java
// ВАРИАНТ A: EventBus как единственный путь (рекомендуется)
// Убрать прямые statistics.reportDeath из сервисов.
// Island.onEntityRemoved публикует событие — StatisticsService слушает.
// Минус: CleanupService должен вызывать removeEntity до releaseAnimal,
// а lastDeathCause должен быть установлен до очистки пула.

// ВАРИАНТ B: Прямые вызовы как единственный путь (проще, без EventBus для статистики)
// Island.onEntityRemoved НЕ публикует EntityDiedEvent (публикует кастомное событие без статистики).
// Сервисы продолжают вызывать statistics.reportDeath напрямую.
```

---

### Баг 2 [HIGH]: `Organism.components` HashMap — не thread-safe

```java
// Organism.java
private final Map<Class<? extends Component>, Component> components = new HashMap<>();
```

`HashMap` не является потокобезопасным. `Organism` участвует в параллельной обработке чанков: один поток читает `getComponent(HealthComponent.class)` в `LifecycleService`, другой — в `FeedingService`. Обе операции `get()` на `HashMap` конкурируют без синхронизации.

Существующий `energyLock` в `Organism` защищает только методы `addEnergy`/`consumeEnergy`, но не сам `components`.

Для `AgeComponent` риск ниже (запись только в `LifecycleService`), но для `HealthComponent` — очень высокий: несколько сервисов читают и пишут `currentEnergy`, `isAlive`.

```java
// Правильное решение: ConcurrentHashMap
private final Map<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();

// Или: оставить HashMap, но вся мутация компонентов через energyLock
// (текущий energyLock уже защищает конкретные методы, но не чтение самой Map)
```

Примечание: `addComponent()` вызывается только в конструкторе и `init()` (из `AnimalFactory`) — до момента попадания объекта в симуляцию. Это снижает риск race condition для самого `put()`. Но `getComponent()` + `HealthComponent.setCurrentEnergy()` без блокировки `components` — всё ещё риск.

---

## 🟡 Новые архитектурные риски

### Риск 1 [MEDIUM]: `DefaultEventBus.publish()` не защищён от исключений подписчиков

```java
public void publish(Object event) {
    subscribers.getOrDefault(eventType, List.of()).forEach(subscriber -> {
        consumer.accept(event); // ← исключение здесь прерывает forEach
    });
}
```

Если первый подписчик выбрасывает `RuntimeException`, `forEach` прерывается — остальные подписчики не получают событие. В параллельном контексте симуляции с несколькими подписчиками это приведёт к частичной обработке и несогласованному состоянию.

```java
// Правильно: изолировать исключения подписчиков
public void publish(Object event) {
    Class<?> eventType = event.getClass();
    for (Consumer<?> subscriber : subscribers.getOrDefault(eventType, List.of())) {
        try {
            @SuppressWarnings("unchecked")
            Consumer<Object> consumer = (Consumer<Object>) subscriber;
            consumer.accept(event);
        } catch (Exception e) {
            log.error("EventBus subscriber threw exception for event {}: {}",
                      eventType.getSimpleName(), e.getMessage(), e);
        }
    }
}
```

---

### Риск 2 [MEDIUM]: `setEventBus()` — изменяемое состояние в `SimulationWorld`

```java
// SimulationWorld.java
void setEventBus(EventBus eventBus); // мутирующий метод в интерфейсе

// SimulationEngine.build()
world.setEventBus(eventBus); // вызывается после createWorld()
```

`SimulationWorld` — это контракт движка. Наличие `setEventBus()` в интерфейсе означает, что **любая** реализация `SimulationWorld` должна поддерживать изменение шины событий в runtime. Это нарушает принцип наименьшего удивления: разработчик плагина ожидает, что `EventBus` задаётся при создании, а не инжектируется снаружи после.

`Island.eventBus` помечен `@Setter` (Lombok) — то есть его можно заменить в любой момент во время симуляции, включая mid-tick.

```java
// Лучше: передать EventBus через createWorld() или в конструктор плагина
// SimulationPlugin:
SimulationWorld<T, ?> createWorld(EventBus eventBus); // EventBus известен до создания мира

// SimulationEngine.build():
EventBus eventBus = new DefaultEventBus();
SimulationWorld<T, ?> world = plugin.createWorld(eventBus); // мир сразу знает свою шину
```

---

### Риск 3 [LOW]: `EntityBornEvent` / `EntityDiedEvent` несут `Mortal`, но подписчики ожидают `Animal`

```java
// EntityBornEvent.java
public class EntityBornEvent {
    private final Mortal entity; // ← Mortal, не Animal
}

// StatisticsService.subscribe()
bus.subscribe(EntityBornEvent.class, event -> {
    if (event.getEntity() instanceof Animal a) { // ← defensive instanceof
        registerBirth(a.getSpeciesKey());
    }
    // если не Animal — молча игнорируется
});
```

Если в будущем добавится новый тип существа (например, `NPC`, `Robot`), рождение которого тоже стоит отслеживать — событие будет молча проигнорировано. `instanceof Animal` становится скрытым фильтром.

Альтернатива: типизировать события по домену — `AnimalBornEvent extends EntityBornEvent` с `Animal getAnimal()`. Или перейти к `EntityBornEvent<T extends Mortal>`. Сейчас это minor, но при расширении домена станет source of bugs.

---

### Риск 4 [LOW]: ECS — частичная миграция без System-слоя

Введены `Component` + компонентный контейнер в `Organism`. Но нет `System`-слоя в ECS-смысле. Сервисы всё ещё работают с `Animal`/`Organism` напрямую, а не с компонентами через запросы.

```java
// Текущий паттерн — не настоящий ECS:
// LifecycleService получает Cell → forEachAnimal → animal.tryConsumeEnergy()
// tryConsumeEnergy внутри обращается к HealthComponent

// Настоящий ECS-паттерн выглядел бы так:
// HealthSystem.process(world.query(HealthComponent.class, AgeComponent.class))
```

Это **не баг** — это честный первый шаг к ECS. Но нужно понимать: текущая реализация даёт гибкость хранения данных в компонентах, но не даёт производительности ECS (cache-friendly iteration по компонентам). Для текущих объёмов — приемлемо.

---

## 📋 Оставшиеся проблемы из предыдущих версий

### SpeciesKey — глобальный синглтон (v1 → v4, не исправлен)

```java
private static final Map<String, SpeciesKey> REGISTRY = new ConcurrentHashMap<>();
public static final SpeciesKey WOLF = register("wolf", true);
// ...
public static final SpeciesKey MUSHROOM = register("mushroom", false);
```

Каждый запуск теста инициализирует один и тот же статический реестр. Параллельные тесты не изолированы. При добавлении нового вида нужно менять эту константу — это coupling между данными и кодом.

**Примечание:** Для текущего масштаба (одна симуляция в JVM) это работает. Но `SurvivalCalibrationTest` запускает 5 итераций в одном JVM — SpeciesKey-синглтон при этом не перезагружается, что потенциально вызывает накопление состояния.

---

## 🔬 Оценка архитектурного теста `ArchitectureEvolutionTest`

Тест проверяет три вещи:

1. `EventBus` доставляет `EntityDiedEvent` — корректный smoke test шины
2. `Organism` поддерживает кастомный `Component` — проверяет расширяемость ECS
3. `Animal` получает `MovementComponent` по умолчанию и синхронизирует скорость при `mutate()`

Это хорошие тесты для новых механизмов. Но тест #3 (`mutate` обновляет `MovementComponent`) является критичным — если `Animal.mutate()` когда-либо сломается, это единственный тест, который это поймает.

**Чего не хватает в тест-сюите:**
- Тест изолированности `EventBus` от исключений подписчиков (см. Риск 1)
- Тест двойной подписки (что не происходит дублирования при повторном `subscribe`)
- Тест thread-safety `DefaultEventBus.publish()` при конкурентной публикации

---

## 💡 Приоритетные улучшения

### [CRITICAL] Устранить двойную отчётность смертей

Выбрать **один** канал регистрации. Рекомендуется вариант A: EventBus как единственный путь.

```java
// Island.onEntityRemoved — ЕДИНСТВЕННЫЙ публикатор EntityDiedEvent
public void onEntityRemoved(Organism entity) {
    if (entity instanceof Animal a && eventBus != null) {
        DeathCause cause = a.getLastDeathCause();
        eventBus.publish(new EntityDiedEvent(a, cause != null ? cause.name() : "REMOVED"));
    }
}

// FeedingService, LifecycleService, MovementService — УБРАТЬ прямые publish(EntityDiedEvent)
// Оставить только: a.die(cause) → node.removeEntity(a)
// Удаление из ячейки само тригернёт Island.onEntityRemoved → EventBus
```

**Для LifecycleService смертей (голод/старость):** животное умирает, но не удаляется до CleanupService. `eventBus.publish` в `LifecycleService` надо убрать — `CleanupService` при `cleanupDeadEntities()` вызывает `removeEntity()`, что тригернёт `onEntityRemoved`.

### [HIGH] Защитить `DefaultEventBus.publish()` от исключений

```java
for (Consumer<?> subscriber : subscribers.getOrDefault(eventType, List.of())) {
    try {
        ((Consumer<Object>) subscriber).accept(event);
    } catch (Exception e) {
        log.error("Subscriber error for {}: {}", eventType.getSimpleName(), e.getMessage(), e);
    }
}
```

### [HIGH] Сделать `EventBus` immutable в `SimulationWorld`

Убрать `setEventBus()` из интерфейса. Передавать `EventBus` в `createWorld()`:

```java
public interface SimulationPlugin<T extends Mortal> {
    SimulationWorld<T, ?> createWorld(EventBus eventBus); // EventBus известен сразу
    void registerTasks(GameLoop<T> loop, SimulationWorld<T, ?> world, EventBus eventBus);
}
```

### [MEDIUM] `Organism.components` — ConcurrentHashMap

```java
private final Map<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();
```

Однострочное изменение. Снимает race condition при параллельном чтении компонентов.

### [MEDIUM] Написать тест для двойной подписки

```java
@Test
void eventBusShouldNotDoubleCountOnSubscribe() {
    EventBus bus = new DefaultEventBus();
    AtomicInteger count = new AtomicInteger();
    bus.subscribe(EntityDiedEvent.class, e -> count.incrementAndGet());
    bus.publish(new EntityDiedEvent(mockMortal, "TEST"));
    assertEquals(1, count.get());
}
```

### [LOW] Типизировать события по домену

```java
// Вместо EntityBornEvent(Mortal)
public class AnimalBornEvent {
    private final Animal animal; // конкретный тип, без instanceof в подписчике
}
```

---

## 🔧 Рефакторинг: устранение двойного репортинга

### До (v4 — два пути смерти):

```java
// FeedingService — путь 1: прямой publish
a.die(DeathCause.EATEN);
if (node.removeEntity(a)) {
    eventBus.publish(new EntityDiedEvent(a, DeathCause.EATEN.name())); // дубль!
}
// Cell.removeEntity → Island.onEntityRemoved → publish(EntityDiedEvent("EATEN")) // дубль!

// LifecycleService — путь 1: publish до очистки
if (!a.tryConsumeEnergy(metabolism)) {
    eventBus.publish(new EntityDiedEvent(a, DeathCause.HUNGER.name())); // первый
}
// CleanupService → removeEntity → Island.onEntityRemoved → publish HUNGER снова // второй
```

### После (единый путь через Island.onEntityRemoved):

```java
// FeedingService — только убить + удалить, без прямого publish
a.die(DeathCause.EATEN);
if (node.removeEntity(a)) {
    // removeEntity → Cell → WorldListener → Island.onEntityRemoved → EventBus → Statistics
    // Один путь. Один счёт.
    consumer.addEnergy(a.getWeight());
    animalFactory.releaseAnimal(a);
}

// LifecycleService — только убить, без publish
if (!a.tryConsumeEnergy(metabolism)) {
    // a.die(HUNGER) уже вызван внутри tryConsumeEnergy
    // CleanupService уберёт тело → Island.onEntityRemoved → EventBus
}
// Убрать: eventBus.publish(new EntityDiedEvent(a, DeathCause.HUNGER.name()));

// Island.onEntityRemoved — единственный источник истины
public void onEntityRemoved(Organism entity) {
    if (entity instanceof Animal a && eventBus != null) {
        DeathCause cause = a.getLastDeathCause();
        eventBus.publish(new EntityDiedEvent(a, cause != null ? cause.name() : "UNKNOWN"));
    }
}
```

**Важный нюанс:** После смерти от голода (`LifecycleService`) тело остаётся в ячейке до следующего тика `CleanupService`. Это **окно**, когда мёртвое животное может быть «съедено» повторно (FeedingService видит его в ячейке до очистки). Нужно убедиться, что `isAlive()` проверяется перед любым взаимодействием — это уже делается во всех сервисах.

---

## 📊 Итоговая оценка v4

| Критерий | v1 | v2 | v3 (PR20) | v4 | Δ |
|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 7.5 | 8.0 | **8.0** | → EventBus + ECS идейно верны, но двойной репортинг — регрессия |
| **Код** | 7.0 | 7.5 | 8.0 | **7.5** | ↓ Новый баг перевешивает улучшения (GridUtils, SamplingContext) |
| **Переиспользуемость** | 6.0 | 7.0 | 7.5 | **8.0** | ↑ GridUtils, EventBus, ECS в engine без доменных зависимостей |
| **Тестируемость** | 5.0 | 6.0 | 7.0 | **7.5** | ↑ ConfigurationReflectionTest, ArchitectureEvolutionTest, RefactoringVerificationTest |
| **Общая** | 6.5 | 7.5 | 8.0 | **7.5** | ↓ Критический баг в статистике снижает оценку |

---

## Вердикт

**Не готов к merge в `dev` без исправления критического бага.**

Версия демонстрирует уверенное архитектурное мышление: EventBus, ECS-компоненты, `GridUtils`, `SamplingContext`, константы приоритетов — всё это правильные шаги. Тестовое покрытие выросло в качестве (не только интеграция, но и архитектурные сценарии).

Однако введён один **критический баг**: смерти считаются дважды из-за конфликта двух параллельных путей репортинга — прямых `eventBus.publish()` в сервисах и `Island.onEntityRemoved()` через `WorldListener`. Это делает данные `StatisticsService`, `SurvivalCalibrationTest` и `ExtinctionBalanceTest` недостоверными.

**До merge нужно:**
1. Устранить двойную отчётность — выбрать один путь (рекомендуется `Island.onEntityRemoved`)
2. Защитить `DefaultEventBus.publish()` от исключений подписчиков
3. `Organism.components` → `ConcurrentHashMap`

После этих трёх правок — **готовность к продакшн-движку: 85%**. Оставшиеся 15% это `SpeciesKey`-синглтон и полноценный `System`-слой в ECS.
