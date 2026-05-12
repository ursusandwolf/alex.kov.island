# Code Review v11: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-10  
**База:** v10  
**Дельта:** +14 файлов, 37 изменённых, новый модуль `island-benchmarks`  
**Ключевые изменения:** JMH бенчмарки, `GameLoop.pause()/resume()`, Jackson prep,
`WorldSnapshotMixin`, `module-info.java` для island-app, ADR-004 (Spring Boot),
полный комплект документации, Mockito в engine-тестах, `BuildingProfile` enum

---

## Прогресс относительно v10

| Задача из v10 | Статус |
|---|---|
| JMH benchmark файлы написать | ✅ `SoABenchmark` — 2 benchmark метода, правильный `@Setup` |
| `island-benchmarks` — отдельный модуль | ✅ `maven-shade-plugin` для fat-JAR запуска |
| GitHub Actions CI | ❌ по-прежнему отсутствует |
| Javadoc полный на `@EngineAPI` | ⚠️ начат в SimulationContext, SimulationPlugin |
| ServiceLoader `provides/uses` | ✅ **ЧАСТИЧНО** — `uses` в `module-info.java` island-app |
| `GameLoop.pause()/resume()/getStatus()` | ✅ `SimulationStatus` enum + `AtomicBoolean paused` |
| Jackson для WebSocket | ✅ `SimulationJacksonConfig` + `WorldSnapshotMixin` |
| ADR для Spring Boot | ✅ ADR-004 статус «Предложено» |
| Полный комплект документации | ✅ CONTRIBUTING, GLOSSARY, ONBOARDING, ADRs, API docs |
| Mockito в engine-тестах | ✅ 5 новых тест-классов |
| Magic numbers в SimCity | ✅ `BuildingProfile` enum с `baseExpense/baseIncome` |
| `logback-classic` версия в parent | ❌ захардкожена в `island-app/pom.xml` |
| JaCoCo threshold | 🔴 **СНИЖЕН** с 58% до 50% |

---

## 🔴 Регрессия: JaCoCo threshold снижен

```xml
<!-- parent pom.xml — было 0.58, стало 0.50 -->
<jacoco.minimum.coverage>0.50</jacoco.minimum.coverage>
```

Были добавлены тесты (GameLoopControlTest, ParallelDispatcherTest и другие), но порог
**снижен**. Это противоположное направление движения. Threshold CI-gate должен только расти.

Вероятная причина: новые тест-классы охватывают только конкретные методы, а
JaCoCo считает coverage по всем модулям включая незатронутые. Правильный ответ —
настроить **per-module threshold** или исключить незатронутые пакеты, а не снижать порог.

```xml
<!-- Правильно: вернуть 0.58 и двигаться вверх -->
<jacoco.minimum.coverage>0.65</jacoco.minimum.coverage>
```

---

## ✅ Детальный разбор улучшений

### 1. GameLoop.pause()/resume() — правильная реализация

```java
private final AtomicBoolean paused = new AtomicBoolean(false);

public enum SimulationStatus { IDLE, RUNNING, PAUSED }

public SimulationStatus getStatus() {
    if (!running.get()) return SimulationStatus.IDLE;
    return paused.get() ? SimulationStatus.PAUSED : SimulationStatus.RUNNING;
}

// В run() loop:
if (paused.get()) {
    Thread.sleep(tickDurationMs);
    continue;  // пропускает tick, но не освобождает поток
}
```

State machine через два `AtomicBoolean` — простое и корректное решение. `getStatus()`
является composite view без дополнительного состояния. Готово для REST `/simulation/status`. ✅

### 2. WorldSnapshotMixin — Jackson без зависимости в engine

```java
// island-app — знает о плагинах, Jackson здесь уместен
@JsonTypeInfo(use = Id.NAME, property = "simulationType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IslandSnapshot.class, name = "nature"),
    @JsonSubTypes.Type(value = CitySnapshot.class, name = "simcity")
})
public interface WorldSnapshotMixin { }

// island-engine — НЕ знает о Jackson, интерфейс чист
public interface WorldSnapshot { ... }  // без аннотаций
```

Правильный паттерн: `engine` остаётся без Jackson-зависимости. Mixin — в `island-app`,
который знает обо всех плагинах. ✅

**Одно замечание:** `WorldSnapshotMixin` явно перечисляет оба плагина. При добавлении
третьего плагина нужно менять `island-app`. Это приемлемо — `island-app` — точка сборки.
Но в Spring-контексте лучше автоматическая регистрация (см. раздел о Spring).

