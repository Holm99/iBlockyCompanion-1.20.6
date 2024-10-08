package net.holm.iblockycompanion;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.text.NumberFormat;
import java.util.Locale;

public class BoosterStatusWindow implements HudRenderCallback {
    private static final int BANNER_HEIGHT = 20;
    private boolean hudVisible = true; // Control HUD visibility
    private boolean isDragging = false;
    private boolean hasDragged = false;
    private boolean needsRenderUpdate = true;

    private int windowX, windowY;
    private int mouseXOffset = 0, mouseYOffset = 0;
    private int originalWindowX, originalWindowY;  // Track the original position before instructions expand
    private boolean shouldReturnToOriginalPosition = false;

    private boolean isAnimating = false;
    private float animationProgress = 0.0f;
    private float animationSpeed = 0.1f;

    private static String sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
    private String timeRemaining = Formatting.RED + "Tokens Booster: N/A";
    private String richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
    private String backpackTimeInfo = Formatting.RED + "Backpack Time: N/A";
    private String totalTokensInfo = Formatting.YELLOW + "Total Sales: 0 Tokens";

    private int tokensBoosterRemainingSeconds = 0;
    private int richBoosterRemainingSeconds = 0;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> countdownTask;

    private long totalSales = 0;

    private final KeyBinding boosterKeyBinding;
    private final KeyBinding toggleBoosterHudKeyBinding;
    private final KeyBinding toggleEnchantHudKeyBinding;
    private final KeyBinding showPlayerListKeyBinding;
    private final KeyBinding toggleInstructionsKeyBinding;

    // Constructor that accepts KeyBindings from the client class
    public BoosterStatusWindow(
            KeyBinding boosterKeyBinding,
            KeyBinding toggleBoosterHudKeyBinding,
            KeyBinding toggleEnchantHudKeyBinding,
            KeyBinding showPlayerListKeyBinding,
            KeyBinding toggleInstructionsKeyBinding) {

        this.boosterKeyBinding = boosterKeyBinding;
        this.toggleBoosterHudKeyBinding = toggleBoosterHudKeyBinding;
        this.toggleEnchantHudKeyBinding = toggleEnchantHudKeyBinding;
        this.showPlayerListKeyBinding = showPlayerListKeyBinding;
        this.toggleInstructionsKeyBinding = toggleInstructionsKeyBinding;

        // Use the static getters from ConfigMenu to get window positions
        windowX = ConfigMenu.getBoosterStatusWindowX();
        windowY = ConfigMenu.getBoosterStatusWindowY();
        startCountdown();
        startBackpackTimeTracking();
    }

    public void toggleInstructions() {
        boolean showInstructions = ConfigMenu.getShowInstructions(); // Get the current state from ConfigMenu
        ConfigMenu.setShowInstructions(!showInstructions); // Toggle it
        ConfigMenu.saveConfig(); // Save the changes

        if (!showInstructions) {
            originalWindowX = windowX;
            originalWindowY = windowY;
            shouldReturnToOriginalPosition = false;  // Reset flag
        } else {
            shouldReturnToOriginalPosition = true;  // Prepare to return to the original position after collapse
        }

        isAnimating = true;  // Start the animation
    }

    private void updateAnimation() {
        if (isAnimating) {
            boolean showInstructions = ConfigMenu.getShowInstructions();
            if (showInstructions && animationProgress < 1.0f) {
                // Expanding instructions
                animationProgress += animationSpeed;
                if (animationProgress >= 1.0f) {
                    animationProgress = 1.0f;
                    isAnimating = false;  // Animation finished
                }
            } else if (!showInstructions && animationProgress > 0.0f) {
                // Collapsing instructions
                animationProgress -= animationSpeed;
                if (animationProgress <= 0.0f) {
                    animationProgress = 0.0f;
                    isAnimating = false;  // Animation finished
                }
            }

            // Check if we need to return to the original position after collapse
            if (!showInstructions && shouldReturnToOriginalPosition && animationProgress <= 0.0f) {
                // Smoothly move the HUD back to the original position while collapsing
                windowX = originalWindowX;
                windowY = originalWindowY;
                shouldReturnToOriginalPosition = false;  // Reset flag after moving back
            }
        }
    }

