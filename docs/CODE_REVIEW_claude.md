# Code Review: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-02  
**Scope:** Simulation engine quality, reusability, extensibility, production-readiness  
**Language:** Java 21, Maven, Lombok  
**Revision:** HEAD of `dev` branch  

---

## Предисловие

Проект представляет собой самописный движок 2D-симуляции с двумя плагинами: `nature` (экосистема острова) и `simcity` (городской симулятор). Анализируется прежде всего **движок** (`com.island.engine`) и его соответствие роли переиспользуемой основы. Плагины рассматриваются как примеры интеграции.

Ревью проведено на уровне продакшн-команды: автор — middle/senior разработчик, цель — подготовка проекта к роли универсального движка.

---

## 🔍 1. Общий обзор проекта

### Что моделируется

Движок реализует **пространственную клеточную симуляцию** с тик-шагом: сетка ячеек (`SimulationNode`), населённых агентами (`Mortal`), обрабатывается параллельными сервисами (`CellService`) через цикл (`GameLoop`). Домен `nature` — экосистема с хищниками, травоядными и биомассой. Домен `simcity` — городская экономика с зданиями и жителями.

### Привязка к сценарию «Остров»

**Хорошо:** Интерфейсный слой движка (`engine` пакет) действительно не знает об остров-специфичных концепциях. `SimulationWorld`, `SimulationNode`, `GameLoop`, `CellService`, `Mortal`, `Tickable` — все без доменных импортов.

**Проблемно:**
- `SimulationContext` (в пакете `engine`) импортирует `com.island.nature.view.SimulationView` — прямая утечка домена в ядро.
- `SimulationWorld.getConfiguration()` возвращает `Object` — это архитектурный запах: движок знает, что конфигурация существует, но не умеет с ней работать типобезопасно.
- `SpeciesKey` — глобальный синглтон с `ConcurrentHashMap`-реестром. Это инфраструктурный объект домена `nature`, но его архитектура делает невозможным запуск двух независимых симуляций с разными наборами видов в одной JVM.

### Переиспользуемость текущих абстракций

| Абстракция | Переиспользуема? | Комментарий |
|---|---|---|
| `GameLoop` | ✅ Да | Полностью generic, без доменных зависимостей |
| `SimulationWorld<T>` | ✅ Да | Чистый интерфейс |
| `SimulationNode<T>` | ✅ Да | Чистый интерфейс |
| `CellService<T,N>` | ✅ Да | Корректно параметризован |
| `SimulationContext` | ⚠️ Нет | Импортирует `SimulationView` из `nature` |
| `AbstractService` | ⚠️ Частично | Жёстко привязан к `NatureWorld`, `Configuration`, `Season` |
| `Configuration` | ❌ Нет | Содержит `islandWidth/Height`, `wolfPackMinSize` и т.д. |

### «Протекание» домена в технику

Три конкретных случая:
1. `SimulationContext` → `import com.island.nature.view.SimulationView` (в пакете `engine`)
2. `Cell.addAnimal()` → `world instanceof Island island` (нарушение LSP: `Cell` знает о конкретной реализации `SimulationWorld`)
3. `AbstractService.tick()` → `if (node instanceof Cell cell)` (fallback сервиса в тесте знает о конкретном типе ноды)

---

## 🏗 2. Архитектура и дизайн

### Архитектурный стиль

Проект реализует **слоистую архитектуру** с элементами **Plugin/Extension Model**:

```
┌─────────────────────────────────────────┐
│          App Layer (Launchers)          │
├────────────────┬────────────────────────┤
│ Nature Plugin  │    SimCity Plugin      │
│ (com.island.   │    (com.island.        │
│  nature)       │     simcity)           │
├────────────────┴────────────────────────┤
│         Engine (com.island.engine)      │
├─────────────────────────────────────────┤
│         Util (com.island.util)          │
└─────────────────────────────────────────┘
```

Архитектура в целом правильная и близка к цели. Документ `MODULAR_ARCHITECTURE.md` корректно описывает намерение. Проблема — реализация отстаёт от намерения в нескольких критических точках.

### Соответствие уровню «движка»

Движок близок к «движку», но не дотягивает по трём причинам:

1. **Нет механизма регистрации плагинов** — `SimulationBootstrap` и `TaskRegistry` жёстко знают о `FeedingService`, `MovementService` и т.д. Нет абстракции `SimulationPlugin` / `WorldPlugin`.
2. **Нет типобезопасной конфигурации** — `getConfiguration()` возвращает `Object`. Движок не может корректно передать конфиг плагину.
3. **Нет event bus / lifecycle hooks** — плагины не могут подписаться на события движка без прямых зависимостей.

### SOLID — анализ по принципам

#### Single Responsibility Principle

