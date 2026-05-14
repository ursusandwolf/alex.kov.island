package com.island.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.engine.model.WorldSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.island.nature.model.Island;
import com.island.nature.model.IslandSnapshot;
import com.island.nature.model.CellSnapshot;
import com.island.nature.model.Cell;
import com.island.nature.service.StatisticsService;
import com.island.config.WorldSnapshotMixin;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SnapshotHistoryServiceTest {

    private SnapshotHistoryService snapshotHistoryService;
    private SimulationService simulationService;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        simulationService = mock(SimulationService.class);
        objectMapper = new ObjectMapper();
        objectMapper.addMixIn(WorldSnapshot.class, WorldSnapshotMixin.class); // Register mixin
        snapshotHistoryService = new SnapshotHistoryService(objectMapper, simulationService);
        ReflectionTestUtils.setField(snapshotHistoryService, "historyDir", tempDir.toString());
        snapshotHistoryService.init();
    }

    @Test
    void testSaveSnapshotWithoutActiveSimulation() {
        when(simulationService.getSnapshot()).thenReturn(Optional.empty());
        Optional<String> result = snapshotHistoryService.saveCurrentSnapshot();
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveAndListSnapshots() throws IOException {
        Island mockIsland = mock(Island.class);
        StatisticsService mockStats = mock(StatisticsService.class);
        when(mockIsland.getWidth()).thenReturn(20);
        when(mockIsland.getHeight()).thenReturn(20);
        when(mockIsland.getStatisticsService()).thenReturn(mockStats);
        when(mockIsland.getSpeciesCounts()).thenReturn(Collections.emptyMap());
        when(mockIsland.getCell(anyInt(), anyInt())).thenReturn(mock(Cell.class));
        
        IslandSnapshot testSnapshot = new IslandSnapshot(mockIsland);
        when(simulationService.getSnapshot()).thenReturn(Optional.of(testSnapshot));

        Optional<String> filename = snapshotHistoryService.saveCurrentSnapshot();
        assertTrue(filename.isPresent());
        assertTrue(Files.exists(tempDir.resolve(filename.get())));
        
        var list = snapshotHistoryService.listSnapshots();
        assertEquals(1, list.size());
        assertEquals(filename.get(), list.get(0));
    }

    @Test
    void testLoadSnapshotSuccess() throws IOException {
        IslandSnapshot testSnapshot = new IslandSnapshot();
        testSnapshot.setWidth(20);
        testSnapshot.setHeight(20);
        testSnapshot.setTickCount(100);
        testSnapshot.setMetrics(Collections.emptyMap());
        testSnapshot.setNodes(new CellSnapshot[20][20]);
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                CellSnapshot cell = new CellSnapshot();
                cell.setCoordinates(x + "," + y);
                testSnapshot.getNodes()[x][y] = cell;
            }
        }

        Path path = tempDir.resolve("test.json");
        // Use the WorldSnapshot type during serialization to ensure type info is included
        objectMapper.writerFor(WorldSnapshot.class).writeValue(path.toFile(), testSnapshot);

        // Point the service to our temp file
        Optional<WorldSnapshot> loaded = snapshotHistoryService.loadSnapshot("test.json");
        
        assertTrue(loaded.isPresent(), "Snapshot should be loaded");
        assertEquals(20, loaded.get().getWidth());
        assertEquals(100, loaded.get().getTickCount());
        assertInstanceOf(IslandSnapshot.class, loaded.get());
        assertNotNull(loaded.get().getNodeSnapshot(0, 0));
        assertEquals("0,0", loaded.get().getNodeSnapshot(0, 0).getCoordinates());
    }

    @Test
    void testLoadNonExistentSnapshotReturnsEmpty() {
        Optional<WorldSnapshot> loaded = snapshotHistoryService.loadSnapshot("nonexistent.json");
        assertTrue(loaded.isEmpty());
    }

    @Test
    void testLoadInvalidFilenameReturnsEmpty() {
        Optional<WorldSnapshot> loaded = snapshotHistoryService.loadSnapshot("../malicious.json");
        assertTrue(loaded.isEmpty());
    }
}
