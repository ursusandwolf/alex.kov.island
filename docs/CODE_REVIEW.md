# CODE REVIEW — alex.kov.island (branch `dev`)

**Reviewer stance:** Tech Lead / Staff Engineer  
**Scope:** architecture, engine reusability, extensibility, domain decoupling, simulation loop, data structures, testability, scalability, risks  
**Important note:** this review is based on the core classes I inspected in `src/main/java/com/island/{engine,model,content,service}`. Where I make an inference beyond the exact snippet observed, I mark it explicitly as a **hypothesis**.

---

## 1. Общий обзор проекта

### Что моделируется
Проект моделирует островную экосистему: клеточное поле, животные, биомасса/растения, перемещения, питание, размножение, смерть, статистику популяции и “red book protection”.

### Насколько доменная модель привязана к конкретному сценарию
Привязка очень сильная.

Ключевые признаки:
- `Island` — это не абстрактный `World`, а конкретный остров с `width/height`, тороидальной адресацией в `getCell(int x, int y)` и `redBookProtectionEnabled`.
- `StatisticsService` и `Island.getProtectionMap(...)` содержат логику, завязанную на природоохранный “red book” сценарий.
- `AnimalType` хранит поля уровня конкретной игры: `maxPerCell`, `speed`, `foodForSaturation`, `huntProbabilities`, `isColdBlooded`, `isPackHunter`, `presenceProb`, `settlementBase`, `settlementRange`.
- `Cell` одновременно выступает как место размещения сущностей, индекс, lock-единица и агрегатор статистики.
- `Organism` / `Animal` уже содержат игровые правила, а не только состояние домена.

Это хорошая база для одной конкретной симуляции, но не для универсального движка.

### Можно ли переиспользовать текущие абстракции в других симуляциях
Частично — да, но только на уровне очень грубых примитивов:
- `Tickable` как интерфейс тика — полезен.
- `SimulationNode` / `SimulationWorld` могут стать отправной точкой для графа мира.
- `StatisticsService` можно переиспользовать как сервис событий.

Но в текущем виде абстракции слишком “островные”. Они содержат:
- географическую модель клетки,
- зоологические понятия,
- пищевые и репродуктивные правила,
- статистику по видам,
- special-case механики вроде red book protection.

Для SimCity-подобной игры или другой симуляции ядро пришлось бы переработать, а не просто заменить набор сущностей.

### Есть ли протекание домена в технические детали
Да, и это один из главных архитектурных рисков:
- `model.Cell` знает о `ReentrantLock`, `SimulationWorld`, `Chunk`, `StatisticsService` через побочные эффекты.
- `model.Island` содержит orchestration, domain policy, statistics aggregation and movement semantics.
- `content.Organism` и `content.Animal` завязаны на `SimulationConstants`, `EnergyPolicy`, `Poolable`, `UUID`.
- `GameLoop` сам создаёт пул потоков и скрыто управляет выполнением, но при этом не использует его в `runTick()`; это техническая деталь, смешанная с orchestration-слоем.

---

## 2. Архитектура и дизайн

### Архитектурный стиль
Это не чистая layered architecture и не чистый DDD. По факту это смесь:
- **anemic-ish model objects** для части данных,
- **rich domain objects** для `Animal`/`Organism`,
- **service-oriented orchestration** (`FeedingService`, `MovementService`, `ReproductionService`, `LifecycleService`),
- **monolithic world aggregate** (`Island`) с сильной централизацией правил,
- местами **data-driven registry/flyweight** (`SpeciesRegistry`, `AnimalType`).

### Насколько архитектура соответствует уровню “движка”
Пока — скорее “игра с претензией на движок”, чем движок.

Почему:
1. Engine-core не выделен как отдельный модуль с минимальным API.
2. Доменная модель и механики острова взаимно переплетены.
3. Сервисы существуют, но их границы не обеспечивают переносимость.
4. Подсистемы статистики, перемещения, защиты вида и жизненного цикла связаны через конкретные классы острова и клеток.

Для движка нужен слой, где:
- мир — это абстракция,
- сущности — это данные + поведение через стратегии/системы,
- правила — подключаемые модули,
- домен игры не живёт внутри ядра.

---

### SOLID

#### S — Single Responsibility Principle
Нарушен системно.

Примеры:

