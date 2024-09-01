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
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        // Trim hours if they are zero
        if (hours > 0) {
            return Formatting.AQUA + String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return Formatting.AQUA + String.format("%dm %ds", minutes, seconds);
        } else {
            return Formatting.AQUA + String.format("%ds", seconds);
        }
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int windowWidth = 150;
        int windowHeight = 80;

        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterInfo), windowX + 5, windowY + 5, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(sellBoostInfo), windowX + 5, windowY + 20, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + 5, windowY + 35, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(richBoosterTimeRemaining), windowX + 5, windowY + 50, 0xFFFFFF);

        if (isDragging) {
            double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
            double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);
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
