package com.island.content;

public interface Reproducible<T extends Organism> {
    T reproduce();
}