**Нарушение: `Island.java` (~250 строк)**  
Класс отвечает за: хранение сетки, управление чанками, обновление сезонов, статистику, защиту видов, перемещение организмов, перемещение биомассы, инициализацию соседей. Это минимум 5 ответственностей.

**Нарушение: `Cell.java` (~300 строк)**  
Методы: управление животными, биомасса, стриды, семплирование, счётчики. Класс одновременно является контейнером и реестром с бизнес-логикой (проверка terrain accessibility).

#### Open/Closed Principle

**Нарушение: `GameLoop.runTick()`**
```java
// Строки 87-92: жёстко закодированный instanceof для специальной ветки
boolean isCellService = task instanceof CellService;
if (isCellService != inCellServiceGroup && !currentGroup.isEmpty()) {
    executeGroup(currentGroup, inCellServiceGroup);
```
Добавление нового типа специальной задачи (например, `SpatialService` или `NetworkService`) потребует изменения `GameLoop`. Принцип открытости нарушен: новое поведение требует модификации ядра.

#### Liskov Substitution Principle

**Нарушение: `Cell.addAnimal()` и `Cell.removeAnimal()`**
```java
// Cell.java:147
if (world instanceof Island island) {
    island.onOrganismAdded(animal.getSpeciesKey());
}
```
`Cell` реализует `SimulationNode<Organism>`, но внутри проверяет конкретный тип мира. Если заменить `Island` другой реализацией `SimulationWorld<Organism>`, статистика не будет обновляться — молчаливая потеря поведения.

#### Interface Segregation Principle

**Нарушение: `NatureWorld`**
```java
public interface NatureWorld extends SimulationWorld<Organism>, 
    NatureRegistry, NatureStatistics, NatureEnvironment, BiomassManager { }
```
`Island` вынужден реализовывать 5 интерфейсов через один класс. Тест, требующий только `NatureStatistics`, должен создавать полноценный `Island`. Это делает unit-тестирование чрезмерно дорогим.

#### Dependency Inversion Principle

**Нарушение: `LifecycleService`**
```java
// LifecycleService.java:33
super(null, executor, random); // null world!
```
Конструктор передаёт `null` в `AbstractService` для обхода зависимости. `AbstractService.tick()` содержит `if (world == null) return;` как защитный костыль. Это не DI — это обход зависимости через null.

**Нарушение: `AbstractService`** жёстко зависит от `Configuration`, `NatureEnvironment`, `NatureWorld` — все из домена `nature`. Переиспользовать `AbstractService` для SimCity-сервисов без рефакторинга невозможно.

### Антипаттерны

| Антипаттерн | Где | Описание |
|---|---|---|
| **God Interface** | `NatureWorld` | 5 интерфейсов в одном |
| **Null Object (ненастоящий)** | `LifecycleService(null, ...)` | null как «нет мира» вместо Optional или NullObject |
| **Downcasting** | `Cell.addAnimal`, `Island.moveEntity` | `instanceof Island`, `instanceof Cell` вместо полиморфизма |
| **Magic Numbers** | `Island.updateSeason()` | `int seasonDuration = 50` без имени константы |
| **Partial Config Loading** | `Configuration.load()` | Из файла грузятся только 3 поля из ~50 |
| **Type Dispatch в ядре** | `GameLoop.executeGroup()` | `@SuppressWarnings("unchecked")` + ручной каст |

### Coupling / Cohesion

**Высокое coupling:**
- `AbstractService` → `Configuration`, `NatureWorld`, `NatureEnvironment`, `Season`, `SpeciesKey`, `Cell` — 6+ доменных зависимостей в базовом классе сервисов
- `SimulationContext` (engine) → `SimulationView` (nature) — прямое нарушение слоёв
- `FeedingService` → `NatureWorld`, `AnimalFactory`, `InteractionProvider`, `SpeciesRegistry`, `HuntingStrategy` — 7 зависимостей в одном сервисе

**Нарушение Cohesion:**
- `Island` — видит и чанки, и сезоны, и статистику, и защиту видов, и сетку
- `Configuration` — содержит `islandWidth/Height` (геометрия), `wolfPackMinSize` (поведение хищника), `caterpillarMetabolismRateBP` (конкретный вид) в одном классе

### Разделение слоёв

Слои существуют, но не полностью соблюдены:

```
engine → [nature.view] ❌ НАРУШЕНИЕ
nature.service → [engine.Cell] ❌ НАРУШЕНИЕ (instanceof Cell в AbstractService.tick)
nature.model.Cell → [nature.model.Island] ❌ НАРУШЕНИЕ (Cell знает о Island)
```

---

## ⚙️ 3. Алгоритмы и логика симуляции

### Основной цикл (Game Loop / Tick)

`GameLoop.runTick()` работает по схеме:
1. `world.tick(tickCount)` — обновление глобального состояния
2. Группировка задач по типу (`CellService` vs. остальные)
3. `CellService`-группы исполняются параллельно по чанкам через `ExecutorService.invokeAll()`
4. Остальные задачи — последовательно

