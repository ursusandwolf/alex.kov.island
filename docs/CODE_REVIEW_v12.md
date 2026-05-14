# Code Review v12: alex.kov.island (branch: dev)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-13  
**База:** feature/spring00  
**Объём:** +5 Java файлов, 29 изменённых, +5 frontend файлов  
**Ключевые изменения:** `NamedSimulationPlugin`, `SimulationProperties`, `GlobalExceptionHandler`,
`CorsConfig`, компонентное разбиение фронтенда, Vitest тесты, `volatile`, `Optional<>` везде

---

## Прогресс: всё из ревью spring00 закрыто

| Замечание из spring00 | Статус |
|---|---|
| `node_modules/` не в `.gitignore` | ✅ Добавлен `island-ui/node_modules/`, `island-ui/dist/` |
| `volatile SimulationContext<?> context` | ✅ Проставлен |
| `volatile int snapshotInterval` | ✅ Проставлен |
| `SimulationService`: local capture `current = this.context` | ✅ Все методы читают через local var |
| `colors.ts` регистр: `'Wolf'` не совпадал | ✅ `code.toLowerCase()` |
| `@SpringBootTest` в контроллер-тесте | ✅ `@WebMvcTest` + `@MockBean` |
| `@Validated` + `@Min`/`@Max` на параметрах | ✅ `width`, `height`, `tickMs` |
| `GlobalExceptionHandler` | ✅ `@RestControllerAdvice` с тремя handlers |
| `SnapshotHistoryService` — `null` вместо `Optional` | ✅ `Optional<String>`, `Optional<WorldSnapshot>` |
| Frontend CI job | ✅ `frontend-build` с `lint` + `build` |
| `error` state не отображался в UI | ✅ Отображается в `App.tsx` |
| HTTP 4xx/5xx игнорировались в store | ✅ `res.ok` проверяется, `error` в store |
| `if/else` по типу плагина в `SimulationService` | ✅ `NamedSimulationPlugin` + реестр через `Map` |
| `SimulationProperties` с `@ConfigurationProperties` | ✅ `prefix = "sim"` |
| CORS не настроен для REST | ✅ `CorsConfig` с `@Value` для `allowed-origins` |
| `App.tsx` — монолит 250 строк | ✅ `SimulationControls`, `SimulationMetrics`, `SnapshotHistoryPanel` |
| Нет frontend-тестов | ✅ `WorldCanvas.test.tsx`, `useSimulationStore.test.ts` с Vitest |
| `spring.profiles.active` как sim type | ✅ `sim.defaultPlugin` |

**Это полное закрытие всех 17 замечаний прошлого ревью за один цикл.**

---

## 🔴 Баг: `SimulationServiceIntegrationTest` не скомпилируется

```java
// SimulationServiceIntegrationTest.java:39
simulationService.start("nature", 20, 20, 100);
await().atMost(2, SECONDS)   // ← await() не импортирован
       .until(() -> simulationService.getStatus() == SimulationStatus.RUNNING); // SECONDS — тоже
```

`await()` — статический метод из Awaitility. `SECONDS` — из `java.util.concurrent.TimeUnit`.
Ни того ни другого нет в импортах теста. Awaitility нет в `pom.xml`. Тест упадёт при компиляции.

```java
// Fix A: добавить Awaitility в island-app/pom.xml:
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.1</version>
    <scope>test</scope>
</dependency>
// + импорты:
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

// Fix B: без Awaitility — Thread.sleep + retry
int attempts = 0;
while (simulationService.getStatus() != SimulationStatus.RUNNING && attempts++ < 20) {
    Thread.sleep(100);
}
assertEquals(SimulationStatus.RUNNING, simulationService.getStatus());
```

Awaitility рекомендуется — это стандарт для асинхронных Spring Boot тестов.

---

## 🟡 Замечания

### 1. Frontend CI: `npm test` отсутствует в pipeline