**`model.Island`**
- хранит grid,
- создаёт и партиционирует chunks,
- инициализирует соседей,
- считает статистику,
- управляет перемещением животных и биомассы,
- реализует защиту видов,
- является источником callbacks в `StatisticsService`,
- содержит tick-state.

Это сразу несколько причин для изменения. Класс слишком большой, слишком центральный и слишком зависимый.

**`model.Cell`**
- хранит сущности,
- реализует thread safety,
- ведёт индексы через `EntityContainer`,
- считает статистику,
- возвращает копии коллекций,
- выполняет cleanup,
- занимается переносом биомассы и death cleanup.

Класс одновременно является storage, concurrency boundary и query service.

**`content.Organism`**
- хранит состояние живого существа,
- отвечает за энергетику,
- lifecycle (`die`, `checkAgeDeath`, `reset`, `init`),
- содержит базовые combat/survival checks,
- знает о `Poolable`.

Это уже почти “engine entity base class”, но с доменной логикой и техническими контрактами одновременно.

---

#### O — Open/Closed Principle
Нарушен из-за глубокой привязки к типам и ветвления по ролям.

Примеры:
- `EntityContainer.addAnimal/removeAnimal` держит отдельные списки для predators/herbivores/type/size. Добавление новой классификации потребует изменения контейнера.
- `Cell` добавляет методы уровня доменных фильтров: `getPredators()`, `getHerbivores()`, `getAnimalsBySize(...)`, `countAnimalsByType(...)`.
- `StatisticsService` и `Island.getTotalAnimalDeathCount(...)` завязаны на `DeathCause` и на понимание того, что часть `SpeciesKey` — это biomass, часть — animal.

Это значит, что новая механика потребует не расширения, а переписывания существующих классов.

---

#### L — Liskov Substitution Principle
Формально классы наследуются нормально, но есть признаки нарушения семантики.

**`content.Animal` extends `Organism`**
- добавляет `isHiding`, `canEat`, `getHuntProbability`, `getMaxPerCell`, `getSpeed`.
- Однако базовый контракт `Organism` уже содержит `getDynamicMetabolismRate()` через `getWeight()`, а `Animal` частично переопределяет поведение через данные `AnimalType`.
- **Гипотеза:** в будущем разные подклассы животных могут начать ломать ожидания `Organism`, потому что базовый класс слишком много знает о текущей игре.

**`GenericBiomass` / plant-related entities**
- если biomass и animal наследуют общий `Organism`, но живут по разным жизненным правилам, то substitutability становится хрупким.  
- **Гипотеза:** на уровне runtime это может проявиться в неочевидных инвариантах: например, не все organisms должны уметь двигаться, охотиться или иметь одинаковую “жизнь”.

---

#### I — Interface Segregation Principle
Частично нарушен.

`SimulationWorld` судя по использованию в `Cell` и `Island` объединяет слишком много обязанностей:
- координатная навигация,
- перенос животных,
- перенос биомассы,
- статистика,
- protection map,
- смерть/рождение/удаление,
- tick callbacks.

Это интерфейс “всего на свете”. Клиентам нужен не полный `SimulationWorld`, а узкие интерфейсы:
- `SpatialWorld`
- `PopulationTracker`
- `MovementPort`
- `DeathPort`
- `ProtectionPolicyProvider`

---

#### D — Dependency Inversion Principle
Нарушен в нескольких местах.

**`GameLoop`**
- создаёт `FixedThreadPool` внутри конструктора;
- таким образом high-level orchestration зависит от конкретной реализации executor;
- тестирование и настройка scheduler вынужденно усложняются.

**`Island`**
- напрямую зависит от `StatisticsService`, `SpeciesRegistry`, `DeathCause`, `AnimalType`, `Biomass`;
- сам выступает и как доменный объект, и как инфраструктурная точка координации.

**`Cell`**
- напрямую вызывает `world.onOrganismAdded/Removed`;
- это связывает storage-level объект с глобальной статистикой.

---

### Анти-паттерны

#### 1) God Object
- `model.Island`
- частично `model.Cell`

Это центральные классы, куда стекается бизнес-логика, инфраструктура и вся навигация.

#### 2) Data Clump + Feature Envy
- `AnimalType` агрегирует множество параметров, которые дальше разносятся по коду.
- `Organism`/`Animal` постоянно дергают `AnimalType`, `SimulationConstants`, `EnergyPolicy`.