Это корректная архитектура с хорошим разделением параллельного и последовательного. **Однако:**

**Проблема 1: Порядок группировки — хрупкий инвариант.**  
`recurringTasks` — `ArrayList`, порядок определяет группировку. `CellService`-задачи должны идти подряд, иначе каждый «разрыв» создаёт отдельный fork/join. Это нигде не документировано, не проверяется и легко нарушается при добавлении новой задачи.

**Проблема 2: `beforeTick()` вызывается для всей группы, а не per-cell.**  
В `AbstractService.beforeTick()` обновляется `protectionMap` для всего тика — это корректно. Но если несколько `CellService` имеют взаимозависимые состояния, порядок `beforeTick()`-вызовов внутри группы не гарантирован (фактически sequential, но это деталь реализации).

**Проблема 3: Обработка исключений в tick-петле.**
```java
} catch (Exception e) {
    System.err.println("Error during simulation tick: " + e.getMessage());
    e.printStackTrace();
}
```
Любая ошибка в задаче поглощается и симуляция продолжается. Это может привести к накоплению несогласованного состояния без возможности отладки в production.

**Проблема 4: `Thread.sleep` без ScheduledExecutor.**  
`run()` использует `Thread.sleep(sleepTime)`, что даёт неточный тайминг при накоплении задержки. При tickDurationMs < времени обработки тика симуляция «гонит» без backpressure.

### Универсальность цикла

Цикл достаточно универсален — он не знает о домене. Но:
- Нет поддержки **variable time step** (fixed timestep достаточен для симуляций, но не для игр)
- Нет **pause / resume** без остановки потока
- Нет **deterministic replay** — `DefaultRandomProvider` не сериализуем
- Нет **tick ordering** для зависимых задач (только manual ordering через List)

### Сложность алгоритмов

| Операция | Сложность | Комментарий |
|---|---|---|
| `EntityContainer.countBySpecies()` | O(T) где T — число типов | Итерация по всем типам; мог быть O(1) с дополнительным индексом |
| `ReproductionService.processCell()` | O(limit²) | Двойной вложенный цикл по кандидатам; bounded LOD=30, т.е. макс. 900 итераций |
| `InteractionMatrix.buildFrom()` | O(S²) где S — число видов | Перебор всех пар; приемлемо при малом S |
| `Island.partitionIntoChunks()` | O(W×H) | Одноразово при инициализации, ок |
| `CityMap.getParallelWorkUnits()` | O(W×H) + allocation | **Вызывается каждый тик**, каждый раз создаёт новые `ArrayList`! |

### Узкие места при росте сущностей

1. **`Cell.getAnimalCount()`** вызывает `container.getAllAnimals().size()` → `LinkedHashSet.size()` — O(1), ок. Но `getEntities()` создаёт новый `ArrayList`, объединяя животных и биомассу. При интенсивном вызове — постоянный GC-pressure.

2. **`forEachAnimalSampled` / `forEachHerbivoreSampled`**: итерация по `LinkedHashSet` для семплирования — O(N/step) по элементам множества. При N=10000 и step=100 — 100 итераций, но сам обход `LinkedHashSet` не cache-friendly.

3. **`SpeciesKey.REGISTRY`** — `ConcurrentHashMap`, глобальный синглтон. При создании нескольких симуляций — общее состояние.

---

## 📦 4. Коллекции и структуры данных

### Неоптимальный выбор структур

**`EntityContainer`** использует `LinkedHashSet<Animal>` для `allAnimals`, `predators`, `herbivores` и per-type buckets. При удалении одного животного выполняется 4 remove-операции из 4 разных коллекций — O(1) каждая, но с высокими накладными расходами на cache miss и pointer-chasing.

```java
// EntityContainer.removeAnimal() — 4 операции удаления при каждом kill
allAnimals.remove(animal);       // LinkedHashSet
predators.remove(animal);        // или herbivores
animalsByType.get(type).remove(a); // HashMap → LinkedHashSet
animalsBySize.get(size).remove(a); // EnumMap → LinkedHashSet
```

**Альтернатива для большого числа сущностей:** `ArrayList` + dirty flag + batch cleanup в конце тика. Это более cache-friendly и сокращает количество операций.

**`CityTile` использует `CopyOnWriteArrayList`:**
```java
private final List<SimEntity> entities = new CopyOnWriteArrayList<>();
```
`CopyOnWriteArrayList` оптимален для read-heavy, редко обновляемых коллекций. В симуляции, где здания добавляются/удаляются регулярно, каждая мутация копирует весь массив. Для 100×100 сетки и активного строительства это катастрофически медленно.

