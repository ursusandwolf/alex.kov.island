# Code Review v9: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-08  
**База:** v8 (multi-module)  
**Дельта:** +20 новых файлов, 62 изменённых  
**Ключевые изменения:** `engine.internal`, SoA-хранилища, `SimulationConfig`,
`AutoCloseable SimulationContext`, `module-info` для всех плагинов, `SimCityBoundaryTest`

---

## Прогресс относительно v8

| Проблема из v8 | Статус |
|---|---|
| `engine.parallel`/`engine.scheduling` — internal классы экспортировались | ✅ **ИСПРАВЛЕНО** — `engine.internal` пакет, не экспортируется |
| SimCity тесты напрямую создавали `ParallelDispatcher`/`PhaseScheduler` | ✅ **ИСПРАВЛЕНО** — используют `SimulationEngine.build()` |
| `WorldInitializationTest` в неверном модуле и пакете | ✅ **ИСПРАВЛЕНО** — `island-nature/integration/` |
| `@InternalEngine`/`@EngineAPI` — `RetentionPolicy.SOURCE` | ✅ **ИСПРАВЛЕНО** — `RetentionPolicy.CLASS` |
| Нет `module-info.java` для плагинов | ✅ **ИСПРАВЛЕНО** — для `island-nature` и `island-simcity` |
| ArchUnit — нет правила для engine.internal | ✅ **ИСПРАВЛЕНО** — `pluginsShouldNotUseEngineInternals` |
| `SimulationContext` не освобождает ресурсы | ✅ **ИСПРАВЛЕНО** — `AutoCloseable` + `try-with-resources` |
| `EconomySystem.process()` — пустое тело | ✅ **ИСПРАВЛЕНО** — реализована логика |
| SoA/HealthSoAStore — volatile на array полях | ❌ **НЕ ИСПРАВЛЕНО** — данные race при `ensureCapacity()` |
| SoA хранилища не используются в горячем пути | ⚠️ **ЧАСТИЧНО** — wired, но системы не читают напрямую |
| `island-nature` модуль экспортирует слишком много | ⚠️ **НОВОЕ** |

---

## ✅ Детальный разбор ключевых улучшений

### 1. `engine.internal` — правильная инкапсуляция внутренностей

```
до v9: exports com.island.engine.parallel  (ParallelDispatcher — виден плагинам)
       exports com.island.engine.scheduling (PhaseScheduler — виден плагинам)

в v9:  engine.internal.*  — НЕ в module-info exports
       ParallelDispatcher, PhaseScheduler, DefaultEventBus → engine.internal
       ScheduledTask, Phase, GameLoop → engine.scheduling (остаются публичными, нужны плагину)
```

Теперь плагин физически не может импортировать `ParallelDispatcher` — JPMS выдаст
ошибку компиляции. ArchUnit добавляет второй уровень защиты. Это стандарт
production-библиотек. ✅

---

### 2. SoA-хранилища — правильная архитектурная идея

```java
// Public API: interface в engine.core
@EngineAPI
public interface HealthStorage {
    static HealthStorage create(int initialCapacity) {
        return new com.island.engine.internal.HealthSoAStore(initialCapacity); // factory method
    }
}

// Implementation: в engine.internal, @InternalEngine
@InternalEngine
public class HealthSoAStore implements HealthStorage {
    private long[] currentEnergy;  // примитивы, не объекты
    private long[] maxEnergy;
    private boolean[] alive;
}
```

Static factory method на интерфейсе скрывает реализацию. Плагин использует
`HealthStorage.create(n)` — никакого знания об `HealthSoAStore`. Архитектурный паттерн
правильный: API/SPI в `engine.core`, impl в `engine.internal`. ✅

Данные хранятся как примитивные массивы (`long[]`, `boolean[]`) — это Structure of Arrays.
По сравнению с `Map<entityId, HealthComponent>` даёт:
- Нет boxing/unboxing для `long` → `Long`
- Итерация по массиву cache-friendly (CPU prefetcher)
- Нет overhead объектных заголовков

---

### 3. `SimulationConfig` + `AutoCloseable` — чистый API

```java
// До v9:
context = engine.build(plugin, 100 /*ms*/, 4 /*threads*/);

// В v9:
SimulationConfig config = SimulationConfig.defaultFor(4);
try (SimulationContext<Organism> context = engine.build(plugin, config)) {
    // guaranteed resource cleanup
}
```

