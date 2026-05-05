## Code Review: Island Ecosystem Simulator — PR #21

### Общая оценка

Проект демонстрирует высокий уровень архитектурного мышления: plugin-система, фазовый планировщик, EDA через EventBus, LOD-модель, GC-оптимизации — всё это грамотные решения. Changelog v1.6–v1.14 показывает осознанную эволюцию. Главные риски сейчас — `GameLoop` превращается в God Class, `ECS` завис в половинчатом состоянии, и в нескольких местах абстракция engine пробивается конкретными доменными типами. Если это не закрыть сейчас, следующие 5–10 фич будут всё дороже.

---

### 🔴 Критично — нарушает архитектурные границы

---

**1. `NatureLauncher` прокалывает engine-абстракцию прямым кастом**

```java
// NatureLauncher.java
Island island = (Island) context.getWorld();
Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
```

`SimulationEngine` возвращает `SimulationContext<Organism>` с `SimulationWorld<T, C>`. Launcher сразу кастует к конкретному `Island`. Любое переименование или замена реализации сломает это место без предупреждения компилятора. Логика мониторинга («стоп при вымирании вида») — доменная. Её место в `NaturePlugin`, а не в лаунчере.

Как исправить — добавить hook в `SimulationPlugin`:
```java
// SimulationPlugin.java — добавить метод:
default boolean shouldStop(SimulationContext<T> context) {
    return false;
}

// NaturePlugin.java:
@Override
public boolean shouldStop(SimulationContext<Organism> context) {
    Island island = (Island) context.getWorld(); // каст здесь — в домене, не в engine
    return island.getSpeciesCounts().entrySet().stream()
        .anyMatch(e -> !isPlant(e.getKey()) && e.getValue() == 0);
}
```

Тогда `NatureLauncher` работает только с `SimulationEngine` + `SimulationPlugin` без знания домена.

---

**2. `onSimulationStopped` никогда не вызывается — утечка lifecycle**

```java
// SimulationPlugin.java
default void onSimulationStopped(SimulationContext<T> context) { } // мёртвый код

// SimulationEngine.java — вызов отсутствует полностью
```

Lifecycle `start → stop` задекларирован в интерфейсе, но `SimulationEngine` вызывает только `onSimulationStarted`. Если плагин зарегистрировал ресурсы (потоки, соединения, подписки EventBus) в `onSimulationStarted`, нет места где их корректно освободить. Это потенциальная утечка ресурсов.

Как исправить:
```java
// SimulationEngine.java
public void stop(SimulationContext<T> context, SimulationPlugin<T> plugin) {
    context.getGameLoop().stop();
    plugin.onSimulationStopped(context);
}
```

---

**3. `CellService.tick()` — fallback сломан по контракту**

```java
// CellService.java
@Override
default void tick(int tickCount) {
    // Fallback for non-optimized execution
    beforeTick(tickCount); // ← только beforeTick, processCell никогда не вызывается!
}
```

Если `CellService` когда-либо попадёт в non-optimized путь (например, в тесте, или если ExecutionMode изменится), он выполнит `beforeTick` и молча ничего больше не сделает. Симуляция продолжится без кормления, движения или размножения. Ошибка не бросится — просто неправильные результаты.

Это либо должно бросать `UnsupportedOperationException` («я не умею работать без world»), либо быть реализовано корректно. Текущий вариант — худший из возможных: работает, но неправильно.

---

### 🟡 Стоит улучшить — архитектурный долг

---

**4. `GameLoop` растёт в God Class (270 строк, 4 ответственности)**

Текущий `GameLoop` делает:
- управление временем тика (sleep loop)
- фазовую сортировку и группировку задач
- параллельное исполнение через `CellProcessor`-пул
- управление жизненным циклом потоков

Уже сейчас это ~270 строк и `CellProcessor` как вложенный класс. С каждой новой фичей сложность будет расти нелинейно.

Рекомендуемое разделение:
```
GameLoop          — только тик-цикл + lifecycle (start/stop/runTick)
PhaseScheduler    — сортировка задач по Phase + priority
ParallelDispatcher — управление CellProcessor-пулом + CountDownLatch
```

Это не срочно сейчас, но хороший момент сделать `PhaseScheduler` — он уже логически изолирован в `runTick()`.

---

**5. `CellProcessor` — потенциальный data race на mutable-полях**

```java
// CellProcessor — поля без volatile:
private Collection<? extends SimulationNode<T>> unit;
private List<CellService<T, SimulationNode<T>>> services;
private int tickCount;

void update(...) { this.unit = unit; this.services = services; ... } // главный поток
public void run() { for (node : unit) { ... } }                      // рабочий поток
```

