# Code Review v6: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-06  
**База:** v5 (dev)  
**Дельта:** 52 изменённых файла, +68 новых, -47 удалённых  
**Ключевые изменения:** реорганизация пакетов engine, полный ECS System-слой,
`ComponentStore`/`ComponentRegistry`, `HealthSystem`/`MovementSystem`,
ArchUnit, `SimulationStopConditionTest`, `StatisticsDeathCountingTest`

---

## Сводная таблица: прогресс относительно v5

| Проблема из v5 | Статус | Детали |
|---|---|---|
| Двойная отчётность смертей (все 3 случая) | ✅ **ИСПРАВЛЕНО** | `StatisticsDeathCountingTest` на 5 сценариев |
| `shouldStop()` хук не вызывался | ✅ **ИСПРАВЛЕНО** | `GameLoop.setStopCondition()` + тест |
| ECS без System-слоя | ✅ **ИСПРАВЛЕНО** | `EntitySystem`, `NatureEntitySystem`, `HealthSystem`, `MovementSystem` |
| `ComponentStore` в `Organism` без интерфейса | ✅ **ИСПРАВЛЕНО** | `ComponentStore`, `DefaultComponentStore`, `ArrayComponentStore` |
| Нет ArchUnit теста | ✅ **ИСПРАВЛЕНО** | `ArchitectureTest.engineShouldNotDependOnDomainPackages` |
| Нет теста на stop condition | ✅ **ИСПРАВЛЕНО** | `SimulationStopConditionTest` |
| `SpeciesKey` — глобальный синглтон | ✅ **ИСПРАВЛЕНО** | `SpeciesRegistry` как instance-объект |
| `ComponentRegistry` — новый глобальный синглтон | 🔴 **НОВЫЙ БАГ** | `static ConcurrentHashMap` заменил `SpeciesKey` |
| `ArrayComponentStore` — молчаливый overflow | 🔴 **НОВЫЙ БАГ** | hardcoded 32, silent drop при index ≥ 32 |
| `EntitySystem.process(T, int)` — мёртвый метод | 🟡 **НОВЫЙ ДОЛГ** | `final` заглушка в `NatureEntitySystem` |
| `GrowthComponent`/`MetabolismComponent` — пустые маркеры | 🟡 **НОВЫЙ ДОЛГ** | Нет данных, системы не используют |
| `instanceof` в `HealthSystem`, `MovementSystem` | 🟡 **ПРИЗНАН, НЕ ИСПРАВЛЕН** | TODO-комментарии без решения |
| `instanceof` в 8 других местах | 🟡 **НЕ ЗАТРОНУТО** | Требует паттернов — разбор ниже |

---

## ✅ Что сделано хорошо

### 1. Реорганизация пакетов — правильное разделение

```
engine/
  core/        — SimulationWorld, SimulationNode, SimulationEngine, SimulationPlugin
  ecs/         — Entity, Component, ComponentStore, EntitySystem, EntityQuery
  model/       — Mortal, Tickable, WorldSnapshot, NodeSnapshot
  parallel/    — ParallelDispatcher, ParallelTask
  scheduling/  — GameLoop, PhaseScheduler, ScheduledTask, Phase
  service/     — CellService
  event/       — EventBus, DefaultEventBus, ...

nature/entities/
  core/        — Organism, Animal, Biomass, SpeciesKey, DeathCause, ...
  components/  — HealthComponent, AgeComponent, MovementComponent, ...
  domain/      — NatureWorld, NatureDomainContext, TaskRegistry, ...
  environment/ — Season, SeasonManager
  registry/    — SpeciesRegistry, SpeciesLoader, AnimalFactory, ...
  strategy/    — HuntingStrategy, DefaultHuntingStrategy, PreyProvider

util/
  common/      — RandomProvider, ObjectPool, ...
  interaction/ — InteractionMatrix, InteractionProvider
  math/        — GridUtils
  sampling/    — SamplingUtils, SamplingContext
```

