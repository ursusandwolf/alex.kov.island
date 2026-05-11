# Code Review v10: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-09  
**База:** v9  
**Дельта:** +11 Java файлов, 50 изменённых, +5 плагинов в pom.xml  
**Ключевые изменения:** `AtomicLongArray`/`StampedLock` в SoA, `AnimalHealthSystem` → горячий
путь через storage, 7 engine-тестов, JaCoCo+PITest+revapi+Enforcer+Checkstyle+JMH в pom,
`ZoningService`, `DesirabilityService`

---

## Прогресс относительно v9

| Задача из v9 | Статус |
|---|---|
| `HealthSoAStore` — volatile + атомарные операции | ✅ `AtomicLongArray` + `synchronized ensureCapacity` |
| `AgeSoAStore` — thread-safety | ✅ `AtomicIntegerArray` |
| SoA в горячем пути `AnimalHealthSystem` | ✅ Прямой доступ через `HealthStorage` |
| `MovementSoAStore` — новый store | ✅ `StampedLock` с optimistic read |
| `maven-enforcer-plugin` | ✅ Java 21+, Maven 3.8+, convergence |
| `jacoco-maven-plugin` с threshold | ✅ 58% (см. замечание) |
| `maven-source-plugin` | ✅ sources JAR |
| `maven-javadoc-plugin` | ✅ javadoc JAR |
| `maven-checkstyle-plugin` | ✅ подключён |
| JMH зависимость | ✅ в engine pom, **но `@Benchmark` классов нет** |
| PITest | ✅ threshold 65% для `engine.*` |
| `revapi-maven-plugin` | ✅ API binary compatibility check |
| Engine unit-тесты | ✅ 7 новых файлов |
| `ZoningService` / `DesirabilityService` | ✅ симуляция города растёт |
| GitHub Actions CI | ❌ по-прежнему отсутствует |
| Javadoc на `@EngineAPI` методах | ⚠️ начат, не завершён |
| `ServiceLoader` / JPMS `provides/uses` | ❌ не реализован |

---

## ✅ Детальный разбор значимых улучшений

### 1. HealthSoAStore — правильная многоуровневая thread-safety

```java
// Было (v9): обычные массивы — гонка при concurrent read+ensureCapacity
private long[] currentEnergy;

// Стало (v10): три уровня защиты
private volatile AtomicLongArray currentEnergy;  // volatile = видимость ссылки
// AtomicLongArray = атомарные операции над элементами
// synchronized ensureCapacity = безопасное создание нового массива

@Override
public long addEnergy(int entityId, long delta) {
    return currentEnergy.addAndGet(entityId, delta); // atomic, нет race для CAS
}

private synchronized void ensureCapacity(int entityId) {
    if (entityId >= capacity) {
        AtomicLongArray newArr = new AtomicLongArray(newCapacity);
        // copy ...
        this.currentEnergy = newArr;  // volatile write: все потоки увидят новый массив
    }
}
```

Три гарантии, каждая своя:
- `volatile AtomicLongArray` — видимость ссылки при замене массива
- `AtomicLongArray` — lock-free атомарные операции на элементах
- `synchronized ensureCapacity` — сериализация роста массива

Это корректный паттерн. ✅

---

### 2. MovementSoAStore — StampedLock с оптимистичным чтением

```java
@Override
public int getSpeed(int entityId) {
    long stamp = lock.tryOptimisticRead();          // не блокирует
    int cap = capacity;
    AtomicIntegerArray arr = speeds;
    int speed = (entityId < cap) ? arr.get(entityId) : 0;

    if (!lock.validate(stamp)) {                    // если была запись — перечитать
        stamp = lock.readLock();
        try { speed = ...; } finally { lock.unlockRead(stamp); }
    }
    return speed;
}
```

`StampedLock` с optimistic read — правильный выбор для read-heavy сценария (скорость
читается каждый тик, изменяется редко). Это то же решение, что использует
`java.util.concurrent.ConcurrentHashMap` внутри для счётчиков. Знание тонкостей JUC — хорошо.

Но: `StampedLock` **не переиспользуем** и **не поддерживает реентрантность**. Если в
будущем `ensureCapacity` будет вызван из кода, уже держащего write lock — deadlock.
Документировать инвариант в Javadoc.

---

### 3. AnimalHealthSystem — SoA в горячем пути