Только `error` помечен `volatile`. Поля `unit`, `services`, `tickCount` обновляются главным потоком, читаются рабочими. `CountDownLatch.await()` гарантирует порядок завершения, но не publication до старта следующего тика. Формально это data race.

Решение — либо объявить поля `volatile`, либо использовать `final` fields + создавать новый `CellProcessor` (но тогда теряется смысл пула), либо добавить `happens-before` через `volatile boolean ready`.

---

**6. ECS застрял между двумя парадигмами**

```java
// engine/ecs/Component.java — просто пустой маркер
public interface Component { }

// Changelog: "Organism: replaced ConcurrentHashMap lookups with direct field references"
```

Два тика происходит одновременно: в changelog написано "ECS Transition" и добавлены `AgeComponent`, `HealthComponent`, а следующая версия оптимизирует это обратно к прямым полям. ECS как концепция требует решения на уровне engine: `Entity` = ID, компоненты в Arrays of Structs, Systems без объектов-сущностей. Текущий `Organism` с прямыми полями — это OOP. Держать оба подхода одновременно — Divergent Change в чистом виде.

Рекомендация: принять архитектурное решение явно. Если производительность важнее расширяемости — оставить OOP `Organism` и убрать `Component`. Если нужна модульность — идти в настоящий ECS (entity = long ID, компоненты в typed arrays). Половина пути дороже любого из вариантов.

---

**7. `SimulationWorld<T, C>` — параметр C утекает в engine**

```java
public interface SimulationWorld<T extends Mortal, C> extends Tickable {
    C getConfiguration();
    ...
}
```

Engine не использует `C` ни в одном месте — ни `GameLoop`, ни `SimulationEngine`, ни `SimulationContext`. Зато вынужденный wildcard `SimulationWorld<T, ?>` разбросан везде. Это domain-specific деталь (`Configuration`) в generic-сигнатуре core engine. Метод `getConfiguration()` можно перенести в `NatureWorld` (доменный интерфейс), убрав `C` из engine-контракта.

---

**8. Два параллельных механизма событий — `WorldListener` vs `EventBus`**

```java
// SimulationWorld.java
void addListener(WorldListener<T> listener);
List<WorldListener<T>> getListeners();
com.island.engine.event.EventBus getEventBus();
```

У world есть и `WorldListener` (Observer через список), и `EventBus`. По changelog v1.12 `Island.onEntityRemoved` публикует `EntityDiedEvent` через `WorldListener`, а EventBus используется для `StatisticsService` и `AlertService`. Два разных канала для одних и тех же событий — Shotgun Surgery при добавлении нового типа события (нужно добавлять и в listener, и в bus).

Решение: `WorldListener` — внутренний механизм (protected/package-private), внешние подписчики работают только через `EventBus`. Тогда `World` — producer, все остальные — consumers через шину.

---

### 🟢 Мелочи

- `fix_fqns.py`, `fix_imports.py`, `fix_imports_v2.py` в корне репозитория — это dev-утилиты, им место в `.gitignore` или `scripts/` с явным README
- `NatureLauncher.monitor()` — magic numbers `5 * 60 * 1000` и `2` секунды: вынести в `Configuration` (там уже есть паттерн для этого)
- `GameLoop.loopThread` читается из `stop()` без `volatile` — не критично при текущем использовании, но стоит пометить
- `DefaultEventBus.getTypeHierarchy()` рекурсивный — для глубоких иерархий может быть проблемой, итеративный вариант через stack надёжнее
- Логи на русском в `NatureLauncher` (`"Запуск симуляции острова..."`) при английском коде в остальных местах — нестабильная конвенция
- `addRecurringTask(Tickable)` оборачивает в анонимный `ScheduledTask` с захардкоженными Phase.SIMULATION и priority=50 — молчаливые дефолты, лучше требовать `ScheduledTask` напрямую

---

### Итоговые рекомендации по приоритетам

**Сделать сейчас (до следующей крупной фичи):**
1. Вызвать `onSimulationStopped` в `SimulationEngine` — это дыра в lifecycle, которую легко закрыть
2. Починить `CellService.tick()` fallback — бросить `UnsupportedOperationException` или убрать дефолт
3. Перенести логику мониторинга вымирания из `NatureLauncher` в `NaturePlugin.shouldStop()`

**Запланировать на рефакторинг:**
4. Убрать `C` из `SimulationWorld<T, C>` — простое изменение с большим эффектом
5. Унифицировать event-механизм: `WorldListener` → только internal, наружу только `EventBus`
6. Принять решение по ECS и зафиксировать его в `DOCUMENTATION.md`

**Архитектурный backlog:**
7. Разделить `GameLoop` → `GameLoop` + `PhaseScheduler` + `ParallelDispatcher`
8. `data race` в `CellProcessor` — добавить `volatile` на shared поля