package com.island.content;

/**
 * Marker interface for Herbivore animals.
 * Herbivores move and act after Predators in the simulation tick.
 * They primarily eat plants, but some (like Duck) can also eat insects (Caterpillar).
 */
public interface Herbivore {
    // Marker interface, no methods needed yet.
    // Used by SimulationEngine to sort execution order.
}
