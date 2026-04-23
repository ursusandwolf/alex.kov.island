package com.island.engine;

import com.island.model.Island;
import java.util.List;
import java.util.ArrayList;

public class GameEngine {
    private final Island island;
    private final List<Runnable> phases = new ArrayList<>();

    public GameEngine(Island island) {
        this.island = island;
    }

    public void addPhase(Runnable phase) {
        phases.add(phase);
    }

    public void tick() {
        // Выполнение всех фаз цикла
        for (Runnable phase : phases) {
            phase.run();
        }
    }
}
