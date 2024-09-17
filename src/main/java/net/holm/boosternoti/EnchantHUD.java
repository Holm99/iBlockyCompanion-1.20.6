package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class EnchantHUD implements HudRenderCallback {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private int selectedEnchantIndex = 0;
    private final List<String> enchantNames;
    private final Map<String, Integer> extractedPrestigeLevels;
    private boolean hudDirty = true; // Track if the HUD needs to be updated

    // Cached values for the last rendered enchant and prestige level
    private String cachedEnchant = "";
    private int cachedPrestigeLevel = 0;

    public EnchantHUD() {
        this.extractedPrestigeLevels = PickaxeDataFetcher.enchantPrestigeLevels;
        this.enchantNames = new ArrayList<>(extractedPrestigeLevels.keySet());

        if (!enchantNames.isEmpty()) {
            // Initially mark as dirty to force render on first display
            hudDirty = true;
            updateCachedValues();
        }
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
        if (client == null || client.player == null || enchantNames.isEmpty()) {
            return;
        }

        // Update the cached values if the HUD is dirty
        if (hudDirty) {
            System.out.println("EnchantHUD: Rendering updated values");
            updateCachedValues();  // Update cached enchant and prestige level
            hudDirty = false;       // Reset dirty flag after updating
        }

        // Render the cached values (these will be rendered even when the HUD isn't dirty)
        int xPos = 10;  // Adjusted X position
        int yPos = 10;  // Adjusted Y position

        // Render selected enchant and prestige level using cached values
        drawContext.drawTextWithShadow(client.textRenderer, Text.of("Selected Enchant: " + cachedEnchant), xPos, yPos, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of("Prestige Level: " + cachedPrestigeLevel), xPos, yPos + 15, 0xFFFFFF);
    }

    // Call this method whenever enchant data changes, to re-trigger the rendering
    public void markHudDirty() {
        hudDirty = true;
    }
}