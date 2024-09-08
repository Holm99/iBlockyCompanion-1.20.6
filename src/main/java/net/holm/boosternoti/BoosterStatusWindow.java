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

public class BoosterStatusWindow implements HudRenderCallback {
    private static final int BANNER_HEIGHT = 20; // Height of the banner at the top
    private boolean hudVisible = true;  // Instance field to manage visibility
    private boolean isDragging = false;
    private boolean isCloseButtonHovered = false; // State for close button hover
    private boolean needsRenderUpdate = true;  // Flag to indicate when rendering content is needed
    private boolean showInstructions = true;  // Flag to manage instructions visibility

    private static final Logger LOGGER = LoggerFactory.getLogger(BoosterStatusWindow.class);

    private int windowX, windowY;
    private int mouseXOffset = 0, mouseYOffset = 0;

    private static String sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
    private String timeRemaining = Formatting.RED + "Tokens Booster: N/A";
    private String richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
    private String backpackTimeInfo = Formatting.RED + "Backpack Time: N/A"; // New variable to store backpack time info

    private int tokensBoosterRemainingSeconds = 0;
    private int richBoosterRemainingSeconds = 0;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> countdownTask;

    private final BoosterConfig config;

    // Key bindings for toggling instructions
    private static final KeyBinding toggleInstructionsKeyBinding = new KeyBinding(
            "key.boosternoti.toggleInstructions",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.boosternoti.general"
    );

