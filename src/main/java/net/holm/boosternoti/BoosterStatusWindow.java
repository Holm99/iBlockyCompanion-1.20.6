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
    private static String sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
    private static String timeRemaining = Formatting.RED + "Tokens Booster: N/A";
    private static String richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";

    private static int tokensBoosterRemainingSeconds = 0;
    private static int richBoosterRemainingSeconds = 0;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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

        updateTokensBoosterTime();
        updateRichBoosterTime();
    }

    public static void updateSellBoostDisplay(double totalSellBoost) {
        sellBoostInfo = Formatting.GOLD + "Sell Boost: " + Formatting.YELLOW + String.format("%.2f", totalSellBoost) + "x";
    }

    public static void setTokensBoosterActive(boolean active, String multiplier, String time) {
        if (active) {
            tokensBoosterRemainingSeconds = parseTimeToSeconds(time);
            SellBoostCalculator.setTokenBoost(Double.parseDouble(multiplier));
        } else {
            SellBoostCalculator.setTokenBoost(1.0);
        }
        updateTokensBoosterTime();
    }

    public static void setRichBoosterActive(boolean active, String time) {
        if (active) {
            richBoosterRemainingSeconds = parseTimeToSeconds(time);
            SellBoostCalculator.setRichPetBoost(2.0);
        } else {
            SellBoostCalculator.setRichPetBoost(1.0);
        }
        updateRichBoosterTime();
    }

    private static void updateTokensBoosterTime() {
        if (tokensBoosterRemainingSeconds > 0) {
            timeRemaining = Formatting.AQUA + "Tokens Booster: " + formatTime(tokensBoosterRemainingSeconds);
        } else {
            clearTokenBoosterInfo();
        }
    }

    private static void updateRichBoosterTime() {
        if (richBoosterRemainingSeconds > 0) {
            richBoosterTimeRemaining = Formatting.AQUA + "Rich Booster: " + formatTime(richBoosterRemainingSeconds);
        } else {
            richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        }
    }

    public static void clearTokenBoosterInfo() {
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
    }

    public static void clearBoosterInfo() {
        sellBoostInfo = Formatting.RED + "Sell Boost: N/A";
        timeRemaining = Formatting.RED + "Tokens Booster: N/A";
        richBoosterTimeRemaining = Formatting.RED + "Rich Booster: N/A";
        SellBoostCalculator.resetBoosts();
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
        MinecraftClient client = MinecraftClient.getInstance();
        int padding = 5;
        int lineHeight = 15;

        int longestTextWidth = Math.max(
                Math.max(client.textRenderer.getWidth(sellBoostInfo), client.textRenderer.getWidth(timeRemaining)),
                client.textRenderer.getWidth(richBoosterTimeRemaining)
        );

        int windowWidth = longestTextWidth + 2 * padding;
        int windowHeight = 3 * lineHeight + 2 * padding;

        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(sellBoostInfo), windowX + padding, windowY + padding, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + padding, windowY + padding + lineHeight, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(richBoosterTimeRemaining), windowX + padding, windowY + padding + lineHeight * 2, 0xFFFFFF);

        if (isDragging) {
            double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
            double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);

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
