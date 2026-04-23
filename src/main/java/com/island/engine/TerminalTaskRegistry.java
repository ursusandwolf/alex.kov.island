package com.island.engine;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Класс для обработки терминальных (разовых) задач,
 * которые должны быть выполнены вне основного цикла игры.
 */
public class TerminalTaskRegistry {
    private final Queue<Runnable> terminalTasks = new ConcurrentLinkedQueue<>();

    public void submit(Runnable task) {
        terminalTasks.add(task);
    }

    public void processAll() {
        Runnable task;
        while ((task = terminalTasks.poll()) != null) {
            task.run();
        }
    }
}
