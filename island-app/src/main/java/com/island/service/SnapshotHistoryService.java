package com.island.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.engine.model.WorldSnapshot;
import com.island.persistence.SimulationSnapshotEntity;
import com.island.persistence.SimulationSnapshotRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service for saving and loading simulation snapshots using JPA persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotHistoryService {

    private final ObjectMapper objectMapper;
    private final SimulationService simulationService;
    private final SimulationSnapshotRepository repository;

    /**
     * Saves the current simulation snapshot to the database.
     * 
     * @return the generated filename
     */
    @Transactional
    public Optional<String> saveCurrentSnapshot() {
        Optional<WorldSnapshot> snapshotOpt = simulationService.getSnapshot();
        if (snapshotOpt.isEmpty()) {
            log.warn("No active simulation to snapshot");
            return Optional.empty();
        }

        WorldSnapshot snapshot = snapshotOpt.get();
        String filename = "snapshot_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        try {
            String content = objectMapper.writeValueAsString(snapshot);
            SimulationSnapshotEntity entity = SimulationSnapshotEntity.builder()
                    .filename(filename)
                    .createdAt(LocalDateTime.now())
                    .content(content)
                    .build();
            
            repository.save(entity);
            log.info("Saved historical snapshot '{}' to database", filename);
            return Optional.of(filename);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot", e);
            return Optional.empty();
        }
    }

    /**
     * Lists all saved snapshot filenames.
     */
    public List<String> listSnapshots() {
        return repository.findAll().stream()
                .map(SimulationSnapshotEntity::getFilename)
                .sorted()
                .toList();
    }

    /**
     * Loads a specific snapshot by filename from the database.
     */
    public Optional<WorldSnapshot> loadSnapshot(String filename) {
        return repository.findByFilename(filename)
                .flatMap(entity -> {
                    try {
                        return Optional.of(objectMapper.readValue(entity.getContent(), WorldSnapshot.class));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize snapshot '{}' from database", filename, e);
                        return Optional.empty();
                    }
                });
    }
}