#### 3) Parallelism leakage
- `GameLoop` создаёт `ExecutorService`, но фактический цикл `runTick()` выполняется последовательно.
- В коде есть намёк на параллельную модель, но архитектурно она не доведена до конца. Это опаснее, чем отсутствие параллелизма: создаётся ложное ощущение scalability.

#### 4) Anemic domain with technical coupling
- `EntityContainer` — storage-only структура, но доступ к ней жёстко завязан на `Cell`.
- Внутренние коллекции возвращаются наружу без копирования (`getAllAnimals()`, `getPredators()`, etc.), что ломает инкапсуляцию.

---

### Coupling / Cohesion

#### Высокая связанность
- `Island` ↔ `Cell` ↔ `EntityContainer` ↔ `StatisticsService` ↔ `SpeciesRegistry`
- `Animal` ↔ `AnimalType` ↔ `EnergyPolicy` ↔ `SimulationConstants`
- `Organism` ↔ `Poolable` ↔ `EnergyPolicy` ↔ `SimulationConstants`

Эти связи не только по контрактам, но и по конкретным implementation details.

#### Низкая cohesion
Особенно у:
- `Island`
- `Cell`
- partially `GameLoop`

Эти классы совмещают разнородные обязанности: state, policy, orchestration, metrics, threading, movement.

---

### Разделение слоев
Фактически отсутствует жёсткое разделение на:
- domain layer,
- engine/application layer,
- infrastructure layer,
- presentation/visualization layer.

`view` есть как папка, но по core-части видно, что engine и domain переплетены напрямую.

---

### Где архитектура мешает переиспользованию
1. В `Island` заложен именно островной world model и тороидальная топология.
2. В `AnimalType` зашиты виды, ограничения по клетке, поведенческие флаги, охота.
3. В `StatisticsService` есть явное различение между животными и biomass.
4. В `Cell` логика индексации подстроена под predators/herbivores/size class.
5. В `GameLoop` предполагается tick-driven world с recurring tasks, но без общего event bus или system pipeline.

---

## 3. Алгоритмы и логика симуляции

### Основной цикл симуляции
`GameLoop`:
- хранит список recurring tasks;
- на каждом тике вызывает `runTick()`;
- внутри `runTick()` последовательно проходит все tasks;
- измеряет длительность цикла и sleeps до `tickDurationMs`.

Проблема: `taskExecutor` создаётся, но в `runTick()` не используется.  
Это значит, что:
- параллельность на уровне loop фактически не реализована;
- есть лишний lifecycle у executor;
- код вводит в заблуждение относительно модели исполнения.

### Универсальность loop для других симуляций
Ограничена.

Подходит для простого tick-based сценария, но не для универсального движка, потому что:
- нет фазы `preTick / update / resolve / postTick`;
- нет event pipeline;
- нет explicit ordering for systems;
- нет dependency between systems;
- нет deterministic replay hooks кроме косвенной идеи seed-based randomness.

### Лишние вычисления / проходы
Критичные места:

**`Island.getSpeciesCounts()`**
- проходит по всей сетке;
- для каждой клетки проходит по biomass containers;
- параллельно обращается к `StatisticsService`;
- затем снова суммирует biomass.

**`Island.getTotalOrganismCount()`**
- повторяет полные проходы по сетке.

**`Island.getGlobalSatiety()`**
- полный обход всех клеток и животных.

**`Island.getStarvingCount()`**
- полный обход всех клеток и животных.

Если такие методы вызываются в UI каждую секунду, они станут заметным hotspot.  
Для движка лучше иметь инкрементальные агрегаты, обновляемые по событиям, а не full scan on demand.

### Потенциальные проблемы сложности
#### `EntityContainer.fastRemove(...)`
Использует `indexOf` на `List`, то есть:
- поиск — `O(n)`,
- remove — `O(1)` после поиска.

На словах “fast remove”, на деле при росте плотности в клетке это всё равно линейная операция.  
Для небольших списков это нормально, но claim о constant-time removal неверен.

#### `EntityContainer.countBySpecies(...)`
Сканирует `animalsByType.entrySet()`.  
Для каждого запроса это `O(types in cell)`. Пока типов мало — терпимо. Но как только появятся более сложные сущности/подтипы, это станет дорогим.

