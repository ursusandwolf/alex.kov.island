# Code Review — `alex.kov.island` · branch `dev`

**Reviewer:** Staff Engineer / Tech Lead  
**Date:** 2026-04-30  
**Stack:** Java 21, Maven, JUnit 5, Mockito, Lombok  
**Scope:** Полный архитектурный и код-ревью ветки `dev`  
**Контекст:** Проект рассматривается как основа переиспользуемого simulation engine.

---

## 🔍 1. Общий обзор проекта

### Что моделируется
Экосистема острова: хищники, травоядные, растения, насекомые (Caterpillar/Butterfly как биомасса). Сущности перемещаются по grid-у, едят, стареют, размножаются. Реализован цикл "биологического маятника" для насекомых.

### Насколько доменная модель привязана к сценарию «остров»
**Привязанность — умеренная, но значимая.** Видно, что авторы целенаправленно вводили абстракции (`SimulationWorld`, `SimulationNode`, `Tickable`), однако decoupling не доведён до конца:

- `SimulationView.display(Island island)` — интерфейс на уровне engine принимает конкретный доменный класс. Это **финальная миля**, которая не пройдена.
- `TaskRegistry.registerAll()` содержит `world instanceof com.island.model.Island island` — island-специфичная ветка прямо в engine-коде.
- `ConsoleView` импортирует `Island`, `Cell`, `Animal`, `Biomass`, `DeathCause`, `SpeciesKey` — весь домен целиком.
- `InteractionMatrix.buildFrom()` хардкодит `SpeciesKey.PLANT`, `SpeciesKey.GRASS`, `SpeciesKey.CABBAGE` как fallback.
- `GenericAnimal` — метаболизм herbivore определяется через `canEat(SpeciesKey.PLANT) || canEat(SpeciesKey.GRASS) || canEat(SpeciesKey.CABBAGE)` — прямые ссылки на конкретные виды острова.

### Переиспользуемость текущих абстракций

| Абстракция | Переиспользуема? | Комментарий |
|---|---|---|
| `GameLoop` | ✅ | Domain-agnostic, принимает `Tickable` |
| `AbstractService` | ✅ | Принимает `SimulationWorld` — engine уровень |
| `SimulationWorld` | ✅ | Хороший интерфейс, почти чистый |
| `SimulationNode` | ⚠️ | Есть, но все сервисы сразу кастуют к `Cell` |
| `InteractionProvider` | ✅ | Минималистичный и чистый |
| `HuntingStrategy` | ✅ | Чистый Strategy pattern |
| `SimulationView` | ❌ | `display(Island)` — domain-coupled |
| `SpeciesKey` | ⚠️ | Static registry не позволяет иметь два домена в JVM |
| `ObjectPool` | ✅ | Полностью универсален |

### Протекание домена в технические детали
- **`FeedingService`** — per-cell вычисляет `getWorld().getProtectionMap(speciesRegistry)`. Это одновременно и performance проблема, и domain logic внутри сервисного слоя.
- **`ConsoleView`** — прямо итерирует `island.getGrid()[x][y]`, знает структуру grid.
- **`SimulatorMain.monitor()`** — фильтрует виды по `isBiomass` — domain знание в entry point.

---

## 🏗 2. Архитектура и дизайн

### Архитектурный стиль
**Слоистая архитектура с элементами Service-Oriented Design и попыткой выделения Engine-слоя.**

```
engine/     ← SimulationWorld, SimulationNode, Tickable, GameLoop, Bootstrap
model/      ← Island, Cell, Chunk, EntityContainer  
content/    ← Animal, Biomass, Organism, SpeciesKey, AnimalType
service/    ← AbstractService + все сервисы
view/       ← SimulationView (interface), ConsoleView
util/       ← RandomProvider, InteractionProvider, ObjectPool
config/     ← SimulationConstants, Configuration, EnergyPolicy
```

Слоистость правильная. Но **барьеры между слоями не защищены**: нет Java модулей, нет package-private ограничений — любой класс может импортировать что угодно из любого слоя.

### Соответствие уровню «движка»
**На 70%.** Engine-фундамент заложен (`SimulationWorld`, `Tickable`, `AbstractService`), но последние шаги не сделаны. Ключевой индикатор: если заменить `Island` на `FarmWorld implements SimulationWorld`, `TaskRegistry` сломается из-за `instanceof Island` cast, а `SimulationView` не сможет отрисовать ничего.

---

### SOLID

#### S — Single Responsibility

