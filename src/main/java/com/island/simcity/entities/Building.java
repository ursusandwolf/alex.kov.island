package com.island.simcity.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Building extends SimEntity {
    public enum Type { RESIDENTIAL, COMMERCIAL, INDUSTRIAL, ROAD }
    
    private final Type type;
    private int level = 1;

    public Building(Type type) {
        this.type = type;
    }

    @Override
    public String getTypeName() {
        return type.name();
    }
}
