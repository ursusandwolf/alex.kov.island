package com.island.controller;

import com.island.service.SimulationService;
import com.island.service.SnapshotHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.island.engine.scheduling.SimulationStatus;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationController.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimulationService simulationService;

    @MockBean
    private SnapshotHistoryService historyService;

    @Test
    void testSimulationStatus() throws Exception {
        when(simulationService.getStatus()).thenReturn(SimulationStatus.RUNNING);

        mockMvc.perform(get("/api/v1/simulation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void testSimulationPause() throws Exception {
        when(simulationService.getStatus()).thenReturn(SimulationStatus.PAUSED);

        mockMvc.perform(post("/api/v1/simulation/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(simulationService).pause();
    }

    @Test
    void testSimulationResume() throws Exception {
        when(simulationService.getStatus()).thenReturn(SimulationStatus.RUNNING);

        mockMvc.perform(post("/api/v1/simulation/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        verify(simulationService).resume();
    }

    @Test
    void testSimulationStop() throws Exception {
        when(simulationService.getStatus()).thenReturn(SimulationStatus.IDLE);

        mockMvc.perform(post("/api/v1/simulation/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IDLE"));

        verify(simulationService).stop();
    }

    @Test
    void testStartSimulation() throws Exception {
        mockMvc.perform(post("/api/v1/simulation/start")
                .param("type", "nature")
                .param("width", "30")
                .param("height", "30")
                .param("tickMs", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void testStartWithInvalidParamsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/simulation/start")
                .param("width", "2")
                .param("height", "20")
                .param("type", "nature"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnknownPluginReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Unknown plugin: invalid"))
                .when(simulationService).start("invalid", 20, 20, 100);

        mockMvc.perform(post("/api/v1/simulation/start")
                .param("type", "invalid")
                .param("width", "20")
                .param("height", "20")
                .param("tickMs", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown plugin: invalid"));
    }

    @Test
    void testListSnapshots() throws Exception {
        when(historyService.listSnapshots()).thenReturn(List.of("snapshot1.json"));
        mockMvc.perform(get("/api/v1/simulation/snapshot/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("snapshot1.json"));
    }

    @Test
    void testSaveSnapshot() throws Exception {
        when(historyService.saveCurrentSnapshot()).thenReturn(Optional.of("snap.json"));
        mockMvc.perform(post("/api/v1/simulation/snapshot/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("snap.json"));
    }
}
