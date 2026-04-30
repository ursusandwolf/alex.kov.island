# CODE REVIEW — alex.kov.island (branch `dev`)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2024  
**Scope:** Architecture, engine reusability, extensibility, domain decoupling, simulation loop, data structures, testability, scalability, risks  
**Goal:** Evaluate project as a potential foundation for a reusable simulation engine (SimCity, "Happy Farm" level games)

---

## 1. Общий обзор проекта

### Что моделируется (домен)

Проект моделирует **островную экосистему** с клеточным полем:
- Животные (хищники, травоядные, хладнокровные)
- Биомасса/растения (трава, капуста)
- Перемещения между клетками
- Питание (охота, поедание растений, pack hunting)
- Размножение (с учётом endangered status)
- Смерть (голод, возраст, съедение, истощение при движении)
- Статистика популяции
- Механика "red book protection" (защита исчезающих видов)

### Насколько доменная модель привязана к конкретному сценарию

**Привязка критически сильная.** Проект architected как "игра про остров", а не как универсальный движок.

Ключевые признаки сильной привязки:

| Класс | Проблема |
|-------|----------|
| `Island` | Конкретный остров с `width/height`, тороидальной адресацией, `redBookProtectionEnabled` |
| `StatisticsService` | Логика завязана на подсчёт животных vs biomass, death causes специфичны для зоосимулятора |
| `AnimalType` | Хранит поля уровня конкретной игры: `maxPerCell`, `speed`, `huntProbabilities`, `isColdBlooded`, `isPackHunter`, `presenceChance`, `settlementBase`, `settlementRange` |
| `Cell` | Одновременно storage, lock boundary, statistics aggregator, cleanup manager |
| `Organism` / `Animal` | Содержат игровые правила (метаболизм, голод, hibernation), а не только состояние домена |
| `SpeciesKey` | Хардкодит конкретные виды (`WOLF`, `BEAR`, `GRASS`), что делает его непригодным для других доменов |

**Вывод:** Это хорошая база для одной конкретной симуляции, но не для универсального движка. Для SimCity или factory simulator ядро придётся переписывать.

### Можно ли переиспользовать текущие абстракции в других симуляциях

**Частично — да, но только на уровне грубых примитивов:**

✅ **Полезные абстракции:**
- `Tickable` — интерфейс тика, универсален
- `SimulationNode` / `SimulationWorld` — могут стать основой для графа мира
- `CellService` — паттерн обработки узлов в параллельном цикле
- `Mortal` — простой интерфейс жизни/смерти

❌ **Слишком специфичные:**
- `AnimalType` — заточен под зоологию (predator, pack hunter, cold blooded)
- `SizeClass` — enum с размерами животных (TINY, SMALL, MEDIUM, LARGE, HUGE)
- `DeathCause` — перечисление причин смерти животных (HUNGER, EATEN, AGE, MOVEMENT_EXHAUSTION)
- `FeedingService`, `ReproductionService` — логика специфична для биологических существ

Для city builder потребуются:
- `BuildingType` вместо `AnimalType`
- `ServiceCoverage` вместо `HuntingStrategy`
- `ResourceFlow` вместо `Biomass`
- `Population` вместо `Animal`

### Есть ли "протекание" домена в технические детали

**Да, и это один из главных архитектурных рисков:**

```
┌─────────────────────────────────────────────────────────────┐
│                    Domain Leakage                           │
├─────────────────────────────────────────────────────────────┤
│ model.Cell → знает о ReentrantLock, SimulationWorld,       │
│                StatisticsService через побочные эффекты     │
│                                                             │
│ model.Island → содержит orchestration, domain policy,      │
│                statistics aggregation, movement semantics   │
│                                                             │
│ content.Organism → завязан на SimulationConstants,         │
│                    EnergyPolicy, Poolable, UUID             │
│                                                             │
│ GameLoop → создаёт пул потоков внутри конструктора,        │
│            смешивая technical detail с orchestration        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Архитектура и дизайн

### Архитектурный стиль

Это **не чистая layered architecture** и **не чистый DDD**. По факту — смесь:

| Стиль | Проявление | Оценка |
|-------|------------|--------|
| **Anemic model objects** | Часть данных в `AnimalType`, `SpeciesKey` | ⚠️ |
| **Rich domain objects** | `Animal`/`Organism` с поведением | ✅ |
| **Service-oriented orchestration** | `FeedingService`, `MovementService`, `LifecycleService` | ✅ |
| **Monolithic world aggregate** | `Island` с централизацией правил | ❌ |
| **Data-driven registry/flyweight** | `SpeciesRegistry`, `AnimalType` | ✅ |

### Насколько архитектура соответствует уровню "движка"

**Пока — скорее "игра с претензией на движок", чем движок.**

Причины:

1. **Engine-core не выделен как отдельный модуль** с минимальным API
2. **Доменная модель и механики острова взаимно переплетены**
3. **Сервисы существуют, но их границы не обеспечивают переносимость**
4. **Подсистемы статистики, перемещения, защиты вида и жизненного цикла** связаны через конкретные классы острова и клеток

**Для движка нужен слой, где:**
- Мир — это абстракция (`WorldGrid`, `SpatialIndex`)
- Сущности — данные + поведение через стратегии/системы
- Правила — подключаемые модули (`MovementPolicy`, `ReproductionStrategy`)
- Домен игры **не живёт внутри ядра**

### SOLID (каждый принцип отдельно)

#### S — Single Responsibility Principle

**Нарушен системно.**

**`model.Island`** — God Object:
```java
public class Island implements SimulationWorld {
    // 1. Хранит grid
    private final Cell[][] grid;
    
    // 2. Создаёт и партиционирует chunks
    private final List<Chunk> chunks;
    private void partitionIntoChunks() { ... }
    
    // 3. Инициализирует соседей
    private void initNeighbors() { ... }
    
    // 4. Считает статистику
    public Map<SpeciesKey, Integer> getSpeciesCounts() { ... }
    public double getGlobalSatiety() { ... }
    
    // 5. Управляет перемещением
    public boolean moveOrganism(Animal animal, Cell from, Cell to) { ... }
    public void moveBiomassPartially(...) { ... }
    
    // 6. Реализует защиту видов
    public Map<SpeciesKey, Integer> getProtectionMap(...) { ... }
    
    // 7. Callbacks в StatisticsService
    public void reportDeath(...) { ... }
    public void onOrganismAdded(...) { ... }
    
