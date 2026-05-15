package com.island.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.engine.model.WorldSnapshot;
import com.island.persistence.SimulationSnapshotEntity;
import com.island.persistence.SimulationSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.island.nature.model.IslandSnapshot;
import com.island.nature.model.CellSnapshot;
import com.island.config.WorldSnapshotMixin;
import java.util.Optional;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SnapshotHistoryServiceTest {

    private SnapshotHistoryService snapshotHistoryService;
    private SimulationService simulationService;
    private SimulationSnapshotRepository repository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        simulationService = mock(SimulationService.class);
        repository = mock(SimulationSnapshotRepository.class);
        objectMapper = new ObjectMapper();
        objectMapper.addMixIn(WorldSnapshot.class, WorldSnapshotMixin.class);
        snapshotHistoryService = new SnapshotHistoryService(objectMapper, simulationService, repository);
    }

    @Test
    void testSaveSnapshotWithoutActiveSimulation() {
        when(simulationService.getSnapshot()).thenReturn(Optional.empty());
        Optional<String> result = snapshotHistoryService.saveCurrentSnapshot();
        assertTrue(result.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void testSaveAndListSnapshots() {
        IslandSnapshot testSnapshot = new IslandSnapshot();
        testSnapshot.setWidth(20);
        testSnapshot.setHeight(20);
        when(simulationService.getSnapshot()).thenReturn(Optional.of(testSnapshot));

        Optional<String> filename = snapshotHistoryService.saveCurrentSnapshot();
        assertTrue(filename.isPresent());
        
        ArgumentCaptor<SimulationSnapshotEntity> captor = ArgumentCaptor.forClass(SimulationSnapshotEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(filename.get(), captor.getValue().getFilename());
        assertNotNull(captor.getValue().getContent());
        
        when(repository.findAll()).thenReturn(List.of(captor.getValue()));
        var list = snapshotHistoryService.listSnapshots();
        assertEquals(1, list.size());
        assertEquals(filename.get(), list.get(0));
    }

    @Test
    void testLoadSnapshotSuccess() throws Exception {
        IslandSnapshot testSnapshot = new IslandSnapshot();
        testSnapshot.setWidth(20);
        testSnapshot.setHeight(20);
        testSnapshot.setTickCount(100);
        
        String json = objectMapper.writeValueAsString(testSnapshot);
        SimulationSnapshotEntity entity = SimulationSnapshotEntity.builder()
                .filename("test")
                .content(json)
                .build();
        
        when(repository.findByFilename("test")).thenReturn(Optional.of(entity));

        Optional<WorldSnapshot> loaded = snapshotHistoryService.loadSnapshot("test");
        
        assertTrue(loaded.isPresent());
        assertEquals(20, loaded.get().getWidth());
        assertEquals(100, loaded.get().getTickCount());
    }

    @Test
    void testLoadNonExistentSnapshotReturnsEmpty() {
        when(repository.findByFilename("nonexistent")).thenReturn(Optional.empty());
        Optional<WorldSnapshot> loaded = snapshotHistoryService.loadSnapshot("nonexistent");
        assertTrue(loaded.isEmpty());
    }
}
