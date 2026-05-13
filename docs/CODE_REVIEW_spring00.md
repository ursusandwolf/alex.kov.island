# Code Review: feature/spring00 (Spring Boot интеграция + Frontend)

**Reviewer:** Tech Lead / Staff Engineer  
**Date:** 2026-05-12  
**База:** v11 (dev)  
**Ветка:** `feature/spring00`  
**Масштаб:** полный Spring Boot 3.2.5 + React 18 + TypeScript + Vite  

---

## Общий прогресс

За один PR доставлено: Spring Boot интеграция, WebSocket/STOMP, REST API, frontend на React,
Jackson с Mixin, GitHub Actions CI, JaCoCo threshold поднят до 0.65, snapshot-история.
Это значительный объём работы, и большинство решений архитектурно правильные.

| Пункт | Статус |
|---|---|
| Spring Boot BOM через `<scope>import</scope>` | ✅ Правильно |
| `Jackson2ObjectMapperBuilderCustomizer` (не `@Bean ObjectMapper`) | ✅ Правильно |
| `@PreDestroy` + `AutoCloseable SimulationContext` | ✅ Правильно |
| `SimulationStartedEvent` — динамическое подключение broadcaster | ✅ Изящное решение |
| STOMP broadcast из `Phase.POSTPROCESS` ScheduledTask | ✅ Правильно |
| `module-info.java` с `opens` для Spring рефлексии | ✅ Правильно |
| `application.yml` с профилями `nature`/`simcity` | ✅ Правильно |
| `vite.config.ts` с proxy на Spring Boot 8080 | ✅ Правильно |
| GitHub Actions CI | ✅ **Наконец добавлен!** |
| JaCoCo threshold 0.65 | ✅ Повышен (было 0.50) |
| `node_modules` в `.gitignore` | ❌ **КРИТИЧНО** — не добавлен |
| `SimulationService.context` — volatile | ❌ Race condition |
| `SimulationBroadcaster.snapshotInterval` — volatile | ❌ Race condition |
| Input validation на `@RequestParam` | ❌ Нет `@Min`/`@Max` |
| `@ControllerAdvice` — глобальная обработка ошибок | ❌ Отсутствует |
| `SimulationControllerTest` — `@SpringBootTest` вместо `@WebMvcTest` | ❌ Тяжёлый тест |
| `colors.ts` — регистр имён не совпадает с Java | ❌ Баг рендеринга |
| Frontend CI | ❌ Не в GitHub Actions |

---

## 🔴 Критические проблемы

### 1. `node_modules` в репозитории

```
# .gitignore — ОТСУТСТВУЮТ строки:
node_modules/
island-ui/dist/
island-ui/node_modules/
```

В архиве находятся тысячи файлов npm-зависимостей. `node_modules` никогда не должен
попадать в git. Это раздувает репозиторий на сотни мегабайт, ломает `git clone` на медленных
соединениях и создаёт ложные изменения в PR.

```bash
# Немедленно добавить в .gitignore:
island-ui/node_modules/
island-ui/dist/

# Удалить из истории:
git rm -r --cached island-ui/node_modules
git rm -r --cached island-ui/dist
```

---

### 2. `SimulationService.context` — гонка между `start()` и остальными методами

```java
// SimulationService.java
private SimulationContext<?> context;  // ← обычное поле, не volatile

public synchronized void start(...) {
    // synchronized — context обновляется с lock
    this.context = new SimulationEngine().build(plugin, config);
}

public void pause() {
    if (context != null) {          // ← читается БЕЗ lock
        context.gameLoop().pause(); // ← возможна гонка
    }
}
```

`start()` — `synchronized`, но `pause()`, `resume()`, `stop()`, `getSnapshot()` — нет.
HTTP-поток, вызывающий `pause()`, может прочитать старое значение `context`, пока
другой поток выполняет `start()`. JMM не гарантирует видимость без `volatile` или `synchronized`.

```java
// Fix 1: volatile + volatile read везде
private volatile SimulationContext<?> context;

// Fix 2: synchronized на всех методах (проще, но блокирует HTTP-поток)
public synchronized void pause() { ... }
public synchronized WorldSnapshot getSnapshot() { ... }
```

`volatile` — правильный выбор: запись в `start()` (уже под synchronized) публикует
значение, остальные методы читают свежее значение без блокировки.

---

### 3. `SimulationBroadcaster.snapshotInterval` — видимость между потоками