    // 8. Tick state
    private int tickCount;
    private Season currentSeason;
}
```

**8+ причин для изменения** — класс слишком большой, центральный и зависимый.

**`model.Cell`** —多重职责:
- Хранит сущности (storage)
- Реализует thread safety (concurrency boundary)
- Ведёт индексы через `EntityContainer` (indexing)
- Считает статистику (query service)
- Возвращает копии коллекций (DTO factory)
- Выполняет cleanup (lifecycle management)

**`content.Organism`**:
- Хранит состояние живого существа
- Отвечает за энергетику (`tryConsumeEnergy`, `addEnergy`)
- Lifecycle (`die`, `checkAgeDeath`, `reset`, `init`)
- Знает о `Poolable` (object pool pattern)
- Содержит базовые survival checks (`canPerformAction`, `isStarving`)

#### O — Open/Closed Principle

**Нарушен из-за глубокой привязки к типам и ветвления по ролям.**

**Пример 1: `EntityContainer`**
```java
public class EntityContainer {
    private final Map<AnimalType, Set<Animal>> animalsByType;
    private final Map<SizeClass, Set<Animal>> animalsBySize;
    private final Set<Animal> predators;      // hard-coded classification
    private final Set<Animal> herbivores;     // hard-coded classification
    private final Set<Animal> allAnimals;
    
    public void addAnimal(Animal animal) {
        // Добавление новой классификации (например, "scavenger")
        // потребует изменения этого класса!
        if (animal.isAnimalPredator()) {
            predators.add(animal);
        } else {
            herbivores.add(animal);
        }
    }
}
```

**Пример 2: `Cell`**
```java
public class Cell {
    public List<Animal> getPredators() { ... }      // domain-specific
    public List<Animal> getHerbivores() { ... }     // domain-specific
    public List<Animal> getAnimalsBySize(SizeClass size) { ... }
    public int countAnimalsByType(AnimalType type) { ... }
}
```

**Пример 3: `StatisticsService`**
```java
public int getTotalAnimalDeathCount(DeathCause cause) {
    return statisticsService.getTotalDeaths(cause).entrySet().stream()
        .filter(e -> !registry.getAnimalType(e.getKey())
            .map(AnimalType::isBiomass).orElse(false))  // знание о biomass vs animal
        .mapToInt(Map.Entry::getValue).sum();
}
```

**Вывод:** Новая механика потребует **переписывания**, а не расширения.

#### L — Liskov Substitution Principle

**Формально наследование работает, но семантика хрупкая.**

**`content.Animal extends Organism`:**
- Добавляет `isHiding`, `canEat`, `getHuntProbability`, `getMaxPerCell`, `getSpeed`
- Базовый `Organism` содержит `getDynamicMetabolismRate()` через `getWeight()`
- `Animal` переопределяет поведение через данные `AnimalType`

**⚠️ Гипотеза:** В будущем разные подклассы животных могут ломать ожидания `Organism`, потому что базовый класс слишком много знает о текущей игре.

**`GenericBiomass` / plant-related entities:**
- Biomass и Animal наследуют общий `Organism`, но живут по разным правилам
- Не все organisms должны уметь двигаться, охотиться или иметь одинаковый lifecycle

**Риск:** На уровне runtime это проявляется в неочевидных инвариантах.

#### I — Interface Segregation Principle

**Частично нарушен.**

`SimulationWorld` объединяет слишком много обязанностей:

```java
public interface SimulationWorld extends Tickable {
    // 1. Spatial navigation
    Optional<SimulationNode> getNode(SimulationNode current, int dx, int dy);
    Collection<? extends Collection<? extends SimulationNode>> getParallelWorkUnits();
    
    // 2. Movement
    boolean moveAnimal(Animal animal, SimulationNode from, SimulationNode to);
    void moveBiomassPartially(Biomass b, SimulationNode from, SimulationNode to, long amount);
    
    // 3. Population tracking
    void reportDeath(SpeciesKey key, DeathCause cause);
    void onOrganismAdded(SpeciesKey key);
    void onOrganismRemoved(SpeciesKey key);
    int getSpeciesCount(SpeciesKey key);
    
    // 4. Protection policy
    Map<SpeciesKey, Integer> getProtectionMap(SpeciesRegistry registry);
    ProtectionService getProtectionService();
    
    // 5. World metadata
    int getWidth();
    int getHeight();
    Season getCurrentSeason();
    
    // 6. Registry access
    SpeciesRegistry getRegistry();
    StatisticsService getStatisticsService();
    
    // 7. Snapshot creation
    WorldSnapshot createSnapshot();
}
```

**Клиентам нужны узкие интерфейсы:**
```java
public interface SpatialWorld {
    Optional<Node> neighborOf(Node node, int dx, int dy);
    List<? extends Node> workUnits();
}

public interface PopulationTracker {
    void onBirth(SpeciesKey key);
    void onRemoval(SpeciesKey key);
    void onDeath(SpeciesKey key, DeathCause cause);
    int getCount(SpeciesKey key);
}

public interface MovementPort {
    boolean move(Animal animal, Node from, Node to);
}

public interface ProtectionPolicyProvider {
    Map<SpeciesKey, Integer> getProtectionModifiers();
}
```

#### D — Dependency Inversion Principle

**Нарушен в нескольких местах.**

**`GameLoop`:**
```java
public class GameLoop {
    private final ExecutorService taskExecutor;
    
    public GameLoop(long tickDurationMs, int threadCount) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = Executors.newFixedThreadPool(threadCount); // ❌ Concrete dependency
    }
}
```

High-level orchestration зависит от конкретной реализации executor. Тестирование и настройка scheduler усложняются.

**`Island`:**
```java
public class Island implements SimulationWorld {
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    private final ProtectionService protectionService;
    
    public Island(int width, int height, SpeciesRegistry registry, 
                  StatisticsService statisticsService) {
        this.registry = registry;
        this.statisticsService = statisticsService;
        this.protectionService = new DefaultProtectionService(...); // ❌ Hard-coded dependency
    }
}
```

**`Cell`:**
```java
public boolean addAnimal(Animal animal) {
    container.addAnimal(animal);
    world.onOrganismAdded(animal.getSpeciesKey()); // ❌ Storage-level object calls global statistics
    return true;
}
```

### Анти-паттерны

#### 1) God Object

**`model.Island`** и частично **`model.Cell`** — центральные классы, куда стекается:
- Бизнес-логика
- Инфраструктура (locks, threading)
- Навигация (grid, neighbors)
- Статистика
- Policies (protection, seasons)

#### 2) Data Clump + Feature Envy

**`AnimalType`** агрегирует множество параметров:
```java
public final class AnimalType {
    private final SpeciesKey speciesKey;
    private final String typeName;
    private final long weight;
    private final long foodForSaturation;
    private final long maxEnergy;
    private final int maxPerCell;
    private final int speed;
    private final int maxLifespan;
    private final Map<SpeciesKey, Integer> huntProbabilities;
    private final boolean isPredator;
    private final SizeClass sizeClass;
    private final boolean isColdBlooded;
    private final boolean isPackHunter;
    private final boolean isBiomass;
    private final boolean isPlant;
    private final int reproductionChance;
    private final int maxOffspring;
    private final int presenceChance;
    private final long settlementBase;
    private final long settlementRange;
    private final boolean canFly;
    private final boolean canSwim;
    private final boolean canWalk;
}
```

`Organism`/`Animal` постоянно дергают `AnimalType`, `SimulationConstants`, `EnergyPolicy`.

#### 3) Parallelism Leakage

**`GameLoop`:**
```java
public class GameLoop {
    private final ExecutorService taskExecutor; // Создаётся, но...
    