```yaml
# .github/workflows/ci.yml — frontend-build job
- name: Lint
  run: npm run lint
- name: Build
  run: npm run build
# ← npm run test отсутствует
```

Vitest тесты написаны, но CI их не запускает. Тест падает только локально — не в PR.

```yaml
# Добавить:
- name: Test
  run: npm run test
```

---

### 2. `SimulationBeanConfig` — избыточный `@Bean` для коллекции плагинов

```java
@Bean
public List<NamedSimulationPlugin<?>> simulationPlugins(List<NamedSimulationPlugin<?>> plugins) {
    log.info("Registered plugins injected by Spring: {}", plugins);
    return plugins;  // ← принять List и вернуть тот же List
}
```

Spring автоматически инжектирует `List<NamedSimulationPlugin<?>>` в конструктор
`SimulationService` без этого `@Bean`. Метод делает то, что Spring делает сам.
Создаёт путаницу: кажется что возвращается новый список, а не тот же.

```java
// Fix: удалить @Bean, оставить только логирование при инициализации SimulationService:
public SimulationService(..., List<NamedSimulationPlugin<?>> pluginList, ...) {
    this.plugins = pluginList.stream().collect(Collectors.toMap(...));
    log.info("Registered plugins: {}", plugins.keySet()); // ← логирование здесь
}
```

---

### 3. `@ComponentScan` в `SimulationBeanConfig` — дублирует `@SpringBootApplication`

```java
// SimulationBeanConfig.java
@ComponentScan(basePackages = "com.island")  // ← избыточно

// Main.java
@SpringBootApplication(scanBasePackages = "com.island")  // ← уже сканирует com.island
```

Двойной `@ComponentScan` не вызывает ошибок, но создаёт двойной проход по пакетам
при старте. Убрать из `SimulationBeanConfig`.

---

### 4. `SimulationProperties` не покрывает все `sim.*` свойства

```java
@ConfigurationProperties(prefix = "sim")
public class SimulationProperties {
    private int width = 20;
    private int height = 20;
    private int threads = 4;
    private int tickMs = 100;
    private String defaultPlugin = "nature";
    // ← отсутствуют:
}

// В SnapshotHistoryService:
@Value("${sim.history.dir:data/snapshots}") private String historyDir;

// В SimulationBroadcaster:
@Value("${sim.broadcast-interval:5}") private volatile int snapshotInterval;

// В CorsConfig:
@Value("${sim.cors.allowed-origins:...}") private String[] origins;
```

Идея `@ConfigurationProperties` — все свойства домена в одном типизированном месте.
Половина `sim.*` по-прежнему в `@Value`. Добавить:

```java
@ConfigurationProperties(prefix = "sim")
public class SimulationProperties {
    private int width = 20;
    private int height = 20;
    private int threads = 4;
    private int tickMs = 100;
    private String defaultPlugin = "nature";
    private String historyDir = "data/snapshots";     // +
    private int broadcastInterval = 5;                 // +
    private Cors cors = new Cors();                    // +

    @Getter @Setter
    public static class Cors {
        private String[] allowedOrigins = {"http://localhost:5173"};
    }
}
```

---

### 5. `SimulationControllerTest` — нет теста на валидацию

`@Validated` + `@Min(5)` на `width` добавлен, но ни одного теста на неверный ввод:

```java
// Добавить в SimulationControllerTest:
@Test
void start_withInvalidWidth_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/simulation/start")
                   .param("width", "2"))  // < Min(5)
           .andExpect(status().isBadRequest());
}

@Test
void start_withUnknownPlugin_returns400() throws Exception {
    doThrow(new IllegalArgumentException("Unknown plugin: random"))
        .when(simulationService).start(eq("random"), anyInt(), anyInt(), anyInt());

    mockMvc.perform(post("/api/v1/simulation/start")
                   .param("type", "random"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message").value("Unknown plugin: random"));
}
```

---

### 6. Frontend: нет теста на ошибочный fetch в `useSimulationStore.test.ts`

