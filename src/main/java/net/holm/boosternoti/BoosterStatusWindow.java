package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class BoosterStatusWindow implements HudRenderCallback {

    private static boolean isDragging = false;
    private static int windowX = 100;
    private static int windowY = 100;
    private static int mouseXOffset = 0;
    private static int mouseYOffset = 0;
    private static String boosterInfo = "Active Booster: ❌";
    private static String timeRemaining = "Time Remaining: N/A";

    public static void setBoosterActive(boolean active, String time) {
        if (active) {
            boosterInfo = "Active Booster: ✔️";
            timeRemaining = "Time Remaining: " + time;
        } else {
            boosterInfo = "Active Booster: ❌";
            timeRemaining = "Time Remaining: N/A";
        }
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int windowWidth = 150;
        int windowHeight = 50;

        // Draw window background
        drawContext.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0x80000000); // Semi-transparent black

        // Draw booster info
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(boosterInfo), windowX + 5, windowY + 5, 0xFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.of(timeRemaining), windowX + 5, windowY + 20, 0xFFFFFF);

        // Handle dragging
        if (isDragging) {
            double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
            double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
            windowX = MathHelper.floor(mouseX - mouseXOffset);
            windowY = MathHelper.floor(mouseY - mouseYOffset);
        }
    }

    public static void handleMousePress(double mouseX, double mouseY) {
        if (mouseX >= windowX && mouseX <= windowX + 150 && mouseY >= windowY && mouseY <= windowY + 50) {
            isDragging = true;
            mouseXOffset = (int) (mouseX - windowX);
            mouseYOffset = (int) (mouseY - windowY);
        }
    }

    public static void handleMouseRelease() {
        isDragging = false;
    }
}
