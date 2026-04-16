package io.github.lisu.evoisle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @Test
    void greetingShouldBeCorrect() {
        assertEquals("Hello world!", Main.getGreeting());
    }
}