package com.island.controller;

import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.SimulationStatus;
import com.island.service.SimulationService;
import com.island.service.SnapshotHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@Validated
@Tag(name = "Simulation", description = "Endpoints for managing the simulation lifecycle and world state. NOTE: Supports single-user concurrent simulations.")
public class SimulationController {

    private final SimulationService simulationService;
    private final SnapshotHistoryService historyService;

    /**
     * Starts a new simulation, replacing any currently running one.
     */
    @Operation(summary = "Start simulation", description = "Initializes and starts a new simulation with the given parameters.")
    @ApiResponse(responseCode = "200", description = "Simulation started successfully")
    @PostMapping("/start")
    public ResponseEntity<StatusResponse> start(
            @Parameter(description = "Type of simulation") @RequestParam(defaultValue = "NATURE") SimulationType type,
            @Parameter(description = "Grid width") @RequestParam(defaultValue = "20") @Min(5) @Max(200) int width,
            @Parameter(description = "Grid height") @RequestParam(defaultValue = "20") @Min(5) @Max(200) int height,
            @Parameter(description = "Tick duration in ms") @RequestParam(defaultValue = "100") @Min(10) @Max(10000) int tickMs) {
        simulationService.start(type, width, height, tickMs);
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Starts a new simulation from a historical snapshot, replacing any currently running one.
     */
    @Operation(summary = "Start from snapshot", description = "Loads a saved snapshot and starts a simulation from its state.")
    @ApiResponse(responseCode = "200", description = "Simulation started from snapshot")
    @ApiResponse(responseCode = "404", description = "Snapshot file not found")
    @PostMapping("/start-from-snapshot")
    public ResponseEntity<StatusResponse> startFromSnapshot(
            @Parameter(description = "Filename of the snapshot") @RequestParam String filename,
            @Parameter(description = "Type of simulation") @RequestParam(defaultValue = "NATURE") SimulationType type,
            @Parameter(description = "Tick duration in ms") @RequestParam(defaultValue = "100") @Min(10) @Max(10000) int tickMs) {
        return historyService.loadSnapshot(filename)
                .map(snapshot -> {
                    simulationService.startFromSnapshot(snapshot, type, tickMs);
                    return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Stops the simulation game loop.
     */
    @Operation(summary = "Stop simulation", description = "Permanently stops the simulation game loop.")
    @PostMapping("/stop")
    public ResponseEntity<StatusResponse> stop() {
        simulationService.stop();
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Pauses the simulation.
     */
    @Operation(summary = "Pause simulation", description = "Pauses the execution of ticks.")
    @PostMapping("/pause")
    public ResponseEntity<StatusResponse> pause() {
        simulationService.pause();
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Resumes the simulation.
     */
    @Operation(summary = "Resume simulation", description = "Resumes the execution of ticks after a pause.")
    @PostMapping("/resume")
    public ResponseEntity<StatusResponse> resume() {
        simulationService.resume();
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Returns the current simulation status.
     */
    @Operation(summary = "Get status", description = "Returns the current status of the simulation (RUNNING, PAUSED, IDLE).")
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        return ResponseEntity.ok(new StatusResponse(simulationService.getStatus()));
    }

    /**
     * Returns a snapshot of the current world state.
     */
    @Operation(summary = "Get snapshot", description = "Captures and returns the current state of the simulation world.")
    @ApiResponse(responseCode = "200", description = "Snapshot captured")
    @ApiResponse(responseCode = "204", description = "No simulation running")
    @GetMapping("/snapshot")
    public ResponseEntity<WorldSnapshot> getSnapshot() {
        return simulationService.getSnapshot()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Saves the current simulation snapshot to disk.
     */
    @Operation(summary = "Save snapshot", description = "Saves the current world state to a JSON file on disk.")
    @ApiResponse(responseCode = "200", description = "Snapshot saved")
    @ApiResponse(responseCode = "500", description = "Failed to save snapshot")
    @PostMapping("/snapshot/save")
    public ResponseEntity<String> saveSnapshot() {
        return historyService.saveCurrentSnapshot()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.internalServerError().build());
    }

    /**
     * Lists all saved historical snapshots.
     */
    @Operation(summary = "List history", description = "Returns a list of all saved snapshot filenames.")
    @GetMapping("/snapshot/history")
    public ResponseEntity<SnapshotListResponse> listSnapshots() {
        List<String> snapshots = historyService.listSnapshots();
        return ResponseEntity.ok(new SnapshotListResponse(snapshots, snapshots.size()));
    }

    /**
     * Loads a specific historical snapshot by filename.
     */
    @Operation(summary = "Get historical snapshot", description = "Retrieves the contents of a specific saved snapshot.")
    @ApiResponse(responseCode = "200", description = "Snapshot found")
    @ApiResponse(responseCode = "404", description = "Snapshot not found")
    @GetMapping("/snapshot/history/{filename}")
    public ResponseEntity<WorldSnapshot> getHistoricalSnapshot(
            @Parameter(description = "Filename of the snapshot") @PathVariable String filename) {
        return historyService.loadSnapshot(filename)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DTO for simulation status responses.
     */
    @Schema(description = "Response containing the simulation status.")
    public record StatusResponse(@Schema(description = "Current status") SimulationStatus status) {}
}
