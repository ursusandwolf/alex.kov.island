# 🔍 Tech Lead Code Review — `dev` branch
### alex.kov.island · Сравнительный анализ: `main` → `dev`

**Reviewer:** Staff Engineer  
**Date:** 2026-04-30  
**Branch:** `dev` (vs. предыдущий ревью `main`)  
**Stack:** Java 21, Maven, JUnit 5, Mockito, Lombok  

> **Формат документа:** Полноценный ревью по 11 разделам с явными пометками  
> `✅ ИСПРАВЛЕНО` / `⚠️ ЧАСТИЧНО` / `❌ НЕ ИСПРАВЛЕНО` / `🆕 НОВАЯ ПРОБЛЕМА`  
> для каждой находки из предыдущего ревью `main`.

---

## Раздел 0. Что изменилось: Executive Summary

`dev` — это серьёзный шаг вперёд по сравнению с `main`. Автор закрыл несколько критических архитектурных пробелов. Но ряд фундаментальных проблем остался, а в двух местах появились новые регрессии.

| Категория | main | dev | Δ |
|---|---|---|---|
| Критических багов | 5 | 3 | -2 |
| Нарушений OCP | 4 | 2 | -2 |
| Hardcoded domain в core | 7 | 4 | -3 |
| Отсутствующих абстракций | 4 | 2 | -2 |
| Новых проблем (регрессий) | — | 3 | +3 |

**Ключевые достижения `dev`:**
- `InteractionMatrix` больше не статический — главный blocking issue предыдущего ревью закрыт
- Введены `SimulationWorld` + `SimulationNode` интерфейсы — foundation для engine
- `StatisticsService` вынесен из `Island` — правильное SRP
- `EntityContainer` извлечён из `Cell` — правильное разделение
- `PreyProvider` унифицирован — больше нет hardcoded CATERPILLAR/BUTTERFLY
- `AnimalType` обогащён data-driven флагами (`isColdBlooded`, `isPackHunter`)
- `WorldInitializer` не знает про конкретных видов — конфиг в `species.properties`

**Главные нерешённые проблемы:**
- `SimulationView.display(Island)` — интерфейс по-прежнему domain-coupled
- `TaskRegistry` — `instanceof Island` cast аннулирует `SimulationWorld` абстракцию
- Все сервисы — `instanceof Cell` cast в каждом `processCell` — `SimulationNode` сразу выбрасывается
- `Organism.reset()` — опасный паттерн объектного пула

---

## 🔍 1. Общий обзор проекта

### Что моделируется
Без изменений: экосистема острова, жизненный цикл организмов, пищевые цепи.

### Привязка к домену «остров»
Значительно улучшилась. `SimulationWorld` — полноценный интерфейс, который описывает мир без привязки к острову. `Island implements SimulationWorld` — правильный паттерн. Это открывает путь к "FarmWorld implements SimulationWorld".

**Однако:** `ConsoleView.display(Island island)` и `SimulationView.display(Island island)` по-прежнему принимают конкретный класс. Одна нога стоит в domain, другая тянется к engine.

### Переиспользуемость абстракций

| Абстракция | main | dev | Комментарий |
|---|---|---|---|
| `GameLoop` | ✅ | ✅ | Улучшен: теперь `Tickable` вместо `Runnable` |
| `AbstractService` | ✅ | ✅ | Теперь принимает `SimulationWorld` — лучше |
| `SimulationWorld` | ❌ | ✅ | Новый интерфейс — engine-уровень |
| `SimulationNode` | ❌ | ⚠️ | Есть, но сразу кастуется к Cell везде |
| `InteractionMatrix` | ❌ | ✅ | Больше не static, есть `InteractionProvider` |
| `SimulationView` | ❌ | ❌ | По-прежнему `display(Island)` |
| `HuntingStrategy` | ✅ | ✅ | `selectPrey()` добавлен — улучшено |
| `SpeciesKey` | ⚠️ | ⚠️ | Static registry не исправлен |

---

## 🏗 2. Архитектура и дизайн

### Архитектурный стиль — `dev`
Добавился чёткий engine-слой: `engine/SimulationWorld`, `engine/SimulationNode`, `engine/Tickable`. Это правильное направление — движение от "слоистого монолита" к "hexagonal-подобной" архитектуре, где domain изолирован от engine core.

**Но:** изоляция неполная. Engine-интерфейсы сразу же нарушаются кастами к domain-типам в каждом сервисе.

---

### SOLID в `dev`

