
---

## Code Review #4: Island dev — JPMS, SoA, API stability

### Что закрыто из Review #3 — все семь пунктов

| Замечание | Статус |
|---|---|
| Призрачное дерево `src/` — 83 дублирующих файла | ✅ Удалено |
| `ArchitectureTest` без главного правила `engine→nature` | ✅ Перенесён в `island-app`, 4 правила: `engine→domain`, `util→domain`, `nature↔simcity`, `plugins→internal` |
| `GameLoopOptimizationTest` в неправильном модуле | ✅ Перемещён в `island-engine` |
| Кэш `PhaseScheduler` через `List.equals()` | ✅ Заменён `tasksVersion` счётчиком |
| `DefaultWorkUnit` — 60 строк делегирования | ✅ Теперь `extends AbstractList` |
| `GameLoop.running` — TOCTOU | ✅ `AtomicBoolean` + `compareAndSet` |
| Python-скрипты в корне | ✅ Перенесены в `scripts/` |

Дополнительно появились: JPMS `module-info.java`, аннотации `@EngineAPI`/`@InternalEngine`, SoA-хранилища, расширение SimCity (загрязнение, электричество, агро), `SimCityBoundaryTest`. Темп и направление — правильные.

---

### 🔴 Критично

---

**1. `module-info.java` экспортирует `engine.internal` — весь слой защиты перечёркнут**

```java
// island-engine/src/main/java/module-info.java
exports com.island.engine.internal;  // ← нейтрализует весь замысел
```

Одновременно в `island-nature` используется то, что должно быть скрыто:

```java
// NatureDomainContextFactory.java, NatureDomainContext.java
import com.island.engine.internal.AgeSoAStore;
import com.island.engine.internal.EntityIdManager;
import com.island.engine.internal.HealthSoAStore;
```

И в `ArchitectureTest` стоит правило:
```java
noClasses().that().resideInAnyPackage("com.island.nature..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("com.island.engine.internal..")
```

Эти три факта несовместимы: `module-info` разрешает доступ → природа его использует → ArchUnit запрещает. Тест **падает**. Это не просто запах — это одновременно сломанная CI, нарушенный контракт `@InternalEngine` и нарушение JPMS-замысла.

**Как исправить.** SoA-хранилища — внутренняя оптимизация, но `nature` нужен доступ к ним через *публичный* контракт. Решение — фабричный метод в публичном API:

```java
// com.island.engine.core — PUBLIC
@EngineAPI
public interface ComponentStorage {
    /** Allocates a high-performance SoA store for health data. */
    static ComponentStorage createHealthStore(int capacity) {
        return new com.island.engine.internal.HealthSoAStore(capacity); // ← здесь внутренний класс OK
    }
    // + get/set методы через публичный интерфейс
}
```

Тогда `module-info` убирает `exports com.island.engine.internal`, `nature` работает с `ComponentStorage` из `engine.core`, ArchUnit не видит нарушений.

---

**2. SoA-хранилища выделены, но нигде не используются**

```java
// NatureDomainContextFactory.java
int capacity = config.getIslandWidth() * config.getIslandHeight() * 10;
return NatureDomainContext.builder()
    .entityIdManager(new EntityIdManager())
    .healthSoAStore(new HealthSoAStore(capacity))   // ← для 100×100: 1M записей
    .ageSoAStore(new AgeSoAStore(capacity))          // ← ещё 1M записей
    ...
```

Нет ни одного места в `island-nature` (кроме самого `NatureDomainContext`), где эти хранилища читаются или обновляются. Ни `AnimalHealthSystem`, ни `AnimalReproductionSystem`, ни `Organism` к ним не обращаются. Это незавершённая миграция: инфраструктура создана, интеграция не сделана.

При `100×100` острове выделяется `1_000_000 × (long[] + long[] + boolean[])` для `HealthSoAStore` + `1_000_000 × (int[] + int[])` для `AgeSoAStore` — порядка **~25 MB** при старте без какой-либо пользы.

