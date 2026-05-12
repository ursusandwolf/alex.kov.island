package com.island.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for SimulationController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("nature")
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSimulationLifecycle() throws Exception {
        // 1. Check status (should be RUNNING due to auto-start)
        mockMvc.perform(get("/api/v1/simulation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        // 2. Pause
        mockMvc.perform(post("/api/v1/simulation/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        // 3. Resume
        mockMvc.perform(post("/api/v1/simulation/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        // 4. Snapshot
        mockMvc.perform(get("/api/v1/simulation/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").exists());
    }
}