#### S — Single Responsibility
✅ `StatisticsService` выделен из `Island` — Island перестал быть God Object в части статистики.  
✅ `EntityContainer` выделен из `Cell` — Cell делегирует хранение.  
⚠️ `FeedingService.processCell()` всё ещё выполняет: pack hunting + individual hunting + herbivore feeding + LOD sampling + protection check. ~80 строк, 4+ ответственности.

#### O — Open/Closed
✅ **`PreyProvider` исправлен** — теперь итерирует `cell.getBiomassContainers()` универсально. Новый тип биомассы добавляется без правки `PreyProvider`.

✅ **`WorldInitializer`** — больше не знает про BEAR и WOLF. Конфиг в `species.properties`.

❌ **`ReproductionService.tryReproduce()`** — hardcoded шансы размножения по SizeClass прямо в коде:
```java
double chance = switch (type.getSizeClass()) {
    case TINY  -> 0.25;  // Mouse, Hamster
    case SMALL -> 0.18;  // Duck, Rabbit
    case NORMAL -> 0.12; // Fox, Eagle
    case MEDIUM -> 0.08; // Wolf, Goat, Sheep
    case LARGE -> 0.04;  // Bear, Boar, Deer
    case HUGE  -> 0.02;  // Buffalo, Horse
};
```
Это domain knowledge в сервисе. Должно быть в `AnimalType` (поле `reproductionChance`) или в `species.properties`. Добавить новый SizeClass = правка switch.

❌ **`InteractionMatrix.buildFrom()`** — по-прежнему знает про `SpeciesKey.PLANT`, `SpeciesKey.GRASS`, `SpeciesKey.CABBAGE` как hardcoded fallback:
```java
if (preyKey.equals(SpeciesKey.PLANT)) {
    matrix.setChance(predatorKey, SpeciesKey.GRASS, chance);
    matrix.setChance(predatorKey, SpeciesKey.CABBAGE, chance);
}
```
Логика "если ест PLANT — ест GRASS и CABBAGE" должна быть выражена через тегирование (`isPlant=true`) в конфиге, а не через конкретные ключи.

#### L — Liskov Substitution
❌ **`Butterfly.addBiomass()` — LSP-нарушение не исправлено:**
```java
// Biomass.addBiomass (родитель) — cap по maxBiomass
public void addBiomass(double amount) {
    this.biomass = Math.min(maxBiomass, this.biomass + amount); // ОГРАНИЧЕНИЕ
}

// Butterfly (наследник) — cap убран
public void addBiomass(double amount) {
    this.biomass += amount; // БЕЗ ОГРАНИЧЕНИЯ — нарушение инварианта
}
```
Клиент, работающий через `Biomass`, ожидает `biomass <= maxBiomass`. Butterfly ломает это. Потенциальный unbounded growth.

⚠️ **`Organism implements Poolable`** — добавление `reset()` к базовому классу всех организмов это mix of concerns. Poolable — это деталь реализации инфраструктуры, а не часть доменной модели организма. Правильнее: отдельный wrapper или adapter.

#### I — Interface Segregation
⚠️ `SimulationView.setSilent(boolean)` — по-прежнему в интерфейсе. GUI-реализация не нуждается в "silent mode".  
✅ `InteractionProvider` — правильная минималистичная абстракция: только `getChance` и `hasAnimalPrey`.

#### D — Dependency Inversion
✅ Сервисы теперь зависят от `SimulationWorld`, а не от `Island` — большой прогресс.  
✅ `AnimalFactory` принимает `RandomProvider` через конструктор — DI правильный.  
❌ `SimulationView.display(Island island)` — high-level view зависит от конкретного domain-класса.  
❌ `TaskRegistry.registerAll()` — явный `instanceof Island` cast:
```java
gameLoop.addRecurringTask(() -> {
    if (world instanceof com.island.model.Island island) {
        view.display(island);
    }
});
```
Это полное аннулирование `SimulationWorld` абстракции. Если `world` не `Island` — view не вызывается. Для другого домена — рендеринг просто не работает.

---

### Антипаттерны

| Антипаттерн | main | dev | Статус |
|---|---|---|---|
| Static Initialization Trap (`InteractionMatrix`) | ❌ | ✅ | ИСПРАВЛЕНО |
| God Object (`Island`) | ❌ | ⚠️ | Частично (StatisticsService выделен) |
| `instanceof` chain в сервисах | — | 🆕 | 5 мест: каждый `processCell` кастует к `Cell` |
| `instanceof` в TaskRegistry | — | 🆕 | `world instanceof Island` — нарушает DIP |
| Domain leak в `SimulationView` | ❌ | ❌ | Не исправлено |
| Dangerous Pool Pattern | — | 🆕 | `Organism.reset()` без полного сброса |

