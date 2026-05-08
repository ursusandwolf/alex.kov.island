# Code Review v7: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-06  
**База:** v6  
**Дельта:** 68 изменённых файлов, +31 новый, -8 удалённых  
**Ключевые изменения:** Split Systems (6 ECS-систем), ConsumableComponent, TypedEvents,
ClimateService, SystemExecutionGraph, EntityArchetype, DynamicChunkingStrategy,
WorkUnit с таймингом, AbstractSimCityService

---

## Сводная таблица: прогресс относительно v6

| Проблема | Статус | Детали |
|---|---|---|
| `ComponentRegistry` — глобальный синглтон | ✅ **ИСПРАВЛЕНО** | Instance-based, передаётся через `NatureDomainContext` |
| `ArrayComponentStore` — silent overflow | ✅ **ИСПРАВЛЕНО** | `ensureCapacity()` с `Arrays.copyOf` |
| Split Systems (HealthSystem/MovementSystem с instanceof) | ✅ **ИСПРАВЛЕНО** | 6 систем: Animal/Biomass разделены |
| `EntitySystem.process(T,int)` — мёртвый метод | ✅ **ИСПРАВЛЕНО** | Заменён `readComponents()`/`writeComponents()` |
| `GrowthComponent`/`MetabolismComponent` — пустые маркеры | ✅ **ИСПРАВЛЕНО** | Несут реальные данные |
| Typed Events в EventBus | ✅ **ИСПРАВЛЕНО** | `AnimalDiedEvent(Animal)`, `AnimalBornEvent(Animal)` |
| `StatisticsService` — `instanceof Animal` в подписчике | ✅ **ИСПРАВЛЕНО** | Подписка на `AnimalDiedEvent` без instanceof |
| `ConsumableComponent` — устраняет instanceof в кормлении | ✅ **ИСПРАВЛЕНО** | BiFunction-based strategy |
| Нет System-ориентированного конфликт-детектора | ✅ **НОВОЕ** | `SystemExecutionGraph` по read/write компонентам |
| Нет динамического балансирования чанков | ✅ **НОВОЕ** | `DynamicChunkingStrategy` (рекурсивное разбиение) |
| Нет ClimateService | ✅ **НОВОЕ** | `DefaultClimateService` с температурой и флуктуацией |
| `util/interaction` зависит от `nature` классов | ❌ **НЕ ИСПРАВЛЕНО** | `InteractionMatrix` импортирует `SpeciesKey`, `AnimalType` |
| Engine и плагины — один Maven-модуль | ❌ **НЕ ИСПРАВЛЕНО** | Нет multi-module структуры |
| SimCity — по-прежнему много `instanceof` | ⚠️ **ЧАСТИЧНО** | `AbstractSimCityService` добавлен, сервисы не мигрированы |
| `EntityBornEvent`/`EntityDiedEvent` в `engine` — дублируют `nature.event` | 🟡 **НОВЫЙ ДОЛГ** | Две параллельных иерархии событий |

---

## ✅ Детальный разбор ключевых улучшений

### 1. Split Systems — полная реализация рекомендации из v6

Шесть ECS-систем без единого `instanceof Animal/Biomass` в `process()`:

```
AnimalHealthSystem    (HealthComponent, AgeComponent, MetabolismComponent)
BiomassGrowthSystem   (GrowthComponent)
AnimalMovementSystem  (MovementComponent + MetabolismComponent)
BiomassMovementSystem (MovementComponent + GrowthComponent)
AnimalFeedingSystem   (HealthComponent + MetabolismComponent)
AnimalReproductionSystem (ReproductionComponent)
```

`requiredComponents()` играет роль типового фильтра: `BiomassGrowthSystem` объявляет
`GrowthComponent` — только `Biomass` его имеет. Каст `(Biomass) entity` в `process()`
**безопасен без instanceof**. Это именно то решение, которое было рекомендовано. ✅

---

### 2. ConsumableComponent — Strategy через компонент

```java
// ConsumableComponent — логика потребления инкапсулирована в BiFunction
ConsumableComponent.builder()
    .isAnimal(true)
    .consumeAction((requested, context) -> {
        if (animal.isAlive()) {
            animal.die(DeathCause.EATEN);
            return animal.getWeight();
        }
        return 0L;
    })
    .build()
```

