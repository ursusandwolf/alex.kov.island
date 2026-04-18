package com.island.model;

import com.island.content.Organism;
import com.island.content.Animal;
import com.island.content.Plant;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a single cell (location) on the island.
 * Thread-safe implementation using ReentrantLock for concurrent access.
 * 
 * Contains:
 * - List of animals in this cell
 * - List of plants in this cell
 * - Terrain type (future extension)
 * 
 * GOF Patterns:
 * - Mediator: coordinates interactions between organisms in same cell
 * 
 * GRASP Principles:
 * - Information Expert: cell knows its contents
 * - High Cohesion: all cell-related data is here
 * - Low Coupling: minimal dependencies on other classes
 */
public class Cell {
    
    // Cell coordinates
    private final int x;
    private final int y;
    
    // Thread-safe lists for concurrent access
    // CopyOnWriteArrayList allows iteration without locking during reads
    private final List<Animal> animals;
    private final List<Plant> plants;
    
    // Lock for write operations (adding/removing organisms)
    private final ReentrantLock lock;
    
    // Total plant biomass in this cell (cached for performance)
    private double totalPlantBiomass;
    
    /**
     * Create a new cell at specified coordinates.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.animals = new CopyOnWriteArrayList<>();
        this.plants = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.totalPlantBiomass = 0;
    }
    
    /**
     * Get X coordinate.
     * @return x
     */
    public int getX() {
        return x;
    }
    
    /**
     * Get Y coordinate.
     * @return y
     */
    public int getY() {
        return y;
    }
    
    /**
     * Get cell coordinates as string "x,y".
     * @return coordinate string
     */
    public String getCoordinates() {
        return x + "," + y;
    }
    
    /**
     * Get lock for thread-safe operations.
     * @return the ReentrantLock
     */
    public ReentrantLock getLock() {
        return lock;
    }
    
    // ==================== ANIMAL OPERATIONS ====================
    
    /**
     * Add animal to this cell.
     * Thread-safe operation.
     * 
     * @param animal the animal to add
     * @return true if added successfully
     */
    public boolean addAnimal(Animal animal) {
        lock.lock();
        try {
            // Check species capacity limit
            String speciesKey = animal.getSpeciesKey();
            long countOfSpecies = animals.stream()
                .filter(a -> a.getSpeciesKey().equals(speciesKey))
                .count();
            
            if (countOfSpecies >= animal.getMaxPerCell()) {
                return false; // Capacity reached
            }
            
            animals.add(animal);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remove animal from this cell.
     * Thread-safe operation.
     * 
     * @param animal the animal to remove
     * @return true if removed
     */
    public boolean removeAnimal(Animal animal) {
        lock.lock();
        try {
            return animals.remove(animal);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get all animals in this cell.
     * Returns snapshot - safe to iterate without locking.
     * 
     * @return list of animals
     */
    public List<Animal> getAnimals() {
        return new CopyOnWriteArrayList<>(animals); // Return defensive copy
    }
    
    /**
     * Get animals of specific species in this cell.
     * 
     * @param speciesKey the species to filter by
     * @return list of matching animals
     */
    public List<Animal> getAnimalsBySpecies(String speciesKey) {
        return animals.stream()
            .filter(a -> a.getSpeciesKey().equals(speciesKey))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Count animals of specific species.
     * 
     * @param speciesKey the species
     * @return count
     */
    public int countAnimalsBySpecies(String speciesKey) {
        return (int) animals.stream()
            .filter(a -> a.getSpeciesKey().equals(speciesKey))
            .count();
    }
    
    /**
     * Get total animal count in this cell.
     * 
     * @return count
     */
    public int getAnimalCount() {
        return animals.size();
    }
    
    // ==================== PLANT OPERATIONS ====================
    
    /**
     * Add plant to this cell.
     * Thread-safe operation.
     * 
     * @param plant the plant to add
     * @return true if added successfully
     */
    public boolean addPlant(Plant plant) {
        lock.lock();
        try {
            plants.add(plant);
            totalPlantBiomass += plant.getBiomass();
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remove plant from this cell.
     * Thread-safe operation.
     * 
     * @param plant the plant to remove
     * @return true if removed
     */
    public boolean removePlant(Plant plant) {
        lock.lock();
        try {
            boolean removed = plants.remove(plant);
            if (removed) {
                totalPlantBiomass -= plant.getBiomass();
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get all plants in this cell.
     * Returns snapshot - safe to iterate.
     * 
     * @return list of plants
     */
    public List<Plant> getPlants() {
        return new CopyOnWriteArrayList<>(plants);
    }
    
    /**
     * Get total plant biomass in this cell.
     * 
     * @return biomass in kg
     */
    public double getTotalPlantBiomass() {
        return totalPlantBiomass;
    }
    
    /**
     * Get plant count in this cell.
     * 
     * @return count
     */
    public int getPlantCount() {
        return plants.size();
    }
    
    /**
     * Grow all plants in this cell.
     * Called during plant growth phase.
     */
    public void growPlants() {
        for (Plant plant : plants) {
            if (plant.isAlive()) {
                plant.grow();
            }
        }
        // Recalculate total biomass
        totalPlantBiomass = plants.stream()
            .filter(Plant::isAlive)
            .mapToDouble(Plant::getBiomass)
            .sum();
    }
    
    /**
     * Remove dead organisms from this cell.
     * Should be called at end of each tick.
     */
    public void cleanupDeadOrganisms() {
        lock.lock();
        try {
            // Remove dead animals
            animals.removeIf(animal -> !animal.isAlive());
            
            // Remove dead plants and update biomass
            plants.removeIf(plant -> {
                if (!plant.isAlive()) {
                    totalPlantBiomass -= plant.getBiomass();
                    return true;
                }
                return false;
            });
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get statistics for this cell.
     * 
     * @return formatted statistics string
     */
    public String getStatistics() {
        return String.format("Cell[%d,%d]: Animals=%d, Plants=%d, Biomass=%.2fkg",
            x, y, animals.size(), plants.size(), totalPlantBiomass);
    }
}