❌ **`FeedingService.processCell()`** — выполняет: pack hunting, individual hunting, herbivore feeding, LOD sampling, ROI check, protection check, fatigue penalty. Это минимум 5 ответственностей в одном методе. При добавлении любой новой механики (яд, капкан, союзники) метод растёт дальше.

❌ **`Island`** — несмотря на выделение `StatisticsService`, остаётся ответственным за: хранение grid, управление чанками, движение организмов, вычисление protection map, toroidal/non-toroidal boundary logic, инициализацию соседей. Это 5+ ответственностей.

✅ **`StatisticsService`** — корректно изолирован, single responsibility соблюдено.  
✅ **`EntityContainer`** — корректно отвечает только за хранение и индексацию сущностей в клетке.

#### O — Open/Closed

❌ **`InteractionMatrix.buildFrom()`** — хардкодит `SpeciesKey.PLANT → GRASS + CABBAGE` расширение:
```java
if (preyKey.equals(SpeciesKey.PLANT)) {
    matrix.setChance(predatorKey, SpeciesKey.GRASS, chance);
    matrix.setChance(predatorKey, SpeciesKey.CABBAGE, chance);
}
```
Добавление нового «родового» ключа (например, `FISH` как категория всех рыб) требует правки этого метода.

❌ **`ReproductionService.tryReproduce()`** — шансы воспроизводства захардкожены по `SizeClass`:
```java
double chance = switch (type.getSizeClass()) {
    case TINY  -> 0.25;
    case SMALL -> 0.18;
    case NORMAL -> 0.12;
    case MEDIUM -> 0.08;
    case LARGE -> 0.04;
    case HUGE  -> 0.02;
};
```
Это domain knowledge прямо в сервисе. Должно быть в `AnimalType.reproductionChance` и в `species.properties`. Добавление нового `SizeClass` — обязательная правка этого switch.

❌ **`GenericAnimal`** — определяет herbivore-статус через прямые ссылки на конкретные виды:
```java
this.isHerbivore = type.canEat(SpeciesKey.PLANT)
                || type.canEat(SpeciesKey.GRASS)
                || type.canEat(SpeciesKey.CABBAGE);
```
Добавление нового растения без флага `isPlant` не будет учтено метаболизмом.

#### L — Liskov Substitution

❌ **`Butterfly.addBiomass()`** нарушает контракт родителя `Biomass`:
```java
// Biomass (родитель) — гарантирует biomass <= maxBiomass
public void addBiomass(double amount) {
    this.biomass = Math.min(maxBiomass, this.biomass + amount);
}

// Butterfly (наследник) — удаляет гарантию
public void addBiomass(double amount) {
    this.biomass += amount; // НЕТ cap — инвариант сломан
}
```
Клиент, работающий через `Biomass`, ожидает `biomass <= maxBiomass`. Butterfly нарушает это. При `maxBiomass == 0` (когда Butterfly создаётся с нулевой массой) — biomass растёт неограниченно.

⚠️ **`Organism implements Poolable`** — смешение доменной модели организма с инфраструктурным контрактом (`reset()`). Organisм должен знать о своём жизненном цикле, но не о своём пуле переиспользования. Это нарушение separation of concerns, пограничное с LSP.

#### I — Interface Segregation

⚠️ **`SimulationView.setSilent(boolean)`** — implementation detail конкретной консольной реализации присутствует в интерфейсе. GUI-реализация или headless-тест должны реализовывать бессмысленный метод.

✅ **`InteractionProvider`** — минималистичный интерфейс, только `getChance` и `hasAnimalPrey`. Образцовый пример ISP.

#### D — Dependency Inversion

❌ **`SimulationView.display(Island island)`** — высокоуровневый интерфейс зависит от конкретного доменного класса `Island`. Должно быть `display(WorldSnapshot snapshot)`.

❌ **`TaskRegistry.registerAll()`**:
```java
gameLoop.addRecurringTask(() -> {
    if (world instanceof com.island.model.Island island) { // ← DIP нарушен
        view.display(island);
    }
});
```
Engine-уровень (`TaskRegistry`) знает про конкретный `Island`. Если `world` — не `Island`, view не вызывается вообще. Для другого домена рендеринг молча отключается.

❌ **`SimulationBootstrap`** жёстко создаёт `ConsoleView`. Нет DI — нельзя инжектировать другой view без правки Bootstrap.

---

### Антипаттерны