```java
// v9: hot path шёл через объектный граф
entity.tryConsumeEnergy(metabolism) → Organism.healthStorage.setCurrentEnergy(id, ...)

// v10: система читает данные напрямую из storage
private final HealthStorage healthStorage;

public AnimalHealthSystem(NatureWorld world, ...) {
    this.healthStorage = world.getHealthStorage(); // получает storage при инициализации
}

protected void process(Organism entity, Cell cell, int tickCount) {
    int id = entity.getEntityId();
    if (id == -1 || !healthStorage.isAlive(id)) return; // прямо из AtomicIntegerArray
    long currentEnergy = healthStorage.getCurrentEnergy(id); // прямо из AtomicLongArray
    healthStorage.setCurrentEnergy(id, nextEnergy);          // прямо в AtomicLongArray
}
```

Полный SoA-паттерн: система не идёт через `Organism` → поиск в Map → компонент → данные.
Это именно то, что было рекомендовано в v9, и что даёт реальное ускорение на горячем пути.

---

### 4. revapi-maven-plugin — бинарная совместимость API

```xml
<plugin>
    <groupId>org.revapi</groupId>
    <artifactId>revapi-maven-plugin</artifactId>
    <version>0.15.0</version>
    ...
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

Это профессиональный инструмент, который используют JUnit, Spring, Hibernate. `revapi`
сравнивает текущий JAR с предыдущим тегом и выдаёт ошибку при нарушении бинарной
совместимости: удалении метода, изменении сигнатуры, уменьшении видимости.

Без первого релиза `revapi` пока ни с чем не сравнивает — нужна базовая версия
(`revapi.oldVersion` или `oldArtifacts`). После первого `mvn deploy` — заработает. ✅

---

### 5. Новые engine-тесты — закрывают gap

7 тест-файлов покрывают: `EntityIdManager`, `EventBus`, `ComponentRegistry`,
`ComponentStore`, `EntityQuery`, `SimulationConfig`, `GridUtils`, `RandomUtils`, `ViewUtils`,
`PhaseScheduler`, `SimulationEngine`, `SimulationWorld`, `HealthSoAStore`, `AgeSoAStore`.

`SoAStoreTest.store_thread_safety()` — конкурентный тест с 4 потоками × 1000 итераций,
проверяет что `addEnergy` атомарен. Корректный тест для многопоточного кода. ✅

---

### 6. ZoningService — именованные константы, правильная фаза

```java
private static final int UPGRADE_TICK_INTERVAL   = 5;
private static final int MIN_DESIRABILITY_MEDIUM = 60;
private static final int MIN_DESIRABILITY_HIGH   = 85;
private static final int MIN_HAPPINESS_MIDDLE    = 70;
private static final int MAX_POLLUTION_WEALTHY   = 5;
```

Все пороговые значения — именованные константы. `DesirabilityService` работает в
`Phase.PREPARE` с `priority=30` (после Connectivity и Pollution, до Zoning) — правильный
порядок: сначала посчитать привлекательность, потом принять решения об апгрейде. ✅

---

## 🔴 Технические замечания

### 1. [MEDIUM] JaCoCo threshold 58% — слишком низкий для library

```xml
<minimum>0.58</minimum>  <!-- 58% line coverage — недостаточно -->
```

58% означает, что 42% строк не покрыты тестами. Для публичной библиотеки стандарт:

| Модуль | Рекомендуемый минимум |
|---|---|
| `island-engine` (публичный API) | **75%** |
| `island-nature` (плагин) | **65%** |
| `island-simcity` (плагин) | **60%** |

Повысить постепенно: сначала до 65%, затем — по мере роста тестов — до 75%.

```xml
<!-- Разные threshold для разных модулей через profile или module-specific config -->
<minimum>0.75</minimum>  <!-- для island-engine -->
```

---

### 2. [MEDIUM] JMH зависимость без benchmark файлов

```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <scope>test</scope>
</dependency>
```

JMH добавлен в pom, но ни одного `@Benchmark` класса нет. Инфраструктура без
использования. Нужен хотя бы один benchmark для `HealthSoAStore` vs `HashMap`:

```java
// island-engine/src/test/java/com/island/engine/bench/SoABenchmark.java
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class SoABenchmark {

    private HealthStorage soaStore;
    private Map<Integer, Long> mapStore;

    @Setup
    public void setup() {
        soaStore = HealthStorage.create(10_000);
        mapStore = new HashMap<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            soaStore.set(i, 1000L, 1000L, true);
            mapStore.put(i, 1000L);
        }
    }

    @Benchmark
    public void soaSequentialRead(Blackhole bh) {
        for (int i = 0; i < 10_000; i++) {
            bh.consume(soaStore.getCurrentEnergy(i));
        }
    }

    @Benchmark
    public void mapSequentialRead(Blackhole bh) {
        for (int i = 0; i < 10_000; i++) {
            bh.consume(mapStore.get(i));
        }
    }
}
```

---

### 3. [LOW] `SchedulingTest` — тест engine.internal в engine-модуле (допустимо, но потенциальная ловушка)

```java
// SchedulingTest.java — в island-engine/test
import com.island.engine.internal.ParallelDispatcher;
import com.island.engine.internal.PhaseScheduler;
```

Технически допустимо: тесты движка имеют право тестировать внутренние классы.
JPMS не ограничивает доступ внутри модуля. Но если тест `PhaseScheduler` через его
конструктор — это **white-box тест**, который сломается при рефакторинге внутренностей.

Лучший подход: тест scheduling через `SimulationEngine.build()` как чёрный ящик.
White-box тесты — только для edge cases, недостижимых через публичный API.

---

### 4. [LOW] `SimulationWorldTest` — null Javadoc в тестовой реализации

```java
@Override public WorldSnapshot createSnapshot() { return null; }
@Override public EventBus getEventBus() { return null; }
```

`world.initialize()` вызывается, затем `assertNotNull(world)` — этот тест проверяет
что объект не null после создания. Это почти тривиально. Тест не проверяет реального
поведения `initialize()`. Усилить:

```java
@Test
void world_should_register_event_bus_on_init() {
    EventBus bus = new DefaultEventBus();
    world.onEventBusSet(bus); // если есть такой lifecycle
    world.initialize();
    assertNotNull(world.getEventBus());
}
```

---

## 📐 Оставшийся путь к профессиональному стандарту

### 1. GitHub Actions CI — критически важно [HIGH]

Единственный крупный пункт, которого не хватает. Все инструменты (JaCoCo, PITest,
revapi, Checkstyle, Enforcer) настроены, но не запускаются автоматически на PR.

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [dev, main]
  pull_request:
    branches: [dev, main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }  # нужен для revapi (сравнение с предыдущим тегом)

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build, Test, Coverage
        run: mvn --batch-mode --no-transfer-progress verify

      - name: Upload JaCoCo reports
        uses: codecov/codecov-action@v4
        with:
          files: '**/target/site/jacoco/jacoco.xml'
          fail_ci_if_error: false

      - name: Archive test results
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/target/surefire-reports/'

  checkstyle:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: 'maven' }
      - name: Checkstyle
        run: mvn --batch-mode checkstyle:check
```

