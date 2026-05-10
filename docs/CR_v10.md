Теперь у меня полная картина. Пишу ревью.

---

## Code Review #5: Island dev — SoA интеграция, CI, Build infrastructure

### Что закрыто из Review #4 — все шесть пунктов

| Замечание | Статус |
|---|---|
| `module-info` экспортировал `engine.internal` — ArchUnit падал | ✅ Убран экспорт. Публичный слой: `HealthStorage`, `AgeStorage`, `MovementStorage`, `EntityIdProvider` в `engine.core` |
| SoA-хранилища выделены, но нигде не используются | ✅ Полностью интегрированы в `AnimalHealthSystem`, `AnimalFactory` |
| `SimulationConfig.defaultFor()` — инверсия логики | ✅ Исправлено: `threadCount > 0 → PARALLEL` |
| `EconomySystem` бросает `UnsupportedOperationException` | ✅ Заменён `TODO`-комментарием, не падает в runtime |
| CI отсутствует | ✅ `.github/workflows/ci.yml` с `mvn verify` + codecov |
| `EcosystemBalanceTest` не написан | ✅ Добавлен 500-тиковый интеграционный тест |

Дополнительно в этой версии: `BooleanSupplier` вместо `Supplier<Boolean>` (нет boxing), `volatile` на полях `GameLoop`, JaCoCo с порогом 60%, PITest, JMH, `maven-enforcer-plugin`, `maven-source-plugin`, `maven-javadoc-plugin`, `provides`/`uses` в `module-info.java` для ServiceLoader, фикс дедлока `StampedLock` в `Island.moveOrganism`. Это серьёзный шаг в сторону production-уровня.

---

### 🔴 Критично

---

**1. `volatile long[]` — неправильный способ обеспечить thread-safety элементов массива**

Changelog v1.38 говорит: «Fixed critical race conditions in `HealthSoAStore` and `AgeSoAStore` by making array fields `volatile`». Это распространённая ошибка в Java concurrency: `volatile` на **ссылке** к массиву гарантирует видимость самой ссылки, но **не** видимость отдельных элементов.

```java
// HealthSoAStore.java — как ЕСТЬ:
private volatile long[] currentEnergy;

// Поток A: healthStorage.setCurrentEnergy(id, nextEnergy)  →  currentEnergy[id] = value
// Поток B: healthStorage.getCurrentEnergy(id)              →  currentEnergy[id] // может видеть stale!
```

JMM гарантирует: если поток B видит ту же ссылку на массив, что и поток A, — элементы массива всё равно могут быть устаревшими. Это false security: гонка не устранена, она просто стала менее воспроизводимой.

Три корректных пути:

```java
// Вариант A — AtomicLongArray (рекомендуется, минимальные изменения):
private final AtomicLongArray currentEnergy;
// currentEnergy.set(id, value) / currentEnergy.get(id) — гарантированная visibility

// Вариант B — VarHandle (Java 9+, максимальный контроль над memory semantics):
private long[] currentEnergy;
private static final VarHandle ENERGY_HANDLE = 
    MethodHandles.arrayElementVarHandle(long[].class);
// ENERGY_HANDLE.setVolatile(currentEnergy, id, value)

// Вариант C — synchronized на методах чтения/записи (проще, медленнее):
public synchronized void setCurrentEnergy(int id, long value) { ... }
```

Для системы с тысячами entity в параллельных потоках — `AtomicLongArray` / `AtomicIntegerArray` — стандартный production-подход.

---

### 🟡 Стоит улучшить

---

**2. `EcosystemBalanceTest` — нет фиксированного seed, тест флакси**

```java
// EcosystemBalanceTest.java
Configuration config = new Configuration(); // ← seed не задан
NaturePlugin plugin = new NaturePlugin(config);
// 500 тиков с рандомным размещением, рандомными встречами
```

500-тиковый тест на `10×10` острове — это ~50 000 клеточных взаимодействий. При неудачном начальном распределении (все хищники в одном углу, вся добыча в другом) тест упадёт не из-за регрессии в коде, а из-за Random seed. В CI это таймаут или sporadically failing test.

```java
// Добавить в Configuration:
public static Configuration deterministicTest() {
    Configuration c = new Configuration();
    c.setIslandWidth(10);
    c.setIslandHeight(10);
    c.setHeadless(true);
    c.setRandomSeed(42L);   // ← фиксированный seed
    return c;
}
```

Также проверить, что `DefaultRandomProvider` принимает seed и передаёт его в `NatureDomainContextFactory`. Тест с фиксированным seed детерминирован — если упал, значит регрессия в коде.

---

**3. `maven-javadoc-plugin` с `<doclint>none</doclint>` обнуляет свою ценность**

```xml
<!-- pom.xml -->
<artifactId>maven-javadoc-plugin</artifactId>
<configuration>
    <doclint>none</doclint>  <!-- ← все предупреждения отключены -->
    <quiet>true</quiet>
</configuration>
```

Плагин добавлен, но `doclint>none` означает: незадокументированные `@EngineAPI`-классы, неправильные `@param`/`@return` — всё проходит без предупреждений. Смысл плагина — обеспечить качество публичного API документации. Сейчас он генерирует Javadoc, но не контролирует его качество.

