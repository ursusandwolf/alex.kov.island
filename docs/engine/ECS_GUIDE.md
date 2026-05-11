# ECS: Как добавить Component и System

## Концепция
- **ДАННЫЕ** → Component
- **ЛОГИКА** → System
- **ФИЛЬТРАЦИЯ** → EntityQuery

## Добавить новый Component

### 1. Создать класс компонента
Компонент — это маркер или контейнер данных.

```java
public class HungerComponent implements Component {
    // Данные могут быть здесь (AoS) или в SoAStore (предпочтительно для производительности)
    private int level = 0;
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
```

### 2. Регистрация
Движок автоматически обнаружит компонент, если он добавлен к сущности через `entity.addComponent(new HungerComponent())`.

## Добавить новую System

Системы реализуют логику симуляции. Рекомендуется наследоваться от базовых классов плагина (например, `NatureEntitySystem`).

### 1. Реализация
```java
public class HungerSystem extends NatureEntitySystem {

    @Override
    public List<Class<? extends Component>> requiredComponents() {
        // Система будет обрабатывать только сущности, имеющие оба этих компонента
        return List.of(HungerComponent.class, HealthComponent.class);
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        // Укажите компоненты, которые система изменяет (для графа выполнения)
        return List.of(HungerComponent.class);
    }

    @Override
    public int priority() {
        return 75; // Чем выше число, тем раньше выполнится в своей фазе
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        HungerComponent hunger = entity.getComponent(HungerComponent.class);
        hunger.setLevel(hunger.getLevel() + 1);
        
        if (hunger.getLevel() > 100) {
            entity.die(DeathCause.HUNGER);
        }
    }
}
```

### 2. Регистрация в плагине
Добавьте систему в `GameLoop` внутри метода `registerTasks` вашего плагина:
```java
gameLoop.addRecurringTask(new HungerSystem());
```

## EntityQuery
Если вам нужно найти сущности вне метода `process` (например, для поиска цели), используйте запросы:

```java
EntityQuery query = EntityQuery.builder()
    .with(PositionComponent.class)
    .without(DeadComponent.class)
    .build();

List<Entity> targets = world.findEntities(query);
```

## SoA Оптимизация
Если данных много и они часто меняются, рассмотрите создание `SoAStore` (см. `ADR-002`). Это позволит системе работать напрямую с массивами примитивов.