| Антипаттерн | Место | Инженерный аргумент |
|---|---|---|
| **Hollow Abstraction** | `SimulationNode` в сервисах | Введён как абстракция, но все 5 сервисов немедленно кастуют к `Cell`. Если node не `Cell` — action молча пропускается. Хуже прямой зависимости |
| **Domain Leak** | `SimulationView.display(Island)` | View-интерфейс в engine-пакете знает про domain-класс |
| **Instanceof Chain** | `TaskRegistry` → `instanceof Island` | Engine пробивает собственную абстракцию |
| **Dangerous Pool Reset** | `Organism.reset()` | `currentEnergy = 0` без полного сброса; use-before-init не защищён |
| **Static Mutable Registry** | `SpeciesKey.REGISTRY` | `HashMap` не thread-safe при concurrent `fromCode()` |
| **Config в Code** | `ReproductionService` switch | Шансы воспроизводства — domain config, не code |

---

### Coupling / Cohesion

**Высокий coupling:**
- `ConsoleView` → `Island`, `Cell`, `Animal`, `Biomass`, `DeathCause`, `SpeciesKey`, `ViewUtils` (10+ зависимостей)
- `FeedingService` → `SimulationWorld`, `AnimalFactory`, `InteractionProvider`, `SpeciesRegistry`, `HuntingStrategy`, `PreyProvider`, `Cell`, `Animal`, `Biomass`, `DeathCause`, `SpeciesKey` (11 зависимостей)

**Нарушение cohesion:**
- `Island` отвечает за grid storage, chunk partitioning, movement logic, boundary logic, protection map calculation, neighbor initialization
- `FeedingService.tryEat()` — ROI check, protection check, population density bonus, energy gain, death reporting — всё в одном методе

---

## ⚙️ 3. Алгоритмы и логика симуляции

### Game Loop

```
GameLoop.runTick()
  → island.tick()          // tickCount + statisticsService.onTickStarted()
  → LifecycleService        // метаболизм, старение, рост биомассы
  → FeedingService          // охота, еда
  → MovementService         // движение животных и биомассы
  → ReproductionService     // размножение
  → CleanupService          // удаление мёртвых, возврат в пул
  → ConsoleView.display()   // рендеринг
```

Каждый сервис параллелизуется по chunk-ам через `ExecutorService`. GameLoop сам по себе **универсален** — работает со списком `Tickable`. Порядок `Tickable` жёстко задаётся в `TaskRegistry` без возможности приоритизации или условного выполнения.

### Универсальность loop
`GameLoop` + `AbstractService` + `Tickable` — полностью domain-agnostic. Для другого домена достаточно создать новые `Tickable`-сервисы. Проблема в `TaskRegistry`, который создаёт конкретные island-сервисы без механизма замены.

### Лишние вычисления — критическое место

❌ **`getProtectionMap()` вычисляется PER CELL:**
```java
// FeedingService.processCell() — вызывается N раз на острове N×M
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) {
        this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // КАЖДЫЙ РАЗ
```

`getProtectionMap()` итерирует все виды и вызывает `getSpeciesCount()` для каждого. На острове 20×20 = 400 ячеек → 400 × 20 видов = 8000 итераций за тик только для этого вычисления. Должно вычисляться **один раз** в начале тика.

❌ **`Island.getSpeciesCounts()`** — O(W × H × biomass containers):
```java
// Вызывается каждый тик в ConsoleView
for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        for (Biomass b : grid[x][y].getBiomassContainers()) { // полный scan
```
Biomass не отслеживается инкрементально в `StatisticsService`. При 20×20 — 400 ячеек × N biomass containers = тысячи итераций на каждый display-тик.

❌ **`Island.getGlobalSatiety()` / `getStarvingCount()`** — O(W × H × animals per cell). Вызываются каждый тик из `ConsoleView`. Не кэшируются.

### LOD (`forEachSampled`) — новая механика

```java
protected <T> void forEachSampled(List<T> list, int limit, Consumer<T> action) {
    int step = (size > limit) ? (size / limit + 1) : 1;
    for (int i = 0; i < size; i += step) { // всегда начинает с i=0
        action.accept(list.get(i));
    }
}
```

**Концептуально правильно**, но выборка детерминирована: всегда элемент 0, step, 2×step... Животные в начале `ArrayList` получают **систематическое преимущество** — они всегда едят и размножаются, животные в конце — нет. Нужен случайный стартовый offset.

### Big-O при росте сущностей

| Операция | Сложность | Порог проблемы |
|---|---|---|
| `getProtectionMap()` per cell | O(S × W × H) per tick | Любой остров |
| `getSpeciesCounts()` biomass | O(W × H × B) | 2000+ клеток |
| `getGlobalSatiety()` | O(W × H × A) | 2000+ животных |
| `EntityContainer.fastRemove` | O(N) indexOf | N > 100 в клетке |
| `PreyProvider.buildBuffet()` sort | O(N log N) | Допустимо |