    private void startBackpackTimeTracking() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            backpackTimeInfo = Formatting.AQUA + "Backpack Time: " + BackpackSpaceTracker.getElapsedTime();
            needsRenderUpdate = true;
        });
    }

    private String formatWithThousandSeparator(long number) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        return numberFormat.format(number);
    }

    // Method to set the total sales and update the HUD
    public void setTotalSales(long totalSales) {
        this.totalSales = totalSales;
        totalTokensInfo = Formatting.YELLOW + "Total Sales: " + formatWithThousandSeparator(totalSales) + " Tokens";
        needsRenderUpdate = true;
    }

    private void saveWindowPosition() {
        ConfigMenu.setBoosterStatusWindowX(windowX); // Save position to ConfigMenu
        ConfigMenu.setBoosterStatusWindowY(windowY);
        ConfigMenu.saveConfig(); // Save the updated config
    }

    private void startCountdown() {
        if (countdownTask == null || countdownTask.isCancelled()) {
            countdownTask = scheduler.scheduleAtFixedRate(this::handleCountdown, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void stopCountdown() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel(false);
        }
    }

    private void handleCountdown() {
        if (isGameActive() && isAnyBoosterActive()) {
            boolean shouldUpdateSellBoost = false;

            if (tokensBoosterRemainingSeconds > 0) {
                tokensBoosterRemainingSeconds--;
                updateTokensBoosterTime();

                if (tokensBoosterRemainingSeconds == 0) {
                    if (SellBoostCalculator.getTokenBoost() != 0.0) {
                        SellBoostCalculator.setTokenBoost(0.0);
                        shouldUpdateSellBoost = true;
                    }
                }
            }

            if (richBoosterRemainingSeconds > 0) {
                richBoosterRemainingSeconds--;
                updateRichBoosterTime();

                if (richBoosterRemainingSeconds == 0) {
                    if (SellBoostCalculator.getRichPetBoost() != 0.0) {
                        SellBoostCalculator.setRichPetBoost(0.0);
                        shouldUpdateSellBoost = true;
                    }
                }
            }

            if (shouldUpdateSellBoost) {
                updateTotalSellBoost();
            }

            needsRenderUpdate = true;
        } else {
            stopCountdown();
        }
    }

    private void updateTotalSellBoost() {
        double totalSellBoost = SellBoostCalculator.getTotalSellBoost();
        updateSellBoostDisplay(totalSellBoost);
        needsRenderUpdate = true;
    }

    private boolean isGameActive() {
        return MinecraftClient.getInstance().world != null;
    }

    private boolean isAnyBoosterActive() {
        return tokensBoosterRemainingSeconds > 0 || richBoosterRemainingSeconds > 0;
    }

    public static void updateSellBoostDisplay(double totalSellBoost) {
        sellBoostInfo = Formatting.GOLD + "Sell Boost: " + Formatting.YELLOW + String.format("%.2fx", totalSellBoost);
    }

    public void setTokensBoosterActive(boolean active, String multiplier, String time) {
        if (active) {
            tokensBoosterRemainingSeconds = parseTimeToSeconds(time);
            SellBoostCalculator.setTokenBoost(Double.parseDouble(multiplier));
            startCountdown();
        } else {
            SellBoostCalculator.setTokenBoost(0.0);
        }
        updateTokensBoosterTime();
        needsRenderUpdate = true;
    }

    public void setRichBoosterActive(boolean active, String time) {
        if (active) {
            richBoosterRemainingSeconds = parseTimeToSeconds(time);
            SellBoostCalculator.setRichPetBoost(2.0);
            startCountdown();
        } else {
            SellBoostCalculator.setRichPetBoost(0.0);
        }
        updateRichBoosterTime();
        needsRenderUpdate = true;
    }

    private void updateTokensBoosterTime() {
        if (tokensBoosterRemainingSeconds > 0) {
            timeRemaining = Formatting.AQUA + "Tokens Booster: " + formatTime(tokensBoosterRemainingSeconds);
        } else {
            clearTokenBoosterInfo();
        }
        needsRenderUpdate = true;
    }

    private void updateRichBoosterTime() {
        if (richBoosterRemainingSeconds > 0) {
            richBoosterTimeRemaining = Formatting.AQUA + "Rich Booster: " + formatTime(richBoosterRemainingSeconds);
        } else {
            clearRichBoosterInfo();
        }
        needsRenderUpdate = true;
    }

    public void clearTokenBoosterInfo() {
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        needsRenderUpdate = true;
    }

    public void clearRichBoosterInfo() {
        richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        needsRenderUpdate = true;
    }

    public void clearBoosterInfo() {
        sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        SellBoostCalculator.resetBoosts();
        needsRenderUpdate = true;
    }

    private String tokenBalanceInfo = Formatting.GOLD + "Balance: 0 Tokens";

    public void setTokenBalance(double balance) {
        // Format the balance with a thousand separator
        tokenBalanceInfo = Formatting.GOLD + "Balance: " + String.format("%,.0f Tokens", balance);
        needsRenderUpdate = true;
    }

    private String formatTime(int totalSeconds) {
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            formattedTime.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            formattedTime.append(minutes).append("m ");
        }
        formattedTime.append(seconds).append("s");

        return formattedTime.toString();
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!isGameActive() || !hudVisible) {
            return;
        }

        updateAnimation();  // Update the animation state each tick

        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int lineHeight = 15;
        int bannerHeight = 15;

        // Calculate maxTextWidth for regular HUD elements
        int maxTextWidth = 0;
        for (String text : new String[]{sellBoostInfo, timeRemaining, richBoosterTimeRemaining, backpackTimeInfo, totalTokensInfo, tokenBalanceInfo}) {
            int textWidth = client.textRenderer.getWidth(text);
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }

        // If instructions are shown, calculate maxTextWidth for instructions as well
        if (animationProgress > 0) {  // Or config.showInstructions depending on your logic
            String boosterKey = boosterKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleBoosterHudKey = toggleBoosterHudKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleEnchantHudKey = toggleEnchantHudKeyBinding.getBoundKeyLocalizedText().getString();
            String showPlayerListKey = showPlayerListKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleInstructionsKey = toggleInstructionsKeyBinding.getBoundKeyLocalizedText().getString();

            for (String text : new String[]{
                    "---------------------------------------",
                    boosterKey + " - Run Booster Command",
                    toggleBoosterHudKey + " - To toggle BoosterHUD",
                    toggleEnchantHudKey + " - To toggle EnchantHUD",
                    showPlayerListKey + " - Custom Player List",
                    toggleInstructionsKey + " - Toggle this text",
                    "Arrow keys to cycle through EnchantHUD text",
                    "/summaryclear - To clear Total Sales",
                    "---------------------------------------"}) {
                int textWidth = client.textRenderer.getWidth(text);
                maxTextWidth = Math.max(maxTextWidth, textWidth);
            }
        }

        // Calculate total lines and window height
        int lineCount = 6; // 5 lines for boosters + backpack + 1 line for total tokens + 1 line for balance
        int windowWidth = maxTextWidth + 2 * padding;
        int baseWindowHeight = (6 * lineHeight) + bannerHeight + 2 * padding;

        // Dynamically calculate instruction height based on animation progress
        int instructionsHeight = (int) (9 * lineHeight * animationProgress);
        int windowHeight = baseWindowHeight + instructionsHeight;

        // Ensure window stays within screen
        ensureWindowWithinScreen(client, windowWidth, windowHeight);

        // Draw the banner
        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + bannerHeight, 0xD0000000);

        // Draw the HUD body with background
        int currentY = windowY + bannerHeight + padding;
        drawContext.fill(windowX, windowY + bannerHeight, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        // Calculate alpha (opacity) based on animation progress
        int alpha = (int) (255 * animationProgress);  // Max alpha is 255, scale it with animationProgress

        // Render booster and sales info with fading effect
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(sellBoostInfo), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(richBoosterTimeRemaining), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(backpackTimeInfo), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(totalTokensInfo), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(tokenBalanceInfo), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        // Render dynamic instructions if animationProgress > 0, with text fading
        if (animationProgress > 0) {
            String boosterKey = boosterKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleBoosterHudKey = toggleBoosterHudKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleEnchantHudKey = toggleEnchantHudKeyBinding.getBoundKeyLocalizedText().getString();
            String showPlayerListKey = showPlayerListKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleInstructionsKey = toggleInstructionsKeyBinding.getBoundKeyLocalizedText().getString();
            drawContext.drawTextWithShadow(client.textRenderer, Text.of("---------------------------------------"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterKey + " - Run Booster Command"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(toggleBoosterHudKey + " - To toggle BoosterHUD"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(toggleEnchantHudKey + " - To toggle EnchantHUD"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(showPlayerListKey + " - Custom Player List"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(toggleInstructionsKey + " - Toggle this text"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of("Arrow keys to cycle through EnchantHUD text"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of("/summaryclear - To clear Total Sales"), windowX + padding, currentY, applyAlpha(alpha));
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of("---------------------------------------"), windowX + padding, currentY, applyAlpha(alpha));
        }

        needsRenderUpdate = false;
    }

    // Helper method to apply alpha to color
    private int applyAlpha(int alpha) {
        return (alpha << 24) | (0x00FFFFFF);
    }

    public void clearTotalSales() {
        totalSales = 0;
        totalTokensInfo = Formatting.YELLOW + "Total Sales: 0 Tokens";
        needsRenderUpdate = true;
    }

    public void handleMousePress(double mouseX, double mouseY) {
        int padding = 5;
        int bannerHeight = 15;
        MinecraftClient client = MinecraftClient.getInstance();

        // Inline maxTextWidth calculation for all displayed text
        int maxTextWidth = 0;
        for (String text : new String[]{sellBoostInfo, timeRemaining, richBoosterTimeRemaining, backpackTimeInfo, totalTokensInfo, tokenBalanceInfo}) {
            int textWidth = client.textRenderer.getWidth(text);
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }

        // If instructions are shown, add their widths to maxTextWidth
        if (ConfigMenu.getShowInstructions()) {
            String boosterKey = boosterKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleBoosterHudKey = toggleBoosterHudKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleEnchantHudKey = toggleEnchantHudKeyBinding.getBoundKeyLocalizedText().getString();
            String showPlayerListKey = showPlayerListKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleInstructionsKey = toggleInstructionsKeyBinding.getBoundKeyLocalizedText().getString();

            for (String text : new String[]{
                    "---------------------------------------",
                    boosterKey + " - Run Booster Command",
                    toggleBoosterHudKey + " - To toggle BoosterHUD",
                    toggleEnchantHudKey + " - To toggle EnchantHUD",
                    showPlayerListKey + " - Custom Player List",
                    toggleInstructionsKey + " - Toggle this text"
            }) {
                int textWidth = client.textRenderer.getWidth(text);
                maxTextWidth = Math.max(maxTextWidth, textWidth);
            }
        }

        int windowWidth = maxTextWidth + 2 * padding; // Recalculate window width dynamically

        // Make the HUD draggable only from the banner area
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
            MinecraftClient client = MinecraftClient.getInstance();

            // Check if the OS is macOS
            boolean isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");

            // Get the window scale factor (helps handle macOS Retina display scaling)
            double scaleFactor = client.getWindow().getScaleFactor();

            // Only apply scaling on macOS
            if (isMacOS) {
                mouseX /= scaleFactor;
                mouseY /= scaleFactor;
            }

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

    public void handleScreenClose() {
        isDragging = false;
    }

    // New method to set HUD visibility
    public void setHudVisible(boolean visible) {
        this.hudVisible = visible;
    }

    // New method to get HUD visibility status
    public boolean isHudVisible() {
        return this.hudVisible;
    }

    private static int parseTimeToSeconds(String time) {
        int totalSeconds = 0;

        // Split the time string by spaces, expecting parts like '2d', '23h', '57m', and '38s'
        String[] parts = time.split(" ");
        for (String part : parts) {
            try {
                if (part.endsWith("d")) {
                    int days = Integer.parseInt(part.replace("d", ""));
                    totalSeconds += days * 86400;  // Convert days to seconds
                } else if (part.endsWith("h")) {
                    int hours = Integer.parseInt(part.replace("h", ""));
                    totalSeconds += hours * 3600;  // Convert hours to seconds
                } else if (part.endsWith("m")) {
                    int minutes = Integer.parseInt(part.replace("m", ""));
                    totalSeconds += minutes * 60;  // Convert minutes to seconds
                } else if (part.endsWith("s")) {
                    int seconds = Integer.parseInt(part.replace("s", ""));
                    totalSeconds += seconds;  // Add seconds
                }
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse time part: " + part);
            }
        }
        return totalSeconds;
    }
}
