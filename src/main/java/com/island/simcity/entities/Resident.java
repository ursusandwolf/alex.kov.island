package com.island.simcity.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Resident extends SimEntity {
    private int happiness = 100;
    private int age = 0;

    @Override
    public String getTypeName() {
        return "Resident";
    }

    public void updateHappiness(int delta) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + delta));
    }
}