#### `Island.moveOrganism(...)`
- берёт два lock;
- удаляет из одной клетки, добавляет в другую;
- в случае неудачи пытается вернуть обратно;
- в последнем fallback может убить животное.

Это сложная транзакционная операция, которая при росте concurrency может стать узким местом из-за lock contention.

### Узкие места при росте количества сущностей
- `Cell.getAnimals()` возвращает копию списка — дорого при частых запросах.
- `Island` делает глобальные scans.
- `StatisticsService` опирается на `AtomicInteger`/ConcurrentHashMap, но множество операций всё равно считываются неатомарно на уровне агрегатов.
- `GameLoop` не масштабируется реально параллельно, несмотря на наличие executor.

---

## 4. Коллекции и структуры данных

### Где выбор структур не оптимален

#### `EntityContainer`
Сейчас используются:
- `HashMap<AnimalType, List<Animal>>`
- `EnumMap<SizeClass, List<Animal>>`
- `ArrayList<Animal>` для predators/herbivores/allAnimals
- `HashMap<SpeciesKey, Biomass>`

Проблемы:
1. `List` + `indexOf` + swap-to-remove = слабая гарантия реальной эффективности.
2. Дублирование данных в нескольких индексах увеличивает стоимость обновления и риск рассинхронизации.
3. `allAnimals`, `predators`, `herbivores`, `animalsByType`, `animalsBySize` — это несколько projections одной и той же сущности. Это оправдано только если измерено профилированием.

#### `Cell.get...()` методы
Возвращают `new ArrayList<>(...)`, т.е. каждый read API плодит аллокации.  
Для UI/analytics это может быть допустимо. Для hot path — нет.

#### `GameLoop.recurringTasks`
`ArrayList` подходит, если задачи добавляются редко и исполняются часто. Но:
- нет защиты от concurrent modification;
- нет явного lifecycle management;
- нет remove task API.

### Где происходят лишние аллокации / копирования
- `Cell.getAnimals()`, `getPredators()`, `getHerbivores()`, `getAnimalsByType()`, `getAnimalsBySize()`, `getBiomassContainers()` создают новые `ArrayList`.
- `Cell.setNeighbors(...)` делает `List.copyOf(neighbors)`.
- `Island.getSpeciesCounts()` создаёт новый `HashMap`.
- `Island.getProtectionMap(...)` создаёт новый `HashMap` на каждый вызов.
- `Island.getParallelWorkUnits()` делает `chunks.stream().map(...).toList()`.

Сами по себе это нормальные решения для не-hot paths, но здесь они находятся внутри core simulation model, где часто будут вызываться в цикле.

### Насколько структуры универсальны
Слабо универсальны:
- `EnumMap<SizeClass, ...>` и `AnimalType` предполагают именно “зоологическую” классификацию.
- Для city builder или factory simulator придётся заново переделывать контейнеры под другие измерения: building type, job type, service radius, queue state, logistics state и т.д.

---

## 5. Качество кода

### Нейминг
Есть несколько проблемных мест.

#### `GameLoop.taskExecutor`
Поле создаётся, но в показанном коде не используется.  
Нейминг обещает один смысл, поведение — другое.

#### `EntityContainer`
Имя слишком общее для класса, который фактически является indexed cell storage.  
Более точное имя: `CellEntityIndex`, `CellOccupancyIndex`, `CellContents`.

#### `Animal.isProtected(int currentTick)`
Параметр `currentTick` не используется. Это либо:
- недоделанный контракт,
- либо ошибка дизайна интерфейса.

#### `Island.getProtectionMap(SpeciesRegistry registry)`
Метод принимает `registry`, хотя у `Island` уже есть собственный `registry` field. Это сигнал о смешении dependency ownership.

#### `getPlantCount()`
Возвращает `int`, но суммирует `double total`.  
Семантически это не “count”, а “mass rounded to int” или “biomass amount”. Это вводит в заблуждение.

### Дублирование
Есть заметное повторение логики:
- проходы по всей сетке в `getSpeciesCounts()`, `getTotalOrganismCount()`, `getGlobalSatiety()`, `getStarvingCount()`;
- double-lock ordering в `moveOrganism(...)` и `moveBiomassPartially(...)`;
- `Cell` содержит множество почти одинаковых guarded getter methods.