`try-with-resources` + `SimulationConfig` — это стандарт java.net.http, jOOQ, R2DBC.
`EcosystemBalanceTest` использует этот паттерн: ресурсы освобождаются даже при падении теста. ✅

---

### 4. `SimCityBoundaryTest` — образцовые тесты граничных условий

13 тест-кейсов с `@DisplayName`, все через публичный `SimulationEngine.build()` API:

```java
// Экономика: деньги == стоимость → успех
// Экономика: деньги == стоимость - 1 → отказ
// Пространство: угловые тайлы (0,0) и (width-1, height-1)
// Пространство: за пределами сетки → отказ
// Электросеть: ток через здания, не через дороги, не через пустые тайлы
// Загрязнение: промышленность снижает happiness соседей
// Налог 0% → дохода нет; Налог 100% → happiness падает
```

Это **professional-grade boundary value testing** (BVT). Каждый тест проверяет ровно одно
правило домена, название описывает ожидание. Это уровень команд, готовящих продукт
к релизу. ✅

---

### 5. `module-info.java` для всех модулей — JPMS изоляция завершена

```java
// island-nature — минимальный публичный API
module com.island.nature {
    requires com.island.engine;
    exports com.island.nature;               // NaturePlugin
    exports com.island.nature.config;        // Configuration
    exports com.island.nature.model;         // Island, Cell
    exports com.island.nature.view;          // ConsoleView, HeadlessView
    exports com.island.nature.entities.core; // Animal, Organism, SpeciesKey
    // entities.domain, entities.strategy, service — НЕ экспортируются ✅
}

// island-simcity — минимальный публичный API
module com.island.simcity {
    requires com.island.engine;
    exports com.island.simcity;              // SimCityPlugin
    exports com.island.simcity.model;        // CityMap, CityTile
    exports com.island.simcity.entities;     // SimEntity
    exports com.island.simcity.service;      // ← см. замечание
    exports com.island.simcity.view;
}
```

---

## 🔴 Технические проблемы

### 1. [HIGH] `HealthSoAStore`/`AgeSoAStore` — гонка при `ensureCapacity`

```java
// HealthSoAStore.java — НЕ thread-safe
private long[] currentEnergy;  // обычное поле, не volatile

private void ensureCapacity(int entityId) {
    if (entityId >= capacity) {
        // Создаём новый массив...
        currentEnergy = java.util.Arrays.copyOf(currentEnergy, newCapacity); // ← запись в поле
    }
}

// Другой поток читает:
public long getCurrentEnergy(int entityId) {
    return currentEnergy[entityId]; // ← читает старый массив-ссылку
}
```

Если `ensureCapacity` вызывается из одного потока, а `getCurrentEnergy` из другого,
JMM не гарантирует видимость новой ссылки на массив без `volatile`. Теоретически
второй поток может читать из устаревшего (меньшего) массива и получить `0` вместо
реального значения, или `ArrayIndexOutOfBoundsException`.

На практике сейчас `bindStorage` вызывается при инициализации (до начала параллельных
тиков), поэтому риск невелик. Но это хрупкий инвариант.

```java
// Fix: volatile на ссылки на массивы
private volatile long[] currentEnergy;
private volatile long[] maxEnergy;
private volatile boolean[] alive;

// И ensureCapacity — synchronized или через AtomicReference<long[]>
```

---

### 2. [MEDIUM] SoA в горячем пути не используется — паттерн не завершён

`HealthSoAStore` создан, заполнен данными через `bindStorage()`, но `AnimalHealthSystem`
до них не добирается напрямую:

```java
// AnimalHealthSystem.process() — идёт через объект Organism
entity.tryConsumeEnergy(metabolism);  // → внутри: healthStorage.setCurrentEnergy(entityId, ...)
entity.isAlive()                      // → внутри: healthStorage.isAlive(entityId)
```

Каждый вызов проходит через `Organism` → `entityId` lookup → `healthStorage[entityId]`.
Это лучше чем `HashMap`, но хуже полного SoA-паттерна. Реальная производительность SoA
достигается когда **система** итерирует по массиву напрямую:

```java
// Полный SoA-паттерн — система читает данные без посредников
public class AnimalHealthSystem extends NatureEntitySystem {
    private final HealthStorage healthStorage;
    private final EntityIdProvider idProvider;

    protected void processTick(int tickCount) {
        // Итерация по примитивному массиву — максимальная cache locality
        for (int id = 0; id < maxEntityId; id++) {
            if (!healthStorage.isAlive(id)) continue;
            long energy = healthStorage.getCurrentEnergy(id);
            long metabolism = getMetabolism(id);
            long newEnergy = energy - metabolism;
            if (newEnergy <= 0) {
                healthStorage.setAlive(id, false);
                eventBus.publish(new AnimalDiedEvent(..., DeathCause.HUNGER));
            } else {
                healthStorage.setCurrentEnergy(id, newEnergy);
            }
        }
    }
}
```

Пока SoA не используется так — это архитектурный задел, не реальное ускорение.

---

## 🟡 Архитектурные замечания

### 3. `island-nature` экспортирует детали реализации

```java
exports com.island.nature.model;  // Cell, Island, EntityContainer, Chunk...
```

`Cell` и `Island` — это внутренняя реализация природного плагина. Внешний код (island-app,
тесты) получает `Island` через `context.world()` как `SimulationWorld<Organism>`. Он
не должен знать о `Cell`, `EntityContainer`, `Chunk`.

Если `island-app` вынужден делать `(Island) context.world()` — это запах: значит `NatureWorld`
или `SimulationWorld` не предоставляет нужных методов.

```java
// Правильный путь:
// 1. NaturePlugin возвращает NatureContext с доступом к статистике
// 2. island-app получает NatureContext из NaturePlugin напрямую, не через world

module com.island.nature {
    exports com.island.nature;              // NaturePlugin, NatureContext
    exports com.island.nature.config;       // Configuration
    exports com.island.nature.view;         // Views
    exports com.island.nature.entities.core; // Organism, Animal, SpeciesKey
    // model — НЕ экспортировать
}
```

---

### 4. `island-simcity` экспортирует `service` пакет

```java
exports com.island.simcity.service; // BuildingService, EconomySystem, PopulationSystem...
```

Сервисы — это implementation details SimCity-плагина. `island-app` не должен
инстанциировать `BuildingService` напрямую — это делает `SimCityPlugin`. Тесты,
которые тестируют `BuildingService`, находятся в том же модуле (`island-simcity/test`)
и не требуют экспорта.

```java
module com.island.simcity {
    exports com.island.simcity;           // SimCityPlugin
    exports com.island.simcity.model;     // CityMap, CityTile (для app-уровня)
    exports com.island.simcity.entities;  // SimEntity
    exports com.island.simcity.view;      // CityConsoleView
    // service — НЕ экспортировать
}
// Тесты могут использовать opens com.island.simcity.service to ... (для рефлексии)
```

---

### 5. `EntityIdManager` — смешанная синхронизация

```java
private final AtomicInteger nextId = new AtomicInteger(0);
private final BitSet recycledIds = new BitSet();  // не thread-safe сам по себе

public int acquireId() {
    synchronized (recycledIds) {   // ← lock на recycledIds
        int firstSetBit = recycledIds.nextSetBit(0);
        if (firstSetBit != -1) {
            recycledIds.clear(firstSetBit);
            return firstSetBit;
        }
    }                              // ← lock освобождён
    return nextId.getAndIncrement(); // ← без lock
}
```

Между выходом из `synchronized` и `nextId.getAndIncrement()` — другой поток может
войти в `synchronized`, тоже не найти recycled ID, и тоже вызвать `getAndIncrement`.
Это **нормально** — они получат разные ID. Но: если к тому моменту в recycledIds
появился ID (от параллельного `releaseId`), первый поток его пропустит и возьмёт новый.

Это не баг (корректность сохранена), но это неоптимальное использование recycledIds.
Для реального ID-recycling нужен `ConcurrentLinkedQueue<Integer>` вместо `BitSet+synchronized`.

---

## 📐 Что нужно для профессионального стандарта

### Блок 1: Build & Release

**1.1 Maven-плагины для релизного артефакта**

Профессиональная Java-библиотека публикует три артефакта: `island-engine.jar`,
`island-engine-sources.jar`, `island-engine-javadoc.jar`. Сейчас pom.xml не настраивает ни один из них.