`AnimalFeedingSystem` теперь не проверяет `instanceof Animal/Biomass` — он просто
вызывает `consumable.consume(amount, context)`. Добавление нового типа съедобного —
новый компонент с `consumeAction`, без изменений в системе. OCP соблюдён. ✅

**Одно замечание:** `BiFunction<Long, Object, Long>` — `Object context` не типобезопасен.
`NatureComponentFactory` передаёт `Cell` через `Object`, затем делает `instanceof Cell`.
Лучше создать функциональный интерфейс:

```java
@FunctionalInterface
public interface ConsumeAction<CTX> {
    long consume(long requested, CTX context);
}
// ConsumableComponent<CTX extends SimulationNode<?>> — тогда context типизирован
// Или: оставить BiFunction, но accept Cell как параметр типа
```

---

### 3. SystemExecutionGraph — автоматическое параллельное расписание

```java
// Проверка конфликта по read/write компонентам
private static boolean conflicts(ParallelTask<T> a, ParallelTask<T> b) {
    // A writes X, B reads/writes X → conflict
    for (Class<? extends Component> c : aWrite) {
        if (bRead.contains(c) || bWrite.contains(c)) return true;
    }
    // B writes X, A reads X → conflict
    for (Class<? extends Component> c : bWrite) {
        if (aRead.contains(c)) return true;
    }
    return false;
}
```

Это фундаментальный алгоритм ECS-шедулера — аналог того, что делает Unity DOTS Jobs.
Системы без конфликтов по компонентам могут выполняться в одном batch параллельно.
`AnimalMovementSystem` (читает MovementComponent, пишет MetabolismComponent) и
`BiomassGrowthSystem` (пишет GrowthComponent) — нет общих компонентов → один batch. ✅

**Замечание:** `SystemExecutionGraph` не интегрирован в `PhaseScheduler`. Он создан,
но не используется в `GameLoop`. Это dead code пока. Нужно подключить:

```java
// PhaseScheduler — использовать SystemExecutionGraph вместо ручной группировки
List<List<ParallelTask<T>>> schedule = SystemExecutionGraph.buildSchedule(parallelTasks);
for (List<ParallelTask<T>> batch : schedule) {
    dispatcher.dispatch(workUnits, batch);
}
```

---

### 4. WorkUnit с таймингом — задел на балансировку

```java
public interface WorkUnit<T extends Mortal> extends Collection<SimulationNode<T>> {
    void setLastExecutionTimeNanos(long nanos);
    long getLastExecutionTimeNanos();
}
```

`ParallelDispatcher` записывает время выполнения каждого `WorkUnit`. Это позволит
`DynamicChunkingStrategy` принимать решения о перебалансировке на основе реального
времени, а не только числа сущностей. Правильный задел для адаптивного LOD. ✅

---

### 5. ClimateService — погода из TODO-списка

```java
// Погода как ScheduledTask в фазе PREPARE (приоритет 100 — первым)
public interface ClimateService extends ScheduledTask {
    Season getCurrentSeason();
    int getTemperature();
    @Override default Phase phase() { return Phase.PREPARE; }
}
```

Температурная логика корректно вынесена из `AnimalHealthSystem` — система получает
температуру через `getEnvironment().getTemperature()`, а не хранит её сама. Отделение
климата от метаболизма — хорошее разделение ответственностей. ✅

---

### 6. AbstractSimCityService — паттерн Template Method для SimCity

```java
// Рекомендовался в v6 — реализован
public abstract class AbstractSimCityService implements CellService<SimEntity> {
    @Override
    public final void processCell(SimulationNode<SimEntity> node, int tickCount) {
        if (node instanceof CityTile tile) {    // ← один instanceof в базе
            doProcessTile(tile, tickCount);
        }
    }
    protected abstract void doProcessTile(CityTile tile, int tickCount);
}
```

Один `instanceof CityTile` в одном месте. Все SimCity-сервисы работают с типизированным
`CityTile`. Аналог `AbstractService` для nature-домена. ✅

---

## 🟡 Оставшиеся архитектурные проблемы

### 1. Дублирование event-иерархий: `engine.event` vs `nature.event`

```
engine.event.EntityBornEvent  (Mortal entity)     ← базовые, но неиспользуемые
engine.event.EntityDiedEvent  (Mortal entity, String cause)

nature.event.AnimalBornEvent  (Animal animal)     ← реально используемые
nature.event.AnimalDiedEvent  (Animal animal, DeathCause cause)
```