### Магические значения
- `chunkSize = 20` в `Island.partitionIntoChunks()`
- `0.05` в `getProtectionMap(...)`
- `0.60` и `0.30` в расчёте `hideChance`
- `30.0` в `getStarvingCount()`
- `100.0` в `getGlobalSatiety()`
- `DEATH_EPSILON`, `BASE_METABOLISM_PERCENT`, `REPRODUCTION_MIN` и другие constants — нормальны, но часть правил всё же захардкожена напрямую.

Если эти значения — доменные параметры, они должны быть в policy/config object, а не в методе.

### Слишком большие методы / классы
#### `Island`
Самый заметный кандидат на распил.  
Он слишком многозадачный и уже стал центральной точкой для:
- grid init,
- topology,
- movement,
- population stats,
- lifecycle callbacks,
- protection policy,
- biomass transfer,
- tick state.

#### `Cell`
Тоже слишком большой: storage + concurrent access + statistics + cleanup + movement + biomass.

#### `Organism`
Не огромный по строкам, но слишком “dense” по responsibilities.

---

## 6. Тестируемость

### Где код сложно тестировать
#### `GameLoop`
- Создаёт собственный `Thread` через `new Thread(this::run).start()`.
- Создаёт собственный `ExecutorService`.
- Сложно deterministically контролировать tick progression.

Для тестов нужен injected scheduler / clock / executor.

#### `Island`
- Жёстко завязан на `StatisticsService`, `SpeciesRegistry`.
- Переходы состояния завязаны на клеточные locks и side effects.
- `moveOrganism(...)` трудно тестировать без комплексного setup.

#### `Cell`
- Методы вроде `cleanupDeadOrganisms()` и `move`-операции требуют реальных `Animal`, `Biomass`, `SimulationWorld` объектов.
- Возврат копий коллекций скрывает часть состояния, усложняя assert-ы на инварианты.

#### `Organism`
- Использует internal state + locks + static policy values.
- Без вынесения energy policy и lifecycle policy в тестовые стратегии трудно писать быстрые unit tests.

### Можно ли тестировать ядро изолированно от домена
Пока — только частично.  
`GameLoop` можно тестировать отдельно, `StatisticsService` — почти отдельно, но `Island`/`Cell`/`Organism` ещё тесно переплетены с доменом острова.

### Где не хватает абстракций
- `Clock` / `TickScheduler`
- `ExecutorProvider`
- `MovementPolicy`
- `ReproductionPolicy`
- `MortalityPolicy`
- `PopulationCounter`
- `ProtectionPolicy`
- `CellStorage` interface

---

## 7. Масштабируемость и расширяемость

### Добавить новый тип сущности
Сейчас это будет дорого.

Потребуется затронуть:
- `AnimalType`
- `SpeciesRegistry`
- `EntityContainer`
- `Cell`
- `Island.getProtectionMap(...)`
- `StatisticsService`
- possibly `Animal` / `Organism`
- probably `view` if есть отображение
- services feeding/reproduction/movement

Итого: не просто registration, а цепочка каскадных изменений.

### Изменить правила взаимодействия
Например, добавить:
- территориальность,
- заражение,
- климат,
- seasons,
- disease,
- migration pressure,
- city-like service coverage.

Текущая модель плохо подходит, потому что правила живут:
- частично в сущностях,
- частично в `Island`,
- частично в `StatisticsService`,
- частично в `AnimalType`.

То есть бизнес-правила не в одном месте.

### Добавить новую механику (ресурсы, экономика, погода)
Потребуется встраивание ещё одного слоя в уже перегруженный `Island`.  
Это верный путь к “framework collapse”: класс начнёт разрастаться до недопустимого размера.

### Использовать код в другой игре с другим доменом
Почти наверняка придётся переписывать ядро.

Особенно:
- `Island` как world model,
- `Cell` как occupant container,
- `AnimalType` как единственный type descriptor,
- `StatisticsService` как population tracker,
- `redBookProtectionEnabled` как доменно-специфичный policy switch.

---

## 8. Потенциальные баги и риски

### Конкретные подозрительные места

#### `GameLoop.stop()`
- `running = false; taskExecutor.shutdown();`
- но сам `Thread` из `start()` не хранится и не join-ится;
- `run()` может продолжать жить до следующей итерации;
- `taskExecutor` в показанном коде не используется, но всё равно shutdown вызывается.