```xml
<!-- В island-engine/pom.xml -->
<build>
  <plugins>
    <!-- Sources JAR -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-source-plugin</artifactId>
      <version>3.3.0</version>
      <executions>
        <execution>
          <id>attach-sources</id>
          <goals><goal>jar-no-fork</goal></goals>
        </execution>
      </executions>
    </plugin>

    <!-- Javadoc JAR -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-javadoc-plugin</artifactId>
      <version>3.6.0</version>
      <configuration>
        <doclint>none</doclint>  <!-- не падать на warning -->
        <quiet>true</quiet>
      </configuration>
      <executions>
        <execution>
          <id>attach-javadocs</id>
          <goals><goal>jar</goal></goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**1.2 `maven-enforcer-plugin` — гарантия консистентности сборки**

```xml
<!-- В parent pom.xml -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.4.1</version>
  <executions>
    <execution>
      <goals><goal>enforce</goal></goals>
      <configuration>
        <rules>
          <requireMavenVersion><version>[3.8,)</version></requireMavenVersion>
          <requireJavaVersion><version>[21,)</version></requireJavaVersion>
          <dependencyConvergence/>  <!-- все зависимости одной версии -->
          <banDuplicatePomDependencyVersions/>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

### Блок 2: Качество кода

**2.1 JaCoCo — code coverage как CI gate**

```xml
<!-- В parent pom.xml -->
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>check</id>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <!-- engine должен быть хорошо покрыт -->
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.70</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

**2.2 JMH — performance regression как CI gate**

```xml
<!-- В island-engine/pom.xml (отдельный execution) -->
<dependency>
  <groupId>org.openjdk.jmh</groupId>
  <artifactId>jmh-core</artifactId>
  <version>1.37</version>
  <scope>test</scope>
</dependency>
```

```java
// island-engine/src/test/java/com/island/engine/bench/EventBusBenchmark.java
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class EventBusBenchmark {
    @Benchmark
    public void publishToOneSubscriber(Blackhole bh) {
        eventBus.publish(diedEvent);
        bh.consume(received.get());
    }
}
```

**2.3 PITest — mutation testing**

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>1.15.8</version>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>1.2.1</version>
    </dependency>
  </dependencies>
  <configuration>
    <targetClasses>
      <param>com.island.engine.*</param>
    </targetClasses>
    <mutationThreshold>65</mutationThreshold>
  </configuration>
</plugin>
```

---

### Блок 3: Javadoc — контракт библиотеки

Сейчас у `@EngineAPI`-классов есть Javadoc, но он неполный. Профессиональный стандарт
требует Javadoc на каждый публичный метод в engine.core:

```java
/**
 * Entry point for creating and managing simulation instances.
 *
 * <p>Usage:
 * <pre>{@code
 * SimulationConfig config = SimulationConfig.defaultFor(4);
 * try (SimulationContext<Organism> ctx = new SimulationEngine<Organism>().build(plugin, config)) {
 *     ctx.gameLoop().runTick();
 * }
 * }</pre>
 *
 * @param <T> the base entity type of the simulation
 * @since 1.0
 */
@EngineAPI
public class SimulationEngine<T extends Mortal> {

    /**
     * Builds a simulation context from the given plugin and config.
     * The returned context must be closed when no longer needed.
     *
     * @param plugin the simulation plugin providing world and tasks
     * @param config execution configuration (tick rate, thread count)
     * @return a ready-to-use, started simulation context
     * @throws IllegalArgumentException if plugin.createWorld() returns null
     */
    public SimulationContext<T> build(SimulationPlugin<T> plugin, SimulationConfig config) { ... }
}
```

---

### Блок 4: CI/CD

**4.1 GitHub Actions — минимальный pipeline**

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: 'maven' }

      - name: Build & Test
        run: mvn --batch-mode verify

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with: { files: '**/target/site/jacoco/jacoco.xml' }

      - name: ArchUnit (already in tests)
        run: echo "ArchUnit runs as part of island-app tests"