    public void runTick() {
        // ...не используется в основном цикле!
        for (Tickable task : recurringTasks) {
            task.tick(tickCount); // Последовательное выполнение
        }
    }
}
```

Есть намёк на параллельную модель, но архитектурно она не доведена до конца. **Опаснее, чем отсутствие параллелизма** — создаётся ложное ощущение scalability.

#### 4) Anemic Domain with Technical Coupling

**`EntityContainer`** — storage-only структура, но:
- Доступ жёстко завязан на `Cell`
- Внутренние коллекции возвращаются без копирования
- Нет инкапсуляции

### Coupling / Cohesion

#### Высокая связанность (Coupling)

```
Island ↔ Cell ↔ EntityContainer ↔ StatisticsService ↔ SpeciesRegistry
   ↓        ↓          ↓                  ↓                  ↓
Animal ↔ AnimalType ↔ EnergyPolicy ↔ SimulationConstants
   ↓
Organism ↔ Poolable
```

Эти связи не только по контрактам, но и по конкретным implementation details.

#### Низкая связность (Cohesion)

Особенно у:
- **`Island`** — state, policy, orchestration, metrics, threading, movement
- **`Cell`** — storage, concurrency, indexing, queries, cleanup
- **`GameLoop`** — scheduling, threading, task management, timing

### Разделение слоев

**Фактически отсутствует** жёсткое разделение на:

```
┌─────────────────────────────────────┐
│        Presentation Layer           │  ← view.ConsoleView
├─────────────────────────────────────┤
│     Application/Engine Layer        │  ← GameLoop, TaskRegistry
├─────────────────────────────────────┤
│          Domain Layer               │  ← Island, Cell, Animal
├─────────────────────────────────────┤
│       Infrastructure Layer          │  ← StatisticsService, ConfigLoader
└─────────────────────────────────────┘
```

`view` есть как папка, но engine и domain переплетены напрямую.

### Где архитектура мешает переиспользованию

| Проблема | Последствие |
|----------|-------------|
| `Island` — островной world model с тороидальной топологией | Нельзя использовать для non-grid worlds |
| `AnimalType` — зоологические флаги и ограничения | Не подходит для buildings, resources, services |
| `StatisticsService` — различение animal vs biomass | Требует переделки для других доменов |
| `Cell` — индексация под predators/herbivores/size | Не адаптируется под другие классификации |
| `GameLoop` — tick-driven без event bus | Сложно добавить реактивные системы |

---

## 3. Алгоритмы и логика симуляции

### Основной цикл симуляции (Game Loop)

**Текущая реализация:**

```java
public class GameLoop {
    private final List<Tickable> recurringTasks = new ArrayList<>();
    private final ExecutorService taskExecutor; // Не используется в runTick()
    private volatile boolean running = false;
    
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            runTick();
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = tickDurationMs - elapsed;
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        }
    }
    
    public void runTick() {
        tickCount++;
        for (Tickable task : recurringTasks) {
            task.tick(tickCount); // Последовательно!
        }
    }
}
```

**Проблемы:**

1. **`taskExecutor` создаётся, но в `runTick()` не используется**
   - Параллельность на уровне loop фактически не реализована
   - Лишний lifecycle у executor
   - Код вводит в заблуждение относительно модели исполнения

2. **Нет явных фаз:**
   ```
   ❌ Сейчас: [tick] → [tick] → [tick] → ...
   ✅ Нужно: [preTick] → [update] → [resolve] → [postTick] → ...
   ```

3. **Нет event pipeline** — системы не могут реагировать на события друг друга

4. **Нет explicit ordering for systems** — порядок зависит от порядка добавления в `recurringTasks`

5. **Нет dependency between systems** — нельзя выразить, что Feeding должен идти после Movement

6. **Нет deterministic replay hooks** — кроме косвенной seed-based randomness

### Универсальность loop для других симуляций

**Ограничена.** Подходит для простого tick-based сценария, но не для универсального движка.

**Для city builder потребуется:**
- Event-driven architecture (building completed, resource depleted)
- Priority-based scheduling (emergency services first)
- Conditional execution (only process active districts)
- Save/load state hooks

### Лишние вычисления / проходы

**Критичные места:**

#### `Island.getSpeciesCounts()`
```java
public Map<SpeciesKey, Integer> getSpeciesCounts() {
    Map<SpeciesKey, Integer> counts = new HashMap<>();
    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            Cell cell = grid[x][y];
            // Проход по всем животным
            for (Animal animal : cell.getAnimals()) {
                counts.merge(animal.getSpeciesKey(), 1, Integer::sum);
            }
            // Проход по всей биомассе
            for (Biomass biomass : cell.getBiomassContainers()) {
                counts.merge(biomass.getSpeciesKey(), 
                    (int)(biomass.getBiomass() / SCALE_1M), Integer::sum);
            }
        }
    }
    return counts;
}
```

**O(W×H×N)** — полный обход сетки на каждый вызов.

#### `Island.getTotalOrganismCount()`
Повторяет полные проходы по сетке.

#### `Island.getGlobalSatiety()`
```java
public double getGlobalSatiety() {
    long totalCurrent = 0;
    long totalMax = 0;
    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            for (Animal animal : grid[x][y].getAnimals()) {
                totalCurrent += animal.getCurrentEnergy();
                totalMax += animal.getMaxEnergy();
            }
        }
    }
    return (totalMax == 0) ? 100.0 : (double) totalCurrent / totalMax * 100.0;
}
```

**O(W×H×N)** — ещё один полный обход.

#### `Island.getStarvingCount()`
Аналогично — полный обход всех клеток и животных.

**Если такие методы вызываются в UI каждую секунду, они станут заметным hotspot.**

**Решение для движка:** Инкрементальные агрегаты, обновляемые по событиям (birth, death, energy change), а не full scan on demand.

### Потенциальные проблемы сложности (Big-O)

#### `EntityContainer.removeAnimal(...)`
```java
public boolean removeAnimal(Animal animal) {
    AnimalType type = animal.getAnimalType();
    Set<Animal> typeSet = animalsByType.get(type);
    if (typeSet != null && typeSet.remove(animal)) { // LinkedHashSet.remove = O(1)
        allAnimals.remove(animal); // LinkedHashSet.remove = O(1)
        predators.remove(animal);  // LinkedHashSet.remove = O(1)
        herbivores.remove(animal); // LinkedHashSet.remove = O(1)
        // ...
        return true;
    }
    return false;
}
```

**Фактически O(1)** благодаря `LinkedHashSet`, но множественные удаления создают overhead.

#### `EntityContainer.countBySpecies(...)`
```java
public int countBySpecies(SpeciesKey key) {
    int count = 0;
    for (Map.Entry<AnimalType, Set<Animal>> entry : animalsByType.entrySet()) {
        if (entry.getKey().getSpeciesKey().equals(key)) {
            count += entry.getValue().size();
        }
    }
    // + проверка biomass
    return count;
}
```

**O(T)** где T — количество типов в клетке. Пока типов мало — терпимо.

#### `Island.moveOrganism(...)`
```java
public boolean moveOrganism(Animal animal, Cell from, Cell to) {
    Cell first = (from.getX() < to.getX() || ...) ? from : to;
    Cell second = (first == from) ? to : from;
    
    first.getLock().lock();
    try {
        second.getLock().lock();
        try {
            if (to.canAccept(animal)) {
                if (from.removeAnimal(animal)) {
                    if (!to.addAnimal(animal)) {
                        if (!from.addAnimal(animal)) {
                            // Kill animal as fallback!
                            animal.tryConsumeEnergy(animal.getCurrentEnergy());
                            reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                        }
                    }
                }
            }
        } finally {
            second.getLock().unlock();
        }
    } finally {
        first.getLock().unlock();
    }
}
```

**Сложная транзакционная операция** с double-lock ordering. При росте concurrency может стать узким местом из-за lock contention.

### Узкие места при росте количества сущностей

| Место | Проблема | Масштаб |
|-------|----------|---------|
| `Cell.getAnimals()` | Возвращает копию списка | O(N) аллокация на вызов |
| `Island.*` методы | Глобальные scans по сетке | O(W×H×N) |
| `StatisticsService` | AtomicInteger/ConcurrentHashMap | Чтения неатомарны на уровне агрегатов |
| `GameLoop` | Нет реального параллелизма | Не масштабируется с количеством ядер |

---

## 4. Коллекции и структуры данных

### Где выбор структур не оптимален

#### `EntityContainer`

**Текущие структуры:**
```java
public class EntityContainer {
    private final Map<AnimalType, Set<Animal>> animalsByType = new HashMap<>();
    private final Map<SizeClass, Set<Animal>> animalsBySize = new EnumMap<>(SizeClass.class);
    private final Set<Animal> predators = new LinkedHashSet<>();
    private final Set<Animal> herbivores = new LinkedHashSet<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new HashMap<>();
    private final Set<Animal> allAnimals = new LinkedHashSet<>();
    private final List<Biomass> allBiomass = new ArrayList<>();
}
```

**Проблемы:**

1. **Дублирование данных в нескольких индексах** увеличивает:
   - Стоимость обновления (add/remove нужно синхронизировать во всех коллекциях)
   - Риск рассинхронизации (баг: удалить из одного индекса, забыть про другой)
   - Потребление памяти (6+ структур на одну клетку)

2. **`List` для `allBiomass`** — удаление O(N), поиск O(N)

3. **Множественные projections** одной сущности оправданы только если измерено профилированием

#### `Cell.get...()` методы

Возвращают `new ArrayList<>(...)`, т.е. каждый read API плодит аллокации:

```java
public List<Animal> getAnimals() {
    rwLock.readLock().lock();
    try {
        return new ArrayList<>(container.getAllAnimals()); // Аллокация!
    } finally {
        rwLock.readLock().unlock();
    }
}