---

### Новый антипаттерн: `instanceof Cell` во всех сервисах

`SimulationNode` введён как абстракция, но **все 5 сервисов** немедленно кастуют его к `Cell`:

```java
// LifecycleService, MovementService, FeedingService, CleanupService, ReproductionService
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) { // <- каждый сервис
        // ...
    }
}
```

Это означает, что `SimulationNode` — пустая абстракция. Никакой другой тип node нельзя использовать в этих сервисах. Если `SimulationNode` — не Cell, action молча пропускается без ошибки. Это хуже чем прямая зависимость — она создаёт ложное ощущение абстракции.

**Инженерный аргумент:** Нужно либо убрать `SimulationNode` из `processCell` и использовать `Cell` напрямую, либо перенести методы (getAnimals, getBiomassContainers) в `SimulationNode` интерфейс. Текущий подход — худшее из двух миров.

---

### Разделение слоёв в `dev`

```
engine/    ← SimulationWorld, SimulationNode, Tickable, GameLoop, Bootstrap
model/     ← Island, Cell, Chunk, EntityContainer
content/   ← Animal, Biomass, Organism, SpeciesKey, AnimalType
service/   ← AbstractService и все сервисы
view/      ← SimulationView (interface), ConsoleView
util/      ← RandomProvider, InteractionProvider, ObjectPool
config/    ← SimulationConstants, Configuration, EnergyPolicy
```

Слоистость лучше, чем в `main`. Но `ConsoleView` по-прежнему тянет `Cell`, `Animal`, `Biomass`, `Island`, `DeathCause` из разных слоёв. View должен работать с абстракцией состояния мира, а не с конкретными классами.

---

## ⚙️ 3. Алгоритмы и логика симуляции

### Game Loop
✅ `GameLoop` теперь использует `Tickable` вместо `Runnable` — type-safe, передаёт `tickCount`. Улучшение.  
✅ `island.tick(tickCount)` принимает номер тика — статистика сбрасывается правильно.

### LOD (Level of Detail) — `forEachSampled`
🆕 Новая механика в `AbstractService`:
```java
protected <T> void forEachSampled(List<T> list, int limit, Consumer<T> action) {
    int step = (size > limit) ? (size / limit + 1) : 1;
    for (int i = 0; i < size; i += step) {
        action.accept(list.get(i));
    }
}
```
**Концепция правильная** — skip обработки при переполнении клетки. Но реализация имеет проблему: выборка детерминирована (всегда элемент 0, step, 2*step...). Животные в начале списка всегда обрабатываются, в конце — нет. Это создаёт систематическое преимущество для "первых" животных. Выборка должна быть случайной (`random.nextInt(step)` как стартовый offset).

### Big-O проблемы

| Место | dev | Статус |
|---|---|---|
| `Island.getSpeciesCounts()` — O(W×H×biomass) | ❌ Не исправлено | Каждый рендер |
| `Island.getGlobalSatiety()` — O(W×H×animals) | ❌ Не исправлено | Каждый рендер |
| `Island.getStarvingCount()` — O(W×H×animals) | ❌ Не исправлено | Каждый рендер |
| `FeedingService.processCell()` — `getProtectionMap()` | 🆕 РЕГРЕССИЯ | Стало хуже |

**`getProtectionMap()` — регрессия по сравнению с `main`:**

В `main` вычислялся один раз за tick в `FeedingService.run()`.  
В `dev` вычисляется **per-cell** в `processCell()`:
```java
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) {
        this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // PER CELL
```

На острове 20×20 = 400 ячеек → `getProtectionMap()` вызывается 400 раз за тик. Внутри — итерация по всем видам + подсчёт популяции. Это O(species × W×H) per cell = O(species × W² × H²) total. Явная регрессия.

---

## 📦 4. Коллекции и структуры данных

### ObjectPool — новое введение

✅ Концепция правильная, реализация через `ConcurrentLinkedQueue` — thread-safe.

🆕 **Опасный паттерн: `Organism.reset()` — неполный сброс:**
```java
public void reset() {
    this.isAlive = true;
    this.age = 0;
    // Generating UUID is slow. Let's just keep the old ID.
    this.currentEnergy = 0;  // <- ПРОБЛЕМА
}
```
`reset()` устанавливает `currentEnergy = 0`, но не сбрасывает `maxEnergy` и `maxLifespan`. Затем `init()` вызывается отдельно. Если между `pool.acquire()` и `animal.init()` что-то пойдёт не так (exception, ранний возврат), объект будет использован с `energy=0`. Такое животное мгновенно умрёт при первом `tryConsumeEnergy`. Это тихий баг.