Риск: непредсказуемый shutdown lifecycle.

#### `EntityContainer.removeBiomass(Biomass b)`
```java
if (biomassBySpecies.remove(b.getSpeciesKey()) != null) {
    allBiomass.remove(b);
    return true;
}
```
Если в карте уже лежит другой `Biomass` под тем же ключом, удаление по ключу может снести не тот объект-состояние.  
Это выглядит как потенциальная модельная ошибка, если biomass агрегируется по виду.

#### `Cell.addBiomass(SpeciesKey key, double amount)`
Если `existing == null`, метод возвращает `false`.  
Но `Island.moveBiomassPartially(...)` вызывает `to.addBiomass(...)` и по результату решает, переносить ли массу. Это делает новую biomass-пересылку зависимой от существования контейнера в target cell.  
**Гипотеза:** при первом заселении biomass в клетке возможна логическая потеря ресурса или необходимость предварительной инициализации контейнера.

#### `Island.moveOrganism(...)`
Сильный риск при failover:
- remove из source прошёл,
- add в target не прошёл,
- fallback return to source тоже не прошёл,
- тогда объект forcibly killed.

Это бизнес-решение, которое может скрывать баги вместо их обнаружения. Для production-grade simulation это опасно: потеря сущности происходит как side effect of contention/capacity overflow.

#### `Island.getGlobalSatiety()`
```java
return (animalCount == 0) ? 100.0 : (totalCurrent / totalMax) * 100.0;
```
Если `totalMax == 0`, получится `NaN`/`Infinity` при недокументированном edge case.  
**Гипотеза:** сейчас это маловероятно, но при добавлении сущностей с нулевой max energy риск станет реальным.

#### `Organism.setEnergy(double energy)`
- не использует `energyLock`;
- меняет `currentEnergy` и `isAlive` без синхронизации;
- в другом месте `tryConsumeEnergy(...)` синхронизирован.

Это гонка состояний.

#### `EntityContainer.getAllAnimals()/getPredators()/...`
Возвращают внутренние mutable lists.  
Любой внешний caller может нарушить инварианты контейнера.

---

## 9. Приоритетные улучшения

### [HIGH]
1. **Выделить engine-core из domain model.**  
   `Island`, `Cell`, `GameLoop`, `StatisticsService` должны быть разнесены по слоям, а игровые правила — вынесены в стратегии/системы.

2. **Убрать публичную утечку mutable collections.**  
   `EntityContainer` не должен отдавать внутренние списки напрямую.

3. **Унифицировать lifecycle и threading.**  
   `GameLoop` должен принимать executor/scheduler извне и реально использовать его либо удалить лишний.

4. **Разорвать зависимость `Island` от конкретного сценария.**  
   Red-book policy, satiety, starving threshold, toroidal cell access должны стать подключаемыми policies.

### [MEDIUM]
1. Вынести статистику в event-driven aggregator вместо частых full scans.
2. Разделить `Cell` на `CellState` + `CellIndex` + `CellLock` + `CellQueries`.
3. Упростить `moveOrganism` и `moveBiomassPartially` через transactional move service.
4. Сделать `AnimalType` immutable config object без смешения mechanics flags и balance data.
5. Добавить явные interfaces для movement, reproduction, mortality, protection.

### [LOW]
1. Переименовать методы и классы для точности (`EntityContainer` → `CellEntityIndex`, `getPlantCount()` → `getBiomassAmountAsInt()` или аналог).
2. Убрать неиспользуемые параметры.
3. Свести магические константы в policy/config objects.
4. Добавить более явные JavaDoc по инвариантам.

---

## 10. Рефакторинг (с примерами)

### Пример 1. Выделение engine-ядра из доменной логики

#### До
```java
public class Island implements SimulationWorld {
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();

    public void moveOrganism(Animal animal, Cell from, Cell to) { ... }
    public void reportDeath(SpeciesKey speciesKey, DeathCause cause) { ... }
    public Map<SpeciesKey, Double> getProtectionMap(SpeciesRegistry registry) { ... }
    public double getGlobalSatiety() { ... }
}
```