Структура стала читаемой. Новый разработчик понимает назначение пакета по имени без
открытия файлов. `engine.ecs` — изолированная подсистема. `nature.entities.strategy` — чётко
выделенные стратегии. Это значительный прогресс по информационной архитектуре.

---

### 2. ECS System-слой — задача из Sprint 2 закрыта

```java
// engine/ecs/EntitySystem<T> — интерфейс в движке, без domain-зависимостей
public interface EntitySystem<T extends Mortal> extends CellService<T> {
    List<Class<? extends Component>> requiredComponents();
    void process(T entity, int tickCount);
}

// nature/service/NatureEntitySystem — domain-специфичная база
public abstract class NatureEntitySystem extends AbstractService
    implements EntitySystem<Organism> {
    protected void doProcessCell(Cell cell, int tickCount) {
        cell.query(entityQuery, entity -> process(entity, cell, tickCount));
    }
    protected abstract void process(Organism entity, Cell cell, int tickCount);
}

// HealthSystem, MovementSystem — конкретные системы
```

Цепочка `EntitySystem → NatureEntitySystem → HealthSystem` корректна. `Cell.query(EntityQuery)`
фильтрует сущности по компонентам — именно так должен работать ECS-запрос. ✅

---

### 3. ComponentStore с двумя реализациями

```java
// DefaultComponentStore — ConcurrentHashMap, безопасный дефолт
// ArrayComponentStore — массив для горячего пути, O(1) доступ без boxing
```

Абстракция `ComponentStore` + две реализации — правильная структура. `Organism`
делегирует к `componentStore` и дополнительно кэширует `healthComponent`/`ageComponent`
в прямых полях для горячего пути. Это паттерн inline cache — обоснован для компонентов,
обращаемых в каждом тике.

---

### 4. StatisticsDeathCountingTest — полное покрытие критического бага

5 тест-кейсов: EATEN, EATEN_BY_PACK, MOVEMENT_EXHAUSTION, REPRODUCTION_EXHAUSTION,
и отдельный тест на `CleanupService` + `movementServiceShouldNotDoubleReportDeath`.
Это именно тот уровень покрытия, который нужен для критического бага. ✅

---

### 5. ArchUnit — engine изолирован автоматически

```java
@Test
void engineShouldNotDependOnDomainPackages() {
    noClasses().that().resideInAPackage("..engine..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..nature..", "..simcity..");
}
```

Теперь любое нарушение слоёв в engine сломает CI. Это постоянная защита, не зависящая
от внимания ревьюера. ✅ Рекомендуется расширить (см. раздел «Улучшения»).

---

## 🔴 Критические новые проблемы

### Баг 1 [HIGH]: `ComponentRegistry` — снова глобальный синглтон

Только что исправили `SpeciesKey`-синглтон — теперь появился новый в том же месте:

```java
// engine/ecs/ComponentRegistry.java
public final class ComponentRegistry {
    private static final Map<Class<? extends Component>, Integer> indices
        = new ConcurrentHashMap<>();  // ← СТАТИЧЕСКОЕ СОСТОЯНИЕ
    private static final AtomicInteger nextIndex = new AtomicInteger(0);

    public static int getIndex(Class<? extends Component> type) {
        return indices.computeIfAbsent(type, k -> nextIndex.getAndIncrement());
    }
}
```

`ComponentRegistry` разделяет состояние между всеми экземплярами `ArrayComponentStore`
во всех симуляциях в JVM. Это не просто архитектурный запах — это конкретная проблема:

- Тест 1 добавляет `HealthComponent` → index=0
- Тест 2 запускается, создаёт новый `ArrayComponentStore`, пытается добавить
  `MovementComponent` → index=1 (но тест ожидал 0)
- `nextIndex` никогда не сбрасывается между тестами

