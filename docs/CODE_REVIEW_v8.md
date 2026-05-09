# Code Review v8: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-07  
**База:** v7 (монолит)  
**Ключевое изменение:** переход на Maven multi-module + module-info.java  
**Структура:** `island-engine` → `island-nature` / `island-simcity` → `island-app`

---

## Сводная таблица: прогресс относительно v7

| Задача | Статус | Детали |
|---|---|---|
| Maven multi-module структура | ✅ **ВЫПОЛНЕНО** | 4 модуля с правильным DAG зависимостей |
| `util/interaction` → `nature.model` | ✅ **ВЫПОЛНЕНО** | `InteractionMatrix`, `InteractionProvider` в `island-nature` |
| `module-info.java` для engine | ✅ **ВЫПОЛНЕНО** | Полный JPMS-манифест |
| `EntityBornEvent/EntityDiedEvent` удалены из engine | ✅ **ВЫПОЛНЕНО** | Остались только `EventBus`, `DefaultEventBus` |
| `EngineAPI` / `InternalEngine` аннотации | ✅ **ВЫПОЛНЕНО** | Оба файла созданы |
| `ConsumeAction<T>` вместо `BiFunction<Long, Object, Long>` | ✅ **ВЫПОЛНЕНО** | Типизированный функциональный интерфейс |
| SimCity получает ECS | ✅ **ВЫПОЛНЕНО** | `AbstractSimCitySystem`, `EconomySystem`, `PopulationSystem` |
| `ArchUnit` → `utilShouldNotDependOnDomain` | ✅ **ВЫПОЛНЕНО** | Новое правило добавлено |
| `ArchUnit` → `natureAndSimCityShouldNotDependOnEachOther` | ✅ **ВЫПОЛНЕНО** | Взаимная изоляция плагинов |
| `engine.parallel` / `engine.scheduling` — экспорт внутренних пакетов | ❌ **ПРОБЛЕМА** | Internal классы видны плагинам |
| SimCity тесты обходят `SimulationEngine`, используют внутренние классы | ❌ **ПРОБЛЕМА** | `ParallelDispatcher` в тест-коде плагина |
| `WorldInitializationTest` — неправильный модуль и пакет | ❌ **ПРОБЛЕМА** | Файл в `island-nature/test`, пакет `com.island.engine` |
| `@InternalEngine` / `@EngineAPI` не применены к классам | ⚠️ **ДОЛГ** | Аннотации есть, классы не размечены |
| Нет `module-info.java` для `island-nature`, `island-simcity` | ⚠️ **ДОЛГ** | JPMS защита только у engine |
| `EconomySystem.process()` — пустое тело | ⚠️ **ДОЛГ** | Заглушка без реализации |

---

## Граф зависимостей модулей

```
island-app  ─────────┬──→ island-nature ──→ island-engine
                     └──→ island-simcity ──→ island-engine

island-nature  НЕ зависит от island-simcity  ✅
island-simcity НЕ зависит от island-nature   ✅
island-engine  НЕ зависит от island-nature   ✅
island-engine  НЕ зависит от island-simcity  ✅
```

Граф ациклический, направление зависимостей правильное. Плагины не знают друг о друге.
ArchUnit верифицирует эти инварианты. Это **принципиально верная структура**.

---

## ✅ Детальный разбор правильных решений

### 1. Maven multi-module — структура выполнена корректно

```xml
<!-- Parent pom — правильные практики: -->
<packaging>pom</packaging>           <!-- parent не создаёт JAR -->
<dependencyManagement>               <!-- версии в одном месте -->
    <dependency>lombok — provided</dependency>  <!-- Lombok не в runtime -->
</dependencyManagement>

<!-- Дочерние pom — без дублирования версий: -->
<parent>island-simulator-parent</parent>
<dependency>island-engine</dependency>  <!-- без <version>: берётся из parent -->
```

