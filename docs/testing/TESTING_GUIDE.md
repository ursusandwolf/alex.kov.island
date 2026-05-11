# Гид по тестированию

## Типы тестов в проекте

| Тип | Где | Когда писать | Примеры |
|---|---|---|---|
| **Unit** | `*/src/test/java` | Для каждого нового класса/логики | `AnimalHealthSystemTest`, `ECSTest` |
| **Boundary** | `*/src/test/java` | Для проверки крайних состояний (0, max, null) | `SimCityBoundaryTest` |
| **Integration** | `*/src/test/java` | Для взаимодействия нескольких систем | `EcosystemBalanceTest` |
| **Architecture** | `island-app/src/test/java` | При изменении структуры модулей | `ArchitectureTest` |

## Золотые правила

### 1. Тестировать через публичный API
Старайтесь не тестировать приватные поля или внутренние классы. Лучший тест — тот, который проверяет поведение через `SimulationEngine` или интерфейсы плагина.

### 2. @DisplayName на каждом тесте
Используйте понятные описания на английском языке, чтобы отчеты о тестах были читаемыми.
```java
@Test
@DisplayName("Animal should die when energy reaches zero")
void animal_dies_at_zero_energy() { ... }
```

### 3. Структура Arrange-Act-Assert (AAA)
```java
@Test
void wolf_loses_energy_on_move() {
    // Arrange (Подготовка)
    Animal wolf = animalFactory.create(SpeciesKey.WOLF, cell);
    long energyBefore = wolf.getCurrentEnergy();

    // Act (Действие)
    movementSystem.process(wolf, cell, 1);

    // Assert (Проверка)
    assertThat(wolf.getCurrentEnergy()).isLessThan(energyBefore);
}
```

## Инструменты
- **JUnit 5:** Базовый фреймворк для тестов.
- **AssertJ:** Для удобных и читаемых проверок (`assertThat`).
- **Mockito:** Для создания моков (используйте осторожно, предпочитайте реальные объекты для доменной логики).
- **ArchUnit:** Для контроля архитектурных правил и зависимостей.

## Запуск тестов
```bash
mvn test                              # Запустить все тесты
mvn test -pl island-engine            # Только тесты движка
mvn jacoco:report                     # Сгенерировать отчет о покрытии
```

Отчет о покрытии будет доступен в `target/site/jacoco/index.html` соответствующего модуля. Мы стремимся к покрытию критической логики не менее 80%.