Кроме того, `ArrayComponentStore` создаётся с `new Component[32]` на основе текущего
`ComponentRegistry.getMaxIndex()` **на момент создания**. Если регистрация компонентов
происходит после создания хранилища — индексы выйдут за пределы массива.

**Правильное решение: передавать `ComponentRegistry` как зависимость:**

```java
// Не статический — instance с жизненным циклом симуляции
public final class ComponentRegistry {
    private final Map<Class<? extends Component>, Integer> indices = new HashMap<>();
    private int nextIndex = 0;

    public int getOrRegister(Class<? extends Component> type) {
        return indices.computeIfAbsent(type, k -> nextIndex++);
    }
    public int size() { return nextIndex; }
}

// ArrayComponentStore получает registry как зависимость
public class ArrayComponentStore implements ComponentStore {
    private final ComponentRegistry registry;
    private Component[] components;

    public ArrayComponentStore(ComponentRegistry registry) {
        this.registry = registry;
        this.components = new Component[Math.max(registry.size(), 8)];
    }
    // при add(): если index >= components.length → Arrays.copyOf(components, index * 2)
}

// NatureDomainContext/NaturePlugin создают один ComponentRegistry и передают его везде
```

---

### Баг 2 [MEDIUM]: `ArrayComponentStore` — молчаливое игнорирование overflow

```java
// ArrayComponentStore.add()
int index = ComponentRegistry.getIndex(component.getClass());
if (index < components.length) {
    components[index] = component;
}
// Если index >= 32 — компонент молча НЕ добавляется
```

При добавлении 33-го типа компонента (или при изменении порядка регистрации через
глобальный синглтон) компонент теряется без исключения. `has()` вернёт `false`,
`get()` вернёт `null`. Система, которая требует этот компонент, либо получит NPE,
либо тихо пропустит обработку сущности.

**Fix:** добавить grow-логику или `IllegalStateException`:

```java
public <C extends Component> void add(C component) {
    int index = registry.getOrRegister(component.getClass());
    if (index >= components.length) {
        components = Arrays.copyOf(components, Math.max(index + 1, components.length * 2));
    }
    components[index] = component;
}
```

---

## 🟡 Архитектурные замечания

### 1. `EntitySystem.process(T, int)` — мёртвый метод интерфейса

```java
// EntitySystem.java — публичный контракт
void process(T entity, int tickCount);

// NatureEntitySystem — заглушка, намеренно мёртвая
@Override
public final void process(Organism entity, int tickCount) {
    // Default implementation from EntitySystem, not used by doProcessCell anymore
}
```

`NatureEntitySystem.doProcessCell()` вызывает `process(entity, cell, tickCount)` — с `Cell`.
Но `EntitySystem` объявляет `process(T, int)` — без `Cell`. Обе сигнатуры существуют
одновременно. Пользователь `EntitySystem`-интерфейса не может предсказать, какой метод
реально выполняется. Это нарушение принципа наименьшего удивления.

**Корень:** при переходе от `CellService` к `EntitySystem` появилась необходимость
передавать `Cell` в `process()`, но интерфейс этого не позволяет. Есть два пути:

```java
// Путь A: параметризовать контекст обработки в интерфейсе
public interface EntitySystem<T extends Mortal, CTX> extends CellService<T> {
    void process(T entity, CTX context, int tickCount);
}
// NatureEntitySystem<Organism, Cell>: process(organism, cell, tick)

// Путь B: убрать process() из EntitySystem, оставить только requiredComponents()
// Тогда конкретная система сама решает, как итерировать
public interface EntitySystem<T extends Mortal> {
    List<Class<? extends Component>> requiredComponents();
    // processCell() наследуется от CellService и реализуется конкретно
}
```

**Путь B** проще и честнее — он не навязывает сигнатуру, несовместимую с реальностью.

---

### 2. `GrowthComponent` и `MetabolismComponent` — пустые маркеры без семантики

```java
public class GrowthComponent implements Component { }  // нет полей
public class MetabolismComponent implements Component { } // нет полей
```

