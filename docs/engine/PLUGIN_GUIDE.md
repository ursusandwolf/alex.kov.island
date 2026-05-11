# Как написать новый плагин

## Минимальный плагин за 5 шагов

### Шаг 1: Создать Maven-модуль
<!-- в parent pom.xml: -->
<modules>
    <module>island-engine</module>
    <module>island-nature</module>
    <module>island-simcity</module>
    <module>island-myplugin</module>  <!-- добавить -->
    <module>island-app</module>
</modules>

<!-- island-myplugin/pom.xml -->
<parent>
    <groupId>com.island</groupId>
    <artifactId>alex.kov.island</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>
<artifactId>island-myplugin</artifactId>

<dependencies>
    <dependency>
        <groupId>com.island</groupId>
        <artifactId>island-engine</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>

### Шаг 2: Определить тип сущности
Сущность должна реализовывать интерфейс `Mortal` (если она может погибать) или быть просто `Entity`.

```java
public class MyEntity implements Mortal {
    private boolean alive = true;
    
    @Override public boolean isAlive() { return alive; }
    @Override public void die() { this.alive = false; }
    @Override public String getTypeName() { return "MyEntity"; }
}
```

### Шаг 3: Реализовать мир (SimulationWorld)
Мир управляет хранением сущностей и предоставляет чанки для параллельной обработки.

```java
public class MyWorld implements SimulationWorld<MyEntity> {
    // Реализуйте сетку ячеек, методы добавления/удаления
    // и getParallelWorkUnits() для GameLoop
}
```

### Шаг 4: Реализовать плагин
Создайте класс, реализующий `SimulationPlugin`.

```java
public class MyPlugin implements SimulationPlugin<MyEntity> {
    @Override
    public SimulationWorld<MyEntity> createWorld(EventBus eventBus) {
        return new MyWorld(...);
    }

    @Override
    public void registerTasks(GameLoop<MyEntity> loop, SimulationWorld<MyEntity> world, EventBus bus) {
        // Регистрация систем и сервисов
        loop.addRecurringTask(new MySystem());
    }
}
```

### Шаг 5: Запустить симуляцию
Используйте `SimulationEngine` для запуска.

```java
SimulationConfig cfg = SimulationConfig.defaultFor(4);
try (SimulationContext<MyEntity> ctx = new SimulationEngine<MyEntity>().start(new MyPlugin(), cfg)) {
    // Симуляция запущена в фоне
    Thread.sleep(5000);
}
```

## Рекомендации
- Используйте пакетную структуру: `com.island.myplugin.{entities, systems, model, event}`.
- Не экспортируйте внутренние пакеты в `module-info.java`.
- Для сложных систем используйте `ECS_GUIDE.md`.
