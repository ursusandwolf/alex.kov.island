# 🏝️ Tech Lead / Staff Engineer Code Review
## PR #17: `dev` → `main` | Island Ecosystem Simulator
**Статус:** OPEN (не смёрджен) | **Дата:** 2026-04-28  
**63 коммита · 74 Java · 4 MD · 2 XML · 1 properties**  
**Тест-результат из PR:** 22 passed, 0 failures, BUILD SUCCESS ✅  
**Checkstyle:** 0 violations ✅

---

## Содержание

1. [Общий обзор проекта](#1-общий-обзор-проекта)
2. [Архитектура и дизайн](#2-архитектура-и-дизайн)
3. [Алгоритмы и логика](#3-алгоритмы-и-логика)
4. [Работа с коллекциями и структурами данных](#4-работа-с-коллекциями-и-структурами-данных)
5. [Качество кода](#5-качество-кода)
6. [Тестируемость](#6-тестируемость)
7. [Масштабируемость и расширяемость](#7-масштабируемость-и-расширяемость)
8. [Потенциальные баги и риски](#8-потенциальные-баги-и-риски)
9. [Конкретные рекомендации](#9-конкретные-рекомендации)
10. [Рефакторинг — практические примеры](#10-рефакторинг--практические-примеры)
11. [Итоговая оценка](#11-итоговая-оценка)

---

## 1. Общий обзор проекта

### Доменная модель

Проект моделирует экосистему острова: 100×20 клеток, 20+ видов организмов (хищники, травоядные, насекомые, растения), многофазный симуляционный цикл (Eat → Move → Reproduce → Lifecycle), параллельная обработка через чанки.

**Основные сущности и ответственность:**

| Сущность | Ответственность |
|---|---|
| `Organism` | Базовое состояние (энергия, возраст, жив/мёртв) |
| `Animal` | Поведение животного (Flyweight через `AnimalType`) |
| `Biomass` | Контейнер биомассы (растения, насекомые-рои) |
| `Cell` | Thread-safe контейнер организмов ячейки |
| `Island` | Пространственная модель, AtomicInteger счётчики |
| `Chunk` | Параллельная единица обработки |
| `FeedingService` | Фаза питания (ROI-охота, иерархия) |
| `MovementService` | Фаза движения (ordered locking) |
| `ReproductionService` | Фаза размножения (energy redistribution) |
| `LifecycleService` | Метаболизм, старение, смерть |
| `GameLoop` | Оркестрация тиков (Virtual Threads) |
| `SimulationContext` | DI-контейнер сервисов |
| `PreyProvider` | Mediator: логика "кто доступен для охоты" |
| `SpeciesRegistry` | Данные видов (заменил SpeciesConfig) |
| `HuntingStrategy` | Strategy: логика охоты (OCP) |

### Архитектурный стиль

**Rich Domain Model с элементами Service Layer.** Не анемичная модель — `Organism` содержит бизнес-логику (энергетика, смерть). Сервисы реализуют сложные взаимодействия между объектами, не дублируя логику, которая принадлежит домену.

Это правильный выбор для симуляции: объекты "знают" своё состояние, сервисы координируют взаимодействие.

### Читаемость без контекста

**Хорошая.** Новый разработчик может понять поток: `SimulatorMain` → `SimulationBootstrap` → `GameLoop` → сервисы → `Cell`. `SpeciesKey` enum, `SizeClass`, `EnergyPolicy` делают намерения прозрачными. Checkstyle 0 violations — форматирование консистентно.

---

## 2. Архитектура и дизайн

### 2.1 SOLID — Полный разбор

#### S — Single Responsibility Principle

**✅ Значительно улучшен по сравнению с предыдущими PR:**

- `SpeciesLoader` отделён от `SpeciesRegistry` — загрузка vs хранение
- `ConfigLoader` отделён от `Configuration` — загрузка vs доступ
- `SimulationBootstrap` отделён от `SimulatorMain`
- `AbstractService` вынес общую параллельную логику

**❌ Нарушение — `ConfigLoader.loadInteractionMatrix()`:**

```java
// ConfigLoader отвечает за ДВЕ вещи: загрузку конфига И построение матрицы
public InteractionMatrix loadInteractionMatrix(SpeciesRegistry registry) {
    InteractionMatrix matrix = new InteractionMatrix();
    for (SpeciesKey predatorKey : SpeciesKey.values()) {
        // ... построение матрицы взаимодействий
    }
    return matrix;
}
```

Построение `InteractionMatrix` — ответственность `InteractionMatrix` или её фабрики, не `ConfigLoader`.

**❌ Нарушение — `Configuration.java` (@Getter + ручные геттеры):**

```java
@Getter                   // Lombok генерирует getIslandWidth()
public class Configuration {
    public int getIslandWidth() { return islandWidth; }  // дублирует!
    public int getIslandHeight() { return islandHeight; } // дублирует!
```

Этот баг существует с PR #14 и не исправлен. Компиляция успешна только потому, что Lombok подавляет генерацию при наличии ручного метода — но это неочевидное поведение и нарушение принципа наименьшего удивления.

#### O — Open/Closed Principle

**✅ Кардинально улучшен:**

- `HuntingStrategy` интерфейс — добавление новой логики охоты без изменения `FeedingService`
- `DefaultHuntingStrategy` — конкретная реализация
- `Biomass` как абстракция — новый тип биомассы добавляется без изменения `Cell`

**❌ Нарушение — `SpeciesKey` enum:**

```java
public enum SpeciesKey {
    WOLF, BOA, FOX, BEAR, EAGLE, HORSE, DEER, RABBIT, MOUSE,
    GOAT, SHEEP, BOAR, BUFFALO, DUCK, CATERPILLAR, BUTTERFLY,
    HAMSTER, PLANT, GRASS, CABBAGE;

    public boolean isPredator() { ... }
}
```

Добавление нового вида (`Leopard`, `Crocodile`) требует **изменения enum** — это нарушение OCP. Enum по своей природе закрыт для расширения. Для доменной модели, где список видов расширяется в ходе разработки, enum-based реестр создаёт friction.

**Альтернатива:** `SpeciesKey` как неизменяемый value object (String wrapper или record), загружаемый из конфига.

#### L — Liskov Substitution Principle

**✅ Исправлен `Caterpillar extends Plant` → `Caterpillar extends Biomass`.**

Теперь гусеница — контейнер биомассы, это семантически верно. `Plant` тоже extends `Biomass`. Иерархия:

```
Organism
├── Animal
│   ├── Predator (abstract class)
│   │   └── Wolf, Boa, Fox, Bear, Eagle
│   └── Herbivore (interface)
│       └── Rabbit, Horse, ...
└── Biomass
    ├── Grass, Cabbage (растения)
    └── Caterpillar, Butterfly (рои)
```

**⚠️ Новое нарушение — `abstract class Predator`:**

Конкретные хищники теперь наследуют от `abstract class Predator`. При этом `Herbivore` — интерфейс. Асимметрия: нельзя добавить `Omnivore` (медведь) который был бы и `Predator`, и `Herbivore` без множественного наследования.

```java
// Медведь не может одновременно:
class Bear extends Predator { } // и
class Bear extends Herbivore { } // Java не позволяет
// Правильно: оба должны быть интерфейсами
```

#### I — Interface Segregation Principle

**✅ Исправлен `OrganismBehavior` → разделён на `Mobile`, `Consumer`, `Reproducible`.**

Растения больше не реализуют `move()`. Это правильное разделение.

**⚠️ `OrganismBehavior` интерфейс всё ещё присутствует** (в файловом дереве есть `OrganismBehavior.java`). Нужно убедиться что он не является fat interface в своём текущем виде, и не используется как "запасной вариант".

#### D — Dependency Inversion Principle

**✅ Значительно улучшен:**

- `SimulationContext` передаёт зависимости через конструктор
- `HuntingStrategy` инжектируется в `FeedingService`
- `SpeciesRegistry` инжектируется, не создаётся внутри сервисов

**❌ Нарушение — `ConfigLoader` и `SpeciesKey`:**

`ConfigLoader.loadInteractionMatrix()` итерирует `SpeciesKey.values()` — это прямая зависимость на конкретный enum. При изменении источника ключей видов (`SpeciesKey` из enum → из конфига) нужно менять `ConfigLoader`.

---

### 2.2 Признаки антипаттернов

**God Object:** Не наблюдается. `Island` крупный, но задача у него одна — пространственная модель.

**Feature Envy:** Потенциально в `FeedingService` — если он много читает из `Animal` для принятия решений об охоте. `HuntingStrategy` должен это инкапсулировать.

**Shotgun Surgery:** Добавление нового вида требует: 1) новый класс вида, 2) обновление `SpeciesKey`, 3) обновление `species.properties`, 4) обновление `AnimalFactory`. 4 файла — умеренная связанность.

**Divergent Change:** `SimulationConstants` — если он изменяется по разным причинам (баланс охоты, баланс метаболизма, баланс размножения) — это признак divergent change. `EnergyPolicy` enum частично решает это.

---

### 2.3 Нужно ли разделять хищников и травоядных?

**Ответ: да, уже сделано через `abstract class Predator` и `Herbivore` интерфейс.** Но асимметрия `abstract class` vs `interface` создаёт проблемы с `Omnivore`. Рекомендация — оба как интерфейсы.

### 2.4 Крупные и мелкие существа — `SizeClass` enum

**`SizeClass` enum добавлен — правильно.** Позволяет `PreyProvider` фильтровать добычу по размеру без `instanceof`. Вопрос: используется ли `SizeClass` в `FeedingService` для оптимизации поиска добычи?

### 2.5 Паттерны — оценка

| Паттерн | Статус | Оценка |
|---|---|---|
| **Strategy** (`HuntingStrategy`) | ✅ Реализован | Правильно |
| **Flyweight** (`AnimalType`) | ✅ Реализован | Правильно |
| **Factory** (`AnimalFactory`) | ✅ Реализован | Правильно |
| **Mediator** (`PreyProvider`) | ✅ Реализован | Правильно |
| **Composite** (`Island→Chunk→Cell`) | ✅ Реализован | Правильно |
| **Template Method** (`AbstractService`) | ✅ Реализован | Правильно |
| **State** | ❌ Отсутствует | Bear Hibernation реализован через if-в-методе, State паттерн дал бы чище |
| **ECS** | ❌ Не нужен | Для учебного проекта OOP достаточен |
| **Observer** (Statistics) | ❌ Отсутствует | Сбор статистики вшит в сервисы, лучше через события |

---

## 3. Алгоритмы и логика

### 3.1 Эффективность симуляции

**Сильные стороны:**
- Virtual Threads для параллельного выполнения чанков — правильный выбор
- `AtomicInteger` счётчики в `Island` — O(1) популяция без full-scan
- `EnumMap` в `Cell` — O(1) доступ к биомассе по виду
- for-i вместо Stream в hot-path — 3–5x быстрее

**Узкие места:**

#### 3.1.1 `StabilityIntegrationTest` показывает вымирание базовых сущностей

```
=== STABILITY CHECK REPORT (40 Ticks) ===
Survived | Extinct Species
18       | butterfly, plant
```

**Растения вымирают за 40 тиков** — это критический баланс-баг. Экосистема без растений нежизнеспособна: травоядные умрут от голода, затем хищники. Тест "проходит", потому что он не проверяет выживаемость конкретных видов — только что симуляция не падает с исключением.

Причина, скорее всего, в параметрах `PLANT_GROWTH_RATE_MAX = 0.80` (очень быстрый рост) в сочетании с высоким `OFFSPRING_INSECT = 15` (быстрое размножение гусениц). Если гусеницы растут быстрее растений — биологический маятник схлопывается.

#### 3.1.2 226,379 организмов при инициализации

```
Total organisms after initialization: 226379
```

На острове 100×20 = 2000 ячеек. В среднем **113 организмов на ячейку** при старте. Это создаёт:
- Высокое давление на GC при первых тиках
- Риск немедленного вымирания от перенаселения

Норма по задаче: 10–45% заполнения ячейки. При `maxPerCell` для кроликов = 150, гусениц (как биомасса, без лимита?) возможно инициализируется слишком много.

#### 3.1.3 ROI-охота — проверить threshold

Если `ROI = expected_gain × probability < hunt_cost`, хищники голодают при пустых ячейках. При малой популяции добычи — хищники перестают охотиться раньше чем умирают от голода. Это правильно биологически, но нужен тест, подтверждающий что хищники не выживают вечно при нулевой добыче.

### 3.2 Лишние проходы по коллекциям

**`ConfigLoader.loadInteractionMatrix()` — двойная итерация:**

```java
for (SpeciesKey predatorKey : SpeciesKey.values()) {
    for (SpeciesKey preyKey : SpeciesKey.values()) {  // O(N²) — 20×20=400 итераций
        // ...
    }
    // Затем СНОВА для PLANT:
    int plantChance = registry.getHuntProbability(predatorKey, SpeciesKey.PLANT);
}
```

`PLANT` уже входит в `SpeciesKey.values()` — проверка дублируется. Это O(N²)+O(N) вместо O(N²). Тривиально, но показывает невнимательность.

### 3.3 Bear Hibernation — алгоритмическая проблема

Если спячка реализована через:
```java
if (currentTick <= 50) { // нет метаболизма
```

Это глобальный тик, не тик жизни конкретного медведя. Все медведи просыпаются одновременно на тике 51 — это неестественно и создаёт "лавину охоты" в один момент.

Правильно: каждый медведь отсчитывает свои 50+100 тиков от момента рождения:
```java
private int ticksAlive = 0;
private boolean isHibernating() {
    return (ticksAlive % 150) < 50; // 50 спит, 100 активен, цикл
}
```

---

## 4. Работа с коллекциями и структурами данных

### 4.1 Что используется правильно

| Структура | Где | Почему правильно |
|---|---|---|
| `EnumMap<SpeciesKey, Biomass>` | `Cell` | O(1) доступ по виду, нет boxing |
| `AtomicInteger` (в Island) | Счётчики популяции | Thread-safe, O(1), lock-free |
| `ArrayList` + `ReentrantLock` | Животные в Cell | Write-heavy, правильный выбор vs COWAL |
| `ConcurrentHashMap` | Island.speciesCounts | Thread-safe без global lock |

### 4.2 Потенциальные проблемы

#### `SpeciesKey` enum в `EnumMap` — OCP риск

```java
// Cell.java:
private final EnumMap<SpeciesKey, Biomass> biomassContainers =
    new EnumMap<>(SpeciesKey.class);
```

`EnumMap` работает только с `enum`. При переходе `SpeciesKey` на другой тип (String, record) — всю коллекционную логику Cell нужно переписать.

#### Нет выделенного списка для хищников vs травоядных

`Cell.animals` — единый список. Чтобы найти добычу волка, нужно фильтровать всех животных. При 100+ животных в ячейке `PreyProvider` выполняет O(n) filter при каждом вызове.

Улучшение:
```java
// В Cell — два списка:
private final List<Animal> predators  = new ArrayList<>(4);
private final List<Animal> herbivores = new ArrayList<>(30);
// PreyProvider.getAvailablePrey(wolf) → cell.getHerbivores() // O(1)
```

#### `InteractionMatrix` — проверить тип хранения

Если матрица хранится как `Map<String, Map<String, Integer>>` — каждый lookup = 2 HashMap + String.equals. При 20 видах и 300+ организмах за тик это миллионы строковых операций. С `SpeciesKey` enum возможен переход на `int[][]` — O(1) по индексу без аллокаций.

### 4.3 Уменьшение аллокаций

**Butterfly — 25% диффузия биомассы:**

Если `Butterfly.move()` создаёт новый объект при каждом перемещении — это лишние аллокации. Правильно: перемещать ссылку на существующий `Biomass` объект, не создавать копию.

---

## 5. Качество кода

### 5.1 Что улучшилось ✅

- `EnergyPolicy` enum вместо мутируемых double-констант — отличное решение
- `DeathCause` enum — чёткая типизация причин смерти
- `SpeciesKey` enum — type-safe keys вместо строк
- `SizeClass` enum — классификация существ
- Checkstyle 0 violations — консистентное форматирование
- Удалены `System.out.println` из production кода
- `UML.md` + `CHANGELOG.md` — документация присутствует

### 5.2 Проблемы

#### Несогласованность `@Getter` и ручных геттеров (существует с PR#14)

```java
// Configuration.java:
@Getter                              // Lombok генерирует getIslandWidth()
public class Configuration {
    public int getIslandWidth() { ... }  // ручной дубликат
    public int getIslandHeight() { ... } // ручной дубликат
    // tickDurationMs — только Lombok (нет ручного)
```

Частичная автоматизация хуже чем полная ручная или полная автоматическая. Убрать `@Getter`.

#### `UML.md` устарел

```mermaid
Plant <|-- Caterpillar : "Optimized Biomass"
```

В коде теперь `Biomass <|-- Caterpillar`. Документация лжёт о текущей архитектуре.

#### `REFACTORING_BIOMASS_REPORT.md` — внутренний артефакт AI-агента

```
*Report generated by Gemini CLI Agent.*
```

Этот файл — internal work log от AI-агента (Gemini CLI), случайно попавший в коммит. Не подходит для включения в main. Удалить из PR.

#### Commit History — слишком шумный

```
SizeClass enum        → 32d0bdc
SizeClass enum        → 7ab736d   (дубликат!)
SizeClass enum NORMAL → 99d0278   (итерация на одном файле)
```

3 коммита для одного enum — признак отладки через git вместо локального рефакторинга. Перед merge в main нужно **squash** в смысловые коммиты.

#### `SimulationConstants` — ещё проверить наличие `final`

Если поля до сих пор `public static double` без `final` — критическая проблема для многопоточности и тестов (описана в предыдущем ревью). Нужно убедиться что в этом PR добавлен `final`.

#### Комментарий на русском в `Configuration.java`

```java
// Загрузка параметров из файла  ← русскоязычный комментарий (+ оставшийся от старого кода)
```

Мертвый код предыдущей версии присутствует в diff как ghost code — нужно убедиться что файл полностью очищен.

### 5.3 Нейминг

| Имя | Проблема | Рекомендация |
|---|---|---|
| `getBiomassContainers()` | Длинное, возвращает `EnumMap` | `getBiomass()` |
| `checkAgeDeath()` | Глагол + существительное | `isDeadFromAge()` / `checkAge()` |
| `loadInteractionMatrix()` | Метод в неправильном классе | Перенести в `InteractionMatrix.load()` |
| `redBookProtectionEnabled` | Специфичная реализация в публичном API | `isSpeciesProtected(key)` |

---

## 6. Тестируемость

### 6.1 Что хорошо

- 22 теста, все проходят
- Тесты реальные, не тривиальные: `TrophicFeedingTest`, `BiologicalPendulumTest`
- `BoundaryConditionsTest` — хорошая идея для edge cases
- `EcosystemStabilityTest` — интеграционный тест стабильности

### 6.2 Критические проблемы тестирования

#### `StabilityIntegrationTest` пропускает вымирание растений

```
Survived | Extinct Species
18       | butterfly, plant
```

Тест **проходит** при вымирании `plant` — базового ресурса экосистемы. Это означает:
1. Тест не проверяет выживаемость ключевых видов
2. Или растения как отдельный вид ("plant") заменены Grass/Cabbage, и это ожидаемо

Нужна явная проверка:
```java
assertTrue(survived.contains("grass"),
    "Grass must survive — ecosystem collapses without primary producers");
```

#### `EcosystemStabilityTest` — что именно проверяется?

Без просмотра кода непонятно какой assertion стоит в тесте. Если он только проверяет отсутствие исключений за N тиков — это smoke test, не stability test.

#### Тесты зависят от глобального состояния `SimulationConstants`

Если в тестах есть:
```java
SimulationConstants.SOME_FIELD = testValue; // мутация статика
```
Это делает тесты порядкозависимыми. Нужно убедиться что `final` добавлен.

#### Отсутствие тестов для новых механик

- `HuntingStrategy` — нет unit-теста для `DefaultHuntingStrategy`
- `SizeClass` — нет теста классификации
- `EnergyPolicy` — нет теста что значения консистентны между enum и сервисами
- `Bear Hibernation` — нет теста
- `Butterfly lifecycle` — butterfly вымирает (по тесту), нет теста воспроизводства

---

## 7. Масштабируемость и расширяемость

### 7.1 Добавление нового вида

**Текущий процесс (4 шага + риски):**

1. Создать класс (напр. `Leopard.java`) ✅ лёгко
2. Добавить в `SpeciesKey` enum ❌ нарушение OCP
3. Добавить в `species.properties` ✅ лёгко
4. Зарегистрировать в `AnimalFactory` ✅ лёгко
5. Если хищник — наследовать от `abstract Predator` ✅ лёгко

Шаг 2 — системная проблема. `SpeciesKey` как enum означает что список видов задан на этапе компиляции. Нельзя добавить вид через конфиг без перекомпиляции.

### 7.2 Изменение правил симуляции

- Новая стратегия охоты → создать новый `HuntingStrategy` ✅ OCP соблюдён
- Новая модель метаболизма → нужно менять `LifecycleService` ⚠️
- Новый тип биомассы → создать `extends Biomass` ✅ OCP соблюдён
- Изменить параметры баланса → `species.properties` ✅ лёгко

### 7.3 Где код сломается при росте требований

**Сценарий: добавить 50+ видов**
- `SpeciesKey` enum становится огромным
- `EnumMap` в `Cell` всегда выделяет слоты для всех видов (даже если их нет в ячейке)
- `InteractionMatrix` вырастает до 50×50 = 2500 ячеек

**Сценарий: добавить события (засуха, пожар, миграция)**
- Нет системы событий. Всё придётся добавлять в `GameLoop` как if-условия
- Отсутствие Observer/EventBus — накапливается связанность

**Сценарий: UI/API для наблюдения симуляции**
- `ConsoleView` смешан с логикой рендеринга — нет чёткого разделения View/Model
- Нет интерфейса `SimulationView` — подключить другой рендерер сложно

---

## 8. Потенциальные баги и риски

### 🔴 BUG-1: Растения вымирают в `StabilityIntegrationTest`

```
Extinct Species: butterfly, plant
```

Если "plant" означает конкретный вид (а не Grass/Cabbage как подвиды), то базовый ресурс уничтожен. Экосистема без первичных продуцентов нежизнеспособна. Тест не фиксирует это как провал.

**Причина:** возможно `PLANT_GROWTH_RATE_MAX = 0.80` создаёт переокисление → гусеницы съедают всё → маятник схлопывается.

### 🔴 BUG-2: `Configuration.java` — конфликт `@Getter` и ручных геттеров

Lombok при наличии ручного геттера не генерирует свой — это задокументированное поведение. Но `tickDurationMs` не имеет ручного геттера, значит Lombok генерирует только для него. Итог: разные геттеры имеют разное происхождение — это сбивает с толку и нарушает консистентность.

### 🔴 BUG-3: `REFACTORING_BIOMASS_REPORT.md` — не должен попасть в main

Внутренний артефакт AI-агента попал в PR. Это не баг симуляции, но загрязняет репозиторий служебными данными.

### 🟡 РИСК-1: 226,379 организмов при инициализации — перегрузка GC

При старте 226k объектов создаются немедленно. Первые 5–10 тиков — массовая смерть от перенаселения. Это создаёт spike на GC, который может вызвать задержки тиков и нарушение `scheduleWithFixedDelay`.

### 🟡 РИСК-2: Virtual Threads + `synchronized consumeEnergy` = Pinning

```java
public synchronized void consumeEnergy(double amount) { ... }
```

`synchronized` в Java 21 с Virtual Threads вызывает **pinning** — виртуальный поток не может быть unmounted пока держит monitor. Это нивелирует преимущество Virtual Threads при высоком contention.

**Исправление:** `ReentrantLock` вместо `synchronized`:
```java
private final ReentrantLock energyLock = new ReentrantLock();
public void consumeEnergy(double amount) {
    energyLock.lock();
    try { ... }
    finally { energyLock.unlock(); }
}
```

### 🟡 РИСК-3: Bear Hibernation — глобальный тик vs тик жизни

Если `tick <= 50` — все медведи действуют синхронно, что неестественно и создаёт batch-эффекты.

### 🟡 РИСК-4: `SpeciesKey` как enum — не расширяемый

Нельзя добавить вид через конфиг. При добавлении 5+ новых видов enum нужно менять вручную.

### 🟢 РИСК-5: `HuntingStrategy` — single implementation

Если в системе только `DefaultHuntingStrategy`, стратегия де-факто не является расширяемой точкой — нет реального полиморфизма. Нужна хотя бы одна альтернативная реализация (тест или специализированная) для подтверждения что паттерн работает.

---

## 9. Конкретные рекомендации

### [HIGH] Исправить до merge в main

- **[HIGH]** Исправить баланс экосистемы — растения не должны вымирать за 40 тиков. Добавить assertion в `StabilityIntegrationTest`.
- **[HIGH]** Заменить `synchronized` → `ReentrantLock` в `consumeEnergy` (pinning с Virtual Threads).
- **[HIGH]** Удалить `REFACTORING_BIOMASS_REPORT.md` из PR — это внутренний лог AI-агента.
- **[HIGH]** Убрать `@Getter` из `Configuration.java` (конфликт с ручными геттерами).
- **[HIGH]** Squash 63 коммитов в 5–8 смысловых перед merge в main.
- **[HIGH]** Обновить `UML.md` — убрать `Plant <|-- Caterpillar`, добавить `Biomass`.
- **[HIGH]** Добавить `final` ко всем полям `SimulationConstants` если ещё не сделано.

### [MEDIUM] Следующий спринт

- **[MEDIUM]** Изменить `abstract class Predator` → интерфейс `Predator` для поддержки `Omnivore`.
- **[MEDIUM]** Разделить `Cell.animals` на `predators` + `herbivores` списки для O(1) доступа.
- **[MEDIUM]** `InteractionMatrix` — перейти на `int[][]` с индексами `SpeciesKey.ordinal()`.
- **[MEDIUM]** `Bear.isHibernating()` — отсчёт от `ticksAlive`, не от глобального тика.
- **[MEDIUM]** Добавить `SimulationView` интерфейс для отделения `ConsoleView` от GameLoop.
- **[MEDIUM]** Добавить тест для `HuntingStrategy` — как минимум smoke test альтернативной реализации.
- **[MEDIUM]** `ConfigLoader.loadInteractionMatrix()` → переместить в `InteractionMatrix.buildFrom(registry)`.
- **[MEDIUM]** Тест `StabilityIntegrationTest` — добавить assertion выживаемости `grass`/`cabbage`.
- **[MEDIUM]** Снизить initial population — 226k объектов слишком много для стабильного старта.

### [LOW] Технический долг

- **[LOW]** `SpeciesKey` — рассмотреть `record SpeciesKey(String id)` вместо enum для OCP.
- **[LOW]** `HuntingStrategy` — добавить вторую реализацию (например, пассивная засада `AmbushStrategy`).
- **[LOW]** `Observer/EventBus` — ввести для событий симуляции (смерть вида, переполнение ячейки).
- **[LOW]** `State` паттерн для `Bear` (Hibernating/Active/Hungry states).
- **[LOW]** Javadoc на всех новых публичных методах.
- **[LOW]** `balance-log.md` — документировать обоснование числовых параметров.

---

## 10. Рефакторинг — практические примеры

### Пример 1: `Predator` как интерфейс вместо abstract class

**Проблема:** `abstract class Predator` блокирует `Omnivore` (медведь).

```java
// БЫЛО — abstract class создаёт hierarchy lock:
public abstract class Predator extends Animal {
    // общее поведение хищника
}
public class Wolf extends Predator { }
public class Bear extends Predator { } // Медведь не может быть и Herbivore

// СТАЛО — два интерфейса + default методы для общего поведения:
public interface Predator {
    /** Returns the preferred size categories of prey for this predator. */
    Set<SizeClass> getPreferredPreySizes();

    /** Returns true if this predator uses ambush tactics (reduced energy cost). */
    default boolean isAmbushPredator() { return false; }
}

public interface Herbivore {
    /** Returns preferred plant types. */
    Set<SpeciesKey> getPreferredPlants();
}

// Теперь медведь может быть обоим:
public class Bear extends Animal implements Predator, Herbivore {
    @Override
    public Set<SizeClass> getPreferredPreySizes() {
        return Set.of(SizeClass.SMALL, SizeClass.NORMAL);
    }

    @Override
    public Set<SpeciesKey> getPreferredPlants() {
        return Set.of(SpeciesKey.GRASS);
    }

    // Hibernation — через State:
    private BearState state = BearState.HIBERNATING;
    private int stateTick = 0;

    @Override
    public synchronized void consumeEnergy(double amount) {
        if (state == BearState.HIBERNATING) return; // нет расхода во сне
        super.consumeEnergy(amount);
    }
}

enum BearState { HIBERNATING, ACTIVE, HUNGRY }
```

---

### Пример 2: `SpeciesKey` как расширяемый value object (вместо enum)

**Проблема:** Новый вид = изменение enum = нарушение OCP.

```java
// БЫЛО — enum закрыт для расширения:
public enum SpeciesKey {
    WOLF, RABBIT, /* ... */ HAMSTER; // нельзя добавить без изменения файла
    public boolean isPredator() { ... }
}

// СТАЛО — record + registry:
public record SpeciesKey(String id) {
    // Встроенные константы для ключевых видов (backward compat):
    public static final SpeciesKey WOLF     = new SpeciesKey("wolf");
    public static final SpeciesKey RABBIT   = new SpeciesKey("rabbit");
    // ... остальные при необходимости

    // isPredator — не здесь! Это знает SpeciesRegistry:
    // registry.getType(key) → PREDATOR / HERBIVORE / PLANT
}

// SpeciesRegistry загружает из конфига:
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> types = new HashMap<>();

    public void registerFromProperties(Properties props) {
        // Читает все species.* ключи динамически
        // Новый вид = добавить строки в properties, не менять код
    }

    public boolean isPredator(SpeciesKey key) {
        return types.containsKey(key) && types.get(key).hasPredatorBehavior();
    }
}

// Cell переходит с EnumMap на:
private final Map<SpeciesKey, Biomass> biomassContainers = new HashMap<>();
// Или LinkedHashMap для предсказуемого порядка итерации
```

---

### Пример 3: `StabilityIntegrationTest` — сделать тест содержательным

**Проблема:** Тест проходит при вымирании растений — это false positive.

```java
// БЫЛО — тест только логирует, не проверяет:
@Test
void ecosystemSurvivesFor40Ticks() {
    // ... запуск симуляции ...
    System.out.println("Survived: " + survived + " | Extinct: " + extinct);
    // Никаких assertions о конкретных видах!
}

// СТАЛО — смысловые assertions с биологическим обоснованием:
@Test
@DisplayName("Primary producers (Grass, Cabbage) must survive 40 ticks")
void primaryProducersMustSurvive() {
    SimulationResult result = runSimulation(40);

    assertAll("Primary producers",
        () -> assertTrue(result.isAlive(SpeciesKey.GRASS),
            "Grass must survive — without it herbivores starve, ecosystem collapses"),
        () -> assertTrue(result.isAlive(SpeciesKey.CABBAGE),
            "Cabbage must survive — secondary plant layer")
    );
}

@Test
@DisplayName("At least one predator species must survive 40 ticks")
void atLeastOnePredatorMustSurvive() {
    SimulationResult result = runSimulation(40);
    long survivingPredators = PREDATOR_SPECIES.stream()
        .filter(result::isAlive)
        .count();
    assertTrue(survivingPredators >= 1,
        "At least one predator must survive to maintain trophic balance");
}

@Test
@DisplayName("No species explosion — population must stay bounded")
void noSpeciesExplosion() {
    SimulationResult result = runSimulation(40);
    for (SpeciesKey species : SpeciesKey.values()) {
        int count = result.getPopulation(species);
        int maxCapacity = island.getWidth() * island.getHeight()
            * registry.getMaxPerCell(species);
        assertTrue(count <= maxCapacity,
            "Species " + species + " exceeded island capacity: " + count + " > " + maxCapacity);
    }
}
```

---

## 11. Итоговая оценка

| Категория | Оценка | Комментарий |
|---|:---:|---|
| **Архитектура** | **8/10** | SOLID значительно улучшен. Оценку снижает нерасширяемый `SpeciesKey` enum и асимметрия Predator abstract class vs Herbivore interface |
| **Качество кода** | **7.5/10** | Checkstyle 0 violations, хорошие паттерны. Снижает: незакрытый @Getter баг, устаревший UML, шумная история коммитов, артефакт AI-агента в PR |
| **Алгоритмы** | **7.5/10** | Virtual Threads, EnumMap O(1), for-i loops — правильно. Снижает: вымирание растений за 40 тиков, 226k организмов на старте |
| **Поддерживаемость** | **8/10** | `EnergyPolicy`, `SizeClass`, `SpeciesRegistry` — отличные добавления. Снижает: SpeciesKey enum как OCP barrier |
| **Тесты** | **6.5/10** | 22 теста, но StabilityTest не детектирует экологический коллапс. Нет тестов для новых механик (HuntingStrategy, Butterfly lifecycle) |
| **Общая оценка** | **7.5/10** | |

### Резюме тимлида

**Этот код брать в main — нельзя в текущем состоянии.** Не из-за критических архитектурных проблем (архитектура стала хорошей), а из-за трёх конкретных вещей:

1. **Экосистема нестабильна** — растения вымирают за 40 тиков, тест это не ловит. Мержить нестабильную симуляцию в main бессмысленно.

2. **63 неструктурированных коммита** с историей балансировки, тремя "SizeClass enum", артефактом Gemini CLI — это не то что должно попасть в `main`. Нужен squash в 5–8 осмысленных коммитов с человекочитаемыми сообщениями.

3. **`@Getter` конфликт** — баг из PR #14, существует уже в двух PR без исправления.

После исправления HIGH-пунктов (2–3 часа работы) — это **хороший код**, который демонстрирует понимание архитектуры, многопоточности, паттернов проектирования и биологической модели на уровне выше среднего для учебного проекта. Автор явно думает об инженерных решениях, а не просто пишет код.

**Что особенно впечатляет:** `EnergyPolicy` enum вместо мутируемых констант, `Biomass` унификация, ROI-охота, `HuntingStrategy` OCP, Virtual Threads с правильной мотивацией, Checkstyle интеграция.

---

*Ревью выполнено по PR #17 (`dev` → `main`): 63 коммита, 74 Java файла, test output из комментария к PR*  
*Предыдущие ревью: PR #14 (commit b81e95f), PR #16 (commit dca2edf)*