Компонент в ECS несёт **данные**, а система их **обрабатывает**. Пустой маркер — это
тег, не компонент. `HealthSystem` не использует `MetabolismComponent` ни в каком запросе.
`MovementSystem` явно проверяет `getComponent(MovementComponent.class)` (с данными скорости),
но `GrowthComponent` для роста биомассы не используется — `Biomass.grow()` вызывается
напрямую.

**Решение:** либо добавить данные в маркеры, либо удалить их:

```java
// MetabolismComponent — с реальными данными
public class MetabolismComponent implements Component {
    private volatile long basalMetabolicRate;  // перенести из Configuration
    private volatile double seasonModifier = 1.0;
}

// GrowthComponent — с данными роста
public class GrowthComponent implements Component {
    private volatile long growthRateBP;     // перенести из Configuration/BiomassType
    private volatile long maxBiomass;
}
```

Это разгрузит `Configuration` от биологических параметров и сделает систему
настоящим ECS — данные в компонентах, логика в системах.

---

## 🔍 Детальный анализ `instanceof` — паттерны и решения

В кодовой базе 18 мест с `instanceof`. Они делятся на **пять семантически разных категорий**,
каждая требует своего паттерна. Разберём каждую.

---

### Категория 1: Диспетчеризация по типу сущности в System (OCP-нарушение)

**Где:** `HealthSystem.process()`, `MovementSystem.process()`

```java
// HealthSystem.java:39 — TODO-комментарий автора
if (entity instanceof Animal animal) {
    processAnimal(animal);
} else if (entity instanceof Biomass biomass) {
    processBiomass(biomass, cell);
}
```

**Проблема:** каждый новый тип сущности (`NPC`, `Robot`, `Vehicle`) требует нового `else if`.
Это классическое нарушение OCP — система закрыта для расширения без модификации.

**Паттерн: Visitor (Double Dispatch)**

```java
// engine/ecs/EntityVisitor.java — в engine-пакете
public interface EntityVisitor<T extends Mortal> {
    void visit(T entity, SimulationNode<T> node, int tickCount);
}

// Organism реализует acceptVisitor через ECS-компоненты:
// Не Visitor в классическом виде — скорее Component-based dispatch

// Правильное решение для ECS: разделить системы по типу сущности
// Автор уже написал TODO: это точный ответ.
```

**Конкретный план:**

```java
// Вместо одной HealthSystem с instanceof:
public class AnimalHealthSystem extends NatureEntitySystem {
    @Override
    public List<Class<? extends Component>> requiredComponents() {
        return List.of(HealthComponent.class, AgeComponent.class, MetabolismComponent.class);
    }
    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        processAnimal((Animal) entity); // безопасный каст — гарантирован requiredComponents
    }
}

public class BiomassGrowthSystem extends NatureEntitySystem {
    @Override
    public List<Class<? extends Component>> requiredComponents() {
        return List.of(GrowthComponent.class);
    }
    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        processBiomass((Biomass) entity, cell); // аналогично
    }
}
```

`requiredComponents()` теперь является **типовым фильтром**: система сама объявляет,
с какими сущностями она работает. Каст становится безопасным — `EntityQuery.matches()`
гарантирует наличие компонентов, присущих только данному типу. Никакого `instanceof`.

---

### Категория 2: `node instanceof Cell cell` — знание о конкретном типе узла

**Где:** `AbstractService.tick()` (строка 64), `MovementSystem.processBiomass()` (строка 113),
`Island.getNode()` (строка 148), `Island.moveEntity()` (строка 167, 186),
`PreyProvider` (строки 40, 63), `Butterfly/Caterpillar.eat()` (строки 26, 44, 26)

```java
// AbstractService.tick()
if (node instanceof Cell cell) {
    doProcessCell(cell, tickCount);
}

// Island.getNode()
if (current instanceof Cell cell) { ... }
```

