package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Formatting;  // Import for color and text formatting

public class BoosterStatusWindow implements HudRenderCallback {

    private static boolean isDragging = false;
    private static int windowX = 100;
    private static int windowY = 100;
    private static int mouseXOffset = 0;
    private static int mouseYOffset = 0;
    private static String boosterInfo = Formatting.RED + "Active Booster: ❌"; // Start with red color for no booster
    private static String sellBoostInfo = Formatting.GRAY + "Sell Boost: N/A"; // Gray color for no active boost
    private static String timeRemaining = Formatting.GRAY + "Time Remaining: N/A"; // Gray color for no remaining time

    private static int remainingSeconds = 0;
    private static boolean isCountingDown = false;

    public BoosterStatusWindow() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isCountingDown && remainingSeconds > 0) {
                assert client.world != null;
                if (client.world.getTime() % 20 == 0) {
                    remainingSeconds--;
                    updateDisplayTime();
                }
            }
        });
    }

    public static void setBoosterActive(boolean active, String multiplier, String time) {
        if (active) {
            boosterInfo = Formatting.GREEN + "Active Booster: ✔"; // Green color for active booster
            sellBoostInfo = Formatting.GOLD + "Sell Boost: " + Formatting.YELLOW + multiplier + "x"; // Gold and Yellow for boost details
            remainingSeconds = parseTimeToSeconds(time);
            isCountingDown = true;
            updateDisplayTime();
        } else {
            boosterInfo = Formatting.RED + "Active Booster: ❌"; // Red color for inactive booster
            sellBoostInfo = Formatting.GRAY + "Sell Boost: N/A";
            timeRemaining = Formatting.GRAY + "Time Remaining: N/A";
            isCountingDown = false;
        }
    }

    public static void clearBoosterInfo() {
        boosterInfo = Formatting.RED + "Active Booster: ❌";
        sellBoostInfo = Formatting.GRAY + "Sell Boost: N/A";
        timeRemaining = Formatting.GRAY + "Time Remaining: N/A";
        remainingSeconds = 0;
        isCountingDown = false;
    }

    private static void updateDisplayTime() {
        int days = remainingSeconds / 86400;
        int hours = (remainingSeconds % 86400) / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        timeRemaining = Formatting.AQUA + String.format("Time Remaining: %dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int windowWidth = 150;
        int windowHeight = 65;

        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0x80000000);

        drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterInfo), windowX + 5, windowY + 5, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(sellBoostInfo), windowX + 5, windowY + 20, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + 5, windowY + 35, 0xFFFFFF);

        if (isDragging) {
            double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
            double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);
        }
    }

    public static void handleMousePress(double mouseX, double mouseY) {
        if (mouseX >= windowX && mouseX <= windowX + 150 && mouseY >= windowY && mouseY <= windowY + 65) {
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