package com.island.model;

import com.island.content.Animal;
import com.island.content.Plant;
import com.island.content.SpeciesConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * This class is responsible for:
 * - Storing the grid of cells
 * - Managing chunk-based multithreading
 * - Handling organism movement between cells
 * - Initialization of the world with random organisms
 * 
 * GOF Patterns:
 * - Composite: Island is composed of Cells
 * - Factory: Uses AnimalFactory to create organisms during initialization
 * 
 * GRASP Principles:
 * - Information Expert: Island knows its structure and coordinates
 * - Creator: Creates and initializes cells and organisms
 * - Controller: Coordinates high-level simulation operations
 */
public class Island {
    
    private final int width;
    private final int height;
    private final Cell[][] grid;
    
    // Chunk management for multithreading
    // TODO: Implement chunk division based on available CPU cores
    // Hint: For 100x20 map, you might create 4-8 chunks depending on core count
    private final int chunkSizeX;
    private final int chunkSizeY;
    private final List<Chunk> chunks;
    
    /**
     * Create island with specified dimensions.
     * 
     * @param width width in cells (e.g., 100)
     * @param height height in cells (e.g., 20)
     */
    public Island(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
        
        // TODO: Calculate optimal chunk size based on world dimensions
        // Suggestion: chunks should be roughly square and divide evenly
        // Example: for 100x20, use 5 chunks of 20x4 or 10 chunks of 10x2
        this.chunkSizeX = width / 5; // TODO: Make configurable
        this.chunkSizeY = height / 2; // TODO: Make configurable
        this.chunks = new ArrayList<>();
        
        initializeGrid();
        initializeChunks();
    }
    