### 3. SoABenchmark — правильный JMH-класс

```java
@BenchmarkMode(Mode.Throughput)
@Fork(1) @Warmup(iterations = 2, time = 1) @Measurement(iterations = 3, time = 1)
public class SoABenchmark {
    @Setup
    public void setup() {
        soaStore = HealthStorage.create(10_000);
        mapStore = new HashMap<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            soaStore.set(i, 1000L, 1000L, true);
            mapStore.put(i, 1000L);
        }
    }

    @Benchmark public void soaSequentialRead(Blackhole bh) { ... }
    @Benchmark public void mapSequentialRead(Blackhole bh) { ... }
}
```

`Blackhole` предотвращает dead code elimination. `@State(Scope.Thread)` — изолированное
состояние для каждого потока JMH. Запуск: `java -jar island-benchmarks/target/benchmarks.jar`. ✅

**Нюанс:** `@Fork(1)` — один fork. Для более надёжных результатов стандарт: `@Fork(2)`.
`@Warmup(iterations=2, time=1)` — минимальный прогрев. Для публикации результатов: минимум 5.

### 4. BuildingProfile enum — magic numbers устранены

```java
public enum BuildingProfile {
    ROAD(1, 0), RESIDENTIAL(2, 0), COMMERCIAL(5, 500),
    INDUSTRIAL(10, 1000), POWER_PLANT(50, 0);

    private final long baseExpense;
    private final long baseIncome;

    public static long getDensityMultiplier(Density density) {
        return switch (density) { case LOW -> 1; case MEDIUM -> 4; case HIGH -> 12; };
    }
}
```

Паттерн «данные в enum» — стандарт для доменных констант. `EconomyService` теперь
использует `BuildingProfile.of(type).getBaseExpense()` вместо `switch` с числами. ✅

### 5. Документация — полный комплект

ADR-001 (ECS), ADR-002 (SoA), ADR-003 (JPMS), ADR-004 (Spring) + CONTRIBUTING, GLOSSARY,
ONBOARDING, ECS_GUIDE, EVENTS_CATALOG, PLUGIN_GUIDE, SCHEDULING, TESTING_GUIDE, API docs.
Полный комплект реализован за один итерацию. ✅

---

## 📐 Spring Boot интеграция: правильная архитектура

ADR-004 описывает намерение. Вопрос: как реализовать правильно?

### Структура нового модуля

```
island-app/
├── pom.xml                                     ← добавить spring-boot-starter-web
│                                                  spring-boot-starter-websocket
└── src/main/java/com/island/
    ├── IslandApplication.java                  ← @SpringBootApplication
    ├── config/
    │   ├── SimulationBeanConfig.java           ← @Configuration: плагины как Beans
    │   ├── JacksonConfig.java                  ← ObjectMapper + Mixin регистрация
    │   └── WebSocketConfig.java                ← STOMP endpoint
    ├── controller/
    │   └── SimulationController.java           ← @RestController
    ├── service/
    │   └── SimulationService.java              ← @Service, lifecycle
    └── websocket/
        └── SimulationBroadcaster.java          ← EventBus → STOMP push
```

---

### Шаг 1: pom.xml — Spring Boot без переопределения engine

```xml
<!-- island-app/pom.xml -->
<parent>
    <!-- НЕ spring-boot-starter-parent — у нас свой parent -->
    <groupId>com.island</groupId>
    <artifactId>island-simulator-parent</artifactId>
</parent>

<dependencies>
    <!-- Simulation modules -->
    <dependency><groupId>com.island</groupId><artifactId>island-engine</artifactId></dependency>
    <dependency><groupId>com.island</groupId><artifactId>island-nature</artifactId></dependency>
    <dependency><groupId>com.island</groupId><artifactId>island-simcity</artifactId></dependency>

    <!-- Spring Boot — только web-слой -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.2.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
        <version>3.2.0</version>
    </dependency>
    <!-- Jackson уже в spring-boot-starter-web, отдельно не нужен -->
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.2.0</version>
            <executions>
                <execution>
                    <goals><goal>repackage</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Критически важно:** Spring Boot BOM (`spring-boot-dependencies`) управляет версиями
зависимостей. Вместо `spring-boot-starter-parent` (требует смены parent) — импортировать
BOM как `<scope>import</scope>` в parent pom:

```xml
<!-- parent pom.xml — dependencyManagement -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>3.2.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

