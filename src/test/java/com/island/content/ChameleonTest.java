package com.island.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.content.animals.predators.Chameleon;
import org.junit.jupiter.api.Test;
import java.util.HashMap;

public class ChameleonTest {

    @Test
    void testChameleonInvisibilityRate() {
        AnimalType type = AnimalType.builder()
                .speciesKey(SpeciesKey.CHAMELEON)
                .typeName("chameleon")
                .weight(0.1)
                .maxPerCell(300)
                .speed(1)
                .foodForSaturation(0.02)
                .maxEnergy(0.02)
                .maxLifespan(100)
                .huntProbabilities(new HashMap<>())
                .isColdBlooded(true)
                .presenceProb(0.3)
                .settlementBase(0.3)
                .settlementRange(0.2)
                .build();
                
        Chameleon chameleon = new Chameleon(type);

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