Для `@EngineAPI`-классов минимально оправданный уровень:

```xml
<configuration>
    <doclint>reference,syntax</doclint>  <!-- ловит битые ссылки и синтаксис -->
    <failOnWarnings>false</failOnWarnings>
    <excludePackageNames>com.island.engine.internal</excludePackageNames>
</configuration>
```

`all` — слишком строго для текущей стадии, `none` — бессмысленно. `reference,syntax` — разумный баланс.

---

**4. JMH и PITest добавлены в `pom.xml`, но не привязаны к CI-фазе**

CI запускает `mvn verify`. JMH-бенчмарки и PITest запускаются отдельными командами (`mvn jmh:benchmark`, `mvn org.pitest:pitest-maven:mutationCoverage`). Если они не привязаны к lifecycle через `<execution>` с `<phase>verify</phase>` — в CI они не запустятся.

Для PITest это особенно важно: mutation score — ценная метрика для TDD-проекта. Рекомендуется добавить в CI отдельный job (mutation testing долгий, не стоит блокировать main build):

```yaml
# .github/workflows/ci.yml — добавить:
  mutation-testing:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: mvn -pl island-engine,island-nature org.pitest:pitest-maven:mutationCoverage
```

---

### 🟢 Мелочи

- `scripts/` по-прежнему не в `.gitignore` — это пятый ревью подряд. Одна строка: `scripts/`
- `AnimalHealthSystem` — magic numbers `12000`, `15000`, `13000` для температурных модификаторов метаболизма. В `Configuration` или `SimulationConstants` уже есть паттерн для этого — они там и должны быть
- `maven-javadoc-plugin` привязан к build, но JaCoCo `check` goal не имеет явного `minimum` в читаемом виде — стоит проверить что порог 60% действительно применён, а не просто объявлен

---

## Где проект относительно профессионального стандарта сейчас

За пять итераций ревью проект прошёл путь от «учебный проект с God Class» до структуры, которую не стыдно показать на собеседовании Senior Java Engineer. Ниже — честное сравнение с реальными референсными проектами.

### Что уже на уровне production open-source

**Архитектурная дисциплина** сопоставима с Artemis-ODB и libGDX: JPMS модули, ArchUnit правила, публичный API через интерфейсы с `@EngineAPI`, внутренние детали скрыты за `@InternalEngine`. У многих коммерческих проектов этого нет.

**ECS-реализация** технически корректна и близка к Artemis-ODB: `EntitySystem` с декларированными `readComponents`/`writeComponents`, `SystemExecutionGraph` для параллельного расписания, `ArrayComponentStore` для плотного хранения, SoA-хранилища для hot-path компонентов. Это не учебный ECS.

**Build infrastructure** теперь сопоставима с зрелыми open-source проектами: JaCoCo + порог, PITest, JMH, enforcer, source + javadoc артефакты, CI на каждый push.

**Plugin-система** с `SimulationPlugin<T>`, lifecycle hooks, ServiceLoader-поддержкой — это enterprise-grade расширяемость. Два независимых домена на одном движке (Nature + SimCity) — живое доказательство работающей абстракции.

### Что ещё отделяет от уровня libGDX / Artemis-ODB

**Нет Revapi** (проверка бинарной совместимости API между версиями). Artemis-ODB и Spring Framework автоматически ловят случайные breaking changes. Без Revapi изменение сигнатуры `@EngineAPI`-метода тихо ломает downstream-код. Добавляется одним плагином в `island-engine/pom.xml`.

**Нет `island-benchmarks` модуля с JMH-тестами** как отдельного артефакта. JMH в engine-модуле — хорошо, но профессиональные проекты держат бенчмарки отдельно чтобы они не влияли на размер артефакта и dependency tree.

**Нет README с Quick Start** для нового контрибьютора. `README.md` существует, но не содержит: `git clone → mvn verify → java -jar → видишь симуляцию`. Это первое, что делают в любом open-source проекте.

**Нет property-based тестов** (jqwik или junit-quickcheck). Для симулятора с вероятностной логикой (кормление, размножение, охота) property-based тесты — стандарт: «для любого валидного острова после N тиков энергия не может быть отрицательной». Это находит edge cases, которые unit-тесты пропускают.

### Конкретные следующие шаги к профессиональному стандарту

| Шаг | Что даёт | Сложность |
|---|---|---|
| Исправить `volatile long[]` → `AtomicLongArray` | Устраняет реальную гонку данных | Средняя |
| Фиксированный seed в `EcosystemBalanceTest` | Детерминированный CI | Низкая |
| `doclint>reference,syntax` в javadoc-plugin | Реальный контроль API-документации | Низкая |
| PITest job в CI | Mutation score как метрика тестов | Средняя |
| `scripts/` в `.gitignore` | Чистота репозитория | Минуты |
| Revapi в `island-engine/pom.xml` | Защита от случайных API-breaking changes | Средняя |
| `island-benchmarks` модуль | Изолированные JMH-тесты | Средняя |
| README Quick Start | Входной порог для нового разработчика | Низкая |
| jqwik property-based тесты | Покрытие вероятностной логики | Высокая |