🆕 **`energyLock` не сбрасывается в `reset()`:**
```java
// Organism.java
private final java.util.concurrent.locks.ReentrantLock energyLock = new java.util.concurrent.locks.ReentrantLock();
```
Поле `final`, не сбрасывается — это нормально. Но lock инициализируется при создании объекта, а не в `init()`. Если lock захвачен в момент смерти (маловероятно, но при многопоточности не исключено) и объект вернули в пул — следующий `acquire()` получит lock в захваченном состоянии.

🆕 **Поля `Organism` потеряли `final`:**

В `main`:
```java
private final double maxEnergy;
private final int maxLifespan;
```

В `dev`:
```java
private double maxEnergy;  // НЕТ final
private int maxLifespan;   // НЕТ final
```

Ради объектного пула пожертвовали иммутабельностью. Теперь внешний код может случайно вызвать `init()` на живом организме, изменив его параметры. Инвариант нарушен.

### `EntityContainer` — ✅ новый класс
Правильное разделение хранения от доступа. `fastRemove` через swap-with-last — O(1). Но `countBySpecies()` всё ещё O(N) по типам, что нормально для небольших ячеек.

### `SpeciesRegistry` с Lombok `@RequiredArgsConstructor`
В `main` конструктор оборачивал maps в `Collections.unmodifiableMap()`. В `dev` с `@RequiredArgsConstructor` этого нет — maps передаются напрямую:
```java
@RequiredArgsConstructor
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes; // mutable map!
```
Мутабельные поля в "immutable registry" — нарушение контракта класса.

### Лишние аллокации
- `PreyProvider` всё ещё создаётся per-cell per-tick (два инстанса: pack и regular). На 400 ячейках = 800 объектов на тик.
- `new ArrayList<>(container.getAllAnimals())` — snapshot в каждом `getAnimals()`, `getPredators()`, `getHerbivores()` — не изменилось.

---

## 🧱 5. Качество кода

### Нейминг
Незначительные улучшения. Проблемы из `main`:

| Проблема | Статус |
|---|---|
| `getPlantCount()` возвращает суммарный вес биомассы | ❌ Не исправлено |
| `getPercent()` в `EnergyPolicy` возвращает абсолютное число | ❌ Не исправлено |
| `isAnimalPredator()` — избыточное Animal в имени | ❌ Не исправлено |

🆕 **`shouldAct()` в `FeedingService` — имя не отражает семантики:**
```java
private boolean shouldAct(Animal animal, int tickCount) {
    if (!animal.canPerformAction()) return false;
    return !animal.getAnimalType().isColdBlooded() || (tickCount % 3 == 0);
}
```
Метод называется `shouldAct` но реально проверяет "может ли cold-blooded действовать в этот тик". Лучше: `canActThisTick()`.

### Дублирование
✅ Логика "endangered" вынесена в `Island.getProtectionMap()` — частичное улучшение. Но `ReproductionService` всё ещё пересчитывает endangered статус самостоятельно через `island.isRedBookProtectionEnabled()`.

❌ `shouldMove()` в `MovementService` и `shouldAct()` в `FeedingService` — идентичная логика "cold-blooded skip ticks" дублируется:
```java
// MovementService
if (animal.getAnimalType().isColdBlooded()) return (tickCount % 2 == 0);

// FeedingService
return !animal.getAnimalType().isColdBlooded() || (tickCount % 3 == 0);
```
К тому же разные константы (2 vs 3) — источник путаницы.

### Магические значения

🆕 `FeedingService.tryEat()`:
```java
int preyCount = cell.getOrganismCount(a.getSpeciesKey());
if (preyCount > a.getAnimalType().getMaxPerCell() / 2) {
    chance += 15; // <- магия
}
```
+15% шанса охоты при перенаселённости. Откуда 15? Должно быть в `SimulationConstants`.

🆕 `FeedingService.tryEat()`:
```java
consumer.consumeEnergy(consumer.getMaxEnergy() * 0.05); // хищник
consumer.consumeEnergy(consumer.getMaxEnergy() * 0.03); // травоядное
```
Штраф за неудачную охоту/поиск пищи. Магические 5% и 3%.

`ReproductionService.tryReproduce()` — see OCP section.

### Слишком большие методы
`FeedingService` — три крупных приватных метода: `processPredators`, `processPackHunting`, `tryEat`. Лучше чем в `main`, но `tryEat()` (~50 строк) всё ещё многоответственен.

