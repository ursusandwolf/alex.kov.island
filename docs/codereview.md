Теперь у меня есть полная картина. Читаю ключевые правки системно, затем пишу ревью.У меня полная картина. Пишу ревью.

---

## Code Review #2: Island dev — сравнение с PR21

### Общая оценка: проект развивается правильно

Из 8 критичных находок предыдущего ревью закрыты все 8. `GameLoop` разделён на три ответственности, `onSimulationStopped` вызывается, `shouldStop` в плагине, `SimulationWorld<T, C>` упрощён, дублирование `WorldListener` + `EventBus` устранено, ECS вырос из маркер-интерфейса в полноценную систему с `ArrayComponentStore`, `ComponentRegistry`, `EntitySystem` и `SystemExecutionGraph`. `SimulationContext` стал `record`. Архитектурный вектор — правильный.

Есть одна регрессия по производительности (намеренно оптимизировалась в v1.14), два новых структурных дефекта, и три более мелких нарушения.

---

### 🔴 Критично

---

**1. GC-регрессия в `PhaseScheduler` — откат работы v1.13–v1.14**

```java
// PhaseScheduler.execute() — вызывается КАЖДЫЙ тик:
Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
for (Phase phase : Phase.values()) {
    phasedTasks.put(phase, new ArrayList<>());  // ← 4 новых ArrayList на каждый тик
}
List<ParallelTask<T>> parallelGroup = new ArrayList<>(); // ← ещё один
```

Changelog v1.13–v1.14 явно фиксировал «eliminated thousands of ArrayList allocations per tick» — ради этого в `GameLoop` были введены `phasedTasks` как поле с `EnumMap` и `clear()` вместо `new`. После декомпозиции `GameLoop → GameLoop + PhaseScheduler` эта оптимизация потерялась: `PhaseScheduler.execute()` — stateless, его состояние не сохраняется между тиками, и всё создаётся заново.

Как исправить — перенести коллекции в поля `PhaseScheduler` и очищать, а не пересоздавать:

```java
public class PhaseScheduler<T extends Mortal> {
    private final ParallelDispatcher<T> dispatcher;
    // Reused across ticks — same optimization as pre-split GameLoop
    private final Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
    private final List<ParallelTask<T>> parallelGroup = new ArrayList<>();

    public PhaseScheduler(ParallelDispatcher<T> dispatcher) {
        this.dispatcher = dispatcher;
        for (Phase phase : Phase.values()) {
            phasedTasks.put(phase, new ArrayList<>());
        }
    }

    public void execute(...) {
        for (List<ScheduledTask> list : phasedTasks.values()) {
            list.clear(); // О(n) по кол-ву задач, не O(n) по GC
        }
        // ...
    }
}
```

---

**2. Два независимых пути остановки — `onSimulationStopped` вызывается не всегда**

В `SimulationEngine.build()` устанавливается:
```java
gameLoop.setStopCondition(() -> plugin.shouldStop(context));
```

Когда `stopCondition` срабатывает внутри `GameLoop.run()`, цикл завершается установкой `running = false`. При этом `engine.stop(context, plugin)` — и следовательно `plugin.onSimulationStopped()` — **не вызывается**.

Параллельно `NatureLauncher.monitor()` тоже проверяет `plugin.shouldStop()` и при истине вызывает `engine.stop(context, plugin)` — корректно. Но если `GameLoop` останавливается первым (до следующего тика монитора через 2 секунды), монитор видит `!gameLoop.isRunning()` → `latch.countDown()` без вызова `engine.stop()`.

Итог: lifecycle `onSimulationStopped` не гарантирован. Это тот самый дефект, который был закрыт как «критично» в PR21 — но проявился в другом месте.

Решение — убрать дублирование. Пусть остановку всегда инициирует одна точка:

```java
// Вариант A: GameLoop вызывает stopHandler при срабатывании условия
gameLoop.setOnStop(() -> engine.stop(context, plugin));

// Вариант B: убрать stopCondition из GameLoop,
// оставить мониторинг только в NatureLauncher через plugin.shouldStop()
```

---

### 🟡 Стоит улучшить

---

**3. `ArchitectureEvolutionTest` нарушает правило, которое сам тест защищает**

```java
// com/island/engine/ArchitectureEvolutionTest.java
import com.island.nature.config.Configuration;
import com.island.nature.entities.components.HealthComponent;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
```

`ArchitectureTest` запрещает `engine` зависеть от `nature`. Тест лежит в пакете `com.island.engine` и импортирует `com.island.nature.*`. ArchUnit не проверяет тесты (`DO_NOT_INCLUDE_TESTS`), поэтому нарушение не ловится автоматически — но по духу это противоречие.

Правильное место для теста, который проверяет совместную работу ECS + Organism + EventBus — `com.island` или `com.island.integration`, не `com.island.engine`.

---

**4. `SimulationWorld.onEntityAdded` / `onEntityRemoved` — нет дефолтной реализации**

```java
// SimulationWorld.java
void onEntityAdded(T entity);    // ← абстрактный
void onEntityRemoved(T entity);  // ← абстрактный
```

Любой тестовый или stub-мир обязан реализовать оба метода, даже если ему это не нужно. В PR21 это было `WorldListener` с опциональными методами — теперь контракт стал жёстче без причины. Достаточно сделать `default`:

```java
default void onEntityAdded(T entity) { }
default void onEntityRemoved(T entity) { }
```

---

**5. FQN в теле `PhaseScheduler` — нарушение code standards проекта**

```java
// PhaseScheduler.java, строки 56 и 72:
List<List<ParallelTask<T>>> batches = 
    com.island.engine.ecs.SystemExecutionGraph.buildSchedule(parallelGroup);
```

Changelog v1.11 явно фиксирует политику: «Refactored test files to remove Fully Qualified Names (FQNs) in code bodies». Это нарушение в production-коде, а не в тестах. Нужен импорт.

---

### 🟢 Мелочи

- `AnimalFeedingSystem.process()` — пустой метод с комментарием «can use it for individual feeding if needed». Это dead code с ложным обещанием. Либо реализовать, либо удалить — пустые методы в абстрактной иерархии создают путаницу для следующего разработчика
- `Island.partitionIntoChunks()` — magic numbers `64`, `16`, `32` в логике разбивки на чанки. Паттерн с `Configuration` уже есть в проекте — они там и должны жить
- `NaturePlugin` конструктор — по-прежнему 14+ строк ручной сборки зависимостей. Это был «архитектурный backlog» из PR21. Не регрессия, но задолженность накапливается
- `shouldStop` изменил семантику: было «любой вид вымер», стало «все животные == 0». Поведение не то что хуже, но это молчаливое изменение без записи в CHANGELOG — если это намеренное решение, стоит зафиксировать

---

### Итог: что делать в первую очередь

| Приоритет | Задача |
|---|---|
| 1 | Перенести `phasedTasks` / `parallelGroup` в поля `PhaseScheduler` — восстановить GC-оптимизацию |
| 2 | Унифицировать путь остановки: один из двух механизмов (`stopCondition` vs монитор) должен быть единственным, и он должен вызывать `onSimulationStopped` |
| 3 | Перенести `ArchitectureEvolutionTest` в интеграционный пакет |
| 4 | Добавить `default` к `onEntityAdded`/`onEntityRemoved` в `SimulationWorld` |
| 5 | Заменить FQN на import в `PhaseScheduler` |