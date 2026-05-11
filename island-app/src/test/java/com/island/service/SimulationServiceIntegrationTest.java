package com.island.service;

import com.island.engine.scheduling.SimulationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SimulationServiceIntegrationTest {

    @Autowired
    private SimulationService simulationService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void shouldStartAndStopNatureSimulation() {
        assertEquals(SimulationStatus.IDLE, simulationService.getStatus());
        
        simulationService.startSimulation("nature");
        assertEquals(SimulationStatus.RUNNING, simulationService.getStatus());
        
        simulationService.stop();
        assertEquals(SimulationStatus.IDLE, simulationService.getStatus());
    }

    @Test
    void shouldPauseAndResumeSimulation() {
        simulationService.startSimulation("nature");
        
        simulationService.pause();
        assertEquals(SimulationStatus.PAUSED, simulationService.getStatus());
        
        simulationService.resume();
        assertEquals(SimulationStatus.RUNNING, simulationService.getStatus());
        
        simulationService.stop();
    }
}