---

## 🧪 6. Тестируемость

### Что улучшилось
✅ `FeedingMechanicsTest` — использует Mockito `SimulationWorld` mock. Правильный unit test.  
✅ `LifecycleServiceTest` — полностью изолированный тест через `SimulationWorld` mock.  
✅ `SimulationOptimizationTest` — тесты LOD и cold-blooded skip.  
✅ `AnimalFactory` принимает `RandomProvider` через конструктор — детерминированные тесты.  
✅ `InteractionMatrix` теперь инстанс-уровневый — тесты полностью изолированы.

### Что остаётся проблемой

❌ **`Chameleon.isProtected()` — static dependency:**
```java
return super.isProtected(currentTick) || RandomUtils.nextDouble() < 0.95;
```
`RandomUtils` — глобальный статик. `Chameleon` не принимает `RandomProvider` через конструктор. Невозможно детерминированно тестировать защиту хамелеона без изменения глобального состояния.

❌ **`RandomUtils.setProvider()` — глобальный мутируемый статик:**  
В тестах используется `RandomUtils.setProvider(...)` без `@AfterEach` гарантии сброса. `DeterminismTest.testDeterministicRandom()` явно не сбрасывает в try/finally.

❌ **`LongTermSurvivalTest.testStabilityFor500Ticks()` — System.setProperty не работает:**
```java
System.setProperty("island.width", "10");  // Нет эффекта!
System.setProperty("island.height", "10"); // Нет эффекта!
```
`Configuration.load()` читает из `InputStream` classpath, не из System properties. Тест всегда запускается на `20×20` из `species.properties`. Баг перенесён из `main` без исправления.

❌ **Integration tests с `SimulationBootstrap` в `@BeforeEach`** — по-прежнему тяжёлые интеграционные тесты под маркой unit. `BoundaryConditionsTest.setUp()` поднимает всю симуляцию.

---

## 🚀 7. Масштабируемость и расширяемость

### Добавить новый тип сущности
**Обычное животное:** `species.properties` + (опционально) новый класс. ✅ Значительно лучше, чем в `main`.  
**Новый Biomass-тип:** `isBiomass=true` в `.properties` → `GenericBiomass`. ✅ OCP соблюдён.  
**Особая механика (как Bear):** Новый класс + регистрация в `AnimalFactory`. Приемлемо.

### Изменить правила взаимодействия
`species.properties` (вероятности) → всё работает. Pack hunting теперь data-driven. ✅

### Добавить новую механику
Новый сервис extends `AbstractService` → добавить в `TaskRegistry` → работает. ✅  
Но: если механике нужны новые данные в `Cell` — Cell придётся расширять.

### Использовать в другой игре с другим доменом
**Частично реализуемо**, но всё ещё блокируется двумя проблемами:

1. `SimulationView.display(Island)` — view не работает с другим доменом
2. `instanceof Cell` в каждом сервисе — сервисы несовместимы с другими node-типами

Если эти два пункта исправить — engine становится реально переиспользуемым.

### Где потребуется переписывание ядра
1. ~~`InteractionMatrix` — static~~ → ✅ ИСПРАВЛЕНО в dev
2. `SimulationView` → нужен `WorldSnapshot` интерфейс — пока не сделано
3. `instanceof Cell` в сервисах → нужно перенести методы в `SimulationNode`

---

## 🐞 8. Потенциальные баги и риски

### [BUG] `SpeciesLoader.loadEntry()` — plant добавляется в оба реестра
```java
if (isPlant) {
    plantWeights.put(key, weight);
    plantMaxCounts.put(key, maxCount);
    plantSpeeds.put(key, speed);
}
// НЕТ return/continue — код ПРОДОЛЖАЕТСЯ и добавляет AnimalType для растения!
double food = ...;
AnimalType type = AnimalType.builder()...build();
animalTypes.put(key, type); // Grass/Cabbage попадают в animalTypes тоже
```
Трава (`grass.isPlant=true`) добавляется и в `plantWeights`, и в `animalTypes`. В `SpeciesRegistry.getAllAnimalKeys()` вернёт Grass и Cabbage как животных. `AnimalFactory` создаст для них пул. `WorldInitializer` попытается создать `GenericAnimal(grassType)` вместо `GenericBiomass`. Это логический баг с потенциально непредсказуемым поведением.