    /**
     * Initialize all cells in the grid.
     */
    private void initializeGrid() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new Cell(x, y);
            }
        }
    }
    
    /**
     * Initialize chunks for multithreaded processing.
     * TODO: Implement proper chunk creation logic
     */
    private void initializeChunks() {
        // TODO: Divide the island into chunks
        // Each chunk should know its bounds (startX, endX, startY, endY)
        // Chunks will be processed by separate threads
        /*
        for (int chunkX = 0; chunkX < width / chunkSizeX; chunkX++) {
            for (int chunkY = 0; chunkY < height / chunkSizeY; chunkY++) {
                int startX = chunkX * chunkSizeX;
                int startY = chunkY * chunkSizeY;
                int endX = Math.min(startX + chunkSizeX, width);
                int endY = Math.min(startY + chunkSizeY, height);
                
                Chunk chunk = new Chunk(chunkX, chunkY, startX, endX, startY, endY, this);
                chunks.add(chunk);
            }
        }
        */
    }
    
    /**
     * Get cell at specified coordinates with wrap-around (toroidal world).
     * 
     * @param x x-coordinate (can be negative or >= width)
     * @param y y-coordinate (can be negative or >= height)
     * @return the cell at wrapped coordinates
     */
    public Cell getCell(int x, int y) {
        // Wrap around using modulo operator
        // Handle negative coordinates properly
        int wrappedX = ((x % width) + width) % width;
        int wrappedY = ((y % height) + height) % height;
        return grid[wrappedX][wrappedY];
    }
    
    /**
     * Get cell without wrap-around (for internal use).
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @return the cell, or null if out of bounds
     */
    public Cell getCellUnsafe(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return grid[x][y];
    }
    
    /**
     * Move an organism from one cell to an adjacent cell.
     * Thread-safe operation that handles lock ordering to prevent deadlocks.
     * 
     * TODO: Implement safe movement between cells
     * - Lock source and destination cells in consistent order (by coordinates)
     * - Remove organism from source cell
     * - Add organism to destination cell
     * 
     * @param organism the organism to move
     * @param fromX source x coordinate
     * @param fromY source y coordinate
     * @param toX destination x coordinate
     * @param toY destination y coordinate
     * @return true if moved successfully
     */
    public boolean moveOrganism(Animal organism, int fromX, int fromY, int toX, int toY) {
        // TODO: Implement thread-safe movement
        // Important: Always lock cells in consistent order to prevent deadlocks
        // Strategy: Lock the cell with smaller (x,y) coordinates first
        
        /*
        Cell fromCell = getCell(fromX, fromY);
        Cell toCell = getCell(toX, toY);
        
        if (fromCell == null || toCell == null) {
            return false;
        }
        
        // Determine lock order to prevent deadlocks
        Cell firstLock, secondLock;
        if (fromCell.getX() < toCell.getX() || 
            (fromCell.getX() == toCell.getX() && fromCell.getY() < toCell.getY())) {
            firstLock = fromCell;
            secondLock = toCell;
        } else {
            firstLock = toCell;
            secondLock = fromCell;
        }
        
        firstLock.getLock().lock();
        try {
            secondLock.getLock().lock();
            try {
                // Perform the move
                boolean removed = fromCell.removeAnimal(organism);
                if (!removed) {
                    return false; // Organism not in source cell
                }
                
                boolean added = toCell.addAnimal(organism);
                if (!added) {
                    // Failed to add to destination (capacity?), put back
                    fromCell.addAnimal(organism);
                    return false;
                }
                
                return true;
            } finally {
                secondLock.getLock().unlock();
            }
        } finally {
            firstLock.getLock().unlock();
        }
        */
        
        return false; // TODO: Replace with actual implementation
    }
    
    /**
     * Initialize the island with random organisms.
     * Each cell gets 15-45% of max capacity for each species.
     * 
     * TODO: Implement initialization logic
     * - Iterate through all cells
     * - For each species, generate random count (15-45% of max per cell)
     * - Create organisms with unique IDs and full energy
     * - Add plants to each cell
     * 
     * @param config species configuration
     */
    public void initializeWorld(SpeciesConfig config) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // TODO: Loop through all cells and populate with organisms
        /*
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = grid[x][y];
                
                // For each animal species
                for (String speciesKey : config.getAllSpeciesKeys()) {
                    int maxPerCell = config.getMaxPerCell(speciesKey);
                    
                    // Generate random count: 15-45% of max
                    int minCount = (int) Math.ceil(maxPerCell * 0.15);
                    int maxCount = (int) Math.floor(maxPerCell * 0.45);
                    int count = random.nextInt(minCount, maxCount + 1);
                    
                    // Create organisms
                    for (int i = 0; i < count; i++) {
                        Animal animal = AnimalFactory.createAnimal(speciesKey);
                        if (animal != null) {
                            // Assign unique ID
                            animal.setId(generateUniqueId());
                            // Set full energy (max energy from config)
                            animal.setEnergy(config.getFullSatietyKg(speciesKey) * 10); // TODO: Define max energy formula
                            cell.addAnimal(animal);
                        }
                    }
                }
                
                // Add plants to cell
                // TODO: Add initial plant biomass
                int plantCount = random.nextInt(10, 50);
                for (int i = 0; i < plantCount; i++) {
                    Plant plant = new Plant();
                    cell.addPlant(plant);
                }
            }
        }
        */
    }
    
    /**
     * Generate unique ID for organism.
     * TODO: Implement proper ID generation (atomic counter or UUID)
     * 
     * @return unique ID
     */
    private long generateUniqueId() {
        // TODO: Use AtomicLong for thread-safe ID generation
        // static private AtomicLong idCounter = new AtomicLong(0);
        // return idCounter.incrementAndGet();
        return System.nanoTime(); // Temporary placeholder
    }
    
    /**
     * Get all chunks for parallel processing.
     * 
     * @return list of chunks
     */
    public List<Chunk> getChunks() {
        return new ArrayList<>(chunks);
    }
    
    /**
     * Get island width.
     * 
     * @return width in cells
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Get island height.
     * 
     * @return height in cells
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Get total organism count across all cells.
     * 
     * @return total count
     */
    public int getTotalOrganismCount() {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                count += grid[x][y].getAnimalCount();
                count += grid[x][y].getPlantCount();
            }
        }
        return count;
    }
    
    /**
     * Get statistics for entire island.
     * 
     * @return formatted statistics string
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Island[%dx%d] Total organisms: %d\n", width, height, getTotalOrganismCount()));
        
        // TODO: Aggregate statistics per species
        // Hint: Iterate through all cells and count by species
        
        return sb.toString();
    }
}