**Проблема:** `AbstractService` (и другие) принимают `SimulationNode<Organism>`, но реально
работают только с `Cell`. Если придёт другой `SimulationNode` — поведение молча не выполнится.

**Паттерн: Narrowing через Generic Bound + Template Method**

```java
// AbstractService становится типизированным по типу узла
public abstract class AbstractService
    extends CellServiceBase<Organism, Cell>  // явно: работаем с Cell
    implements CellService<Organism> {

    // Родительский CellService.processCell(SimulationNode) делегирует:
    @Override
    public final void processCell(SimulationNode<Organism> node, int tickCount) {
        if (node instanceof Cell cell) {
            doProcessCell(cell, tickCount); // один instanceof — в одном месте
        } else {
            log.warn("AbstractService received non-Cell node: {}", node.getClass());
        }
    }

    // Подклассы переопределяют только type-safe версию:
    protected abstract void doProcessCell(Cell cell, int tickCount);
}
```

**Принцип:** один `instanceof` в одной точке входа — это приемлемо. Размножение
`instanceof Cell` по 7 местам — не приемлемо. Все семь должны стать одним.

Для `Island.moveEntity()` и `Island.getNode()`: `Island` знает о `Cell` — это нормально.
`Island` — конкретная реализация `SimulationWorld<Organism>`, она владеет сеткой `Cell[][]`.
Этот `instanceof` **допустим** как внутренний инвариант реализации, но должен быть
задокументирован.

---

### Категория 3: Диспетчеризация в EventBus-обработчиках

**Где:** `Island.onEntityAdded/Removed` (строка 67, 74),
`StatisticsService.subscribe()` (строки 48, 53), `AlertService.subscribe()` (строка 20)

```java
// Island.onEntityRemoved
if (entity instanceof Animal a && eventBus != null) {
    eventBus.publish(new EntityDiedEvent(a, ...));
}
// Биомасса умирает молча — не публикуется событие

// StatisticsService
if (event.getEntity() instanceof Animal a) {
    registerBirth(a.getSpeciesKey());
}
```

**Проблема:** `Mortal`/`Organism` — слишком широкий тип для EventBus. Система фильтрует
постфактум вместо того, чтобы публиковать правильно типизированные события.

**Паттерн: Typed Events (специализированные классы событий)**

```java
// Вместо EntityDiedEvent(Mortal entity, String cause):
public class AnimalDiedEvent {
    private final Animal animal;
    private final DeathCause cause;
}
public class BiomassDiedEvent {
    private final Biomass biomass;
}

// Island.onEntityRemoved:
if (entity instanceof Animal a) {
    eventBus.publish(new AnimalDiedEvent(a, a.getLastDeathCause()));
} else if (entity instanceof Biomass b) {
    eventBus.publish(new BiomassDiedEvent(b)); // если нужно
}

// StatisticsService.subscribe() — без instanceof:
bus.subscribe(AnimalDiedEvent.class, event -> {
    registerDeath(event.getAnimal().getSpeciesKey(), event.getCause());
});
// Нет instanceof — тип известен из подписки
```

Этот же паттерн убирает `instanceof` из `AlertService` и `StatisticsService`.

---

### Категория 4: Диспетчеризация в FeedingService по типу добычи

**Где:** `FeedingService.java` (строки 112, 166, 186)

```java
if (preyCandidate instanceof Animal aCandidate) { ... }
else if (preyCandidate instanceof Biomass b && b.getBiomass() > 0) { ... }
```

**Проблема:** `FeedingService` знает о всех подтипах добычи.
Добавление нового типа (гриб, паразит, падаль) — изменение в `FeedingService`.

**Паттерн: Strategy через компонент «Потребляемость»**