Нужно либо завершить интеграцию (подключить к `AnimalHealthSystem` / `AnimalReproductionSystem`), либо временно убрать из `NatureDomainContext` с TODO в changelog.

---

### 🟡 Стоит улучшить

---

**3. `SimulationConfig.defaultFor()` — логика `executionMode` перевёрнута**

```java
public static SimulationConfig defaultFor(int threadCount) {
    return SimulationConfig.builder()
            .executionMode(threadCount > 0 ? ExecutionMode.SEQUENTIAL   // ← ???
                                           : ExecutionMode.PARALLEL)    // ← ???
```

`threadCount > 0` → SEQUENTIAL, `threadCount == 0` → PARALLEL. Это инверсия смысла: виртуальные потоки (0) не делают задачи параллельными в ECS-смысле. Вероятно, должно быть наоборот. Это тихий баг: метод используется в тестах и приложении.

---

**4. `EconomySystem.process()` бросает `UnsupportedOperationException` в production-коде**

```java
protected void process(SimEntity entity, CityTile tile, int tickCount) {
    throw new UnsupportedOperationException("EconomySystem logic not yet implemented");
}
```

`UnsupportedOperationException` — правильный выбор вместо тихого no-op. Но если `EconomySystem` зарегистрирован в `SimCityPlugin`, любой запуск SimCity через `doProcessCell` упадёт. Нужно или реализовать (логика в `EconomyService` уже есть), или не регистрировать систему до реализации, или добавить guard-условие в `AbstractSimCitySystem`.

---

### 🟢 Мелочи

- `scripts/` не в `.gitignore` — 13 Python-скриптов коммитятся как код проекта. Для долгосрочного проекта это шум в истории; стоит добавить `scripts/` в `.gitignore` или перенести в отдельную ветку `tooling/`
- SoA capacity = `width * height * 10` — магический множитель `10` без комментария. Откуда взялось это число? Стоит документировать: `"10 — worst-case entities per cell based on species.properties max_count"`

---

## Сравнение с профессиональным стандартом

Теперь к вопросу об уровне проекта. Для сравнения возьмём реальные референсные проекты со схожей областью: **Artemis-ODB** (Java ECS фреймворк), **libGDX** (Java игровой фреймворк), **Unity DOTS** (ECS для игр), **Bevy** (Rust ECS).

### Что уже на профессиональном уровне

**Архитектурные слои и изоляция** — четырёхмодульная Maven-структура с однонаправленными зависимостями, `module-info.java`, ArchUnit-тесты на слои — это то, что делают Artemis-ODB и Spring Framework. Многие коммерческие проекты этого не имеют.

**ECS-модель** — `EntitySystem` с декларированными read/write компонентами, `SystemExecutionGraph` для параллельного расписания, `ArrayComponentStore` для плотного хранения — точная копия подхода Artemis-ODB и Unity Burst/Jobs. Это не учебный уровень, это production ECS.

**Параллельная обработка** — chunk-based параллелизм с `WorkUnit`, `DynamicChunkingStrategy`, телеметрия исполнения через `setLastExecutionTimeNanos` — есть аналоги в Minecraft-подобных движках (Folia, Minestom).

**Plugin-система** — `SimulationPlugin<T>` с lifecycle hooks — архитектурно близко к Eclipse Plugin API и libGDX `ApplicationAdapter`. Два независимых домена (`island-nature`, `island-simcity`) на одном движке — хорошее доказательство обобщённости.

**@EngineAPI / @InternalEngine + ArchUnit** — аналог `@Stable`/`@Unstable` в Apache Commons и `@Internal` в Guava. Немногие open-source проекты имеют автоматическую проверку через ArchUnit.

---

### Что отделяет от production-уровня

Ниже — конкретные gap-ы с примерами из реальных проектов и тем, что нужно сделать.

**1. SemVer + API-стабильность без инструментов**