---

### Шаг 2: SimulationBeanConfig — плагины как Spring Beans

```java
@Configuration
public class SimulationBeanConfig {

    // Spring Profiles: запуск с -Dspring.profiles.active=nature или simcity
    @Bean
    @Profile("nature")
    public SimulationPlugin<Organism> naturePlugin(
            @Value("${sim.width:20}") int width,
            @Value("${sim.height:20}") int height) {
        Configuration cfg = Configuration.load();
        cfg.setIslandWidth(width);
        cfg.setIslandHeight(height);
        return new NaturePlugin(cfg);
    }

    @Bean
    @Profile("simcity")
    public SimulationPlugin<SimEntity> simCityPlugin() {
        return new SimCityPlugin();
    }

    // SimulationConfig из application.yml
    @Bean
    public SimulationConfig simulationConfig(
            @Value("${sim.threads:4}") int threads,
            @Value("${sim.tickMs:100}") int tickMs) {
        return SimulationConfig.builder()
                .threadCount(threads)
                .tickDurationMs(tickMs)
                .build();
    }

    // SimulationContext как управляемый Spring-бин
    // ВАЖНО: SimulationContext<T> — generic, Spring не любит raw generics
    // Решение: отдельный bean для каждого плагина через @SuppressWarnings
    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SimulationContext simulationContext(
            SimulationPlugin plugin,
            SimulationConfig config) {
        return new SimulationEngine().build(plugin, config);
    }
}
```

---

### Шаг 3: SimulationService — управление жизненным циклом

```java
@Service
public class SimulationService {

    private final SimulationContext<?> context;

    public SimulationService(SimulationContext<?> context) {
        this.context = context;
    }

    // AutoCloseable + Spring lifecycle = корректное освобождение ресурсов
    @PreDestroy
    public void shutdown() {
        context.close(); // вызывает gameLoop.stop() + executor.shutdown()
        log.info("Simulation shutdown complete");
    }

    public void pause()  { context.gameLoop().pause(); }
    public void resume() { context.gameLoop().resume(); }

    public SimulationStatus getStatus() {
        return context.gameLoop().getStatus();
    }

    public WorldSnapshot getSnapshot() {
        return context.world().createSnapshot();
    }
}
```

---

### Шаг 4: SimulationController — REST API

```java
@RestController
@RequestMapping("/api/v1/simulation")
public class SimulationController {

    private final SimulationService service;

    @PostMapping("/pause")
    public ResponseEntity<StatusResponse> pause() {
        service.pause();
        return ResponseEntity.ok(new StatusResponse(service.getStatus()));
    }

    @PostMapping("/resume")
    public ResponseEntity<StatusResponse> resume() {
        service.resume();
        return ResponseEntity.ok(new StatusResponse(service.getStatus()));
    }

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        return ResponseEntity.ok(new StatusResponse(service.getStatus()));
    }

    @GetMapping("/snapshot")
    public ResponseEntity<WorldSnapshot> snapshot() {
        return ResponseEntity.ok(service.getSnapshot());
    }

    // DTO — никогда не возвращать напрямую domain-объекты из engine
    record StatusResponse(GameLoop.SimulationStatus status) {}
}
```

---

### Шаг 5: SimulationBroadcaster — EventBus → WebSocket (правильная интеграция)

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-simulation")
                .setAllowedOriginPatterns("*")  // настроить для prod
                .withSockJS();
    }
}

// SimulationBroadcaster.java
@Component
public class SimulationBroadcaster {

    private final SimpMessagingTemplate messaging;
    private final EventBus eventBus;
    private final SimulationContext<?> context;

    private volatile int snapshotInterval = 5; // каждые N тиков

    // КЛЮЧЕВОЙ МОМЕНТ: подписаться на EventBus, НЕ на Spring Events
    // Симуляция работает в своём потоке, EventBus — её коммуникационный канал
    @EventListener(ApplicationStartedEvent.class)
    public void startBroadcasting() {
        // 1. Подписаться на завершение тика через ScheduledTask в фазе POSTPROCESS
        context.gameLoop().addRecurringTask(new TickBroadcastTask());

        // 2. Подписаться на доменные события
        eventBus.subscribe(AnimalDiedEvent.class, this::onAnimalDied);
        eventBus.subscribe(AnimalBornEvent.class, this::onAnimalBorn);
    }