Lombok управляется через `<dependencyManagement>` с `<scope>provided</scope>`.
Это значит `island-engine.jar` не тянет Lombok как транзитивную compile-зависимость.
Байткод Lombok-аннотаций работает без lombok.jar в runtime. ✅

### 2. module-info.java — правильная основа

```java
module com.island.engine {
    requires static lombok;   // ← compile-only, не runtime
    requires org.slf4j;

    exports com.island.engine.core;
    exports com.island.engine.ecs;
    exports com.island.engine.event;
    exports com.island.engine.model;
    exports com.island.engine.parallel;     // ← см. замечание ниже
    exports com.island.engine.scheduling;   // ← см. замечание ниже
    exports com.island.engine.service;
    exports com.island.util.common;
    exports com.island.util.math;
    exports com.island.util.sampling;
}
```

`requires static lombok` — annotation processor нужен только при компиляции, не в runtime.
Это корректно для библиотеки: пользователь engine-JAR не получает ненужный Lombok. ✅

### 3. ArchUnit — три правила закрывают главные границы

```java
void engineShouldNotDependOnDomain()           // engine → nature/simcity
void utilShouldNotDependOnDomain()             // util → nature/simcity
void natureAndSimCityShouldNotDependOnEachOther() // nature ↔ simcity
```

Три правила покрывают все стрелки в DAG, которые **не должны** существовать.
Нарушение детектируется в CI до code review. ✅

### 4. ConsumeAction<T> — типизированная замена BiFunction

```java
@FunctionalInterface
public interface ConsumeAction<T> {
    long consume(long requestedAmount, T context);
}
// Использование в NatureComponentFactory:
ConsumeAction<Cell> action = (requested, cell) -> biomass.consumeBiomass(requested, cell);
// Больше никакого instanceof Cell внутри лямбды
```

Тип контекста теперь явный. Компилятор защищает от передачи неверного типа. ✅

### 5. SimCity ECS — паритет с nature

`AbstractSimCitySystem` зеркалирует `NatureEntitySystem`: один `instanceof CityTile`
в базе, `process(entity, tile, tick)` в подклассах. `EconomySystem`, `PopulationSystem`
следуют той же структуре что и `AnimalHealthSystem`, `BiomassGrowthSystem`. Архитектурный
паритет между плагинами — хороший знак для переиспользуемости engine. ✅

---

## 🔴 Критические проблемы модульной структуры

### Проблема 1 [HIGH]: `engine.parallel` и `engine.scheduling` экспортируются как публичные пакеты

```java
// module-info.java
exports com.island.engine.parallel;    // ParallelDispatcher, ParallelTask
exports com.island.engine.scheduling;  // GameLoop, PhaseScheduler, Phase, ScheduledTask
```

Публичный экспорт означает: любой плагин может делать `new ParallelDispatcher(executor)`
и `new PhaseScheduler(dispatcher)` в production-коде. Именно это делают SimCity тесты.

**Почему это проблема:**

`ParallelDispatcher` и `PhaseScheduler` — это **implementation details** `SimulationEngine`.
Они могут быть заменены, переименованы или объединены без нарушения публичного API движка.
Если плагин зависит от них напрямую — любой рефакторинг внутренностей становится breaking change.

Исключение: `ScheduledTask`, `Phase`, `GameLoop.addRecurringTask()` — это **часть SPI**,
нужны плагину для регистрации задач. Их экспортировать правильно.

**Правильное разделение:**