```java
@Value("${sim.broadcast-interval:5}")
private int snapshotInterval;  // ← обычное int, не volatile

// Пишется из HTTP-потока (через setSnapshotInterval):
public void setSnapshotInterval(int interval) {
    this.snapshotInterval = Math.max(1, interval); // ← без volatile
}

// Читается из потока симуляции (ScheduledTask.tick):
if (tickCount % snapshotInterval != 0) return; // ← может видеть старое значение
```

```java
// Fix:
private volatile int snapshotInterval;
```

---

## 🟡 Spring-специфичные замечания

### 4. `SimulationControllerTest` — `@SpringBootTest` вместо `@WebMvcTest`

```java
@SpringBootTest          // ← поднимает ВЕСЬ контекст Spring + запускает симуляцию
@AutoConfigureMockMvc
@ActiveProfiles("nature")
class SimulationControllerTest { ... }
```

`@SpringBootTest` для теста контроллера — это как выехать на экскурсию на самосвале.
Поднимается полный контекст, стартует симуляция, тест медленный и хрупкий.

```java
// Правильно: тестируем только web-слой
@WebMvcTest(SimulationController.class)
class SimulationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean SimulationService simulationService;
    @MockBean SnapshotHistoryService historyService;

    @Test
    void pause_should_return_paused_status() throws Exception {
        when(simulationService.getStatus()).thenReturn(SimulationStatus.PAUSED);

        mockMvc.perform(post("/api/v1/simulation/pause"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(simulationService).pause();
    }
}
```

`@WebMvcTest` запускает только Spring MVC слой, без симуляции. Тесты в 10× быстрее.

---

### 5. Нет input validation на `@RequestParam`

```java
// SimulationController.java — сейчас
@PostMapping("/start")
public ResponseEntity<StatusResponse> start(
        @RequestParam(defaultValue = "nature") String type,  // любая строка
        @RequestParam(defaultValue = "20") int width,        // может быть 0 или 99999
        @RequestParam(defaultValue = "20") int height,
        @RequestParam(defaultValue = "100") int tickMs) {
```

`width=0` → Island создаётся с нулевой шириной → NPE или некорректное поведение.
`tickMs=1` → симуляция съедает CPU. `type="randomvalue"` → тихо создаётся Nature.

```java
// Правильно — Bean Validation:
@Validated  // ← на классе контроллера
@PostMapping("/start")
public ResponseEntity<StatusResponse> start(
        @RequestParam(defaultValue = "nature") String type,
        @RequestParam(defaultValue = "20") @Min(5) @Max(200) int width,
        @RequestParam(defaultValue = "20") @Min(5) @Max(200) int height,
        @RequestParam(defaultValue = "100") @Min(10) @Max(10000) int tickMs) {

// Добавить в pom.xml:
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### 6. Нет `@ControllerAdvice` — все ошибки → 500

```java
// Сейчас: если simulationService.start() бросает RuntimeException
// → Spring возвращает HTML error page или дефолтный JSON с stacktrace

// Правильно:
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
               .body(new ErrorResponse("Internal error"));
    }

    public record ErrorResponse(String message) {}
}
```

---

### 7. `SnapshotHistoryService` — `null` вместо `Optional`

```java
// Сейчас — вынуждает null-checks в вызывающем коде
public WorldSnapshot loadSnapshot(String filename) {
    ...
    return null;  // ← anti-pattern в современном Java/Spring
}

// Правильно:
public Optional<WorldSnapshot> loadSnapshot(String filename) {
    if (!Files.exists(path)) return Optional.empty();
    try {
        return Optional.of(objectMapper.readValue(path.toFile(), WorldSnapshot.class));
    } catch (IOException e) {
        log.error("Failed to deserialize snapshot", e);
        return Optional.empty();
    }
}

// SimulationController:
return historyService.loadSnapshot(filename)
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```

---

### 8. `SimulationService` — хардкоженный if/else для плагинов

```java
private void doStart(String type, ...) {
    SimulationPlugin<?> plugin;
    if ("simcity".equalsIgnoreCase(type)) {
        plugin = new SimCityPlugin(width, height, initialSnapshot);
    } else {
        ...
        plugin = new NaturePlugin(cfg, initialSnapshot);
    }
}
```

Добавление третьего плагина — изменение `SimulationService`. Нарушение OCP.
Spring-решение — реестр плагинов:

```java
// Интерфейс с именем плагина
public interface NamedSimulationPlugin<T extends Mortal> extends SimulationPlugin<T> {
    String getPluginName();
}

