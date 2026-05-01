Следующий уровень после “домики и тики” — это уже не визуализация, а **система причинно-следственных связей**.
Игрок должен понимать:

> почему город растет;
> почему деградирует;
> откуда деньги;
> почему появился дефицит.

Это и есть ядро SimCity-like симуляции.

---

# 1) Главная идея модели

Не симулируй “реальных людей”.

Симулируй:

* спрос,
* привлекательность,
* доступность,
* денежный поток.

Большинство city-builder’ов — это не “агенты”, а набор экономических формул поверх карты.

---

# 2) Почему растет население

Население растет, если город:

* имеет жилье;
* имеет работу;
* имеет инфраструктуру;
* остается финансово стабильным.

То есть:

```text id="w21142"
growthScore =
    housing +
    jobs +
    services +
    landValue +
    safety -
    pollution -
    taxes
```

Если `growthScore > threshold`
→ появляются новые жители.

---

# 3) Минимальная модель роста

## Дом

Дом имеет:

* capacity
* occupancy
* desirability

Например:

```java
class ResidentialBuilding {
    int capacity = 10;
    int occupancy = 6;
    double desirability = 0.72;
}
```

---

## Каждый тик:

```text id="w50327"
if desirability > 0.6:
    occupancy += random growth

if desirability < 0.3:
    occupancy -= random decay
```

---

# 4) Что влияет на desirability

## Позитив

* дорога рядом;
* работа доступна;
* парк;
* полиция;
* электричество;
* низкий налог.

## Негатив

* загрязнение;
* пробки;
* высокий налог;
* отсутствие воды/электричества;
* шум;
* преступность.

---

# 5) Не делай “реального человека”

Плохой старт:

```java
Resident {
   hunger;
   fatigue;
   wallet;
   mood;
   ...
}
```

Это быстро взрывает сложность.

Для начала лучше:

```java
Building {
    occupancy
    happiness
}
```

То есть дом — агрегат населения.

---

# 6) Денежный поток города

Самая важная часть gameplay loop.

Игрок:

* тратит деньги;
* строит;
* получает рост;
* рост увеличивает налоги;
* налоги позволяют строить больше.

---

# 7) Минимальная экономика

## Доходы

### Налоги жителей

```text id="w94455"
income = population * residentialTax
```

### Налоги бизнеса

```text id="w92810"
income += jobs * commercialTax
```

---

## Расходы

### Содержание зданий

```text id="w61772"
expenses += roadMaintenance
expenses += policeMaintenance
expenses += powerPlantMaintenance
```

---

# 8) Самая важная механика

Игрок должен иметь возможность:

* построить слишком много;
* уйти в минус;
* начать терять город.

Без риска нет стратегии.

---

# 9) Простейший финансовый цикл

```text id="w29770"
population grows
    ↓
more taxes
    ↓
more services
    ↓
higher attractiveness
    ↓
more population
```

И отрицательный цикл:

```text id="w92143"
high taxes
    ↓
citizens unhappy
    ↓
people leave
    ↓
less taxes
    ↓
budget crisis
```

---

# 10) Архитектура симуляции экономики

Очень важно разделить:

## State

Текущее состояние:

* population
* treasury
* buildings
* happiness

---

## Systems

Кто изменяет state:

### EconomySystem

Считает:

* доход;
* расходы;
* бюджет.

### PopulationSystem

Считает:

* миграцию;
* заселение;
* happiness.

### InfrastructureSystem

Считает:

* connected roads;
* utilities;
* service coverage.

---

# 11) Хороший паттерн для city-builder

Вместо:

```java
building.update()
```

Лучше:

```java
economySystem.process(city)
populationSystem.process(city)
```

Почему:

* меньше coupling;
* легче балансировать;
* легче тестировать;
* проще оптимизация.

---

# 12) Как SimCity скрывает сложность

Игрок видит:

* “спрос высокий”
* “не хватает дорог”
* “город беднеет”

Но внутри обычно:

* коэффициенты;
* heatmap;
* aggregate simulation.

То есть:
не тысячи NPC,
а статистические модели.

---

# 13) Очень полезная механика — demand bars

Три счетчика:

* Residential Demand
* Commercial Demand
* Industrial Demand

---

## Например:

```text id="w14611"
residentialDemand =
    jobsAvailable
    - taxes
    + landValue
```

Если demand высокий:

* новые дома быстро заселяются.

Если отрицательный:

* люди уезжают.

Это уже создает “ощущение живого города”.

---

# 14) Минимальный “живой город”

Тебе уже достаточно:

## Есть:

* карта;
* дороги;
* дома;
* работа;
* налоги;
* maintenance;
* desirability;
* спрос.

## Нет:

* сложных NPC;
* транспорта;
* AI поведения.

Но игра уже будет ощущаться как city-builder.

---

# 15) Следующий правильный шаг после этого

После базовой экономики обычно добавляют:

## 1. Service Coverage

Радиус действия:

* полиции;
* больницы;
* школы.

---

## 2. Traffic

Не симуляция машин,
а:

* congestion score;
* throughput.

---

## 3. Utilities

* вода;
* электричество;
* мусор.

---

## 4. Land Value

Цена территории.

Это ключевая скрытая переменная жанра.

---

# 16) Самая полезная инженерная цель

Не “реализм”.

А:

* стабильная симуляция;
* понятные feedback loops;
* предсказуемые последствия решений игрока.

---

# 17) Очень хороший MVP

Если твой движок умеет:

* строить дом;
* подключать дорогу;
* заселять жителей;
* собирать налоги;
* считать maintenance;
* банкротить город;
* вызывать отток населения;

— это уже настоящий foundation city-builder.

Следующим сообщением могу показать:

* формулы growth/desirability;
* структуру EconomySystem;
* как считать land value;
* как устроить demand simulation;
* или архитектуру ECS/System-based движка для Java.