---

## 📦 4. Коллекции и структуры данных

### ObjectPool — `AnimalFactory` + `ObjectPool<Animal>`

Концептуально правильно: `ConcurrentLinkedQueue`, thread-safe, GC pressure снижается. Но структурная проблема: **`Organism.reset()` не сбрасывает полное состояние**:

```java
public void reset() {
    this.isAlive = true;
    this.age = 0;
    this.currentEnergy = 0; // установлен в 0, не в корректное начальное значение
    // maxEnergy, maxLifespan, isHiding — НЕ сбрасываются
}
```
`init()` вызывается сразу после `acquire()`, но если между ними произойдёт исключение — объект с `energy=0` попадёт в клетку и умрёт при первом метаболизме без диагностики.

**Поля `Organism` потеряли `final` ради пула:**
```java
private double maxEnergy;    // не final — можно изменить извне
private int maxLifespan;     // не final — можно изменить извне
```
Внешний код может случайно вызвать `init()` на живом организме, полностью изменив его параметры.

### `EntityContainer`
Правильный выбор: мультиплексированное хранение по `AnimalType`, `SizeClass`, роль (predators/herbivores). `fastRemove` через swap-with-last — O(1) удаление. Небольшое замечание: `countBySpecies()` итерирует по всем `AnimalType` записям — O(types). Приемлемо при малом числе типов.

### `SpeciesRegistry` с Lombok `@RequiredArgsConstructor`
Конструктор через Lombok не оборачивает maps в `Collections.unmodifiableMap()`:
```java
@RequiredArgsConstructor
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes; // mutable!
```
Передаётся ссылка на оригинальный HashMap из `SpeciesLoader`. Внешний код может модифицировать "immutable registry" через ту же ссылку.

### Лишние аллокации
- `cell.getAnimals()` / `getPredators()` / `getHerbivores()` — каждый вызов создаёт `new ArrayList<>()`. Методы вызываются из каждого сервиса на каждой клетке каждый тик.
- `PreyProvider` создаётся per-cell per-tick (до 2 инстансов: pack + regular). На 400 клетках = 800 объектов/тик для GC.
- `ConsoleView.updateHistory()` вызывает `island.getSpeciesCounts()` — тот же O(W×H) traversal, который затем повторяется в `renderStatsWithGraphs`.

---

## 🧱 5. Качество кода

### Нейминг

| Проблема | Место | Что означает на самом деле |
|---|---|---|
| `getPlantCount()` | `Cell.java` | Возвращает суммарный **вес** биомассы, не количество растений |
| `getPercent()` | `EnergyPolicy` | Возвращает абсолютное число (70.0), не процент от чего-то |
| `isAnimalPredator()` | `Animal.java` | Избыточное "Animal" в методе класса Animal |
| `shouldAct()` | `FeedingService` | Проверяет "can cold-blooded act this tick" — не "should act" |
| `minPackSize` | `FeedingService` | Используется как порог включения pack-режима, не минимальный размер стаи |
| `tryConsumeEnergy()` | `Organism` | Метод с "try" всегда выполняет side effect (убивает при 0 энергии) |

### Дублирование

`shouldMove()` в `MovementService` и `shouldAct()` в `FeedingService` — идентичная логика "cold-blooded skip ticks" с разными константами (2 vs 3):
```java
// MovementService
if (animal.getAnimalType().isColdBlooded()) return (tickCount % 2 == 0);

// FeedingService  
return !animal.getAnimalType().isColdBlooded() || (tickCount % 3 == 0);
```
Разные пороги могут быть намеренными, но нигде не задокументированы и не вынесены в конфиг.

### Магические числа

```java
// FeedingService.tryEat() — бонус при перенаселённости
chance += 15; // откуда 15?

// FeedingService.tryEat() — штрафы за неудачу
consumer.consumeEnergy(consumer.getMaxEnergy() * 0.05); // хищник
consumer.consumeEnergy(consumer.getMaxEnergy() * 0.03); // травоядное

// Butterfly.processPendulum()
double appetite = biomass * 0.10; // 10% — нет константы

// Bear.java
private static final int FULL_CYCLE = 150;  // в коде, не в species.properties
private static final int SLEEP_PERIOD = 50;

// Island.partitionIntoChunks()
int chunkSize = 20; // не в конфиге
```

### Методы/классы с превышением размера
- `FeedingService.tryEat()` — ~50 строк, смешаны: ROI, protection check, density bonus, energy gain, death reporting, failure penalty
- `ConsoleView.display()` — ~80 строк, рендеринг + история + форматирование цветов + статистика смертей
- `Caterpillar` — pipeline жизненного цикла (`processPendulum` + `advanceStages` + `updateTotalBiomass`) без комментариев к численным константам массивов `[40]` и `[20]`