Файл создать в `.github/workflows/ci.yml` — это 30 минут работы.

---

### 2. JMH benchmark — написать хотя бы один файл [MEDIUM]

```java
// Даёт реальные данные о преимуществах SoA
// Показывает что инвестиция в AtomicLongArray оправдана
```

---

### 3. ServiceLoader — discoverable plugins [MEDIUM]

```java
// island-nature/src/main/java/module-info.java — добавить:
provides com.island.engine.core.SimulationPlugin
    with com.island.nature.NaturePlugin;

// island-engine/src/main/java/module-info.java — добавить:
uses com.island.engine.core.SimulationPlugin;

// island-app/Main.java — использовать:
ServiceLoader.load(SimulationPlugin.class)
    .findFirst()
    .ifPresent(plugin -> new SimulationEngine<>().start(plugin, config));
```

---

### 4. Javadoc — публичный контракт [MEDIUM]

Для публикации на Maven Central нужен Javadoc на каждый `@EngineAPI`-метод.
Приоритет: `SimulationEngine`, `SimulationPlugin`, `EventBus`, `ComponentStore`, `EntitySystem`.

```java
/**
 * Builds and starts a simulation from the given plugin and configuration.
 *
 * <p>The returned {@link SimulationContext} implements {@link AutoCloseable}
 * and should be used in a try-with-resources block to ensure proper cleanup.
 *
 * <pre>{@code
 * SimulationConfig cfg = SimulationConfig.defaultFor(4);
 * try (SimulationContext<Organism> ctx = engine.build(plugin, cfg)) {
 *     ctx.gameLoop().runTick();
 * }
 * }</pre>
 *
 * @param plugin the plugin providing world factory and task registration
 * @param config execution parameters (tick rate, threading)
 * @return a started simulation context; caller is responsible for closing
 * @since 1.0
 */
@EngineAPI
public SimulationContext<T> build(SimulationPlugin<T> plugin, SimulationConfig config) { ... }
```