```java
module com.island.engine {
    // ПУБЛИЧНЫЙ API — плагин ИСПОЛЬЗУЕТ:
    exports com.island.engine.core;        // SimulationEngine, SimulationPlugin, SimulationContext...
    exports com.island.engine.ecs;         // Component, EntitySystem, ComponentStore...
    exports com.island.engine.event;       // EventBus
    exports com.island.engine.model;       // Mortal, Tickable, WorldSnapshot
    exports com.island.engine.service;     // CellService

    // ПУБЛИЧНЫЙ SPI — плагин РЕАЛИЗУЕТ:
    exports com.island.engine.scheduling;  // ScheduledTask, Phase (нужны для addRecurringTask)
                                           // НО: GameLoop, PhaseScheduler — внутренние!

    // INTERNAL — плагин НЕ ДОЛЖЕН трогать:
    // com.island.engine.parallel  ← НЕ ЭКСПОРТИРОВАТЬ
    // PhaseScheduler              ← перенести в engine.internal или не экспортировать

    exports com.island.util.common;
    exports com.island.util.math;
    exports com.island.util.sampling;
}
```

**Практическое решение без переноса файлов:** использовать `exports ... to`:
```java
// Ограниченный экспорт — только для тестов и app, не для сторонних плагинов:
exports com.island.engine.parallel to com.island.nature, com.island.simcity;
// Или полностью убрать export — тогда плагин вынужден идти через SimulationEngine
```

---

### Проблема 2 [HIGH]: SimCity тесты напрямую создают внутренние классы движка

```java
// SimCitySmokeTest.java — обходит SimulationEngine
import com.island.engine.parallel.ParallelDispatcher;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.PhaseScheduler;

ParallelDispatcher<SimEntity> dispatcher = new ParallelDispatcher<>(executor);
PhaseScheduler<SimEntity> scheduler = new PhaseScheduler<>(dispatcher);
GameLoop<SimEntity> gameLoop = new GameLoop<>(0, executor, scheduler);
```

Это **API misuse**: тест плагина вручную собирает internal-компоненты движка вместо
использования `SimulationEngine.build()`. Это означает:
- Если изменится конструктор `PhaseScheduler` — тест сломается, хотя API не менялся
- Тест не покрывает реальный production-путь (через `SimulationEngine`)
- JPMS не защитит от этого, пока `engine.parallel` экспортируется

```java
// Правильная версия теста:
SimulationEngine<SimEntity> engine = new SimulationEngine<>();
SimulationContext<SimEntity> context = engine.build(new SimCityPlugin(), 0, 2);
context.gameLoop().runTick();
assertThat(context.world()).isNotNull();
```

---

### Проблема 3 [MEDIUM]: `WorldInitializationTest` — неправильный модуль и пакет

```
Файл:    island-nature/src/test/java/com/island/engine/WorldInitializationTest.java
Пакет:   package com.island.engine;   ← !!
Модуль:  island-nature (тест-класспас)
```

Тест находится в модуле `island-nature`, но объявлен в пакете `com.island.engine`.
Это антипаттерн «white-box test через package-private доступ». В текущей конфигурации
без JPMS для тестов это работает. Но:

1. Семантически неверно: тест природного домена не должен быть в пакете engine
2. При включении JPMS для тестов (`--add-opens`) это сломается
3. Вводит в заблуждение — разработчик не понимает, почему тест engine-класса в nature-модуле

```java
// Правильно:
// Переместить в island-engine/src/test/java/com/island/engine/WorldInitializationTest.java
// Или переименовать пакет: package com.island.nature.integration;
```

---

## 🟡 Менее критичные замечания

### 4. `@EngineAPI` / `@InternalEngine` — аннотации без применения

```java
// Аннотации существуют...
@Retention(RetentionPolicy.SOURCE)
public @interface EngineAPI { }

@Retention(RetentionPolicy.SOURCE)
public @interface InternalEngine { }

// ...но применяются только к одному классу:
@InternalEngine
public class ParallelDispatcher<T extends Mortal> { ... }

// Не применяются к:
// SimulationEngine, SimulationPlugin, EventBus, ComponentStore — должны быть @EngineAPI
// PhaseScheduler, CellProcessor — должны быть @InternalEngine
```

`@Retention(RetentionPolicy.SOURCE)` — аннотации существуют только в исходнике, исчезают
при компиляции. Это значит ArchUnit **не может** их проверить (`ClassFileImporter`
читает bytecode, где SOURCE-аннотаций нет).