// Регистрация как @Component
@Component
public class NaturePluginFactory implements NamedSimulationPlugin<Organism> {
    @Override public String getPluginName() { return "nature"; }
    @Override public SimulationWorld<Organism> createWorld(EventBus bus) { ... }
}

@Component
public class SimCityPluginFactory implements NamedSimulationPlugin<SimEntity> {
    @Override public String getPluginName() { return "simcity"; }
}

// SimulationService — инжектирует все плагины автоматически
@Service
public class SimulationService {
    private final Map<String, NamedSimulationPlugin<?>> plugins;

    public SimulationService(List<NamedSimulationPlugin<?>> pluginList) {
        this.plugins = pluginList.stream()
            .collect(Collectors.toMap(NamedSimulationPlugin::getPluginName, p -> p));
    }

    private void doStart(String type, ...) {
        NamedSimulationPlugin<?> plugin = plugins.get(type.toLowerCase());
        if (plugin == null) throw new IllegalArgumentException("Unknown plugin: " + type);
        ...
    }
}
// Новый плагин = новый @Component. SimulationService не меняется.
```

---

### 9. `@Value("${spring.profiles.active:nature}")` — ненадёжный способ

```java
@Value("${spring.profiles.active:nature}")
private String defaultProfile;
```

`spring.profiles.active` может содержать несколько профилей через запятую: `nature,debug,local`.
Строка `"nature,debug"` не совпадёт с `"simcity"` в if/else.

```yaml
# application.yml — добавить отдельное свойство:
sim:
  default-plugin: nature