public List<Animal> getPredators() { /* Аллокация! */ }
public List<Animal> getHerbivores() { /* Аллокация! */ }
public List<Animal> getAnimalsByType(AnimalType type) { /* Аллокация! */ }
public List<Animal> getAnimalsBySize(SizeClass size) { /* Аллокация! */ }
public List<Biomass> getBiomassContainers() { /* Аллокация! */ }
```

**Для UI/analytics это допустимо. Для hot path — нет.**

#### `GameLoop.recurringTasks`

```java
private final List<Tickable> recurringTasks = new ArrayList<>();
```

**Проблемы:**
- Нет защиты от concurrent modification
- Нет явного lifecycle management
- Нет remove task API

### Где происходят лишние аллокации / копирования

| Метод | Аллокация | Частота |
|-------|-----------|---------|
| `Cell.getAnimals()` | `new ArrayList<>()` | High |
| `Cell.getPredators()` | `new ArrayList<>()` | Medium |
| `Cell.getHerbivores()` | `new ArrayList<>()` | Medium |
| `Cell.getAnimalsByType()` | `new ArrayList<>()` | Medium |
| `Cell.setNeighbors()` | `List.copyOf()` | Once per init |
| `Island.getSpeciesCounts()` | `new HashMap<>()` | Per call |
| `Island.getProtectionMap()` | `new HashMap<>()` | Per tick |
| `Island.getParallelWorkUnits()` | `.toList()` | Per tick |

**Сами по себе нормальные решения для не-hot paths, но здесь они внутри core simulation model.**

### Насколько структуры универсальны

**Слабо универсальны:**

- `EnumMap<SizeClass, ...>` предполагает зоологическую классификацию
- `AnimalType` с `isPredator`, `isPackHunter` не применим к city builder
- Для factory simulator потребуются:
  - `MachineType` вместо `AnimalType`
  - `QueueState` вместо `EnergyState`
  - `ConveyorLink` вместо `Neighbor`

---

## 5. Качество кода

### Нейминг (конкретные плохие примеры)

| Класс/Метод | Проблема | Предложение |
|-------------|----------|-------------|
| `GameLoop.taskExecutor` | Создаётся, но не используется в `runTick()` | Удалить или реально использовать |
| `EntityContainer` | Слишком общее имя для indexed cell storage | `CellEntityIndex`, `CellOccupancyIndex` |
| `Animal.isProtected(int currentTick)` | Параметр `currentTick` не используется | Удалить параметр или реализовать логику |
| `Island.getProtectionMap(SpeciesRegistry registry)` | Принимает `registry`, хотя у `Island` есть свой field | Убрать параметр |
| `getPlantCount()` | Возвращает `int`, но суммирует биомассу | `getBiomassAmountAsInt()` |

### Дублирование

**Проходы по всей сетке:**
```java
// В Island.java
public Map<SpeciesKey, Integer> getSpeciesCounts() { /* полный обход */ }
public int getTotalOrganismCount() { /* полный обход */ }
public double getGlobalSatiety() { /* полный обход */ }
public int getStarvingCount() { /* полный обход */ }
```

**Double-lock ordering:**
```java
// В moveOrganism(...)
Cell first = ...; Cell second = ...;
first.getLock().lock();
try {
    second.getLock().lock();
    try { ... }
    finally { second.getLock().unlock(); }
}
finally { first.getLock().unlock(); }

// В moveBiomassPartially(...) — та же логика дублируется!
```

**Guarded getter methods в `Cell`:**
```java
public void forEachAnimal(Consumer<Animal> action) {
    rwLock.readLock().lock();
    try { container.getAllAnimals().forEach(action); }
    finally { rwLock.readLock().unlock(); }
}

public void forEachBiomass(Consumer<Biomass> action) {
    rwLock.readLock().lock();
    try { container.getAllBiomass().forEach(action); }
    finally { rwLock.readLock().unlock(); }
}

