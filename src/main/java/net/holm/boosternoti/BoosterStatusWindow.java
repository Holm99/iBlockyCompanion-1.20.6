package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.text.NumberFormat;
import java.util.Locale;

import static net.holm.boosternoti.iBlockyBoosterNotificationClient.setHudVisible;

public class BoosterStatusWindow implements HudRenderCallback {
    private static final int BANNER_HEIGHT = 20;
    private boolean hudVisible = true;
    private boolean isDragging = false;
    private boolean hasDragged = false;
    private boolean isCloseButtonHovered = false;
    private boolean needsRenderUpdate = true;
    private boolean showInstructions = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(BoosterStatusWindow.class);

    private int windowX, windowY;
    private int mouseXOffset = 0, mouseYOffset = 0;

    private static String sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
    private String timeRemaining = Formatting.RED + "Tokens Booster: N/A";
    private String richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
    private String backpackTimeInfo = Formatting.RED + "Backpack Time: N/A";
    private String totalTokensInfo = Formatting.YELLOW + "Total Sales: 0 Tokens"; // New variable to store total tokens info

    private int tokensBoosterRemainingSeconds = 0;
    private int richBoosterRemainingSeconds = 0;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> countdownTask;

    private long totalSales = 0;

    private final BoosterConfig config;
    private final KeyBinding boosterKeyBinding;
    private final KeyBinding toggleHudKeyBinding;
    private final KeyBinding showPlayerListKeyBinding;
    private final KeyBinding toggleInstructionsKeyBinding;


    public void toggleInstructions() {
        showInstructions = !showInstructions;
        needsRenderUpdate = true; // Ensure it re-renders when instructions are toggled
    }

    // Constructor that accepts KeyBindings from the client class
    public BoosterStatusWindow(BoosterConfig config,
                               KeyBinding boosterKeyBinding,
                               KeyBinding toggleHudKeyBinding,
                               KeyBinding showPlayerListKeyBinding,
                               KeyBinding toggleInstructionsKeyBinding) {
        this.config = config;
        this.boosterKeyBinding = boosterKeyBinding;
        this.toggleHudKeyBinding = toggleHudKeyBinding;
        this.showPlayerListKeyBinding = showPlayerListKeyBinding;
        this.toggleInstructionsKeyBinding = toggleInstructionsKeyBinding;
        windowX = config.windowX;
        windowY = config.windowY;
        startCountdown();
        startBackpackTimeTracking();
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
        // System.out.println("Total sales updated to: " + totalSales);
    }

