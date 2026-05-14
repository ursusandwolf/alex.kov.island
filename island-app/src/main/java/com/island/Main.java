package com.island;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Island Ecosystem Simulation (Spring Boot).
 */
@SpringBootApplication(scanBasePackages = "com.island")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