// ... и так далее 10+ методов с одинаковым паттерном
```

### Магические значения

| Значение | Где | Проблема |
|----------|-----|----------|
| `chunkSize = 20` | `Island.partitionIntoChunks()` | Должно быть в конфиге |
| `0.05` | `getProtectionMap()` | Magic number для endangered threshold |
| `0.60` / `0.30` | Расчёт `hideChance` | Magic numbers для hide mechanics |
| `30.0` | `getStarvingCount()` | Порог голода захардкожен |
| `100.0` | `getGlobalSatiety()` | Нормализация захардкожена |

**Если эти значения — доменные параметры, они должны быть в policy/config object.**

### Слишком большие методы / классы

#### `Island` (~290 строк)
Самый заметный кандидат на распил. Уже стал центральной точкой для:
- Grid init
- Topology
- Movement
- Population stats
- Lifecycle callbacks
- Protection policy
- Biomass transfer
- Tick state

#### `Cell` (~430 строк)
Тоже слишком большой:
- Storage
- Concurrent access
- Statistics
- Cleanup
- Movement support
- Biomass management

#### `Organism` (~140 строк)
Не огромный по строкам, но слишком "dense" по responsibilities.

---

## 6. Тестируемость

### Где код невозможно протестировать и почему

#### `GameLoop`
```java
public class GameLoop {
    public GameLoop(long tickDurationMs, int threadCount) {
        this.taskExecutor = Executors.newFixedThreadPool(threadCount);
    }
    
    public void start() {
        new Thread(this::run).start(); // Hard-to-test thread creation
    }
}
```

**Проблемы:**
- Создаёт собственный `Thread` через `new Thread(this::run).start()`
- Создаёт собственный `ExecutorService`
- Сложно deterministically контролировать tick progression
- Нет возможности inject mock clock/scheduler

**Для тестов нужен:** Injected scheduler / clock / executor.

#### `Island`
```java
public class Island implements SimulationWorld {
    public Island(int width, int height, SpeciesRegistry registry, 
                  StatisticsService statisticsService) {
        this.protectionService = new DefaultProtectionService(...); // Hard-coded
    }
}
```

**Проблемы:**
- Жёстко завязан на `StatisticsService`, `SpeciesRegistry`
- Переходы состояния завязаны на cellular locks и side effects
- `moveOrganism(...)` трудно тестировать без комплексного setup

#### `Cell`
```java
public class Cell implements SimulationNode {
    private final SimulationWorld world;
    private final EntityContainer container = new EntityContainer();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
}
```

**Проблемы:**
- Методы вроде `cleanupDeadOrganisms()` требуют реальных `Animal`, `Biomass` объектов
- Возврат копий коллекций скрывает часть состояния
- Сложно assert-ить инварианты без доступа к внутреннему состоянию

#### `Organism`
```java
public abstract class Organism implements Poolable, Mortal {
    private final ReentrantLock energyLock = new ReentrantLock();
    
    public boolean tryConsumeEnergy(long amount) {
        energyLock.lock();
        try {
            currentEnergy = Math.max(0, currentEnergy - amount);
            if (currentEnergy == 0 && isAlive) {
                isAlive = false;
            }
            return isAlive;
        } finally {
            energyLock.unlock();
        }
    }
}
```

**Проблемы:**
- Использует internal state + locks + static policy values
- Без вынесения energy policy и lifecycle policy в тестовые стратегии трудно писать быстрые unit tests

### Можно ли тестировать ядро симуляции изолированно от домена

**Пока — только частично.**

| Компонент | Изолируемость | Причина |
|-----------|---------------|---------|
| `GameLoop` | ⚠️ Частично | Thread creation hard-coded |
| `StatisticsService` | ✅ Да | Почти независим |
| `Island` | ❌ Нет | Завязан на domain classes |
| `Cell` | ❌ Нет | Требует SimulationWorld, Animal, Biomass |
| `Organism` | ⚠️ Частично | Locks + static policies |

### Где не хватает абстракций (интерфейсы, DI)

**Отсутствующие интерфейсы:**

```java
// Clock / Scheduler
public interface SimulationClock {
    long getCurrentTick();
    void advance();
}

// Executor Provider
public interface ExecutorProvider {
    ExecutorService getExecutor();
    void shutdown();
}

// Movement Policy
public interface MovementPolicy<T extends Organism> {
    MovementResult move(T entity, Node from, Node to);
}

// Reproduction Strategy
public interface ReproductionStrategy<T extends Organism> {
    Optional<T> reproduce(T parent1, T parent2, Node location);
}

// Mortality Policy
public interface MortalityPolicy<T extends Organism> {
    boolean shouldDie(T entity, DeathContext context);
}

// Population Counter
public interface PopulationCounter {
    int getCount(SpeciesKey key);
    Map<SpeciesKey, Integer> getAllCounts();
}

// Protection Policy
public interface ProtectionPolicy {
    Map<SpeciesKey, Integer> getProtectionModifiers(WorldSnapshot snapshot);
}