**Два выхода:**
- Изменить на `RetentionPolicy.CLASS` — аннотации видны в bytecode, ArchUnit может их проверять
- Добавить ArchUnit правило: `@InternalEngine`-классы не должны импортироваться из других модулей

---

### 5. Нет `module-info.java` для `island-nature` и `island-simcity`

Только `island-engine` объявлен как named Java module. `island-nature` и `island-simcity`
остаются unnamed modules (classpath). Это создаёт асимметрию:

- Engine защищён JPMS: нельзя случайно получить доступ к неэкспортированным пакетам
- Nature/SimCity не защищены: любой класс в nature доступен из app напрямую

Для полной модульной изоляции нужны `module-info.java` для обоих плагинов:

```java
// island-nature/src/main/java/module-info.java
module com.island.nature {
    requires com.island.engine;
    requires static lombok;
    requires org.slf4j;

    // Публичное SPI — что плагин предоставляет наружу:
    exports com.island.nature;  // NaturePlugin

    // Всё остальное — internal:
    // entities.*, model.*, service.*, config.* — НЕ экспортируются
}
```

---

### 6. ArchUnit — отсутствует правило для internal engine пакетов

Текущие три правила не проверяют, что плагин не использует internal классы движка:

```java
// Добавить в ArchitectureTest:
@Test
void pluginsShouldNotUseEngineInternals() {
    noClasses().that().resideInAnyPackage("..nature..", "..simcity..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "com.island.engine.parallel..",   // ParallelDispatcher — internal
            "com.island.engine.scheduling.."  // PhaseScheduler — internal
            // GameLoop, ScheduledTask, Phase — допустимы
        )
        .ignoreDependency(
            // Исключение: ScheduledTask и Phase нужны для регистрации задач
            resideInAPackage("..scheduling..").and(
                haveSimpleName("ScheduledTask").or(haveSimpleName("Phase"))
            )
        )
        .check(classes);
}
```

---

### 7. `EconomySystem.process()` — пустое тело

```java
@Override
protected void process(SimEntity entity, CityTile tile, int tickCount) {
    // Logic will iterate over relevant entities and update shared state
}
```

Заглушка в production-коде. Допустима на ранней стадии, но требует отметки:

```java
// TODO(MVP): implement economy logic
throw new UnsupportedOperationException("EconomySystem not yet implemented");
// или явный TODO-ticket
```

---

## 📊 Оценка правильности разделения на модули

### Что реализовано правильно

| Аспект | Оценка | Комментарий |
|---|---|---|
| Граф зависимостей (DAG) | ✅ Отлично | Нет циклов, правильное направление |
| engine domain-free | ✅ Отлично | Нет импортов nature/simcity |
| util изоляция | ✅ Отлично | InteractionMatrix перемещён |
| Плагины не зависят друг от друга | ✅ Отлично | nature ↔ simcity изолированы |
| Lombok как provided | ✅ Отлично | Не тянется в runtime |
| ArchUnit покрытие межмодульных границ | ✅ Хорошо | 3 правила |

### Что требует доработки

| Аспект | Оценка | Комментарий |
|---|---|---|
| Разграничение engine API vs internal | ⚠️ Частично | `engine.parallel` экспортируется |
| Использование внутренних классов в тестах | ❌ Проблема | SimCity тесты — ParallelDispatcher |
| module-info для плагинов | ⚠️ Отсутствует | Только у engine |
| ArchUnit для internal доступа | ⚠️ Отсутствует | Нет правила против misuse |
| Тест в неверном модуле | ❌ Ошибка | WorldInitializationTest |

---

## 💡 Конкретные шаги для завершения модульного разделения

### Шаг 1: Разделить экспорты engine на API и internal (0.5 дня)