В Artemis-ODB и libGDX `@Stable` + отдельный `BREAKING_CHANGES.md` автоматически проверяется через `Revapi` (Maven plugin для бинарной совместимости). Changelog есть, но нет:
- Разделения на `@EngineAPI(since = "1.31", until = "?")`
- Машинной проверки: "эта версия сломала публичный API?"

Добавить в `island-engine/pom.xml`:
```xml
<plugin>
    <groupId>org.revapi</groupId>
    <artifactId>revapi-maven-plugin</artifactId>
    <version>0.15.0</version>
    <!-- сравнивает текущий jar с предыдущим, ловит случайные API-breaking changes -->
</plugin>
```

**2. Нет JMH-бенчмарков для горячих путей**

В Unity DOTS и Minestom каждое изменение hot-path сопровождается бенчмарком. Сейчас `PhaseScheduler`, `SystemExecutionGraph`, `DynamicChunkingStrategy` оптимизировались «на глаз» по changelog. Нет числа — нет регрессии.

Минимальный модуль `island-benchmarks`:
```java
@Benchmark
public void phaseSchedulerThroughput(Blackhole bh) {
    gameLoop.runTick();
    bh.consume(gameLoop.getTickCount());
}
```

**3. Нет интеграционного теста экосистемного баланса**

У каждого симулятора (NetLogo, Mesa, MASON) есть «balance test»: запустить N тиков, проверить, что ни один вид не вымер за первые 100 тиков при дефолтной конфигурации. Сейчас есть `SimulationStopConditionTest`, но нет теста, проверяющего что симуляция *не* останавливается раньше времени. Любое изменение `DefaultClimateService` или `AnimalHealthSystem` может неожиданно убить волков без единого падающего теста.

```java
@Test
void ecosystemShouldSurviveMinimumTicks() {
    // 500 тиков без вымирания = экосистема стабильна
    assertThat(islandAfter(500).getSpeciesCounts())
        .allSatisfy((species, count) -> assertThat(count).isGreaterThan(0));
}
```

**4. Javadoc неполный на публичном API**

В libGDX и Artemis-ODB каждый `@EngineAPI`-класс имеет Javadoc с примером использования. Сейчас у `SimulationEngine`, `SimulationPlugin`, `GameLoop` есть Javadoc, но у `WorkUnit`, `SimulationNode`, `EntitySystem`, `ComponentRegistry` — только частично или нет.

Добавить в `island-engine/pom.xml` в CI:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    <configuration>
        <failOnWarnings>true</failOnWarnings>
        <excludePackageNames>com.island.engine.internal</excludePackageNames>
    </configuration>
</plugin>
```

**5. Нет CI конфигурации**

В репозитории нет `.github/workflows/` или `Jenkinsfile`. Это значит, что ArchUnit-правила, которые сейчас нарушены (`pluginsShouldNotUseEngineInternals`), никогда не запускаются автоматически. Минимальный GitHub Actions:

```yaml
# .github/workflows/ci.yml
- run: mvn verify -pl island-engine,island-nature,island-simcity,island-app
```

---

### Приоритеты на следующий спринт

| Приоритет | Задача | Почему |
|---|---|---|
| 1 | Убрать `exports engine.internal` из `module-info`, вынести SoA за публичный интерфейс | Текущее состояние ломает ArchUnit CI |
| 2 | Завершить интеграцию SoA или убрать из `NatureDomainContext` | ~25 MB мёртвой памяти |
| 3 | Исправить `ExecutionMode` в `SimulationConfig.defaultFor()` | Тихий логический баг |
| 4 | Добавить `.github/workflows/ci.yml` с `mvn verify` | Без CI ArchUnit-правила не работают |
| 5 | Написать `EcosystemBalanceTest` (500 тиков без вымирания) | Защита от тихих регрессий в балансе |
| 6 | JMH-модуль для `PhaseScheduler` и `SystemExecutionGraph` | Числовое подтверждение оптимизаций |