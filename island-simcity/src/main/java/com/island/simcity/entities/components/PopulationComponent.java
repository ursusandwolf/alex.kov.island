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
    private int age;
    private int happiness;

    public static PopulationComponentBuilder builder() {
        return new PopulationComponentBuilder();
    }

    public static class PopulationComponentBuilder {
        private int age;
        private int happiness;

        public PopulationComponentBuilder age(int age) {
            this.age = age;
            return this;
        }

        public PopulationComponentBuilder happiness(int happiness) {
            this.happiness = happiness;
            return this;
        }

        public PopulationComponent build() {
            return new PopulationComponent(age, happiness);
        }
    }

    public int getAge() { return age; }
    public int getHappiness() { return happiness; }

    public void setAge(int age) { this.age = age; }
    public void setHappiness(int happiness) { this.happiness = happiness; }

    public void updateHappiness(int delta) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + delta));
    }
}