```

```java
@Value("${sim.default-plugin:nature}")
private String defaultPlugin;
```

---

### 10. CORS не настроен для REST API

`WebSocketConfig` имеет `setAllowedOriginPatterns("*")` для WebSocket (это ОК для dev).
Но REST API (`/api/**`) не имеет CORS-заголовков. С Vite proxy в dev это не заметно,
но в production (frontend на отдельном домене/порту) REST-запросы заблокирует браузер.

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${sim.cors.allowed-origins:http://localhost:5173}") String[] origins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(origins)
                    .allowedMethods("GET", "POST", "PATCH", "DELETE");
            }
        };
    }
}
```

```yaml
# application.yml production profile:
sim.cors.allowed-origins: https://island-simulator.example.com
```

---

## ⚛️ Frontend — замечания

### 11. `colors.ts` — регистр не совпадает с Java

```typescript
// colors.ts — ожидает заглавные буквы
if (code === 'Wolf' || code === 'Bear') return '#f44336';
if (code === 'Rabbit' || code === 'Deer') return '#2196f3';

// Java SpeciesKey — отправляет строчные
public static final SpeciesKey WOLF = register("wolf", true);  // code = "wolf"
```

`getSpeciesColor("wolf", false)` → не совпадёт ни с одним условием → вернёт `#9c27b0` (purple).
Все клетки с животными будут фиолетовыми.

```typescript
// Fix: нормализовать регистр
const normalizedCode = code.toLowerCase();
if (normalizedCode === 'wolf' || normalizedCode === 'bear' || normalizedCode === 'fox') {
    return '#f44336'; // red — predators
}
if (normalizedCode === 'rabbit' || normalizedCode === 'deer' || normalizedCode === 'caterpillar') {
    return '#2196f3'; // blue — herbivores
}
// SimCity
const cityColors: Record<string, string> = {
    road: '#78909c', residential: '#4caf50',
    commercial: '#2196f3', industrial: '#ffeb3b'
};
return cityColors[normalizedCode] ?? '#9c27b0';
```

---

### 12. Frontend CI — отсутствует в GitHub Actions

```yaml
# .github/workflows/ci.yml — добавить job:
  frontend-build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: island-ui
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm', cache-dependency-path: 'island-ui/package-lock.json' }
      - run: npm ci          # не npm install — строгая установка
      - run: npm run lint
      - run: npm run build
```

---

### 13. `useSimulationStore` — ошибки HTTP не проверяются

```typescript
// Сейчас — fetch() падает только при network error, не при 4xx/5xx
pause: async () => {
    await fetch('/api/v1/simulation/pause', { method: 'POST' });  // 500? не видно
    await get().updateStatus();
}

// Правильно: проверять response.ok
const apiPost = async (url: string) => {
    const res = await fetch(url, { method: 'POST' });
    if (!res.ok) {
        const error = await res.text();
        throw new Error(`API error ${res.status}: ${error}`);
    }
    return res.json();
};

pause: async () => {
    try {
        const data = await apiPost('/api/v1/simulation/pause');
        set({ status: data.status, error: null });
    } catch (err) {
        set({ error: String(err) });  // error отображается в UI
    }
}
```

---

### 14. `App.tsx` — монолитный компонент (~250 строк)

Стандартная практика React — один компонент = одна ответственность:

```
App.tsx              (layout + routing)
├── SimulationControls.tsx   (кнопки Start/Pause/Resume/Stop + конфиг)
├── WorldCanvas.tsx          (уже вынесен ✅)
├── MetricsPanel.tsx         (tick count, entities, metrics)
├── CellDetailsPanel.tsx     (выбранная ячейка)
├── HistoryPanel.tsx         (список снапшотов)
└── Legend.tsx               (легенда цветов)
```

---

### 15. Нет отображения `error` из store

```typescript
// useSimulationStore.ts — error поле есть
error: string | null;

// App.tsx — error нигде не рендерится
// Пользователь не видит "Connection lost" или "API error 500"
```

```tsx
// Добавить в App.tsx:
const { error } = useSimulationStore();
// ...
{error && (
    <div style={{ background: '#ffebee', color: '#c62828', padding: '10px', borderRadius: 4 }}>
        ⚠️ {error}
    </div>
)}
```

---

## ✅ Что сделано правильно — профессиональный стандарт

```
Spring Boot BOM через import scope          — не переопределяет parent pom ✅
Jackson2ObjectMapperBuilderCustomizer       — additive, не замена ✅
@PreDestroy + AutoCloseable                 — корректный lifecycle ✅
SimulationStartedEvent                      — loose coupling через events ✅
STOMP в POSTPROCESS фазе                   — симуляция управляет своим тредом ✅
module-info.java с opens для Spring         — JPMS + рефлексия ✅
Vite proxy на Spring Boot                  — zero CORS в dev ✅
GitHub Actions CI                          — наконец появился! ✅
Multi-profile application.yml              — чистое разделение конфигов ✅
WorldCanvas с Canvas API                   — производительный рендеринг ✅
Zustand store                              — правильный state management ✅
@stomp/stompjs + SockJS                    — стандарт Spring WebSocket ✅
TypeScript strict mode                     — noUnusedLocals, noUnusedParameters ✅
```

---

## 📊 Итоговые оценки

| Критерий | v10 | v11 (dev) | Spring PR |
|---|---|---|---|
| **Архитектура** | 9.5 | 9.5 | **9.5** |
| **Код** | 9.5 | 9.5 | **9.0** ↓ |
| **Spring integration** | — | 3.0 | **8.0** |
| **Production-ready** | 8.0 | 8.5 | **8.5** |
| **Frontend** | — | — | **6.5** |
| **Общая** | 9.5 | 9.5 | **9.0** |

*Код снижен из-за race conditions на `context`/`snapshotInterval` и отсутствия validation.*  
*Frontend 6.5: рабочий прототип, но не production-ready (цветовой баг, нет error UI, монолит).*

---

## Приоритеты до merge

| # | Задача | Сложность | Критично |
|---|---|---|---|
| 1 | Добавить `node_modules/` в `.gitignore` и удалить из индекса | 5 мин | 🔴 |
| 2 | `volatile SimulationContext<?> context` | 1 строка | 🔴 |
| 3 | `volatile int snapshotInterval` | 1 строка | 🔴 |
| 4 | Исправить регистр в `colors.ts` | 10 мин | 🔴 (визуальный баг) |
| 5 | `@WebMvcTest` вместо `@SpringBootTest` в `SimulationControllerTest` | 30 мин | 🟡 |
| 6 | `@Validated` + `@Min`/`@Max` на параметрах start() | 30 мин | 🟡 |
| 7 | `@RestControllerAdvice` GlobalExceptionHandler | 1 час | 🟡 |
| 8 | `Optional<WorldSnapshot>` в `SnapshotHistoryService` | 30 мин | 🟡 |
| 9 | Frontend CI job в `ci.yml` | 15 мин | 🟡 |
| 10 | `error` state отображается в `App.tsx` | 15 мин | 🟡 |
| 11 | HTTP error handling в store | 1 час | 🟡 |
| 12 | Plugin registry вместо if/else | 2 часа | 🟢 |
| 13 | CORS config для production | 30 мин | 🟢 |
| 14 | Разбить `App.tsx` на компоненты | 2 часа | 🟢 |
