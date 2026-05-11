Теперь у меня полный срез. Пишу финальное ревью.

---

## Code Review #6: Island dev — Spring Boot Readiness

### Что закрыто из Review #5 — все шесть пунктов

| Замечание | Статус |
|---|---|
| `volatile long[]` — не гарантирует thread-safety элементов | ✅ `AtomicLongArray` / `AtomicIntegerArray` во всех трёх SoA-хранилищах |
| `EcosystemBalanceTest` — нет фиксированного seed | ✅ `config.setRandomSeed(42L)` |
| `maven-javadoc-plugin` с `<doclint>none</doclint>` | ✅ Исправлено на `reference,syntax` |
| `scripts/` не в `.gitignore` | ✅ Добавлено `scripts/` в `.gitignore` |
| Magic numbers в `AnimalHealthSystem` | ✅ Заменены константами через `Configuration` |
| PITest и JMH не привязаны к CI | ✅ CI расширен: отдельный job Checkstyle, upload артефактов при сбое |

Дополнительно: `SimulationContext.close()` с `awaitTermination` + `shutdownNow`-fallback, `GameLoop` переведён на `taskExecutor.submit()` вместо сырых Thread, 100% Javadoc на `@EngineAPI`-классах, четыре ADR, `ONBOARDING.md`, `CONTRIBUTING.md`, `DOCUMENTATION_STANDARDS.md`, `GLOSSARY.md`, REST и WebSocket API спроектированы в docs, `NaturePlugin` получил no-arg конструктор для ServiceLoader.

---

### 🔴 Критично — блокирует Spring Boot переход

---

**1. `GameLoop` не поддерживает PAUSE/RESUME — REST API недоступен**

ADR 004 и `REST_API.md` декларируют:
```
POST /api/v1/simulation/pause
POST /api/v1/simulation/resume
GET  /api/v1/simulation/status → RUNNING | PAUSED | IDLE
```

В `GameLoop` есть только `start()` и `stop()`. `stop()` — деструктивная операция: отменяет `Future`, теряет `tickCount`, требует пересоздания контекста. Если Spring-контроллер вызовет `gameLoop.stop()` как "pause" — перезапустить симуляцию без пересоздания объектов будет нельзя.

Решение — добавить в `GameLoop` до начала Spring-интеграции:

```java
// GameLoop.java
private final AtomicBoolean paused = new AtomicBoolean(false);

public void pause() {
    paused.set(true);
    log.info("GameLoop paused.");
}

public void resume() {
    paused.compareAndSet(true, false);
    log.info("GameLoop resumed.");
}

public SimulationStatus getStatus() {
    if (!running.get()) return SimulationStatus.IDLE;
    return paused.get() ? SimulationStatus.PAUSED : SimulationStatus.RUNNING;
}

// В run():
if (paused.get()) {
    TimeUnit.MILLISECONDS.sleep(tickDurationMs);
    continue;
}
```

Без этого `POST /pause` и `GET /status` физически не реализуемы.

---

**2. `IslandSnapshot` — живая ссылка на мутирующий объект, небезопасно для WebSocket**

```java
public class IslandSnapshot implements WorldSnapshot {
    private final Island island;  // ← живой объект, тикает в другом потоке

    @Override
    public Map<String, Number> getMetrics() {
        // ↓ читает island в момент вызова — возможна гонка данных
        metrics.put("globalSatiety", island.getStatisticsService().calculateGlobalSatiety(island));
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return new CellSnapshot(island.getCell(x, y)); // ← Cell может изменяться
    }
}
```

WebSocket-сериализатор в Spring будет вызывать `getMetrics()` и `getNodeSnapshot()` из потока Jackson в то время, когда `GameLoop` модифицирует `island` из своего потока. Это data race на неатомарных структурах `Island`.

Правильная семантика `Snapshot` — копия данных в момент создания:

```java
public class IslandSnapshot implements WorldSnapshot {
    private final int tickCount;
    private final int width, height, totalCount;
    private final Map<String, Number> metrics;  // ← скопировано в конструкторе
    private final CellSnapshot[][] nodes;       // ← immutable grid copy

    public IslandSnapshot(Island island) {
        // Все данные копируются здесь, под защитой Island-лока
        this.tickCount = island.getTickCount();
        this.metrics = buildMetrics(island);    // O(n) — один раз
        this.nodes = buildNodes(island);
    }
}
```

---

**3. `WorldSnapshot` интерфейс не сериализуется Jackson без полиморфной конфигурации**

```java
// island-engine/core — нет Jackson-зависимости, нет аннотаций
public interface WorldSnapshot { ... }
```

Когда Spring попытается сериализовать `WorldSnapshot` через `@RestController`:
```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
  No serializer found for interface WorldSnapshot
```

Так как `island-engine` не должен зависеть от Jackson, конфигурация должна жить в `island-app`:

```java
// island-app: SimulationJacksonConfig.java
@Configuration
public class SimulationJacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer snapshotPolymorphism() {
        return builder -> builder.mixIn(WorldSnapshot.class, WorldSnapshotMixin.class);
    }
}

// WorldSnapshotMixin.java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "simulationType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IslandSnapshot.class, name = "nature"),
    @JsonSubTypes.Type(value = CitySnapshot.class, name = "simcity")
})
interface WorldSnapshotMixin {}
```

Без этого `GET /api/v1/simulation/snapshot` вернёт ошибку 500.

---

### 🟡 Стоит улучшить

---

**4. `JMH SoABenchmark` в `src/test/java` — не запустится как бенчмарк**

```
island-engine/src/test/java/com/island/engine/bench/SoABenchmark.java
```