### [BUG] `Butterfly.addBiomass()` — LSP нарушение (перенесено из `main`)
```java
public void addBiomass(double amount) {
    this.biomass += amount; // нет ограничения maxBiomass
}
```
При `maxBiomass == 0` (начальное состояние для Butterfly) и постоянном `spawn()` из Caterpillar — unbounded growth биомассы бабочек.

### [BUG] `Organism.reset()` + `ObjectPool` — use before init
```java
public T acquire() {
    T obj = pool.poll();
    if (obj == null) return factory.get();
    return obj; // obj.currentEnergy == 0 после reset()
}
// Между acquire() и init() — животное уязвимо
```
Если вызывающий код использует животное до `init()` или `init()` не вызывается из-за exception — объект с `energy=0` попадает в клетку и умирает в первый же тик от голода без диагностики.

### [BUG] `FeedingService.processCell()` — `protectionMap` вычисляется per-cell (регрессия)
`this.protectionMap = getWorld().getProtectionMap(speciesRegistry)` внутри `processCell`. Для острова 20×20 = 400 вызовов за тик. Каждый вызов итерирует всех видов и обращается к `statisticsService.getSpeciesCount()`. Performance регрессия vs `main`.

### [RISK] `SpeciesKey.REGISTRY` — `HashMap` не thread-safe
```java
private static final Map<String, SpeciesKey> REGISTRY = new HashMap<>();
```
`fromCode(String, boolean)` вызывает `register()` который пишет в `REGISTRY`. При concurrent `fromCode()` из нескольких потоков — race condition. В production при динамическом добавлении видов — data corruption.

### [RISK] `island.getCell()` — toroidal, но `SimulationWorld.getNode()` — нет
```java
// Island.getNode() — НЕ toroidal:
if (tx >= 0 && tx < width && ty >= 0 && ty < height) {
    return Optional.of(grid[tx][ty]);
}
return Optional.empty();

// Island.getCell() — toroidal:
int tx = (x % width + width) % width;
```
`MovementService` использует `getWorld().getNode()` через `SimulationWorld` → границы не тороидальные. Животные у края карты не могут перейти на другую сторону. Поведение изменилось по сравнению с `main`.

### [RISK] `ReproductionService` — `canInitiateReproduction()` требует `age >= 1`
В `dev` добавлено `getAge() >= 1` в `canInitiateReproduction()`. Тест `ReproductionServiceTest.testReproductionWithMaxEnergy()` явно инкрементирует возраст:
```java
r1.checkAgeDeath(); // без этого тест не пройдёт
```
Но `createBaby()` в `AnimalFactory` создаёт животное с `initialAge=0`. Новорождённые не могут размножаться сразу — ок. Но животные из `ObjectPool` после `init()` получают `age=0` — правильно. Потенциально детеныши, возвращённые в пул и переиспользованные без инкремента возраста, навсегда останутся в `age=0` и смогут жить бесконечно (если `maxLifespan > 0` и возраст не инкрементируется).

---

## 💡 9. Приоритетные улучшения

### [HIGH] Исправить `SpeciesLoader.loadEntry()` — убрать fall-through для plants
```java
if (isPlant) {
    plantWeights.put(key, weight);
    // ...
    return; // <- добавить этот return
}
// Иначе растения попадают в animalTypes
```

### [HIGH] Вынести `getProtectionMap()` из `processCell` в начало tick
```java
// AbstractService или FeedingService.tick() — вычислить ОДИН РАЗ
@Override
public void tick(int tickCount) {
    this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // перед invokeAll
    super.tick(tickCount);
}
```

### [HIGH] Исправить `SimulationView.display(Island)` → `display(WorldSnapshot)`
Ввести `WorldSnapshot` интерфейс. Это последний ключевой шаг для реального engine.

### [HIGH] Исправить `TaskRegistry` — убрать `instanceof Island` cast
```java
// Сейчас:
if (world instanceof com.island.model.Island island) {
    view.display(island);
}

// После исправления SimulationView:
gameLoop.addRecurringTask(t -> view.display(world.createSnapshot()));
```

### [HIGH] Исправить `Butterfly.addBiomass()` — восстановить `maxBiomass` guard:
```java
@Override
public void addBiomass(double amount) {
    // Если maxBiomass == 0 (неинициализированный) — не ограничиваем, иначе — cap
    if (maxBiomass > 0) {
        this.biomass = Math.min(maxBiomass, this.biomass + amount);
    } else {
        this.biomass += amount;
    }
}
```

### [HIGH] Перенести методы `getAnimals()`/`getBiomassContainers()` в `SimulationNode`
Убрать `instanceof Cell` из всех сервисов:
```java
// SimulationNode — добавить:
List<? extends Organism> getEntities();
List<? extends Organism> getBiomassEntities();
```

