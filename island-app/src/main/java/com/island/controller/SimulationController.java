package com.island.controller;

import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.SimulationStatus;
import com.island.service.SimulationService;
import com.island.service.SnapshotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for controlling the simulation state and snapshots.
 */
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final SnapshotHistoryService historyService;

    /**
     * Starts a new simulation, replacing any currently running one.
     * 
     * @param type   simulation type (nature, simcity)
     * @param width  grid width (default 20)
     * @param height grid height (default 20)
     * @param tickMs tick duration in milliseconds (default 100)
     * @return the current simulation status
     */
    @PostMapping("/start")
    public ResponseEntity<StatusResponse> start(
            @RequestParam(defaultValue = "nature") String type,
            @RequestParam(defaultValue = "20") int width,
            @RequestParam(defaultValue = "20") int height,
            @RequestParam(defaultValue = "100") int tickMs) {
        simulationService.start(type, width, height, tickMs);
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Starts a new simulation from a historical snapshot, replacing any currently running one.
     * 
     * @param filename the snapshot filename
     * @param type     simulation type (nature, simcity)
     * @param tickMs   tick duration in milliseconds (default 100)
     * @return the current simulation status
     */
    @PostMapping("/start-from-snapshot")
    public ResponseEntity<StatusResponse> startFromSnapshot(
            @RequestParam String filename,
            @RequestParam(defaultValue = "nature") String type,
            @RequestParam(defaultValue = "100") int tickMs) {
        WorldSnapshot snapshot = historyService.loadSnapshot(filename);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        simulationService.startFromSnapshot(snapshot, type, tickMs);
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Stops the simulation game loop.
     * 
     * @return the current simulation status
     */
    @PostMapping("/stop")
    public ResponseEntity<StatusResponse> stop() {
        simulationService.stop();
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

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
        WorldSnapshot snapshot = simulationService.getSnapshot();
        if (snapshot == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Saves the current simulation snapshot to disk.
     *
     * @return the filename of the saved snapshot
     */
    @PostMapping("/snapshot/save")
    public ResponseEntity<String> saveSnapshot() {
        String filename = historyService.saveCurrentSnapshot();
        if (filename == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(filename);
    }

    /**
     * Lists all saved historical snapshots.
     *
     * @return list of filenames
     */
    @GetMapping("/snapshot/history")
    public ResponseEntity<List<String>> listSnapshots() {
        return ResponseEntity.ok(historyService.listSnapshots());
    }

    /**
     * Loads a specific historical snapshot by filename.
     *
     * @param filename the snapshot filename
     * @return the loaded world snapshot
     */
    @GetMapping("/snapshot/history/{filename}")
    public ResponseEntity<WorldSnapshot> getHistoricalSnapshot(@PathVariable String filename) {
        WorldSnapshot snapshot = historyService.loadSnapshot(filename);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * DTO for simulation status responses.
     */
    public record StatusResponse(SimulationStatus status) {}
}