```

**4.2 Semantic versioning + CHANGELOG**

```
island-simulator-parent 1.0.0   — первый стабильный релиз
island-engine 1.0.0             — публичный API заморожен
island-nature 1.0.0             — плагин природы v1
island-simcity 1.0.0            — плагин города v1
```

`CHANGELOG.md` уже есть в проекте — нужно перевести его в формат
[Keep a Changelog](https://keepachangelog.com) с секциями `Added`, `Changed`,
`Fixed`, `Removed` для каждой версии.

---

### Блок 5: ServiceLoader — плагины без жёсткой зависимости

Сейчас `NatureLauncher` делает `new NaturePlugin(config)` — жёсткая связь.
Стандартный Java-способ discoverable plugins — `ServiceLoader`:

```java
// В island-engine:
// META-INF/services/com.island.engine.core.SimulationPlugin — не нужен для engine

// В island-nature:
// island-nature/src/main/resources/META-INF/services/com.island.engine.core.SimulationPlugin
com.island.nature.NaturePlugin

// В island-app:
ServiceLoader<SimulationPlugin<?>> loader = ServiceLoader.load(SimulationPlugin.class);
SimulationPlugin<?> plugin = loader.findFirst()
    .orElseThrow(() -> new IllegalStateException("No plugin found on classpath"));
```

Или с именованными модулями (JPMS `provides`/`uses`):
```java
// island-nature/module-info.java
provides com.island.engine.core.SimulationPlugin with com.island.nature.NaturePlugin;

// island-engine/module-info.java
uses com.island.engine.core.SimulationPlugin;
```

---

### Блок 6: API стабильность и семантическое версионирование

```java
// Разметить все публичные классы стабильностью:

@EngineAPI  // стабильный, не меняется без major-версии
public interface SimulationPlugin<T extends Mortal> { ... }

@EngineAPI
@Deprecated(since = "1.1", forRemoval = true) // старый метод с путём к новому
public SimulationContext<T> build(SimulationPlugin<T> p, int tick, int threads) { ... }

// @Beta (можно взять из guava или написать свой)
@Beta  // нестабильный, может меняться в minor-версиях
public interface HealthStorage { ... }  // SoA — ещё в разработке
```

---

## 📊 Итоговые оценки

### Динамика v1 → v9

| Критерий | v1 | v3 | v5 | v7 | v8 | v9 |
|---|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 8.0 | 8.5 | 9.0 | 9.0 | **9.5** |
| **Код** | 7.0 | 8.0 | 8.0 | 8.5 | 8.5 | **9.0** |
| **Переиспользуемость** | 6.0 | 7.5 | 8.5 | 9.0 | 9.5 | **9.5** |
| **Тестируемость** | 5.0 | 7.0 | 8.0 | 9.0 | 9.0 | **9.5** |
| **Готовность к JAR** | — | — | 6.5 | 8.5 | 8.5 | **9.5** |
| **Production-ready** | — | — | — | — | — | **7.0** |
| **Общая** | 6.5 | 8.0 | 8.5 | 9.0 | 9.0 | **9.5** |

*`Production-ready` — новая метрика: CI/CD, coverage, javadoc, benchmarks, versioning.*

---

## Вердикт: что нужно для профессионального стандарта

Проект достиг **9.5/10 по архитектуре** — это уровень, который редко встречается
даже в open-source библиотеках такого масштаба. Engine изолирован, JPMS работает,
два плагина доказывают переиспользуемость, ArchUnit защищает границы автоматически.

**Для `production-ready` уровня (сейчас 7.0/10) нужно 6 шагов:**

| Шаг | Что даёт | Трудоёмкость |
|---|---|---|
| `maven-source-plugin` + `maven-javadoc-plugin` | Публикация на Maven Central | 2 часа |
| `JaCoCo` с threshold | Coverage как CI gate | 1 день |
| `maven-enforcer-plugin` | Надёжность сборки | 2 часа |
| GitHub Actions CI | Автоматическая проверка PR | 2 часа |
| Javadoc на `@EngineAPI` методах | Контракт библиотеки | 2-3 дня |
| Semantic versioning + CHANGELOG | Понятный release process | 1 день |
| `ServiceLoader` (`provides`/`uses`) | Discoverable plugins | 1 день |
| `volatile` на SoA полях + SoA в горячем пути | Корректность + производительность | 2 дня |

**Суммарно: 10-14 рабочих дней** — и `island-engine` готов к публикации
на Maven Central как самостоятельная библиотека.
