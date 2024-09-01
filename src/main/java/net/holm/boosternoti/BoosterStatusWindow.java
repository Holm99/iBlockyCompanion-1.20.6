package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BoosterStatusWindow implements HudRenderCallback {

    private static boolean isDragging = false;
    private static int windowX = 100;
    private static int windowY = 100;
    private static int mouseXOffset = 0;
    private static int mouseYOffset = 0;
    private static String boosterInfo = Formatting.RED + "Active Booster: ❌"; // Start with red color for no booster
    private static String sellBoostInfo = Formatting.RED + "Sell Boost: N/A"; // RED color for no active boost
    private static String timeRemaining = Formatting.RED + "Tokens Booster: N/A"; // RED color for no remaining time
    private static String richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A"; // RED color for no rich booster

    private static int tokensBoosterRemainingSeconds = 0;
    private static int richBoosterRemainingSeconds = 0;

    // Use a ScheduledExecutorService for managing countdowns
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Static initializer to start the scheduler
    static {
        scheduler.scheduleAtFixedRate(BoosterStatusWindow::handleCountdown, 0, 1, TimeUnit.SECONDS);
    }

    private static void handleCountdown() {
        if (tokensBoosterRemainingSeconds > 0) {
            tokensBoosterRemainingSeconds--;
        }

        if (richBoosterRemainingSeconds > 0) {
            richBoosterRemainingSeconds--;
        }

        // Update the display after decrementing
        updateTokensBoosterTime();
        updateRichBoosterTime();
    }

    public static void setTokensBoosterActive(boolean active, String multiplier, String time) {
        if (active) {
            boosterInfo = Formatting.GREEN + "Active Booster: ✔"; // Green color for active booster
            sellBoostInfo = Formatting.GOLD + "Sell Boost: " + Formatting.YELLOW + multiplier + "x"; // Gold and Yellow for boost details
            tokensBoosterRemainingSeconds = parseTimeToSeconds(time);
            updateTokensBoosterTime();
        } else {
            boosterInfo = Formatting.RED + "Active Booster: ❌"; // Red color for inactive booster
            sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
            timeRemaining = Formatting.RED + "Tokens Booster: N/A";
            tokensBoosterRemainingSeconds = 0;
        }
    }

    public static void setRichBoosterActive(boolean active, String time) {
        if (active) {
            richBoosterTimeRemaining = Formatting.GREEN + "Rich Booster: ✔"; // Green color for active booster
            richBoosterRemainingSeconds = parseTimeToSeconds(time);
            updateRichBoosterTime();
        } else {
            richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
            richBoosterRemainingSeconds = 0;
        }
    }

    private static void updateTokensBoosterTime() {
        if (tokensBoosterRemainingSeconds > 0) {
            timeRemaining = Formatting.AQUA + "Tokens Booster: " + formatTime(tokensBoosterRemainingSeconds); // Prefix for tokens booster
        } else {
            timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        }
    }

    private static void updateRichBoosterTime() {
        if (richBoosterRemainingSeconds > 0) {
            richBoosterTimeRemaining = Formatting.AQUA + "Rich Booster: " + formatTime(richBoosterRemainingSeconds); // Prefix for rich booster
        } else {
            richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        }
    }

    public static void clearBoosterInfo() {
        boosterInfo = Formatting.RED + "Active Booster: ❌";
        sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        tokensBoosterRemainingSeconds = 0;
        richBoosterRemainingSeconds = 0;
    }

    private static String formatTime(int totalSeconds) {
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {  // Show hours if there are any days or hours
            formattedTime.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {  // Show minutes if there are any days, hours, or minutes
            formattedTime.append(minutes).append("m ");
        }
        formattedTime.append(seconds).append("s");

        return formattedTime.toString();
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int lineHeight = 15;

        int longestTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(boosterInfo), client.textRenderer.getWidth(sellBoostInfo)),
                Math.max(client.textRenderer.getWidth(timeRemaining), client.textRenderer.getWidth(richBoosterTimeRemaining))
        );

        int windowWidth = longestTextWidth + 2 * padding;
        int windowHeight = 4 * lineHeight + 2 * padding;

        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterInfo), windowX + padding, windowY + padding, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(sellBoostInfo), windowX + padding, windowY + padding + lineHeight, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + padding, windowY + padding + lineHeight * 2, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(richBoosterTimeRemaining), windowX + padding, windowY + padding + lineHeight * 3, 0xFFFFFF);

        if (isDragging) {
            double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
            double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);

            // Ensure window stays within screen bounds
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            windowX = MathHelper.clamp(windowX, 0, screenWidth - windowWidth);
            windowY = MathHelper.clamp(windowY, 0, screenHeight - windowHeight);
        }
    }

    public static void handleMousePress(double mouseX, double mouseY) {
        if (mouseX >= windowX && mouseX <= windowX + 150 && mouseY >= windowY && mouseY <= windowY + 80) {
            isDragging = true;
            mouseXOffset = (int) (mouseX - windowX);
            mouseYOffset = (int) (mouseY - windowY);
        }
    }

    public static void handleMouseRelease() {
        isDragging = false;
    }

    public static void handleScreenClose() {
        isDragging = false; // Ensure dragging stops when screen is closed or changed
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