**`CityMap.getParallelWorkUnits()`** — создаёт 2 новых `ArrayList` на каждый тик:
```java
// Вызывается каждый тик в GameLoop.runCellServicesParallel()
List<List<CityTile>> chunks = new ArrayList<>();
List<CityTile> currentChunk = new ArrayList<>();
// ...
```
При 1000 тиков × 10000 ячеек — 10+ миллионов временных объектов. `Island` правильно кэширует чанки в `List<Chunk> chunks` — SimCity должен следовать той же стратегии.

### Лишние аллокации

- `Cell.getEntities()` — `new ArrayList<>()` при каждом вызове
- `Cell.getAnimals()`, `getPredators()`, `getHerbivores()` — аналогично, defensive copies при каждом вызове
- `MovementService.processMobileBiomass()` — `new ArrayList<>()` для mobile biomass per cell per tick
- `FeedingService.processPredators()` — `new ArrayList<>()` для packHunters и soloHunters per cell per tick

### Универсальность структур

`EntityContainer` жёстко типизирован на `Animal`, `Biomass`, `AnimalType`, `SizeClass`, `SpeciesKey` — все из домена `nature`. Невозможно переиспользовать для SimCity или любого другого домена без полного переписывания.

---

## 🧱 5. Качество кода

### Нейминг

В целом — хороший. Есть несколько спорных моментов:

- **`scale1M` / `scale10K`** в `Configuration` — это не масштабы, это делители для fixed-point арифметики. Лучше: `FIXED_POINT_SCALE` и `BASIS_POINTS_SCALE`.
- **`getParallelWorkUnits()`** — возвращает `Collection<Collection<SimulationNode>>`. Это «единицы параллельной работы», но имя не несёт смысла «чанк». Лучше: `getChunks()` или `getWorkPartitions()`.
- **`forEachAnimalSampled(int limit, ...)`** — параметр называется `limit`, но логика сложнее (stride sampling). Имя вводит в заблуждение.
- **`tryConsumeEnergy(long amount)`** — возвращает `boolean` (isAlive), но семантика "try" предполагает возможность отказа по ресурсу, а не по смерти.

### Дублирование

**Три почти идентичных блока семплирования:**

1. `Cell.forEachAnimalSampled()` — stride sampling для `Set<Animal>`
2. `Cell.forEachHerbivoreSampled()` — то же самое для другого Set
3. `AbstractService.forEachSampled()` — то же самое для `List`

Все три реализуют одну логику — stride-based sampling с random offset. Это должен быть один утилитный метод `SamplingUtils.forEachSampled(Collection, limit, random, action)`.

**Два конструктора в `FeedingService` и `MovementService`** с очень похожей логикой инициализации. Это признак неудовлетворительного DI — компонент пытается поддержать два сценария использования (полный мир / частичный мир для тестов) через overloading.

**`initNeighbors()`** дублируется в `Island.init()` и `CityMap.initialize()` — идентичная логика 8-связных соседей. Должна быть утилита в движке или `default`-метод в `SimulationWorld`.

### Магические значения

```java
// Island.updateSeason() — строка 217
int seasonDuration = 50; // ← Почему 50? Нет объяснения, нет константы

// ReproductionService.tryReproduceScaled()
baseMaxOffspring += 2; // ← Почему 2? 

// FeedingService.tryEat()
int maxAttempts = consumer.getAnimalType().isPredator() ? 5 : 3; // ← 5 и 3?

// FeedingService.processPackHunting()
int maxAttempts = 5; // ← Снова 5, но не связано с предыдущим

// Configuration.java — ок, значения именованы через поля
```

### Размеры методов и классов

- `Island.java` — ~250 строк, ~15 public методов
- `Cell.java` — ~300 строк, ~20 public методов
- `FeedingService.java` — ~200 строк, метод `tryEat()` ~70 строк
- `Configuration.java` — ~100 полей, метод `load()` грузит только 3

`tryEat()` в `FeedingService` и `processPackHunting()` — кандидаты на Extract Method.

---

## 🧪 6. Тестируемость

### Где код нетестируем и почему

**`AbstractService` — нетестируемый базовый класс:**
- Принимает `NatureWorld` в конструкторе — бог-интерфейс из 5 интерфейсов
- Мок `NatureWorld` требует реализации `SimulationWorld`, `NatureRegistry`, `NatureStatistics`, `NatureEnvironment`, `BiomassManager`
- Альтернативный конструктор через `NatureEnvironment` передаёт `null` как `world`

**`LifecycleService(NatureStatistics, NatureEnvironment, ...)` — brittle:**
```java
super(null, executor, random); // null world → скрытый NPE-риск в fallback tick()
```
Любой тест, который вызовет `tick()` напрямую (не через GameLoop), получит `world == null` → тихо выйдет без обработки ячеек.