```java
// Компонент — данные о потребляемости
public class ConsumableComponent implements Component {
    private final long energyValue;    // сколько энергии при поедании
    private final boolean isAnimal;    // влияет ли на счётчик популяции
    private final Supplier<Long> currentAvailable; // текущий доступный объём
}

// FeedingService — без instanceof:
ConsumableComponent consumable = prey.getComponent(ConsumableComponent.class);
if (consumable != null && consumable.getCurrentAvailable().get() > 0) {
    long energy = Math.min(consumable.getEnergyValue(), consumer.getEnergyNeeded());
    consumer.addEnergy(energy);
    consumable.consume(energy); // уменьшает доступный объём
}
```

Каждый тип добычи сам объявляет как его потреблять через `ConsumableComponent`.
`FeedingService` работает с любым `Organism`, у которого есть этот компонент.

---

### Категория 5: Сущность знает о своём контейнере (Biomass.grow, SwarmOrganism)

**Где:** `Biomass.grow()` (строка 78), `SwarmOrganism` (строка 100),
`Butterfly.eat()` (строка 26), `Caterpillar.eat()` (строка 26)

```java
// Biomass.grow()
if (delta != 0 && node.getWorld() instanceof NatureWorld nw) {
    nw.onOrganismGrown(this, delta);
}
```

**Проблема:** `Biomass` знает о `NatureWorld` — конкретном интерфейсе мира.
Сущность тянется к миру за обратной связью — это инверсия зависимостей.

**Паттерн: Observer через EventBus (уже внедрённый)**

```java
// Biomass.grow() — публикует событие, не знает о NatureWorld
public void grow(Cell cell, double modifier) {
    long delta = calculateGrowth(modifier);
    if (delta != 0) {
        this.biomass += delta;
        // Не знаем о NatureWorld — публикуем событие
        // EventBus не нужен у Biomass: CleanupService/Island обработает при следующем тике
        // Или: Biomass возвращает delta, вызывающий код (HealthSystem/BiomassGrowthSystem)
        //       сам уведомляет мир
    }
}

// BiomassGrowthSystem.process():
long delta = biomass.grow(modifier);
if (delta != 0) {
    world.onBiomassChanged(biomass, delta); // мир уведомляется системой, не биомассой
}
```

`Butterfly.eat()` и `Caterpillar.eat()` — аналогично: поведение питания должно быть
в `FeedingService`/`EatingSystem`, а не в сущности. Сущность — данные, система — логика.

---

### Итоговая карта instanceof → паттерн

| Место | Тип | Паттерн | Приоритет |
|---|---|---|---|
| `HealthSystem`, `MovementSystem` | Диспетч. по типу сущности | **Split Systems** (один системный класс = один тип сущности) | HIGH |
| `AbstractService.tick()` | Narrowing узла | **Template Method** (один `instanceof` в базе) | MEDIUM |
| `Island.onEntityRemoved/Added` | Тип события | **Typed Events** (`AnimalDiedEvent` vs `BiomassDiedEvent`) | MEDIUM |
| `StatisticsService`, `AlertService` | Фильтр в подписчике | **Typed Events** → подписка на конкретный тип | MEDIUM |
| `FeedingService` | Тип добычи | **ConsumableComponent** (Strategy через ECS) | MEDIUM |
| `Biomass.grow()`, `SwarmOrganism` | Сущность → мир | **Observer/EventBus** (система уведомляет мир) | LOW |
| `Island.moveEntity()`, `getNode()` | Внутренний инвариант | **Документировать как ограничение Island** | LOW |
| `Butterfly/Caterpillar.eat()` | Поведение в сущности | **EatingSystem** (переместить в систему) | LOW |

---

## 📊 Оценка архитектурного прогресса: v1 → v6

### Динамика по версиям

| Критерий | v1 | v2 | v3 | v4 | v5 | v6 |
|---|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 7.5 | 8.0 | 8.0 | 8.5 | **8.5** |
| **Код** | 7.0 | 7.5 | 8.0 | 7.5 | 8.0 | **8.0** |
| **Переиспользуемость** | 6.0 | 7.0 | 7.5 | 8.0 | 8.5 | **9.0** |
| **Тестируемость** | 5.0 | 6.0 | 7.0 | 7.5 | 8.0 | **8.5** |
| **Общая** | 6.5 | 7.5 | 8.0 | 7.5 | 8.5 | **8.5** |

