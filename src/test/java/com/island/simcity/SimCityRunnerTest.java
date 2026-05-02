package com.island.simcity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SimCityRunnerTest {
    @Test
    @Disabled("Runs infinite simulation")
    void runSimulation() {
        SimCityLauncher.main(new String[0]);
    }
}
