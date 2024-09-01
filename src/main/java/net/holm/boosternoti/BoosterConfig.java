package net.holm.boosternoti;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BoosterConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/iblocky_boosternoti.json");
    private static final Logger LOGGER = LoggerFactory.getLogger(BoosterConfig.class);

    public int windowX = 100; // Default X position
    public int windowY = 100; // Default Y position

    // Load configuration from file
    public static BoosterConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, BoosterConfig.class);
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.error("Failed to load config file: {}", e.getMessage(), e);
            }
        }
        return new BoosterConfig(); // Return default config if file does not exist
    }

    // Save configuration to file
    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: {}", e.getMessage(), e);
        }
    }
}