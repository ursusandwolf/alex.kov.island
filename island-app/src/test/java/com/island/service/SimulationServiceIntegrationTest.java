package com.island.service;

import com.island.engine.scheduling.SimulationStatus;
import com.island.nature.NaturePlugin;
import com.island.engine.core.NamedSimulationPlugin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("nature")
class SimulationServiceIntegrationTest {

    @Autowired
    private SimulationService simulationService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void shouldManageSimulationLifecycle() {
        simulationService.pause();
        assertEquals(SimulationStatus.PAUSED, simulationService.getStatus());
        
        simulationService.resume();
        assertEquals(SimulationStatus.RUNNING, simulationService.getStatus());
        
        simulationService.stop();
        assertEquals(SimulationStatus.IDLE, simulationService.getStatus());

        simulationService.start("nature", 20, 20, 100);
        assertEquals(SimulationStatus.RUNNING, simulationService.getStatus());
    }
}
