package com.island.simcity.entities;

import com.island.engine.Mortal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SimEntity implements Mortal {
    private boolean alive = true;

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void die() {
        this.alive = false;
    }

    @Override
    public abstract String getTypeName();
}