---

## 🧪 6. Тестируемость

### Где тестирование затруднено

❌ **`Chameleon.isProtected()` — hardcoded static dependency:**
```java
import com.island.util.RandomUtils; // static global

public boolean isProtected(int currentTick) {
    return super.isProtected(currentTick) || RandomUtils.nextDouble() < 0.95;
}
```
`Chameleon` не принимает `RandomProvider` через конструктор. Тест не может контролировать случайность без изменения глобального `RandomUtils.provider`.

❌ **`RandomUtils.setProvider()` без гарантии сброса:**
```java
// DeterminismTest
RandomUtils.setProvider(mockProvider);
// assertions... (если упадут — строка ниже не выполнится)
RandomUtils.setProvider(new DefaultRandomProvider()); // нет try/finally
```
Упавший тест "заражает" следующий тест мок-провайдером.

❌ **`LongTermSurvivalTest` — `System.setProperty` без эффекта:**
```java
System.setProperty("island.width", "10");
System.setProperty("island.height", "10");
// Configuration.load() читает из InputStream, не из System.properties
// Тест всегда работает на 20×20 из species.properties
```

❌ **Интеграционные тесты под видом unit:**
`BoundaryConditionsTest.setUp()` поднимает полную симуляцию через `SimulationBootstrap`. Медленно, нестабильно, при провале не указывает на конкретный компонент.

### Что хорошо тестируется

✅ `LifecycleServiceTest` — правильный unit тест через Mockito mock `SimulationWorld`.  
✅ `FeedingMechanicsTest` — детерминированный `RandomProvider`, моканый `SimulationWorld`.  
✅ `HuntingStrategyTest` — чистый unit через mock `InteractionMatrix`.  
✅ `CellTest` — изолированный тест без full bootstrap.  
✅ `InteractionMatrixTest` — инстанс-уровневый matrix тестируется независимо.

### Где не хватает абстракций для тестирования
- `Island` не имеет собственного интерфейса (кроме `SimulationWorld`) — нельзя мокать для тестов `ConsoleView`
- `Caterpillar` / `Butterfly` — тестируются только интеграционно с реальным `Island`
- `forEachSampled` логика не unit-тестируется отдельно

---

## 🚀 7. Масштабируемость и расширяемость

### Добавить новый тип сущности

**Обычное животное:** `species.properties` + `species.list`. Никаких правок кода.  
**Оценка: ✅ Очень легко.** Data-driven через `GenericAnimal`.

**Новый Biomass-тип:** `isBiomass=true` в `.properties`. Опционально новый класс.  
**Оценка: ✅ Легко.** `PreyProvider` и `WorldInitializer` работают через `getBiomassContainers()`.

**Животное с уникальной механикой (как Bear):** Новый класс + регистрация в `AnimalFactory.creators`.  
**Оценка: ⚠️ Средне.** Hooks (`isHibernating`, `isProtected`) предусмотрены.

### Изменить правила взаимодействия
Вероятности — через `species.properties`. Pack hunting, ROI — правка `FeedingService`.  
**Оценка: ⚠️ Средне.** Данные data-driven, логика — нет.

### Добавить новую механику (погода, ресурсы, экономика)

**Погода:** Новый `WeatherService extends AbstractService` → `TaskRegistry`. Если влияет на метаболизм — нужно добавить weather state в `SimulationWorld`.  
**Оценка: ⚠️ Возможно, требует расширения `SimulationWorld`.**

**Ресурсы/экономика:** Расширение `Cell` / `SimulationNode` новыми полями. Затрагивает все сервисы.  
**Оценка: ❌ Требует переработки модели.**

### Использовать в другой игре с другим доменом

**Блокеры на сегодня:**
1. `SimulationView.display(Island)` — view не работает с другим доменом
2. `TaskRegistry` — `instanceof Island` cast — рендеринг выключается для другого world
3. `instanceof Cell` в каждом сервисе — привязывают к `Cell`-типу
4. `SpeciesKey.REGISTRY` — static — два домена в одном JVM невозможны

Если исправить пункты 1 и 2 — engine становится реально переиспользуемым для нового домена.

### Где потребуется переписывание ядра
1. `SimulationView` + `TaskRegistry` — нужен `WorldSnapshot` интерфейс (~150 строк)
2. `instanceof Cell` в сервисах → перенос методов в `SimulationNode` (~80 строк)
3. `SpeciesKey.REGISTRY` → instance-level registry (среднесрочно)