    // Вызывается из потока симуляции — messaging.convertAndSend() thread-safe ✅
    private class TickBroadcastTask implements ScheduledTask {
        @Override public Phase phase() { return Phase.POSTPROCESS; }
        @Override public int priority() { return -1; } // последним

        @Override
        public void tick(int tickCount) {
            if (tickCount % snapshotInterval != 0) return;
            WorldSnapshot snapshot = context.world().createSnapshot();
            messaging.convertAndSend("/topic/world-state",
                new TickSnapshotMessage("tick_snapshot", snapshot));
        }
    }

    private void onAnimalDied(AnimalDiedEvent event) {
        messaging.convertAndSend("/topic/events",
            new EntityEventMessage("ANIMAL_DIED", event.getAnimal().getSpeciesKey().getCode(),
                event.getCause().name(), -1, -1)); // x/y нужно добавить в событие
    }

    @PostMapping("/api/v1/simulation/speed")
    public void setSpeed(@RequestParam int interval) {
        this.snapshotInterval = interval;
    }

    record TickSnapshotMessage(String type, WorldSnapshot payload) {}
    record EntityEventMessage(String type, String species, String cause, int x, int y) {}
}
```

---

### Шаг 6: JacksonConfig — правильная регистрация в Spring

```java
// ВМЕСТО SimulationJacksonConfig.createMapper() (ручное создание)
// ИСПОЛЬЗОВАТЬ Jackson2ObjectMapperBuilderCustomizer (Spring-way):

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer simulationJsonCustomizer() {
        return builder -> builder
            .mixIn(WorldSnapshot.class, WorldSnapshotMixin.class)
            .mixIn(NodeSnapshot.class, NodeSnapshotMixin.class)  // если нужно
            .featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );
    }
}
```

**Почему `Jackson2ObjectMapperBuilderCustomizer` а не `ObjectMapper @Bean`:**
- Spring Boot создаёт `ObjectMapper` через `JacksonAutoConfiguration`
- `@Bean ObjectMapper` переопределяет автоконфигурацию полностью — теряются дефолты Spring Boot
- Customizer — аддитивный: добавляет настройки к Spring Boot ObjectMapper без замены

---

### Шаг 7: application.yml — конфигурация симуляции

```yaml
# application.yml
spring:
  profiles:
    active: nature  # или simcity

server:
  port: 8080

sim:
  width: 20
  height: 20
  threads: 4
  tickMs: 100

# Для природы
---
spring.config.activate.on-profile: nature
sim:
  width: 30
  height: 30

---
spring.config.activate.on-profile: simcity
sim:
  width: 15
  height: 15
  tickMs: 200
```

---

### Ключевые проблемы при Spring-интеграции и их решения

#### Проблема 1: Generic types и Spring DI

```java
// ПРОБЛЕМА: Spring не умеет различать SimulationPlugin<Organism> и SimulationPlugin<SimEntity>
@Autowired SimulationPlugin<?> plugin;  // какой именно?

// РЕШЕНИЕ A: Квалификаторы
@Bean @Qualifier("nature") public SimulationPlugin<Organism> naturePlugin() {...}
@Bean @Qualifier("simcity") public SimulationPlugin<SimEntity> simCityPlugin() {...}
@Autowired @Qualifier("nature") SimulationPlugin<?> plugin;

// РЕШЕНИЕ B (рекомендуется): Spring Profiles (уже в ADR-004)
// @Profile("nature") на bean → только один активен
```

#### Проблема 2: WorldSnapshot — интерфейс, Jackson не знает конкретный тип

```java
// ПРОБЛЕМА: @GetMapping("/snapshot") возвращает WorldSnapshot (интерфейс)
// Jackson не знает как сериализовать, выдаёт {}

// РЕШЕНИЕ: WorldSnapshotMixin уже создан (✅ сделано в v11)
// Добавить в JacksonConfig через Jackson2ObjectMapperBuilderCustomizer
```

#### Проблема 3: Симуляция в отдельном потоке — не останавливается при Ctrl+C

```java
// ПРОБЛЕМА: GameLoop.run() в отдельном потоке, Spring shutdown hook не знает о нём

// РЕШЕНИЕ: SimulationService с @PreDestroy (✅ показано выше)
// SimulationContext implements AutoCloseable → context.close() в @PreDestroy
// Spring вызывает @PreDestroy при graceful shutdown → поток корректно останавливается
```

#### Проблема 4: GC-давление при частой сериализации большого WorldSnapshot

```java
// ПРОБЛЕМА: Каждый тик создаёт новый WorldSnapshot → JSON → миллионы объектов