`Island.onEntityAdded()` публикует `AnimalBornEvent` — **не** `EntityBornEvent`.
`StatisticsService` подписывается на `AnimalBornEvent` — **не** `EntityBornEvent`.
Движковые события стали мёртвым кодом.

Вопрос: зачем они нужны в `engine`? Два варианта:
- **(A) Движок публикует базовые события, домен расширяет:**
  ```java
  // engine.event.EntityBornEvent — базовый
  // nature.event.AnimalBornEvent extends EntityBornEvent — специализированный
  // Island публикует AnimalBornEvent, подписчик Object.class → EventBus receives it as EntityBornEvent too
  ```
- **(B) Двиговые события удалить, события — только в домене:**
  Движок не знает о рождениях/смертях. EventBus — общий инфраструктурный инструмент.

Рекомендуется **(B)**: убрать `EntityBornEvent`/`EntityDiedEvent` из `engine.event`,
оставить только `EventBus`. Рождение и смерть — доменные концепции, не движковые.

---

### 2. `util/interaction` зависит от `nature` — нарушение границ

```java
// util/interaction/InteractionMatrix.java
import com.island.nature.entities.core.AnimalType;   // ← nature
import com.island.nature.entities.core.SpeciesKey;   // ← nature
import com.island.nature.entities.registry.SpeciesRegistry; // ← nature

// util/interaction/InteractionProvider.java
import com.island.nature.entities.core.SpeciesKey;   // ← nature
```

`util` должен быть независимым. `InteractionMatrix` — доменная логика `nature`, не утилита.

```
Правильное расположение:
  com.island.util.*             — чистые утилиты (GridUtils, SamplingUtils, RandomUtils)
  com.island.nature.model.*     — доменные структуры (InteractionMatrix, InteractionProvider)
```

Это блокирует выделение `engine` и `util` в отдельный JAR без природного домена.

---

### 3. SimCity — ECS не применён, много `instanceof` в сервисах

`AbstractSimCityService` добавлен, но сами сервисы не переработаны:

```java
// PopulationService.java — 6 мест с instanceof
if (entity instanceof Building building) { ... }
if (entity instanceof Resident resident) { ... }

// EconomyService.java — 2 места
if (entity instanceof Building b) { ... }
else if (entity instanceof Resident && tile.isConnected()) { ... }
```

SimCity не получил ECS-систем по образцу `nature`. Это допустимо для proof-of-concept,
но создаёт разрыв: природный домен — современный ECS, городской — старый стиль.

---

### 4. `ConsumableComponent.consumeAction` — `Object context` не типобезопасен

```java
// NatureComponentFactory.java:57
.consumeAction((requested, context) -> {
    if (context instanceof Cell cell) {   // ← instanceof внутри BiFunction!
        return biomass.consumeBiomass(requested, cell);
    }
    return 0L;
})
```

Паттерн устранил `instanceof` из `AnimalFeedingSystem`, но перенёс его внутрь лямбды
в `ConsumableComponent`. Instanceof никуда не делся — он просто скрыт глубже.

---

## 🔬 Главный вопрос: готов ли движок к standalone JAR?

### Что engine даёт плагину сегодня — API поверхность

```java
// Что плагин РЕАЛИЗУЕТ (SPI):
SimulationPlugin<T>       — createWorld(EventBus), registerTasks(), lifecycle hooks
SimulationWorld<T>        — пространство, чанки, перемещение
SimulationNode<T>         — узел, блокировка, эвенты
Entity                    — сущность с ComponentStore
EntitySystem<T>           — ECS-система с read/write компонентами

// Что плагин ИСПОЛЬЗУЕТ (API):
SimulationEngine<T>       — build(plugin, tick, threads)
SimulationContext<T>      — world(), gameLoop(), eventBus()
GameLoop<T>               — addRecurringTask(), setStopCondition()
EventBus                  — publish(), subscribe(), unsubscribe()
ComponentRegistry         — getOrRegister(), getBitSet()
ComponentStore            — add(), get(), has(), remove()
EntityQuery<T>            — matches()
SystemExecutionGraph      — buildSchedule() [не интегрирован]
WorkUnit<T>               — итерация + тайминг
Phase, ExecutionMode      — порядок выполнения
```

**Вывод: API поверхность богатая и достаточная.** Плагин может реализовать полноценную
симуляцию, используя только `engine` пакет + `util`. Концептуально движок готов.

---

### Что мешает выделить engine в отдельный JAR — конкретный список