```java
module com.island.engine {
    requires static lombok;
    requires org.slf4j;

    // SPI + API — плагин использует и реализует
    exports com.island.engine.core;
    exports com.island.engine.ecs;
    exports com.island.engine.event;
    exports com.island.engine.model;
    exports com.island.engine.service;
    exports com.island.engine.scheduling;  // ScheduledTask, Phase нужны
    exports com.island.util.common;
    exports com.island.util.math;
    exports com.island.util.sampling;

    // УБРАТЬ или ограничить:
    // exports com.island.engine.parallel;  ← убрать полностью
    // GameLoop и PhaseScheduler — оставить в scheduling, но задокументировать как internal
}
```

### Шаг 2: Исправить SimCity тесты — использовать SimulationEngine (1 день)

```java
// SimCitySmokeTest — правильный паттерн
@Test
void cityShouldRunWithoutErrors() {
    SimulationContext<SimEntity> ctx = new SimulationEngine<SimEntity>()
        .build(new SimCityPlugin(), 0, 2);
    ctx.gameLoop().runTick();
    assertThat(ctx.world().createSnapshot()).isNotNull();
    ctx.gameLoop().stop();
}
```

### Шаг 3: Переместить WorldInitializationTest (0.5 часа)

```
island-nature/src/test/java/com/island/engine/WorldInitializationTest.java
→ island-engine/src/test/java/com/island/engine/WorldInitializationTest.java
   (или package com.island.nature.integration)
```

### Шаг 4: Добавить ArchUnit правило для internal engine (0.5 дня)

```java
@Test
void pluginsShouldNotInstantiateEngineInternals() {
    noClasses().that().resideInAnyPackage("..nature..", "..simcity..")
        .should().dependOnClassesThat()
        .haveNameMatching(".*ParallelDispatcher|.*PhaseScheduler|.*CellProcessor")
        .check(classes);
}
```

### Шаг 5: Сменить @EngineAPI/@InternalEngine на RetentionPolicy.CLASS (0.5 часа)

```java
@Retention(RetentionPolicy.CLASS)  // было SOURCE
public @interface EngineAPI { }
```

Разметить все публичные классы движка. Добавить ArchUnit-проверку по аннотации.

### Шаг 6 (опционально): `module-info.java` для island-nature и island-simcity

Это даёт полную JPMS-изоляцию. Требует тщательной проверки `opens` для тестов
(JUnit/Mockito требуют `--add-opens`). Осложняется тем, что Mockito плохо дружит
с JPMS без дополнительной конфигурации.

---

## 📊 Итоговые оценки

| Критерий | v1 | v5 | v7 | v8 |
|---|---|---|---|---|
| **Архитектура** | 6.5 | 8.5 | 9.0 | **9.0** |
| **Код** | 7.0 | 8.0 | 8.5 | **8.5** |
| **Переиспользуемость** | 6.0 | 8.5 | 9.0 | **9.5** |
| **Тестируемость** | 5.0 | 8.0 | 9.0 | **9.0** |
| **Готовность к JAR-библиотеке** | — | — | 6.5 | **8.5** |
| **Общая** | 6.5 | 8.5 | 9.0 | **9.0** |

---

## Вердикт

**Разделение на модули выполнено правильно по структуре, но не до конца по контракту.**

Граф зависимостей, Maven-организация, `module-info.java`, перемещение `util/interaction`
в nature, изоляция плагинов друг от друга, Lombok как `provided` — всё это сделано верно.
`island-engine.jar` сегодня можно опубликовать в Maven, и сторонний разработчик сможет
написать свой плагин.

Три вещи мешают назвать разделение **завершённым**:

1. `engine.parallel` экспортируется — внутренние классы видны плагинам
2. SimCity тесты используют эти внутренние классы напрямую, закрепляя нежелательную зависимость
3. `WorldInitializationTest` в неверном модуле создаёт ложное ощущение что тесты engine проверяются из nature

Устранение всех трёх займёт **1-2 рабочих дня**. После этого движок будет готов к
публикации как автономная библиотека с версионированным, стабильным API.