---

## 🐞 8. Потенциальные баги и риски

### [BUG] `SpeciesLoader.loadEntry()` — plants добавляются в оба реестра

```java
private void loadEntry(SpeciesKey key, Properties props, ...) {
    if (isPlant) {
        plantWeights.put(key, weight);
        plantMaxCounts.put(key, maxCount);
        plantSpeeds.put(key, speed);
        // НЕТ return — код ПРОДОЛЖАЕТСЯ!
    }
    // Grass, Cabbage, Plant — все попадают в animalTypes тоже
    AnimalType type = AnimalType.builder()...build();
    animalTypes.put(key, type);
}
```
`SpeciesRegistry.getAllAnimalKeys()` вернёт Grass и Cabbage среди животных. `AnimalFactory` создаст для них пул. Логика `WorldInitializer` работает только потому что проверка `type.isPlant()` стоит первой — хрупкая защита.

### [BUG] `Butterfly.addBiomass()` — нарушение инварианта `maxBiomass`

```java
@Override
public void addBiomass(double amount) {
    this.biomass += amount; // нет cap по maxBiomass
}
```
`Caterpillar.advanceStages()` вызывает `b.addBiomass(emergingButterflies)` каждый тик. При `maxBiomass == 0` — unbounded growth биомассы бабочек.

### [BUG] `Organism.reset()` — use-before-init при ObjectPool

```java
public T acquire() {
    T obj = pool.poll();
    if (obj == null) return factory.get();
    return obj; // obj.currentEnergy == 0 после reset()
}
// Если init() не вызван из-за exception — животное с energy=0 в клетке
```

### [BUG] `FeedingService.processCell()` — `protectionMap` per-cell (performance)

```java
this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // 400x за тик
```
На острове 20×20 = 400 ячеек → `getProtectionMap()` вызывается 400 раз вместо 1. При 20 видах — 8000 итераций вместо 20.

### [BUG] `LongTermSurvivalTest` — System.setProperty без эффекта
`Configuration.load()` читает из classpath `InputStream`, не из `System.properties`. Тест всегда тестирует остров 20×20.

### [RISK] `SpeciesKey.REGISTRY` — HashMap не thread-safe
Concurrent `fromCode()` из нескольких потоков — race condition при записи в `REGISTRY`. В тестовой среде с параллельными тестами — возможный data corruption.

### [RISK] Непоследовательные границы карты
`Island.getNode()` (используется сервисами) — **не тороидальный**.  
`Island.getCell()` (используется в тестах) — **тороидальный**.  
Животные у края не могут двигаться "за край", но тест `testToroidalBoundaries` проверяет другой метод. Несоответствие нигде не задокументировано.

### [RISK] `energyLock` в `Organism` — потенциально избыточный
`Animal` принадлежит конкретной `Cell`, защищённой `Cell.lock`. `energyLock` создаёт второй уровень синхронизации. При рефакторинге захват в разном порядке `Cell.lock` и `energyLock` из разных потоков — потенциальный deadlock.

---

## 💡 9. Приоритетные улучшения

### [HIGH] Вынести `getProtectionMap()` из `processCell` в начало тика

```java
// FeedingService
@Override
public void tick(int tickCount) {
    this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // 1 раз
    super.tick(tickCount);
}
```
400x ускорение этого вычисления. Одно изменение.

### [HIGH] Исправить `SpeciesLoader.loadEntry()` — добавить `return` для plants

```java
if (isPlant) {
    plantWeights.put(key, weight);
    plantMaxCounts.put(key, maxCount);
    plantSpeeds.put(key, speed);
    return; // ← добавить
}
```

### [HIGH] Исправить `Butterfly.addBiomass()` — восстановить `maxBiomass` guard

```java
@Override
public void addBiomass(double amount) {
    if (maxBiomass > 0) {
        this.biomass = Math.min(maxBiomass, this.biomass + amount);
    } else {
        this.biomass += amount;
    }
}
```

### [HIGH] Ввести `WorldSnapshot` — исправить `SimulationView.display(Island)`
Детали — в разделе 10.

### [HIGH] Убрать `instanceof Island` из `TaskRegistry`
Следует за исправлением `SimulationView`.

### [MEDIUM] Вынести `reproductionChance` в `AnimalType` и `species.properties`

```properties
rabbit.reproductionChance=0.18
wolf.reproductionChance=0.08
```
Убрать switch по `SizeClass` из `ReproductionService`.

### [MEDIUM] Убрать `instanceof Cell` из сервисов → методы в `SimulationNode`
Детали — в разделе 10.

