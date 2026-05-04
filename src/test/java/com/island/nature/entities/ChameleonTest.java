package com.island.nature.entities;

import static com.island.nature.config.SimulationConstants.SCALE_1M;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.entities.predators.Chameleon;
import com.island.util.DefaultRandomProvider;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class ChameleonTest {

    @Test
    void testChameleonInvisibilityRate() {
        AnimalType type = AnimalType.builder()
                .speciesKey(SpeciesKey.CHAMELEON)
                .typeName("chameleon")
                .weight((long) (0.1 * SCALE_1M))
                .maxPerCell(300)
                .speed(1)
                .foodForSaturation((long) (0.02 * SCALE_1M))
                .maxEnergy((long) (0.02 * SCALE_1M))
                .maxLifespan(100)
                .huntProbabilities(new HashMap<>())
                .isColdBlooded(true)
                .presenceChance(30)
                .settlementBase((long) (0.3 * SCALE_1M))
                .settlementRange((long) (0.2 * SCALE_1M))
                .build();
                
        Chameleon chameleon = new Chameleon(type, new DefaultRandomProvider());

        int protectedCount = 0;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            if (chameleon.isProtected(1)) {
                protectedCount++;
            }
        }

        double rate = (double) protectedCount / iterations;
        
        // Should be around 0.95. Allowing 3% margin for randomness.
        System.out.println("Chameleon protection rate: " + rate);
        assertTrue(rate > 0.92, "Protection rate should be >= 95%, but was " + rate);
    }
}