**`Cell` — не изолируется от `Island`:**
```java
if (world instanceof Island island) {
    island.onOrganismAdded(animal.getSpeciesKey());
}
```
Тест `Cell.addAnimal()` без `Island` потеряет статистику. Тест с заглушкой `SimulationWorld` не воспроизведёт реальное поведение.

### Изоляция ядра от домена

`GameLoop` тестируем изолированно — нет доменных зависимостей. ✅  
`SimulationNode` / `SimulationWorld` — интерфейсы, мокируются. ✅  
`AbstractService` — нет, см. выше. ❌  
`Configuration` — тестируема, POJO. ✅

### Текущее состояние тестов

Тесты есть (~15 классов), но они в основном **интеграционные**, не unit:
- `SimCityCoreLogicTest`, `SimCitySmokeTest` — создают полный `CityMap`
- `TrophicFeedingTest`, `WolfPackBalanceTest` — создают полный `Island`
- `ReproductionServiceTest` — напрямую instantiates `ReproductionService` с реальным `NatureWorld`

Настоящих unit-тестов движка нет: нет тестов `GameLoop` с mock-задачами, нет тестов параллелизма, нет тестов конкурентного доступа к ячейкам.

---

## 🚀 7. Масштабируемость и расширяемость

### Добавить новый тип сущности

**Для `nature`:** Нужно добавить `SpeciesKey.*` константу, запись в `species.properties`, реализовать класс, добавить в `SpeciesLoader`. Относительно просто, data-driven. ✅

**Для нового домена:** Нужно реализовать `Mortal` + `SimulationWorld` + `SimulationNode` + сервисы. Абстракции достаточны, но нет scaffolding/шаблона. ⚠️

### Изменить правила взаимодействия

Правила в `species.properties` + `InteractionMatrix`. Менять логику — через `HuntingStrategy`. Стратегия инжектируется, расширяема. ✅  
Но `DefaultHuntingStrategy` знает о `wolfPackMinSize`, `bearHuntMaxChancePercent` — жёстко завязан на конкретные виды `nature`-домена. ⚠️

### Добавить новую механику (ресурсы, экономика, погода)

- **Погода:** Нужно добавить интерфейс в `NatureEnvironment`, реализацию в `Island`, подписку в `LifecycleService`. Минимально инвазивно. ✅
- **Ресурсы/экономика:** Требует нового набора сервисов + расширения `Configuration`. Движок позволяет, но `AbstractService` придётся либо расширять (и получать всё наследие), либо писать с нуля. ⚠️
- **Мультипользовательская симуляция:** `SpeciesKey` — синглтон. Невозможно без рефакторинга. ❌

### Использовать в другой игре с другим доменом

**SimCity как доказательство концепции — работает.** `CityMap implements SimulationWorld<SimEntity>`, `CityTile implements SimulationNode<SimEntity>`, сервисы работают через `GameLoop`. Движок действительно переиспользуется.

**Что потребует переписывания ядра:**
1. `SimulationContext` — из-за импорта `SimulationView`
2. `AbstractService` — нет аналога для другого домена без переписывания
3. `CityMap.getParallelWorkUnits()` — аллоцирует при каждом тике (не требует переписывания ядра, но требует фикса)

### Где потребуется переписывание ядра при масштабировании

- **>100K сущностей:** Текущий `LinkedHashSet` в `EntityContainer` — слабое место. Нужно spatial index (квадродерево / пространственный хэш)
- **Сетевая/распределённая симуляция:** Нет абстракции сериализации состояния (только `WorldSnapshot` — snapshot, не state transfer)
- **Несколько одновременных симуляций:** `SpeciesKey` — глобальный синглтон, исключает изоляцию

---

## 🐞 8. Потенциальные баги и риски

### 1. NPE-бомба: `LifecycleService` с null world

```java
// LifecycleService.java:33
super(null, executor, random);
```
`AbstractService.tick()` защищён:
```java
if (world == null) { return; }
```
Но если в будущем кто-то добавит логику в `tick()` до этой проверки — тихий NPE. И `this.config = world.getConfiguration()` в конструкторе `AbstractService(NatureWorld, ...)` упадёт на null сразу.  
**Риск: HIGH** — скрытый, трудно отслеживаемый.

### 2. SpeciesKey.ordinal() возвращает hashCode

```java
// SpeciesKey.java:74
public int ordinal() {
    return code.hashCode(); // НЕ уникально, НЕ стабильно между JVM
}
```
`InteractionMatrix` не использует `ordinal()` напрямую (использует `indexMap`), но метод публичный и создаёт ловушку для будущих разработчиков. `hashCode` строки не гарантирует уникальность и не гарантирует стабильность между запусками JVM (хотя на практике для строк в Java — стабилен).  
**Риск: MEDIUM** — создаёт ложную уверенность.

### 3. Race condition в CityMap

