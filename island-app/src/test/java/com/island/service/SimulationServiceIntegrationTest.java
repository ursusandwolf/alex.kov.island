package com.island.service;

import com.island.engine.scheduling.SimulationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
        // By default, it should be running because of @EventListener(ApplicationStartedEvent.class)
        // However, in tests, it might take a moment to start or we might want to check transitions
        
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
