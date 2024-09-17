package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnchantHUD implements HudRenderCallback {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private int selectedEnchantIndex = 0;
    private List<String> enchantNames;
    private final Map<String, Integer> extractedPrestigeLevels;

    private boolean hudDirty = true; // Track if the HUD needs to be updated
    private boolean isDragging = false;
    private boolean hasDragged = false;
    private int windowX = 10, windowY = 10;
    private int mouseXOffset = 0, mouseYOffset = 0;

    // Cached values for the last rendered enchant and prestige level
    private String cachedEnchant = "";
    private int cachedPrestigeLevel = 0;

    private final BoosterConfig config; // Make config final and pass it via constructor

    public EnchantHUD(BoosterConfig config) {
        this.config = config; // Initialize config
        windowX = config.enchantHUDWindowX;
        windowY = config.enchantHUDWindowY;
        this.extractedPrestigeLevels = PickaxeDataFetcher.enchantPrestigeLevels;
        updateEnchantNames(); // Call this method to update the enchant names during initialization
    }

    private void saveWindowPosition() {
        config.enchantHUDWindowX = windowX;
        config.enchantHUDWindowY = windowY;
        config.save();
    }

    // Method to update the enchantNames list and mark HUD as dirty
    public void updateEnchantNames() {
        this.enchantNames = extractedPrestigeLevels.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)  // Only include enchants with prestige > 0
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        hudDirty = !enchantNames.isEmpty();
        if (hudDirty) {
            updateCachedValues();  // Update cached values for the first render
        }
        System.out.println("Filtered Enchant Names (non-zero prestige): " + enchantNames);
    }

    // Method to cycle left through enchants and mark the HUD as dirty
    public void cycleLeft() {
        if (!enchantNames.isEmpty()) {
            selectedEnchantIndex = (selectedEnchantIndex - 1 + enchantNames.size()) % enchantNames.size();
            hudDirty = true;  // Mark the HUD as dirty when enchant changes
        }
    }

    // Method to cycle right through enchants and mark the HUD as dirty
    public void cycleRight() {
        if (!enchantNames.isEmpty()) {
            selectedEnchantIndex = (selectedEnchantIndex + 1) % enchantNames.size();
            hudDirty = true;  // Mark the HUD as dirty when enchant changes
        }
    }

    // Update cached values when HUD is dirty
    private void updateCachedValues() {
        if (!enchantNames.isEmpty()) {
            cachedEnchant = enchantNames.get(selectedEnchantIndex);
            cachedPrestigeLevel = extractedPrestigeLevels.getOrDefault(cachedEnchant, 0);
        }
    }

    // HUD rendering logic
    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (client == null || client.player == null) {
            return;
        }

        int padding = 5;
        int lineHeight = 15;
        int bannerHeight = 15;

        if (hudDirty) {
            updateCachedValues();  // Update cached enchant and prestige level
            hudDirty = false;       // Reset dirty flag after updating
        }

        // Calculate the width based on the text size
        int maxTextWidth = client.textRenderer.getWidth("Selected Enchant: " + cachedEnchant);
        maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth("Prestige Level: " + cachedPrestigeLevel));

        int windowWidth = maxTextWidth + 2 * padding;
        int windowHeight = (2 * lineHeight) + bannerHeight + 2 * padding;

        ensureWindowWithinScreen(client, windowWidth, windowHeight);

        // Draw the banner for dragging
        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + bannerHeight, 0xD0000000); // Banner color

        // Draw the HUD background
        int currentY = windowY + bannerHeight + padding;
        drawContext.fill(windowX, windowY + bannerHeight, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        // Render enchant information
        drawContext.drawTextWithShadow(client.textRenderer, Text.of("Selected Enchant: " + cachedEnchant), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;
        drawContext.drawTextWithShadow(client.textRenderer, Text.of("Prestige Level: " + cachedPrestigeLevel), windowX + padding, currentY, 0xFFFFFF);
    }

    // Handle mouse drag and press for dragging the HUD
    public void handleMousePress(double mouseX, double mouseY) {
        int padding = 5;
        int bannerHeight = 15;
        int windowWidth = Math.max(client.textRenderer.getWidth("Selected Enchant: " + cachedEnchant), client.textRenderer.getWidth("Prestige Level: " + cachedPrestigeLevel)) + 2 * padding;

        // Banner drag area
        if (mouseX >= windowX && mouseX <= windowX + windowWidth && mouseY >= windowY && mouseY <= windowY + bannerHeight) {
            isDragging = true;
            hasDragged = true;
            mouseXOffset = (int) (mouseX - windowX);
            mouseYOffset = (int) (mouseY - windowY);
        }
    }

    public void handleMouseRelease() {
        isDragging = false;
        saveWindowPosition();
    }

    public void onMouseMove(double mouseX, double mouseY) {
        if (isDragging) {
            windowX = (int) (mouseX - mouseXOffset);
            windowY = (int) (mouseY - mouseYOffset);

            // Mark that the window has been moved
            hasDragged = true;
        }
    }

    private void ensureWindowWithinScreen(MinecraftClient client, int windowWidth, int windowHeight) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        windowX = MathHelper.clamp(windowX, 0, screenWidth - windowWidth);
        windowY = MathHelper.clamp(windowY, 0, screenHeight - windowHeight);
    }
}

