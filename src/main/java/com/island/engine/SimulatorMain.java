package com.island.engine;
import com.island.util.InteractionMatrix;
import com.island.config.Configuration;
import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.content.FeedingService;
import com.island.service.*;

import java.util.Map;

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
        gameLoop.addRecurringTask(island::nextTick); // Инкремент такта в начале
        gameLoop.addRecurringTask(new LifecycleService(island, gameLoop.getTaskExecutor())); // Старение и метаболизм
        gameLoop.addRecurringTask(new FeedingService(island, interactionMatrix, gameLoop.getTaskExecutor())); // Питание
        gameLoop.addRecurringTask(new MovementService(island, gameLoop.getTaskExecutor()));  // Перемещение
        gameLoop.addRecurringTask(new ReproductionService(island, gameLoop.getTaskExecutor())); // Розмножение (животные + растения)
        gameLoop.addRecurringTask(new CleanupService(island, gameLoop.getTaskExecutor()));   // Очистка трупов
        
        // Добавляем вывод статистики через вьюху
        gameLoop.addRecurringTask(() -> consoleView.display(island));

        // 7. Запуск
        System.out.println("Запуск симуляции острова (параллельная инициализация и перемещение)...");
        System.out.println("Лимит времени: 5 минут. Условие остановки: вымирание любого вида.");
        gameLoop.start();

        // 8. Мониторинг завершения (в главном потоке)
        long startTime = System.currentTimeMillis();
        long maxDurationMs = 5 * 60 * 1000; // 5 минут

        try {
            while (gameLoop.isRunning()) {
                Thread.sleep(2000); // Проверка каждые 2 секунды

                // Проверка лимита времени
                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    System.out.println("\n⏳ Время вышло (5 минут). Остановка симуляции...");
                    gameLoop.stop();
                    break;
                }

                // Проверка вымирания видов
                Map<String, Integer> counts = island.getSpeciesCounts();
                // Проверяем все виды, которые были изначально (из конфига)
                for (String species : speciesConfig.getAllSpeciesKeys()) {
                    if (counts.getOrDefault(species, 0) == 0) {
                        System.out.println("\n💀 Вид '" + species + "' вымер! Остановка симуляции...");
                        gameLoop.stop();
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Симуляция завершена.");
    }
}