### [MEDIUM] Добавить `reproductionChance` в `AnimalType` и `species.properties`
```
rabbit.reproductionChance=0.18
wolf.reproductionChance=0.08
```
Убрать switch в `ReproductionService.tryReproduce()`.

### [MEDIUM] Исправить `SpeciesKey.REGISTRY` → `ConcurrentHashMap`
Одна строка, защищает от race condition при concurrent `fromCode()`.

### [MEDIUM] Рандомизировать offset в `forEachSampled`
```java
int startOffset = (size > limit) ? random.nextInt(step) : 0;
for (int i = startOffset; i < size; i += step) { ... }
```
Устраняет систематическое преимущество "первых" животных в переполненной клетке.

### [MEDIUM] Injеct `RandomProvider` в `Chameleon`
```java
public class Chameleon extends Animal {
    private final RandomProvider random;
    public Chameleon(AnimalType type, RandomProvider random) {
        super(type);
        this.random = random;
    }
    public boolean isProtected(int currentTick) {
        return super.isProtected(currentTick) || random.nextDouble() < 0.95;
    }
}
```

### [MEDIUM] Кэшировать biomass в StatisticsService
`onBiomassChanged(SpeciesKey, double delta)` → StatisticsService отслеживает инкрементально. Убирает O(W×H) traversal из `getSpeciesCounts()`.

### [LOW] Сделать `SpeciesRegistry` реально immutable
```java
public SpeciesRegistry(Map<...> animalTypes, ...) {
    this.animalTypes = Collections.unmodifiableMap(new HashMap<>(animalTypes));
    // ...
}
```

### [LOW] Исправить `LongTermSurvivalTest` — убрать бесполезные `System.setProperty`
### [LOW] Вынести Magic Numbers `0.05`, `0.03` из `FeedingService.tryEat()` в `SimulationConstants`
### [LOW] Унифицировать "cold-blooded tick skip" — вынести в `AnimalType.getTickInterval()`

---

## 🔧 10. Рефакторинг с примерами

### Пример 1: `SimulationNode` — устранение `instanceof Cell` во всех сервисах

**ДО** (текущий dev):
```java
// Все 5 сервисов — одинаковый паттерн
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) {  // <- абстракция выбрасывается
        processAnimals(cell, tickCount);
    }
    // Если не Cell — молча ничего не делается
}
```

**ПОСЛЕ — перенести базовые операции в интерфейс:**
```java
// engine/SimulationNode.java — расширить интерфейс
public interface SimulationNode {
    ReentrantLock getLock();
    String getCoordinates();
    void setNeighbors(List<SimulationNode> neighbors);
    List<SimulationNode> getNeighbors();

    // Добавить — domain-агностичные операции:
    List<? extends Mortal> getLivingEntities();       // Animal + Biomass
    List<? extends Mortal> getBiomassEntities();      // только Biomass
    boolean addEntity(Mortal entity);
    boolean removeEntity(Mortal entity);
}

// Cell.java — реализовать контракт
@Override
public List<? extends Mortal> getLivingEntities() {
    lock.lock();
    try { return new ArrayList<>(container.getAllAnimals()); }
    finally { lock.unlock(); }
}

// AbstractService — без instanceof
protected void processCell(SimulationNode node, int tickCount) {
    // Всегда работает — нет cast
    List<? extends Mortal> entities = node.getLivingEntities();
    for (Mortal m : entities) {
        process(m, node, tickCount);
    }
}
```

**Выигрыш:** `SimulationNode` реально абстрактный. Тест с mock-node возможен. Другой тип симуляции с другим Node-типом работает без правки сервисов.

---

### Пример 2: `SimulationView` — устранение domain coupling

**ДО** (текущий dev):
```java
// SimulationView.java
public interface SimulationView {
    void display(Island island);  // domain-coupled
    void setSilent(boolean silent);
}

// TaskRegistry.java
gameLoop.addRecurringTask(() -> {
    if (world instanceof com.island.model.Island island) { // <- cast
        view.display(island);
    }
});
```