    private void saveWindowPosition() {
        config.windowX = windowX;
        config.windowY = windowY;
        config.save();
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
        if (!isGameActive() || !iBlockyBoosterNotificationClient.isHudVisible()) {
            return;
        }

        // Null-check the key bindings before using them
        if (boosterKeyBinding == null || toggleHudKeyBinding == null || showPlayerListKeyBinding == null || toggleInstructionsKeyBinding == null) {
            return; // Exit early if any key binding is not initialized
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int lineHeight = 15;
        int bannerHeight = 15;

        // Calculate the maximum width of all the elements we want to display
        int maxTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                Math.max(client.textRenderer.getWidth(richBoosterTimeRemaining), client.textRenderer.getWidth(backpackTimeInfo))
        );

        maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(totalTokensInfo));
        maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(tokenBalanceInfo));

        if (showInstructions) {
            // Get dynamic key bindings and add instruction text widths
            String boosterKey = boosterKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleHudKey = toggleHudKeyBinding.getBoundKeyLocalizedText().getString();
            String showPlayerListKey = showPlayerListKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleInstructionsKey = toggleInstructionsKeyBinding.getBoundKeyLocalizedText().getString();

            int instructionsWidth = Math.max(
                    Math.max(client.textRenderer.getWidth("---------------------------------"), client.textRenderer.getWidth(boosterKey + " - Run Booster Command")),
                    Math.max(client.textRenderer.getWidth(toggleHudKey + " - To toggle HUD"), client.textRenderer.getWidth(toggleInstructionsKey + " - Toggle this text"))
            );
            maxTextWidth = Math.max(maxTextWidth, instructionsWidth);
        }

        // Calculate total lines
        int lineCount = 6; // 5 lines for boosters + backpack + 1 line for total tokens + 1 line for balance
        if (showInstructions) {
            lineCount += 7;  // Instructions take 7 extra lines
        }

        // Calculate window dimensions
        int windowWidth = maxTextWidth + 2 * padding;
        int windowHeight = (lineCount * lineHeight) + bannerHeight + 2 * padding;

        // Ensure window stays within screen
        ensureWindowWithinScreen(client, windowWidth, windowHeight);

        // Draw the banner and the close button
        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + bannerHeight, 0xD0000000);
        String closeButton = "[X]";
        int closeButtonWidth = client.textRenderer.getWidth(closeButton);
        int closeButtonX = windowX + windowWidth - closeButtonWidth - padding;
        int closeButtonYStart = windowY + (bannerHeight / 2) - (client.textRenderer.fontHeight / 2);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(closeButton), closeButtonX, closeButtonYStart, 0xFFFFFF);

        // Draw the HUD body with background
        int currentY = windowY + bannerHeight + padding;
        drawContext.fill(windowX, windowY + bannerHeight, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        // Render booster and sales info
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

        // Render dynamic instructions if they are visible
        if (showInstructions) {
            String boosterKey = boosterKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleHudKey = toggleHudKeyBinding.getBoundKeyLocalizedText().getString();
            String showPlayerListKey = showPlayerListKeyBinding.getBoundKeyLocalizedText().getString();
            String toggleInstructionsKey = toggleInstructionsKeyBinding.getBoundKeyLocalizedText().getString();

            drawContext.drawTextWithShadow(client.textRenderer, Text.of("---------------------------------"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterKey + " - Run Booster Command"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(toggleHudKey + " - To toggle HUD"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(showPlayerListKey + " - Custom Player List"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of(toggleInstructionsKey + " - Toggle this text"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of("/summaryclear - To clear Total Sales"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            drawContext.drawTextWithShadow(client.textRenderer, Text.of("---------------------------------"), windowX + padding, currentY, 0xFFFFFF);
        }
        needsRenderUpdate = false;
    }

    public void clearTotalSales() {
        totalSales = 0;
        totalTokensInfo = Formatting.YELLOW + "Total Sales: 0 Tokens";
        needsRenderUpdate = true;
        // System.out.println("Total sales cleared.");
    }

    public void handleMousePress(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int bannerHeight = 15; // Fixed banner height for close button positioning

        // Calculate the window width dynamically based on whether instructions are shown or not
        int maxTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                Math.max(client.textRenderer.getWidth(richBoosterTimeRemaining), client.textRenderer.getWidth(backpackTimeInfo))
        );

        // Add instruction width if instructions are visible
        if (showInstructions) {
            int instructionsWidth = Math.max(
                    Math.max(client.textRenderer.getWidth("---------------------------------"), client.textRenderer.getWidth("B - Run Booster Command")),
                    Math.max(client.textRenderer.getWidth("H - To toggle HUD"), client.textRenderer.getWidth("N - Toggle this text"))
            );
            maxTextWidth = Math.max(maxTextWidth, instructionsWidth);
        }

        int windowWidth = maxTextWidth + 2 * padding; // Total window width

        // The [X] button is always at the far right of the banner
        String closeButton = "[X]";
        int closeButtonWidth = client.textRenderer.getWidth(closeButton);
        int closeButtonXStart = windowX + windowWidth - closeButtonWidth - padding; // Positioned at the far right with padding

        // Define the hitbox for the close button
        if (mouseX >= closeButtonXStart && mouseX <= closeButtonXStart + closeButtonWidth
                && mouseY >= windowY && mouseY <= windowY + bannerHeight) {
            isCloseButtonHovered = true;
            LOGGER.info("Close button hovered during mouse press");
        } else {
            isCloseButtonHovered = false;
        }

        // Make the HUD draggable only from the banner area
        if (mouseX >= windowX && mouseX <= windowX + windowWidth && mouseY >= windowY && mouseY <= windowY + bannerHeight) {
            isDragging = true;
            hasDragged = true;  // Reset the flag when dragging starts
            mouseXOffset = (int) (mouseX - windowX);
            mouseYOffset = (int) (mouseY - windowY);
            LOGGER.info("Window drag started");
        }
    }

    public void handleMouseRelease(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int bannerHeight = 15; // Fixed banner height for close button positioning

        // Calculate the window width dynamically based on whether instructions are shown or not
        int maxTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                Math.max(client.textRenderer.getWidth(richBoosterTimeRemaining), client.textRenderer.getWidth(backpackTimeInfo))
        );

        // Add instruction width if instructions are visible
        if (showInstructions) {
            int instructionsWidth = Math.max(
                    Math.max(client.textRenderer.getWidth("---------------------------------"), client.textRenderer.getWidth("B - Run Booster Command")),
                    Math.max(client.textRenderer.getWidth("H - To toggle HUD"), client.textRenderer.getWidth("N - Toggle this text"))
            );
            maxTextWidth = Math.max(maxTextWidth, instructionsWidth);
        }

        int windowWidth = maxTextWidth + 2 * padding; // Total window width

        // The [X] button is always at the far right of the banner
        String closeButton = "[X]";
        int closeButtonWidth = client.textRenderer.getWidth(closeButton);
        int closeButtonXStart = windowX + windowWidth - closeButtonWidth - padding; // Positioned at the far right with padding

        // Check if the close button was clicked
        if (isCloseButtonHovered && mouseX >= closeButtonXStart && mouseX <= closeButtonXStart + closeButtonWidth
                && mouseY >= windowY && mouseY <= windowY + bannerHeight) {
            LOGGER.info("Close button clicked");
            iBlockyBoosterNotificationClient.toggleHudVisibility(false); // Hide the HUD
            setHudVisible(false);  // Hide the HUD in the BoosterStatusWindow
        }

        // Save window position only if dragging was active and the window was moved
        if (isDragging && hasDragged) {
            saveWindowPosition();
            LOGGER.info("Window position saved after dragging");
        }

        isDragging = false; // Stop dragging
    }

    public void onMouseMove(double mouseX, double mouseY) {
        // Only update the window position if dragging is active
        if (isDragging) {
            MinecraftClient client = MinecraftClient.getInstance();

            // Move the window to follow the cursor
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);

            // Mark that the window has been moved
            hasDragged = true;

            // Ensure the window stays within the screen bounds
            ensureWindowWithinScreen(client, windowX, windowY);

            LOGGER.info("Window dragged to new position: X={}, Y={}", windowX, windowY);
        }
    }

    private void ensureWindowWithinScreen(MinecraftClient client, int windowWidth, int windowHeight) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        if (windowX < 0) {
            windowX = 0;
        } else if (windowX + windowWidth > screenWidth) {
            windowX = screenWidth - windowWidth;
        }

        if (windowY < 0) {
            windowY = 0;
        } else if (windowY + windowHeight > screenHeight) {
            windowY = screenHeight - windowHeight;
        }
    }

    public void handleScreenClose() {
        isDragging = false;
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

        System.out.println("Total seconds calculated: " + totalSeconds);  // Log total seconds calculated
        return totalSeconds;
    }

}