```java
// CityMap.java
private volatile long money = 10000;
// ...
public synchronized void addMoney(long amount) { this.money += amount; }
public synchronized void tick(int tickCount) { ... }
```
`volatile` + `synchronized` на разных методах. Если `EconomyService` и `tick()` работают параллельно — возможна гонка: `tick()` синхронизирован, `EconomyService` может читать `money` (volatile read) пока `addMoney()` незаблокирован.  
**Риск: MEDIUM** — зависит от планировщика.

### 4. Configuration.load() — «молчаливое» неполное чтение

```java
// Configuration.java — грузит только 3 параметра из ~50
config.islandWidth = getIntProperty(props, "island.width", config.islandWidth);
config.islandHeight = getIntProperty(props, "island.height", config.islandHeight);
config.tickDurationMs = getIntProperty(props, "island.tickDurationMs", config.tickDurationMs);
```
Все остальные ~47 параметров (включая `wolfPackMinSize`, `feedingLodLimit`, `plantGrowthRateBP`) всегда берутся из hardcoded defaults. Изменение `species.properties` не влияет на эти параметры. Пользователь/оператор не получает ошибки.  
**Риск: HIGH** — конфигурируемость сломана, это не обнаруживается тестами.

### 5. Семплирование в LinkedHashSet — детерминированный bias

```java
// Cell.forEachAnimalSampled() — stride sampling по LinkedHashSet
int step = (size > limit) ? (size / limit + 1) : 1;
int startOffset = (size > limit) ? random.nextInt(step) : 0;
```
`LinkedHashSet` итерируется в порядке вставки. Животные, добавленные раньше, **систематически** чаще попадают в выборку (меньше шанс быть «за конец» при stride). Это не случайная выборка — это детерминированный bias, маскирующийся под случайность.  
**Риск: LOW (для gameplay) / MEDIUM (для научной точности)**.

### 6. Deadlock-риск при обратном порядке блокировок

```java
// Island.moveOrganism() — корректный lock ordering по (X,Y)
Cell first = (from.getX() < to.getX() || ...) ? from : to;
first.getLock().lock();
second.getLock().lock();
```
Deadlock предотвращается. ✅  
Но: `Island.moveBiomassPartially()` повторяет ту же логику независимо. Если в будущем появится третья операция с двойной блокировкой и автор забудет порядок — deadlock.  
**Риск: LOW сейчас, HIGH потенциально** — нет централизованного механизма упорядочения блокировок.

### 7. FeedingService — двойная проверка liveness

```java
// FeedingService.tryEat() — removeEntity, затем die()
if (getRandom().nextInt(0, 100) < chance && node.removeEntity(a)) {
    a.die();
    consumer.addEnergy(a.getWeight());
```
`removeEntity(a)` успешно — значит `a` был в ячейке. Затем `a.die()`. Но между `removeEntity` (под write lock) и `a.die()` другой поток мог получить ссылку на `a` через `getRandomAnimalByType()` (под read lock). `a.isAlive()` вернёт `true` пока не вызван `die()`.  
**Риск: MEDIUM** — временное окно между удалением из ячейки и смертью.

---

## 💡 9. Приоритетные улучшения

### [HIGH] Устранить утечку домена из движка
`SimulationContext` в пакете `engine` импортирует `com.island.nature.view.SimulationView`. Ввести `interface SimulationView` в пакете `engine` или `util`, убрать доменный импорт из движка.

### [HIGH] Починить Configuration.load()
Добавить загрузку всех параметров из properties-файла или явно задокументировать, что файл управляет только геометрией, а поведенческие параметры — только через код. Текущее состояние создаёт иллюзию конфигурируемости.

### [HIGH] Устранить null в LifecycleService
Либо ввести интерфейс `NatureEnvironmentProvider` без `NatureWorld`, либо использовать `Optional<NatureWorld>`, либо разделить `AbstractService` на два базовых класса.

### [HIGH] Исправить CityMap.getParallelWorkUnits()
Вынести построение чанков в конструктор (как в `Island`). Убрать аллокацию на каждом тике.

### [MEDIUM] Устранить Cell instanceof Island
Ввести callback-интерфейс в `SimulationWorld` для событий добавления/удаления сущностей. Например:
```java
interface EntityEventListener<T> {
    void onEntityAdded(T entity);
    void onEntityRemoved(T entity);
}
```

### [MEDIUM] Сделать SpeciesKey инстанс-объектом
Убрать глобальный статический реестр. Передавать реестр видов через `SpeciesRegistry`. Это разблокирует multi-tenant симуляции.

### [MEDIUM] Вынести семплирование в утилиту
Три идентичных реализации stride sampling → один `SamplingUtils`. Добавить unit-тест.

### [MEDIUM] Заменить CopyOnWriteArrayList в CityTile
Использовать `ArrayList` + write lock или `java.util.concurrent.ConcurrentLinkedDeque`.

