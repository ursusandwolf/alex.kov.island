package com.island.simcity.entities.components;

import com.island.engine.ecs.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Component for population-related data in SimCity.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopulationComponent implements Component {
    public enum WealthLevel {
        POOR,
        MIDDLE,
        WEALTHY
    }

    private int age;
    private int happiness;
    private int education; // Education Quotient (EQ), 0-200
    private int health;    // Health level, 0-100
    @Builder.Default
    private WealthLevel wealth = WealthLevel.POOR;

    public void updateHappiness(int delta) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + delta));
    }
}