    // Constructor initializes window position using config
    public BoosterStatusWindow(BoosterConfig config) {
        this.config = config; // Assign the passed config to the instance field
        windowX = config.windowX;
        windowY = config.windowY;
        startCountdown(); // Start the countdown when the HUD is initialized

        // Register the keybinding
        KeyBindingHelper.registerKeyBinding(toggleInstructionsKeyBinding);

        // Register a tick event to check for key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleInstructionsKeyBinding.wasPressed()) {
                showInstructions = !showInstructions;  // Toggle instructions visibility
                needsRenderUpdate = true; // Trigger a render update
            }
        });
        startBackpackTimeTracking();
    }

    // Method to toggle the visibility of the HUD
    public void setHudVisible(boolean visible) {
        this.hudVisible = visible;
        needsRenderUpdate = true;  // Trigger render update
    }

    private void startBackpackTimeTracking() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            backpackTimeInfo = Formatting.AQUA + "Backpack Time: " + BackpackSpaceTracker.getElapsedTime();
            needsRenderUpdate = true; // Trigger render update
        });
    }

    private void saveWindowPosition() {
        config.windowX = windowX;
        config.windowY = windowY;
        config.save();  // Save the updated config to the file
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

            needsRenderUpdate = true; // Set flag to true whenever the countdown updates
        } else {
            stopCountdown();
        }
    }

    private void updateTotalSellBoost() {
        double totalSellBoost = SellBoostCalculator.getTotalSellBoost();
        updateSellBoostDisplay(totalSellBoost);
        needsRenderUpdate = true; // Trigger a render update whenever the total sell boost changes
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
        needsRenderUpdate = true; // Trigger a render update when booster status changes
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
        needsRenderUpdate = true; // Trigger a render update when booster status changes
    }

    private void updateTokensBoosterTime() {
        if (tokensBoosterRemainingSeconds > 0) {
            timeRemaining = Formatting.AQUA + "Tokens Booster: " + formatTime(tokensBoosterRemainingSeconds);
        } else {
            clearTokenBoosterInfo();
        }
        needsRenderUpdate = true; // Trigger a render update when time changes
    }

    private void updateRichBoosterTime() {
        if (richBoosterRemainingSeconds > 0) {
            richBoosterTimeRemaining = Formatting.AQUA + "Rich Booster: " + formatTime(richBoosterRemainingSeconds);
        } else {
            clearRichBoosterInfo();
        }
        needsRenderUpdate = true; // Trigger a render update when time changes
    }

    public void clearTokenBoosterInfo() {
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        needsRenderUpdate = true; // Trigger a render update when info is cleared
    }

    public void clearRichBoosterInfo() {
        richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        needsRenderUpdate = true; // Trigger a render update when info is cleared
    }

    public void clearBoosterInfo() {
        sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        SellBoostCalculator.resetBoosts();
        needsRenderUpdate = true; // Trigger a render update when all info is cleared
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
            return; // Do not render HUD if game is not active or if it's hidden
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5; // Padding for the text inside the box
        int lineHeight = 15; // Height of each line of text

        // Adjust the banner height to 15px
        int bannerHeight = 15;

        // Calculate the maximum text width dynamically based on all HUD elements, including instructions if visible
        int maxTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                Math.max(client.textRenderer.getWidth(richBoosterTimeRemaining), client.textRenderer.getWidth(backpackTimeInfo)) // Include backpack time info
        );

        if (showInstructions) {
            int instructionsWidth = Math.max(
                    Math.max(client.textRenderer.getWidth("----------------------"), client.textRenderer.getWidth("B - Run Booster Command")),
                    Math.max(client.textRenderer.getWidth("H - To toggle HUD"), client.textRenderer.getWidth("N - Toggle this text"))
            );
            maxTextWidth = Math.max(maxTextWidth, instructionsWidth);
        }

        // Calculate the number of lines to display, including backpack info and instructions
        int lineCount = 4; // 3 lines for boosters + 1 for backpack time
        if (showInstructions) {
            lineCount += 5; // Adding 5 lines for instructions (3 lines for actual text + 2 lines for separators)
        }

        int windowWidth = maxTextWidth + 2 * padding;
        int windowHeight = (lineCount * lineHeight) + bannerHeight + 2 * padding;

        ensureWindowWithinScreen(client, windowWidth, windowHeight);  // Ensure the window is within the screen bounds

        // Render the banner/header with a darker background color than the HUD (darker and reduced height)
        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + bannerHeight, 0xD0000000); // Darker banner (almost black)

        // Draw the close button (X) on the right side of the banner
        String closeButton = "[X]";
        int closeButtonWidth = client.textRenderer.getWidth(closeButton);
        int closeButtonX = windowX + windowWidth - closeButtonWidth - padding;
        int closeButtonYStart = windowY + (bannerHeight / 2) - (client.textRenderer.fontHeight / 2);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(closeButton), closeButtonX, closeButtonYStart, 0xFFFFFF);

        // Start currentY after the banner
        int currentY = windowY + bannerHeight + padding; // Initialize the current Y position for the text after the banner

        // Render booster information
        drawContext.fill(windowX, windowY + bannerHeight, windowX + windowWidth, windowY + windowHeight, 0x80000000); // HUD background (transparent dark)
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(sellBoostInfo), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight; // Move to the next line

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(richBoosterTimeRemaining), windowX + padding, currentY, 0xFFFFFF);
        currentY += lineHeight;

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(backpackTimeInfo), windowX + padding, currentY, 0xFFFFFF); // Draw backpack info
        currentY += lineHeight;

        // Draw the instructions if visible
        if (showInstructions) {
            drawContext.drawTextWithShadow(client.textRenderer, Text.of("----------------------"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;
            drawContext.drawTextWithShadow(client.textRenderer, Text.of("B - Run Booster Command"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;
            drawContext.drawTextWithShadow(client.textRenderer, Text.of("H - To toggle HUD"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;
            drawContext.drawTextWithShadow(client.textRenderer, Text.of("N - Toggle this text"), windowX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;
            drawContext.drawTextWithShadow(client.textRenderer, Text.of("----------------------"), windowX + padding, currentY, 0xFFFFFF);
        }

        // Handle dragging only within the banner
        if (isDragging) {
            double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
            double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

            // Move the window to follow the cursor exactly, locking the initial offset to the drag start position
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);

            ensureWindowWithinScreen(client, windowWidth, windowHeight);  // Re-check after dragging
        }

        needsRenderUpdate = false; // Reset the flag after rendering
    }

    public void handleMousePress(double mouseX, double mouseY) {
        int bannerHeight = 15; // Updated banner height

        // Calculate windowWidth dynamically based on current content
        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int lineHeight = 15;
        int maxTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                Math.max(client.textRenderer.getWidth(richBoosterTimeRemaining), client.textRenderer.getWidth(backpackTimeInfo))
        );

        if (showInstructions) {
            int instructionsWidth = Math.max(
                    Math.max(client.textRenderer.getWidth("----------------------"), client.textRenderer.getWidth("B - Run Booster Command")),
                    Math.max(client.textRenderer.getWidth("H - To toggle HUD"), client.textRenderer.getWidth("N - Toggle this text"))
            );
            maxTextWidth = Math.max(maxTextWidth, instructionsWidth);
        }

        int windowWidth = maxTextWidth + 2 * padding; // Calculate the window width

        // Handle close button click
        String closeButton = "[X]";
        int closeButtonWidth = client.textRenderer.getWidth(closeButton);
        int closeButtonXStart = windowX + windowWidth - closeButtonWidth - 5;

        // Check if the close button was hovered and clicked
        if (mouseX >= closeButtonXStart && mouseX <= closeButtonXStart + closeButtonWidth
                && mouseY >= windowY && mouseY <= windowY + bannerHeight) {
            isCloseButtonHovered = true;
        }

        // Make the HUD draggable only from the banner area
        if (mouseX >= windowX && mouseX <= windowX + windowWidth && mouseY >= windowY && mouseY <= windowY + bannerHeight) {
            isDragging = true;
            mouseXOffset = (int) (mouseX - windowX);
            mouseYOffset = (int) (mouseY - windowY);
        }
    }

    public void handleMouseRelease(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int lineHeight = 15;

        // Calculate windowWidth dynamically based on current content
        int maxTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                Math.max(client.textRenderer.getWidth(richBoosterTimeRemaining), client.textRenderer.getWidth(backpackTimeInfo))
        );

        if (showInstructions) {
            int instructionsWidth = Math.max(
                    Math.max(client.textRenderer.getWidth("----------------------"), client.textRenderer.getWidth("B - Run Booster Command")),
                    Math.max(client.textRenderer.getWidth("H - To toggle HUD"), client.textRenderer.getWidth("N - Toggle this text"))
            );
            maxTextWidth = Math.max(maxTextWidth, instructionsWidth);
        }

        int windowWidth = maxTextWidth + 2 * padding; // Calculate the window width

        // Close button release check
        if (isCloseButtonHovered) {
            String closeButton = "[X]";
            int closeButtonWidth = client.textRenderer.getWidth(closeButton);
            int closeButtonXStart = windowX + windowWidth - closeButtonWidth - 5;

            if (mouseX >= closeButtonXStart && mouseX <= closeButtonXStart + closeButtonWidth
                    && mouseY >= windowY && mouseY <= windowY + BANNER_HEIGHT) {
                // Close button clicked and released, hide HUD
                iBlockyBoosterNotificationClient.toggleHudVisibility(false); // Update the main class state by hiding HUD
                setHudVisible(false);  // Hide the HUD in the BoosterStatusWindow
            }
            isCloseButtonHovered = false; // Reset hover state after release
        }

        isDragging = false;
        saveWindowPosition();
    }

    private void ensureWindowWithinScreen(MinecraftClient client, int windowWidth, int windowHeight) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Adjust windowX to stay within screen width
        if (windowX < 0) {
            windowX = 0;
        } else if (windowX + windowWidth > screenWidth) {
            windowX = screenWidth - windowWidth;
        }

        // Adjust windowY to stay within screen height
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

        String[] parts = time.split(" ");
        for (String part : parts) {
            if (part.endsWith("d")) {
                int days = Integer.parseInt(part.replace("d", ""));
                totalSeconds += days * 86400;
            } else if (part.endsWith("h")) {
                int hours = Integer.parseInt(part.replace("h", ""));
                totalSeconds += hours * 3600;
            } else if (part.endsWith("m")) {
                int minutes = Integer.parseInt(part.replace("m", ""));
                totalSeconds += minutes * 60;
            } else if (part.endsWith("s")) {
                int seconds = Integer.parseInt(part.replace("s", ""));
                totalSeconds += seconds;
            }
        }

        return totalSeconds;
    }
}