### [MEDIUM] `SpeciesKey.REGISTRY` → `ConcurrentHashMap`

### [MEDIUM] Рандомизировать offset в `forEachSampled`

```java
int startOffset = (size > limit) ? getRandom().nextInt(step) : 0;
for (int i = startOffset; i < size; i += step) { ... }
```

### [MEDIUM] Inject `RandomProvider` в `Chameleon` через конструктор

```java
public Chameleon(AnimalType type, RandomProvider random) {
    super(type);
    this.random = random; // не RandomUtils.nextDouble()
}
```

### [MEDIUM] Кэшировать Biomass в `StatisticsService`
`onBiomassChanged(SpeciesKey, double delta)` hook → инкрементальный трекинг. Убирает O(W×H) scan из `getSpeciesCounts()`.

### [LOW] `SpeciesRegistry` → реальная иммутабельность через `unmodifiableMap`
### [LOW] Вынести `chunkSize=20`, `FULL_CYCLE=150`, `SLEEP_PERIOD=50` в конфиг
### [LOW] Исправить `LongTermSurvivalTest` — убрать бесполезные `System.setProperty`
### [LOW] Переименовать: `getPlantCount()→getTotalBiomassWeight()`, `getPercent()→getValue()`
### [LOW] Убрать `setSilent()` из `SimulationView` интерфейса

---

## 🔧 10. Рефакторинг с примерами

### Пример 1: Устранение `instanceof Cell` — `SimulationNode` как реальная абстракция

**Проблема:** Все 5 сервисов немедленно кастуют `SimulationNode` к `Cell`. Абстракция не работает.

**ДО:**
```java
// Паттерн во ВСЕХ сервисах
@Override
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) {  // абстракция выбрасывается
        for (Animal a : cell.getAnimals()) {
            // логика
        }
    }
    // если не Cell — молча ничего
}
```

**ПОСЛЕ — минимальный контракт в `SimulationNode`:**
```java
// engine/SimulationNode.java — добавить базовые операции
public interface SimulationNode {
    ReentrantLock getLock();
    String getCoordinates();
    void setNeighbors(List<SimulationNode> neighbors);
    List<SimulationNode> getNeighbors();

    // Добавить — domain-agnostic:
    List<? extends Animal> getLivingAnimals();
    List<? extends Animal> getLivingPredators();
    List<? extends Animal> getLivingHerbivores();
    List<? extends Biomass> getBiomassEntities();
    boolean addAnimal(Animal a);
    boolean removeAnimal(Animal a);
}

// Cell.java — реализация делегирует к существующим методам
@Override
public List<? extends Animal> getLivingAnimals() {
    return getAnimals(); // уже существует
}

// LifecycleService — без instanceof, без Cell
@Override
protected void processCell(SimulationNode node, int tickCount) {
    for (Animal a : node.getLivingAnimals()) {
        if (a.isAlive()) {
            double metabolism = a.getDynamicMetabolismRate();
            if (!a.tryConsumeEnergy(metabolism)) {
                getWorld().reportDeath(a.getSpeciesKey(), DeathCause.HUNGER);
            }
            if (a.isAlive() && a.checkAgeDeath()) {
                getWorld().reportDeath(a.getSpeciesKey(), DeathCause.AGE);
            }
        }
    }
    for (Biomass b : node.getBiomassEntities()) {
        if (b.isAlive()) b.grow();
    }
}
```

**Выигрыш:**
- `SimulationNode` — реальная рабочая абстракция
- Mock-node в unit тестах без создания `Island`
- Новый тип узла (`WaterCell`) работает без правки сервисов

---

### Пример 2: `WorldSnapshot` — полный domain decoupling для View

**Проблема:** `ConsoleView` знает про `Island`, `Cell`, `Animal`, `Biomass`. Для другого домена нужен новый View с нуля.

**ДО:**
```java
// SimulationView.java — domain-coupled
public interface SimulationView {
    void display(Island island);
}

// TaskRegistry.java — пробивает абстракцию
gameLoop.addRecurringTask(() -> {
    if (world instanceof com.island.model.Island island) {
        view.display(island);
    }
});

// ConsoleView — знает про Grid
for (int y = 0; y < island.getHeight(); y++) {
    for (int x = 0; x < island.getWidth(); x++) {
        renderCell(sb, island.getGrid()[x][y]);
    }
}
```