// РЕШЕНИЯ:
// 1. Снижать частоту: каждые 5 тиков, не каждый
// 2. Дельта-сжатие: отправлять только изменившиеся cells
// 3. Binary протокол вместо JSON: MessagePack или Protobuf
// 4. Слабые ссылки на снэпшот: не держать в памяти старый snapshot

// Для начала — достаточно snapshotInterval=5
```

#### Проблема 5: CORS для локальной React разработки

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")  // React dev server
                    .allowedMethods("GET", "POST", "PATCH");
            }
        };
    }
}
```

---

## 🟡 Оставшиеся замечания

### 1. `logback-classic` — версия захардкожена в island-app

```xml
<!-- island-app/pom.xml — захардкоженная версия -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>  <!-- ← перенести в parent dependencyManagement -->
</dependency>
```

При переходе на Spring Boot — `spring-boot-starter-web` включает logback. Эта
зависимость станет дублирующей. Удалить после добавления Spring Boot.

### 2. `GameLoopControlTest` импортирует `engine.internal`

```java
import com.island.engine.internal.PhaseScheduler;
PhaseScheduler scheduler = mock(PhaseScheduler.class);
```

Приемлемо для white-box теста в engine-модуле. Но тест-мок `PhaseScheduler` хрупкий:
при изменении конструктора сломается. Рекомендуется тестировать через `SimulationEngine.build()`.

### 3. GitHub Actions CI — по-прежнему отсутствует

Единственный пункт из professional checklist, который не добавлен за 11 итераций.
Без CI инструменты (JaCoCo, PITest, revapi, Checkstyle, Enforcer) проверяются только локально.

`.github/workflows/ci.yml` — 30 минут работы. Шаблон дан в ревью v9.

---

## 📊 Итоговая оценка

| Критерий | v1 | v5 | v8 | v10 | v11 |
|---|---|---|---|---|---|
| **Архитектура** | 6.5 | 8.5 | 9.0 | 9.5 | **9.5** |
| **Код** | 7.0 | 8.0 | 8.5 | 9.5 | **9.5** |
| **Переиспользуемость** | 6.0 | 8.5 | 9.0 | 9.5 | **9.5** |
| **Тестируемость** | 5.0 | 8.0 | 9.0 | 9.5 | **9.5** |
| **Готовность к JAR** | — | 6.5 | 9.5 | 9.5 | **9.5** |
| **Production-ready** | — | — | — | 8.0 | **8.5** |
| **Spring-ready** | — | — | — | — | **6.0** |
| **Общая** | 6.5 | 8.5 | 9.0 | 9.5 | **9.5** |

*Spring-ready 6.0/10: ADR готов, Jackson подготовлен, pause/resume добавлен,
но Spring Boot зависимости не подключены, beans не объявлены.*

---

## Вердикт

Проект вплотную подошёл к Spring-интеграции: `GameLoop` имеет pause/resume/status,
`SimulationContext` — `AutoCloseable`, Jackson-конфигурация подготовлена,
ADR-004 описывает правильную архитектуру.

**Следующий шаг — один PR с конкретными файлами:**

| Файл | Назначение |
|---|---|
| `island-app/pom.xml` | Spring Boot BOM + web + websocket starter |
| `IslandApplication.java` | `@SpringBootApplication` |
| `SimulationBeanConfig.java` | Плагины как `@Bean` с `@Profile` |
| `JacksonConfig.java` | `Jackson2ObjectMapperBuilderCustomizer` |
| `WebSocketConfig.java` | STOMP endpoint |
| `SimulationController.java` | pause/resume/status/snapshot |
| `SimulationService.java` | `@Service` + `@PreDestroy` |
| `SimulationBroadcaster.java` | EventBus → STOMP push |
| `application.yml` | Конфигурация через Spring profiles |
| `.github/workflows/ci.yml` | GitHub Actions — **давно ждёт** |

**Ключевые решения, которые нельзя нарушать:**
- `Jackson2ObjectMapperBuilderCustomizer` вместо `@Bean ObjectMapper` (не ломает Spring Boot autoconfigure)
- `@Profile` для разных плагинов (не условные `if` в коде)
- `SimulationContext.close()` в `@PreDestroy` (не просто `gameLoop.stop()`)
- Broadcast из ScheduledTask POSTPROCESS, не из Spring `@Scheduled` (симуляция живёт в своём потоке)
