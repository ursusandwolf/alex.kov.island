package com.island.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.engine.model.WorldSnapshot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for saving and loading simulation snapshots to/from the filesystem.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotHistoryService {

    private final ObjectMapper objectMapper;
    private final SimulationService simulationService;

    @Value("${sim.history.dir:data/snapshots}")
    private String historyDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(historyDir));
        } catch (IOException e) {
            log.error("Could not create history directory at {}", historyDir, e);
        }
    }

    /**
     * Saves the current simulation snapshot to a JSON file.
     * 
     * @return the generated filename, or null if failed
     */
    public String saveCurrentSnapshot() {
        WorldSnapshot snapshot = simulationService.getSnapshot();
        if (snapshot == null) {
            log.warn("No active simulation to snapshot");
            return null;
        }

        String filename = "snapshot_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        Path path = Paths.get(historyDir, filename);

        try {
            objectMapper.writeValue(path.toFile(), snapshot);
            log.info("Saved historical snapshot to {}", path);
            return filename;
        } catch (IOException e) {
            log.error("Failed to serialize and save snapshot", e);
            return null;
        }
    }

    /**
     * Lists all saved snapshot filenames.
     */
    public List<String> listSnapshots() {
        try (var stream = Files.list(Paths.get(historyDir))) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list snapshots in {}", historyDir, e);
            return List.of();
        }
    }

    /**
     * Loads a specific snapshot by filename.
     */
    public WorldSnapshot loadSnapshot(String filename) {
        Path path = Paths.get(historyDir, filename).normalize();
        if (!path.startsWith(Paths.get(historyDir).normalize())) {
            log.warn("Invalid snapshot filename or path traversal attempt: {}", filename);
            return null;
        }

        if (!Files.exists(path)) {
            log.warn("Snapshot file not found: {}", path);
            return null;
        }

        try {
            return objectMapper.readValue(path.toFile(), WorldSnapshot.class);
        } catch (IOException e) {
            log.error("Failed to deserialize snapshot from {}", path, e);
            return null;
        }
    }
}