### [MEDIUM] Добавить SimulationPlugin/WorldPlugin интерфейс
Для регистрации задач плагином без явного знания движком о домене:
```java
interface SimulationPlugin<T extends Mortal> {
    void registerTasks(GameLoop<T> loop, SimulationWorld<T> world);
}
```

### [LOW] Заменить System.out/err на SLF4J
Минимальный overhead, стандарт отрасли. Без этого невозможно управлять уровнем логирования в production.

### [LOW] Добавить константу для seasonDuration
`private static final int SEASON_DURATION_TICKS = 50;` в `Island` или в `Configuration`.

### [LOW] Unit-тесты для GameLoop
Тест параллельного выполнения CellService, тест порядка задач, тест обработки исключений в задаче.

---

## 🔧 10. Рефакторинг с примерами

### Пример 1: Устранение утечки домена из SimulationContext

**До:**
```java
// engine/SimulationContext.java
package com.island.engine;

import com.island.util.RandomProvider;
import com.island.nature.view.SimulationView; // ❌ Доменный импорт в engine!

public class SimulationContext<T extends Mortal> {
    private final SimulationView view; // привязан к nature.view
    ...
}
```

**После:**
```java
// engine/SimulationView.java — новый интерфейс в engine
package com.island.engine;

public interface SimulationView {
    void display(WorldSnapshot snapshot);
}

// engine/SimulationContext.java — исправленный
package com.island.engine;

import com.island.util.RandomProvider;
// Нет импортов из nature!

public class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T> world;
    private final GameLoop<T> gameLoop;
    private final SimulationView view; // использует engine-интерфейс
    private final RandomProvider random;
    ...
}

// nature/view/ConsoleView.java — реализует engine-интерфейс
package com.island.nature.view;

import com.island.engine.SimulationView;
import com.island.engine.WorldSnapshot;

public class ConsoleView implements SimulationView {
    @Override
    public void display(WorldSnapshot snapshot) { ... }
}
```

Результат: `engine` пакет становится полностью domain-free.

---

### Пример 2: Устранение Cell instanceof Island через EntityEventListener

**До:**
```java
// Cell.java — Cell знает о конкретной реализации мира
public boolean addAnimal(Animal animal) {
    rwLock.writeLock().lock();
    try {
        if (container.countByType(animal.getAnimalType()) >= animal.getMaxPerCell()) {
            return false;
        }
        container.addAnimal(animal);
        if (world instanceof Island island) { // ❌ Downcast!
            island.onOrganismAdded(animal.getSpeciesKey());
        }
        return true;
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

**После:**
```java
// engine/SimulationNode.java — добавить listener в интерфейс
interface SimulationNode<T extends Mortal> {
    // ... существующие методы ...

    /** Called when an entity is successfully added to this node */
    default void onEntityAdded(T entity) {}

    /** Called when an entity is successfully removed from this node */
    default void onEntityRemoved(T entity) {}
}

// nature/model/Cell.java — переопределить, не знать об Island
@Override
public void onEntityAdded(Organism organism) {
    if (organism instanceof Animal a) {
        // Передаём событие через интерфейс SimulationWorld
        // который Island уже реализует через NatureStatistics
        ((NatureWorld) world).onOrganismAdded(a.getSpeciesKey());
    }
}

// или ещё чище — через typed listener:
// Cell принимает Consumer<Organism> в конструкторе:
public class Cell implements SimulationNode<Organism> {
    private final Consumer<Organism> onAddedListener;
    private final Consumer<Organism> onRemovedListener;

    public Cell(int x, int y, SimulationWorld<Organism> world,
                Consumer<Organism> onAdded, Consumer<Organism> onRemoved) {
        ...
        this.onAddedListener = onAdded != null ? onAdded : o -> {};
        this.onRemovedListener = onRemoved != null ? onRemoved : o -> {};
    }

