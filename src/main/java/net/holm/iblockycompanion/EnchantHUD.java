package net.holm.iblockycompanion;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnchantHUD implements HudRenderCallback {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private int selectedEnchantIndex = 0;
    private List<String> enchantNames;
    private final Map<String, Integer> extractedPrestigeLevels;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##"); // For formatting large numbers with decimals
    private boolean isDefaultMessageVisible = true; // Flag to track if the default message is visible

    private boolean hudDirty = true; // Track if the HUD needs to be updated
    private boolean isDragging = false;
    private boolean hasDragged = false;
    private boolean isHudVisible = true; // Flag to control visibility
    private int windowX = 10, windowY = 10;
    private int mouseXOffset = 0, mouseYOffset = 0;

    private String cachedEnchant = "";
    private int cachedPrestigeLevel = 0;
    private final Map<String, List<Double>> enchantCostsCache;
    private int maxTextWidth;  // Store max text width here

    private static final Map<String, Formatting> enchantColors = Map.ofEntries(
            Map.entry("Locksmith", Formatting.GOLD),
            Map.entry("Jurassic", Formatting.DARK_GREEN),
            Map.entry("Terminator", Formatting.DARK_RED),
            Map.entry("Efficiency", Formatting.GREEN),
            Map.entry("Explosive", Formatting.RED),
            Map.entry("Greed", Formatting.DARK_GREEN),
            Map.entry("Drill", Formatting.GOLD),
            Map.entry("Profit", Formatting.GOLD),
            Map.entry("Multiplier", Formatting.AQUA),
            Map.entry("Spelunker", Formatting.DARK_PURPLE),
            Map.entry("Spirit", Formatting.AQUA),
            Map.entry("Vein Miner", Formatting.AQUA),
            Map.entry("Cubed", Formatting.LIGHT_PURPLE),
            Map.entry("Jackhammer", Formatting.DARK_RED),
            Map.entry("Stellar Sight", Formatting.LIGHT_PURPLE),
            Map.entry("Speed", Formatting.AQUA),
            Map.entry("Starstruck", Formatting.YELLOW),
            Map.entry("Blackhole", Formatting.DARK_GRAY),
            Map.entry("Lucky", Formatting.RED)
    );

    public EnchantHUD() throws GeneralSecurityException, IOException {
        // Load initial config values from ConfigMenu
        windowX = ConfigMenu.getEnchantHUDWindowX();
        windowY = ConfigMenu.getEnchantHUDWindowY();
        this.extractedPrestigeLevels = PickaxeDataFetcher.enchantPrestigeLevels;

        CSVFetcher.fetchCSVData();
        this.enchantCostsCache = CSVFetcher.getEnchantCostsCache();
        updateEnchantNames();
    }

    public void setHudVisible(boolean visible) {
        this.isHudVisible = visible;
    }

    public boolean isHudVisible() {
        return this.isHudVisible;
    }

    private void saveWindowPosition() {
        // Save the position using ConfigMenu's methods
        ConfigMenu.setEnchantHUDWindowX(windowX);
        ConfigMenu.setEnchantHUDWindowY(windowY);
        ConfigMenu.saveConfig();
    }

    public void updateEnchantNames() {
        this.enchantNames = extractedPrestigeLevels.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                // Exclude Terminator if its prestige level is exactly 5
                .filter(entry -> !(entry.getKey().equals("Terminator") && entry.getValue() == 5))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        hudDirty = !enchantNames.isEmpty(); // Mark the HUD as dirty to trigger an update
        if (hudDirty) {
            updateCachedValues();
        }
    }

    public void cycleLeft() {
        if (!enchantNames.isEmpty()) {
            selectedEnchantIndex = (selectedEnchantIndex - 1 + enchantNames.size()) % enchantNames.size();
            hudDirty = true;
        }
    }

    public void cycleRight() {
        if (!enchantNames.isEmpty()) {
            selectedEnchantIndex = (selectedEnchantIndex + 1) % enchantNames.size();
            hudDirty = true;
        }
    }

    public void clearAndRepopulateCache(Map<String, Integer> newPrestigeLevels, Map<String, List<Double>> newEnchantCosts) {
        // Clear existing prestige level data
        extractedPrestigeLevels.clear();

        // Populate the cache with new prestige levels
        extractedPrestigeLevels.putAll(newPrestigeLevels);

        // Reprocess the enchant names and mark the HUD as dirty to trigger a re-render
        updateEnchantNames();
        hudDirty = true; // Mark the HUD as dirty to ensure it re-renders with the updated data
    }

    public void updateCachedValues() {
        if (!enchantNames.isEmpty()) {
            selectedEnchantIndex = MathHelper.clamp(selectedEnchantIndex, 0, enchantNames.size() - 1);
            cachedEnchant = enchantNames.get(selectedEnchantIndex);
            cachedPrestigeLevel = extractedPrestigeLevels.getOrDefault(cachedEnchant, 0);

            List<Double> enchantCosts = enchantCostsCache.get(cachedEnchant);
            if (enchantCosts != null && enchantCosts.size() >= 4) {
                // Unused values; adjust based on your needs.
                double toMaxFirst = enchantCosts.get(0);
                double toPrestigeFirst = enchantCosts.get(1);
                double incrementMax = enchantCosts.get(2);
                double incrementPrestige = enchantCosts.get(3);
            } else {
                System.err.println("Error: Incomplete enchant cost data for " + cachedEnchant);
            }

            isDefaultMessageVisible = false;
        } else {
            cachedEnchant = "";
            cachedPrestigeLevel = 0;
            isDefaultMessageVisible = true;
        }
        hudDirty = true;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!isHudVisible || client == null || client.player == null) {
            return;
        }

        int padding = 5;
        int lineHeight = 15;
        int bannerHeight = 15;

        if (hudDirty) {
            updateCachedValues();
            hudDirty = false;
        }

        if (isDefaultMessageVisible) {
            String defaultMessage = "No enchants available. Start cycling!";
            int defaultMessageWidth = client.textRenderer.getWidth(defaultMessage);
            int windowWidth = defaultMessageWidth + 2 * padding;
            int windowHeight = lineHeight + 2 * padding;

            ensureWindowWithinScreen(client, windowWidth, windowHeight);

            drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0x80000000);
            drawContext.drawTextWithShadow(client.textRenderer, defaultMessage, windowX + padding, windowY + padding, 0xFFFFFF);
            return;
        }

        Formatting enchantColor = enchantColors.getOrDefault(cachedEnchant, Formatting.WHITE);

        int nextPrestigeLevel = cachedPrestigeLevel + 1;
        double nextPrestigeCostValue = calculateNextPrestigeCost(cachedEnchant, cachedPrestigeLevel, nextPrestigeLevel);
        String formattedNextPrestigeCost = DECIMAL_FORMAT.format(nextPrestigeCostValue);

        Text prestigeLevelText = Text.literal("Current prestige: ").formatted(Formatting.AQUA)
                .append(Text.literal(String.valueOf(cachedPrestigeLevel)).formatted(Formatting.GOLD));
        Text nextPrestigeCostText = Text.literal("Next prestige cost: ").formatted(Formatting.AQUA)
                .append(Text.literal(formattedNextPrestigeCost).formatted(Formatting.GOLD));

        String bannerText = "< " + cachedEnchant + " >";
        int bannerTextWidth = client.textRenderer.getWidth(bannerText);

        maxTextWidth = Math.max(bannerTextWidth, client.textRenderer.getWidth(prestigeLevelText));
        maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(nextPrestigeCostText));

        int windowWidth = maxTextWidth + 2 * padding;
        int windowHeight = (2 * lineHeight) + bannerHeight + 2 * padding;

        ensureWindowWithinScreen(client, windowWidth, windowHeight);

        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + bannerHeight, 0xD0000000);

        int centeredX = windowX + (windowWidth - bannerTextWidth) / 2;
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(bannerText).formatted(enchantColor), centeredX, windowY + padding, 0xFFFFFF);

        int currentY = windowY + bannerHeight + padding;
        drawContext.fill(windowX, windowY + bannerHeight, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        drawContext.drawTextWithShadow(client.textRenderer, prestigeLevelText, windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, nextPrestigeCostText, windowX + padding, currentY, 0xFFFFFF);
    }

    private double calculateNextPrestigeCost(String enchantName, int currentPrestigeLevel, int nextPrestigeLevel) {
        List<Double> enchantCosts = enchantCostsCache.get(enchantName);

        if (enchantCosts != null && enchantCosts.size() == 4) {
            double toMaxFirst = enchantCosts.get(0);
            double incrementMax = enchantCosts.get(2);
            double toPrestigeFirst = enchantCosts.get(1);
            double incrementPrestige = enchantCosts.get(3);

            return (toMaxFirst + (incrementMax * nextPrestigeLevel)) + (toPrestigeFirst + (incrementPrestige * currentPrestigeLevel));
        }

        return 0.0;
    }

    public void handleMousePress(double mouseX, double mouseY) {
        int padding = 5;
        int bannerHeight = 15;

        int windowWidth = maxTextWidth + 2 * padding;

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
            hasDragged = true;
        }
    }

    public void handleScreenClose() {
        isDragging = false;
    }

    private void ensureWindowWithinScreen(MinecraftClient client, int windowWidth, int windowHeight) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        windowX = MathHelper.clamp(windowX, 0, screenWidth - windowWidth);
        windowY = MathHelper.clamp(windowY, 0, screenHeight - windowHeight);
    }
}
