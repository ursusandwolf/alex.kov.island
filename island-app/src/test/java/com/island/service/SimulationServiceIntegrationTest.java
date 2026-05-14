package com.island.service;

import com.island.engine.scheduling.SimulationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

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
        await().atMost(2, SECONDS).until(() -> simulationService.getStatus() == SimulationStatus.RUNNING);
    }
}