#### Блокер 1 [CRITICAL]: Один Maven-модуль — один артефакт

```xml
<!-- Текущий pom.xml -->
<artifactId>island-simulator</artifactId>
<packaging>jar</packaging>
<!-- Один JAR содержит engine + nature + simcity -->
```

Нет способа зависеть только от движка. Подключение `island-simulator.jar` тянет
за собой весь природный домен, SimCity, Lombok, ArchUnit.

**Решение: Maven multi-module**

```
island-simulator/
├── pom.xml                    (parent, <packaging>pom</packaging>)
├── island-engine/             (движок — публичная библиотека)
│   ├── pom.xml
│   └── src/main/java/com/island/engine/
│       └── src/main/java/com/island/util/  (только clean utils)
├── island-nature/             (плагин природы)
│   ├── pom.xml
│   │   └── <dependency>island-engine</dependency>
│   └── src/main/java/com/island/nature/
├── island-simcity/            (плагин города)
│   ├── pom.xml
│   │   └── <dependency>island-engine</dependency>
│   └── src/main/java/com/island/simcity/
└── island-app/                (launcher — собирает всё)
    ├── pom.xml
    └── src/main/java/com/island/
        ├── NatureLauncher.java
        └── SimCityLauncher.java
```

`island-engine.jar` содержит только `engine.*` + `util.common.*` + `util.math.*` + `util.sampling.*`.

---

#### Блокер 2 [CRITICAL]: `util/interaction` нарушает границу engine-модуля

`InteractionMatrix` и `InteractionProvider` в `util.interaction` импортируют `nature` классы.
Если переместить `util` в `island-engine` (а нам нужно), эти два файла тянут за собой весь `nature`.

**Решение:** переместить `util/interaction/` в `island-nature/`:
```
com.island.nature.model.InteractionMatrix    (было: com.island.util.interaction)
com.island.nature.model.InteractionProvider  (было: com.island.util.interaction)
```

---

#### Блокер 3 [HIGH]: Нет `module-info.java` — движок невидим как Java-модуль

Без `module-info.java` нет гарантии изоляции, нет явного API/SPI контракта,
нет защиты от доступа к внутренним классам движка из плагина.

```java
// island-engine/src/main/java/module-info.java
module com.island.engine {
    requires lombok;          // annotation processor
    requires slf4j.api;

    // Публичное API — то, что плагины используют
    exports com.island.engine.core;
    exports com.island.engine.ecs;
    exports com.island.engine.event;
    exports com.island.engine.model;
    exports com.island.engine.scheduling;
    exports com.island.engine.service;
    exports com.island.util.common;
    exports com.island.util.math;
    exports com.island.util.sampling;

    // Внутренние — плагины не трогают
    // com.island.engine.parallel — НЕ экспортируется
}
```

`ParallelDispatcher`, `PhaseScheduler`, `CellProcessor` — это implementation details движка.
Плагин не должен зависеть от них напрямую.

---

#### Проблема 4 [MEDIUM]: Lombok — транзитивная зависимость для пользователей библиотеки

`engine` классы (`SimulationContext`, `SimulationPlugin`, `WorkUnit`) используют Lombok-аннотации
(`@Getter`, `@RequiredArgsConstructor`). Пользователь библиотеки получает Lombok как
транзитивную зависимость.

**Решение:** в `island-engine/pom.xml` Lombok только как `provided`/`optional`:
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>   <!-- annotation processor, не в итоговом JAR -->
    <optional>true</optional>
</dependency>
```

Lombok-генерированный байткод не требует lombok.jar в runtime. ✅

---

#### Проблема 5 [MEDIUM]: Нет версионирования API

Нет разметки что является публичным API (стабильным), что — внутренним (может меняться):

```java
// Рекомендуется: аннотации для API stability
@Retention(RetentionPolicy.SOURCE)
public @interface EngineAPI { }        // Публичный, стабильный контракт

@Retention(RetentionPolicy.SOURCE)
public @interface InternalEngine { }   // Внутренний, может меняться без notice