JMH-бенчмарки в test scope компилируются вместе с тестами, но `mvn verify` их не запускает — JMH нужен отдельный uber-jar через `exec:java` или `jmh:benchmark`. В текущей конфигурации benchmark — это мёртвый код с точки зрения CI.

Стандартная структура — отдельный модуль:
```
island-benchmarks/
  src/main/java/com/island/bench/SoABenchmark.java
  pom.xml  ← depends on island-engine, jmh-core, jmh-generator-annprocess
```

Либо, если хочется остаться в одном модуле, добавить в CI отдельный job (с `if: github.event_name == 'schedule'`).

---

**5. `HealthSoAStore.ensureCapacity` — `synchronized` несовместимо с `volatile AtomicLongArray`**

```java
private volatile AtomicLongArray currentEnergy; // ← volatile нужен для swap

private synchronized void ensureCapacity(int entityId) { // ← только один поток расширяет
    // ...создаём новый AtomicLongArray, копируем, присваиваем currentEnergy
}
```

Это работает, но логика неконсистентна между тремя хранилищами: `HealthSoAStore` использует `synchronized`, `MovementSoAStore` — `StampedLock` с оптимистичными чтениями, `AgeSoAStore` — `synchronized`. Три разных подхода к одной и той же задаче создают когнитивную нагрузку при поддержке.

Рекомендация — унифицировать на `StampedLock` (как в `MovementSoAStore`, там наиболее грамотная реализация) или добавить комментарий в каждый store с объяснением выбора.

---

### 🟢 Мелочи

- `IslandSnapshot.getMetrics()` итерирует весь остров (O(W×H) через `calculateGlobalSatiety`) при каждом вызове — при WebSocket трансляции каждые 100ms на острове 100×100 это 10K операций на тик. Стоит кэшировать результат в snapshot-конструкторе
- `SimulationConstants` помечен `@Deprecated` — хорошо. Стоит добавить Javadoc с миграционным путём для тех, кто использует статические поля напрямую

---

## Готов ли проект к Spring Boot + React?

Честный ответ: **архитектурно — да, на уровне кода — требует трёх правок**.

Что уже готово без изменений: `SimulationEngine.build()` → `@Bean`, `SimulationContext` как `AutoCloseable` → `@PreDestroy`, EventBus → WebSocket publisher feed, `WorldSnapshot` иерархия для REST-ответов, ServiceLoader-обнаружение плагинов, headless-режим для сервера.

Что нужно закрыть перед стартом Spring Boot модуля:

| # | Задача | Сложность |
|---|---|---|
| 1 | Добавить `pause()`/`resume()`/`getStatus()` в `GameLoop` | Малая |
| 2 | Переделать `IslandSnapshot` в истинный immutable snapshot | Средняя |
| 3 | Добавить Jackson Mixin-конфигурацию в `island-app` | Малая |

После этих трёх изменений можно добавлять `island-api` модуль:
```
island-api/
  pom.xml         ← spring-boot-starter-web, spring-boot-starter-websocket
  SimulationController.java
  WebSocketConfig.java
  SimulationService.java  ← оборачивает SimulationEngine + GameLoop
```

---

## Сравнение с профессиональным стандартом — итоговая карта

За шесть итераций проект прошёл путь от монолитного `GameLoop` с God Class до многомодульной системы с JPMS, ECS, SoA, ArchUnit, ADR, JMH, CI и спроектированным REST API. Ниже — честная карта позиционирования.

### Что на уровне зрелых open-source проектов

Многомодульная JPMS-архитектура с `@EngineAPI`/`@InternalEngine`, ArchUnit-тесты в `island-app` со всеми четырьмя правилами, `AtomicLongArray`-хранилища, `SystemExecutionGraph` для параллельного ECS, ServiceLoader-поддержка — это уровень Artemis-ODB, Minestom, Vert.x. ADR-документация и `CONTRIBUTING.md` — стандарт Apache, Spring, Kafka.

### Три оставшихся gap до полного professional-уровня

**Gap 1 — Нет Revapi.** API совместимость между версиями автоматически проверяется в Spring Framework, Guava, Apache Commons. Без Revapi случайное удаление `@EngineAPI`-метода ломает downstream-пользователей без предупреждения. Добавляется одним плагином в `island-engine/pom.xml`.

**Gap 2 — Нет property-based тестов.** Для симулятора с вероятностной логикой jqwik / junit-quickcheck — индустриальный стандарт. Примеры свойств: «при любой валидной `Configuration` энергия после тика не может стать отрицательной», «при любом острове W×H сумма entities во всех клетках равна `totalOrganismCount()`». Эти инварианты невозможно покрыть unit-тестами полностью.

**Gap 3 — PITest не запускается в CI.** Mutation testing добавлен в `pom.xml`, но не привязан к pipeline. Mutation score — это единственная метрика, которая показывает, насколько тесты реально ловят регрессии, а не просто покрывают строки. Добавить scheduled job в CI (раз в день/на PR в main).

### Конкретный план для полного профессионального уровня

| Приоритет | Задача |
|---|---|
| Перед Spring Boot | `pause()`/`resume()` в `GameLoop`, immutable snapshots, Jackson Mixin |
| Sprint 1 | `island-api` модуль (Spring Boot + WebSocket) |
| Sprint 2 | Revapi в `island-engine/pom.xml` |
| Sprint 3 | jqwik property-based тесты для `AnimalHealthSystem`, `InteractionMatrix` |
| Sprint 4 | PITest scheduled job в CI, `island-benchmarks` отдельный модуль |