**ПОСЛЕ:**
```java
// engine/WorldSnapshot.java — domain-agnostic DTO
public interface WorldSnapshot {
    int getTickCount();
    int getTotalEntityCount();
    Map<String, Integer> getEntityCounts();  // String code, не SpeciesKey
    double getGlobalSatiety();
    int getStarvingCount();
    Map<String, Integer> getDeathStats(String causeCode);
    int getWidth();
    int getHeight();
    List<CellInfo> getCellInfos(); // flat list
}

// engine/CellInfo.java — light DTO
public record CellInfo(int x, int y, String dominantEntityCode, double dominantMass) {}

// engine/SimulationView.java — чистый
public interface SimulationView {
    void display(WorldSnapshot snapshot); // нет Island
    void setSilent(boolean silent);
}

// engine/SimulationWorld.java — добавить factory method
public interface SimulationWorld extends Tickable {
    // ... все существующие методы
    WorldSnapshot createSnapshot();
}

// model/Island.java — реализация snapshot
@Override
public WorldSnapshot createSnapshot() {
    return new IslandSnapshot(this);
}

// model/IslandSnapshot.java — adapter
public class IslandSnapshot implements WorldSnapshot {
    private final Island island;

    @Override
    public Map<String, Integer> getEntityCounts() {
        return island.getSpeciesCounts().entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().getCode(),
                Map.Entry::getValue
            ));
    }

    @Override
    public List<CellInfo> getCellInfos() {
        List<CellInfo> result = new ArrayList<>();
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                result.add(buildCellInfo(island.getGrid()[x][y], x, y));
            }
        }
        return result;
    }
    // ...
}

// TaskRegistry.registerAll() — без instanceof, без Island
gameLoop.addRecurringTask(t -> view.display(world.createSnapshot()));

// ConsoleView — не знает про Island
public class ConsoleView implements SimulationView {
    public void display(WorldSnapshot snapshot) {
        for (CellInfo cell : snapshot.getCellInfos()) {
            String icon = ICONS.getOrDefault(cell.dominantEntityCode(), "🏜️");
            sb.append(icon).append(" ");
        }
    }
}
```

**Выигрыш:**
- `ConsoleView` без изменений работает с `FarmWorld`, `CityWorld`, любым доменом
- `TaskRegistry` — ноль domain знаний
- `WorldSnapshot` — тестируемый DTO: тест рендеринга без запуска симуляции

---

## 📊 11. Итоговая оценка

| Критерий | Оценка | Обоснование |
|---|---|---|
| **Архитектура** | 7.5/10 | `SimulationWorld` + `Tickable` + сервисный слой — сильный foundation. Минус: `SimulationNode` — hollow abstraction, `SimulationView` domain-coupled, `TaskRegistry` instanceof |
| **Код** | 7/10 | Читаемый, константы вынесены, паттерны применяются. Минус: performance regression `protectionMap` per-cell, магические числа в ключевых местах, дублирование cold-blooded логики |
| **Переиспользуемость** | 6/10 | `GameLoop`, `AbstractService`, `InteractionProvider`, `ObjectPool` — реально переиспользуемы. `SimulationView.display(Island)` + `instanceof Cell` в сервисах + static `SpeciesKey.REGISTRY` блокируют full engine-статус |
| **Общая оценка** | **7/10** | Уровень upper-middle. Архитектурные намерения правильные, реализация на 80% завершена |

---

### 🏁 Вердикт

**Готов ли проект стать основой для универсального симуляционного движка?**

> **Потенциал — определённо да. Текущее состояние — 80% пути.**

**Что уже делает проект engine-материалом:**
- `SimulationWorld` — чистый интерфейс среды симуляции
- `Tickable` + `GameLoop` — domain-agnostic tick execution
- `AbstractService` + chunk parallelism — масштабируемая execution model
- `InteractionProvider` — абстрактный контракт взаимодействий
- `AnimalType` (Flyweight) + `species.properties` — data-driven конфигурация
- `ObjectPool` + `Poolable` — production-ready memory management
- `PreyProvider` — универсальный, не знает про конкретные виды

**Что блокирует прямо сейчас — 4 конкретные задачи:**

| # | Задача | Объём | Эффект |
|---|---|---|---|
| 1 | `SpeciesLoader`: добавить `return` после plant-ветки | 1 строка | Устраняет production bug |
| 2 | `FeedingService`: вынести `getProtectionMap()` из per-cell | 5 строк | 400x perf fix |
| 3 | Ввести `WorldSnapshot`, исправить `SimulationView` | ~150 строк | Full domain decoupling |
| 4 | Расширить `SimulationNode`, убрать `instanceof Cell` в сервисах | ~80 строк | Абстракция начинает работать |

После этих четырёх изменений — `FarmWorld implements SimulationWorld` подключается без правки engine. `ConsoleView` работает с любым доменом. Это реальный, переиспользуемый simulation engine.
