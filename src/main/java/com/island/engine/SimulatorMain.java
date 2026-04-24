package com.island.engine;

import com.island.config.Configuration;
import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.service.*;

public class SimulatorMain {
    public static void main(String[] args) {
        // 1. Загрузка конфигурации
        Configuration config = Configuration.load();
        
        // 2. Инициализация матрицы взаимодействий
        InteractionMatrix interactionMatrix = new InteractionMatrix();
        SpeciesConfig speciesConfig = SpeciesConfig.getInstance();
        
        // Наполняем матрицу из SpeciesConfig
        for (String predatorKey : speciesConfig.getAllSpeciesKeys()) {
            for (String preyKey : speciesConfig.getAllSpeciesKeys()) {
                int chance = speciesConfig.getHuntProbability(predatorKey, preyKey);
                if (chance > 0) {
                    interactionMatrix.setChance(predatorKey, preyKey, chance);
                }
            }
            // Добавляем поедание растений (для всех, у кого в конфиге нет хищничества или явно)
            // В данной упрощенной версии считаем, что кролики и т.д. едят траву со 100% вероятностью
            if (predatorKey.equals("rabbit") || predatorKey.equals("duck")) {
                interactionMatrix.setChance(predatorKey, "Plant", 100);
            }
        }

        // 3. Создание острова
        Island island = new Island(config.getIslandWidth(), config.getIslandHeight());

        // 4. Настройка GameLoop (нужен для получения ExecutorService)
        GameLoop gameLoop = new GameLoop(config.getTickDurationMs());
        com.island.view.ConsoleView consoleView = new com.island.view.ConsoleView();

        // 5. Инициализация мира (теперь параллельно через executor)
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, speciesConfig, gameLoop.getTaskExecutor());

        // 6. Добавляем фазы симуляции в правильном порядке
        gameLoop.addRecurringTask(new LifecycleService(island, gameLoop.getTaskExecutor())); // Старение и метаболизм
        gameLoop.addRecurringTask(new FeedingService(island, interactionMatrix, gameLoop.getTaskExecutor())); // Питание
        gameLoop.addRecurringTask(new MovementService(island, gameLoop.getTaskExecutor()));  // Перемещение
        gameLoop.addRecurringTask(new ReproductionService(island, gameLoop.getTaskExecutor())); // Розмножение (животные + растения)
        gameLoop.addRecurringTask(new CleanupService(island, gameLoop.getTaskExecutor()));   // Очистка трупов
        
        // Добавляем вывод статистики через вьюху
        gameLoop.addRecurringTask(() -> consoleView.display(island));

        // 7. Запуск
        System.out.println("Запуск симуляции острова (параллельная инициализация и перемещение)...");
        gameLoop.start();
    }
}
