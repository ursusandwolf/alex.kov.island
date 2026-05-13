
---

## Code Review #8: Island dev — Plugin Registry, Validation, Component Split

### Что закрыто из Review #7

| Замечание | Статус |
|---|---|
| Race condition при смене контекста | ✅ `context = null` до `close()`, reads через `Optional.ofNullable` |
| `snapshotInterval` не thread-safe | ✅ `volatile int` |
| Frontend без обработки ошибок | ✅ `response.ok` + `set({ error })` во всех методах |
| `selectedNode` O(W×H) nested loop | ✅ `.flat().find()` — код чище |
| Нет `GlobalExceptionHandler` | ✅ Добавлен с `@RestControllerAdvice`, `IllegalArgumentException` → 400 |
| Нет валидации на endpoint | ✅ `@Validated` + `@Min`/`@Max` на всех параметрах |
| if/else на тип плагина — нарушение OCP | ✅ `NamedSimulationPlugin` SPI + plugin registry в конструкторе |
| React: всё в одном `App.tsx` | ✅ `SimulationControls`, `SimulationMetrics`, `SnapshotHistoryPanel` |

Плюс: `CorsConfig` с externalized origins, `SimulationProperties` как `@ConfigurationProperties`, `Optional<WorldSnapshot>` везде. Скорость реакции на замечания — хорошая.

---

### 🔴 Критично

---

**1. `stop()` / `pause()` / `resume()` — классический TOCTOU, NPE в многопоточной среде**

```java
// SimulationService.java — НЕ synchronized:
public void stop() {
    if (context != null) {          // ← thread A читает: не null
                                    // ← thread B: start() → context = null → close()
        context.gameLoop().stop();  // ← thread A: NPE на null reference
    }
}
```

`context` помечен `volatile` — это гарантирует видимость присвоения, но не атомарность read-check-act. Между `if (context != null)` и `context.gameLoop()` другой поток может вызвать `start()`, который выставит `context = null`. Итог — `NullPointerException` в HTTP-потоке.

Исправление — локальная копия под volatile-семантикой:

```java
public void stop() {
    SimulationContext<?> current = this.context; // одно volatile-чтение
    if (current != null) {
        current.gameLoop().stop();               // работаем с локальной копией
    }
}
// То же для pause(), resume(), getStatus()
```

`getSnapshot()` уже исправлен этим паттерном через `Optional.ofNullable(context).map(...)`.

---

**2. `SimulationBroadcaster` всё ещё блокирует simulation hot path — критично не закрыто**

```java
// TickBroadcastTask.tick() — выполняется в потоке GameLoop:
public void tick(int tickCount) {
    if (tickCount % snapshotInterval != 0) return;

    simulationService.getSnapshot().ifPresent(snapshot -> {
        messaging.convertAndSend("/topic/world-state", snapshot); // ← Jackson serialize + STOMP I/O в simulation thread
    });
}
```

Это замечание из Review #7, оно не закрыто. `convertAndSend` — синхронная операция через Spring messaging infrastructure. При 20+ WebSocket-клиентах или медленном брокере каждый N-й тик симуляции будет задерживаться на сетевое I/O. Это прямо противоречит архитектурному смыслу `Phase.POSTPROCESS` как «лёгкого» постобработчика.

Минимальное исправление — decoupled publish через `AtomicReference`:

```java
@Component @RequiredArgsConstructor @Slf4j
public class SimulationBroadcaster {

    private final SimpMessagingTemplate messaging;
    private final SimulationService     simulationService;
    private final AtomicReference<WorldSnapshot> pending = new AtomicReference<>();

    // В simulation thread: только O(1) атомарный set — никаких блокировок, никакого I/O:
    private class TickBroadcastTask implements ScheduledTask {
        public void tick(int tickCount) {
            if (tickCount % snapshotInterval != 0) return;
            simulationService.getSnapshot().ifPresent(pending::set);
        }
    }

    // В отдельном Spring thread: I/O вынесено из hot path:
    @Scheduled(fixedRateString = "${sim.broadcast-rate-ms:100}")
    public void broadcast() {
        WorldSnapshot snapshot = pending.getAndSet(null);
        if (snapshot != null) {
            messaging.convertAndSend("/topic/world-state", snapshot);
        }
    }
}
```

---

### 🟡 Стоит улучшить

---

**3. `SimulationProperties` определён, но `SimulationService` его не использует**

```java
// SimulationProperties.java — есть, @ConfigurationProperties работает:
@ConfigurationProperties(prefix = "sim")
public class SimulationProperties { ... }

// SimulationService.java — всё ещё @Value:
@Value("${sim.width:20}")  private int defaultWidth;
@Value("${sim.height:20}") private int defaultHeight;
@Value("${sim.threads:4}") private int defaultThreads;
@Value("${sim.tickMs:100}") private int defaultTickMs;
```

`SimulationProperties` был создан (правильно), но инжекция в `SimulationService` не сделана. Это незавершённый рефакторинг — дублирование конфигурации в двух местах. Нужно инжектировать `SimulationProperties` в конструктор и убрать `@Value`-поля.