// Применение:
@EngineAPI public interface SimulationPlugin<T> { ... }
@EngineAPI public interface EventBus { ... }
@InternalEngine public class PhaseScheduler<T> { ... }
@InternalEngine public class ParallelDispatcher<T> { ... }
```

Без этого плагин-разработчик не знает от чего зависеть безопасно.

---

#### Проблема 6 [MEDIUM]: `EntityBornEvent`/`EntityDiedEvent` в `engine` — нарушают domain isolation

Движок, по определению, не знает о рождениях и смертях сущностей — это доменные концепции.
Публикация этих событий происходит в домене (`Island`), не в движке. Эти классы в `engine.event`
нарушают domain independence и должны быть удалены из engine-пакета.

---

#### Проблема 7 [LOW]: ArchUnit не проверяет `util` изоляцию

```java
// ArchitectureTest.java — только одно правило
void engineShouldNotDependOnDomainPackages() { ... }
```

Нарушение `util → nature` не детектируется. При выделении engine в JAR
это будет компиляционной ошибкой, но сейчас проходит незамеченным.

```java
// Добавить:
@Test
void utilShouldNotDependOnNature() {
    noClasses().that().resideInAPackage("..util..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..nature..", "..simcity..");
}
```

---

## 📋 Пошаговый план: от монолита к библиотеке

### Шаг 1 (1-2 дня): Перенести `util/interaction` в `nature`

```
com.island.util.interaction.InteractionMatrix    →  com.island.nature.model.InteractionMatrix
com.island.util.interaction.InteractionProvider  →  com.island.nature.model.InteractionProvider
```

Обновить все импорты. Добавить ArchUnit-правило для `util`.

### Шаг 2 (2-3 дня): Создать Maven multi-module структуру

Без изменения кода — только реорганизация `pom.xml`:
1. Создать `island-engine/pom.xml` с исходниками `engine.*` + `util.common.*` + `util.math.*` + `util.sampling.*`
2. Создать `island-nature/pom.xml` с зависимостью от `island-engine`
3. Создать `island-simcity/pom.xml` аналогично
4. Создать parent `pom.xml`

Убедиться что `mvn install island-engine` собирается без ошибок без природных классов.

### Шаг 3 (1 день): Удалить `EntityBornEvent`/`EntityDiedEvent` из `engine`

Заменить использование (если есть) на domain-специфичные события.

### Шаг 4 (2-3 дня): Добавить `module-info.java`

Описать exports и requires. Убедиться что `engine` не открывает internal пакеты.

### Шаг 5 (0.5 дня): Lombok → `provided` scope в `island-engine`

### Шаг 6 (1 день): Подключить `SystemExecutionGraph` в `PhaseScheduler`

Использовать граф конфликтов для автоматического формирования параллельных батчей
вместо текущего ручного управления через `priority()`.

---

## 📊 Итоговая оценка

| Критерий | v1 | v3 | v5 | v6 | v7 |
|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 8.0 | 8.5 | 8.5 | **9.0** |
| **Код** | 7.0 | 8.0 | 8.0 | 8.0 | **8.5** |
| **Переиспользуемость** | 6.0 | 7.5 | 8.5 | 9.0 | **9.0** |
| **Тестируемость** | 5.0 | 7.0 | 8.0 | 8.5 | **9.0** |
| **Готовность к JAR** | — | — | — | — | **6.5** |
| **Общая** | 6.5 | 8.0 | 8.5 | 8.5 | **9.0** |

**Готовность к standalone library JAR: 6.5/10**

Концептуально — готов. Технически — требует multi-module Maven, `module-info.java`,
перемещения `util/interaction` и удаления domain-событий из `engine`. Это 1-2 недели
работы, не требующие изменений в логике симуляции.

---

## Вердикт

**Движок концептуально готов к роли библиотеки.** `engine` пакет чист, не имеет
ни одного доменного импорта, предоставляет богатый API для плагинов (SPI+API).
Два независимых плагина (`nature`, `simcity`) доказывают реальную переиспользуемость.

**До production-ready JAR остаётся организационная работа, не архитектурная:**

| Задача | Сложность | Эффект |
|---|---|---|
| Maven multi-module | Средняя | Отдельные артефакты |
| `util/interaction` → `nature` | Низкая | Чистый `engine` JAR |
| `module-info.java` | Средняя | JPMS изоляция |
| Убрать engine events | Низкая | Чистота domain |
| `SystemExecutionGraph` интеграция | Низкая | Умный шедулер |
| Lombok → provided | Минимальная | Чистые транзитивные зависимости |

За 6 итераций проект прошёл путь **6.5 → 9.0**. Это стабильная восходящая кривая
без откатов (кроме v4→v5 из-за двойных смертей). Автор последовательно применяет
рекомендации и предвосхищает следующие шаги (SystemExecutionGraph готов до интеграции).
