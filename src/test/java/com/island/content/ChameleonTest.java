package com.island.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.content.animals.predators.Chameleon;
import org.junit.jupiter.api.Test;
import java.util.HashMap;

public class ChameleonTest {

    @Test
    void testChameleonInvisibilityRate() {
        AnimalType type = new AnimalType(
            SpeciesKey.CHAMELEON, "chameleon", 0.1, 300, 1, 0.02, 100, new HashMap<>()
        );
        Chameleon chameleon = new Chameleon(type);

        int protectedCount = 0;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            if (chameleon.isProtected(1)) {
                protectedCount++;
            }
        }

        double rate = (double) protectedCount / iterations;
        
        // Should be around 0.80. Allowing 5% margin for randomness.
        System.out.println("Chameleon protection rate: " + rate);
        assertTrue(rate > 0.75 && rate < 0.85, "Protection rate should be ~80%, but was " + rate);
    }
}