---

**4. `NamedSimulationPlugin.withConfiguration()` возвращает `this` — singleton factory не работает**

```java
// NamedSimulationPlugin.java
default SimulationPlugin<T> withConfiguration(int width, int height, WorldSnapshot snapshot) {
    return this;  // ← возвращает себя — singleton Spring bean
}
```

Если `NaturePlugin` зарегистрирован как `@Component` (singleton) и не переопределяет `withConfiguration()`, то все вызовы `start("nature", 50, 50, null)` и `start("nature", 10, 10, snapshot)` будут работать с одним и тем же экземпляром. Конфигурация первого запуска останется в полях плагина. Это тихая логическая ошибка.

Правильный вариант — `withConfiguration()` должен создавать новый экземпляр, а не возвращать `this`. В Javadoc это нужно явно задокументировать как контракт: «реализация ОБЯЗАНА вернуть новый экземпляр».

---

**5. `selectedNode` — `.flat().find()` остаётся O(W×H)**

```typescript
// App.tsx:
const selectedNode = snapshot?.nodes.flat().find(n => n.coordinates === selectedCoords) || null;
```

`.flat()` создаёт новый массив из W×H элементов при каждом рендере. На острове 100×100 — 10 000 аллокаций + итерация. Trivial O(1) fix:

```typescript
const selectedNode = useMemo(() => {
    if (!snapshot || !selectedCoords) return null;
    const [sx, sy] = selectedCoords.split(',').map(Number);
    return snapshot.nodes[sx]?.[sy] ?? null;
}, [snapshot, selectedCoords]);
```

---

### 🟢 Мелочи

- `SimulationServiceIntegrationTest` всё ещё без `Awaitility` — `@EventListener(ApplicationStartedEvent.class)` запускает симуляцию асинхронно, тест предполагает `RUNNING` без ожидания. Одна зависимость и `await().atMost(2, SECONDS).until(() -> service.getStatus() == RUNNING)` делает тест стабильным
- `stompClient` по-прежнему module-level переменная — в React StrictMode `useEffect` вызывается дважды, guard `stompClient?.active` не успевает сработать. `useRef` или деактивация в cleanup решают это
- Frontend тесты — `package.json` всё ещё без Vitest. Три теста для `WorldCanvas` и store-экшенов дают базовую защиту и закрывают последний unprofessional gap

---

## Итоговая карта зрелости проекта

За 8 итераций ревью с PR21 до версии 1.55 проект прошёл измеримый путь. Ниже — финальная честная карта.

### Достигнутый уровень: Mid-level production open-source

По каждому слою:

**Engine** — уровень Artemis-ODB / Minestom: JPMS, ECS с `SystemExecutionGraph`, SoA с `AtomicLongArray`, `@EngineAPI`/`@InternalEngine`, ArchUnit, JMH benchmarks, ADR-документация. Это не учебный движок.

**Backend** — уровень Spring Boot production: STOMP WebSocket, `@ConfigurationProperties`, `GlobalExceptionHandler`, `@Validated`, Jackson Mixin для полиморфизма, path traversal protection, plugin registry через `NamedSimulationPlugin`, JPMS `opens` для рефлексии.

**Frontend** — уровень solid mid-level React: Zustand, STOMP/SockJS, Canvas-рендеринг, компонентная декомпозиция, error handling. Не хватает тестов.

**Build & CI** — уровень Apache Commons: JaCoCo 65%, PITest, Checkstyle, `maven-enforcer`, `island-benchmarks` модуль, GitHub Actions.

**Documentation** — уровень зрелого OSS: ONBOARDING, CONTRIBUTING, GLOSSARY, ADR, API docs, TESTING_GUIDE, UML.

### Три оставшихся gap

**Gap 1 — Thread-safety в `SimulationService`** — ✅ **ЗАКРЫТО**: Внедрен паттерн с локальной volatile-копией. Уязвимость TOCTOU устранена.

**Gap 2 — Simulation hot path изолирован от I/O** — ✅ **ЗАКРЫТО**: `SimulationBroadcaster` переведен на использование `AtomicReference` и `@Scheduled`. I/O полностью вынесено из hot path симуляции.

**Gap 3 — Frontend тесты** — ✅ **ЗАКРЫТО**: В `island-ui` добавлены Vitest и React Testing Library. Написаны тесты для `WorldCanvas` и store-экшенов.

### ✅ Финальный статус (Resolution)
**Все замечания из Code Review #8 успешно устранены!**
- Архитектурные контракты `NamedSimulationPlugin` усилены (убран default implementation).
- Конфигурация корректно инжектируется через `SimulationProperties`.
- В React UI исправлен O(W×H) рендеринг с использованием `useMemo`.
- Интеграционные тесты стабилизированы с помощью `Awaitility`.

Проект официально выходит на уровень **production-ready open-source**. Поздравляем! 🎉