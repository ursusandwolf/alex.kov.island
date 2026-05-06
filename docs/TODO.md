# TODO: Архитектурные направления развития

> Документ описывает приоритетные задачи и стратегические направления развития
> симуляционного движка. Организован по горизонтам планирования.
> Текущая версия движка: **1.16.0**

---

## 🔴 HOTFIX — Блокирующие баги (до следующего релиза)

### HF-1: Устранить двойную отчётность смертей [COMPLETED]
**Файлы:** `FeedingService.java`, `MovementService.java`, `ReproductionService.java`

Три сервиса публиковали `EntityDiedEvent` напрямую, тогда как `Island.onEntityRemoved()`
публиковал второй раз при удалении из ячейки. Все счётчики смертей были завышены в 2 раза.

- [x] `FeedingService`: заменено на `a.die(EATEN_BY_PACK)` — прямой `publish()` удалён
- [x] `MovementService`: заменено на `animal.die(MOVEMENT_EXHAUSTION)` — прямой `publish()` удалён
- [x] `ReproductionService`: заменено на `parent.die(REPRODUCTION_EXHAUSTION)` — прямой `publish()` удалён
- [x] Добавлен тест `StatisticsDeathCountingTest`: убедились, что одна смерть = ровно одно событие в `StatisticsService`


---

## 🟡 SPRINT 1 — Техдолг и стабилизация ядра

### S1-1: Устранить `SpeciesKey` как глобальный синглтон [COMPLETED]
**Файлы:** `SpeciesKey.java`, `SpeciesLoader.java`, `SpeciesRegistry.java`

`private static final Map<String, SpeciesKey> REGISTRY` делает невозможным параллельный
запуск нескольких изолированных симуляций в одной JVM (например, параллельные тесты).

- [x] Убрать `static final` поля `WOLF`, `BEAR` и т.д. из `SpeciesKey` — оставить только data-класс с `code` и `isPredator`
- [x] Перенести реестр видов в `SpeciesRegistry` как instance-состояние
- [x] `SpeciesLoader.load()` возвращает `SpeciesRegistry` с полным набором `SpeciesKey`-экземпляров
- [x] Обновить все ссылки вида `SpeciesKey.WOLF` → получение через инжектированный `SpeciesRegistry`
- [x] Убедиться, что два параллельных теста с разными `SpeciesRegistry` не влияют друг на друга

### S1-2: Перевести `PhaseScheduler.parallelGroup` в локальную переменную [COMPLETED]
**Файл:** `PhaseScheduler.java`

Instance-field `parallelGroup` — рабочий буфер, не состояние объекта.
Скрытая зависимость от порядка вызовов; потенциальный источник багов при рефакторинге.

- [x] Объявить `parallelGroup` как `List<CellService>` внутри метода `execute()`
- [x] Убедиться, что тесты проходят без изменений

### S1-3: Добавить `shouldStop()` как полноценный механизм завершения [COMPLETED]
**Файл:** `SimulationPlugin.java`, `SimulationEngine.java`

Хук `shouldStop()` объявлен, но не вызывается в `GameLoop.run()`.
Симуляция не может завершиться по доменному условию (полное вымирание, достижение цели).

- [x] В `GameLoop.run()` добавить вызов `plugin.shouldStop(context)` после каждого `runTick()`
- [x] Реализовать `NaturePlugin.shouldStop()`: останавливать при полном вымирании всех животных
- [x] Добавить тест `SimulationStopConditionTest`: мир с одним животным → оно умирает → симуляция останавливается

### S1-4: Документация EventBus — type hierarchy как явный контракт [COMPLETED]
**Файл:** `EventBus.java`

Подписка на суперкласс (`Object.class`) работает как wildcard и получает все события.
Это не очевидно и не задокументировано.

- [x] Добавить Javadoc к `EventBus.subscribe()` с описанием type hierarchy
- [x] Добавить тест `EventBusTest.subscribingToObjectClassReceivesAllEvents()`
- [x] Рассмотреть добавление `publishAsync()` для неблокирующей публикации

---

## 🟢 SPRINT 2 — ECS: от задатков к полноценному слою

### S2-1: Ввести `ComponentStore<C>` — типизированное хранилище компонентов [COMPLETED]
**Новый файл:** `engine/ecs/ComponentStore.java`

`Organism` хранит компоненты в `ConcurrentHashMap<Class, Component>`. При тысячах
организмов это дорого: boxing, cache miss, lock-free overhead. `ComponentStore` изолирует
хранилище и позволяет менять реализацию без изменения `Organism`.

- [x] Создать `DefaultComponentStore` (текущая ConcurrentHashMap-реализация)
- [x] Создать `ArrayComponentStore` для фиксированного набора компонентов (без boxing, O(1) по индексу)
- [x] `Organism` делегирует к `ComponentStore` вместо собственного `Map`
- [x] Сравнить throughput двух реализаций через `SimulationOptimizationTest`

### S2-2: Ввести `System`-слой в ECS-архитектуру [PARTIALLY COMPLETED]
**Новая директория:** `engine/ecs/`

Сервисы (`FeedingService`, `LifecycleService`) работают с `Animal`/`Organism` напрямую.
Настоящий ECS предполагает `System`, которая итерирует по компонентам, а не по сущностям.
Это даёт cache-friendly доступ и возможность переиспользования систем между доменами.

- [x] Создать `EntitySystem<T>` интерфейс в `engine/ecs/`
- [x] Создать `EntityQuery<T>` — выборка сущностей по набору компонентов из `SimulationWorld`
- [x] Мигрировать `LifecycleService` как `HealthSystem` (требует `HealthComponent`, `AgeComponent`)
- [x] Мигрировать `MovementService` как `MovementSystem` (требует `MovementComponent`)
- [x] Убедиться, что `EntitySystem` реализует `CellService` — остаётся в pipeline `GameLoop`

### S2-3: Добавить `ComponentFactory` для создания стандартных наборов
**Новый файл:** `engine/ecs/ComponentFactory.java`

Сейчас `Animal`, `Organism`, `GenericBiomass` создают компоненты в конструкторе.
При расширении набора компонентов каждый конструктор нужно менять вручную.

- [ ] `ComponentFactory.createAnimalComponents(AnimalType)` — возвращает список компонентов
- [ ] `ComponentFactory.createBiomassComponents(BiomassConfig)` — аналогично
- [ ] Поддержка `ComponentBundle` — именованный preset: `PREDATOR_BUNDLE`, `HERBIVORE_BUNDLE`

---
...