    public boolean addAnimal(Animal animal) {
        rwLock.writeLock().lock();
        try {
            if (container.countByType(animal.getAnimalType()) >= animal.getMaxPerCell()) {
                return false;
            }
            container.addAnimal(animal);
            onAddedListener.accept(animal); // никакого instanceof!
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}

// Island создаёт Cell с лямбдами:
grid[x][y] = new Cell(x, y, this,
    o -> statisticsService.registerBirth(((Animal) o).getSpeciesKey()),
    o -> statisticsService.registerRemoval(((Animal) o).getSpeciesKey())
);
```

Результат: `Cell` не знает об `Island`. Тестирование `Cell` без `Island` — корректно.

---

### Пример 3: Выделение SimulationPlugin для регистрации задач

**До:**
```java
// SimulationBootstrap.java — Bootstrap знает о всех сервисах домена
TaskRegistry taskRegistry = new TaskRegistry(gameLoop, island, matrix, 
    animalFactory, registry, view, random);
taskRegistry.registerAll(); // жёстко хардкоженный список сервисов
```

**После:**
```java
// engine/SimulationPlugin.java
package com.island.engine;

public interface SimulationPlugin<T extends Mortal> {
    SimulationWorld<T> createWorld();
    void registerTasks(GameLoop<T> loop, SimulationWorld<T> world);
    default void onSimulationStarted(SimulationContext<T> context) {}
    default void onSimulationStopped(SimulationContext<T> context) {}
}

// engine/SimulationEngine.java — новый класс
public class SimulationEngine<T extends Mortal> {
    public SimulationContext<T> start(SimulationPlugin<T> plugin, int tickDurationMs, int threads) {
        SimulationWorld<T> world = plugin.createWorld();
        GameLoop<T> loop = new GameLoop<>(tickDurationMs, threads);
        loop.setWorld(world);
        plugin.registerTasks(loop, world);
        SimulationContext<T> ctx = new SimulationContext<>(world, loop, null, null);
        plugin.onSimulationStarted(ctx);
        loop.start();
        return ctx;
    }
}

// nature/NatureSimulationPlugin.java
public class NatureSimulationPlugin implements SimulationPlugin<Organism> {
    private final Configuration config;

    @Override
    public SimulationWorld<Organism> createWorld() { ... }

    @Override
    public void registerTasks(GameLoop<Organism> loop, SimulationWorld<Organism> world) {
        NatureWorld nw = (NatureWorld) world;
        loop.addRecurringTask(new FeedingService(...));
        loop.addRecurringTask(new MovementService(...));
        // ...
    }
}

// Main:
new SimulationEngine<>().start(new NatureSimulationPlugin(config), 100, 4);
// или:
new SimulationEngine<>().start(new SimCityPlugin(), 100, 4);
```

---

## Сравнение с существующими решениями

Прежде чем продолжать разработку движка, стоит оценить, нет ли готовых библиотек:

| Библиотека | Язык | Назначение | Сравнение |
|---|---|---|---|
| **MASON** | Java | Multi-agent симуляция, spatial grid | Мощнее, но академический, тяжёлый UI |
| **Repast Simphony** | Java | ABM для науки | Избыточен для игр |
| **LibGDX** | Java | Game framework, не симуляция | Рендеринг ≠ симуляция |
| **Terasology (движок)** | Java | Voxel game engine | Слишком тяжёлый |
| **Ashley (libGDX ECS)** | Java | Entity-Component-System | Только ECS, нет spatial grid |

**Вывод:** Для уровня «SimCity / Весёлая Ферма» готовые Java-библиотеки либо слишком академичны (MASON), либо не покрывают spatial simulation с game loop. **Самописный движок — оправданное решение**, при условии доведения архитектуры до качества «движка».

---

## 📊 11. Итоговая оценка

### Архитектура: 6.5/10

Правильное намерение, хорошие абстракции в `engine`-пакете, работающий proof-of-concept (два независимых плагина). Но: утечка домена в `SimulationContext`, нарушения LSP в `Cell`, god-interface `NatureWorld`, отсутствие plugin mechanism.

### Код: 7/10

Читаемый, Lombok используется к месту, integer arithmetic обоснована, lock ordering продуман. Минусы: дублирование семплирования, `System.out/err` вместо logging, магические числа, неполный `Configuration.load()`.

### Переиспользуемость: 6/10

`GameLoop`, `SimulationWorld`, `SimulationNode` — переиспользуемы. `AbstractService`, `Configuration`, `SpeciesKey`, `EntityContainer` — жёстко привязаны к `nature`-домену. SimCity-плагин работает, но без `AbstractService` (пишет сервисы с нуля) — что показывает реальный уровень engine reuse.

### Общая оценка: 6.5/10

---

## Вердикт

**Готов ли проект стать основой для универсального симуляционного движка?**

**Не полностью — но потенциал высокий и вектор правильный.**

Движок демонстрирует зрелый подход: параллельный game loop с чанками, lock ordering для предотвращения deadlocks, fixed-point арифметика, object pool, LOD-семплирование, data-driven конфигурация видов. Это не учебный проект — это инженерная работа.

Три блокирующих проблемы для статуса «движка»:
1. Утечка `nature`-домена в `engine`-пакет (`SimulationContext` → `SimulationView`)
2. Нет механизма регистрации плагинов (`SimulationPlugin` interface)
3. `AbstractService` — непереиспользуемый базовый класс для сервисов

После устранения этих трёх пунктов — проект становится полноценным движком уровня «SimCity / Весёлая Ферма» с чётким путём к масштабированию.

**Оценочный объём работы до production-ready engine: 2–3 итерации (спринта)**. Архитектура не требует переписывания — требует доведения до последовательного соблюдения собственных принципов, задокументированных в `MODULAR_ARCHITECTURE.md`.
