package com.island.controller;

import com.island.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for controlling the simulation.
 */
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {
    private final SimulationService simulationService;

    @PostMapping("/start")
    public Map<String, String> start(@RequestParam(defaultValue = "nature") String type) {
        simulationService.startSimulation(type);
        return Map.of("message", "Simulation " + type + " started", "status", "RUNNING");
    }

    @PostMapping("/pause")
    public Map<String, String> pause() {
        simulationService.pause();
        return Map.of("message", "Simulation paused", "status", "PAUSED");
    }

    @PostMapping("/resume")
    public Map<String, String> resume() {
        simulationService.resume();
        return Map.of("message", "Simulation resumed", "status", "RUNNING");
    }

    @PostMapping("/stop")
    public Map<String, String> stop() {
        simulationService.stop();
        return Map.of("message", "Simulation stopped", "status", "IDLE");
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of("status", simulationService.getStatus());
    }
}