// Cell Storage
public interface CellStorage {
    boolean add(Organism organism);
    boolean remove(Organism organism);
    Stream<Organism> stream();
}
```

---

## 7. Масштабируемость и расширяемость

### Оцени, насколько легко:

#### Добавить новый тип сущности

**Сейчас это будет дорого.** Потребуется затронуть:

```
AnimalType          ← Добавить новые поля
SpeciesRegistry     ← Регистрация нового типа
EntityContainer     ← Поддержка новой классификации
Cell                ← Новые query methods
Island              ← Обновление getProtectionMap, statistics
StatisticsService   ← Поддержка нового типа в подсчётах
Animal / Organism   ← Возможно, новые методы
View                ← Если есть отображение
Services            ← Feeding, Reproduction, Movement
```

**Итого:** Не просто registration, а цепочка каскадных изменений.

#### Изменить правила взаимодействия

**Например, добавить:**
- Территориальность
- Заражение / болезни
- Климатические зоны
- Сезоны (уже есть, но hard-coded)
- Миграционное давление
- City-like service coverage

**Текущая модель плохо подходит,** потому что правила живут:
- Частично в сущностях (`Animal.canEat()`)
- Частично в `Island` (`moveOrganism()`)
- Частично в `StatisticsService` (death tracking)
- Частично в `AnimalType` (hunt probabilities)

**Бизнес-правила не в одном месте.**

#### Добавить новую механику (ресурсы, экономика, погода)

**Потребуется встраивание ещё одного слоя в уже перегруженный `Island`.**

Это верный путь к "framework collapse": класс начнёт разрастаться до недопустимого размера.

**Пример:** Добавление экономики потребует:
- `Currency` tracking
- `TradeRoute` между клетками
- `MarketPrice` dynamics
- `Transaction` history

Куда это встроить? В `Island`? В `Cell`? В новый сервис?

#### Использовать этот код в другой игре с другим доменом

**Почти наверняка придётся переписывать ядро.**

Особенно:
- `Island` как world model (тороидальная сетка)
- `Cell` как occupant container (зоологическая индексация)
- `AnimalType` как единственный type descriptor
- `StatisticsService` как population tracker (animal vs biomass)
- `redBookProtectionEnabled` как доменно-специфичный policy switch

### Укажи, где потребуется переписывание ядра

| Компонент | Степень переписывания | Причина |
|-----------|----------------------|---------|
| `Island` | 🔴 Полное | Заточен под grid + toroidal topology |
| `Cell` | 🔴 Полное | Зоологическая индексация |
| `AnimalType` | 🔴 Полное | Predator/prey/cold-blooded flags |
| `SpeciesKey` | 🟡 Частичное | Можно обобщить до `EntityType` |
| `GameLoop` | 🟢 Минимальное | Tickable abstraction универсален |
| `Tickable` | 🟢 Никакого | Уже абстрактный интерфейс |
| `SimulationNode` | 🟡 Частичное | Нужно убрать animal-specific methods |
| `StatisticsService` | 🟡 Частичное | Обобщить до event aggregator |

---

## 8. Потенциальные баги и риски

### Конкретные подозрительные места

#### `GameLoop.stop()`
```java
public void stop() {
    if (!running) {
        return;
    }
    running = false;
    taskExecutor.shutdown();
}
```

**Проблема:**
- Сам `Thread` из `start()` не хранится и не join-ится
- `run()` может продолжать жить до следующей итерации
- `taskExecutor` в показанном коде не используется, но всё равно shutdown вызывается

**Риск:** Непредсказуемый shutdown lifecycle.

#### `EntityContainer.removeBiomass(Biomass b)`
```java
public boolean removeBiomass(Biomass b) {
    if (biomassBySpecies.remove(b.getSpeciesKey()) != null) {
        allBiomass.remove(b);
        return true;
    }
    return false;
}
```

**Проблема:**
Если в карте уже лежит другой `Biomass` под тем же ключом, удаление по ключу может снести не тот объект-состояние.

**Это выглядит как потенциальная модельная ошибка,** если biomass агрегируется по виду.

#### `Cell.addBiomass(SpeciesKey key, long amount)`
```java
public boolean addBiomass(SpeciesKey key, long amount) {
    rwLock.writeLock().lock();
    try {
        Biomass existing = container.getBiomass(key);
        if (existing != null) {
            existing.addBiomass(amount, this);
            return true;
        }
        return false; // ❌ Возвращает false, если biomass ещё не существует
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

**Проблема:**
`Island.moveBiomassPartially(...)` вызывает `to.addBiomass(...)` и по результату решает, переносить ли массу. Это делает новую biomass-пересылку зависимой от существования контейнера в target cell.

**Гипотеза:** При первом заселении biomass в клетке возможна логическая потеря ресурса или необходимость предварительной инициализации контейнера.

#### `Island.moveOrganism(...)` — Fallback Kill
```java
if (to.canAccept(animal)) {
    if (from.removeAnimal(animal)) {
        if (!to.addAnimal(animal)) {
            if (!from.addAnimal(animal)) {
                // ❌ FORCED KILL as fallback!
                animal.tryConsumeEnergy(animal.getCurrentEnergy());
                reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
            }
        }
    }
}
```

**Сильный риск при failover:**
- Remove из source прошёл
- Add в target не прошёл
- Fallback return to source тоже не прошёл
- Тогда объект forcibly killed

**Это бизнес-решение, которое может скрывать баги вместо их обнаружения.** Для production-grade simulation это опасно: потеря сущности происходит как side effect of contention/capacity overflow.

#### `Island.getGlobalSatiety()` — Division by Zero Risk
```java
public double getGlobalSatiety() {
    // ...
    return (animalCount == 0) ? 100.0 : (totalCurrent / totalMax) * 100.0;
}
```

**Проблема:**
Если `totalMax == 0` (все животные с нулевой max energy), получится `NaN`/`Infinity`.

**Гипотеза:** Сейчас это маловероятно, но при добавлении сущностей с нулевой `maxEnergy` риск станет реальным.

#### `Organism.setEnergy(long energy)` — Race Condition
```java
public void setEnergy(long energy) {
    energyLock.lock();
    try {
        this.currentEnergy = Math.min(energy, maxEnergy);
        if (this.currentEnergy == 0 && isAlive) {
            isAlive = false; // ❌ Меняет isAlive без проверки на уже мёртвое
        }
    } finally {
        energyLock.unlock();
    }
}
```

**Проблема:**
- В другом месте `tryConsumeEnergy(...)` синхронизирован
- Но `isAlive` может быть изменён из другого потока concurrently

**Это гонка состояний.**

#### `EntityContainer.getAllAnimals()/getPredators()/...` — Mutable Leak
```java
@Getter private final Set<Animal> allAnimals = new LinkedHashSet<>();
@Getter private final Set<Animal> predators = new LinkedHashSet<>();

// Любой внешний caller может сделать:
cell.getContainer().getAllAnimals().clear(); // ❌ Нарушает инварианты!
```

**Проблема:**
Возвращают внутренние mutable collections. Любой внешний caller может нарушить инварианты контейнера.

---

## 9. Приоритетные улучшения

### [HIGH] — Критично для production / engine reusability

1. **Выделить engine-core из domain model**
   
   `Island`, `Cell`, `GameLoop`, `StatisticsService` должны быть разнесены по слоям. Игровые правила — вынесены в стратегии/системы.
   
   **Цель:** `Island` становится implementor of `WorldGrid`, а не god object.

2. **Убрать публичную утечку mutable collections**
   
   `EntityContainer` не должен отдавать внутренние списки напрямую.
   
   **Решение:**
   ```java
   public Stream<Animal> streamAnimals() {
       return Collections.unmodifiableSet(allAnimals).stream();
   }
   
   public int getAnimalCount() {
       return allAnimals.size();
   }
   ```

3. **Унифицировать lifecycle и threading**
   
   `GameLoop` должен принимать executor/scheduler извне и реально использовать его либо удалить лишний.
   
   **Решение:**
   ```java
   public GameLoop(long tickDurationMs, ExecutorService executor, Scheduler scheduler) {
       this.executor = executor;
       this.scheduler = scheduler;
   }
   ```

4. **Разорвать зависимость `Island` от конкретного сценария**
   
   Red-book policy, satiety, starving threshold, toroidal cell access должны стать подключаемыми policies.
   
   **Решение:**
   ```java
   public interface ProtectionPolicy {
       Map<SpeciesKey, Integer> getProtectionModifiers(WorldSnapshot snapshot);
   }
   
   public interface SatietyCalculator {
       double calculate(WorldSnapshot snapshot);
   }
   ```

### [MEDIUM] — Важно для maintainability

1. **Вынести статистику в event-driven aggregator**
   
   Вместо частых full scans по сетке.
   
   **Решение:**
   ```java
   public class EventDrivenStatistics {
       private final Map<SpeciesKey, AtomicInteger> counts = new ConcurrentHashMap<>();
       
       @Subscribe
       public void onBirth(BirthEvent event) {
           counts.computeIfAbsent(event.getSpeciesKey(), k -> new AtomicInteger(0))
                 .incrementAndGet();
       }
       
       @Subscribe
       public void onDeath(DeathEvent event) {
           counts.get(event.getSpeciesKey()).decrementAndGet();
       }
   }
   ```

2. **Разделить `Cell` на компоненты**
   
   `CellState` + `CellIndex` + `CellLock` + `CellQueries`
   
   **Решение:**
   ```java
   public final class Cell implements SimulationNode {
       private final CellState state;      // Entities storage
       private final CellIndex index;      // Predators/herbivores indices
       private final CellLock lock;        // ReadWriteLock wrapper
       private final CellQueries queries;  // Query methods
   }
   ```

3. **Упростить `moveOrganism` через transactional move service**
   
   **Решение:**
   ```java
   public interface MovementService {
       MovementResult move(Animal animal, Node from, Node to);
   }
   
   public record MovementResult(boolean success, String reason) {
       public static MovementResult rejected(String reason) {
           return new MovementResult(false, reason);
       }
   }
   ```

4. **Сделать `AnimalType` immutable config object**
   
   Без смешения mechanics flags и balance data.
   
   **Решение:** Разделить на:
   - `AnimalConfig` — weight, maxEnergy, lifespan
   - `BehaviorFlags` — isPredator, isPackHunter, isColdBlooded
   - `BalanceParams` — reproductionChance, presenceChance

5. **Добавить явные interfaces для movement, reproduction, mortality, protection**
   
   См. раздел 6 (Тестируемость).

### [LOW] — Nice to have

1. **Переименовать методы и классы для точности**
   
   - `EntityContainer` → `CellEntityIndex`
   - `getPlantCount()` → `getBiomassAmountAsInt()`
   - `taskExecutor` → удалить или переименовать в `parallelExecutor`

2. **Убрать неиспользуемые параметры**
   
   - `Animal.isProtected(int currentTick)` → `isProtected()`
   - `Island.getProtectionMap(SpeciesRegistry registry)` → убрать параметр

3. **Свести магические константы в policy/config objects**
   
   - Вынести `chunkSize`, hide thresholds в `SimulationConfig`
   - Создать `ProtectionPolicy` с настраиваемыми порогами

4. **Добавить более явные JavaDoc по инвариантам**
   
   - Документировать thread-safety гарантии
   - Описать expected behavior при edge cases

---

## 10. Рефакторинг (с примерами)

### Пример 1. Выделение engine-ядра из доменной логики

#### До

```java
public class Island implements SimulationWorld {
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private boolean redBookProtectionEnabled = true;

    public void moveOrganism(Animal animal, Cell from, Cell to) { 
        // Domain-specific logic mixed with infrastructure
    }
    
    public void reportDeath(SpeciesKey speciesKey, DeathCause cause) { 
        // Direct coupling to StatisticsService
    }
    
    public Map<SpeciesKey, Integer> getProtectionMap(SpeciesRegistry registry) { 
        // Red book policy hardcoded here
    }
    
    public double getGlobalSatiety() { 
        // Full scan every call
    }
}
```

#### После

```java
// === ENGINE CORE ===

public interface WorldGrid {
    Optional<Node> neighborOf(Node node, int dx, int dy);
    List<? extends Node> workUnits();
    int getWidth();
    int getHeight();
}

public interface PopulationEventSink {
    void onBirth(EntityType key);
    void onRemoval(EntityType key);
    void onDeath(EntityType key, DeathCause cause);
    int getCount(EntityType key);
}

public interface ProtectionPolicy {
    Map<EntityType, Integer> getProtectionModifiers(WorldSnapshot snapshot);
}

public interface SatietyCalculator {
    double calculate(WorldSnapshot snapshot);
}

public final class SimulationEngine {
    private final WorldGrid world;
    private final PopulationEventSink populationTracker;
    private final ProtectionPolicy protectionPolicy;
    private final List<SimulationSystem> systems;

    public SimulationEngine(WorldGrid world, 
                           PopulationEventSink tracker,
                           ProtectionPolicy protection,
                           List<SimulationSystem> systems) {
        this.world = world;
        this.populationTracker = tracker;
        this.protectionPolicy = protection;
        this.systems = systems;
    }

    public void tick(long tick) {
        for (SimulationSystem system : systems) {
            system.update(world, tick);
        }
    }
}

// === DOMAIN IMPLEMENTATION ===

public class Island implements WorldGrid {
    private final Cell[][] grid;
    private final List<Chunk> chunks;
    
    // No more StatisticsService, ProtectionService, etc.
    // Those are injected as separate interfaces
}

public class RedBookProtectionPolicy implements ProtectionPolicy {
    @Override
    public Map<EntityType, Integer> getProtectionModifiers(WorldSnapshot snapshot) {
        // Policy logic isolated from world model
    }
}
```

**Что это даёт:**

- `Island` перестаёт быть и миром, и политикой, и статистикой
- Можно подключить другой world model: city grid, factory floor, dungeon map, transport network
- Policies становятся заменяемыми модулями
- Engine core не знает о конкретных доменных сущностях

---

### Пример 2. Введение абстракции для перемещения и транзакции сущностей

#### До

```java
public void moveOrganism(Animal animal, Cell from, Cell to) {
    Cell first = (from.getX() < to.getX() || 
                 (from.getX() == to.getX() && from.getY() < to.getY())) ? from : to;
    Cell second = (first == from) ? to : from;

    first.getLock().lock();
    try {
        second.getLock().lock();
        try {
            if (from.removeAnimal(animal)) {
                if (!to.addAnimal(animal)) {
                    if (!from.addAnimal(animal)) {
                        // ❌ Forced kill as fallback!
                        animal.tryConsumeEnergy(animal.getCurrentEnergy());
                        reportDeath(animal.getSpeciesKey(), 
                                   DeathCause.MOVEMENT_EXHAUSTION);
                    }
                }
            }
        } finally {
            second.getLock().unlock();
        }
    } finally {
        first.getLock().unlock();
    }
}
```

#### После

```java
public interface MovementPolicy<T extends Organism> {
    MovementResult move(T entity, CellHandle from, CellHandle to);
}

public record MovementResult(boolean success, 
                            String rejectionReason,
                            Optional<Throwable> error) {
    public static MovementResult applied() {
        return new MovementResult(true, null, Optional.empty());
    }
    
    public static MovementResult rejected(String reason) {
        return new MovementResult(false, reason, Optional.empty());
    }
    
    public static MovementResult failed(Throwable error) {
        return new MovementResult(false, "SYSTEM_ERROR", Optional.of(error));
    }
}

public final class DefaultMovementPolicy implements MovementPolicy<Animal> {
    private final CellRepository cells;
    private final PopulationEventSink events;
    private final CapacityChecker capacityChecker;

    @Override
    public MovementResult move(Animal entity, CellHandle from, CellHandle to) {
        // Validate before any state changes
        if (!capacityChecker.canAccept(to, entity)) {
            return MovementResult.rejected("TARGET_CAPACITY_EXCEEDED");
        }
        
        // Atomic transaction
        try {
            cells.remove(from, entity);
            cells.add(to, entity);
            return MovementResult.applied();
        } catch (Exception e) {
            // Rollback or report, but DON'T kill entity silently
            cells.add(from, entity); // Attempt rollback
            return MovementResult.failed(e);
        }
    }
}

// Usage in service
public class MovementService extends AbstractService<SimulationNode> {
    private final MovementPolicy<Animal> movementPolicy;
    
    @Override
    public void processCell(SimulationNode node, int tickCount) {
        node.forEachAnimal(animal -> {
            if (shouldMove(animal, tickCount)) {
                SimulationNode target = selectTarget(node, animal);
                MovementResult result = movementPolicy.move(
                    animal, 
                    node.asCellHandle(), 
                    target.asCellHandle()
                );
                
                if (!result.success()) {
                    log.debug("Movement failed: {}", result.rejectionReason());
                }
            }
        });
    }
}
```

**Что это даёт:**

- Убирается "самоубийство" как fallback поведения
- Перемещение становится тестируемой транзакцией
- Правила можно заменить для другой игры (например, teleportation, portals)
- Ошибки явно возвращаются, а не скрываются

---

### Пример 3. Разделение storage и query API в `Cell`

#### До

```java
public class Cell implements SimulationNode {
    private final EntityContainer container = new EntityContainer();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public List<Animal> getAnimals() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllAnimals()); // Allocates every call
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public List<Animal> getPredators() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getPredators()); // Allocates every call
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    // ... 10+ similar methods
}
```

#### После

```java
// === READ VIEW (Query API) ===

public interface CellReadView {
    int animalCount();
    int predatorCount();
    int herbivoreCount();
    
    Stream<Animal> streamAnimals();
    Stream<Animal> streamPredators();
    Stream<Animal> streamHerbivores();
    
    Optional<Animal> findAnimal(Predicate<Animal> predicate);
    
    default boolean hasPredators() {
        return predatorCount() > 0;
    }
}

// === WRITE PORT (Command API) ===

public interface CellWritePort {
    boolean add(Animal animal);
    boolean remove(Animal animal);
    boolean add(Biomass biomass);
    boolean remove(Biomass biomass);
    
    void removeDead(Consumer<Animal> onRemoved);
}

// === IMPLEMENTATION ===

public final class Cell implements CellReadView, CellWritePort, SimulationNode {
    private final CellState state;      // Internal storage
    private final CellIndex index;      // Optimized indices
    private final CellLock lock;        // Concurrency control
    
    @Override
    public int animalCount() {
        lock.readLock();
        try {
            return state.animalCount();
        } finally {
            lock.readUnlock();
        }
    }
    
    @Override
    public Stream<Animal> streamAnimals() {
        lock.readLock();
        try {
            return state.streamAnimals(); // Returns stream, not copy
        } finally {
            lock.readUnlock();
        }
    }
    
    @Override
    public boolean add(Animal animal) {
        lock.writeLock();
        try {
            boolean added = state.add(animal);
            if (added) {
                index.reindex(animal);
            }
            return added;
        } finally {
            lock.writeUnlock();
        }
    }
}
```

**Что это даёт:**

- Read path можно оптимизировать отдельно (streams вместо копий)
- Write path перестаёт быть привязан к внутренней структуре списков
- Проще тестировать и профилировать
- Interface segregation: клиенты берут только нужный им API

---

## 11. Итоговая оценка

| Категория | Оценка | Комментарий |
|-----------|--------|-------------|
| **Архитектура** | 5/10 | Смесь стилей, нет чёткого разделения layers, domain leakage |
| **Код** | 6/10 | Рабочий код, но есть anti-patterns, duplication, magic numbers |
| **Переиспользуемость** | 3/10 | Сильная привязка к островному сценарию, zoo-specific abstractions |
| **Тестируемость** | 4/10 | Hard-coded dependencies, thread creation, mutable leaks |
| **Масштабируемость** | 5/10 | Параллелизм не доведён до конца, global scans, lock contention risks |
| **Общая оценка** | **5/10** | Good foundation, needs refactoring to become an engine |

---

### Короткий вердикт

**Проект ещё не готов стать основой универсального симуляционного движка.**

**Причина не в отсутствии отдельных хороших идей** — они есть:

✅ Тик-ориентированная модель (`Tickable`, `GameLoop`)  
✅ Registry/flyweight подход (`SpeciesRegistry`, `AnimalType`)  
✅ Попытка разделить services (`FeedingService`, `MovementService`, etc.)  
✅ Внимание к concurrency (ReadWriteLock, parallel processing)  
✅ Deterministic simulation hooks (seed-based random)

**Проблема в том, что эти идеи не собраны в устойчивое engine-core:**

❌ Домен острова слишком глубоко прошит в ядро  
❌ `Island` и `Cell` перегружены обязанностями (God Objects)  
❌ Статистика и policies смешаны с моделью мира  
❌ Структура данных ориентирована на текущий сценарий, а не на расширяемую платформу  
❌ Нет чёткого разделения на engine layer / domain layer / infrastructure layer

---

### Roadmap к engine-ready состоянию

**Phase 1: Extract Core Interfaces (2-3 недели)**
- Выделить `WorldGrid`, `PopulationTracker`, `MovementPort`
- Создать `SimulationEngine` с injectable policies
- Убрать mutable collection leaks

**Phase 2: Decouple Domain (3-4 недели)**
- Разделить `AnimalType` на config/flags/balance
- Вынести statistics в event-driven aggregator
- Создать `MovementPolicy`, `ReproductionStrategy` interfaces

**Phase 3: Improve Testability (2 недели)**
- Inject executor/scheduler в `GameLoop`
- Добавить mock-friendly constructors
- Покрыть critical paths unit tests

**Phase 4: Optimize Performance (2-3 недели)**
- Заменить full scans на incremental aggregates
- Оптимизировать cell queries (streams вместо копий)
- Profile и улучшить parallel processing

**Total: ~10-12 недель до engine-ready состояния**

---

## Ключевые файлы, на которые опирается review

| Файл | Строк | Проблемы |
|------|-------|----------|
| `src/main/java/com/island/engine/GameLoop.java` | 189 | Unused executor, no phase separation |
| `src/main/java/com/island/model/Cell.java` | 428 | God object, mutable leaks, allocation-heavy |
| `src/main/java/com/island/model/EntityContainer.java` | 138 | Multiple indices, mutable exposure |
| `src/main/java/com/island/model/Island.java` | 290 | God object, domain leakage |
| `src/main/java/com/island/content/Organism.java` | 141 | Mixed responsibilities |
| `src/main/java/com/island/content/Animal.java` | 88 | Domain-specific methods |
| `src/main/java/com/island/content/AnimalType.java` | 90 | Too many fields, zoo-specific |
| `src/main/java/com/island/content/SpeciesRegistry.java` | 68 | Acceptable, could be more generic |
| `src/main/java/com/island/service/StatisticsService.java` | 126 | Animal/biomass distinction |
| `src/main/java/com/island/service/FeedingService.java` | 219 | Domain logic in service |
| `src/main/java/com/island/service/MovementService.java` | 109 | Acceptable structure |
| `src/main/java/com/island/service/LifecycleService.java` | 72 | Acceptable structure |

---

*Review completed based on branch `dev` commit state.*
