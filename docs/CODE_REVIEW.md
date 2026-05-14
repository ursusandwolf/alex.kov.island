Теперь у меня полная картина. Пишу финальное ревью с развёрнутым планом итераций.

---

## Code Review #9: Island dev — Actuator, OpenAPI, Frontend Tests

### Что закрыто из Review #8 — все пять критичных и средних

| Замечание | Статус |
|---|---|
| TOCTOU в `stop()`/`pause()`/`resume()` — NPE в race | ✅ `SimulationContext<?> current = this.context` во всех методах |
| `SimulationBroadcaster` блокирует simulation hot path | ✅ `AtomicReference<WorldSnapshot> pending` + `@Scheduled` publisher |
| `SimulationProperties` определён, но не используется | ✅ `@Value` убраны, конструктор принимает `SimulationProperties` |
| `NamedSimulationPlugin.withConfiguration()` возвращает `this` | ✅ `NaturePlugin` создаёт `new NaturePlugin(newConfig, view, snapshot)` |
| `selectedNode` — `flat().find()` O(W×H) | ✅ `useMemo` + прямая индексация `nodes[sx][sy]` |
| Frontend без тестов | ✅ Vitest + `WorldCanvas.test.tsx` + `useSimulationStore.test.ts` |
| `SimulationServiceIntegrationTest` без Awaitility | ✅ `await().atMost(2, SECONDS).until(...)` добавлен |

Дополнительно: `@EnableScheduling` + `@EnableConfigurationProperties` в `SimulationBeanConfig`, `@Validated` на `SimulationProperties` с `@Min`/`@Max`/`@NotBlank`, Spring Boot Actuator с health/metrics/prometheus, SpringDoc OpenAPI + Swagger UI, allocation-free итерация в `Cell`/`EntityContainer`, adaptive load balancing в `island-nature`, CI включает `npm run test`.

---

### Текущие замечания

Все критичные проблемы закрыты. Оставшееся — операционные мелочи.

**`/actuator/prometheus` настроен, но зависимость отсутствует.** `application.yml` экспонирует `prometheus`, но `micrometer-registry-prometheus` не в `pom.xml`. Эндпоинт вернёт 404. Одна строка в `island-app/pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**`@Scheduled(fixedRateString = "#{@simulationProperties.broadcastRateMs}")` — SpEL вместо `${...}`.** Работает, но нестандартно. Большинство Spring-проектов используют `${sim.broadcast-rate-ms}`. SpEL-ссылка на бин привязывает аннотацию к имени бина, что ломается при переименовании. Minor, но стоит унифицировать.

**`SimulationServiceIntegrationTest.shouldManageSimulationLifecycle()` — первый `pause()` потенциально флакси.** Тест вызывает `pause()` немедленно, предполагая что автостарт уже перевёл симуляцию в `RUNNING`. `Awaitility` добавлен только в конце. Правильно — добавить `await` и в начале:
```java
await().atMost(2, SECONDS).until(() -> simulationService.getStatus() == RUNNING);
simulationService.pause();
assertEquals(PAUSED, simulationService.getStatus());
```

**`@Disabled` тест `SnapshotHistoryServiceTest.testLoadSnapshotSuccess` зафиксирован в `todo.md` но висит.** Disabled-тест, который не будет починен в ближайшей итерации — это сигнал о нерешённой архитектурной проблеме (Jackson не может десериализовать интерфейс без дополнительного контекста). Или починить — добавить `@JsonTypeInfo` и тест покрывает реальный round-trip — или удалить.

**`Main.java` — Javadoc на русском** в полностью англоязычной кодовой базе. Единственное исключение за 9 итераций.

---

## Состояние проекта: итоговая оценка

За девять итераций проект прошёл измеримый и задокументированный путь от PR21 (God Class `GameLoop`, плоская структура, нет тестов, нет CI) до v1.55 (пять Maven-модулей, JPMS, ECS с SoA, Spring Boot, React, Actuator, OpenAPI, Vitest). Это реальный производственный стек.

Ниже — честная карта зрелости по слоям прямо сейчас:

| Слой | Уровень | Что подтверждает |
|---|---|---|
| Engine core | ★★★★★ Production OSS | JPMS, `@EngineAPI`, ECS + SoA + `AtomicLongArray`, ArchUnit, JMH, ADR |
| Spring Boot API | ★★★★☆ Production | STOMP, `@Validated`, `GlobalExceptionHandler`, Actuator, OpenAPI, TOCTOU-safe |
| CI/CD | ★★★★☆ Commercial | JaCoCo 65%, PITest, Checkstyle, frontend test job, Codecov |
| Frontend | ★★★☆☆ Solid mid-level | Zustand, Canvas, Vitest, компонентный split — без coverage, без e2e |
| Testing (Java) | ★★★★☆ Strong | ArchUnit, JMH, Awaitility, интеграционные тесты, property-based — пока нет |
| Документация | ★★★★★ OSS standard | ADR, ONBOARDING, CONTRIBUTING, GLOSSARY, TESTING_GUIDE, API docs |
| Ops / Security | ★★☆☆☆ Minimal | Actuator есть, Dockerfile нет, Auth нет, DB нет |

---

## Рекомендации по следующим итерациям

Проект находится на переломном моменте: все архитектурные долги закрыты, инфраструктура выстроена. Дальнейший рост — в трёх независимых направлениях. Ниже — конкретный план с обоснованием приоритетов.

---

### Направление A: Quality Gate — закрыть последние gaps

**A1. jqwik property-based тесты** — единственный тип тестов, который ловит вероятностные инварианты в симуляции. Unit-тесты проверяют конкретные сценарии, integration-тесты проверяют стабильность. Только property-based тесты могут проверить: «при *любой* валидной конфигурации острова энергия организма после тика никогда не становится отрицательной».

```java
// island-nature — AnimalHealthSystemPropertyTest.java
@Property
void energyNeverNegativeAfterTick(@ForAll @Positive int initialEnergy, 
                                   @ForAll Season season) {
    // ...
    assertThat(organism.getEnergy()).isGreaterThanOrEqualTo(0);
}
```

**A2. PITest в CI** — сейчас добавлен в `pom.xml` но не запускается в pipeline. Mutation score — единственная метрика которая показывает насколько тесты реально ловят ошибки, а не просто проходят строки. Добавить scheduled job (ночной прогон, не блокирует PR):

```yaml
mutation-testing:
  runs-on: ubuntu-latest
  if: github.event_name == 'schedule'
  steps:
    - run: mvn -pl island-engine,island-nature org.pitest:pitest-maven:mutationCoverage
