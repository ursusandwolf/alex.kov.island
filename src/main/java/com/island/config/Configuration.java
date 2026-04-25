package com.island.config;

import lombok.Getter;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class Configuration {
    private int islandWidth = 100;
    private int islandHeight = 20;
    private int tickDurationMs = 1000;

    public static Configuration load() {
        Configuration config = new Configuration();
        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream("species.properties")) {
            if (input == null) {
                return config;
            }
            Properties prop = new Properties();
            prop.load(input);
            // Загрузка параметров из файла
            config.islandWidth = Integer.parseInt(prop.getProperty("island.width", "100"));
            config.islandHeight = Integer.parseInt(prop.getProperty("island.height", "20"));
            config.tickDurationMs = Integer.parseInt(prop.getProperty("island.tickDurationMs", "1000"));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки конфигурации, используются значения по умолчанию: " + e.getMessage());
        }
        return config;
    }
}
