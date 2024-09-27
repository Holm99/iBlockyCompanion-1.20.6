package net.holm.iblockycompanion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigMenu extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config/iBlockyCompanion.json");

    private final Screen parent;
    private int currentPage = 1; // Track current tab

    // Configuration options
    private static int boosterStatusWindowX = 100;
    private static int boosterStatusWindowY = 100;
    private static int enchantHUDWindowX = 0;
    private static int enchantHUDWindowY = 0;
    private static int fetchIntervalSeconds = 120;
    private static int initialFetchDelaySeconds = 30;
    private static boolean showInstructions = true;
    private static final Map<UUID, String> manualRanks = new HashMap<>(); // Store manual ranks

    // Constructor takes in the parent screen
    public ConfigMenu(Screen parent) {
        super(Text.of("iBlocky Companion Config Menu"));
        this.parent = parent;
        loadConfig(); // Load config on initialization
    }

    // Add methods for manual ranks
    public static void setManualRank(UUID playerUUID, String rank) {
        manualRanks.put(playerUUID, rank);
        saveConfig();
    }

    public static void clearManualRank(UUID playerUUID) {
        manualRanks.remove(playerUUID);
        saveConfig();
    }

    public static String getManualRank(UUID playerUUID) {
        return manualRanks.get(playerUUID);
    }

    // Other configuration methods
    public static int getEnchantHUDWindowX() {
        return enchantHUDWindowX;
    }

    public static void setEnchantHUDWindowX(int x) {
        enchantHUDWindowX = x;
    }

    public static int getEnchantHUDWindowY() {
        return enchantHUDWindowY;
    }

    public static void setEnchantHUDWindowY(int y) {
        enchantHUDWindowY = y;
    }

    public static int getBoosterStatusWindowX() {
        return boosterStatusWindowX;
    }

    public static void setBoosterStatusWindowX(int x) {
        boosterStatusWindowX = x;
    }

    public static int getBoosterStatusWindowY() {
        return boosterStatusWindowY;
    }

    public static void setBoosterStatusWindowY(int y) {
        boosterStatusWindowY = y;
    }

    public static boolean getShowInstructions() {
        return showInstructions;
    }

    public static void setShowInstructions(boolean show) {
        showInstructions = show;
    }

    public static int getFetchIntervalSeconds() {
        return fetchIntervalSeconds;
    }

    public static int getInitialFetchDelaySeconds() {
        return initialFetchDelaySeconds;
    }

    // Initialize method to set up the buttons and components
    @Override
    protected void init() {
        this.addTabButtons();

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Done"), button -> {
            saveConfig(); // Save config before closing
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(this.width / 2 - 100, this.height - 25, 200, 20).build());

        this.renderCurrentTab();
    }

    private void renderCurrentTab() {
        this.clearChildren();
        this.addTabButtons();
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Done"), button -> {
            saveConfig();
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(this.width / 2 - 100, this.height - 25, 200, 20).build());

        switch (currentPage) {
            case 1 -> addGeneralPage();
            case 2 -> addBoosterHudPage();
            case 3 -> addEnchantHudPage();
            case 4 -> addCustomPlayerListPage();
        }
    }

    private void addTabButtons() {
        int buttonY = 30;

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("General"), button -> {
            currentPage = 1;
            renderCurrentTab();
        }).dimensions(10, buttonY, 100, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Booster HUD"), button -> {
            currentPage = 2;
            renderCurrentTab();
        }).dimensions(10, buttonY + 25, 100, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Enchant HUD"), button -> {
            currentPage = 3;
            renderCurrentTab();
        }).dimensions(10, buttonY + 50, 100, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Player List"), button -> {
            currentPage = 4;
            renderCurrentTab();
        }).dimensions(10, buttonY + 75, 100, 20).build());
    }

    private void addStaticText(DrawContext context) {
        int centerX = this.width / 2;
        String pageTitle = "iBlocky Companion Config Menu";
        int textWidth = this.textRenderer.getWidth(pageTitle);
        int textX = centerX - textWidth / 2;
        context.drawText(this.textRenderer, pageTitle, textX, 10, 0xFFFFFF, false);
    }

    // Page methods...
    private void addGeneralPage() {
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Toggle Instructions: " + (showInstructions ? "On" : "Off")), button -> {
            showInstructions = !showInstructions;
            button.setMessage(Text.of("Toggle Instructions: " + (showInstructions ? "On" : "Off")));
        }).dimensions(this.width / 2 - 100, this.height / 2 - 25, 200, 20).build());
    }

    private void addBoosterHudPage() {
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Booster HUD X: " + boosterStatusWindowX), button -> {
            boosterStatusWindowX += 10;
            button.setMessage(Text.of("Booster HUD X: " + boosterStatusWindowX));
        }).dimensions(this.width / 2 - 100, this.height / 2 - 25, 200, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Booster HUD Y: " + boosterStatusWindowY), button -> {
            boosterStatusWindowY += 10;
            button.setMessage(Text.of("Booster HUD Y: " + boosterStatusWindowY));
        }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());
    }

    private void addEnchantHudPage() {
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Enchant HUD X: " + enchantHUDWindowX), button -> {
            enchantHUDWindowX += 10;
            button.setMessage(Text.of("Enchant HUD X: " + enchantHUDWindowX));
        }).dimensions(this.width / 2 - 100, this.height / 2 - 25, 200, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Enchant HUD Y: " + enchantHUDWindowY), button -> {
            enchantHUDWindowY += 10;
            button.setMessage(Text.of("Enchant HUD Y: " + enchantHUDWindowY));
        }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());
    }

    private void addCustomPlayerListPage() {
        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Fetch Interval: " + fetchIntervalSeconds + "s"), button -> {
            fetchIntervalSeconds += 10;
            button.setMessage(Text.of("Fetch Interval: " + fetchIntervalSeconds + "s"));
        }).dimensions(this.width / 2 - 100, this.height / 2 - 25, 200, 20).build());

        this.addDrawableChild(new ButtonWidget.Builder(Text.of("Initial Fetch Delay: " + initialFetchDelaySeconds + "s"), button -> {
            initialFetchDelaySeconds += 5;
            button.setMessage(Text.of("Initial Fetch Delay: " + initialFetchDelaySeconds + "s"));
        }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());
    }

    // Render method
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xB0000000, 0xD0000000);
        this.addStaticText(context);
        super.render(context, mouseX, mouseY, delta);
    }

    // Close on ESC key
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // Save and Load Configuration
    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            JsonObject config = new JsonObject();
            config.addProperty("boosterStatusWindowX", boosterStatusWindowX);
            config.addProperty("boosterStatusWindowY", boosterStatusWindowY);
            config.addProperty("enchantHUDWindowX", enchantHUDWindowX);
            config.addProperty("enchantHUDWindowY", enchantHUDWindowY);
            config.addProperty("fetchIntervalSeconds", fetchIntervalSeconds);
            config.addProperty("initialFetchDelaySeconds", initialFetchDelaySeconds);
            config.addProperty("showInstructions", showInstructions);

            // Add manual ranks to the config
            JsonObject manualRanksJson = new JsonObject();
            for (Map.Entry<UUID, String> entry : manualRanks.entrySet()) {
                manualRanksJson.addProperty(entry.getKey().toString(), entry.getValue());
            }
            config.add("manualRanks", manualRanksJson);

            writer.write(GSON.toJson(config));
        } catch (IOException e) {
            System.err.println("Failed to save config file: " + e.getMessage());
        }
    }

    private void loadConfig() {
        if (CONFIG_PATH.toFile().exists()) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                boosterStatusWindowX = config.has("boosterStatusWindowX") ? config.get("boosterStatusWindowX").getAsInt() : boosterStatusWindowX;
                boosterStatusWindowY = config.has("boosterStatusWindowY") ? config.get("boosterStatusWindowY").getAsInt() : boosterStatusWindowY;
                enchantHUDWindowX = config.has("enchantHUDWindowX") ? config.get("enchantHUDWindowX").getAsInt() : enchantHUDWindowX;
                enchantHUDWindowY = config.has("enchantHUDWindowY") ? config.get("enchantHUDWindowY").getAsInt() : enchantHUDWindowY;
                fetchIntervalSeconds = config.has("fetchIntervalSeconds") ? config.get("fetchIntervalSeconds").getAsInt() : fetchIntervalSeconds;
                initialFetchDelaySeconds = config.has("initialFetchDelaySeconds") ? config.get("initialFetchDelaySeconds").getAsInt() : initialFetchDelaySeconds;
                showInstructions = config.has("showInstructions") ? config.get("showInstructions").getAsBoolean() : showInstructions;

                // Load manual ranks
                if (config.has("manualRanks")) {
                    JsonObject manualRanksJson = config.getAsJsonObject("manualRanks");
                    for (Map.Entry<String, ?> entry : manualRanksJson.entrySet()) {
                        manualRanks.put(UUID.fromString(entry.getKey()), manualRanksJson.get(entry.getKey()).getAsString());
                    }
                }

            } catch (JsonSyntaxException | IOException e) {
                System.err.println("Failed to load config file: " + e.getMessage());
            }
        } else {
            saveConfig(); // Create the config file if it doesn't exist
        }
    }
}
