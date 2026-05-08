У меня полная картина по всем трём уровням изменений. Пишу ревью.

---

## Code Review #3: Island dev — модульная архитектура

### Что закрыто из Review #2

Все 5 критичных и средних замечаний закрыты — это хороший темп:

| Замечание | Статус |
|---|---|
| GC-регрессия в `PhaseScheduler` | ✅ Поля вынесены, добавлено кэширование графа |
| Двойной путь остановки / `onSimulationStopped` не гарантирован | ✅ `onStopCallback` в `GameLoop`, устанавливается в `SimulationEngine.build()` |
| FQN в теле `PhaseScheduler` | ✅ Заменён импортом |
| `SimulationWorld.onEntityAdded/onEntityRemoved` без дефолта | ✅ Добавлены `default void` |
| `ArchitectureEvolutionTest` нарушал слоёвость | ✅ Интегрирован в `ArchitectureTest` |
| `NaturePlugin` конструктор — 14 строк ручной сборки | ✅ Вынесен в `NatureDomainContextFactory` |
| Magic numbers в `Island.partitionIntoChunks()` | ✅ Переехали в `Configuration` |

Дополнительно появились: `WorkUnit<T>` с телеметрией, `DynamicChunkingStrategy`, `ClimateService`, headless-режим, SimCity на чистом ECS — всё это архитектурно правильные шаги.

---

### 🔴 Критично

---

**1. Призрачное дерево `src/` — 83 дублирующих файла в репозитории**

```
dev/
├── src/main/java/com/island/nature/   ← 83 файла, НЕ входят ни в один модуль
├── island-engine/
├── island-nature/
├── island-simcity/
└── island-app/
```

Корневой `pom.xml` объявляет только 4 `<module>`: `island-engine`, `island-nature`, `island-simcity`, `island-app`. Старая плоская директория `src/` в них не входит — Maven её игнорирует. Но для разработчика в IDE это невидимая ловушка: две копии каждого класса, и неясно какую редактировать.

Один файл там *уникален* и вводит в заблуждение сильнее всего:

```
src/main/java/com/island/nature/NatureLauncher.java   ← package com.island.nature  ❌
island-app/.../com/island/NatureLauncher.java          ← package com.island         ✅
```

Если кто-то откроет не тот файл и отредактирует его — изменения тихо потеряются, и никакой ошибки компиляции не будет.

**Действие**: удалить `src/` из корня репозитория целиком.

---

**2. `ArchitectureTest` потерял главное правило — `engine` не должен зависеть от `nature`**

```java
// island-engine/src/test/java/com/island/ArchitectureTest.java
// Единственное правило:
noClasses().that().resideInAPackage("com.island.util..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("com.island.nature..", "com.island.simcity..");
```

В PR21 тест проверял `engine → nature`. Это правило исчезло. При этом тест живёт в `island-engine`, у которого нет `island-nature` в `pom.xml` — значит на classpath во время теста нет `nature`-классов вообще. ArchUnit не найдёт их при `importPackages("com.island")` и правило `util → nature` пройдёт вакуумно: нечего нарушать.

Maven-зависимости дают защиту на уровне модулей — если кто-то добавит `island-nature` в `island-engine/pom.xml`, это видно сразу. Но ArchUnit-тест в нынешнем виде не ловит нарушения на уровне Java-кода и создаёт ложное ощущение безопасности.

Как исправить — перенести тест в `island-app` (у которого все модули на classpath) и восстановить критичное правило:

```java
// island-app/src/test/java/com/island/ArchitectureTest.java
@Test
void engineShouldNotDependOnDomain() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.island");
    noClasses().that().resideInAPackage("com.island.engine..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("com.island.nature..", "com.island.simcity..")
        .check(classes);
}

@Test
void utilShouldNotDependOnDomain() { ... }

@Test
void natureAndSimCityShouldNotDependOnEachOther() { ... }
```

---

### 🟡 Стоит улучшить

---

**3. `GameLoopOptimizationTest` живёт не в том модуле**

```
island-nature/src/test/java/com/island/engine/GameLoopOptimizationTest.java
```

