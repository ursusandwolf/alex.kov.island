package com.island.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chunk of the island for parallel processing.
 * A chunk is a rectangular region of cells that can be processed by a single thread.
 * 
 * The ChunkManager assigns chunks to threads in the worker pool.
 * Each chunk processes all organisms within its bounds during a simulation tick.
 * 
 * TODO: Implement chunk-based processing logic
 * - Store references to cells within chunk bounds
 * - Execute simulation phases (eat, move, reproduce, checkState) for all organisms in chunk
 * - Handle boundary interactions with neighboring chunks
 * 
 * GOF Patterns:
 * - Composite: Chunk contains multiple Cells
 * - Command: Chunk can be submitted as a task to executor pool
 * 
 * GRASP Principles:
 * - Information Expert: Chunk knows its cells and bounds
 * - Low Coupling: Minimal dependencies on other chunks
 */
public class Chunk {
    
    private final int chunkIdX;
    private final int chunkIdY;
    private final int startX;
    private final int endX; // Exclusive
    private final int startY;
    private final int endY; // Exclusive
    private final Island island;
    
    // List of cells in this chunk (cached for fast iteration)
    private final List<Cell> cells;
    
    /**
     * Create a new chunk.
     * 
     * @param chunkIdX chunk ID in X direction
     * @param chunkIdY chunk ID in Y direction
     * @param startX start X coordinate (inclusive)
     * @param endX end X coordinate (exclusive)
     * @param startY start Y coordinate (inclusive)
     * @param endY end Y coordinate (exclusive)
     * @param island reference to parent island
     */
    public Chunk(int chunkIdX, int chunkIdY, int startX, int endX, int startY, int endY, Island island) {
        this.chunkIdX = chunkIdX;
        this.chunkIdY = chunkIdY;
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.island = island;
        this.cells = new ArrayList<>();
        
        initializeCells();
    }
    
    /**
     * Initialize list of cells in this chunk.
     */
    private void initializeCells() {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                Cell cell = island.getCellUnsafe(x, y);
                if (cell != null) {
                    cells.add(cell);
                }
            }
        }
    }
    
    /**
     * Process all organisms in this chunk for one simulation tick.
     * This method is called by a worker thread from the thread pool.
     * 
     * TODO: Implement the 4-phase simulation for this chunk
     * Phase 1: Eat - all organisms attempt to eat (predators first, then herbivores)
     * Phase 2: Move - organisms move to adjacent cells
     * Phase 3: Reproduce - organisms with partners create offspring
     * Phase 4: Check State - remove dead organisms, update statistics
     * 
     * Important: When organisms move to adjacent chunks, coordinate with neighbor chunks
     */
    public void processTick() {
        // TODO: Implement 4-phase processing
        
        /*
        // Phase 1: Eating
        // Sort organisms by priority: predators first (by speed), then herbivores
        List<Animal> allAnimals = collectAllAnimals();
        allAnimals.sort((a1, a2) -> {
            // Predators before herbivores
            boolean isPredator1 = a1 instanceof Predator;
            boolean isPredator2 = a2 instanceof Predator;
            
            if (isPredator1 && !isPredator2) return -1;
            if (!isPredator1 && isPredator2) return 1;
            
            // Within same type, faster animals go first
            return Integer.compare(a2.getSpeed(), a1.getSpeed());
        });
        
        for (Animal animal : allAnimals) {
            if (animal.isAlive()) {
                animal.eat(getCellForOrganism(animal));
            }
        }
        
        // Phase 2: Movement
        for (Animal animal : allAnimals) {
            if (animal.isAlive()) {
                animal.move(this);
            }
        }
        
        // Phase 3: Reproduction
        for (Animal animal : allAnimals) {
            if (animal.isAlive()) {
                animal.reproduce(getCellForOrganism(animal));
            }
        }
        
        // Phase 4: Cleanup dead organisms
        for (Cell cell : cells) {
            cell.cleanupDeadOrganisms();
        }
        */
    }
    
    /**
     * Collect all animals from all cells in this chunk.
     * 
     * @return list of all animals
     */
    private List<Animal> collectAllAnimals() {
        List<Animal> allAnimals = new ArrayList<>();
        for (Cell cell : cells) {
            allAnimals.addAll(cell.getAnimals());
        }
        return allAnimals;
    }
    
    /**
     * Get the cell containing a specific organism.
     * 
     * @param organism the organism
     * @return the cell, or null if not found in this chunk
     */
    private Cell getCellForOrganism(Animal organism) {
        // TODO: Find which cell contains this organism
        // Hint: Organism should have current cell reference
        return null; // Placeholder
    }
    
    /**
     * Get number of cells in this chunk.
     * 
     * @return cell count
     */
    public int getCellCount() {
        return cells.size();
    }
    
    /**
     * Get chunk coordinates as string.
     * 
     * @return formatted string
     */
    @Override
    public String toString() {
        return String.format("Chunk[%d,%d] cells=%d", chunkIdX, chunkIdY, cells.size());
    }
}
