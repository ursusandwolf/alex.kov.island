package com.island.controller;

import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.SimulationStatus;
import com.island.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for controlling the simulation state.
 */
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * Pauses the simulation.
     * 
     * @return the current simulation status
     */
    @PostMapping("/pause")
    public ResponseEntity<StatusResponse> pause() {
        simulationService.pause();
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Resumes the simulation.
     * 
     * @return the current simulation status
     */
    @PostMapping("/resume")
    public ResponseEntity<StatusResponse> resume() {
        simulationService.resume();
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Returns the current simulation status.
     * 
     * @return the current simulation status
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Returns a snapshot of the current world state.
     * 
     * @return the world snapshot
     */
    @GetMapping("/snapshot")
    public ResponseEntity<WorldSnapshot> getSnapshot() {
        return ResponseEntity.ok(simulationService.getSnapshot());
    }

    /**
     * DTO for simulation status responses.
     */
    public record StatusResponse(SimulationStatus status) {}
}