*Общая оценка не растёт с v5 → v6 из-за двух новых критических проблем
(`ComponentRegistry` синглтон, `ArrayComponentStore` overflow), которые компенсируют
значительный прогресс по ECS и структуре.*

---

### Правильно ли развивается проект? Оценка вектора

**Да, вектор правильный.** Проект последовательно движется от монолитной симуляции
к переиспользуемому движку. Каждая итерация закрывает конкретные проблемы из ревью.

**Что идёт хорошо:**
- Архитектурные решения обоснованы (ECS, EventBus, Plugin, PhaseScheduler)
- Технический долг не накапливается бесконтрольно — большинство TODO закрываются в следующем спринте
- Тестовое покрытие растёт качественно: не только smoke, но и контрактные тесты, ArchUnit, ECS-юниты
- Рефакторинг происходит без деградации работающего функционала
- Автор понимает проблемы (TODO в `HealthSystem`/`MovementSystem`) — нужно их закрыть

**Что требует внимания:**
- Новые синглтоны появляются при исправлении старых (`SpeciesKey` → `ComponentRegistry`)
- Маркерные компоненты без данных — признак незавершённой ECS-миграции
- `instanceof` в ключевых местах задокументированы как TODO, но решение пока не сформулировано

---

## 💡 Приоритетные улучшения

### [HIGH] Перевести ComponentRegistry в instance с жизненным циклом симуляции

```java
// Регистрация при старте, стабильные индексы на всё время симуляции
ComponentRegistry registry = new ComponentRegistry();
registry.register(HealthComponent.class);
registry.register(AgeComponent.class);
registry.register(MovementComponent.class);
// ...передаётся в NatureDomainContext и далее в ArrayComponentStore
```

### [HIGH] ArrayComponentStore — grow вместо silent drop

```java
if (index >= components.length) {
    components = Arrays.copyOf(components, index * 2 + 1);
}
components[index] = component;
```

### [HIGH] Split Systems: `AnimalHealthSystem` + `BiomassGrowthSystem`

Закрыть TODO из `HealthSystem` и `MovementSystem`. Это 20-30 строк кода каждый.

### [MEDIUM] Typed Events: `AnimalDiedEvent`, `AnimalBornEvent`

Убрать `instanceof` из `StatisticsService`, `AlertService`, `Island.onEntityRemoved`.

### [MEDIUM] `EntitySystem.process(T, int)` — убрать или переосмыслить

Либо убрать из интерфейса, либо добавить параметр контекста `CTX`.

### [MEDIUM] Расширить ArchUnit

```java
@Test void utilShouldNotDependOnNature() {
    noClasses().that().resideInAPackage("..util..")
        .should().dependOnClassesThat().resideInAPackage("..nature..");
}

@Test void componentsShouldBeDataOnly() {
    // Компоненты не должны иметь методов с побочными эффектами
    noMethods().that().areDeclaredInClassesThat()
        .implement(Component.class)
        .and().areNotConstructors()
        .and().areNotGetters()
        .and().areNotSetters()
        .should().haveRawReturnType(Void.class);
}
```

### [LOW] `GrowthComponent`/`MetabolismComponent` — добавить данные

Перенести `growthRateBP`, `maxBiomass` из `Configuration`/`AnimalType` в компоненты.

### [LOW] `SimulationStopConditionTest` — улучшить

```java
// Текущий тест: gameLoop.setStopCondition(() -> true) — не тестирует реальное условие
// Нужен тест: мир заполняется животными → все умирают → NaturePlugin.shouldStop()=true
@Test
void natureShouldStopWhenExtinct() {
    // 1 animal, starve to death within 50 ticks
    // Assert simulation stopped via NaturePlugin.shouldStop()
}
```