**ПОСЛЕ — ввести WorldSnapshot:**
```java
// engine/WorldSnapshot.java — domain-агностичный контракт
public interface WorldSnapshot {
    int getTickCount();
    int getTotalEntityCount();
    Map<String, Integer> getEntityCounts();    // String, не SpeciesKey
    double getGlobalSatiety();
    int getStarvingCount();
    Map<String, Integer> getDeathStats(String causeCode);
    // Для рендеринга карты:
    int getWidth();
    int getHeight();
    CellSnapshot getCellSnapshot(int x, int y); // light DTO
}

// engine/SimulationView.java
public interface SimulationView {
    void display(WorldSnapshot snapshot);
    void setSilent(boolean silent);
}

// model/IslandSnapshot.java — Island-specific реализация
public class IslandSnapshot implements WorldSnapshot {
    private final Island island;
    @Override
    public Map<String, Integer> getEntityCounts() {
        Map<String, Integer> result = new HashMap<>();
        island.getSpeciesCounts().forEach((k, v) -> result.put(k.getCode(), v));
        return result;
    }
    // ...
}

// TaskRegistry.java — без instanceof
gameLoop.addRecurringTask(t -> view.display(world.createSnapshot()));

// ConsoleView теперь domain-агностичный
public class ConsoleView implements SimulationView {
    public void display(WorldSnapshot snapshot) {  // не знает про Island
        // Работает с любым доменом через WorldSnapshot
    }
}
```

**Выигрыш:** `ConsoleView` без изменений работает с FarmWorld, CityWorld, любым другим доменом. ICONS-маппинг можно инжектировать. TaskRegistry не имеет `instanceof`.

---

### Пример 3: `FeedingService` — вынести `getProtectionMap()` из per-cell

**ДО** (dev — регрессия):
```java
@Override
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) {
        this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // 400x per tick!
        processPredators(cell, tickCount);
```

**ПОСЛЕ:**
```java
// FeedingService.java
@Override
public void tick(int tickCount) {
    this.protectionMap = getWorld().getProtectionMap(speciesRegistry); // 1x per tick
    super.tick(tickCount); // invokeAll chunks
}

@Override
protected void processCell(SimulationNode node, int tickCount) {
    if (node instanceof Cell cell) {
        // protectionMap уже вычислен, переиспользуется
        processPredators(cell, tickCount);
```

Одна строка — 400x ускорение вычисления `protectionMap` per tick.

---

## 📊 11. Итоговая оценка

| Критерий | main | dev | Δ |
|---|---|---|---|
| **Архитектура** | 6/10 | 7.5/10 | +1.5 |
| **Код** | 7/10 | 7/10 | 0 |
| **Переиспользуемость** | 4/10 | 6/10 | +2 |
| **Общая оценка** | 6/10 | **7/10** | +1 |

### Детализация оценок

**Архитектура 7.5/10** (+1.5 vs main):
- `+1.5` за `SimulationWorld` + `SimulationNode` + `Tickable` — real engine foundation
- `+0.5` за `StatisticsService` + `EntityContainer` + `InteractionProvider`
- `-0.5` за `instanceof Cell` паттерн во всех сервисах — абстракция не работает
- `-0.5` за `instanceof Island` в TaskRegistry — сломанный chain

**Код 7/10** (без изменений):
- `+0.5` за Lombok, data-driven flags, `forEachSampled`
- `-0.5` за регрессию `protectionMap` per-cell + новые магические числа

**Переиспользуемость 6/10** (+2 vs main):
- `+2` за `SimulationWorld` как engine contract
- `-1` за `SimulationView.display(Island)` + `instanceof Island` в TaskRegistry — финальная миля не пройдена
- `-1` за `instanceof Cell` — сервисы фактически domain-coupled

---

### 🏁 Вердикт

**Готов ли `dev` стать основой для универсального симуляционного движка?**

> **Почти. Осталось 2-3 прицельных рефакторинга.**

Автор сделал нетривиальную работу: ввёл `SimulationWorld`, изолировал `StatisticsService`, сделал `InteractionMatrix` инстанс-уровневым, добавил data-driven флаги в `AnimalType`, унифицировал `PreyProvider`. Это не косметика — это системные изменения в правильном направлении.

`dev` находится на 80% пути к engine-статусу. Оставшиеся 20% — конкретные, измеримые задачи:

| Задача | Сложность | Эффект |
|---|---|---|
| Исправить `SpeciesLoader` fall-through для plants | 1 строка | Устраняет production bug |
| Вынести `getProtectionMap()` из per-cell | 3 строки | 400x perf fix |
| Ввести `WorldSnapshot` + исправить `SimulationView` | ~100 строк | Full domain decoupling |
| Убрать `instanceof Cell` из сервисов | ~30 строк | `SimulationNode` начинает работать |

Если эти четыре задачи закрыты — проект становится **реальным, переиспользуемым simulation engine**. FarmWorld, CityWorld, ZooSim — всё подключается как новый `SimulationWorld` + `SimulationView` без правки core.