`pause()` теперь проверяет `res.ok` и записывает `error` в store. Но тест
это не проверяет:

```typescript
test('pause fails → error state is set', async () => {
    (global.fetch as any).mockResolvedValueOnce({ ok: false, statusText: 'Service Unavailable' });
    await useSimulationStore.getState().pause();
    expect(useSimulationStore.getState().error).toContain('Pause failed');
    expect(useSimulationStore.getState().status).toBe('IDLE'); // статус не изменился
});
```

---

### 7. `SocialService` — эффект на соседей без lock

```java
// SocialService.doProcessTile() — читает BuildingComponent, пишет в соседние тайлы
for (int dx = -radius; dx <= radius; dx++) {
    for (int dy = -radius; dy <= radius; dy++) {
        CityTile neighbor = map.getGrid()[nx][ny];
        neighbor.setEducationLevel(neighbor.getEducationLevel() + bonus);
    }
}
```

`SocialService` работает в `Phase.PREPARE` — последовательно, не параллельно.
Несколько тайлов могут быть источниками и одновременно модифицировать одного соседа.
В последовательном PREPARE это безопасно, но стоит задокументировать это ограничение.

---

## ✅ Что особенно хорошо

### `SimulationService` — race-free контекст-свитчинг

```java
private void doStart(...) {
    if (context != null) {
        SimulationContext<?> oldContext = this.context;
        this.context = null; // ← сначала null (volatile write), потом close
        oldContext.close();
    }
    ...
    this.context = new SimulationEngine().build(plugin, config); // ← volatile write
    eventPublisher.publishEvent(...);
    this.context.gameLoop().start();
}
```

Паттерн: сначала обнулить `context` (volatile write → видимо всем потокам), потом закрыть
старый. `getStatus()`, `getSnapshot()`, `pause()` — все читают через local variable.
Это корректная публикация нового состояния по JMM. ✅

### `NamedSimulationPlugin.withConfiguration()` — фабричный метод на синглтоне

```java
// NamedSimulationPlugin — документирован контракт
/**
 * Implementations MUST return a new instance to prevent
 * concurrent configuration pollution on singleton beans.
 */
SimulationPlugin<T> withConfiguration(int width, int height, WorldSnapshot snapshot);
```

Синглтон как фабрика — стандартный паттерн. Контракт задокументирован. ✅

### `useSimulationStore.test.ts` — мок fetch глобально + сброс состояния

```typescript
beforeEach(() => {
    vi.resetAllMocks();
    useSimulationStore.setState({ status: 'IDLE', ... });
});
```

Сброс состояния Zustand в `beforeEach` — правильный паттерн изолированных тестов. ✅

---

## 📐 Следующие шаги к профессиональному стандарту

### 1. SpringDoc OpenAPI — автоматическая API-документация

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

После добавления: `http://localhost:8080/swagger-ui.html` — живая документация REST API.
Frontend-разработчик больше не читает Java код чтобы понять что принимает endpoint.

Дополнить аннотациями:
```java
@Operation(summary = "Start simulation", description = "Starts a new simulation, replacing any running one")
@ApiResponse(responseCode = "200", description = "Simulation started")
@ApiResponse(responseCode = "400", description = "Invalid parameters or unknown plugin type")
@PostMapping("/start")
public ResponseEntity<StatusResponse> start(...) { ... }
```

---

### 2. Spring Boot Actuator — production observability

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
```

Добавит:
- `GET /actuator/health` — health check для kubernetes/load balancer
- `GET /actuator/metrics` — JVM, HTTP, custom simulation metrics
- Интеграция с Prometheus/Grafana для мониторинга тиков/секунду

Пользовательская метрика:
```java
@Component
public class SimulationMetricsExporter {
    private final MeterRegistry registry;
    private final SimulationService service;