Тест тестирует только engine-классы (`GameLoop`, `ParallelDispatcher`, `PhaseScheduler`) и не использует ни одного природного класса. Он попал в `island-nature` скорее всего потому, что там есть Mockito-зависимость, которой нет в `island-engine`. Правильное решение — добавить Mockito в `island-engine/pom.xml` (test-scope) и переместить тест туда.

---

**4. `PhaseScheduler` — кэш инвалидируется через `List.equals()` без контракта на `ScheduledTask`**

```java
boolean tasksChanged = lastProcessedTasks == null || !lastProcessedTasks.equals(tasks);
```

`List.equals()` сравнивает элементы через `element.equals()`. `ScheduledTask` — интерфейс без объявленного `equals`. Анонимные реализации из `GameLoop.addRecurringTask(Tickable)` и лямбды используют `Object.equals()` (identity). Это работает, потому что `lastProcessedTasks` хранит те же объекты-ссылки что и `recurringTasks`. Но это неявный контракт: если кто-то создаёт `ScheduledTask`-реализацию с `equals()`-переопределением (например, в тестах), поведение кэша изменится непредсказуемо.

Надёжнее — проверять структурное изменение через size + identity-сравнение:

```java
boolean tasksChanged = lastProcessedTasks == null 
    || lastProcessedTasks.size() != tasks.size()
    || !java.util.Collections.unmodifiableList(lastProcessedTasks).equals(tasks);
```

Или ещё проще: хранить `int lastTaskListVersion` и инкрементировать при добавлении задачи.

---

**5. `DefaultWorkUnit` вручную делегирует все 14 методов `Collection`**

```java
public class DefaultWorkUnit<T extends Mortal> implements WorkUnit<T> {
    // 14 методов: size(), isEmpty(), contains(), iterator(), toArray()...
    // каждый просто вызывает nodes.size(), nodes.isEmpty() и т.д.
```

Это 60 строк шаблонного кода. `AbstractCollection` решает это в 2 строки:

```java
public class DefaultWorkUnit<T extends Mortal> extends AbstractList<SimulationNode<T>>
        implements WorkUnit<T> {
    private final List<SimulationNode<T>> nodes = new ArrayList<>();

    @Override public SimulationNode<T> get(int index) { return nodes.get(index); }
    @Override public int size() { return nodes.size(); }
    // + WorkUnit-специфичные методы
}
```

---

**6. `ComponentRegistry.getBitSet()` — вызывается в `SimCityPlugin`, но отсутствует в исходниках**

```java
// SimCityPlugin.java
componentRegistry.getBitSet(List.of(
    PopulationComponent.class, BuildingComponent.class, EconomyComponent.class
));
```

`ComponentRegistry` в `island-engine` содержит только `getOrRegister()` и `size()`. Метода `getBitSet()` нет. Либо это несинхронизированный файл в репозитории, либо `SimCityPlugin` не скомпилируется. Нужна проверка и синхронизация.

---

### 🟢 Мелочи

- Python-скрипты в корне (`fix_fqns.py`, `fix_imports.py`, `fix_imports_v2.py`, `fix_imports_v3.py`, `fix_tests.py`, `fix_tests_final.py`, `cleanup_imports.py` и др.) продолжают накапливаться — их 9 штук. Если это dev-утилиты, им место в `scripts/` с `.gitignore` или отдельной веткой
- `GameLoop.stop()` — классический TOCTOU на `running`: два потока могут пройти `if (!running) { return; }` одновременно и оба вызвать `taskExecutor.shutdownNow()`. Вероятность низкая при текущей архитектуре, но решается одной строчкой: `private volatile boolean stopping`
- `NatureLauncher` в `island-app` логирует `"Stop condition: extinction of any species"`, но с v1.22 семантика изменилась на «все животные == 0». Лог устарел

---

### Итог

Модульная структура выстроена правильно: `engine → nature/simcity → app`, зависимости однонаправленные, POM-ы корректные. Три вещи требуют внимания до следующего релиза: удалить `src/` призрак, перенести и расширить `ArchitectureTest` в `island-app`, разобраться с `getBitSet()` в `SimCityPlugin`. Остальное — технический долг среднего приоритета.