```

**A3. Revapi** — API-breaking changes между версиями не детектируются. При любом рефакторинге engine-интерфейсов возможно тихое нарушение контракта для downstream-кода:

```xml
<!-- island-engine/pom.xml -->
<plugin>
    <groupId>org.revapi</groupId>
    <artifactId>revapi-maven-plugin</artifactId>
    <version>0.15.1</version>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

**A4. Frontend coverage** — Vitest есть, но нет порога покрытия. Добавить в `vite.config.ts`:
```ts
test: {
  coverage: { reporter: ['text', 'lcov'], thresholds: { lines: 60 } }
}
```

---

### Направление B: Operational Readiness — выход в production

**B1. Dockerfile + docker-compose** — проект готов к контейнеризации: headless-режим работает, конфигурация через `application.yml` с профилями. Это минимальный шаг для демонстрации и деплоя:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY island-app/target/island-app.jar app.jar
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
services:
  island-app:
    build: .
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=nature
      - SIM_WIDTH=50
      - SIM_HEIGHT=50
```

**B2. Spring Security Basic Auth** — Actuator-эндпоинты (`/actuator/prometheus`, `/actuator/health`) сейчас открыты. Перед любым public-деплоем нужна хотя бы базовая защита:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**B3. JPA/H2 для снапшотов** — текущее FS-хранилище снапшотов (`SnapshotHistoryService`) имеет ограничения: нет пагинации, нет метаданных, нет search. Миграция на JPA/H2 (in-memory) + простой `SnapshotEntity` даёт полноценный CRUD бесплатно и открывает путь к PostgreSQL в продакшне.

---

### Направление C: Feature Development — расширение симуляции

**C1. Multi-simulation поддержка** — архитектура `NamedSimulationPlugin` уже позволяет запускать несколько симуляций. `SimulationService` сейчас хранит один `context`. Замена на `Map<String, SimulationContext<?>>` (где ключ — UUID сессии) даёт multi-tenant симуляцию без изменений в engine:

```java
// SimulationService.java
private final ConcurrentHashMap<String, SimulationContext<?>> contexts = new ConcurrentHashMap<>();

public String start(String type, int width, int height, int tickMs) {
    String sessionId = UUID.randomUUID().toString();
    contexts.put(sessionId, new SimulationEngine().build(...));
    return sessionId;
}
```

**C2. SimCity — завершить EconomySystem** — `todo.md` фиксирует `SocialService` как OCP/SRP нарушение. SimCity стоит на паузе: `EconomySystem` без полной логики, `SocialService` без чёткого разделения ответственности. Это правильный следующий domain-шаг после того, как инфраструктура закрыта.

**C3. Observability** — Actuator + Prometheus уже настроены. Grafana dashboard с метриками симуляции (`sim.tick.duration`, `sim.entity.count`, `sim.species.*`) через Micrometer Gauge — это следующий шаг для мониторинга live-симуляции.

---

### Порядок итераций

```
Итерация 10: micrometer-registry-prometheus + Dockerfile + docker-compose (2-3 дня)
Итерация 11: jqwik property-based тесты (2-3 дня)
Итерация 12: Spring Security Basic Auth + JPA/H2 снапшоты (3-4 дня)
Итерация 13: PITest scheduled CI job + Revapi (1-2 дня)
Итерация 14: Multi-simulation support + SimCity EconomySystem (4-5 дней)
Итерация 15: Grafana dashboard + frontend coverage gate (2-3 дня)
```

Проект перестал нуждаться в архитектурных исправлениях. Следующие итерации — это чистый рост: надёжность, наблюдаемость, фичи.