    @Scheduled(fixedRate = 5000)
    public void export() {
        registry.gauge("simulation.tick.count",
            service.getSnapshot().map(WorldSnapshot::getTickCount).orElse(0));
        registry.gauge("simulation.entity.count",
            service.getSnapshot().map(WorldSnapshot::getTotalEntityCount).orElse(0));
    }
}
```

---

### 3. Docker — контейнеризация

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Backend
COPY island-app/target/island-app-*.jar app.jar

# Frontend (собранный dist/)
COPY island-ui/dist/ /app/static/

EXPOSE 8080
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
services:
  simulation:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=nature
      - SIM_WIDTH=30
      - SIM_HEIGHT=30
    volumes:
      - ./data/snapshots:/app/data/snapshots
```

```yaml
# CI — добавить Docker build job:
  docker-build:
    runs-on: ubuntu-latest
    needs: [build-and-test, frontend-build]
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -t island-simulator:${{ github.sha }} .
```

Это позволяет запустить приложение одной командой: `docker-compose up`.

---

### 4. `@ConfigurationPropertiesBinding` — валидация конфигурации при старте

```java
@Validated  // ← добавить к SimulationProperties
@ConfigurationProperties(prefix = "sim")
public class SimulationProperties {

    @Min(5) @Max(500)
    private int width = 20;

    @Min(5) @Max(500)
    private int height = 20;

    @Min(1) @Max(64)
    private int threads = 4;

    @Min(10) @Max(60000)
    private int tickMs = 100;

    @NotBlank
    private String defaultPlugin = "nature";
}
```

При неверном `application.yml` → Spring откажется стартовать с внятной ошибкой:
```
Binding to target SimulationProperties failed:
    Property: sim.threads
    Value: 0
    Reason: must be greater than or equal to 1
```

Лучше, чем молчаливое значение по умолчанию или NPE в runtime.

---

### 5. Snapshot persistence — хранение в БД вместо FS

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

```java
@Entity
public class SimulationSnapshotRecord {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private String pluginType;

    @Column(columnDefinition = "CLOB")
    private String snapshotJson;  // serialized WorldSnapshot
}
```

Плюсы: транзакции, поиск по дате/типу, пагинация, совместная история при scale-out.
H2 для dev, PostgreSQL для production через профиль.

---

### 6. Безопасность — базовая аутентификация

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/simulation/status", "/api/v1/simulation/snapshot").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/ws-simulation/**"))
            .build();
    }
}
```

Даже базовый HTTP Basic защищает от случайного `POST /simulation/start` от соседнего сервиса.

---

## 📊 Итоговая оценка

| Критерий | v1 | v8 | v11 | spring00 | v12 |
|---|---|---|---|---|---|
| **Архитектура движка** | 6.5 | 9.0 | 9.5 | 9.5 | **9.5** |
| **Код** | 7.0 | 8.5 | 9.5 | 9.0 | **9.5** |
| **Spring Integration** | — | — | 3.0 | 8.0 | **9.0** |
| **Frontend** | — | — | — | 6.5 | **7.5** |
| **Production-ready** | — | — | 8.5 | 8.5 | **9.0** |
| **Общая** | 6.5 | 9.0 | 9.5 | 9.0 | **9.5** |

---

## Вердикт

Проект в отличной форме. Все критические замечания предыдущего ревью закрыты.
Spring-слой грамотно инкапсулирован: `volatile` на shared state, `Optional` везде,
плагинный реестр через коллекцию бинов, валидация на входе, обработка ошибок на выходе.

**Три вещи до merge:**

1. **Awaitility в pom** — `SimulationServiceIntegrationTest` не скомпилируется
2. **`npm run test` в CI** — Vitest тесты написаны, но не запускаются в pipeline
3. **Удалить лишний `@Bean simulationPlugins`** — Spring сделает это автоматически

**Следующий шаг для перехода на production-grade** — SpringDoc OpenAPI (1 час) + Spring Boot Actuator (30 минут) + Docker (2 часа). После этого любой инженер сможет поднять симулятор одной командой, получить автодокументацию REST API и health check для инфраструктуры.