#### После
```java
public interface WorldGrid {
    Optional<Node> neighborOf(Node node, int dx, int dy);
    List<? extends Node> workUnits();
}

public interface PopulationEventSink {
    void onBirth(SpeciesKey key);
    void onRemoval(SpeciesKey key);
    void onDeath(SpeciesKey key, DeathCause cause);
}

public interface ProtectionPolicy {
    Map<SpeciesKey, Double> protectionFor(WorldSnapshot snapshot);
}

public final class SimulationEngine {
    private final WorldGrid world;
    private final List<SimulationSystem> systems;

    public void tick(long tick) {
        for (SimulationSystem system : systems) {
            system.update(world, tick);
        }
    }
}
```

**Что это даёт:**  
`Island` перестаёт быть и миром, и политикой, и статистикой.  
Можно подключить другой world model: city grid, factory floor, dungeon map, transport network.

---

### Пример 2. Введение абстракции для перемещения и транзакции сущностей

#### До
```java
public void moveOrganism(Animal animal, Cell from, Cell to) {
    Cell first = ...;
    Cell second = ...;

    first.getLock().lock();
    try {
        second.getLock().lock();
        try {
            if (from.removeAnimal(animal)) {
                if (!to.addAnimal(animal)) {
                    if (!from.addAnimal(animal)) {
                        animal.tryConsumeEnergy(animal.getCurrentEnergy());
                        reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                    }
                }
            }
        } finally {
            second.getLock().unlock();
        }
    } finally {
        first.getLock().unlock();
    }
}
```

#### После
```java
public interface MovementPolicy<T extends Organism> {
    MovementResult move(T entity, CellHandle from, CellHandle to);
}

public final class DefaultMovementPolicy implements MovementPolicy<Animal> {
    private final CellRepository cells;
    private final PopulationEventSink events;

    @Override
    public MovementResult move(Animal entity, CellHandle from, CellHandle to) {
        if (!cells.canAccept(to, entity)) {
            return MovementResult.rejected("target capacity exceeded");
        }

        cells.remove(from, entity);
        cells.add(to, entity);
        return MovementResult.applied();
    }
}
```

**Что это даёт:**  
- убирается “самоубийство” как fallback поведения;
- перемещение становится тестируемой транзакцией;
- правила можно заменить для другой игры.

---

### Пример 3. Разделение storage и query API в `Cell`

#### До
```java
public List<Animal> getAnimals() {
    lock.lock();
    try {
        return new ArrayList<>(container.getAllAnimals());
    } finally {
        lock.unlock();
    }
}
```

#### После
```java
public interface CellReadView {
    int animalCount();
    Stream<Animal> animals();
}

public interface CellWritePort {
    boolean add(Animal animal);
    boolean remove(Animal animal);
}

public final class Cell implements CellReadView, CellWritePort {
    private final CellState state;
    private final CellIndex index;
}
```

**Что это даёт:**  
- read path можно оптимизировать отдельно;
- write path перестаёт быть привязан к внутренней структуре списков;
- проще тестировать и профилировать.

---

## 11. Итоговая оценка

- **Архитектура:** 5/10
- **Код:** 6/10
- **Переиспользуемость:** 3/10
- **Общая оценка:** 5/10

### Короткий вердикт
Проект **ещё не готов** стать основой универсального симуляционного движка.

Причина не в отсутствии отдельных хороших идей — они есть:
- тик-ориентированная модель,
- registry/flyweight подход,
- попытка разделить services,
- внимание к concurrency и deterministic simulation.

Проблема в том, что эти идеи не собраны в устойчивое engine-core:
- домен острова слишком глубоко прошит в ядро,
- `Island` и `Cell` перегружены обязанностями,
- статистика и policies смешаны с моделью мира,
- структура данных ориентирована на текущий сценарий, а не на расширяемую платформу.

Чтобы стать базой для движка, проекту нужно сначала стать **нейтральным симуляционным каркасом**, а уже поверх него — реализовывать остров, город, ферму или любой другой сценарий.

---

## Ключевые файлы, на которые опирается review
- `src/main/java/com/island/engine/GameLoop.java`
- `src/main/java/com/island/model/Cell.java`
- `src/main/java/com/island/model/EntityContainer.java`
- `src/main/java/com/island/model/Island.java`
- `src/main/java/com/island/content/Organism.java`
- `src/main/java/com/island/content/Animal.java`
- `src/main/java/com/island/content/AnimalType.java`
- `src/main/java/com/island/content/SpeciesRegistry.java`
- `src/main/java/com/island/service/StatisticsService.java`

