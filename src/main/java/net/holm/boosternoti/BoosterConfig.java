package net.holm.boosternoti;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoosterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config/iblocky_boosternoti.json");
    private static final Logger LOGGER = LoggerFactory.getLogger(BoosterConfig.class);

    public int BoosterStatusWindowX = 100; // Default X position for BoosterStatusWindow
    public int BoosterStatusWindowY = 100; // Default Y position for BoosterStatusWindow
    public int enchantHUDWindowX = 0; // Default X position for EnchantHUD
    public int enchantHUDWindowY = 0; // Default Y position for EnchantHUD
    public int initialFetchDelaySeconds = 30; // Default fetch delay
    public int fetchIntervalSeconds = 120; // Default fetch interval
    public boolean showInstructions = true; // Default show instructions state

    private final Map<UUID, String> manualRanks = new HashMap<>();

    private static BoosterConfig instance;

    public static BoosterConfig load() {
        if (instance != null) {
            return instance;
        }

        if (CONFIG_PATH.toFile().exists()) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                instance = GSON.fromJson(config, BoosterConfig.class);
                return instance;
            } catch (JsonSyntaxException | IOException e) {
                LOGGER.error("Failed to load config, creating default config. Error: {}", e.getMessage(), e);
            }
        }

        instance = new BoosterConfig();
        saveDefaultConfig(instance);  // Save the default configuration if it doesn't exist
        return instance;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            JsonObject config = new JsonObject();
            config.addProperty("BoosterStatusWindowX", BoosterStatusWindowX);
            config.addProperty("BoosterStatusWindowY", BoosterStatusWindowY);
            config.addProperty("enchantHUDWindowX", enchantHUDWindowX);
            config.addProperty("enchantHUDWindowY", enchantHUDWindowY);
            config.addProperty("initialFetchDelaySeconds", initialFetchDelaySeconds);
            config.addProperty("fetchIntervalSeconds", fetchIntervalSeconds);
            config.addProperty("showInstructions", showInstructions); // Save instructions visibility state

            JsonObject manualRanksJson = new JsonObject();
            for (Map.Entry<UUID, String> entry : manualRanks.entrySet()) {
                manualRanksJson.addProperty(entry.getKey().toString(), entry.getValue());
            }
            config.add("manualRanks", manualRanksJson);

            writer.write(GSON.toJson(config));
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: {}", e.getMessage(), e);
        }
    }

    public void setManualRank(UUID playerUUID, String rank) {
        manualRanks.put(playerUUID, rank);
        save();
    }

    public void clearManualRank(UUID playerUUID) {
        manualRanks.remove(playerUUID);
        save();
    }

    public String getManualRank(UUID playerUUID) {
        return manualRanks.get(playerUUID);
    }

    private static void saveDefaultConfig(BoosterConfig defaultConfig) {
        defaultConfig.save();
    }
}