---

### 5. CHANGELOG — формат Keep a Changelog [LOW]

```markdown
# Changelog

## [Unreleased]
### Added
- `MovementSoAStore` with `StampedLock` for optimistic concurrency
- 7 engine unit test suites covering core, ECS, scheduling, SoA
- `ZoningService`, `DesirabilityService` for SimCity plugin
- JaCoCo, PITest, revapi, Enforcer, Checkstyle, JMH in build
### Fixed
- `HealthSoAStore` race condition — `AtomicLongArray` + `synchronized ensureCapacity`
- `AnimalHealthSystem` now accesses SoA storage directly in hot path

## [1.0.0-SNAPSHOT] — 2026-05-01
### Added
- Multi-module Maven structure: `island-engine`, `island-nature`, `island-simcity`, `island-app`
- JPMS `module-info.java` for all modules
...
```

---

## 📊 Итоговая оценка v10

### Динамика v1 → v10

| Критерий | v1 | v5 | v7 | v9 | v10 |
|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 8.5 | 9.0 | 9.5 | **9.5** |
| **Код** | 7.0 | 8.0 | 8.5 | 9.0 | **9.5** |
| **Переиспользуемость** | 6.0 | 8.5 | 9.0 | 9.5 | **9.5** |
| **Тестируемость** | 5.0 | 8.0 | 9.0 | 9.5 | **9.5** |
| **Готовность к JAR** | — | 6.5 | 8.5 | 9.5 | **9.5** |
| **Production-ready** | — | — | — | 7.0 | **8.5** |
| **Общая** | 6.5 | 8.5 | 9.0 | 9.5 | **9.5** |

---

### Чек-лист: что уже есть на профессиональном уровне

| Пункт | Статус |
|---|---|
| Maven multi-module | ✅ |
| JPMS `module-info.java` для всех модулей | ✅ |
| `engine.internal` не экспортируется | ✅ |
| ArchUnit защищает архитектурные границы | ✅ |
| Thread-safe SoA с atomic arrays | ✅ |
| SoA в реальном горячем пути | ✅ |
| `maven-enforcer-plugin` | ✅ |
| `jacoco-maven-plugin` | ✅ |
| `maven-source-plugin` (sources JAR) | ✅ |
| `maven-javadoc-plugin` (javadoc JAR) | ✅ |
| `maven-checkstyle-plugin` | ✅ |
| PITest (mutation testing) | ✅ |
| `revapi` (binary compat) | ✅ |
| JMH (зависимость) | ✅ |
| `SimulationContext` AutoCloseable | ✅ |
| `SimulationConfig` | ✅ |
| Boundary value tests для SimCity | ✅ |
| `@EngineAPI` / `@InternalEngine` аннотации | ✅ |

| Пункт | Статус |
|---|---|
| GitHub Actions CI | ❌ |
| JMH benchmark файлы | ❌ |
| ServiceLoader `provides`/`uses` | ❌ |
| Javadoc полный на @EngineAPI | ⚠️ |
| JaCoCo threshold 75%+ для engine | ⚠️ |
| CHANGELOG в Keep a Changelog формате | ⚠️ |

---

## Вердикт

**9.5/10 по архитектуре. 8.5/10 по production-ready. До публикации на Maven Central — 3-5 дней работы.**

Проект прошёл трансформацию, которую мало кто завершает:

```
v1:  Монолит, engine импортирует nature, нет тестов ядра
v10: Многомодульный, JPMS, engine.internal, SoA, atomic arrays,
     ArchUnit, JaCoCo, PITest, revapi, Checkstyle, Enforcer, JMH
```

Инструментарий production-grade расставлен полностью. Остался **один блокирующий** для
профессионального стандарта: **GitHub Actions CI**. Без него все настроенные плагины
(JaCoCo, PITest, revapi, Checkstyle, Enforcer) проверяются только локально.
CI — это то, что превращает «хороший код» в «производственную систему»: каждый
PR проверяется автоматически, деградация производительности и нарушения архитектуры
детектируются до merge, а не после.

После добавления `.github/workflows/ci.yml` и первого тега `v1.0.0` — `island-engine.jar`
готов к публикации на Maven Central.
